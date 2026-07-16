package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.Campaign;

public interface ICampaignService extends IService<Campaign> {

    Result queryAdminCampaigns(Integer page, Integer size, Integer status, String type, Long shopId, String keyword);

    Result createCampaign(Campaign campaign);

    Result updateCampaign(Long campaignId, Campaign campaign);

    Result updateCampaignStatus(Long campaignId, Integer status);

    Result queryCampaignDetail(Long campaignId);

    Result queryCampaignRules(Long campaignId);
}
