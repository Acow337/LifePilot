from __future__ import annotations

import json

import httpx
from langchain_core.tools import tool

from .api_client import BackendBusinessError, BackendClient
from .intent import Intent
from .knowledge_base import LocalKnowledgeBase


TOOL_POLICY: dict[Intent, tuple[str, ...]] = {
    Intent.SHOP: ("query_shop_detail", "query_local_knowledge"),
    Intent.VOUCHER: ("query_shop_detail", "query_shop_vouchers", "query_campaign_detail", "query_local_knowledge"),
    Intent.ORDER: ("query_order_fulfillment", "query_local_knowledge"),
    Intent.REFUND: ("query_order_fulfillment", "query_refund_policy", "query_local_knowledge"),
    Intent.RULE: ("query_refund_policy", "query_local_knowledge"),
    Intent.FALLBACK: ("query_local_knowledge",),
}


def make_tool_error(error_code: str, message: str) -> str:
    return json.dumps({"ok": False, "error_code": error_code, "message": message}, ensure_ascii=False)


def classify_tool_error(output: object) -> str | None:
    if not isinstance(output, str):
        return None
    try:
        payload = json.loads(output)
    except json.JSONDecodeError:
        return None
    if isinstance(payload, dict) and payload.get("ok") is False:
        error_code = payload.get("error_code")
        if isinstance(error_code, str):
            return error_code
    return None


def _format_tool_exception(exc: Exception) -> str:
    if isinstance(exc, httpx.TimeoutException):
        return make_tool_error("BACKEND_TIMEOUT", "后端服务响应超时，请稍后重试")
    if isinstance(exc, BackendBusinessError):
        return make_tool_error("BACKEND_ERROR", str(exc))
    if isinstance(exc, httpx.HTTPError):
        return make_tool_error("BACKEND_ERROR", "后端服务暂时不可用，请稍后重试")
    return make_tool_error("BACKEND_ERROR", "工具调用失败，请稍后重试")


def build_tools(api_client: BackendClient, kb: LocalKnowledgeBase, intent: Intent | str | None = None):
    @tool
    def query_shop_detail(shop_id: int) -> str:
        """查询店铺详情。输入 shop_id（int）"""
        try:
            payload = api_client.get_shop_detail(shop_id)
            return json.dumps(payload, ensure_ascii=False)
        except Exception as exc:
            return _format_tool_exception(exc)

    @tool
    def query_shop_vouchers(shop_id: int) -> str:
        """查询店铺优惠券列表。输入 shop_id（int）"""
        try:
            payload = api_client.get_voucher_list(shop_id)
            return json.dumps(payload, ensure_ascii=False)
        except Exception as exc:
            return _format_tool_exception(exc)

    @tool
    def query_seckill_order_status(order_id: int) -> str:
        """查询秒杀订单状态。输入 order_id（int）"""
        try:
            payload = api_client.get_seckill_order_status(order_id)
            return json.dumps(payload, ensure_ascii=False)
        except Exception as exc:
            return _format_tool_exception(exc)

    @tool
    def query_order_fulfillment(order_id: int) -> str:
        """查询订单履约详情和状态机时间线。输入 order_id（int）"""
        try:
            payload = api_client.get_agent_order(order_id)
            return json.dumps(payload, ensure_ascii=False)
        except Exception as exc:
            return _format_tool_exception(exc)

    @tool
    def query_campaign_detail(campaign_id: int) -> str:
        """查询营销活动详情。输入 campaign_id（int）"""
        try:
            payload = api_client.get_agent_campaign(campaign_id)
            return json.dumps(payload, ensure_ascii=False)
        except Exception as exc:
            return _format_tool_exception(exc)

    @tool
    def query_refund_policy() -> str:
        """查询平台退款、核销和 Agent 处理边界规则。无需输入。"""
        try:
            payload = api_client.get_refund_policy()
            return json.dumps(payload, ensure_ascii=False)
        except Exception as exc:
            return _format_tool_exception(exc)

    @tool
    def query_local_knowledge(question: str) -> str:
        """从本地知识库检索运营规则、文档说明。输入问题文本。"""
        results = kb.search_with_scores(question)
        if not results:
            return make_tool_error("KNOWLEDGE_EMPTY", "知识库暂无相关内容")

        pieces: list[str] = []
        for idx, result in enumerate(results, start=1):
            doc = result.document
            source = doc.metadata.get("source", "unknown")
            chunk = doc.metadata.get("chunk", "?")
            chunk_id = doc.metadata.get("chunk_id") or f"{source}#chunk{chunk}"
            section = doc.metadata.get("section", "")
            matched = ",".join(result.matched_keywords)
            pieces.append(
                f"[{idx}] source={source}#chunk{chunk} chunk_id={chunk_id} "
                f"section={section} confidence={result.confidence:.2f} matched={matched}\n"
                f"{doc.page_content[:500]}"
            )
        return "\n\n".join(pieces)

    tools = [
        query_shop_detail,
        query_shop_vouchers,
        query_seckill_order_status,
        query_order_fulfillment,
        query_campaign_detail,
        query_refund_policy,
        query_local_knowledge,
    ]
    if intent is None:
        return tools

    resolved_intent = intent if isinstance(intent, Intent) else Intent(str(intent))
    allowed = set(TOOL_POLICY[resolved_intent])
    return [candidate for candidate in tools if candidate.name in allowed]
