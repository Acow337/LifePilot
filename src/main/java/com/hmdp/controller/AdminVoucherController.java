package com.hmdp.controller;

import com.hmdp.annotation.AdminActionLog;
import com.hmdp.dto.Result;
import com.hmdp.dto.admin.AdminVoucherStatusUpdateDTO;
import com.hmdp.entity.Voucher;
import com.hmdp.service.IVoucherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/admin/vouchers")
public class AdminVoucherController {

    @Resource
    private IVoucherService voucherService;

    @GetMapping
    public Result queryVouchers(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "type", required = false) Integer type,
            @RequestParam(value = "shopId", required = false) Long shopId,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "title", required = false) String title
    ) {
        return voucherService.queryAdminVouchers(page, size, type, shopId, status, title);
    }

    @AdminActionLog(module = "voucher", action = "create", targetType = "voucher", targetId = "#result.data")
    @PostMapping
    public Result createVoucher(@RequestBody Voucher voucher) {
        return voucherService.createAdminVoucher(voucher);
    }

    @AdminActionLog(module = "voucher", action = "update", targetType = "voucher", targetId = "#voucherId")
    @PutMapping("/{id}")
    public Result updateVoucher(@PathVariable("id") Long voucherId,
                                @RequestBody Voucher voucher) {
        return voucherService.updateAdminVoucher(voucherId, voucher);
    }

    @AdminActionLog(module = "voucher", action = "update_status", targetType = "voucher", targetId = "#voucherId")
    @PatchMapping("/{id}/status")
    public Result updateVoucherStatus(@PathVariable("id") Long voucherId,
                                      @RequestBody AdminVoucherStatusUpdateDTO request) {
        return voucherService.updateVoucherStatus(voucherId, request == null ? null : request.getStatus());
    }
}
