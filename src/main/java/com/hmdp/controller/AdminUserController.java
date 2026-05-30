package com.hmdp.controller;

import com.hmdp.annotation.AdminActionLog;
import com.hmdp.dto.Result;
import com.hmdp.dto.admin.AdminUserRoleUpdateDTO;
import com.hmdp.dto.admin.AdminUserStatusUpdateDTO;
import com.hmdp.service.IUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    @Resource
    private IUserService userService;

    @GetMapping
    public Result queryUsers(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) Integer status
    ) {
        return userService.queryAdminUsers(page, size, keyword, status);
    }

    @PatchMapping("/{id}/status")
    @AdminActionLog(module = "user", action = "update_status", targetType = "user", targetId = "#userId")
    public Result updateUserStatus(@PathVariable("id") Long userId,
                                   @RequestBody AdminUserStatusUpdateDTO request) {
        return userService.updateUserStatus(userId, request == null ? null : request.getStatus());
    }

    @PatchMapping("/{id}/role")
    @AdminActionLog(module = "user", action = "update_role", targetType = "user", targetId = "#userId")
    public Result updateUserRole(@PathVariable("id") Long userId,
                                 @RequestBody AdminUserRoleUpdateDTO request) {
        return userService.updateUserRole(userId, request == null ? null : request.getRole());
    }

    // 兼容旧接口
    @PatchMapping("/{id}/ban")
    public Result banUser(@PathVariable("id") Long userId) {
        return userService.banUser(userId);
    }

    // 兼容旧接口
    @PatchMapping("/{id}/unban")
    public Result unbanUser(@PathVariable("id") Long userId) {
        return userService.unbanUser(userId);
    }
}
