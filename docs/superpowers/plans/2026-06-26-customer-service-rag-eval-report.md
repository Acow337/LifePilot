# Customer Service RAG 2.0 and Eval Report Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Improve local RAG reliability and make deterministic eval output more useful.

**Architecture:** `LocalKnowledgeBase` gains structured Markdown chunk metadata and a scored search API while keeping `search()` compatible. The knowledge tool uses scored search to cite sources and reject low-confidence answers. The eval runner returns a category report without calling external services.

**Tech Stack:** Python 3.9, LangChain BM25Retriever, unittest, JSONL.

---

## Tasks

- [ ] Add failing tests for structured knowledge metadata and scored search confidence.
- [ ] Implement structured Markdown section chunking and keyword overlap scoring.
- [ ] Update `query_local_knowledge` to include citation/confidence fields and reject low-confidence matches.
- [ ] Add failing tests for eval report formatting.
- [ ] Implement eval report summary while preserving nonzero failure exit codes.
- [ ] Run unit tests, eval runner, and compile checks.
