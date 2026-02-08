-- 数据库初始化脚本

CREATE DATABASE IF NOT EXISTS thinking_help DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE thinking_help;

-- Users Table
CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '账号 (唯一的登录标识)',
    `password_hash` VARCHAR(100) NOT NULL COMMENT '加密密码',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '用户名 (显示昵称)',
    `gender` INT DEFAULT 2 COMMENT '性别 (0:女, 1:男, 2:未知)',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `role` VARCHAR(20) DEFAULT 'USER' COMMENT '角色 (USER/ADMIN)',
    `memberLevel` INT DEFAULT 0 COMMENT '会员等级 (0: 普通用户, 1: VIP)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户基础信息表';

-- Initialize Admin User (Password: admin123, MD5: 0192023a7bbd73250516f069df18b500)
INSERT INTO `users` (username, password_hash, nickname, role, member_level) 
SELECT 'admin', '0192023a7bbd73250516f069df18b500', 'Administrator', 'ADMIN', 1 
WHERE NOT EXISTS (SELECT * FROM `users` WHERE username = 'admin');

-- Health Profiles Table
CREATE TABLE IF NOT EXISTS `health_profiles` (
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `dynamic_tags` JSON COMMENT '健康标签 (如 ["高血压", "海鲜过敏"])',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户健康档案表';

-- Health Metrics Table
CREATE TABLE IF NOT EXISTS `health_metrics` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `metric_type` VARCHAR(50) NOT NULL COMMENT '指标类型',
    `value` DOUBLE NOT NULL COMMENT '数值',
    `unit` VARCHAR(20) DEFAULT '' COMMENT '单位',
    `recorded_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    `note` VARCHAR(255) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康数据记录表';

-- Health Records Table (Wide Format)
CREATE TABLE IF NOT EXISTS `health_records` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `height` DOUBLE COMMENT '身高(cm)',
    `weight` DOUBLE COMMENT '体重(kg)',
    `systolic` DOUBLE COMMENT '收缩压(mmHg)',
    `diastolic` DOUBLE COMMENT '舒张压(mmHg)',
    `glucose` DOUBLE COMMENT '血糖(mmol/L)',
    `heart_rate` INT COMMENT '心率(bpm)',
    `recorded_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康综合记录表';

-- 食物营养表
CREATE TABLE IF NOT EXISTS `food_nutrition` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '食物ID',
    `name` VARCHAR(100) NOT NULL COMMENT '食物名称',
    `calories_per_100g` INT NOT NULL COMMENT '每100克卡路里',
    `gi_value` INT DEFAULT 0 COMMENT '升糖指数 (GI)',
    `unit_type` VARCHAR(20) DEFAULT 'g' COMMENT '基础计量单位',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食物营养数据库';

-- 食物营养缓存表（用户自定义食物AI估算缓存）
CREATE TABLE IF NOT EXISTS `food_nutrition_cache` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `normalized_name` VARCHAR(128) NOT NULL COMMENT '规范化名称(唯一键)',
    `display_name` VARCHAR(128) COMMENT '展示名称',
    `calories_per_100g` DOUBLE COMMENT '每100克热量',
    `carbs_per_100g` DOUBLE COMMENT '每100克碳水',
    `sugar_per_100g` DOUBLE COMMENT '每100克糖',
    `data_source` VARCHAR(20) DEFAULT 'ai' COMMENT '数据来源',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_food_nutrition_cache_name` (`normalized_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自定义食物营养估算缓存';

-- 本地知识库 (RAG)
CREATE TABLE IF NOT EXISTS `knowledge_base` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `content` TEXT NOT NULL COMMENT '知识内容',
    `vector` JSON COMMENT '向量表示 (模拟或严格向量类型)', 
    `source` VARCHAR(255) COMMENT '来源引用 (如: 指南 P30)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='本地知识库';

-- 饮食记录表
CREATE TABLE IF NOT EXISTS `diet_logs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `food_id` BIGINT NOT NULL COMMENT '食物ID',
    `fuzzy_unit` VARCHAR(20) NOT NULL COMMENT '模糊单位: FIST(拳), PALM(掌), BOWL(碗)',
    `estimated_weight` INT NOT NULL COMMENT '估算重量 (g)',
    `log_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_log` (`user_id`, `log_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户饮食记录表';

-- 初始化示例数据
INSERT INTO `food_nutrition` (name, calories_per_100g, gi_value) VALUES 
('Rice', 130, 83),
('Chicken Breast', 165, 0),
('Broccoli', 34, 15);

INSERT INTO `knowledge_base` (content, source) VALUES 
('Hypertensive patients should limit sodium intake to less than 5g per day.', 'Dietary Guidelines 2024 P12'),
('Diabetic patients should prioritize low GI foods.', 'Diabetes Care Guide P45');

-- User Settings Table
CREATE TABLE IF NOT EXISTS `user_settings` (
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `font_size` INT DEFAULT 0 COMMENT '字号 (0:标准, 1:大, 2:特大)',
    `theme` VARCHAR(20) DEFAULT 'light' COMMENT '主题 (light/dark)',
    `ai_persona` VARCHAR(20) DEFAULT 'gentle' COMMENT 'AI性格 (strict/gentle)',
    `notification_enabled` BOOLEAN DEFAULT TRUE COMMENT '是否开启通知',
    `dashboard_cards` VARCHAR(500) DEFAULT '[\"bmi\",\"bp\",\"meal\",\"record\",\"glucoseAvg\",\"pendingTasks\",\"profileCompletion\"]' COMMENT '首页卡片配置(JSON数组)',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户偏好设置表';
