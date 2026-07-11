# LifePilot

LifePilot 是一个面向本地生活场景的智能服务平台，融合点评业务、营销秒杀、订单履约和智能客服 Agent 能力。项目基于原黑马点评业务模型扩展了前后端页面、优惠券履约、秒杀异步下单、后台运营管理和 LangChain 智能客服，适合作为 Java 后端、Redis 高并发、RabbitMQ 异步削峰和 AI Agent 工程化实践的综合 Demo。

## 功能概览

### 用户端

- 手机号/验证码登录与登录态校验
- 首页店铺浏览、店铺分类、店铺详情
- 探店笔记发布、浏览、点赞、评论与回复
- 关注用户、粉丝关系与 Feed 推送
- 普通优惠券下单、支付、取消、核销、退款
- 秒杀优惠券抢购与订单状态轮询
- 前端智能客服聊天窗口

### 运营后台

- 管理员登录与后台布局
- 用户、店铺、探店笔记、优惠券管理
- 订单履约、退款审核、核销操作
- 秒杀死信队列查看、预演与重放
- 后台操作日志与数据看板

### 智能客服

- 独立 Python FastAPI 服务，默认端口 `9000`
- 基于 LangChain `AgentExecutor` + Tool Calling 构建客服 Agent
- 基于本地 Markdown 文档的 BM25 检索增强问答
- 可调用 Java 后端工具查询店铺、优惠券和秒杀订单状态
- 支持 `session_id` 多轮上下文、槽位补全、兜底回复、快捷建议和 trace id

## 技术栈

| 模块 | 技术 |
| --- | --- |
| 后端 | Spring Boot, MyBatis-Plus, MySQL, Redis, Lua, RabbitMQ, Redisson |
| 前端 | React, TypeScript, Vite |
| 智能客服 | Python, FastAPI, LangChain, httpx, BM25Retriever |
| 数据与脚本 | MySQL SQL Seed, Redis, RabbitMQ, Shell Scripts |

## 架构说明

```text
React 前端
  ├─ 调用 Java 后端 REST API
  └─ 调用 Python 智能客服 /chat

Python FastAPI 客服服务
  ├─ 意图识别与槽位补全
  ├─ LangChain Tool Calling Agent
  ├─ 本地 Markdown 知识库检索
  └─ 调用 Java 后端业务接口

Spring Boot 后端
  ├─ MySQL：用户、店铺、笔记、优惠券、订单、后台日志
  ├─ Redis：登录态、缓存、验证码频控、秒杀库存/状态
  ├─ RabbitMQ：秒杀异步下单与死信队列
  └─ Lua：秒杀资格原子校验与异常回滚
```

## 核心实现

### 高并发秒杀

- 使用 Redis + Lua 完成活动时间窗、库存、一人一单的原子校验
- 秒杀请求通过 RabbitMQ 异步下单，削峰并降低 MySQL 写入压力
- Redis 维护 `PENDING/SUCCESS/FAIL` 状态，前端可轮询查询订单处理结果
- MQ 投递失败时执行 Redis/Lua 回滚，消费失败进入死信队列
- 后台支持死信消息查看、预演、重放和操作日志记录

### 缓存与防刷

- 封装 `CacheClient` 支持缓存穿透、逻辑过期和热点数据缓存重建
- 使用 Redis ZSet + 时间窗口实现短信验证码多级频控
- 使用 Redis TTL、限流 Key 和失败升级策略降低恶意刷接口风险

### 订单履约

- 支持优惠券订单创建、支付、取消、核销、退款申请与退款审核
- 支持超时未支付订单关闭
- 通过状态码约束、事务控制、幂等校验和后台操作日志保证链路可追踪

### 智能客服 Agent

- 前端聊天组件调用 `cs-bot-python` 的 `/chat` 接口
- FastAPI 服务根据用户问题识别意图，并按意图裁剪可用工具列表
- LangChain Agent 先由 LLM 判断是否需要调用工具，再执行工具并基于结果生成回复
- 订单类问题调用 Java 后端 `/voucher-order/status/{id}` 查询状态
- 规则类问题检索 `docs/*.md` 本地知识库并返回来源信息
- 基于 `session_id` 维护最近多轮 `ChatMessageHistory`，并用 `PendingTaskStore` 支持缺参追问和任务恢复

## 目录结构

```text
.
├── src/main/java/com/hmdp        # Spring Boot 后端源码
├── src/main/resources            # 配置、Lua 脚本、MyBatis Mapper、SQL
├── src/test/java/com/hmdp        # Java 单元测试
├── frontend                      # React + Vite 前端
├── cs-bot-python                 # FastAPI + LangChain 智能客服服务
├── docs                          # 项目说明、客服知识库、秒杀模块文档
├── scripts                       # 本地启动、停止、数据库初始化脚本
└── nginx-1.18.0                  # 本地静态资源与图片目录
```

## 本地启动

### 1. 启动依赖服务

```bash
brew services start redis
brew services start mysql
brew services start rabbitmq
```

### 2. 初始化数据库

首次启动或数据库为空时执行：

```bash
./scripts/init-db.sh
```

### 3. 配置智能客服环境变量

```bash
cd cs-bot-python
cp .env.example .env
```

编辑 `cs-bot-python/.env`，至少配置模型密钥：

```env
DEEPSEEK_API_KEY=your_api_key
OPENAI_BASE_URL=https://api.deepseek.com
LLM_MODEL=deepseek-v4-pro
BACKEND_BASE_URL=http://127.0.0.1:8081
```

### 4. 启动后端、前端和客服

可以分别启动：

```bash
./scripts/start-backend-dev.sh
./scripts/start-frontend-dev.sh
./scripts/start-bot-dev.sh
```

也可以一键启动：

```bash
./scripts/start-all-dev.sh
```

## 服务地址

| 服务 | 地址 |
| --- | --- |
| 前端 | `http://127.0.0.1:5173` |
| Java 后端 | `http://127.0.0.1:8081` |
| 智能客服 | `http://127.0.0.1:9000` |

## 启动校验

后端：

```bash
curl http://127.0.0.1:8081/shop-type/list
```

前端：

```bash
curl http://127.0.0.1:5173
```

智能客服：

```bash
curl http://127.0.0.1:9000/health
```

客服对话示例：

```bash
curl -X POST http://127.0.0.1:9000/chat \
  -H 'Content-Type: application/json' \
  -d '{
    "session_id": "s1",
    "user_id": "u1001",
    "message": "帮我查一下订单123456的秒杀状态"
  }'
```

## 常用脚本

| 命令 | 说明 |
| --- | --- |
| `./scripts/init-db.sh` | 初始化本地 MySQL 数据库 |
| `./scripts/start-backend-dev.sh` | 启动 Spring Boot 后端 |
| `./scripts/start-frontend-dev.sh` | 启动 React 前端 |
| `./scripts/start-bot-dev.sh` | 启动智能客服服务 |
| `./scripts/start-all-dev.sh` | 一键启动后端、前端、客服 |
| `./scripts/stop-all-dev.sh` | 停止本地开发服务 |

## 测试

Java 后端测试：

```bash
mvn test
```

智能客服测试：

```bash
cd cs-bot-python
python -m unittest discover -s tests
```

客服评测脚本：

```bash
cd cs-bot-python
python evals/run_agent_eval.py
```

## 相关文档

- `docs/seckill-module.md`：秒杀模块实现说明
- `docs/customer-service-faq.md`：智能客服本地知识库 FAQ
- `cs-bot-python/README.md`：智能客服服务说明
- `docs/admin-mvp-fields.md`：后台管理字段说明

## 环境变量

后端本地开发默认值已在脚本和配置中覆盖，可按需设置：

- `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`, `DB_NAME`
- `REDIS_HOST`
- `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD`, `RABBITMQ_VHOST`
- `FRONTEND_HOST`, `FRONTEND_PORT`
- `HMDP_UPLOAD_DIR`

智能客服可配置：

- `DEEPSEEK_API_KEY` 或 `OPENAI_API_KEY`
- `OPENAI_BASE_URL`
- `LLM_MODEL`
- `LLM_REASONING_EFFORT`
- `LLM_THINKING_ENABLED`
- `BACKEND_BASE_URL`
- `KNOWLEDGE_GLOB`
- `MAX_HISTORY_MESSAGES`

## 说明

本项目用于本地学习和演示，默认配置面向本机开发环境。生产环境使用时需要补充权限隔离、敏感信息管理、日志脱敏、限流熔断、监控告警和持久化会话存储等能力。
