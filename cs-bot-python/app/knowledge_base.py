from __future__ import annotations

from dataclasses import dataclass
import glob
from pathlib import Path
import re
from typing import Optional

from langchain_community.retrievers import BM25Retriever
from langchain_core.documents import Document


@dataclass(frozen=True)
class ScoredDocument:
    document: Document
    confidence: float
    matched_keywords: list[str]


class LocalKnowledgeBase:
    def __init__(self, path_glob: str) -> None:
        self.path_glob = path_glob
        self._retriever: Optional[BM25Retriever] = None

    def rebuild(self) -> int:
        documents: list[Document] = []
        for file_path in glob.glob(self.path_glob):
            text = Path(file_path).read_text(encoding="utf-8")
            documents.extend(_split_markdown(file_path, text))

        if not documents:
            self._retriever = None
            return 0

        retriever = BM25Retriever.from_documents(documents)
        retriever.k = 3
        self._retriever = retriever
        return len(documents)

    def search(self, query: str) -> list[Document]:
        return [result.document for result in self.search_with_scores(query)]

    def search_with_scores(self, query: str) -> list[ScoredDocument]:
        if self._retriever is None:
            return []

        query_tokens = _tokenize(query)
        if not query_tokens:
            return []

        results: list[ScoredDocument] = []
        for document in self._retriever.invoke(query):
            searchable = " ".join(
                [
                    document.page_content,
                    str(document.metadata.get("title") or ""),
                    str(document.metadata.get("section") or ""),
                ]
            )
            matched_keywords = [token for token in query_tokens if token in searchable]
            if not matched_keywords:
                continue
            confidence = min(1.0, len(matched_keywords) / max(len(query_tokens), 1))
            results.append(ScoredDocument(document=document, confidence=confidence, matched_keywords=matched_keywords))

        results.sort(key=lambda item: item.confidence, reverse=True)
        return results


def _split_markdown(file_path: str, text: str) -> list[Document]:
    title = Path(file_path).stem
    current_section = title
    chunks: list[tuple[str, str]] = []
    current_lines: list[str] = []

    for line in text.splitlines():
        title_match = re.match(r"^#\s+(.+?)\s*$", line)
        if title_match:
            title = title_match.group(1)
            continue

        section_match = re.match(r"^##\s+(.+?)\s*$", line)
        if section_match:
            if "\n".join(current_lines).strip():
                chunks.append((current_section, "\n".join(current_lines).strip()))
            current_section = section_match.group(1)
            current_lines = []
            continue

        current_lines.append(line)

    if "\n".join(current_lines).strip():
        chunks.append((current_section, "\n".join(current_lines).strip()))

    if not chunks and text.strip():
        chunks.append((current_section, text.strip()))

    documents: list[Document] = []
    for index, (section, content) in enumerate(chunks, start=1):
        documents.append(
            Document(
                page_content=content,
                metadata={
                    "source": file_path,
                    "title": title,
                    "section": section,
                    "chunk": index,
                    "chunk_id": f"{Path(file_path).name}#chunk{index}",
                },
            )
        )
    return documents


def _tokenize(text: str) -> list[str]:
    raw_tokens = re.findall(r"[A-Za-z0-9_]+|[\u4e00-\u9fff]{2,}", text.lower())
    tokens: list[str] = []
    for token in raw_tokens:
        if re.fullmatch(r"[\u4e00-\u9fff]{2,}", token):
            tokens.append(token)
            tokens.extend(token[start : start + 2] for start in range(0, max(len(token) - 1, 0)))
        else:
            tokens.append(token)
    return list(dict.fromkeys(tokens))
