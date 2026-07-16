package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.InventoryLedger;
import com.hmdp.mapper.InventoryLedgerMapper;
import com.hmdp.service.IInventoryLedgerService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class InventoryLedgerServiceImpl extends ServiceImpl<InventoryLedgerMapper, InventoryLedger> implements IInventoryLedgerService {

    @Override
    public void record(Long campaignId, Long voucherId, Long orderId, String changeType, Integer changeAmount, String source, String detail) {
        InventoryLedger ledger = new InventoryLedger()
                .setCampaignId(campaignId)
                .setVoucherId(voucherId)
                .setOrderId(orderId)
                .setChangeType(changeType)
                .setChangeAmount(changeAmount)
                .setSource(source)
                .setDetail(detail)
                .setCreateTime(LocalDateTime.now());
        try {
            save(ledger);
        } catch (Exception ignored) {
        }
    }

    @Override
    public Result queryLedgers(Integer page, Integer size, Long campaignId, Long voucherId, Long orderId, String changeType) {
        Page<InventoryLedger> result = query()
                .eq(campaignId != null, "campaign_id", campaignId)
                .eq(voucherId != null, "voucher_id", voucherId)
                .eq(orderId != null, "order_id", orderId)
                .eq(StringUtils.hasText(changeType), "change_type", changeType)
                .orderByDesc("create_time")
                .page(new Page<>(page == null ? 1 : page, size == null ? 10 : size));
        return Result.ok(result.getRecords(), result.getTotal());
    }
}
