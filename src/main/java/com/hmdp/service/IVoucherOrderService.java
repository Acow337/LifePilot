package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result seckillVoucher(Long voucherId);

    Result createVoucherOrder(Long voucherId);

    Result querySeckillOrder(Long orderId);

    Result queryMyOrders();

    Result payVoucherOrder(Long orderId);

    Result redeemVoucherOrder(Long orderId);

    Result cancelVoucherOrder(Long orderId);

    Result requestRefund(Long orderId);

    Result approveRefund(Long orderId);

    Result rejectRefund(Long orderId);

    Result cancelExpiredUnpaidOrders();

}
