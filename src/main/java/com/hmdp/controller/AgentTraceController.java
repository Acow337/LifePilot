package com.hmdp.controller;

import com.hmdp.dto.Result;
import com.hmdp.entity.AgentTrace;
import com.hmdp.service.IAgentTraceService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/agent/traces")
public class AgentTraceController {

    @Resource
    private IAgentTraceService agentTraceService;

    @PostMapping
    public Result recordTrace(@RequestBody AgentTrace trace) {
        return agentTraceService.recordTrace(trace);
    }
}
