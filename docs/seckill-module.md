# 秒杀模块实现沉淀（当前实现）

本文档总结当前项目的秒杀模块实现、关键链路、失败分支与运维能力，便于后续维护与迭代。

## 1. 架构总览

当前实现采用：

- **Redis + Lua**：做秒杀资格原子校验（时间窗、库存、一人一单）
- **RabbitMQ 异步下单**：削峰填谷，提升高并发稳定性
- **MySQL 最终落库**：订单与库存最终一致性
- **DLQ 死信队列运维**：支持查看、预演、重放
- **状态查询接口**：前端轮询 `PENDING/SUCCESS/FAIL`

---

## 2. 主链路时序图（含失败分支）

```mermaid
sequenceDiagram
    autonumber
    participant U as 用户/前端
    participant API as VoucherOrderController
    participant S as VoucherOrderServiceImpl
    participant R as Redis(Lua)
    participant MQP as MQSender
    participant MQ as RabbitMQ(seckillQueue)
    participant C as MQReceiver
    participant DB as MySQL
    participant DLQ as RabbitMQ(DLQ)

    U->>API: POST /voucher-order/seckill/{voucherId}
    API->>S: seckillVoucher(voucherId)

    S->>S: RateLimiter限流
    alt 限流失败
        S-->>U: fail("网络正忙，请重试")
    else 限流通过
        S->>R: 执行 seckill.lua(库存/时间/一人一单)
        alt Lua返回非0(无资格)
            R-->>S: 1/2/3/4/5
            S-->>U: 库存不足/重复下单/未开始/已结束/未初始化
        else Lua返回0(有资格)
            R-->>S: 0
            S->>S: 生成orderId
            S->>R: set seckill:pending:{orderId}
            S->>MQP: sendSeckillMessage(orderJson)

            alt MQ发送失败(无confirm/异常)
                MQP-->>S: throw Exception
                S->>R: 执行 seckill_rollback.lua(回补库存+移除用户占位)
                S->>R: set seckill:result:{orderId}=FAIL:消息投递失败
                S-->>U: fail("下单请求拥堵，请重试")
            else MQ发送成功
                MQP-->>S: ack
                S-->>U: ok({orderId,state:PENDING})
                MQP->>MQ: 投递消息
            end
        end
    end

    Note over MQ,C: 异步消费阶段
    MQ->>C: @RabbitListener receive(msg)
    C->>DB: 插入tb_voucher_order(唯一键 user_id+voucher_id)

    alt 重复消息(唯一键冲突)
        DB-->>C: DuplicateKeyException
        C->>R: del pending
        C->>R: set result=SUCCESS:重复消息已忽略
    else 插入成功
        C->>DB: update tb_seckill_voucher set stock=stock-1 where stock>0
        alt DB扣库存失败
            DB-->>C: 0 rows
            C->>R: del pending
            C->>R: set result=FAIL:库存不足
            C->>DLQ: reject不重回主队列(进入DLQ)
        else DB扣库存成功
            DB-->>C: success
            C->>R: del pending
            C->>R: set result=SUCCESS
        end
    end
```

---

## 3. 状态查询时序图（前端轮询）

```mermaid
sequenceDiagram
    autonumber
    participant U as 前端
    participant API as /voucher-order/status/{orderId}
    participant S as VoucherOrderServiceImpl
    participant DB as MySQL
    participant R as Redis

    loop 每1秒轮询(最多N次)
        U->>API: GET status/{orderId}
        API->>S: querySeckillOrder(orderId)
        S->>DB: 查订单(id+user_id)

        alt DB有订单
            S-->>U: SUCCESS
        else DB无订单
            S->>R: get seckill:result:{orderId}
            alt 有最终结果
                S-->>U: FAIL/UNKNOWN
            else 无最终结果
                S->>R: exists seckill:pending:{orderId}
                alt pending存在
                    S-->>U: PENDING
                else pending不存在
                    S-->>U: 订单不存在或超时
                end
            end
        end
    end
```

---

## 4. 关键 Redis Key 设计

- `seckill:stock:{voucherId}`：秒杀库存
- `seckill:begin:{voucherId}`：开始时间（epoch秒）
- `seckill:end:{voucherId}`：结束时间（epoch秒）
- `seckill:order:{voucherId}`：已下单用户集合（set）
- `seckill:pending:{orderId}`：异步处理中标记
- `seckill:result:{orderId}`：最终结果（SUCCESS/FAIL...）

说明：
- Lua 成功路径会为活动 key 设置过期时间（活动结束后 +1天），避免历史 key 长驻。
- MQ发送失败场景使用 `seckill_rollback.lua` 回滚 Redis 资格占位。

---

## 5. 数据库约束与幂等

- 订单表 `tb_voucher_order` 增加唯一约束：
  - `uk_user_voucher(user_id, voucher_id)`
- 消费端先插订单，遇到唯一键冲突视为重复消息，直接幂等返回。

---

## 6. MQ 配置与可靠性

- 发送端：
  - 开启 publisher confirm / returns / mandatory
  - 发送时等待 broker ack
- 消费端：
  - 库存不足等不可重试异常 `reject and dont requeue`
  - 路由到 DLQ，避免无限重试风暴
- 队列拓扑：
  - 主队列：`seckillQueue`
  - 主交换机：`seckillExchange`
  - 死信交换机：`seckillDlxExchange`
  - 死信队列：`seckillQueue.dlq`

---

## 7. 管理端 DLQ 运维能力

### 7.1 查询死信

- `GET /admin/seckill/dlq?limit=20&voucherId={可选}`
- 支持按 voucherId 过滤

### 7.2 预演重放（Dry-run）

- `POST /admin/seckill/dlq/replay/preview?limit=20&voucherId={可选}`
- 仅扫描统计，不实际重放
- 返回：`requested/canReplay/scanned/remaining/sampleOrderIds`

### 7.3 真实重放

- `POST /admin/seckill/dlq/replay?limit=20&voucherId={可选}`
- 支持按 voucherId 精确重放
- 返回：`replayed/failed/scanned/remaining/replayedOrderIds`

### 7.4 审计

- 预演与重放均有 `@AdminActionLog` 记录
- 日志 detail 中包含 method/uri/query/resultData 等上下文

---

## 8. 前端交互（秒杀页）

- 点击“立即抢购”后，先收到 `PENDING`
- 前端轮询 `/voucher-order/status/{orderId}`
- 根据状态更新文案：
  - `PENDING`：排队中
  - `SUCCESS`：下单成功
  - `FAIL`：下单失败（带原因）

后台“秒杀死信”页面支持：
- 查看积压
- 预演重放
- 重放确认弹窗
- 样本订单号一键复制

---

## 9. 主要实现文件索引

### 后端

- `src/main/java/com/hmdp/service/impl/VoucherOrderServiceImpl.java`
- `src/main/resources/seckill.lua`
- `src/main/resources/seckill_rollback.lua`
- `src/main/java/com/hmdp/rebbitmq/MQSender.java`
- `src/main/java/com/hmdp/rebbitmq/MQReceiver.java`
- `src/main/java/com/hmdp/config/RabbitMQTopicConfig.java`
- `src/main/java/com/hmdp/controller/VoucherOrderController.java`
- `src/main/java/com/hmdp/controller/AdminSeckillController.java`
- `src/main/java/com/hmdp/aspect/AdminActionLogAspect.java`

### 前端

- `frontend/src/pages/ShopDetailPage.tsx`
- `frontend/src/pages/admin/AdminSeckillDlqPage.tsx`
- `frontend/src/services/modules/voucher.ts`
- `frontend/src/services/modules/admin.ts`
- `frontend/src/services/types.ts`

---

## 10. 现状与后续建议（可选）

- 当前 `RateLimiter` 为单机限流；多实例可考虑网关级限流 + 分布式限流。
- 可增加“重放任务批次号”，用于更强的回溯与审计。
- 可增加“自动对账任务”（Redis库存 vs DB库存）用于巡检。

