# Customer Service Cards Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add demo-friendly structured cards and source references to the intelligent customer-service widget.

**Architecture:** Keep LangChain tools as text-returning functions for Agent compatibility, and derive deterministic cards from tool results in Python. The frontend renders optional `cards` from `/chat` without changing the existing answer flow.

**Tech Stack:** FastAPI/Pydantic, LangChain tools, unittest, React/TypeScript, Vite, shell scripts.

---

### Task 1: Python cards model and extraction

**Files:**
- Modify: `cs-bot-python/app/models.py`
- Modify: `cs-bot-python/app/main.py`
- Test: `cs-bot-python/tests/test_bot_stability.py`

- [ ] Add failing tests for shop, voucher, and reference card extraction from `intermediate_steps`.
- [ ] Add `ChatCard` and `cards` to `ChatResponse`.
- [ ] Implement `build_cards_from_steps()` with deterministic parsing.
- [ ] Verify `python -m unittest tests/test_bot_stability.py` passes.

### Task 2: Frontend card rendering

**Files:**
- Modify: `frontend/src/services/types.ts`
- Modify: `frontend/src/components/CustomerServiceWidget.tsx`
- Modify: `frontend/src/styles/index.css`

- [ ] Add `ChatCard` TypeScript union.
- [ ] Store assistant `cards` on messages.
- [ ] Render shop, voucher, and reference cards in the chat panel.
- [ ] Verify `npm --prefix frontend run build` passes.

### Task 3: Bot dev startup script

**Files:**
- Create: `scripts/start-bot-dev.sh`
- Modify: `README.md`

- [ ] Add executable startup script for `cs-bot-python`.
- [ ] Document three-terminal startup: backend, frontend, bot.
- [ ] Verify script starts or fails with clear guidance.
