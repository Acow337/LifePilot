from __future__ import annotations

from collections import defaultdict
import json
import logging
import re
import time
from typing import Dict, Union
import uuid

from fastapi import FastAPI, HTTPException
from langchain_community.chat_message_histories import ChatMessageHistory

from .api_client import BackendClient
from .chains import build_agent_executor
from .config import get_settings
from .intent import detect_intent
from .knowledge_base import LocalKnowledgeBase
from .models import ChatCard, ChatRequest, ChatResponse, HealthResponse
from .task_state import PendingTaskStore, SlotStatus, build_slot_status
from .tools import build_tools, classify_tool_error

app = FastAPI(title="LangChain Customer Service Bot", version="0.1.0")

settings = get_settings()
kb = LocalKnowledgeBase(settings.knowledge_glob)
kb_chunks = kb.rebuild()
session_history: dict[str, ChatMessageHistory] = defaultdict(ChatMessageHistory)
pending_tasks = PendingTaskStore()
MAX_HISTORY_MESSAGES = settings.max_history_messages
DEFAULT_SUGGESTIONS = ["查店铺优惠", "秒杀订单状态", "平台规则", "转人工"]
FALLBACK_MESSAGES = {
    "MODEL_NOT_CONFIGURED": "智能客服模型还没有配置完成，但我可以先帮你定位常见问题。",
    "MODEL_ERROR": "智能客服暂时开小差了，请稍后重试。",
    "BACKEND_ERROR": "业务服务暂时不可用，请稍后重试或联系人工客服。",
    "BACKEND_TIMEOUT": "业务服务响应超时，请稍后重试。",
    "KNOWLEDGE_EMPTY": "我暂时没有找到相关规则说明，可以换个问法或联系人工客服。",
    "UNSUPPORTED_INTENT": "这个问题暂时超出智能客服能力范围，建议联系人工客服。",
}
logger = logging.getLogger("cs_bot.chat")


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    return HealthResponse(status="ok")


@app.post("/reindex")
def reindex_knowledge() -> dict[str, int]:
    chunks = kb.rebuild()
    return {"chunks": chunks}


@app.post("/chat", response_model=ChatResponse)
def chat(payload: ChatRequest) -> ChatResponse:
    trace_id = uuid.uuid4().hex
    started_at = time.monotonic()
    intent = detect_intent(payload.message)
    used_tools: list[str] = []
    error_code: str | None = None

    resumed_status = pending_tasks.resume_if_possible(payload.session_id, payload.message, current_intent=intent)
    if resumed_status:
        intent = resumed_status.intent
        if not resumed_status.is_complete:
            response = build_slot_follow_up_response(resumed_status, trace_id)
            log_chat_turn(payload, trace_id, intent.value, used_tools, started_at, response.error_code, True)
            return response
    else:
        slot_status = build_slot_status(intent, payload.message)
        if not slot_status.is_complete:
            pending_tasks.save(payload.session_id, slot_status)
            response = build_slot_follow_up_response(slot_status, trace_id)
            log_chat_turn(payload, trace_id, intent.value, used_tools, started_at, response.error_code, True)
            return response

    if not settings.openai_api_key:
        response = build_fallback_response("MODEL_NOT_CONFIGURED", intent=intent.value, trace_id=trace_id)
        log_chat_turn(payload, trace_id, intent.value, used_tools, started_at, response.error_code, True)
        return response

    api_client = BackendClient(
        base_url=settings.backend_base_url,
        timeout_seconds=settings.backend_timeout_seconds,
        token=payload.user_token,
    )
    tools = build_tools(api_client, kb, intent)
    executor = build_agent_executor(
        llm_model=settings.llm_model,
        openai_api_key=settings.openai_api_key,
        openai_base_url=settings.openai_base_url,
        tools=tools,
        reasoning_effort=settings.llm_reasoning_effort,
        thinking_enabled=settings.llm_thinking_enabled,
        intent=intent.value,
    )

    history = session_history[payload.session_id]

    try:
        result = executor.invoke(
            {
                "input": payload.message,
                "chat_history": history.messages,
            }
        )
    except Exception:
        response = build_fallback_response("MODEL_ERROR", intent=intent.value, trace_id=trace_id)
        log_chat_turn(payload, trace_id, intent.value, used_tools, started_at, response.error_code, True)
        return response

    answer = str(result.get("output", ""))
    append_to_history(history, payload.message, answer, MAX_HISTORY_MESSAGES)

    for step in result.get("intermediate_steps", []):
        if isinstance(step, tuple) and len(step) == 2:
            action = step[0]
            tool_name = getattr(action, "tool", None)
            if tool_name:
                used_tools.append(tool_name)
            step_error = classify_tool_error(step[1])
            if step_error and not error_code:
                error_code = step_error

    response = ChatResponse(
        answer=answer,
        used_tools=used_tools,
        suggestions=build_suggestions(payload.message, used_tools, intent=intent.value),
        cards=build_cards_from_steps(result.get("intermediate_steps", [])),
        intent=intent.value,
        trace_id=trace_id,
        error_code=error_code,
    )
    log_chat_turn(payload, trace_id, intent.value, used_tools, started_at, error_code, False)
    return response


def build_fallback_response(error_code: str, intent: str | None = None, trace_id: str | None = None) -> ChatResponse:
    normalized_error_code = error_code if error_code in FALLBACK_MESSAGES else "MODEL_ERROR"
    answer = FALLBACK_MESSAGES[normalized_error_code]
    if trace_id:
        answer += f"\n\n追踪编号：{trace_id}"
    return ChatResponse(
        answer=answer,
        suggestions=build_suggestions("", [], intent=intent),
        intent=intent,
        trace_id=trace_id,
        error_code=normalized_error_code,
    )


def build_slot_follow_up_response(status: SlotStatus, trace_id: str | None = None) -> ChatResponse:
    answer = status.follow_up or "请补充必要信息后我再帮你查询。"
    if trace_id:
        answer += f"\n\n追踪编号：{trace_id}"
    return ChatResponse(
        answer=answer,
        suggestions=build_suggestions("", [], intent=status.intent.value),
        intent=status.intent.value,
        trace_id=trace_id,
        error_code="MISSING_SLOT",
    )


def build_suggestions(message: str, used_tools: list[str], intent: str | None = None) -> list[str]:
    suggestions: list[str] = []
    text = message.lower()
    if intent == "refund":
        suggestions.extend(["秒杀订单状态", "退款规则", "转人工"])
    if intent == "order" or "订单" in message or "秒杀" in message or "query_seckill_order_status" in used_tools:
        suggestions.extend(["秒杀订单状态", "秒杀失败怎么办"])
    if intent == "voucher" or "店" in message or "优惠" in message or "券" in message or "query_shop_vouchers" in used_tools:
        suggestions.extend(["查店铺优惠", "优惠券怎么使用"])
    if intent == "rule" or "规则" in message or "登录" in message or "评论" in message or "query_local_knowledge" in used_tools:
        suggestions.extend(["平台规则", "如何发布探店笔记"])
    if not suggestions or "help" in text:
        suggestions.extend(DEFAULT_SUGGESTIONS)
    return list(dict.fromkeys(suggestions))[:4]


def log_chat_turn(
    payload: ChatRequest,
    trace_id: str,
    intent: str,
    used_tools: list[str],
    started_at: float,
    error_code: str | None,
    fallback: bool,
) -> None:
    duration_ms = int((time.monotonic() - started_at) * 1000)
    logger.info(
        json.dumps(
            {
                "trace_id": trace_id,
                "session_id": payload.session_id,
                "user_id": payload.user_id,
                "has_user_token": bool(payload.user_token),
                "intent": intent,
                "used_tools": used_tools,
                "duration_ms": duration_ms,
                "error_code": error_code,
                "fallback": fallback,
            },
            ensure_ascii=False,
        )
    )


def build_cards_from_steps(intermediate_steps) -> list[ChatCard]:
    cards: list[ChatCard] = []
    seen: set[tuple[str, str]] = set()
    for step in intermediate_steps or []:
        if not (isinstance(step, tuple) and len(step) == 2):
            continue
        action, output = step
        tool_name = getattr(action, "tool", "")
        text = str(output)
        new_cards: list[ChatCard] = []
        if tool_name == "query_shop_detail":
            new_cards = _cards_from_shop_output(text)
        elif tool_name == "query_shop_vouchers":
            new_cards = _cards_from_voucher_output(text)
        elif tool_name == "query_local_knowledge":
            new_cards = _cards_from_knowledge_output(text)
        for card in new_cards:
            key = (card.type, card.title)
            if key not in seen:
                seen.add(key)
                cards.append(card)
    return cards[:6]


def _cards_from_shop_output(text: str) -> list[ChatCard]:
    try:
        payload = json.loads(text)
    except json.JSONDecodeError:
        return []
    if not isinstance(payload, dict) or not payload.get("name"):
        return []
    images = str(payload.get("images") or "")
    image = images.split(",")[0] if images else None
    return [
        ChatCard(
            type="shop",
            title=str(payload.get("name")),
            subtitle=str(payload.get("address") or payload.get("area") or ""),
            image=image,
            meta={
                "id": payload.get("id"),
                "avgPrice": payload.get("avgPrice"),
                "score": payload.get("score"),
                "openHours": payload.get("openHours"),
            },
        )
    ]


def _cards_from_voucher_output(text: str) -> list[ChatCard]:
    try:
        payload = json.loads(text)
    except json.JSONDecodeError:
        return []
    items = payload.get("data") if isinstance(payload, dict) else payload
    if not isinstance(items, list):
        return []
    cards: list[ChatCard] = []
    for item in items[:3]:
        if not isinstance(item, dict) or not item.get("title"):
            continue
        cards.append(
            ChatCard(
                type="voucher",
                title=str(item.get("title")),
                subtitle=str(item.get("subTitle") or item.get("rules") or ""),
                meta={
                    "id": item.get("id"),
                    "shopId": item.get("shopId"),
                    "type": item.get("type"),
                    "payValue": item.get("payValue"),
                    "actualValue": item.get("actualValue"),
                    "stock": item.get("stock"),
                },
            )
        )
    return cards


def _cards_from_knowledge_output(text: str) -> list[ChatCard]:
    cards: list[ChatCard] = []
    for match in re.finditer(r"source=([^#\n]+)#chunk(\d+)", text):
        source = match.group(1).strip()
        chunk = match.group(2)
        title = source.split("/")[-1]
        cards.append(ChatCard(type="reference", title=title, subtitle=source, meta={"chunk": chunk}))
    return cards[:3]


def append_to_history(history: ChatMessageHistory, user_message: str, ai_message: str, max_messages: int = MAX_HISTORY_MESSAGES) -> None:
    history.add_user_message(user_message)
    history.add_ai_message(ai_message)
    if len(history.messages) > max_messages:
        history.messages = history.messages[-max_messages:]


@app.get("/")
def root() -> Dict[str, Union[str, int]]:
    return {
        "service": "cs-bot-python",
        "knowledge_chunks": kb_chunks,
        "chat_endpoint": "/chat",
    }
