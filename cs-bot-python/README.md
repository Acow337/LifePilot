# cs-bot-python (LangChain 智能客服 MVP)

一个可独立运行的 Python 客服服务，提供：

- `/chat`：客服对话（LangChain Agent）
- 可调用业务工具（店铺、优惠券、订单状态）
- 本地文档知识检索（默认读取 `../docs/*.md`）

## 1. 安装

```bash
cd cs-bot-python
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
```

## 2. 配置

编辑 `.env`：

- `DEEPSEEK_API_KEY` / `OPENAI_BASE_URL` / `LLM_MODEL`
- DeepSeek 示例：`OPENAI_BASE_URL=https://api.deepseek.com`、`LLM_MODEL=deepseek-v4-pro`、`LLM_REASONING_EFFORT=high`、`LLM_THINKING_ENABLED=true`
- `BACKEND_BASE_URL`（你的 Java 后端）

## 3. 启动

```bash
uvicorn app.main:app --host 127.0.0.1 --port 9000 --reload
```

## 4. 调用示例

### 健康检查

```bash
curl http://127.0.0.1:9000/health
```

### 聊天

```bash
curl -X POST http://127.0.0.1:9000/chat \
  -H 'Content-Type: application/json' \
  -d '{
    "session_id":"s1",
    "user_id":"u1001",
    "message":"我想看一下1号店铺有什么优惠券"
  }'
```

### 查询订单状态（让 Agent 自动调用工具）

```bash
curl -X POST http://127.0.0.1:9000/chat \
  -H 'Content-Type: application/json' \
  -d '{
    "session_id":"s1",
    "user_id":"u1001",
    "message":"帮我查一下订单123456789的秒杀状态"
  }'
```

## 5. 与前端集成建议

- 前端新增聊天弹窗，调用 `/chat`
- 每个用户维持 `session_id`
- 对需要鉴权的查询，将登录 token 透传给 `/chat` 的 `user_token`
- 前端展示 `suggestions` 作为追问按钮，可用于演示常见客服路径。

## 6. 生产建议

- 将会话历史从内存迁移到 Redis
- 增加敏感词与越权策略
- 增加埋点：工具调用成功率、平均时延、失败码分布

## 7. 稳定性说明

- `.env` 与 `.venv/` 不应提交到 Git，仓库只保留 `.env.example`。
- `session_id` / `user_id` 仅支持字母、数字、`_`、`-`、`:`，单条消息最长 1000 字符。
- 当前内存会话只保留最近 12 条消息，生产环境建议替换为 Redis 并设置 TTL。
- Java 后端返回 `success=false` 时，客服工具会抛出业务错误并交给 Agent 解释。
- 未配置模型或模型调用失败时，`/chat` 会返回兜底话术和快捷建议，不会让前端空白。
