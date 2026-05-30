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
  ADD UNIQUE KEY `uk_user_voucher` (`user_id`, `voucher_id`);
