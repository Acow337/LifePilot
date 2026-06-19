package com.hmdp.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Shop;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.enums.ErrorCode;
import com.hmdp.exception.BizException;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.rebbitmq.MQSender;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IShopService;
import com.hmdp.service.IVoucherService;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private static final int ORDER_STATUS_UNPAID = 1;
    private static final int ORDER_STATUS_PAID = 2;
    private static final int ORDER_STATUS_USED = 3;
    private static final int ORDER_STATUS_CANCELED = 4;
    private static final int ORDER_STATUS_REFUNDING = 5;
    private static final int ORDER_STATUS_REFUNDED = 6;
    private static final long ORDER_PAY_TIMEOUT_MINUTES = 15;

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

    @Resource
    private IVoucherService voucherService;

    @Resource
    private IShopService shopService;

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

    @Override
    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        if (voucherId == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "优惠券不能为空");
        }
        Long userId = UserHolder.getUser().getId();
        Voucher voucher = voucherService.getById(voucherId);
        if (voucher == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "优惠券不存在");
        }
        if (voucher.getStatus() == null || voucher.getStatus() != 1) {
            throw new BizException(ErrorCode.BIZ_ERROR, "优惠券已下架");
        }
        if (voucher.getType() != null && voucher.getType() == 1) {
            throw new BizException(ErrorCode.BAD_REQUEST, "秒杀券请使用抢购入口");
        }
        int count = count(query().eq("voucher_id", voucherId).eq("user_id", userId).getWrapper());
        if (count > 0) {
            throw new BizException(ErrorCode.BIZ_ERROR, "你已经购买过该优惠券");
        }

        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setPayType(1);
        voucherOrder.setStatus(ORDER_STATUS_UNPAID);
        boolean saved = save(voucherOrder);
        if (!saved) {
            throw new BizException(ErrorCode.BIZ_ERROR, "创建订单失败");
        }

        Map<String, Object> data = new HashMap<>(4);
        data.put("orderId", orderId);
        data.put("state", "SUCCESS");
        data.put("status", voucherOrder.getStatus());
        data.put("message", "购买成功");
        return Result.ok(data);
    }

    @Override
    public Result queryMyOrders() {
        Long userId = UserHolder.getUser().getId();
        List<VoucherOrder> orders = list(query().eq("user_id", userId).orderByDesc("create_time").getWrapper());
        List<Map<String, Object>> rows = new ArrayList<>(orders.size());
        for (VoucherOrder order : orders) {
            rows.add(toOrderRow(order));
        }
        return Result.ok(rows, (long) rows.size());
    }

    @Override
    @Transactional
    public Result payVoucherOrder(Long orderId) {
        VoucherOrder order = requireOrder(orderId);
        Long userId = UserHolder.getUser().getId();
        if (!userId.equals(order.getUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作该订单");
        }
        if (order.getStatus() == null || order.getStatus() != ORDER_STATUS_UNPAID) {
            throw new BizException(ErrorCode.BIZ_ERROR, "当前订单状态不可支付");
        }
        order.setStatus(ORDER_STATUS_PAID);
        order.setPayTime(LocalDateTime.now());
        boolean updated = updateById(order);
        if (!updated) {
            throw new BizException(ErrorCode.BIZ_ERROR, "支付失败，请重试");
        }
        return Result.ok(toOrderState(order, "支付成功"));
    }

    @Override
    @Transactional
    public Result redeemVoucherOrder(Long orderId) {
        VoucherOrder order = requireOrder(orderId);
        if (order.getStatus() == null || order.getStatus() != ORDER_STATUS_PAID) {
            throw new BizException(ErrorCode.BIZ_ERROR, "仅已支付订单可核销");
        }
        order.setStatus(ORDER_STATUS_USED);
        order.setUseTime(LocalDateTime.now());
        boolean updated = updateById(order);
        if (!updated) {
            throw new BizException(ErrorCode.BIZ_ERROR, "核销失败，请重试");
        }
        return Result.ok(toOrderState(order, "核销成功"));
    }

    @Override
    @Transactional
    public Result cancelVoucherOrder(Long orderId) {
        VoucherOrder order = requireOrder(orderId);
        Long userId = UserHolder.getUser().getId();
        if (!userId.equals(order.getUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作该订单");
        }
        if (order.getStatus() == null || order.getStatus() != ORDER_STATUS_UNPAID) {
            throw new BizException(ErrorCode.BIZ_ERROR, "仅未支付订单可取消");
        }
        order.setStatus(ORDER_STATUS_CANCELED);
        boolean updated = updateById(order);
        if (!updated) {
            throw new BizException(ErrorCode.BIZ_ERROR, "取消订单失败");
        }
        return Result.ok(toOrderState(order, "订单已取消"));
    }

    @Override
    @Transactional
    public Result requestRefund(Long orderId) {
        VoucherOrder order = requireOrder(orderId);
        Long userId = UserHolder.getUser().getId();
        if (!userId.equals(order.getUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作该订单");
        }
        if (order.getStatus() == null || order.getStatus() != ORDER_STATUS_PAID) {
            throw new BizException(ErrorCode.BIZ_ERROR, "仅已支付订单可申请退款");
        }
        order.setStatus(ORDER_STATUS_REFUNDING);
        boolean updated = updateById(order);
        if (!updated) {
            throw new BizException(ErrorCode.BIZ_ERROR, "申请退款失败");
        }
        return Result.ok(toOrderState(order, "退款申请已提交"));
    }

    @Override
    @Transactional
    public Result approveRefund(Long orderId) {
        VoucherOrder order = requireOrder(orderId);
        if (order.getStatus() == null || order.getStatus() != ORDER_STATUS_REFUNDING) {
            throw new BizException(ErrorCode.BIZ_ERROR, "仅退款中订单可通过退款");
        }
        order.setStatus(ORDER_STATUS_REFUNDED);
        order.setRefundTime(LocalDateTime.now());
        boolean updated = updateById(order);
        if (!updated) {
            throw new BizException(ErrorCode.BIZ_ERROR, "退款审核失败");
        }
        return Result.ok(toOrderState(order, "退款已通过"));
    }

    @Override
    @Transactional
    public Result rejectRefund(Long orderId) {
        VoucherOrder order = requireOrder(orderId);
        if (order.getStatus() == null || order.getStatus() != ORDER_STATUS_REFUNDING) {
            throw new BizException(ErrorCode.BIZ_ERROR, "仅退款中订单可拒绝退款");
        }
        order.setStatus(ORDER_STATUS_PAID);
        boolean updated = updateById(order);
        if (!updated) {
            throw new BizException(ErrorCode.BIZ_ERROR, "退款审核失败");
        }
        return Result.ok(toOrderState(order, "退款已拒绝"));
    }

    @Override
    @Transactional
    public Result cancelExpiredUnpaidOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(ORDER_PAY_TIMEOUT_MINUTES);
        List<VoucherOrder> orders = list(query()
                .eq("status", ORDER_STATUS_UNPAID)
                .lt("create_time", deadline)
                .getWrapper());
        for (VoucherOrder order : orders) {
            order.setStatus(ORDER_STATUS_CANCELED);
        }
        boolean updated = orders.isEmpty() || updateBatchById(orders);
        if (!updated) {
            throw new BizException(ErrorCode.BIZ_ERROR, "超时订单取消失败");
        }
        Map<String, Object> data = new HashMap<>(2);
        data.put("canceled", orders.size());
        data.put("timeoutMinutes", ORDER_PAY_TIMEOUT_MINUTES);
        return Result.ok(data);
    }

    private VoucherOrder requireOrder(Long orderId) {
        if (orderId == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "订单号不能为空");
        }
        VoucherOrder order = getById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private Map<String, Object> toOrderState(VoucherOrder order, String message) {
        Map<String, Object> data = new HashMap<>(4);
        data.put("orderId", order.getId());
        data.put("status", order.getStatus());
        data.put("statusText", toOrderStatusText(order.getStatus()));
        data.put("message", message);
        return data;
    }

    private Map<String, Object> toOrderRow(VoucherOrder order) {
        Map<String, Object> row = new HashMap<>(16);
        row.put("id", order.getId());
        row.put("voucherId", order.getVoucherId());
        row.put("status", order.getStatus());
        row.put("statusText", toOrderStatusText(order.getStatus()));
        row.put("payType", order.getPayType());
        row.put("createTime", order.getCreateTime());
        row.put("payTime", order.getPayTime());
        row.put("useTime", order.getUseTime());
        row.put("refundTime", order.getRefundTime());

        Voucher voucher = voucherService.getById(order.getVoucherId());
        if (voucher != null) {
            row.put("voucherTitle", voucher.getTitle());
            row.put("voucherType", voucher.getType());
            row.put("payValue", voucher.getPayValue());
            row.put("actualValue", voucher.getActualValue());
            row.put("shopId", voucher.getShopId());
            Shop shop = shopService.getById(voucher.getShopId());
            if (shop != null) {
                row.put("shopName", shop.getName());
            }
        }
        return row;
    }

    private String toOrderStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case ORDER_STATUS_UNPAID:
                return "未支付";
            case ORDER_STATUS_PAID:
                return "已支付";
            case ORDER_STATUS_USED:
                return "已核销";
            case ORDER_STATUS_CANCELED:
                return "已取消";
            case ORDER_STATUS_REFUNDING:
                return "退款中";
            case ORDER_STATUS_REFUNDED:
                return "已退款";
            default:
                return "未知";
        }
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
