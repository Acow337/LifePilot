package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IVoucherOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {
    @Autowired
    private IVoucherOrderService voucherOrderService;

    @PostMapping("seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        return voucherOrderService.seckillVoucher(voucherId);
    }

    @PostMapping("{id}")
    public Result createVoucherOrder(@PathVariable("id") Long voucherId) {
        return voucherOrderService.createVoucherOrder(voucherId);
    }

    @GetMapping("my")
    public Result queryMyOrders() {
        return voucherOrderService.queryMyOrders();
    }

    @PostMapping("{id}/pay")
    public Result payVoucherOrder(@PathVariable("id") Long orderId) {
        return voucherOrderService.payVoucherOrder(orderId);
    }

    @PostMapping("{id}/redeem")
    public Result redeemVoucherOrder(@PathVariable("id") Long orderId) {
        return voucherOrderService.redeemVoucherOrder(orderId);
    }

    @GetMapping("status/{id}")
    public Result querySeckillOrder(@PathVariable("id") Long orderId) {
        return voucherOrderService.querySeckillOrder(orderId);
    }
}
