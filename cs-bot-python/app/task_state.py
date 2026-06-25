from __future__ import annotations

from dataclasses import dataclass, field
import re

from .intent import Intent


REQUIRED_SLOTS: dict[Intent, str] = {
    Intent.SHOP: "shop_id",
    Intent.VOUCHER: "shop_id",
    Intent.ORDER: "order_id",
    Intent.REFUND: "order_id",
}

FOLLOW_UPS: dict[str, str] = {
    "shop_id": "请提供店铺ID，例如：1号店铺。",
    "order_id": "请提供订单号，例如：订单123456。",
}


@dataclass(frozen=True)
class SlotStatus:
    intent: Intent
    slots: dict[str, str] = field(default_factory=dict)
    missing_slot: str | None = None
    follow_up: str | None = None

    @property
    def is_complete(self) -> bool:
        return self.missing_slot is None


def extract_slots(message: str) -> dict[str, str]:
    slots: dict[str, str] = {}

    order_match = re.search(r"(?:订单|订单号)\s*[:：#号]?\s*(\d{3,})", message)
    if order_match:
        slots["order_id"] = order_match.group(1)

    shop_match = re.search(r"(\d+)\s*号?\s*(?:店铺|商家|店)", message)
    if shop_match:
        slots["shop_id"] = shop_match.group(1)

    return slots


def build_slot_status(intent: Intent, message: str, existing_slots: dict[str, str] | None = None) -> SlotStatus:
    slots = dict(existing_slots or {})
    slots.update(extract_slots(message))
    required_slot = REQUIRED_SLOTS.get(intent)
    if required_slot and not slots.get(required_slot):
        return SlotStatus(
            intent=intent,
            slots=slots,
            missing_slot=required_slot,
            follow_up=FOLLOW_UPS[required_slot],
        )
    return SlotStatus(intent=intent, slots=slots)


class PendingTaskStore:
    def __init__(self) -> None:
        self._tasks: dict[str, SlotStatus] = {}

    def get(self, session_id: str) -> SlotStatus | None:
        return self._tasks.get(session_id)

    def save(self, session_id: str, status: SlotStatus) -> None:
        if status.missing_slot:
            self._tasks[session_id] = status
        else:
            self.clear(session_id)

    def clear(self, session_id: str) -> None:
        self._tasks.pop(session_id, None)

    def resume_if_possible(self, session_id: str, message: str, current_intent: Intent | None = None) -> SlotStatus | None:
        pending = self.get(session_id)
        if pending is None:
            return None
        if current_intent and current_intent != Intent.FALLBACK and current_intent != pending.intent:
            self.clear(session_id)
            return None
        existing_slots = dict(pending.slots)
        if pending.missing_slot and pending.missing_slot not in existing_slots:
            bare_number_match = re.fullmatch(r"\s*(\d+)\s*", message)
            if bare_number_match:
                existing_slots[pending.missing_slot] = bare_number_match.group(1)
        status = build_slot_status(pending.intent, message, existing_slots)
        if status.is_complete:
            self.clear(session_id)
        else:
            self.save(session_id, status)
        return status
