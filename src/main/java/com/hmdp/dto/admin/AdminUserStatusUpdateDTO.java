package com.hmdp.dto.admin;

import lombok.Data;

@Data
public class AdminUserStatusUpdateDTO {
    /**
     * 0-正常，1-封禁
     */
    private Integer status;
}
