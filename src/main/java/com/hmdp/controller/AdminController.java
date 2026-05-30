package com.hmdp.controller;

import com.hmdp.annotation.AdminActionLog;
import com.hmdp.dto.Result;
import com.hmdp.service.IBlogCommentsService;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IVoucherService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Resource
    private IBlogService blogService;

    @Resource
    private IBlogCommentsService blogCommentsService;

    @Resource
    private IVoucherService voucherService;

    @PutMapping("/blog/{id}/hide")
    @AdminActionLog(module = "blog", action = "hide", targetType = "blog", targetId = "#id")
    public Result hideBlog(@PathVariable("id") Long id) {
        return blogService.hideBlog(id);
    }

    @PutMapping("/blog/{id}/restore")
    @AdminActionLog(module = "blog", action = "restore", targetType = "blog", targetId = "#id")
    public Result restoreBlog(@PathVariable("id") Long id) {
        return blogService.restoreBlog(id);
    }

    @PutMapping("/blog-comments/{id}/hide")
    @AdminActionLog(module = "comment", action = "hide", targetType = "comment", targetId = "#id")
    public Result hideComment(@PathVariable("id") Long id) {
        return blogCommentsService.hideComment(id) ? Result.ok() : Result.fail("下架失败");
    }

    @PutMapping("/blog-comments/{id}/restore")
    @AdminActionLog(module = "comment", action = "restore", targetType = "comment", targetId = "#id")
    public Result restoreComment(@PathVariable("id") Long id) {
        return blogCommentsService.restoreComment(id) ? Result.ok() : Result.fail("恢复失败");
    }

    @PutMapping("/voucher/seckill/{voucherId}/stock")
    @AdminActionLog(module = "voucher", action = "adjust_stock", targetType = "voucher", targetId = "#voucherId")
    public Result adjustSeckillStock(@PathVariable("voucherId") Long voucherId,
                                     @RequestParam("stock") Integer stock) {
        return voucherService.adjustSeckillStock(voucherId, stock);
    }
}
