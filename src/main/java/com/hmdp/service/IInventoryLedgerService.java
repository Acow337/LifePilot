package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.InventoryLedger;

public interface IInventoryLedgerService extends IService<InventoryLedger> {

    void record(Long campaignId, Long voucherId, Long orderId, String changeType, Integer changeAmount, String source, String detail);

    Result queryLedgers(Integer page, Integer size, Long campaignId, Long voucherId, Long orderId, String changeType);
}
