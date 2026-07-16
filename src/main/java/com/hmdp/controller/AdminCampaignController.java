package com.hmdp.controller;

import com.hmdp.annotation.AdminActionLog;
import com.hmdp.dto.Result;
import com.hmdp.dto.admin.AdminCampaignStatusUpdateDTO;
import com.hmdp.entity.Campaign;
import com.hmdp.service.ICampaignService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/admin/campaigns")
public class AdminCampaignController {

    @Resource
    private ICampaignService campaignService;

    @GetMapping
    public Result queryCampaigns(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "shopId", required = false) Long shopId,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        return campaignService.queryAdminCampaigns(page, size, status, type, shopId, keyword);
    }

    @GetMapping("/{id}")
    public Result queryCampaignDetail(@PathVariable("id") Long campaignId) {
        return campaignService.queryCampaignDetail(campaignId);
    }

    @GetMapping("/{id}/rules")
    public Result queryCampaignRules(@PathVariable("id") Long campaignId) {
        return campaignService.queryCampaignRules(campaignId);
    }

    @AdminActionLog(module = "campaign", action = "create", targetType = "campaign", targetId = "#result.data")
    @PostMapping
    public Result createCampaign(@RequestBody Campaign campaign) {
        return campaignService.createCampaign(campaign);
    }

    @AdminActionLog(module = "campaign", action = "update", targetType = "campaign", targetId = "#campaignId")
    @PutMapping("/{id}")
    public Result updateCampaign(@PathVariable("id") Long campaignId, @RequestBody Campaign campaign) {
        return campaignService.updateCampaign(campaignId, campaign);
    }

    @AdminActionLog(module = "campaign", action = "update_status", targetType = "campaign", targetId = "#campaignId")
    @PatchMapping("/{id}/status")
    public Result updateCampaignStatus(@PathVariable("id") Long campaignId,
                                       @RequestBody AdminCampaignStatusUpdateDTO request) {
        return campaignService.updateCampaignStatus(campaignId, request == null ? null : request.getStatus());
    }
}
