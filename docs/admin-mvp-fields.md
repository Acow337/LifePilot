# 后台管理 MVP 字段定义（前端对接版）

## 1) 用户管理 `/admin/users`

### 列表查询
- 请求：`GET /admin/users?page=1&size=10&keyword=&status=`
- 响应：`Result.data` 为用户数组，`Result.total` 为总数

### 表格列
- `id`：用户ID
- `phone`：账号（邮箱/手机号）
- `nickName`：昵称
- `role`：角色（0普通用户，1管理员）
- `status`：状态（0正常，1封禁）
- `createTime`：注册时间

### 操作按钮
- 封禁/解封：`PATCH /admin/users/{id}/status` body: `{ "status": 1|0 }`
- 设为管理员/普通用户：`PATCH /admin/users/{id}/role` body: `{ "role": 1|0 }`

---

## 2) 笔记审核 `/admin/blogs`

### 列表查询
- 请求：`GET /admin/blogs?page=1&size=10&status=&keyword=`

### 表格列
- `id`：笔记ID
- `title`：标题
- `name`：作者昵称
- `status`：状态（0正常，1下架）
- `liked`：点赞数
- `comments`：评论数
- `createTime`：发布时间

### 审核操作
- 请求：`PATCH /admin/blogs/{id}/review`
- body:
  - `action`: `APPROVE | REJECT | OFFLINE`
  - `reason`: 审核备注（可选）

> 当前后端版本中：`APPROVE -> status=0`，`REJECT/OFFLINE -> status=1`。

---

## 3) 旧接口兼容

- `PATCH /admin/users/{id}/ban`
- `PATCH /admin/users/{id}/unban`
- `PUT /admin/blogs/{id}/hide`
- `PUT /admin/blogs/{id}/restore`

用于兼容旧前端，建议新页面统一走新的 PATCH 接口。

---

## 4) 店铺管理 `/admin/shops`

### 列表查询
- 请求：`GET /admin/shops?page=1&size=10&keyword=&typeId=`

### 表格列
- `id`：店铺ID
- `name`：店铺名
- `typeId`：店铺类型
- `area`：商圈
- `address`：地址
- `avgPrice`：均价
- `sold`：销量
- `score`：评分
- `createTime`：创建时间

### 操作
- 新建：`POST /admin/shops`
- 修改：`PUT /admin/shops/{id}`

---

## 5) 优惠券管理 `/admin/vouchers`

### 列表查询
- 请求：`GET /admin/vouchers?page=1&size=10&type=&shopId=&status=&title=`

### 表格列
- `id`：券ID
- `shopId`：店铺ID
- `title`：标题
- `type`：类型（1秒杀券）
- `status`：状态（1生效 0下架）
- `payValue`：支付金额
- `actualValue`：抵扣金额
- `stock`：库存（秒杀券）
- `beginTime/endTime`：秒杀开始/结束时间

### 操作
- 新建：`POST /admin/vouchers`
- 修改：`PUT /admin/vouchers/{id}`
- 上下架：`PATCH /admin/vouchers/{id}/status` body: `{ "status": 1|0 }`

---

## 6) 操作日志 `/admin/logs`

- 请求：`GET /admin/logs?page=1&size=20&operatorId=&module=&action=`
- 返回：管理员操作记录（按 `createTime` 倒序）
- 字段：`operatorId/module/action/targetType/targetId/detail/createTime`
