# Customer Service Demo Stability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the intelligent customer-service demo more impressive while remaining stable when LLM/backend calls fail.

**Architecture:** Extend the Python bot response contract with `suggestions`, add deterministic fallback replies and FAQ content, and update the React widget to show quick actions and follow-up suggestions. Keep changes small and compatible with existing `/chat` clients.

**Tech Stack:** FastAPI, Pydantic, LangChain, unittest, React, TypeScript, Vite.

---

### Task 1: Python Response Suggestions and Fallback

**Files:**
- Modify: `cs-bot-python/app/models.py`
- Modify: `cs-bot-python/app/main.py`
- Test: `cs-bot-python/tests/test_bot_stability.py`

- [ ] Add failing unittest cases for `ChatResponse.suggestions` and `build_fallback_response`.
- [ ] Run `cd cs-bot-python && .venv/bin/python -m unittest tests/test_bot_stability.py` and verify failure.
- [ ] Add `suggestions: list[str]` to `ChatResponse`.
- [ ] Add `build_fallback_response(reason: str)` and wrap `/chat` LLM execution in fallback handling.
- [ ] Run the unittest command again and verify pass.

### Task 2: Frontend Quick Actions and Suggestions

**Files:**
- Modify: `frontend/src/services/types.ts`
- Modify: `frontend/src/components/CustomerServiceWidget.tsx`
- Modify: `frontend/src/styles/index.css`

- [ ] Extend `ChatBotResponse` with optional `suggestions`.
- [ ] Add quick action buttons for store discounts, seckill order status, platform rules, and human support.
- [ ] Render response suggestions as clickable chips that send the selected prompt.
- [ ] Run `npm --prefix frontend run build` and verify pass.

### Task 3: FAQ Knowledge Content and Docs

**Files:**
- Create: `docs/customer-service-faq.md`
- Modify: `cs-bot-python/README.md`

- [ ] Add FAQ content for login, vouchers, seckill order status, posting/comments, image upload, and human support.
- [ ] Document demo quick actions and fallback behavior.
- [ ] Run Python compile and frontend build.
