package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Campaign;
import com.hmdp.entity.CampaignRule;
import com.hmdp.entity.Shop;
import com.hmdp.entity.Voucher;
import com.hmdp.enums.ErrorCode;
import com.hmdp.exception.BizException;
import com.hmdp.mapper.CampaignMapper;
import com.hmdp.service.ICampaignService;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IShopService;
import com.hmdp.service.IVoucherService;
import com.hmdp.mapper.CampaignRuleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CampaignServiceImpl extends ServiceImpl<CampaignMapper, Campaign> implements ICampaignService {

    @Resource
    private IVoucherService voucherService;

    @Resource
    private IShopService shopService;

    @Resource
    private CampaignRuleMapper campaignRuleMapper;

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Override
    public Result queryAdminCampaigns(Integer page, Integer size, Integer status, String type, Long shopId, String keyword) {
        bootstrapCampaignsFromVouchers();
        Page<Campaign> result = query()
                .eq(status != null, "status", status)
                .eq(StringUtils.hasText(type), "type", type)
                .eq(shopId != null, "shop_id", shopId)
                .like(StringUtils.hasText(keyword), "name", keyword)
                .orderByDesc("create_time")
                .page(new Page<>(page == null ? 1 : page, size == null ? 10 : size));
        List<Map<String, Object>> rows = new ArrayList<>(result.getRecords().size());
        for (Campaign campaign : result.getRecords()) {
            rows.add(toCampaignRow(campaign));
        }
        return Result.ok(rows, result.getTotal());
    }

    @Override
    @Transactional
    public Result createCampaign(Campaign campaign) {
        validateCampaign(campaign);
        campaign.setId(null);
        campaign.setCreateTime(LocalDateTime.now());
        campaign.setUpdateTime(LocalDateTime.now());
        if (campaign.getStatus() == null) {
            campaign.setStatus(1);
        }
        if (!StringUtils.hasText(campaign.getType())) {
            campaign.setType("SECKILL");
        }
        if (campaign.getStockAvailable() == null) {
            campaign.setStockAvailable(campaign.getStockTotal() == null ? 0 : campaign.getStockTotal());
        }
        save(campaign);
        return Result.ok(campaign.getId());
    }

    @Override
    @Transactional
    public Result updateCampaign(Long campaignId, Campaign campaign) {
        Campaign old = requireCampaign(campaignId);
        validateCampaign(campaign);
        campaign.setId(campaignId);
        campaign.setCreateTime(old.getCreateTime());
        campaign.setUpdateTime(LocalDateTime.now());
        updateById(campaign);
        return Result.ok();
    }

    @Override
    @Transactional
    public Result updateCampaignStatus(Long campaignId, Integer status) {
        if (status == null || status < 0 || status > 3) {
            throw new BizException(ErrorCode.BAD_REQUEST, "活动状态不合法");
        }
        requireCampaign(campaignId);
        update().eq("id", campaignId).set("status", status).set("update_time", LocalDateTime.now()).update();
        return Result.ok();
    }

    @Override
    public Result queryCampaignDetail(Long campaignId) {
        return Result.ok(toCampaignRow(requireCampaign(campaignId)));
    }

    @Override
    public Result queryCampaignRules(Long campaignId) {
        requireCampaign(campaignId);
        List<CampaignRule> rules = campaignRuleMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CampaignRule>()
                .eq("campaign_id", campaignId)
                .orderByAsc("id"));
        return Result.ok(rules);
    }

    private Campaign requireCampaign(Long campaignId) {
        if (campaignId == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "活动ID不能为空");
        }
        Campaign campaign = getById(campaignId);
        if (campaign == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "活动不存在");
        }
        return campaign;
    }

    private void validateCampaign(Campaign campaign) {
        if (campaign == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "活动参数不能为空");
        }
        if (!StringUtils.hasText(campaign.getName())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "活动名称不能为空");
        }
        if (campaign.getShopId() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "活动店铺不能为空");
        }
        if (campaign.getStockTotal() == null || campaign.getStockTotal() < 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "活动库存不合法");
        }
        if (campaign.getBeginTime() != null && campaign.getEndTime() != null && campaign.getEndTime().isBefore(campaign.getBeginTime())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "活动结束时间不能早于开始时间");
        }
    }

    private Map<String, Object> toCampaignRow(Campaign campaign) {
        Map<String, Object> row = new HashMap<>(20);
        row.put("id", campaign.getId());
        row.put("voucherId", campaign.getVoucherId());
        row.put("shopId", campaign.getShopId());
        row.put("name", campaign.getName());
        row.put("type", campaign.getType());
        row.put("typeText", toTypeText(campaign.getType()));
        row.put("status", campaign.getStatus());
        row.put("statusText", toStatusText(campaign.getStatus()));
        row.put("stockTotal", campaign.getStockTotal());
        row.put("stockAvailable", campaign.getStockAvailable());
        row.put("budgetCent", campaign.getBudgetCent());
        row.put("beginTime", campaign.getBeginTime());
        row.put("endTime", campaign.getEndTime());
        row.put("description", campaign.getDescription());
        row.put("createTime", campaign.getCreateTime());
        Shop shop = shopService.getById(campaign.getShopId());
        row.put("shopName", shop == null ? "未知店铺" : shop.getName());
        if (campaign.getVoucherId() != null) {
            Voucher voucher = voucherService.getById(campaign.getVoucherId());
            row.put("voucherTitle", voucher == null ? null : voucher.getTitle());
            row.put("payValue", voucher == null ? null : voucher.getPayValue());
            row.put("actualValue", voucher == null ? null : voucher.getActualValue());
        }
        return row;
    }

    private void bootstrapCampaignsFromVouchers() {
        if (count() > 0) {
            return;
        }
        List<Voucher> vouchers = voucherService.list();
        if (vouchers == null || vouchers.isEmpty()) {
            return;
        }
        List<Campaign> campaigns = new ArrayList<>(vouchers.size());
        for (Voucher voucher : vouchers) {
            if (voucher == null || voucher.getId() == null || voucher.getShopId() == null) {
                continue;
            }
            Campaign campaign = new Campaign()
                    .setVoucherId(voucher.getId())
                    .setShopId(voucher.getShopId())
                    .setName(voucher.getTitle() == null ? "存量优惠券活动" : voucher.getTitle())
                    .setType(voucher.getType() != null && voucher.getType() == 1 ? "SECKILL" : "NORMAL")
                    .setStatus(voucher.getStatus() == null ? 1 : voucher.getStatus())
                    .setBudgetCent(voucher.getPayValue() == null ? 0L : voucher.getPayValue())
                    .setDescription(voucher.getRules() == null ? voucher.getSubTitle() : voucher.getRules())
                    .setCreateTime(LocalDateTime.now())
                    .setUpdateTime(LocalDateTime.now());
            com.hmdp.entity.SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucher.getId());
            int stock = seckillVoucher == null || seckillVoucher.getStock() == null ? 0 : seckillVoucher.getStock();
            campaign.setStockTotal(stock);
            campaign.setStockAvailable(stock);
            if (seckillVoucher != null) {
                campaign.setBeginTime(seckillVoucher.getBeginTime());
                campaign.setEndTime(seckillVoucher.getEndTime());
            }
            campaigns.add(campaign);
        }
        if (!campaigns.isEmpty()) {
            saveBatch(campaigns);
        }
    }

    private String toStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 0:
                return "草稿";
            case 1:
                return "上线";
            case 2:
                return "暂停";
            case 3:
                return "结束";
            default:
                return "未知";
        }
    }

    private String toTypeText(String type) {
        if ("NORMAL".equals(type)) {
            return "普通券";
        }
        if ("NEW_USER".equals(type)) {
            return "新人券";
        }
        if ("MEMBER".equals(type)) {
            return "会员券";
        }
        if ("FULL_REDUCTION".equals(type)) {
            return "满减券";
        }
        return "限时秒杀";
    }
}
