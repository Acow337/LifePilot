package com.hmdp.dto.admin;

import lombok.Data;

@Data
public class AdminUserRoleUpdateDTO {
    /**
     * 0-普通用户，1-管理员
     */
    private Integer role;
}
