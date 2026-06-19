package com.hmdp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdp.annotation.AdminActionLog;
import com.hmdp.config.RabbitMQTopicConfig;
import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.rebbitmq.MQSender;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.GetResponse;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/seckill")
public class AdminSeckillController {

    private static final int MAX_LIMIT = 100;
    private static final int MAX_SCAN = 1000;

    @Resource
    private AmqpAdmin amqpAdmin;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private MQSender mqSender;

    @Resource
    private ObjectMapper objectMapper;

    @GetMapping("/dlq")
    public Result queryDlq(@RequestParam(value = "limit", defaultValue = "20") Integer limit,
                           @RequestParam(value = "voucherId", required = false) Long voucherId) {
        int size = normalizeLimit(limit);
        Map<String, Object> data = new HashMap<>(4);
        data.put("queue", RabbitMQTopicConfig.DLQ);
        data.put("messageCount", queryDlqCount());
        data.put("messages", peekDlqMessages(size, voucherId));
        data.put("limit", size);
        data.put("voucherId", voucherId);
        return Result.ok(data);
    }

    @PostMapping("/dlq/replay")
    @AdminActionLog(module = "voucher", action = "replay_dlq", targetType = "mq_dlq", targetId = "0")
    public Result replayDlq(@RequestParam(value = "limit", defaultValue = "20") Integer limit,
                            @RequestParam(value = "voucherId", required = false) Long voucherId) {
        int size = normalizeLimit(limit);
        int replayed = 0;
        int failed = 0;
        int scanned = 0;
        List<String> failures = new ArrayList<>();
        List<Long> orderIds = new ArrayList<>();

        while (replayed + failed < size && scanned < MAX_SCAN) {
            scanned++;
            Object msgObj = rabbitTemplate.receiveAndConvert(RabbitMQTopicConfig.DLQ, 200);
            if (msgObj == null) {
                break;
            }
            String msg = String.valueOf(msgObj);
            VoucherOrder voucherOrder = parseVoucherOrder(msg);
            if (voucherId != null && (voucherOrder == null || !voucherId.equals(voucherOrder.getVoucherId()))) {
                rabbitTemplate.convertAndSend(RabbitMQTopicConfig.DLX_EXCHANGE, RabbitMQTopicConfig.DLQ_ROUTINGKEY, msg);
                continue;
            }

            try {
                mqSender.sendSeckillMessage(msg);
                replayed++;
                if (voucherOrder != null && voucherOrder.getId() != null && orderIds.size() < 50) {
                    orderIds.add(voucherOrder.getId());
                }
            } catch (Exception e) {
                failed++;
                failures.add(e.getMessage());
                rabbitTemplate.convertAndSend(RabbitMQTopicConfig.DLX_EXCHANGE, RabbitMQTopicConfig.DLQ_ROUTINGKEY, msg);
            }
        }

        Map<String, Object> data = new HashMap<>(6);
        data.put("requested", size);
        data.put("replayed", replayed);
        data.put("failed", failed);
        data.put("scanned", scanned);
        data.put("voucherId", voucherId);
        data.put("failures", failures);
        data.put("replayedOrderIds", orderIds);
        data.put("remaining", queryDlqCount());
        return Result.ok(data);
    }

    @PostMapping("/dlq/replay/preview")
    @AdminActionLog(module = "voucher", action = "preview_replay_dlq", targetType = "mq_dlq", targetId = "0")
    public Result previewReplayDlq(@RequestParam(value = "limit", defaultValue = "20") Integer limit,
                                   @RequestParam(value = "voucherId", required = false) Long voucherId) {
        int size = normalizeLimit(limit);
        Map<String, Object> data = previewReplay(size, voucherId);
        data.put("remaining", queryDlqCount());
        return Result.ok(data);
    }

    private Integer queryDlqCount() {
        Properties properties = amqpAdmin.getQueueProperties(RabbitMQTopicConfig.DLQ);
        if (properties == null) {
            return 0;
        }
        Object count = properties.get("QUEUE_MESSAGE_COUNT");
        if (count instanceof Integer) {
            return (Integer) count;
        }
        return 0;
    }

    private List<Map<String, Object>> peekDlqMessages(int limit, Long voucherId) {
        return rabbitTemplate.execute(channel -> {
            List<Map<String, Object>> messages = new ArrayList<>();
            int scanned = 0;
            while (messages.size() < limit && scanned < MAX_SCAN) {
                scanned++;
                GetResponse response = channel.basicGet(RabbitMQTopicConfig.DLQ, false);
                if (response == null) {
                    break;
                }
                long deliveryTag = response.getEnvelope().getDeliveryTag();
                AMQP.BasicProperties props = response.getProps();
                String payload = new String(response.getBody(), StandardCharsets.UTF_8);
                VoucherOrder voucherOrder = parseVoucherOrder(payload);
                if (voucherId != null && (voucherOrder == null || !voucherId.equals(voucherOrder.getVoucherId()))) {
                    channel.basicNack(deliveryTag, false, true);
                    continue;
                }

                Map<String, Object> item = new HashMap<>(8);
                item.put("deliveryTag", deliveryTag);
                item.put("routingKey", response.getEnvelope().getRoutingKey());
                item.put("redeliver", response.getEnvelope().isRedeliver());
                item.put("messageId", props == null ? null : props.getMessageId());
                item.put("timestamp", props == null ? null : props.getTimestamp());
                item.put("orderId", voucherOrder == null ? null : voucherOrder.getId());
                item.put("voucherId", voucherOrder == null ? null : voucherOrder.getVoucherId());
                item.put("payload", payload.length() > 800 ? payload.substring(0, 800) + "..." : payload);
                messages.add(item);

                channel.basicNack(deliveryTag, false, true);
            }
            return messages;
        });
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return 20;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private VoucherOrder parseVoucherOrder(String msg) {
        try {
            return objectMapper.readValue(msg, VoucherOrder.class);
        } catch (Exception ignore) {
            return null;
        }
    }

    private Map<String, Object> previewReplay(int limit, Long voucherId) {
        return rabbitTemplate.execute(channel -> {
            int scanned = 0;
            int canReplay = 0;
            List<Long> sampleOrderIds = new ArrayList<>();
            while (canReplay < limit && scanned < MAX_SCAN) {
                scanned++;
                GetResponse response = channel.basicGet(RabbitMQTopicConfig.DLQ, false);
                if (response == null) {
                    break;
                }
                long deliveryTag = response.getEnvelope().getDeliveryTag();
                String payload = new String(response.getBody(), StandardCharsets.UTF_8);
                VoucherOrder voucherOrder = parseVoucherOrder(payload);
                boolean matched = voucherId == null || (voucherOrder != null && voucherId.equals(voucherOrder.getVoucherId()));
                if (matched) {
                    canReplay++;
                    if (voucherOrder != null && voucherOrder.getId() != null && sampleOrderIds.size() < 50) {
                        sampleOrderIds.add(voucherOrder.getId());
                    }
                }
                channel.basicNack(deliveryTag, false, true);
            }

            Map<String, Object> data = new HashMap<>(6);
            data.put("requested", limit);
            data.put("canReplay", canReplay);
            data.put("scanned", scanned);
            data.put("voucherId", voucherId);
            data.put("sampleOrderIds", sampleOrderIds);
            data.put("note", "dry-run only: no message was replayed");
            return data;
        });
    }
}
