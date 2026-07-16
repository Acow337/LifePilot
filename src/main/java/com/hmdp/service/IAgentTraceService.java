package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.AgentTrace;

public interface IAgentTraceService extends IService<AgentTrace> {

    Result recordTrace(AgentTrace trace);

    Result queryTraces(Integer page, Integer size, String sessionId, String intent, String errorCode);
}
