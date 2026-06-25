from __future__ import annotations

from langchain.agents import AgentExecutor, create_tool_calling_agent
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain_openai import ChatOpenAI


SYSTEM_PROMPT = """
你是电商平台的智能客服助手，请遵循：
1. 当前识别意图是：{intent}。回答和工具调用必须围绕这个意图展开。
2. 只能使用当前提供的工具，不要假设还有其他隐藏工具。
3. 涉及订单、优惠券、店铺信息时，优先调用工具查询后再回答。
4. 涉及活动规则、说明文档时，优先检索本地知识库；如果检索结果包含 source，请在回答中简要说明来源文件名。
5. 回答简洁清晰，必要时给出下一步操作建议。
6. 严禁编造不存在的订单状态、优惠券规则或退款结果。
7. 未获得工具明确确认前，不要声称已经完成退款、取消订单、支付等写操作。
""".strip()


def build_agent_executor(
    llm_model: str,
    openai_api_key: str,
    openai_base_url: str,
    tools,
    reasoning_effort: str = "high",
    thinking_enabled: bool = False,
    intent: str = "fallback",
) -> AgentExecutor:
    extra_body = {"thinking": {"type": "enabled"}} if thinking_enabled else None
    llm = ChatOpenAI(
        model=llm_model,
        api_key=openai_api_key,
        base_url=openai_base_url,
        temperature=0.2,
        reasoning_effort=reasoning_effort,
        extra_body=extra_body,
    )

    prompt = ChatPromptTemplate.from_messages(
        [
            ("system", SYSTEM_PROMPT),
            MessagesPlaceholder("chat_history"),
            ("human", "{input}"),
            MessagesPlaceholder("agent_scratchpad"),
        ]
    ).partial(intent=intent)

    agent = create_tool_calling_agent(llm, tools, prompt)
    return AgentExecutor(agent=agent, tools=tools, return_intermediate_steps=True, verbose=False)
