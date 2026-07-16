package com.hmdp.service.impl;

import com.hmdp.entity.Blog;
import com.hmdp.entity.Shop;
import com.hmdp.entity.User;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IAgentTraceService;
import com.hmdp.service.ICampaignService;
import com.hmdp.service.IInventoryLedgerService;
import com.hmdp.service.IShopService;
import com.hmdp.service.IUserService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.service.IVoucherService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminDashboardService {

    @Resource
    private IVoucherOrderService voucherOrderService;

    @Resource
    private IVoucherService voucherService;

    @Resource
    private IShopService shopService;

    @Resource
    private IUserService userService;

    @Resource
    private IBlogService blogService;

    @Resource
    private ICampaignService campaignService;

    @Resource
    private IInventoryLedgerService inventoryLedgerService;

    @Resource
    private IAgentTraceService agentTraceService;

    public Map<String, Object> queryDashboard() {
        Map<String, Object> data = new HashMap<>(20);
        long totalOrders = voucherOrderService.count();
        long paidOrders = countOrdersByStatus(2);
        long usedOrders = countOrdersByStatus(3);
        long canceledOrders = countOrdersByStatus(4);
        long refundingOrders = countOrdersByStatus(5);
        long refundedOrders = countOrdersByStatus(6);
        long todayOrders = voucherOrderService.count(voucherOrderService.query()
                .ge("create_time", LocalDate.now().atStartOfDay())
                .getWrapper());

        data.put("totalOrders", totalOrders);
        data.put("paidOrders", paidOrders);
        data.put("usedOrders", usedOrders);
        data.put("canceledOrders", canceledOrders);
        data.put("refundingOrders", refundingOrders);
        data.put("refundedOrders", refundedOrders);
        data.put("todayOrders", todayOrders);
        data.put("gmv", calculateGmv());
        data.put("refundRate", ratio(refundedOrders + refundingOrders, totalOrders));
        data.put("conversionRate", ratio(paidOrders + usedOrders + refundingOrders + refundedOrders, totalOrders));
        data.put("entities", queryEntityTotals());
        data.put("statusDistribution", queryStatusDistribution());
        data.put("recentOrders", queryRecentOrders());
        data.put("opsMetrics", queryOpsMetrics(totalOrders, refundingOrders));
        data.put("copilotInsights", queryCopilotInsights(totalOrders, refundingOrders, canceledOrders));
        return data;
    }

    private long countOrdersByStatus(int status) {
        return voucherOrderService.count(voucherOrderService.query().eq("status", status).getWrapper());
    }

    private long calculateGmv() {
        List<VoucherOrder> orders = voucherOrderService.list(voucherOrderService.query()
                .in("status", 2, 3, 5)
                .getWrapper());
        long gmv = 0L;
        for (VoucherOrder order : orders) {
            Voucher voucher = voucherService.getById(order.getVoucherId());
            if (voucher != null && voucher.getPayValue() != null) {
                gmv += voucher.getPayValue();
            }
        }
        return gmv;
    }

    private Map<String, Object> queryEntityTotals() {
        Map<String, Object> entities = new HashMap<>(7);
        entities.put("users", userService.count());
        entities.put("shops", shopService.count());
        entities.put("vouchers", voucherService.count());
        entities.put("blogs", blogService.count());
        entities.put("campaigns", campaignService.count());
        entities.put("inventoryLedgers", inventoryLedgerService.count());
        entities.put("agentTraces", agentTraceService.count());
        return entities;
    }

    private Map<String, Object> queryOpsMetrics(long totalOrders, long refundingOrders) {
        Map<String, Object> metrics = new HashMap<>(8);
        long activeCampaigns = campaignService.count(campaignService.query().eq("status", 1).getWrapper());
        long inventoryEvents = inventoryLedgerService.count();
        long agentTurns = agentTraceService.count();
        long agentFailures = agentTraceService.count(agentTraceService.query().isNotNull("error_code").getWrapper());
        metrics.put("activeCampaigns", activeCampaigns);
        metrics.put("inventoryEvents", inventoryEvents);
        metrics.put("agentTurns", agentTurns);
        metrics.put("agentFailureRate", ratio(agentFailures, agentTurns));
        metrics.put("pendingRefunds", refundingOrders);
        metrics.put("abnormalOrders", countAbnormalOrders(totalOrders));
        return metrics;
    }

    private List<String> queryCopilotInsights(long totalOrders, long refundingOrders, long canceledOrders) {
        List<String> insights = new ArrayList<>();
        if (campaignService.count() == 0) {
            insights.add("建议创建首个 Campaign，将存量优惠券纳入统一营销活动视图。");
        }
        if (refundingOrders > 0) {
            insights.add("当前存在退款中订单，建议运营优先审核，降低用户等待时长。");
        }
        if (ratio(canceledOrders, totalOrders).compareTo(BigDecimal.valueOf(20)) > 0) {
            insights.add("关闭订单占比较高，可检查支付引导或活动库存承诺是否清晰。");
        }
        if (agentTraceService.count() == 0) {
            insights.add("建议开启 Agent Trace 上报，沉淀客服意图、工具调用和失败原因。");
        }
        if (insights.isEmpty()) {
            insights.add("运营链路运行平稳，可继续观察活动转化率、退款率和客服解决率。");
        }
        return insights;
    }

    private long countAbnormalOrders(long totalOrders) {
        return Math.max(0, totalOrders - countOrdersByStatus(1) - countOrdersByStatus(2) - countOrdersByStatus(3)
                - countOrdersByStatus(4) - countOrdersByStatus(5) - countOrdersByStatus(6));
    }

    private List<Map<String, Object>> queryStatusDistribution() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(statusRow(1, "未支付", countOrdersByStatus(1)));
        rows.add(statusRow(2, "已支付", countOrdersByStatus(2)));
        rows.add(statusRow(3, "已核销", countOrdersByStatus(3)));
        rows.add(statusRow(4, "已取消", countOrdersByStatus(4)));
        rows.add(statusRow(5, "退款中", countOrdersByStatus(5)));
        rows.add(statusRow(6, "已退款", countOrdersByStatus(6)));
        return rows;
    }

    private Map<String, Object> statusRow(int status, String label, long count) {
        Map<String, Object> row = new HashMap<>(3);
        row.put("status", status);
        row.put("label", label);
        row.put("count", count);
        return row;
    }

    private List<Map<String, Object>> queryRecentOrders() {
        List<VoucherOrder> orders = voucherOrderService.list(voucherOrderService.query()
                .orderByDesc("create_time")
                .last("limit 8")
                .getWrapper());
        List<Map<String, Object>> rows = new ArrayList<>(orders.size());
        for (VoucherOrder order : orders) {
            Map<String, Object> row = new HashMap<>(8);
            row.put("id", order.getId());
            row.put("userId", order.getUserId());
            row.put("voucherId", order.getVoucherId());
            row.put("status", order.getStatus());
            row.put("statusText", toStatusText(order.getStatus()));
            row.put("createTime", order.getCreateTime());
            Voucher voucher = voucherService.getById(order.getVoucherId());
            row.put("voucherTitle", voucher == null ? "优惠券" : voucher.getTitle());
            row.put("payValue", voucher == null ? 0L : voucher.getPayValue());
            rows.add(row);
        }
        return rows;
    }

    private BigDecimal ratio(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private String toStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 1:
                return "未支付";
            case 2:
                return "已支付";
            case 3:
                return "已核销";
            case 4:
                return "已取消";
            case 5:
                return "退款中";
            case 6:
                return "已退款";
            default:
                return "未知";
        }
    }
}
