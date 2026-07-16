package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.OrderStateLog;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.enums.ErrorCode;
import com.hmdp.exception.BizException;
import com.hmdp.mapper.OrderStateLogMapper;
import com.hmdp.service.IOrderStateLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderStateLogServiceImpl extends ServiceImpl<OrderStateLogMapper, OrderStateLog> implements IOrderStateLogService {

    @Override
    public void record(VoucherOrder order, String action, Integer fromStatus, Integer toStatus, String operatorType, Long operatorId, String message) {
        if (order == null || order.getId() == null || toStatus == null) {
            return;
        }
        OrderStateLog log = new OrderStateLog()
                .setOrderId(order.getId())
                .setUserId(order.getUserId())
                .setVoucherId(order.getVoucherId())
                .setFromStatus(fromStatus)
                .setToStatus(toStatus)
                .setAction(action)
                .setOperatorType(operatorType == null ? "system" : operatorType)
                .setOperatorId(operatorId)
                .setMessage(message)
                .setCreateTime(LocalDateTime.now());
        try {
            save(log);
        } catch (Exception ignored) {
        }
    }

    @Override
    public Result queryOrderTimeline(Long orderId) {
        if (orderId == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "订单号不能为空");
        }
        List<OrderStateLog> logs = query()
                .eq("order_id", orderId)
                .orderByAsc("create_time")
                .list();
        List<Map<String, Object>> rows = logs.stream().map(this::toRow).collect(Collectors.toList());
        return Result.ok(rows);
    }

    private Map<String, Object> toRow(OrderStateLog log) {
        Map<String, Object> row = new HashMap<>(10);
        row.put("id", log.getId());
        row.put("orderId", log.getOrderId());
        row.put("action", log.getAction());
        row.put("fromStatus", log.getFromStatus());
        row.put("fromStatusText", toOrderStatusText(log.getFromStatus()));
        row.put("toStatus", log.getToStatus());
        row.put("toStatusText", toOrderStatusText(log.getToStatus()));
        row.put("operatorType", log.getOperatorType());
        row.put("message", log.getMessage());
        row.put("createTime", log.getCreateTime());
        return row;
    }

    private String toOrderStatusText(Integer status) {
        if (status == null) {
            return "起始";
        }
        switch (status) {
            case 1:
                return "待支付";
            case 2:
                return "待核销";
            case 3:
                return "已核销";
            case 4:
                return "已关闭";
            case 5:
                return "退款中";
            case 6:
                return "已退款";
            default:
                return "异常单";
        }
    }
}
