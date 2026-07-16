-- 后台管理 MVP 扩展脚本（按需执行）

-- 1) 管理员操作日志
CREATE TABLE IF NOT EXISTS `tb_admin_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `operator_id` BIGINT UNSIGNED NOT NULL COMMENT '操作管理员ID',
  `module` VARCHAR(32) NOT NULL COMMENT '模块:user/blog/shop/voucher',
  `action` VARCHAR(32) NOT NULL COMMENT '动作:update_status/review/...',
  `target_type` VARCHAR(32) NOT NULL COMMENT '对象类型:user/blog/shop/voucher',
  `target_id` BIGINT UNSIGNED NOT NULL COMMENT '对象ID',
  `detail` JSON NULL COMMENT '扩展详情',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_operator_time` (`operator_id`, `create_time`),
  KEY `idx_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2) 笔记审核字段（可选）
-- 当前代码已可跑在仅有 status 字段的结构上。
-- 如果要区分“驳回”和“下架”原因，建议补充以下字段：
ALTER TABLE `tb_blog`
  ADD COLUMN IF NOT EXISTS `review_status` TINYINT(1) UNSIGNED NOT NULL DEFAULT 0 COMMENT '0待审核 1通过 2驳回 3下架',
  ADD COLUMN IF NOT EXISTS `review_reason` VARCHAR(255) NULL COMMENT '审核备注',
  ADD COLUMN IF NOT EXISTS `review_time` DATETIME NULL COMMENT '审核时间',
  ADD COLUMN IF NOT EXISTS `review_admin_id` BIGINT UNSIGNED NULL COMMENT '审核管理员ID';

-- 3) 秒杀订单一人一单唯一约束（防重兜底）
ALTER TABLE `tb_voucher_order`
  ADD UNIQUE KEY IF NOT EXISTS `uk_user_voucher` (`user_id`, `voucher_id`);

-- 4) 评论查询索引（评论列表/用户评论列表/回复查询）
ALTER TABLE `tb_blog_comments`
  ADD INDEX `idx_blog_time` (`blog_id`, `create_time`),
  ADD INDEX `idx_user_time` (`user_id`, `create_time`),
  ADD INDEX `idx_parent_id` (`parent_id`);

-- 5) LifePilot 智能营销履约扩展：活动中心 / 库存账本 / 状态机 / Agent 可观测性
CREATE TABLE IF NOT EXISTS `tb_campaign` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `voucher_id` BIGINT UNSIGNED NULL COMMENT '关联优惠券ID，兼容存量券模型',
  `shop_id` BIGINT UNSIGNED NOT NULL COMMENT '适用店铺ID',
  `name` VARCHAR(128) NOT NULL COMMENT '活动名称',
  `type` VARCHAR(32) NOT NULL DEFAULT 'SECKILL' COMMENT 'SECKILL/NORMAL/NEW_USER/MEMBER/FULL_REDUCTION',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0草稿 1上线 2暂停 3结束',
  `stock_total` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '总库存',
  `stock_available` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '可用库存',
  `budget_cent` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '预算上限，单位分',
  `begin_time` DATETIME NULL COMMENT '开始时间',
  `end_time` DATETIME NULL COMMENT '结束时间',
  `description` VARCHAR(512) NULL COMMENT '运营说明',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_campaign_shop_status` (`shop_id`, `status`),
  KEY `idx_campaign_voucher` (`voucher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LifePilot营销活动主表';

CREATE TABLE IF NOT EXISTS `tb_campaign_rule` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `campaign_id` BIGINT UNSIGNED NOT NULL COMMENT '活动ID',
  `rule_type` VARCHAR(32) NOT NULL COMMENT 'LIMIT/TIME_WINDOW/CROWD/BUDGET/SHOP_SCOPE',
  `rule_config` JSON NOT NULL COMMENT '规则配置JSON',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_campaign_rule` (`campaign_id`, `rule_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='营销活动规则';

CREATE TABLE IF NOT EXISTS `tb_inventory_ledger` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `campaign_id` BIGINT UNSIGNED NULL COMMENT '活动ID',
  `voucher_id` BIGINT UNSIGNED NULL COMMENT '优惠券ID',
  `order_id` BIGINT UNSIGNED NULL COMMENT '订单ID',
  `change_type` VARCHAR(32) NOT NULL COMMENT 'RESERVE/DEDUCT/RELEASE/ROLLBACK/REPLAY',
  `change_amount` INT NOT NULL COMMENT '库存变化，扣减为负，释放为正',
  `before_stock` INT NULL COMMENT '变更前库存快照',
  `after_stock` INT NULL COMMENT '变更后库存快照',
  `source` VARCHAR(64) NOT NULL COMMENT '来源：seckill/order/refund/dlq/admin',
  `detail` JSON NULL COMMENT '扩展详情',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_inventory_campaign_time` (`campaign_id`, `create_time`),
  KEY `idx_inventory_order` (`order_id`),
  KEY `idx_inventory_voucher_time` (`voucher_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存账本';

CREATE TABLE IF NOT EXISTS `tb_order_state_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
  `user_id` BIGINT UNSIGNED NULL COMMENT '用户ID',
  `voucher_id` BIGINT UNSIGNED NULL COMMENT '优惠券ID',
  `from_status` TINYINT NULL COMMENT '原状态',
  `to_status` TINYINT NOT NULL COMMENT '新状态',
  `action` VARCHAR(32) NOT NULL COMMENT 'pay/redeem/cancel/refund/timeout_cancel',
  `operator_type` VARCHAR(32) NOT NULL DEFAULT 'system' COMMENT 'user/admin/system/agent',
  `operator_id` BIGINT UNSIGNED NULL COMMENT '操作人ID',
  `message` VARCHAR(255) NULL COMMENT '状态说明',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_state_log_order_time` (`order_id`, `create_time`),
  KEY `idx_order_state_log_action_time` (`action`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单状态机流转日志';

CREATE TABLE IF NOT EXISTS `tb_agent_trace` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `trace_id` VARCHAR(64) NOT NULL COMMENT 'Agent trace id',
  `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户标识',
  `intent` VARCHAR(64) NULL COMMENT '意图',
  `message` VARCHAR(1000) NOT NULL COMMENT '用户问题',
  `used_tools` JSON NULL COMMENT '工具列表',
  `error_code` VARCHAR(64) NULL COMMENT '失败码',
  `latency_ms` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '耗时',
  `answer_preview` VARCHAR(512) NULL COMMENT '回复摘要',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_trace_id` (`trace_id`),
  KEY `idx_agent_trace_session_time` (`session_id`, `create_time`),
  KEY `idx_agent_trace_intent_time` (`intent`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI Agent调用轨迹';

CREATE TABLE IF NOT EXISTS `tb_operation_event` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `event_type` VARCHAR(64) NOT NULL COMMENT '业务事件类型',
  `biz_type` VARCHAR(64) NOT NULL COMMENT 'campaign/order/agent/inventory',
  `biz_id` BIGINT UNSIGNED NULL COMMENT '业务ID',
  `payload` JSON NULL COMMENT '事件载荷',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_operation_event_type_time` (`event_type`, `create_time`),
  KEY `idx_operation_event_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运营事件埋点';

CREATE TABLE IF NOT EXISTS `tb_fulfillment_task` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
  `task_type` VARCHAR(32) NOT NULL COMMENT 'REDEEM/REFUND/EXCEPTION/MANUAL_REVIEW',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0待处理 1处理中 2已完成 3已关闭',
  `assignee_id` BIGINT UNSIGNED NULL COMMENT '处理人ID',
  `priority` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '优先级 1低 2中 3高',
  `summary` VARCHAR(255) NULL COMMENT '任务摘要',
  `detail` JSON NULL COMMENT '任务详情',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_fulfillment_task_order` (`order_id`),
  KEY `idx_fulfillment_task_status_time` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='履约任务';

CREATE TABLE IF NOT EXISTS `tb_agent_session` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户标识',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1进行中 2已解决 3转人工 4关闭',
  `last_intent` VARCHAR(64) NULL COMMENT '最近意图',
  `last_trace_id` VARCHAR(64) NULL COMMENT '最近trace',
  `summary` VARCHAR(512) NULL COMMENT '会话摘要',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_session_id` (`session_id`),
  KEY `idx_agent_session_user_time` (`user_id`, `update_time`),
  KEY `idx_agent_session_status_time` (`status`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI客服会话';

CREATE TABLE IF NOT EXISTS `tb_knowledge_article` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(128) NOT NULL COMMENT '知识标题',
  `category` VARCHAR(64) NOT NULL COMMENT 'refund/redeem/seckill/platform/merchant',
  `source` VARCHAR(255) NULL COMMENT '来源文件或链接',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0下线 1启用',
  `content` TEXT NOT NULL COMMENT '知识内容',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_knowledge_category_status` (`category`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服知识库元数据';
