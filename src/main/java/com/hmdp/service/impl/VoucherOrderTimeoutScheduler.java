package com.hmdp.service.impl;

import com.hmdp.service.IVoucherOrderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class VoucherOrderTimeoutScheduler {

    private final IVoucherOrderService voucherOrderService;

    public VoucherOrderTimeoutScheduler(IVoucherOrderService voucherOrderService) {
        this.voucherOrderService = voucherOrderService;
    }

    @Scheduled(fixedDelayString = "${hmdp.order.timeout-scan-delay-ms:60000}")
    public void cancelExpiredUnpaidOrders() {
        voucherOrderService.cancelExpiredUnpaidOrders();
    }
}
