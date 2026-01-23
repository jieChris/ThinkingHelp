USE thinking_help;

-- Patch 1: Add Nickname
SET @exist := (SELECT count(*) FROM information_schema.columns WHERE table_schema = 'thinking_help' AND table_name = 'users' AND column_name = 'nickname');
SET @sql := IF(@exist = 0, 'ALTER TABLE users ADD COLUMN nickname VARCHAR(50) DEFAULT NULL COMMENT "用户名 (显示昵称)"', 'SELECT "Column nickname already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;

-- Patch 2: Add Gender
SET @exist := (SELECT count(*) FROM information_schema.columns WHERE table_schema = 'thinking_help' AND table_name = 'users' AND column_name = 'gender');
SET @sql := IF(@exist = 0, 'ALTER TABLE users ADD COLUMN gender INT DEFAULT 2 COMMENT "性别 (0:女, 1:男, 2:未知)"', 'SELECT "Column gender already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;

-- Patch 3: Add Role
SET @exist := (SELECT count(*) FROM information_schema.columns WHERE table_schema = 'thinking_help' AND table_name = 'users' AND column_name = 'role');
SET @sql := IF(@exist = 0, 'ALTER TABLE users ADD COLUMN role VARCHAR(20) DEFAULT "USER" COMMENT "角色 (USER/ADMIN)"', 'SELECT "Column role already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;

-- Patch 4: Ensure Admin User Exists (1000000 / admin123)
-- MD5 for admin123: 0192023a7bbd73250516f069df18b500
DELETE FROM users WHERE username = 'admin'; -- Cleanup old invalid admin if any
INSERT INTO users (username, password_hash, nickname, role, member_level, gender) 
SELECT '1000000', '0192023a7bbd73250516f069df18b500', 'Administrator', 'ADMIN', 1, 1
WHERE NOT EXISTS (SELECT * FROM users WHERE username = '1000000');

-- Patch 5: Create Health Profile Table
CREATE TABLE IF NOT EXISTS health_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(50),
    gender VARCHAR(10),
    age INT,
    height DOUBLE,
    weight DOUBLE,
    bmi VARCHAR(20),
    diseases TEXT,
    allergies TEXT,
    other_restrictions TEXT,
    updated_at DATETIME,
    UNIQUE KEY uk_health_profiles_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Patch 6: Create Diet Logs Table
CREATE TABLE IF NOT EXISTS diet_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    meal_type VARCHAR(20) NOT NULL,
    food_id INT,
    food_name VARCHAR(100),
    unit VARCHAR(20),
    count DOUBLE,
    weight_grams DOUBLE,
    calories DOUBLE,
    calories_source VARCHAR(30),
    recorded_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Patch 10: Create Meal Plans Table
CREATE TABLE IF NOT EXISTS meal_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    range_type VARCHAR(20),
    title VARCHAR(200),
    advice TEXT,
    plan_json LONGTEXT,
    created_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Patch 7: Rename legacy table names if present
SET @exist_old := (SELECT count(*) FROM information_schema.tables WHERE table_schema = 'thinking_help' AND table_name = 'health_profile');
SET @exist_new := (SELECT count(*) FROM information_schema.tables WHERE table_schema = 'thinking_help' AND table_name = 'health_profiles');
SET @sql := IF(@exist_old = 1 AND @exist_new = 0, 'RENAME TABLE health_profile TO health_profiles', 'SELECT "health_profiles ok"');
PREPARE stmt FROM @sql;
EXECUTE stmt;

SET @exist_old := (SELECT count(*) FROM information_schema.tables WHERE table_schema = 'thinking_help' AND table_name = 'diet_log');
SET @exist_new := (SELECT count(*) FROM information_schema.tables WHERE table_schema = 'thinking_help' AND table_name = 'diet_logs');
SET @sql := IF(@exist_old = 1 AND @exist_new = 0, 'RENAME TABLE diet_log TO diet_logs', 'SELECT "diet_logs ok"');
PREPARE stmt FROM @sql;
EXECUTE stmt;

-- Patch 8: Ensure id column exists (avoid multiple primary key)
SET @exist := (SELECT count(*) FROM information_schema.columns WHERE table_schema = 'thinking_help' AND table_name = 'health_profiles' AND column_name = 'id');
SET @pk := (SELECT count(*) FROM information_schema.table_constraints WHERE table_schema = 'thinking_help' AND table_name = 'health_profiles' AND constraint_type = 'PRIMARY KEY');
SET @sql := IF(@exist = 0 AND @pk = 0,
    'ALTER TABLE health_profiles ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST',
    IF(@exist = 0 AND @pk = 1,
        'ALTER TABLE health_profiles ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT UNIQUE FIRST',
        'SELECT "Column id already exists"'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;

SET @exist := (SELECT count(*) FROM information_schema.columns WHERE table_schema = 'thinking_help' AND table_name = 'diet_logs' AND column_name = 'id');
SET @pk := (SELECT count(*) FROM information_schema.table_constraints WHERE table_schema = 'thinking_help' AND table_name = 'diet_logs' AND constraint_type = 'PRIMARY KEY');
SET @sql := IF(@exist = 0 AND @pk = 0,
    'ALTER TABLE diet_logs ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST',
    IF(@exist = 0 AND @pk = 1,
        'ALTER TABLE diet_logs ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT UNIQUE FIRST',
        'SELECT "Column id already exists"'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;

-- Patch 9: Extend diet_logs columns
SET @exist := (SELECT count(*) FROM information_schema.columns WHERE table_schema = 'thinking_help' AND table_name = 'diet_logs' AND column_name = 'food_name');
SET @sql := IF(@exist = 0, 'ALTER TABLE diet_logs ADD COLUMN food_name VARCHAR(100)', 'SELECT "Column food_name already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;

SET @exist := (SELECT count(*) FROM information_schema.columns WHERE table_schema = 'thinking_help' AND table_name = 'diet_logs' AND column_name = 'weight_grams');
SET @sql := IF(@exist = 0, 'ALTER TABLE diet_logs ADD COLUMN weight_grams DOUBLE', 'SELECT "Column weight_grams already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;

SET @exist := (SELECT count(*) FROM information_schema.columns WHERE table_schema = 'thinking_help' AND table_name = 'diet_logs' AND column_name = 'calories');
SET @sql := IF(@exist = 0, 'ALTER TABLE diet_logs ADD COLUMN calories DOUBLE', 'SELECT "Column calories already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;

SET @exist := (SELECT count(*) FROM information_schema.columns WHERE table_schema = 'thinking_help' AND table_name = 'diet_logs' AND column_name = 'calories_source');
SET @sql := IF(@exist = 0, 'ALTER TABLE diet_logs ADD COLUMN calories_source VARCHAR(30)', 'SELECT "Column calories_source already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
