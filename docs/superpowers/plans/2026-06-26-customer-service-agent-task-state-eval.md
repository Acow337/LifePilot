# Customer Service Agent Task State and Eval Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add multi-turn slot collection and deterministic offline evals for the customer-service agent.

**Architecture:** A focused `task_state.py` module owns slot extraction, required-slot policy, pending task memory, and follow-up decisions. `/chat` uses it before invoking the LLM. A JSONL eval runner validates routing and tool policy without external services.

**Tech Stack:** Python 3.9, FastAPI, Pydantic v2, LangChain, unittest, JSONL.

---

## Files

- Create `cs-bot-python/app/task_state.py` for task state and slot logic.
- Modify `cs-bot-python/app/main.py` to short-circuit missing-slot turns and resume pending tasks.
- Modify `cs-bot-python/tests/test_bot_stability.py` for task state tests.
- Create `cs-bot-python/evals/customer_service_agent.jsonl` for deterministic eval cases.
- Create `cs-bot-python/evals/run_agent_eval.py` for offline eval execution.

## Tasks

- [ ] Add failing tests for slot extraction, missing slot follow-up, and pending task resume.
- [ ] Implement `task_state.py` minimally to pass those tests.
- [ ] Wire task state into `/chat` before LLM invocation.
- [ ] Add eval dataset and runner tests through real command execution.
- [ ] Run unit tests, eval runner, and compile checks.
