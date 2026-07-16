package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.entity.OperationEvent;

public interface IOperationEventService extends IService<OperationEvent> {

    void record(String eventType, String bizType, Long bizId, String payload);
}
