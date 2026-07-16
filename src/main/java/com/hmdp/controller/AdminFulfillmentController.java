package com.hmdp.controller;

import com.hmdp.dto.Result;
import com.hmdp.service.IInventoryLedgerService;
import com.hmdp.service.IOrderStateLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/admin/fulfillment")
public class AdminFulfillmentController {

    @Resource
    private IOrderStateLogService orderStateLogService;

    @Resource
    private IInventoryLedgerService inventoryLedgerService;

    @GetMapping("/orders/{id}/timeline")
    public Result queryOrderTimeline(@PathVariable("id") Long orderId) {
        return orderStateLogService.queryOrderTimeline(orderId);
    }

    @GetMapping("/inventory-ledgers")
    public Result queryInventoryLedgers(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "campaignId", required = false) Long campaignId,
            @RequestParam(value = "voucherId", required = false) Long voucherId,
            @RequestParam(value = "orderId", required = false) Long orderId,
            @RequestParam(value = "changeType", required = false) String changeType
    ) {
        return inventoryLedgerService.queryLedgers(page, size, campaignId, voucherId, orderId, changeType);
    }
}
