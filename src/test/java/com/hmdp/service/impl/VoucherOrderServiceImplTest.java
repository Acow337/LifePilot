package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Shop;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.exception.BizException;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VoucherOrderServiceImplTest {

    private VoucherServiceImpl voucherService;
    private ShopServiceImpl shopService;
    private RedisIdWorker redisIdWorker;
    private VoucherOrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        voucherService = mock(VoucherServiceImpl.class);
        shopService = mock(ShopServiceImpl.class);
        redisIdWorker = mock(RedisIdWorker.class);
        orderService = org.mockito.Mockito.spy(new VoucherOrderServiceImpl());
        ReflectionTestUtils.setField(orderService, "voucherService", voucherService);
        ReflectionTestUtils.setField(orderService, "shopService", shopService);
        ReflectionTestUtils.setField(orderService, "redisIdWorker", redisIdWorker);

        UserDTO user = new UserDTO();
        user.setId(9002L);
        UserHolder.saveUser(user);
    }

    @AfterEach
    void tearDown() {
        UserHolder.removeUser();
    }

    @Test
    void createVoucherOrderStoresNormalVoucherOrder() {
        Voucher voucher = new Voucher();
        voucher.setId(9001L);
        voucher.setShopId(9001L);
        voucher.setTitle("星河咖啡 50 元代金券");
        voucher.setType(0);
        voucher.setStatus(1);
        when(voucherService.getById(9001L)).thenReturn(voucher);
        org.mockito.Mockito.doReturn(0).when(orderService).count(any());
        when(redisIdWorker.nextId("order")).thenReturn(123456L);
        org.mockito.Mockito.doReturn(true).when(orderService).save(any(VoucherOrder.class));

        Result result = orderService.createVoucherOrder(9001L);

        Map<?, ?> data = (Map<?, ?>) result.getData();
        assertEquals(123456L, data.get("orderId"));
        assertEquals("SUCCESS", data.get("state"));
        ArgumentCaptor<VoucherOrder> captor = ArgumentCaptor.forClass(VoucherOrder.class);
        verify(orderService).save(captor.capture());
        assertEquals(9002L, captor.getValue().getUserId());
        assertEquals(9001L, captor.getValue().getVoucherId());
        assertEquals(1, captor.getValue().getStatus());
    }

    @Test
    void createVoucherOrderRejectsSeckillVoucher() {
        Voucher voucher = new Voucher();
        voucher.setId(9002L);
        voucher.setType(1);
        voucher.setStatus(1);
        when(voucherService.getById(9002L)).thenReturn(voucher);

        assertThrows(BizException.class, () -> orderService.createVoucherOrder(9002L));
    }

    @Test
    void queryMyOrdersReturnsDisplayFields() {
        VoucherOrder order = new VoucherOrder();
        order.setId(123456L);
        order.setUserId(9002L);
        order.setVoucherId(9001L);
        order.setStatus(2);
        order.setCreateTime(LocalDateTime.of(2026, 6, 20, 10, 0));
        org.mockito.Mockito.doReturn(List.of(order)).when(orderService).list(any());

        Voucher voucher = new Voucher();
        voucher.setId(9001L);
        voucher.setShopId(9001L);
        voucher.setTitle("星河咖啡 50 元代金券");
        voucher.setType(0);
        voucher.setPayValue(4200L);
        voucher.setActualValue(5000L);
        when(voucherService.getById(9001L)).thenReturn(voucher);

        Shop shop = new Shop();
        shop.setId(9001L);
        shop.setName("星河手冲咖啡馆");
        when(shopService.getById(9001L)).thenReturn(shop);

        Result result = orderService.queryMyOrders();

        List<?> orders = (List<?>) result.getData();
        assertEquals(1, orders.size());
        Map<?, ?> row = (Map<?, ?>) orders.get(0);
        assertEquals(123456L, row.get("id"));
        assertEquals("星河咖啡 50 元代金券", row.get("voucherTitle"));
        assertEquals("星河手冲咖啡馆", row.get("shopName"));
        assertTrue(row.containsKey("statusText"));
    }

    @Test
    void payVoucherOrderChangesUnpaidOrderToPaid() {
        VoucherOrder order = new VoucherOrder();
        order.setId(123456L);
        order.setUserId(9002L);
        order.setVoucherId(9001L);
        order.setStatus(1);
        org.mockito.Mockito.doReturn(order).when(orderService).getById(123456L);
        org.mockito.Mockito.doReturn(true).when(orderService).updateById(any(VoucherOrder.class));

        Result result = orderService.payVoucherOrder(123456L);

        Map<?, ?> data = (Map<?, ?>) result.getData();
        assertEquals(123456L, data.get("orderId"));
        assertEquals(2, data.get("status"));
        assertEquals("已支付", data.get("statusText"));
        ArgumentCaptor<VoucherOrder> captor = ArgumentCaptor.forClass(VoucherOrder.class);
        verify(orderService).updateById(captor.capture());
        assertEquals(2, captor.getValue().getStatus());
    }

    @Test
    void redeemVoucherOrderChangesPaidOrderToUsed() {
        VoucherOrder order = new VoucherOrder();
        order.setId(123456L);
        order.setUserId(9002L);
        order.setVoucherId(9001L);
        order.setStatus(2);
        org.mockito.Mockito.doReturn(order).when(orderService).getById(123456L);
        org.mockito.Mockito.doReturn(true).when(orderService).updateById(any(VoucherOrder.class));

        Result result = orderService.redeemVoucherOrder(123456L);

        Map<?, ?> data = (Map<?, ?>) result.getData();
        assertEquals(123456L, data.get("orderId"));
        assertEquals(3, data.get("status"));
        assertEquals("已核销", data.get("statusText"));
        ArgumentCaptor<VoucherOrder> captor = ArgumentCaptor.forClass(VoucherOrder.class);
        verify(orderService).updateById(captor.capture());
        assertEquals(3, captor.getValue().getStatus());
    }

    @Test
    void redeemVoucherOrderRejectsUnpaidOrder() {
        VoucherOrder order = new VoucherOrder();
        order.setId(123456L);
        order.setUserId(9002L);
        order.setVoucherId(9001L);
        order.setStatus(1);
        org.mockito.Mockito.doReturn(order).when(orderService).getById(123456L);

        assertThrows(BizException.class, () -> orderService.redeemVoucherOrder(123456L));
    }

    @Test
    void cancelVoucherOrderChangesUnpaidOrderToCanceled() {
        VoucherOrder order = new VoucherOrder();
        order.setId(123456L);
        order.setUserId(9002L);
        order.setVoucherId(9001L);
        order.setStatus(1);
        org.mockito.Mockito.doReturn(order).when(orderService).getById(123456L);
        org.mockito.Mockito.doReturn(true).when(orderService).updateById(any(VoucherOrder.class));

        Result result = orderService.cancelVoucherOrder(123456L);

        Map<?, ?> data = (Map<?, ?>) result.getData();
        assertEquals(4, data.get("status"));
        assertEquals("已取消", data.get("statusText"));
    }

    @Test
    void requestRefundChangesPaidOrderToRefunding() {
        VoucherOrder order = new VoucherOrder();
        order.setId(123456L);
        order.setUserId(9002L);
        order.setVoucherId(9001L);
        order.setStatus(2);
        org.mockito.Mockito.doReturn(order).when(orderService).getById(123456L);
        org.mockito.Mockito.doReturn(true).when(orderService).updateById(any(VoucherOrder.class));

        Result result = orderService.requestRefund(123456L);

        Map<?, ?> data = (Map<?, ?>) result.getData();
        assertEquals(5, data.get("status"));
        assertEquals("退款中", data.get("statusText"));
    }

    @Test
    void approveRefundChangesRefundingOrderToRefunded() {
        VoucherOrder order = new VoucherOrder();
        order.setId(123456L);
        order.setUserId(9002L);
        order.setVoucherId(9001L);
        order.setStatus(5);
        org.mockito.Mockito.doReturn(order).when(orderService).getById(123456L);
        org.mockito.Mockito.doReturn(true).when(orderService).updateById(any(VoucherOrder.class));

        Result result = orderService.approveRefund(123456L);

        Map<?, ?> data = (Map<?, ?>) result.getData();
        assertEquals(6, data.get("status"));
        assertEquals("已退款", data.get("statusText"));
    }

    @Test
    void rejectRefundChangesRefundingOrderBackToPaid() {
        VoucherOrder order = new VoucherOrder();
        order.setId(123456L);
        order.setUserId(9002L);
        order.setVoucherId(9001L);
        order.setStatus(5);
        org.mockito.Mockito.doReturn(order).when(orderService).getById(123456L);
        org.mockito.Mockito.doReturn(true).when(orderService).updateById(any(VoucherOrder.class));

        Result result = orderService.rejectRefund(123456L);

        Map<?, ?> data = (Map<?, ?>) result.getData();
        assertEquals(2, data.get("status"));
        assertEquals("已支付", data.get("statusText"));
    }

    @Test
    void cancelExpiredUnpaidOrdersReturnsCanceledCount() {
        VoucherOrder expired = new VoucherOrder();
        expired.setId(123456L);
        expired.setUserId(9002L);
        expired.setVoucherId(9001L);
        expired.setStatus(1);
        expired.setCreateTime(LocalDateTime.now().minusMinutes(20));
        org.mockito.Mockito.doReturn(List.of(expired)).when(orderService).list(any());
        org.mockito.Mockito.doReturn(true).when(orderService).updateBatchById(any());

        Result result = orderService.cancelExpiredUnpaidOrders();

        Map<?, ?> data = (Map<?, ?>) result.getData();
        assertEquals(1, data.get("canceled"));
    }
}
