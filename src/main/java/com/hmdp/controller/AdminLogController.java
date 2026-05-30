package com.hmdp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.AdminLog;
import com.hmdp.service.IAdminLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/admin/logs")
public class AdminLogController {

    @Resource
    private IAdminLogService adminLogService;

    @GetMapping
    public Result queryLogs(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(value = "operatorId", required = false) Long operatorId,
            @RequestParam(value = "module", required = false) String module,
            @RequestParam(value = "action", required = false) String action
    ) {
        try {
            int currentPage = page == null || page < 1 ? 1 : page;
            int pageSize = size == null || size < 1 ? 20 : Math.min(size, 100);
            Page<AdminLog> logPage = adminLogService.query()
                    .eq(operatorId != null, "operator_id", operatorId)
                    .eq(module != null && !module.trim().isEmpty(), "module", module)
                    .eq(action != null && !action.trim().isEmpty(), "action", action)
                    .orderByDesc("create_time")
                    .page(new Page<>(currentPage, pageSize));
            return Result.ok(logPage.getRecords(), logPage.getTotal());
        } catch (Exception e) {
            return Result.fail("操作日志表未初始化，请先执行 src/main/resources/db/hmdp_admin_mvp.sql");
        }
    }
}
