package com.hmdp.dto.admin;

import lombok.Data;

@Data
public class AdminVoucherStatusUpdateDTO {
    /**
     * 0-下架，1-生效
     */
    private Integer status;
}
