package com.hmdp.controller;

import com.hmdp.annotation.AdminActionLog;
import com.hmdp.dto.Result;
import com.hmdp.dto.admin.AdminBlogReviewDTO;
import com.hmdp.service.IBlogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/admin/blogs")
public class AdminBlogController {

    @Resource
    private IBlogService blogService;

    @GetMapping
    public Result queryBlogs(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        return blogService.queryAdminBlogs(page, size, status, keyword);
    }

    @PatchMapping("/{id}/review")
    @AdminActionLog(module = "blog", action = "review", targetType = "blog", targetId = "#blogId")
    public Result reviewBlog(@PathVariable("id") Long blogId,
                             @RequestBody AdminBlogReviewDTO reviewDTO) {
        return blogService.reviewBlog(blogId, reviewDTO);
    }

    // 兼容旧接口
    @PutMapping("/{id}/hide")
    public Result hideBlog(@PathVariable("id") Long id) {
        return blogService.hideBlog(id);
    }

    // 兼容旧接口
    @PutMapping("/{id}/restore")
    public Result restoreBlog(@PathVariable("id") Long id) {
        return blogService.restoreBlog(id);
    }
}
