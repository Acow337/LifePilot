from __future__ import annotations

from collections import defaultdict
import json
import re
from typing import Dict, Union

from fastapi import FastAPI, HTTPException
from langchain_community.chat_message_histories import ChatMessageHistory

from .api_client import BackendClient
from .chains import build_agent_executor
from .config import get_settings
from .knowledge_base import LocalKnowledgeBase
from .models import ChatCard, ChatRequest, ChatResponse, HealthResponse
from .tools import build_tools

app = FastAPI(title="LangChain Customer Service Bot", version="0.1.0")

settings = get_settings()
kb = LocalKnowledgeBase(settings.knowledge_glob)
kb_chunks = kb.rebuild()
session_history: dict[str, ChatMessageHistory] = defaultdict(ChatMessageHistory)
MAX_HISTORY_MESSAGES = settings.max_history_messages
DEFAULT_SUGGESTIONS = ["查店铺优惠", "秒杀订单状态", "平台规则", "转人工"]


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    return HealthResponse(status="ok")


@app.post("/reindex")
def reindex_knowledge() -> dict[str, int]:
    chunks = kb.rebuild()
    return {"chunks": chunks}


@app.post("/chat", response_model=ChatResponse)
def chat(payload: ChatRequest) -> ChatResponse:
    if not settings.openai_api_key:
        return build_fallback_response("OPENAI_API_KEY 未配置")

    api_client = BackendClient(
        base_url=settings.backend_base_url,
        timeout_seconds=settings.backend_timeout_seconds,
        token=payload.user_token,
    )
    tools = build_tools(api_client, kb)
    executor = build_agent_executor(
        llm_model=settings.llm_model,
        openai_api_key=settings.openai_api_key,
        openai_base_url=settings.openai_base_url,
        tools=tools,
        reasoning_effort=settings.llm_reasoning_effort,
        thinking_enabled=settings.llm_thinking_enabled,
    )

    history = session_history[payload.session_id]

    try:
        result = executor.invoke(
            {
                "input": payload.message,
                "chat_history": history.messages,
            }
        )
    except Exception as exc:
        return build_fallback_response(str(exc))

    answer = str(result.get("output", ""))
    append_to_history(history, payload.message, answer, MAX_HISTORY_MESSAGES)

    used_tools: list[str] = []
    for step in result.get("intermediate_steps", []):
        if isinstance(step, tuple) and len(step) == 2:
            action = step[0]
            tool_name = getattr(action, "tool", None)
            if tool_name:
                used_tools.append(tool_name)

    return ChatResponse(
        answer=answer,
        used_tools=used_tools,
        suggestions=build_suggestions(payload.message, used_tools),
        cards=build_cards_from_steps(result.get("intermediate_steps", [])),
    )


def build_fallback_response(reason: str) -> ChatResponse:
    answer = (
        "智能客服暂时无法连接模型，但我还可以帮你快速定位常见问题。"
        "你可以点击下方快捷问题，或稍后重试。"
    )
    if reason:
        answer += f"\n\n当前原因：{reason}"
    return ChatResponse(answer=answer, suggestions=DEFAULT_SUGGESTIONS)


def build_suggestions(message: str, used_tools: list[str]) -> list[str]:
    suggestions: list[str] = []
    text = message.lower()
    if "订单" in message or "秒杀" in message or "query_seckill_order_status" in used_tools:
        suggestions.extend(["秒杀订单状态", "秒杀失败怎么办"])
    if "店" in message or "优惠" in message or "券" in message or "query_shop_vouchers" in used_tools:
        suggestions.extend(["查店铺优惠", "优惠券怎么使用"])
    if "规则" in message or "登录" in message or "评论" in message or "query_local_knowledge" in used_tools:
        suggestions.extend(["平台规则", "如何发布探店笔记"])
    if not suggestions or "help" in text:
        suggestions.extend(DEFAULT_SUGGESTIONS)
    return list(dict.fromkeys(suggestions))[:4]


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
