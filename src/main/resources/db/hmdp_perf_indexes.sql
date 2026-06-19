-- Performance indexes (idempotent) for hmdp

-- tb_blog: hot list / user list queries
SET @s := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'tb_blog' AND index_name = 'idx_blog_status_liked');
SET @ddl := IF(@s = 0, 'ALTER TABLE tb_blog ADD INDEX idx_blog_status_liked (status, liked)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'tb_blog' AND index_name = 'idx_blog_user_status_time');
SET @ddl := IF(@s = 0, 'ALTER TABLE tb_blog ADD INDEX idx_blog_user_status_time (user_id, status, create_time)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- tb_follow: follower/following relation queries
SET @s := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'tb_follow' AND index_name = 'idx_follow_user_follow');
SET @ddl := IF(@s = 0, 'ALTER TABLE tb_follow ADD INDEX idx_follow_user_follow (user_id, follow_user_id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'tb_follow' AND index_name = 'idx_follow_follow_user');
SET @ddl := IF(@s = 0, 'ALTER TABLE tb_follow ADD INDEX idx_follow_follow_user (follow_user_id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- tb_blog_comments: list top comments / replies
SET @s := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'tb_blog_comments' AND index_name = 'idx_comment_blog_parent_status_time');
SET @ddl := IF(@s = 0, 'ALTER TABLE tb_blog_comments ADD INDEX idx_comment_blog_parent_status_time (blog_id, parent_id, status, create_time)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'tb_blog_comments' AND index_name = 'idx_comment_parent_status_time');
SET @ddl := IF(@s = 0, 'ALTER TABLE tb_blog_comments ADD INDEX idx_comment_parent_status_time (parent_id, status, create_time)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- tb_voucher: voucher list queries in admin/shop
SET @s := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'tb_voucher' AND index_name = 'idx_voucher_shop_status_type');
SET @ddl := IF(@s = 0, 'ALTER TABLE tb_voucher ADD INDEX idx_voucher_shop_status_type (shop_id, status, type)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'tb_voucher' AND index_name = 'idx_voucher_status_create_time');
SET @ddl := IF(@s = 0, 'ALTER TABLE tb_voucher ADD INDEX idx_voucher_status_create_time (status, create_time)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- tb_voucher_order: order query and status tracking
SET @s := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'tb_voucher_order' AND index_name = 'idx_order_user_create_time');
SET @ddl := IF(@s = 0, 'ALTER TABLE tb_voucher_order ADD INDEX idx_order_user_create_time (user_id, create_time)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'tb_voucher_order' AND index_name = 'idx_order_voucher_status');
SET @ddl := IF(@s = 0, 'ALTER TABLE tb_voucher_order ADD INDEX idx_order_voucher_status (voucher_id, status)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
