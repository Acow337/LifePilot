package com.hmdp.rebbitmq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdp.config.RabbitMQTopicConfig;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.SECKILL_PENDING_ORDER_KEY;
import static com.hmdp.utils.RedisConstants.SECKILL_RESULT_KEY;

/**
 * 消息消费者
 */
@Slf4j
@Service
public class MQReceiver {

    @Resource
    IVoucherOrderService voucherOrderService;

    @Resource
    ISeckillVoucherService seckillVoucherService;

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Resource
    ObjectMapper objectMapper;

    /**
     * 接收秒杀信息并下单
     * @param msg
     */
    @Transactional
    @RabbitListener(queues = RabbitMQTopicConfig.QUEUE)
    public void receiveSeckillMessage(String msg){
        log.info("接收到消息: {}", msg);
        VoucherOrder voucherOrder = parseVoucherOrder(msg);

        Long voucherId = voucherOrder.getVoucherId();
        Long userId = voucherOrder.getUserId();
        Long orderId = voucherOrder.getId();

        // 先入库，利用唯一索引兜底防重 (user_id, voucher_id)
        try {
            voucherOrderService.save(voucherOrder);
        } catch (DuplicateKeyException e) {
            log.warn("重复下单消息，忽略。userId={}, voucherId={}", userId, voucherId);
            stringRedisTemplate.delete(SECKILL_PENDING_ORDER_KEY + orderId);
            stringRedisTemplate.opsForValue().set(SECKILL_RESULT_KEY + orderId, "SUCCESS:重复消息已忽略", 1, TimeUnit.DAYS);
            return;
        }

        log.info("扣减库存");
        // 扣减库存失败则抛异常，事务回滚(包括上面的订单插入)
        boolean success = seckillVoucherService
                .update()
                .setSql("stock = stock-1")
                .eq("voucher_id", voucherId)
                .gt("stock",0)//cas乐观锁
                .update();
        if(!success){
            log.error("库存不足，回滚订单。voucherId={}", voucherId);
            stringRedisTemplate.delete(SECKILL_PENDING_ORDER_KEY + orderId);
            stringRedisTemplate.opsForValue().set(SECKILL_RESULT_KEY + orderId, "FAIL:库存不足", 1, TimeUnit.DAYS);
            throw new AmqpRejectAndDontRequeueException("库存不足");
        }

        markSuccessAfterCommit(orderId, "SUCCESS");
    }

    VoucherOrder parseVoucherOrder(String msg) {
        try {
            return objectMapper.readValue(msg, VoucherOrder.class);
        } catch (JsonProcessingException e) {
            throw new AmqpRejectAndDontRequeueException("秒杀消息格式非法", e);
        }
    }

    void markSuccessAfterCommit(Long orderId, String result) {
        Runnable markSuccess = () -> {
            stringRedisTemplate.delete(SECKILL_PENDING_ORDER_KEY + orderId);
            stringRedisTemplate.opsForValue().set(SECKILL_RESULT_KEY + orderId, result, 1, TimeUnit.DAYS);
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            markSuccess.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                markSuccess.run();
            }
        });
    }

}
