package com.hmdp.service.impl;

import com.hmdp.service.IVoucherOrderService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class VoucherOrderTimeoutSchedulerTest {

    @Test
    void cancelExpiredUnpaidOrdersDelegatesToVoucherOrderService() {
        IVoucherOrderService voucherOrderService = mock(IVoucherOrderService.class);
        VoucherOrderTimeoutScheduler scheduler = new VoucherOrderTimeoutScheduler(voucherOrderService);

        scheduler.cancelExpiredUnpaidOrders();

        verify(voucherOrderService).cancelExpiredUnpaidOrders();
    }
}
