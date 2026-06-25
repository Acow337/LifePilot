from __future__ import annotations

import argparse
import json
from pathlib import Path
import sys
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from app.intent import Intent, detect_intent
from app.task_state import build_slot_status
from app.tools import TOOL_POLICY


DEFAULT_DATASET = Path(__file__).with_name("customer_service_agent.jsonl")


def load_cases(path: Path) -> list[dict[str, Any]]:
    cases: list[dict[str, Any]] = []
    with path.open("r", encoding="utf-8") as file:
        for line_number, line in enumerate(file, start=1):
            stripped = line.strip()
            if not stripped:
                continue
            case = json.loads(stripped)
            case["line_number"] = line_number
            cases.append(case)
    return cases


def evaluate_case(case: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    intent = detect_intent(str(case["message"]))
    if intent.value != case["expected_intent"]:
        errors.append(f"intent expected {case['expected_intent']} got {intent.value}")

    slot_status = build_slot_status(intent, str(case["message"]))
    if slot_status.missing_slot != case["expected_missing_slot"]:
        errors.append(f"missing_slot expected {case['expected_missing_slot']} got {slot_status.missing_slot}")

    allowed_tools = list(TOOL_POLICY[Intent(case["expected_intent"])])
    if allowed_tools != case["expected_allowed_tools"]:
        errors.append(f"allowed_tools expected {case['expected_allowed_tools']} got {allowed_tools}")

    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description="Run deterministic customer-service agent evals.")
    parser.add_argument("dataset", nargs="?", default=str(DEFAULT_DATASET))
    args = parser.parse_args()

    path = Path(args.dataset)
    failures: list[str] = []
    cases = load_cases(path)
    for case in cases:
        errors = evaluate_case(case)
        if errors:
            failures.append(f"{case['id']} (line {case['line_number']}): " + "; ".join(errors))

    if failures:
        print(f"FAIL {len(failures)}/{len(cases)} cases")
        for failure in failures:
            print(f"- {failure}")
        return 1

    print(f"PASS {len(cases)} cases")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
