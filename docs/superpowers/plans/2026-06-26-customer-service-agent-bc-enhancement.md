# Customer Service Agent B+C Enhancement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Strengthen the customer-service agent with deterministic intent routing, intent-scoped tools, typed fallbacks, trace metadata, structured logs, and regression tests.

**Architecture:** `/chat` creates a trace ID, routes the message to a deterministic intent, builds only the tools allowed for that intent, invokes the existing LangChain tool-calling agent, classifies errors, and emits structured logs. Existing response fields remain compatible while optional `intent`, `trace_id`, and `error_code` metadata are added.

**Tech Stack:** Python 3, FastAPI, Pydantic v2, LangChain, httpx, unittest.

---

## File Structure

- Create `cs-bot-python/app/intent.py`: intent constants, keyword rules, deterministic `detect_intent`.
- Modify `cs-bot-python/app/models.py`: add optional metadata to `ChatResponse`.
- Modify `cs-bot-python/app/tools.py`: add intent-aware tool policy and structured tool error helpers.
- Modify `cs-bot-python/app/chains.py`: add intent variable to prompt constraints.
- Modify `cs-bot-python/app/main.py`: trace ID, intent routing, typed fallback, structured chat logs.
- Modify `cs-bot-python/tests/test_bot_stability.py`: regression tests for routing, tool gating, metadata, fallback, and error helpers.

## Task 1: Intent Routing

**Files:**
- Create: `cs-bot-python/app/intent.py`
- Test: `cs-bot-python/tests/test_bot_stability.py`

- [ ] **Step 1: Add routing tests**

Add imports and tests to `cs-bot-python/tests/test_bot_stability.py`:

```python
from app.intent import Intent, detect_intent

    def test_detect_intent_routes_common_messages(self):
        examples = {
            "这家店铺地址在哪里": Intent.SHOP,
            "1号店铺有什么优惠券": Intent.VOUCHER,
            "帮我查一下订单123的秒杀状态": Intent.ORDER,
            "这个订单可以退款吗": Intent.REFUND,
            "平台登录规则是什么": Intent.RULE,
            "你好": Intent.FALLBACK,
        }

        for message, expected in examples.items():
            with self.subTest(message=message):
                self.assertEqual(detect_intent(message), expected)

    def test_detect_intent_prioritizes_refund_over_order(self):
        self.assertEqual(detect_intent("订单123怎么取消退款"), Intent.REFUND)
```

- [ ] **Step 2: Run routing tests to verify failure**

Run: `cd cs-bot-python && python -m unittest tests.test_bot_stability -v`
Expected: FAIL because `app.intent` does not exist.

- [ ] **Step 3: Implement deterministic router**

Create `cs-bot-python/app/intent.py`:

```python
from __future__ import annotations

from enum import StrEnum


class Intent(StrEnum):
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
```

- [ ] **Step 4: Run routing tests to verify pass**

Run: `cd cs-bot-python && python -m unittest tests.test_bot_stability -v`
Expected: PASS for existing and new routing tests.

## Task 2: Response Metadata

**Files:**
- Modify: `cs-bot-python/app/models.py`
- Test: `cs-bot-python/tests/test_bot_stability.py`

- [ ] **Step 1: Add response metadata test**

Add this test to `cs-bot-python/tests/test_bot_stability.py`:

```python
    def test_chat_response_serializes_agent_metadata(self):
        response = ChatResponse(
            answer="已帮你查询",
            intent="order",
            trace_id="trace-123",
            error_code=None,
        )

        payload = response.model_dump()

        self.assertEqual(payload["intent"], "order")
        self.assertEqual(payload["trace_id"], "trace-123")
        self.assertIsNone(payload["error_code"])
```

- [ ] **Step 2: Run metadata test to verify failure**

Run: `cd cs-bot-python && python -m unittest tests.test_bot_stability.BotStabilityTest.test_chat_response_serializes_agent_metadata -v`
Expected: FAIL because `ChatResponse` does not expose these fields.

- [ ] **Step 3: Add optional metadata fields**

Modify `cs-bot-python/app/models.py` `ChatResponse`:

```python
class ChatResponse(BaseModel):
    answer: str
    used_tools: list[str] = Field(default_factory=list)
    suggestions: list[str] = Field(default_factory=list)
    cards: list["ChatCard"] = Field(default_factory=list)
    intent: Optional[str] = None
    trace_id: Optional[str] = None
    error_code: Optional[str] = None
```

- [ ] **Step 4: Run metadata test to verify pass**

Run: `cd cs-bot-python && python -m unittest tests.test_bot_stability.BotStabilityTest.test_chat_response_serializes_agent_metadata -v`
Expected: PASS.

## Task 3: Intent-Aware Tools

**Files:**
- Modify: `cs-bot-python/app/tools.py`
- Test: `cs-bot-python/tests/test_bot_stability.py`

- [ ] **Step 1: Add tool policy tests**

Add imports and tests to `cs-bot-python/tests/test_bot_stability.py`:

```python
from app.intent import Intent, detect_intent
from app.tools import build_tools, classify_tool_error, make_tool_error

    def test_build_tools_limits_tools_by_intent(self):
        api_client = object()
        kb = object()

        tools = build_tools(api_client, kb, Intent.ORDER)
        names = [tool.name for tool in tools]

        self.assertEqual(names, ["query_seckill_order_status", "query_local_knowledge"])

    def test_make_tool_error_returns_structured_json(self):
        payload = json.loads(make_tool_error("BACKEND_TIMEOUT", "后端服务响应超时，请稍后重试"))

        self.assertFalse(payload["ok"])
        self.assertEqual(payload["error_code"], "BACKEND_TIMEOUT")
        self.assertEqual(payload["message"], "后端服务响应超时，请稍后重试")

    def test_classify_tool_error_reads_structured_json(self):
        output = make_tool_error("BACKEND_ERROR", "后端返回业务错误")

        self.assertEqual(classify_tool_error(output), "BACKEND_ERROR")
```

- [ ] **Step 2: Run tool tests to verify failure**

Run: `cd cs-bot-python && python -m unittest tests.test_bot_stability.BotStabilityTest.test_build_tools_limits_tools_by_intent tests.test_bot_stability.BotStabilityTest.test_make_tool_error_returns_structured_json tests.test_bot_stability.BotStabilityTest.test_classify_tool_error_reads_structured_json -v`
Expected: FAIL because tool policy helpers do not exist.

- [ ] **Step 3: Implement tool policy and structured errors**

Modify `cs-bot-python/app/tools.py`:

```python
from __future__ import annotations

import json
import httpx

from langchain_core.tools import tool

from .api_client import BackendBusinessError, BackendClient
from .intent import Intent
from .knowledge_base import LocalKnowledgeBase


TOOL_POLICY: dict[Intent, tuple[str, ...]] = {
    Intent.SHOP: ("query_shop_detail", "query_local_knowledge"),
    Intent.VOUCHER: ("query_shop_detail", "query_shop_vouchers", "query_local_knowledge"),
    Intent.ORDER: ("query_seckill_order_status", "query_local_knowledge"),
    Intent.REFUND: ("query_seckill_order_status", "query_local_knowledge"),
    Intent.RULE: ("query_local_knowledge",),
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
    def query_local_knowledge(question: str) -> str:
        """从本地知识库检索运营规则、文档说明。输入问题文本。"""
        docs = kb.search(question)
        if not docs:
            return make_tool_error("KNOWLEDGE_EMPTY", "知识库暂无相关内容")

        pieces: list[str] = []
        for idx, doc in enumerate(docs, start=1):
            source = doc.metadata.get("source", "unknown")
            chunk = doc.metadata.get("chunk", "?")
            pieces.append(f"[{idx}] source={source}#chunk{chunk}\n{doc.page_content[:500]}")
        return "\n\n".join(pieces)

    tools = [query_shop_detail, query_shop_vouchers, query_seckill_order_status, query_local_knowledge]
    if intent is None:
        return tools

    resolved_intent = intent if isinstance(intent, Intent) else Intent(str(intent))
    allowed = set(TOOL_POLICY[resolved_intent])
    return [candidate for candidate in tools if candidate.name in allowed]
```

- [ ] **Step 4: Run tool tests to verify pass**

Run: `cd cs-bot-python && python -m unittest tests.test_bot_stability -v`
Expected: PASS.

## Task 4: Typed Fallbacks and Chat Logging

**Files:**
- Modify: `cs-bot-python/app/main.py`
- Test: `cs-bot-python/tests/test_bot_stability.py`

- [ ] **Step 1: Add fallback tests**

Add tests to `cs-bot-python/tests/test_bot_stability.py`:

```python
    def test_build_fallback_response_sets_error_metadata(self):
        response = build_fallback_response(
            "MODEL_NOT_CONFIGURED",
            intent="fallback",
            trace_id="trace-abc",
        )

        self.assertEqual(response.error_code, "MODEL_NOT_CONFIGURED")
        self.assertEqual(response.intent, "fallback")
        self.assertEqual(response.trace_id, "trace-abc")
        self.assertNotIn("sk-", response.answer)

    def test_build_suggestions_handles_refund_intent(self):
        suggestions = build_suggestions("我想取消订单并退款", [], intent="refund")

        self.assertIn("秒杀订单状态", suggestions)
        self.assertIn("转人工", suggestions)
```

- [ ] **Step 2: Run fallback tests to verify failure**

Run: `cd cs-bot-python && python -m unittest tests.test_bot_stability.BotStabilityTest.test_build_fallback_response_sets_error_metadata tests.test_bot_stability.BotStabilityTest.test_build_suggestions_handles_refund_intent -v`
Expected: FAIL because signatures do not support metadata or intent suggestions.

- [ ] **Step 3: Implement trace, fallback, logging helpers**

Modify `cs-bot-python/app/main.py`:

```python
import logging
import time
import uuid
```

Add module logger after constants:

```python
logger = logging.getLogger("cs_bot.chat")
```

Change `/chat` flow to:

```python
@app.post("/chat", response_model=ChatResponse)
def chat(payload: ChatRequest) -> ChatResponse:
    trace_id = uuid.uuid4().hex
    started_at = time.monotonic()
    intent = detect_intent(payload.message)
    used_tools: list[str] = []
    error_code: str | None = None
    fallback = False

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
                "intent": intent.value,
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
    log_chat_turn(payload, trace_id, intent.value, used_tools, started_at, error_code, fallback)
    return response
```

Update `build_fallback_response`:

```python
FALLBACK_MESSAGES = {
    "MODEL_NOT_CONFIGURED": "智能客服模型还没有配置完成，但我可以先帮你定位常见问题。",
    "MODEL_ERROR": "智能客服暂时开小差了，请稍后重试。",
    "BACKEND_ERROR": "业务服务暂时不可用，请稍后重试或联系人工客服。",
    "BACKEND_TIMEOUT": "业务服务响应超时，请稍后重试。",
    "KNOWLEDGE_EMPTY": "我暂时没有找到相关规则说明，可以换个问法或联系人工客服。",
    "UNSUPPORTED_INTENT": "这个问题暂时超出智能客服能力范围，建议联系人工客服。",
}


def build_fallback_response(error_code: str, intent: str | None = None, trace_id: str | None = None) -> ChatResponse:
    answer = FALLBACK_MESSAGES.get(error_code, FALLBACK_MESSAGES["MODEL_ERROR"])
    if trace_id:
        answer += f"\n\n追踪编号：{trace_id}"
    return ChatResponse(
        answer=answer,
        suggestions=build_suggestions("", [], intent=intent),
        intent=intent,
        trace_id=trace_id,
        error_code=error_code,
    )
```

Update `build_suggestions` signature and refund branch:

```python
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
```

Add logging helper:

```python
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
```

Add imports:

```python
from .intent import detect_intent
from .tools import build_tools, classify_tool_error
```

- [ ] **Step 4: Run fallback tests to verify pass**

Run: `cd cs-bot-python && python -m unittest tests.test_bot_stability -v`
Expected: PASS.

## Task 5: Prompt Intent Constraint

**Files:**
- Modify: `cs-bot-python/app/chains.py`
- Test: `cs-bot-python/tests/test_bot_stability.py`

- [ ] **Step 1: Add prompt construction smoke test**

Add test to `cs-bot-python/tests/test_bot_stability.py`:

```python
    def test_system_prompt_mentions_intent_constraints(self):
        from app.chains import SYSTEM_PROMPT

        self.assertIn("当前识别意图", SYSTEM_PROMPT)
        self.assertIn("只能使用当前提供的工具", SYSTEM_PROMPT)
        self.assertIn("退款", SYSTEM_PROMPT)
```

- [ ] **Step 2: Run prompt test to verify failure**

Run: `cd cs-bot-python && python -m unittest tests.test_bot_stability.BotStabilityTest.test_system_prompt_mentions_intent_constraints -v`
Expected: FAIL because current prompt lacks these constraints.

- [ ] **Step 3: Tighten prompt and accept intent**

Modify `cs-bot-python/app/chains.py`:

```python
SYSTEM_PROMPT = """
你是电商平台的智能客服助手，请遵循：
1. 当前识别意图是：{intent}。回答和工具调用必须围绕这个意图展开。
2. 只能使用当前提供的工具，不要假设还有其他隐藏工具。
3. 涉及订单、优惠券、店铺信息时，优先调用工具查询后再回答。
4. 涉及活动规则、说明文档时，优先检索本地知识库；如果检索结果包含 source，请在回答中简要说明来源文件名。
5. 回答简洁清晰，必要时给出下一步操作建议。
6. 严禁编造不存在的订单状态、优惠券规则或退款结果。
7. 未获得工具明确确认前，不要声称已经完成退款、取消订单、支付等写操作。
""".strip()
```

Update signature:

```python
def build_agent_executor(
    llm_model: str,
    openai_api_key: str,
    openai_base_url: str,
    tools,
    reasoning_effort: str = "high",
    thinking_enabled: bool = False,
    intent: str = "fallback",
) -> AgentExecutor:
```

Keep prompt using `SYSTEM_PROMPT` so `{intent}` is filled from invoke input.

- [ ] **Step 4: Run prompt test to verify pass**

Run: `cd cs-bot-python && python -m unittest tests.test_bot_stability -v`
Expected: PASS.

## Task 6: Final Validation

**Files:**
- Review: `cs-bot-python/app/*.py`
- Review: `cs-bot-python/tests/test_bot_stability.py`

- [ ] **Step 1: Run all bot tests**

Run: `cd cs-bot-python && python -m unittest -v`
Expected: PASS.

- [ ] **Step 2: Run syntax compile check**

Run: `cd cs-bot-python && python -m compileall app tests`
Expected: all files compile successfully.

- [ ] **Step 3: Review diff**

Run: `git diff -- cs-bot-python docs/superpowers/specs/2026-06-26-customer-service-agent-bc-enhancement-design.md docs/superpowers/plans/2026-06-26-customer-service-agent-bc-enhancement.md`
Expected: changes match the approved spec and plan.
