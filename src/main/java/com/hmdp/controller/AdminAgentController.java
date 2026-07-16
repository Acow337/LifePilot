package com.hmdp.controller;

import com.hmdp.dto.Result;
import com.hmdp.entity.AgentTrace;
import com.hmdp.service.IAgentTraceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/admin/agent")
public class AdminAgentController {

    @Resource
    private IAgentTraceService agentTraceService;

    @GetMapping("/traces")
    public Result queryTraces(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @RequestParam(value = "intent", required = false) String intent,
            @RequestParam(value = "errorCode", required = false) String errorCode
    ) {
        return agentTraceService.queryTraces(page, size, sessionId, intent, errorCode);
    }

    @PostMapping("/traces")
    public Result recordTrace(@RequestBody AgentTrace trace) {
        return agentTraceService.recordTrace(trace);
    }
}
