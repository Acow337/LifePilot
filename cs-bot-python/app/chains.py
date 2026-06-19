from __future__ import annotations

from langchain.agents import AgentExecutor, create_tool_calling_agent
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain_openai import ChatOpenAI


SYSTEM_PROMPT = """
你是电商平台的智能客服助手，请遵循：
1. 优先准确回答，不确定就说明不确定。
2. 涉及订单、优惠券、店铺信息时，优先调用工具查询后再回答。
3. 涉及活动规则、说明文档时，优先检索本地知识库。
4. 回答简洁清晰，必要时给出下一步操作建议。
5. 严禁编造不存在的订单状态或优惠券规则。
""".strip()


def build_agent_executor(
    llm_model: str,
    openai_api_key: str,
    openai_base_url: str,
    tools,
    reasoning_effort: str = "high",
    thinking_enabled: bool = False,
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
    )

    agent = create_tool_calling_agent(llm, tools, prompt)
    return AgentExecutor(agent=agent, tools=tools, return_intermediate_steps=True, verbose=False)
