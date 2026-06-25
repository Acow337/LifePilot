from __future__ import annotations

from enum import Enum


class Intent(str, Enum):
    SHOP = "shop"
    VOUCHER = "voucher"
    ORDER = "order"
    REFUND = "refund"
    RULE = "rule"
    FALLBACK = "fallback"


INTENT_KEYWORDS: list[tuple[Intent, tuple[str, ...]]] = [
    (Intent.REFUND, ("退款", "取消", "售后", "退单", "退货", "撤销")),
    (Intent.ORDER, ("订单", "下单", "支付状态", "秒杀状态", "订单状态", "支付", "抢购结果")),
    (Intent.VOUCHER, ("优惠券", "券", "折扣", "秒杀券", "代金券", "满减")),
    (Intent.SHOP, ("店铺", "商家", "地址", "营业", "评分", "人均", "电话")),
    (Intent.RULE, ("规则", "怎么登录", "怎么发布", "限制", "faq", "FAQ", "说明", "流程")),
]


def detect_intent(message: str) -> Intent:
    normalized = message.strip()
    if not normalized:
        return Intent.FALLBACK

    for intent, keywords in INTENT_KEYWORDS:
        if any(keyword in normalized for keyword in keywords):
            return intent
    return Intent.FALLBACK
