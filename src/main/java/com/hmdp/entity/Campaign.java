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
@TableName("tb_campaign")
public class Campaign implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long voucherId;

    private Long shopId;

    private String name;

    private String type;

    private Integer status;

    private Integer stockTotal;

    private Integer stockAvailable;

    private Long budgetCent;

    private LocalDateTime beginTime;

    private LocalDateTime endTime;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
