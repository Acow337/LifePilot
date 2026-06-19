from __future__ import annotations

import glob
from pathlib import Path
from typing import Optional

from langchain_community.retrievers import BM25Retriever
from langchain_core.documents import Document


class LocalKnowledgeBase:
    def __init__(self, path_glob: str) -> None:
        self.path_glob = path_glob
        self._retriever: Optional[BM25Retriever] = None

    def rebuild(self) -> int:
        documents: list[Document] = []
        for file_path in glob.glob(self.path_glob):
            text = Path(file_path).read_text(encoding="utf-8")
            # 简单分块：每1200字符一块
            step = 1200
            for i in range(0, len(text), step):
                chunk = text[i : i + step]
                if chunk.strip():
                    documents.append(
                        Document(
                            page_content=chunk,
                            metadata={"source": file_path, "chunk": i // step + 1},
                        )
                    )

        if not documents:
            self._retriever = None
            return 0

        retriever = BM25Retriever.from_documents(documents)
        retriever.k = 3
        self._retriever = retriever
        return len(documents)

    def search(self, query: str) -> list[Document]:
        if self._retriever is None:
            return []
        return self._retriever.invoke(query)
