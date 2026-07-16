package com.hmdp.controller;

import com.hmdp.dto.Result;
import com.hmdp.entity.Campaign;
import com.hmdp.entity.Shop;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.ICampaignService;
import com.hmdp.service.IOrderStateLogService;
import com.hmdp.service.IShopService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.service.IVoucherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/agent/tools")
public class AgentToolController {

    @Resource
    private IVoucherOrderService voucherOrderService;

    @Resource
    private IVoucherService voucherService;

    @Resource
    private IShopService shopService;

    @Resource
    private ICampaignService campaignService;

    @Resource
    private IOrderStateLogService orderStateLogService;

    @GetMapping("/order/{id}")
    public Result queryOrderForAgent(@PathVariable("id") Long orderId) {
        VoucherOrder order = voucherOrderService.getById(orderId);
        if (order == null) {
            return Result.fail("订单不存在");
        }
        Map<String, Object> data = new HashMap<>(16);
        data.put("orderId", order.getId());
        data.put("userId", order.getUserId());
        data.put("voucherId", order.getVoucherId());
        data.put("status", order.getStatus());
        data.put("statusText", toOrderStatusText(order.getStatus()));
        data.put("createTime", order.getCreateTime());
        data.put("payTime", order.getPayTime());
        data.put("useTime", order.getUseTime());
        data.put("refundTime", order.getRefundTime());
        Voucher voucher = voucherService.getById(order.getVoucherId());
        if (voucher != null) {
            data.put("voucherTitle", voucher.getTitle());
            data.put("payValue", voucher.getPayValue());
            data.put("actualValue", voucher.getActualValue());
            data.put("shopId", voucher.getShopId());
            Shop shop = shopService.getById(voucher.getShopId());
            data.put("shopName", shop == null ? null : shop.getName());
        }
        data.put("timeline", orderStateLogService.queryOrderTimeline(orderId).getData());
        return Result.ok(data);
    }

    @GetMapping("/campaign/{id}")
    public Result queryCampaignForAgent(@PathVariable("id") Long campaignId) {
        return campaignService.queryCampaignDetail(campaignId);
    }

    @GetMapping("/refund-policy")
    public Result queryRefundPolicy() {
        Map<String, Object> data = new HashMap<>(8);
        data.put("title", "LifePilot 退款与核销规则");
        data.put("refundWindow", "订单支付后、核销前可申请退款；核销后原则上不支持退款。");
        data.put("reviewFlow", "用户提交退款申请后进入退款中，由运营后台审核通过或拒绝。");
        data.put("orderStates", "待支付 → 待核销 → 已核销 / 退款中 → 已退款 / 已关闭。");
        data.put("agentBoundary", "AI Agent 只提供查询、解释和处理建议，不直接执行退款写操作。");
        return Result.ok(data);
    }

    private String toOrderStatusText(Integer status) {
        if (status == null) {
            return "未知";
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
