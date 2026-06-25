# Customer Service Agent B+C Enhancement Design

## Background

The project currently includes an independent Python customer-service agent under `cs-bot-python`. It exposes `/chat`, uses LangChain tool-calling, searches local Markdown documents, and can call backend APIs for shop details, shop vouchers, and seckill order status.

The next enhancement focuses on two goals:

- **B: Agent capability** — make the agent better at choosing the right business path and using domain tools.
- **C: Engineering stability** — make the agent safer, easier to debug, and more predictable under failures.

This design intentionally avoids platform-level complexity such as multi-agent orchestration or admin dashboards. The first phase should be small enough to implement and verify inside the current repo.

## Goals

1. Route user messages into clear business intents before invoking the LLM agent.
2. Restrict tool availability by intent to reduce unnecessary or unsafe tool calls.
3. Add structured response metadata for intent, trace ID, and failure reason.
4. Improve fallback behavior for model errors, backend errors, timeouts, and missing knowledge.
5. Add basic observability through structured logs.
6. Add regression tests for routing, tool gating, fallback behavior, and response metadata.

## Non-Goals

- No new frontend chat UI in this phase.
- No vector database or external knowledge service.
- No multi-agent framework.
- No persistent chat-history storage.
- No changes to payment, order, or refund backend domain logic unless a missing read-only endpoint blocks tool integration.

## Current Pain Points

### Agent Capability

- All tools are currently available to the agent for every message.
- Intent is implicit and depends on the model choosing the right tool.
- Suggestions and cards are generated after execution but do not guide execution.
- Knowledge retrieval has no explicit no-answer signal beyond plain text.

### Engineering Stability

- Fallback responses are generic and expose raw exception text to users.
- Responses do not include trace IDs, intent, or error codes.
- Tool errors are returned as plain text, making them hard to classify.
- There is no structured log for a chat turn.
- Tests cover basic validation and cards, but not intent routing or tool policy.

## Proposed Architecture

```text
/chat request
  -> validate request
  -> create trace_id
  -> detect intent
  -> build backend client
  -> build tools allowed for intent
  -> invoke agent executor
  -> classify result or error
  -> append history when appropriate
  -> return structured ChatResponse
  -> emit structured log
```

## Intent Routing

Add `cs-bot-python/app/intent.py` with a lightweight deterministic router.

### Intent Types

- `shop`: shop detail, address, opening hours, price, score.
- `voucher`: coupon, discount, seckill voucher, shop voucher list.
- `order`: order status, seckill order result, payment state.
- `refund`: cancel order, refund, after-sales status. In phase one this can return guided fallback if backend tools are not ready.
- `rule`: platform rules, login rules, posting rules, activity rules, FAQ.
- `fallback`: greeting, unclear question, unsupported request.

### Routing Strategy

Use keyword and phrase matching first. Keep the implementation deterministic and testable. Do not call the LLM just to classify intent in phase one.

Examples:

- Contains `订单`, `下单`, `支付状态`, `秒杀状态` -> `order`.
- Contains `退款`, `取消`, `售后`, `退单` -> `refund`.
- Contains `优惠券`, `券`, `折扣`, `秒杀券` -> `voucher`.
- Contains `店铺`, `商家`, `地址`, `营业`, `评分` -> `shop`.
- Contains `规则`, `怎么登录`, `怎么发布`, `限制`, `FAQ` -> `rule`.

If multiple intents match, use priority:

1. `refund`
2. `order`
3. `voucher`
4. `shop`
5. `rule`
6. `fallback`

This prioritizes sensitive transactional actions over general content.

## Tool Policy

Change `build_tools` to accept an optional `intent` and return only allowed tools.

### Allowed Tools

- `shop`: `query_shop_detail`, `query_local_knowledge`
- `voucher`: `query_shop_detail`, `query_shop_vouchers`, `query_local_knowledge`
- `order`: `query_seckill_order_status`, `query_local_knowledge`
- `refund`: `query_seckill_order_status`, `query_local_knowledge` in phase one; guided fallback for unsupported mutations
- `rule`: `query_local_knowledge`
- `fallback`: `query_local_knowledge`

### Tool Error Shape

Tools should return JSON strings with a consistent shape when possible:

```json
{
  "ok": false,
  "error_code": "BACKEND_TIMEOUT",
  "message": "后端服务响应超时，请稍后重试"
}
```

Successful backend payloads can keep their existing shape during phase one to avoid breaking card parsing, but new helpers should be added to classify error strings and JSON error objects.

## Response Model

Extend `ChatResponse` with optional metadata:

- `intent: str | None`
- `trace_id: str | None`
- `error_code: str | None`

Keep existing fields compatible:

- `answer`
- `used_tools`
- `suggestions`
- `cards`

The frontend can ignore new fields until it is updated.

## Fallback Behavior

Add typed fallback responses instead of a single generic response.

### Error Codes

- `MODEL_NOT_CONFIGURED`: API key missing.
- `MODEL_ERROR`: model invocation failed.
- `BACKEND_ERROR`: backend returned a business or HTTP error.
- `BACKEND_TIMEOUT`: backend timed out.
- `KNOWLEDGE_EMPTY`: no relevant knowledge found.
- `UNSUPPORTED_INTENT`: request is outside current bot scope.

### User-Facing Principles

- Do not expose raw stack traces or internal exception strings in `answer`.
- Include `trace_id` so logs can be correlated.
- Provide a next step, such as retrying, checking login, or contacting human support.
- For refund/cancel requests in phase one, explain that the bot can help check status and guide the user, but cannot directly mutate orders unless backend support exists.

## Observability

Use Python `logging` to emit one structured JSON log per chat turn.

Fields:

- `trace_id`
- `session_id`
- `user_id`
- `intent`
- `used_tools`
- `duration_ms`
- `error_code`
- `fallback`

Avoid logging full `user_token`. Log only whether a token exists.

## Prompt Updates

Update the system prompt in `chains.py`:

- Mention the detected intent as an execution constraint.
- Tell the model to only use provided tools.
- Tell the model not to claim it completed mutations such as refund or cancel unless a tool result confirms it.
- Tell the model to cite knowledge source names when answering rule questions if retrieved context includes sources.

The prompt can receive `intent` as an input variable or include it in the human message envelope.

## Test Plan

Extend `cs-bot-python/tests/test_bot_stability.py` or add focused test files.

Required coverage:

1. Intent routing maps representative Chinese messages to expected intents.
2. Intent priority chooses `refund` before `order` when both appear.
3. Tool builder returns only allowed tools for each intent.
4. `ChatResponse` accepts and serializes `intent`, `trace_id`, and `error_code`.
5. Missing API key returns `MODEL_NOT_CONFIGURED` and does not expose internals.
6. Fallback suggestions remain useful for order, voucher, rule, and fallback cases.
7. Structured error helper classifies backend timeout and backend business errors.

## Implementation Notes

Suggested files:

- `cs-bot-python/app/intent.py`: intent enum/constants and deterministic router.
- `cs-bot-python/app/models.py`: response metadata fields.
- `cs-bot-python/app/tools.py`: intent-aware tool policy and structured tool errors.
- `cs-bot-python/app/chains.py`: prompt constraints and optional intent variable.
- `cs-bot-python/app/main.py`: trace ID, routing, typed fallback, structured logs.
- `cs-bot-python/tests/test_bot_stability.py`: regression tests.

## Rollout

1. Add deterministic routing and tests.
2. Add response metadata and typed fallbacks.
3. Add intent-aware tool gating.
4. Add structured logging.
5. Tighten prompt constraints.
6. Run bot tests.
7. Manually verify `/chat` with representative examples.

## Phase-One Decisions

- Refund and cancel remain guided-only unless existing backend read-only status APIs can safely support the answer. The bot must not claim to submit a refund or cancellation in phase one.
- Structured logs go to stdout through Python `logging`, matching local `uvicorn` development and avoiding new file-management concerns.
- `trace_id` is generated by the bot for every `/chat` request. A future frontend-provided trace header can be added later without changing the response shape.
