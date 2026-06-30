-- 已有库增量：成就「连续学习」统计用（新库请直接执行 init.sql）
-- 可重复执行：字段已存在时不会再次添加。
USE anti_fraud_platform;

DROP PROCEDURE IF EXISTS patch_sys_user_last_login;
DELIMITER //
CREATE PROCEDURE patch_sys_user_last_login()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_user'
          AND COLUMN_NAME = 'last_login_time'
    ) THEN
        ALTER TABLE `sys_user`
            ADD COLUMN `last_login_time` DATETIME NULL COMMENT '最后登录时间(成就连续学习统计)' AFTER `update_time`;
    END IF;
END//
DELIMITER ;

CALL patch_sys_user_last_login();
DROP PROCEDURE IF EXISTS patch_sys_user_last_login;
