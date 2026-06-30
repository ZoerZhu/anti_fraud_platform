-- Add final_score column to scenario_progress table
-- 可重复执行：字段已存在时不会再次添加。

USE anti_fraud_platform;

DROP PROCEDURE IF EXISTS patch_scenario_progress_final_score;
DELIMITER //
CREATE PROCEDURE patch_scenario_progress_final_score()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'scenario_progress'
          AND COLUMN_NAME = 'final_score'
    ) THEN
        ALTER TABLE `scenario_progress`
            ADD COLUMN `final_score` INT DEFAULT 0 COMMENT 'final score' AFTER `start_time`;
    END IF;
END//
DELIMITER ;

CALL patch_scenario_progress_final_score();
DROP PROCEDURE IF EXISTS patch_scenario_progress_final_score;
