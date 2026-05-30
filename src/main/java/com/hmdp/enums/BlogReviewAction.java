package com.hmdp.enums;

public enum BlogReviewAction {
    APPROVE,
    REJECT,
    OFFLINE;

    public static BlogReviewAction from(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        for (BlogReviewAction action : BlogReviewAction.values()) {
            if (action.name().equalsIgnoreCase(value.trim())) {
                return action;
            }
        }
        return null;
    }
}
