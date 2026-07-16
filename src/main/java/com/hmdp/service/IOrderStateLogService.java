package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.OrderStateLog;
import com.hmdp.entity.VoucherOrder;

public interface IOrderStateLogService extends IService<OrderStateLog> {

    void record(VoucherOrder order, String action, Integer fromStatus, Integer toStatus, String operatorType, Long operatorId, String message);

    Result queryOrderTimeline(Long orderId);
}
