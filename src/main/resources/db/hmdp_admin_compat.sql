-- Admin schema compatibility script for MySQL 5.7+/8.0+

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

-- Optional: keep one-order-per-user-voucher invariant
SET @s := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'tb_voucher_order' AND index_name = 'uk_user_voucher');
SET @ddl := IF(@s = 0, 'ALTER TABLE tb_voucher_order ADD UNIQUE KEY uk_user_voucher (user_id, voucher_id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
