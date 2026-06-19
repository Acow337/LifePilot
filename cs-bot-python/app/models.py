from __future__ import annotations

from typing import Optional
from typing import Any

from pydantic import BaseModel, Field, field_validator


class ChatRequest(BaseModel):
    session_id: str = Field(..., min_length=1, max_length=64, description="会话ID")
    user_id: str = Field(..., min_length=1, max_length=64, description="业务用户ID")
    message: str = Field(..., min_length=1, max_length=1000, description="用户消息")
    user_token: Optional[str] = Field(default=None, max_length=512, description="可选，透传业务token")

    @field_validator("session_id", "user_id")
    @classmethod
    def validate_identifier(cls, value: str) -> str:
        allowed = set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-:")
        if any(char not in allowed for char in value):
            raise ValueError("仅支持字母、数字、下划线、中划线和冒号")
        return value


class ChatResponse(BaseModel):
    answer: str
    used_tools: list[str] = Field(default_factory=list)
    suggestions: list[str] = Field(default_factory=list)
    cards: list["ChatCard"] = Field(default_factory=list)


class ChatCard(BaseModel):
    type: str
    title: str
    subtitle: Optional[str] = None
    image: Optional[str] = None
    meta: dict[str, Any] = Field(default_factory=dict)


class HealthResponse(BaseModel):
    status: str
