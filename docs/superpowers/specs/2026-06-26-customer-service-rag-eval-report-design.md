# Customer Service RAG 2.0 and Eval Report Design

## Goal

Upgrade the local customer-service knowledge retrieval from plain BM25 snippets to structured chunks with section metadata, keyword overlap, confidence scoring, and source citations. Extend the offline eval runner to produce a concise report with pass rate and category-level checks.

## Scope

- Preserve the current local Markdown knowledge source.
- Add structured chunk metadata: `source`, `title`, `section`, `chunk`, `chunk_id`.
- Add a scored search API for deterministic tests and tool output.
- Return low-confidence results as structured `KNOWLEDGE_EMPTY` errors instead of forcing an answer.
- Include source citations in `query_local_knowledge` output.
- Upgrade eval output from only PASS/FAIL to a small report containing total cases, pass rate, and failure details.

## Non-Goals

- No external vector database.
- No embedding API dependency.
- No LLM-based answer grading.
- No frontend changes.

## Design

`LocalKnowledgeBase` will keep the existing `search(query)` method for compatibility and add `search_with_scores(query)`. The implementation remains deterministic by computing simple token/keyword overlap against BM25 candidates. A result is considered confident if at least one meaningful query token overlaps with the document content or metadata.

`query_local_knowledge` will use `search_with_scores`; if no confident result exists it returns the existing structured error shape with `KNOWLEDGE_EMPTY`. Otherwise it returns source, section, chunk id, confidence, matched keywords, and content excerpt.

The eval runner will keep exit-code behavior while printing:

- total cases
- passed cases
- failed cases
- pass rate
- failed case details

## Success Criteria

- Existing tests still pass.
- New tests cover structured chunk metadata, scored search, low-confidence detection, and eval report formatting.
- `evals/run_agent_eval.py` still exits `0` for the current dataset and prints `PASS 6 cases` plus report details.
