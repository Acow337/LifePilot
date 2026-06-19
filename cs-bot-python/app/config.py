from __future__ import annotations

import os
from dataclasses import dataclass

from dotenv import load_dotenv

load_dotenv()


@dataclass(frozen=True)
class Settings:
    openai_api_key: str
    openai_base_url: str
    llm_model: str
    llm_reasoning_effort: str
    llm_thinking_enabled: bool
    backend_base_url: str
    backend_timeout_seconds: int
    bot_host: str
    bot_port: int
    knowledge_glob: str
    max_history_messages: int



def get_settings() -> Settings:
    llm_thinking_enabled = os.getenv("LLM_THINKING_ENABLED", "false").lower() in {"1", "true", "yes", "on"}
    return Settings(
        openai_api_key=os.getenv("DEEPSEEK_API_KEY") or os.getenv("OPENAI_API_KEY", ""),
        openai_base_url=os.getenv("OPENAI_BASE_URL", "https://api.deepseek.com"),
        llm_model=os.getenv("LLM_MODEL", "deepseek-v4-pro"),
        llm_reasoning_effort=os.getenv("LLM_REASONING_EFFORT", "high"),
        llm_thinking_enabled=llm_thinking_enabled,
        backend_base_url=os.getenv("BACKEND_BASE_URL", "http://127.0.0.1:8081"),
        backend_timeout_seconds=int(os.getenv("BACKEND_TIMEOUT_SECONDS", "5")),
        bot_host=os.getenv("BOT_HOST", "127.0.0.1"),
        bot_port=int(os.getenv("BOT_PORT", "9000")),
        knowledge_glob=os.getenv("KNOWLEDGE_GLOB", "../docs/*.md"),
        max_history_messages=int(os.getenv("MAX_HISTORY_MESSAGES", "12")),
    )
