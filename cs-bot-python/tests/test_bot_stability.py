from pathlib import Path
import json
import unittest
from unittest.mock import patch

from pydantic import ValidationError

from app.api_client import BackendBusinessError, BackendClient
from app.config import get_settings
from app.intent import Intent, detect_intent
from app.knowledge_base import LocalKnowledgeBase
from app.main import append_to_history, build_cards_from_steps, build_fallback_response, build_slot_follow_up_response, build_suggestions
from app.models import ChatRequest, ChatResponse
from app.tools import build_tools, classify_tool_error, make_tool_error
from app.task_state import PendingTaskStore, SlotStatus, build_slot_status, extract_slots


class BotStabilityTest(unittest.TestCase):
    def test_empty_knowledge_base_is_searchable(self):
        import tempfile

        with tempfile.TemporaryDirectory() as tmp_dir:
            kb = LocalKnowledgeBase(str(Path(tmp_dir) / "*.md"))

            self.assertEqual(kb.rebuild(), 0)
            self.assertEqual(kb.search("优惠券规则"), [])

    def test_knowledge_base_adds_section_metadata_and_scores(self):
        import tempfile

        with tempfile.TemporaryDirectory() as tmp_dir:
            doc_path = Path(tmp_dir) / "faq.md"
            doc_path.write_text("# 客服 FAQ\n\n## 秒杀订单状态\n\n秒杀失败通常因为库存不足。\n", encoding="utf-8")
            kb = LocalKnowledgeBase(str(Path(tmp_dir) / "*.md"))

            self.assertEqual(kb.rebuild(), 1)
            results = kb.search_with_scores("秒杀失败原因")

            self.assertEqual(results[0].document.metadata["title"], "客服 FAQ")
            self.assertEqual(results[0].document.metadata["section"], "秒杀订单状态")
            self.assertIn("faq.md#chunk1", results[0].document.metadata["chunk_id"])
            self.assertGreater(results[0].confidence, 0)
            self.assertIn("秒杀", results[0].matched_keywords)

    def test_knowledge_base_filters_low_confidence_results(self):
        import tempfile

        with tempfile.TemporaryDirectory() as tmp_dir:
            doc_path = Path(tmp_dir) / "faq.md"
            doc_path.write_text("# 客服 FAQ\n\n## 图片上传\n\n图片支持 jpg 格式。\n", encoding="utf-8")
            kb = LocalKnowledgeBase(str(Path(tmp_dir) / "*.md"))
            kb.rebuild()

            self.assertEqual(kb.search_with_scores("火星旅游攻略"), [])

    def test_chat_request_rejects_oversized_message(self):
        with self.assertRaises(ValidationError):
            ChatRequest(session_id="session-1", user_id="u1", message="太长" * 1000)

    def test_chat_request_rejects_unsafe_session_id(self):
        with self.assertRaises(ValidationError):
            ChatRequest(session_id="../../etc/passwd", user_id="u1", message="你好")

    def test_backend_client_unwraps_success_result(self):
        payload = {"success": True, "data": {"id": 1, "name": "店铺"}}

        self.assertEqual(BackendClient._unwrap_result(payload), {"id": 1, "name": "店铺"})

    def test_backend_client_raises_business_error(self):
        payload = {"success": False, "errorCode": "UNAUTHORIZED", "errorMsg": "登录已过期"}

        with self.assertRaises(BackendBusinessError) as context:
            BackendClient._unwrap_result(payload)

        self.assertIn("UNAUTHORIZED", str(context.exception))
        self.assertIn("登录已过期", str(context.exception))

    def test_append_to_history_keeps_recent_rounds_only(self):
        from langchain_community.chat_message_histories import ChatMessageHistory

        history = ChatMessageHistory()
        for idx in range(8):
            append_to_history(history, f"user-{idx}", f"assistant-{idx}", max_messages=6)

        self.assertEqual(len(history.messages), 6)
        self.assertEqual(history.messages[0].content, "user-5")
        self.assertEqual(history.messages[-1].content, "assistant-7")

    def test_chat_response_has_default_suggestions(self):
        response = ChatResponse(answer="你好")

        self.assertEqual(response.suggestions, [])

    def test_chat_response_serializes_agent_metadata(self):
        response = ChatResponse(
            answer="已帮你查询",
            intent="order",
            trace_id="trace-123",
            error_code=None,
        )

        payload = response.model_dump()

        self.assertEqual(payload["intent"], "order")
        self.assertEqual(payload["trace_id"], "trace-123")
        self.assertIsNone(payload["error_code"])

    def test_build_fallback_response_contains_demo_suggestions(self):
        response = build_fallback_response("MODEL_NOT_CONFIGURED")

        self.assertIn("智能客服模型还没有配置完成", response.answer)
        self.assertIn("查店铺优惠", response.suggestions)
        self.assertIn("订单履约状态", response.suggestions)

    def test_build_fallback_response_sets_error_metadata(self):
        response = build_fallback_response(
            "MODEL_NOT_CONFIGURED",
            intent="fallback",
            trace_id="trace-abc",
        )

        self.assertEqual(response.error_code, "MODEL_NOT_CONFIGURED")
        self.assertEqual(response.intent, "fallback")
        self.assertEqual(response.trace_id, "trace-abc")
        self.assertNotIn("sk-", response.answer)

    def test_build_suggestions_handles_refund_intent(self):
        suggestions = build_suggestions("我想取消订单并退款", [], intent="refund")

        self.assertIn("订单履约状态", suggestions)
        self.assertIn("转人工", suggestions)

    def test_detect_intent_routes_common_messages(self):
        examples = {
            "这家店铺地址在哪里": Intent.SHOP,
            "1号店铺有什么优惠券": Intent.VOUCHER,
            "帮我查一下订单123的秒杀状态": Intent.ORDER,
            "这个订单可以退款吗": Intent.REFUND,
            "平台登录规则是什么": Intent.RULE,
            "你好": Intent.FALLBACK,
        }

        for message, expected in examples.items():
            with self.subTest(message=message):
                self.assertEqual(detect_intent(message), expected)

    def test_detect_intent_prioritizes_refund_over_order(self):
        self.assertEqual(detect_intent("订单123怎么取消退款"), Intent.REFUND)

    def test_build_tools_limits_tools_by_intent(self):
        api_client = object()
        kb = object()

        tools = build_tools(api_client, kb, Intent.ORDER)
        names = [tool.name for tool in tools]

        self.assertEqual(names, ["query_order_fulfillment", "query_local_knowledge"])

    def test_make_tool_error_returns_structured_json(self):
        payload = json.loads(make_tool_error("BACKEND_TIMEOUT", "后端服务响应超时，请稍后重试"))

        self.assertFalse(payload["ok"])
        self.assertEqual(payload["error_code"], "BACKEND_TIMEOUT")
        self.assertEqual(payload["message"], "后端服务响应超时，请稍后重试")

    def test_classify_tool_error_reads_structured_json(self):
        output = make_tool_error("BACKEND_ERROR", "后端返回业务错误")

        self.assertEqual(classify_tool_error(output), "BACKEND_ERROR")

    def test_query_local_knowledge_includes_citation_metadata(self):
        import tempfile

        with tempfile.TemporaryDirectory() as tmp_dir:
            doc_path = Path(tmp_dir) / "faq.md"
            doc_path.write_text("# 客服 FAQ\n\n## 秒杀订单状态\n\n秒杀失败通常因为库存不足。\n", encoding="utf-8")
            kb = LocalKnowledgeBase(str(Path(tmp_dir) / "*.md"))
            kb.rebuild()
            tools = build_tools(object(), kb, Intent.RULE)
            knowledge_tool = next(tool for tool in tools if tool.name == "query_local_knowledge")

            output = knowledge_tool.invoke({"question": "秒杀失败原因"})

            self.assertIn("chunk_id=faq.md#chunk1", output)
            self.assertIn("section=秒杀订单状态", output)
            self.assertIn("confidence=", output)
            self.assertIn("matched=", output)

    def test_eval_report_summary_formats_pass_rate(self):
        from evals.run_agent_eval import format_report

        report = format_report(total=4, failures=[])

        self.assertIn("total=4", report)
        self.assertIn("passed=4", report)
        self.assertIn("pass_rate=100.00%", report)

    def test_system_prompt_mentions_intent_constraints(self):
        from app.chains import SYSTEM_PROMPT

        self.assertIn("当前识别意图", SYSTEM_PROMPT)
        self.assertIn("只能使用当前提供的工具", SYSTEM_PROMPT)
        self.assertIn("退款", SYSTEM_PROMPT)

    def test_extract_slots_reads_order_and_shop_ids(self):
        self.assertEqual(extract_slots("帮我查订单123456状态").get("order_id"), "123456")
        self.assertEqual(extract_slots("1号店铺有什么优惠券").get("shop_id"), "1")

    def test_build_slot_status_requests_missing_order_id(self):
        status = build_slot_status(Intent.ORDER, "帮我查订单状态")

        self.assertEqual(status.intent, Intent.ORDER)
        self.assertEqual(status.missing_slot, "order_id")
        self.assertIn("订单号", status.follow_up)

    def test_pending_task_store_resumes_when_user_provides_slot(self):
        store = PendingTaskStore()
        first = build_slot_status(Intent.ORDER, "帮我查订单状态")
        store.save("session-1", first)

        resumed = store.resume_if_possible("session-1", "123456")

        self.assertIsNotNone(resumed)
        self.assertEqual(resumed.intent, Intent.ORDER)
        self.assertEqual(resumed.slots.get("order_id"), "123456")
        self.assertIsNone(store.get("session-1"))

    def test_pending_task_store_does_not_resume_explicit_different_intent(self):
        store = PendingTaskStore()
        first = build_slot_status(Intent.ORDER, "帮我查订单状态")
        store.save("session-1", first)

        resumed = store.resume_if_possible("session-1", "1号店铺有什么优惠券", current_intent=Intent.VOUCHER)

        self.assertIsNone(resumed)
        self.assertIsNone(store.get("session-1"))

    def test_build_slot_follow_up_response_returns_traceable_question(self):
        status = build_slot_status(Intent.VOUCHER, "帮我查优惠券")

        response = build_slot_follow_up_response(status, trace_id="trace-slot")

        self.assertEqual(response.intent, "voucher")
        self.assertEqual(response.trace_id, "trace-slot")
        self.assertEqual(response.error_code, "MISSING_SLOT")
        self.assertIn("店铺ID", response.answer)

    def test_deepseek_settings_are_read_from_environment(self):
        with patch.dict(
            "os.environ",
            {
                "DEEPSEEK_API_KEY": "sk-test",
                "OPENAI_BASE_URL": "https://api.deepseek.com",
                "LLM_MODEL": "deepseek-v4-pro",
                "LLM_REASONING_EFFORT": "high",
                "LLM_THINKING_ENABLED": "true",
            },
            clear=False,
        ):
            settings = get_settings()

        self.assertEqual(settings.openai_api_key, "sk-test")
        self.assertEqual(settings.openai_base_url, "https://api.deepseek.com")
        self.assertEqual(settings.llm_model, "deepseek-v4-pro")
        self.assertEqual(settings.llm_reasoning_effort, "high")
        self.assertTrue(settings.llm_thinking_enabled)

    def test_build_cards_from_shop_and_voucher_tool_steps(self):
        class Action:
            def __init__(self, tool):
                self.tool = tool

        steps = [
            (Action("query_shop_detail"), '{"id":1,"name":"103茶餐厅","address":"金华路","avgPrice":80,"score":37,"openHours":"10:00-22:00","images":"a.jpg,b.jpg"}'),
            (Action("query_shop_vouchers"), '{"data":[{"id":1,"shopId":1,"title":"50元代金券","subTitle":"周一至周日均可使用","type":0,"payValue":4750,"actualValue":5000}]}'),
        ]

        cards = build_cards_from_steps(steps)

        self.assertEqual(cards[0].type, "shop")
        self.assertEqual(cards[0].title, "103茶餐厅")
        self.assertEqual(cards[1].type, "voucher")
        self.assertEqual(cards[1].title, "50元代金券")

    def test_build_cards_from_knowledge_sources(self):
        class Action:
            tool = "query_local_knowledge"

        steps = [
            (Action(), "[1] source=../docs/customer-service-faq.md#chunk1\n登录规则\n\n[2] source=../docs/seckill-module.md#chunk2\n秒杀规则"),
        ]

        cards = build_cards_from_steps(steps)

        self.assertEqual(cards[0].type, "reference")
        self.assertIn("customer-service-faq.md", cards[0].title)
        self.assertEqual(cards[0].meta.get("chunk"), "1")


if __name__ == "__main__":
    unittest.main()
