package com.hmdp.controller;

import com.hmdp.dto.Result;
import com.hmdp.service.impl.AdminDashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    public AdminDashboardController(AdminDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public Result queryDashboard() {
        Map<String, Object> data = dashboardService.queryDashboard();
        return Result.ok(data);
    }
}
