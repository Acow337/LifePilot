package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.entity.OperationEvent;
import com.hmdp.mapper.OperationEventMapper;
import com.hmdp.service.IOperationEventService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OperationEventServiceImpl extends ServiceImpl<OperationEventMapper, OperationEvent> implements IOperationEventService {

    @Override
    public void record(String eventType, String bizType, Long bizId, String payload) {
        OperationEvent event = new OperationEvent()
                .setEventType(eventType)
                .setBizType(bizType)
                .setBizId(bizId)
                .setPayload(payload)
                .setCreateTime(LocalDateTime.now());
        try {
            save(event);
        } catch (Exception ignored) {
        }
    }
}
