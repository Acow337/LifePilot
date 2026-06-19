from pathlib import Path
import unittest
from unittest.mock import patch

from pydantic import ValidationError

from app.api_client import BackendBusinessError, BackendClient
from app.config import get_settings
from app.knowledge_base import LocalKnowledgeBase
from app.main import append_to_history, build_cards_from_steps, build_fallback_response
from app.models import ChatRequest, ChatResponse


class BotStabilityTest(unittest.TestCase):
    def test_empty_knowledge_base_is_searchable(self):
        import tempfile

        with tempfile.TemporaryDirectory() as tmp_dir:
            kb = LocalKnowledgeBase(str(Path(tmp_dir) / "*.md"))

            self.assertEqual(kb.rebuild(), 0)
            self.assertEqual(kb.search("优惠券规则"), [])

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

    def test_build_fallback_response_contains_demo_suggestions(self):
        response = build_fallback_response("OPENAI_API_KEY 未配置")

        self.assertIn("智能客服暂时无法连接模型", response.answer)
        self.assertIn("查店铺优惠", response.suggestions)
        self.assertIn("秒杀订单状态", response.suggestions)

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
