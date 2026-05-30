package com.hmdp.dto.admin;

import lombok.Data;

@Data
public class AdminBlogReviewDTO {
    /**
     * APPROVE / REJECT / OFFLINE
     */
    private String action;

    /**
     * 审核备注（可选）
     */
    private String reason;
}
