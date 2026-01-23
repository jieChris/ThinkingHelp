-- 修复数据库表结构的 SQL 脚本
-- 请在您的数据库工具(如 Navicat)中执行此脚本，或通过命令行运行
USE thinking_help;

-- 1. 修复 health_profiles 表
-- Java 代码期望: name, gender, age, height, weight, bmi, diseases, allergies, other_restrictions
-- 当前 SQL 只有: dynamic_tags

-- 如果表已存在，添加缺少的字段
-- 注意: 表结构修改比较复杂，为了保险，如果您没有重要数据，建议直接 DROP TABLE 后重建。
-- 下面是 ALTER 方案:

-- 检查并添加 name 字段
SET @exist := (SELECT count(*) FROM information_schema.columns WHERE table_schema = 'thinking_help' AND table_name = 'health_profiles' AND column_name = 'name');
SET @sql := IF(@exist = 0, 'ALTER TABLE health_profiles ADD COLUMN name VARCHAR(50) COMMENT "姓名"', 'SELECT "Column name exists"');
PREPARE stmt FROM @sql; EXECUTE stmt;

-- 简单起见，直接提供 ALTER 语句，如果报错说明已存在，忽略即可
-- 对于 Java @TableId(type = IdType.AUTO) private Long id;
-- 原表主键是 user_id，需要调整。

-- ！！！重要！！！
-- 既然代码和数据库严重不一致，最快的方法是重建这两个表。

DROP TABLE IF EXISTS `health_profiles`;
CREATE TABLE `health_profiles` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `name` VARCHAR(50) COMMENT '姓名',
    `gender` VARCHAR(10) COMMENT '性别',
    `age` INT COMMENT '年龄',
    `height` DOUBLE COMMENT '身高(cm)',
    `weight` DOUBLE COMMENT '体重(kg)',
    `bmi` VARCHAR(20) COMMENT 'BMI指数',
    `diseases` TEXT COMMENT '慢性病标签(JSON)',
    `allergies` TEXT COMMENT '过敏源(JSON)',
    `other_restrictions` TEXT COMMENT '其他忌口',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户健康档案表';


DROP TABLE IF EXISTS `diet_logs`;
CREATE TABLE `diet_logs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `meal_type` VARCHAR(20) DEFAULT 'SNACK' COMMENT '餐别',
    `food_id` BIGINT COMMENT '食物ID',
    `unit` VARCHAR(20) COMMENT '单位',
    `count` DOUBLE COMMENT '数量',
    `recorded_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户饮食记录表';
