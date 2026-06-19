package com.hmdp.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.enums.ErrorCode;
import com.hmdp.exception.BizException;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.rebbitmq.MQSender;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.HashMap;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.SECKILL_BEGIN_KEY;
import static com.hmdp.utils.RedisConstants.SECKILL_END_KEY;
import static com.hmdp.utils.RedisConstants.SECKILL_PENDING_ORDER_KEY;
import static com.hmdp.utils.RedisConstants.SECKILL_RESULT_KEY;
import static com.hmdp.utils.RedisConstants.SECKILL_STOCK_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private MQSender mqSender;

    @Resource
    private ObjectMapper objectMapper;

    private RateLimiter rateLimiter=RateLimiter.create(10);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    //lua脚本
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    private static final DefaultRedisScript<Long> SECKILL_ROLLBACK_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);

        SECKILL_ROLLBACK_SCRIPT = new DefaultRedisScript<>();
        SECKILL_ROLLBACK_SCRIPT.setLocation(new ClassPathResource("seckill_rollback.lua"));
        SECKILL_ROLLBACK_SCRIPT.setResultType(Long.class);
    }

    @Override
    public Result seckillVoucher(Long voucherId) {
        //令牌桶算法 限流
        if (!rateLimiter.tryAcquire(1000, TimeUnit.MILLISECONDS)){
            throw new BizException(ErrorCode.BAD_REQUEST, "目前网络正忙，请重试");
        }
        //1.执行lua脚本
        Long userId = UserHolder.getUser().getId();

        Long r = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString()
        );
        if (r != null && r == 5) {
            // Redis秒杀元数据可能在重启后丢失，尝试一次预热后重试
            warmUpSeckillMeta(voucherId);
            r = stringRedisTemplate.execute(
                    SECKILL_SCRIPT,
                    Collections.emptyList(),
                    voucherId.toString(),
                    userId.toString()
            );
        }

        //2.判断结果为0
        if (r == null) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "秒杀服务异常，请稍后重试");
        }
        int result = r.intValue();
        if (result != 0) {
            //2.1不为0代表没有购买资格
            switch (result) {
                case 1:
                    throw new BizException(ErrorCode.BIZ_ERROR, "库存不足");
                case 2:
                    throw new BizException(ErrorCode.BIZ_ERROR, "该用户重复下单");
                case 3:
                    throw new BizException(ErrorCode.BAD_REQUEST, "秒杀尚未开始");
                case 4:
                    throw new BizException(ErrorCode.BAD_REQUEST, "秒杀已结束");
                case 5:
                    throw new BizException(ErrorCode.NOT_FOUND, "秒杀活动不存在或未初始化");
                default:
                    throw new BizException(ErrorCode.BIZ_ERROR, "秒杀失败，请稍后重试");
            }
        }
        //2.2为0代表有购买资格,将下单信息保存到阻塞队列

        //2.3创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        //2.4订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        //2.5用户id
        voucherOrder.setUserId(userId);
        //2.6代金卷id
        voucherOrder.setVoucherId(voucherId);

        //2.7将信息放入MQ中
        stringRedisTemplate.opsForValue().set(SECKILL_PENDING_ORDER_KEY + orderId, "1", 10, TimeUnit.MINUTES);
        try {
            mqSender.sendSeckillMessage(objectMapper.writeValueAsString(voucherOrder));
        } catch (JsonProcessingException e) {
            rollbackSeckillQualification(orderId, voucherId, userId, "FAIL:订单消息序列化失败");
            throw new BizException(ErrorCode.INTERNAL_ERROR, "下单请求处理失败");
        } catch (Exception e) {
            rollbackSeckillQualification(orderId, voucherId, userId, "FAIL:消息投递失败");
            throw new BizException(ErrorCode.BIZ_ERROR, "下单请求拥堵，请重试");
        }


        //2.7 返回订单id
        Map<String, Object> resultData = new HashMap<>(4);
        resultData.put("orderId", orderId);
        resultData.put("state", "PENDING");
        resultData.put("message", "下单请求已受理，正在排队处理");
        return Result.ok(resultData);
//        单机模式下，使用synchronized实现锁
//        synchronized (userId.toString().intern())
//        {
//            //    createVoucherOrder的事物不会生效,因为你调用的方法，其实是this.的方式调用的，事务想要生效，
//            //    还得利用代理来生效，所以这个地方，我们需要获得原始的事务对象， 来操作事务
//            return voucherOrderService.createVoucherOrder(voucherId);
//        }
    }

    private void rollbackSeckillQualification(Long orderId, Long voucherId, Long userId, String result) {
        stringRedisTemplate.delete(SECKILL_PENDING_ORDER_KEY + orderId);
        stringRedisTemplate.execute(
                SECKILL_ROLLBACK_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString()
        );
        stringRedisTemplate.opsForValue().set(SECKILL_RESULT_KEY + orderId, result, 10, TimeUnit.MINUTES);
    }

    private void warmUpSeckillMeta(Long voucherId) {
        if (voucherId == null) {
            return;
        }
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        if (seckillVoucher == null || seckillVoucher.getStock() == null || seckillVoucher.getBeginTime() == null || seckillVoucher.getEndTime() == null) {
            return;
        }
        stringRedisTemplate.opsForValue().set(SECKILL_STOCK_KEY + voucherId, seckillVoucher.getStock().toString());
        stringRedisTemplate.opsForValue().set(SECKILL_BEGIN_KEY + voucherId,
                String.valueOf(seckillVoucher.getBeginTime().atZone(ZoneId.systemDefault()).toEpochSecond()));
        stringRedisTemplate.opsForValue().set(SECKILL_END_KEY + voucherId,
                String.valueOf(seckillVoucher.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond()));

        long keepSeconds = Math.max(3600L,
                seckillVoucher.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond() - Instant.now().getEpochSecond() + TimeUnit.DAYS.toSeconds(1));
        stringRedisTemplate.expire(SECKILL_STOCK_KEY + voucherId, keepSeconds, TimeUnit.SECONDS);
        stringRedisTemplate.expire(SECKILL_BEGIN_KEY + voucherId, keepSeconds, TimeUnit.SECONDS);
        stringRedisTemplate.expire(SECKILL_END_KEY + voucherId, keepSeconds, TimeUnit.SECONDS);
    }

    @Override
    public Result querySeckillOrder(Long orderId) {
        if (orderId == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "订单号不能为空");
        }
        Long userId = UserHolder.getUser().getId();
        VoucherOrder order = query().eq("id", orderId).eq("user_id", userId).one();
        if (order != null) {
            Map<String, Object> data = new HashMap<>(4);
            data.put("state", "SUCCESS");
            data.put("orderId", orderId);
            data.put("status", order.getStatus());
            data.put("message", "下单成功");
            return Result.ok(data);
        }

        String finalResult = stringRedisTemplate.opsForValue().get(SECKILL_RESULT_KEY + orderId);
        if (finalResult != null) {
            Map<String, Object> data = new HashMap<>(4);
            data.put("state", finalResult.startsWith("FAIL") ? "FAIL" : "UNKNOWN");
            data.put("orderId", orderId);
            data.put("message", finalResult);
            return Result.ok(data);
        }

        Boolean pending = stringRedisTemplate.hasKey(SECKILL_PENDING_ORDER_KEY + orderId);
        if (Boolean.TRUE.equals(pending)) {
            Map<String, Object> data = new HashMap<>(3);
            data.put("state", "PENDING");
            data.put("orderId", orderId);
            data.put("message", "订单处理中，请稍后刷新");
            return Result.ok(data);
        }
        throw new BizException(ErrorCode.NOT_FOUND, "订单不存在或已超出查询时效");
    }


//    @Transactional
//    public Result createVoucherOrder(Long voucherId) {
//        // 一人一单逻辑
//        Long userId = UserHolder.getUser().getId();
//
//
//        int count = query().eq("voucher_id", voucherId).eq("user_id", userId).count();
//        if (count > 0){
//            return Result.fail("你已经抢过优惠券了哦");
//        }
//
//        //5. 扣减库存
//        boolean success = seckillVoucherService.update()
//                .setSql("stock = stock - 1")
//                .eq("voucher_id", voucherId)
//                .gt("stock",0)   //加了CAS 乐观锁，Compare and swap
//                .update();
//
//        if (!success) {
//            return Result.fail("库存不足");
//        }
//
////        库存足且在时间范围内的，则创建新的订单
//        //6. 创建订单
//        VoucherOrder voucherOrder = new VoucherOrder();
//        //6.1 设置订单id，生成订单的全局id
//        long orderId = redisIdWorker.nextId("order");
//        //6.2 设置用户id
//        Long id = UserHolder.getUser().getId();
//        //6.3 设置代金券id
//        voucherOrder.setVoucherId(voucherId);
//        voucherOrder.setId(orderId);
//        voucherOrder.setUserId(id);
//        //7. 将订单数据保存到表中
//        save(voucherOrder);
//        //8. 返回订单id
//        return Result.ok(orderId);
//    }
}
