package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.AgentTrace;
import com.hmdp.enums.ErrorCode;
import com.hmdp.exception.BizException;
import com.hmdp.mapper.AgentTraceMapper;
import com.hmdp.service.IAgentTraceService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class AgentTraceServiceImpl extends ServiceImpl<AgentTraceMapper, AgentTrace> implements IAgentTraceService {

    @Override
    public Result recordTrace(AgentTrace trace) {
        if (trace == null || !StringUtils.hasText(trace.getTraceId()) || !StringUtils.hasText(trace.getSessionId())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "trace参数不完整");
        }
        if (trace.getCreateTime() == null) {
            trace.setCreateTime(LocalDateTime.now());
        }
        if (trace.getLatencyMs() == null) {
            trace.setLatencyMs(0);
        }
        if (trace.getAnswerPreview() != null && trace.getAnswerPreview().length() > 512) {
            trace.setAnswerPreview(trace.getAnswerPreview().substring(0, 512));
        }
        save(trace);
        return Result.ok();
    }

    @Override
    public Result queryTraces(Integer page, Integer size, String sessionId, String intent, String errorCode) {
        Page<AgentTrace> result = query()
                .eq(StringUtils.hasText(sessionId), "session_id", sessionId)
                .eq(StringUtils.hasText(intent), "intent", intent)
                .eq(StringUtils.hasText(errorCode), "error_code", errorCode)
                .orderByDesc("create_time")
                .page(new Page<>(page == null ? 1 : page, size == null ? 10 : size));
        return Result.ok(result.getRecords(), result.getTotal());
    }
}
