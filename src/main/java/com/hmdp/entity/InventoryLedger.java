package com.hmdp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_inventory_ledger")
public class InventoryLedger implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long campaignId;

    private Long voucherId;

    private Long orderId;

    private String changeType;

    private Integer changeAmount;

    private Integer beforeStock;

    private Integer afterStock;

    private String source;

    private String detail;

    private LocalDateTime createTime;
}
