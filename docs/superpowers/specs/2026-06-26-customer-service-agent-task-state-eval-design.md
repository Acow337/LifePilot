# Customer Service Agent Task State and Eval Design

## Goal

Add a lightweight multi-turn task state layer and an offline evaluation dataset so the customer-service agent can ask for missing parameters instead of guessing, and future prompt/tool changes can be regression-tested.

## Scope

- Track pending task slots per session for supported intents.
- Ask concise follow-up questions when required parameters are missing.
- Resume the pending task when the user provides the missing value.
- Add an offline eval dataset and runner for intent, slot, and tool-policy behavior.

## Non-Goals

- No persistent storage for task state.
- No LLM-based eval scoring.
- No frontend changes.
- No backend write operations such as refund submission or order cancellation.

## Supported Slots

- `shop_id`: required for shop and voucher tool flows.
- `order_id`: required for order and refund status flows.

## Behavior

1. `/chat` detects intent.
2. The task state layer extracts simple numeric slots from the current message.
3. If the intent requires a missing slot, `/chat` returns a follow-up response without invoking the LLM.
4. The pending task is stored in memory by `session_id`.
5. If a later message provides the missing slot, the pending intent is resumed and the LLM is invoked with the completed slot context.
6. If the user changes to a different explicit intent, the pending task is replaced.

## Eval

Add JSONL cases under `cs-bot-python/evals/customer_service_agent.jsonl` and a Python runner under `cs-bot-python/evals/run_agent_eval.py`.

Each case contains:

- `id`
- `message`
- `expected_intent`
- `expected_missing_slot`
- `expected_allowed_tools`

The runner validates deterministic routing and tool policy without calling the LLM.
