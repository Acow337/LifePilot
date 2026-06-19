from __future__ import annotations

import json

from langchain_core.tools import tool

from .api_client import BackendClient
from .knowledge_base import LocalKnowledgeBase



def build_tools(api_client: BackendClient, kb: LocalKnowledgeBase):
    @tool
    def query_shop_detail(shop_id: int) -> str:
        """查询店铺详情。输入 shop_id（int）"""
        try:
            payload = api_client.get_shop_detail(shop_id)
            return json.dumps(payload, ensure_ascii=False)
        except Exception as exc:
            return f"查询店铺失败: {exc}"

    @tool
    def query_shop_vouchers(shop_id: int) -> str:
        """查询店铺优惠券列表。输入 shop_id（int）"""
        try:
            payload = api_client.get_voucher_list(shop_id)
            return json.dumps(payload, ensure_ascii=False)
        except Exception as exc:
            return f"查询优惠券失败: {exc}"

    @tool
    def query_seckill_order_status(order_id: int) -> str:
        """查询秒杀订单状态。输入 order_id（int）"""
        try:
            payload = api_client.get_seckill_order_status(order_id)
            return json.dumps(payload, ensure_ascii=False)
        except Exception as exc:
            return f"查询订单状态失败: {exc}"

    @tool
    def query_local_knowledge(question: str) -> str:
        """从本地知识库检索运营规则、文档说明。输入问题文本。"""
        docs = kb.search(question)
        if not docs:
            return "知识库暂无相关内容"

        pieces: list[str] = []
        for idx, doc in enumerate(docs, start=1):
            source = doc.metadata.get("source", "unknown")
            chunk = doc.metadata.get("chunk", "?")
            pieces.append(f"[{idx}] source={source}#chunk{chunk}\n{doc.page_content[:500]}")
        return "\n\n".join(pieces)

    return [
        query_shop_detail,
        query_shop_vouchers,
        query_seckill_order_status,
        query_local_knowledge,
    ]
