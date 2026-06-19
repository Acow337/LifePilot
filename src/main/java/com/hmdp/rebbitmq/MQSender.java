package com.hmdp.rebbitmq;

import com.hmdp.config.RabbitMQTopicConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 消息发送者
 */
@Slf4j
@Service
public class MQSender {

    @Autowired
    @Lazy
    private RabbitTemplate rabbitTemplate;

    private static final String ROUTINGKEY = "seckill.message";

    @PostConstruct
    public void initRabbitCallbacks() {
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("消息发送到交换机失败, correlationId={}, cause={}",
                        correlationData == null ? null : correlationData.getId(), cause);
            }
        });
        rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
            Message msg = message;
            log.error("消息路由失败, replyCode={}, replyText={}, exchange={}, routingKey={}, body={}",
                    replyCode, replyText, exchange, routingKey,
                    msg == null ? null : new String(msg.getBody(), StandardCharsets.UTF_8));
        });
    }

    /**
     * 发送秒杀信息
     * @param msg
     */
    public void sendSeckillMessage(String msg){
        log.info("发送消息: {}", msg);
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
        rabbitTemplate.convertAndSend(RabbitMQTopicConfig.EXCHANGE, ROUTINGKEY, msg, correlationData);
        try {
            CorrelationData.Confirm confirm = correlationData.getFuture().get(3, TimeUnit.SECONDS);
            if (confirm == null || !confirm.isAck()) {
                throw new IllegalStateException("消息发送失败: broker未确认");
            }
        } catch (Exception e) {
            throw new IllegalStateException("消息发送失败", e);
        }
    }
}
