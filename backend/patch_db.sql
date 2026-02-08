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

-- Patch 11: Extend health_profiles with exam fields
SET @exist := (SELECT count(*) FROM information_schema.columns WHERE table_schema = 'thinking_help' AND table_name = 'health_profiles' AND column_name = 'report_date');
SET @sql := IF(@exist = 0, 'ALTER TABLE health_profiles ADD COLUMN report_date VARCHAR(20)', 'SELECT \"Column report_date already exists\"');
PREPARE stmt FROM @sql;
EXECUTE stmt;

SET @exist := (SELECT count(*) FROM information_schema.columns WHERE table_schema = 'thinking_help' AND table_name = 'health_profiles' AND column_name = 'bp_systolic');
SET @sql := IF(@exist = 0, 'ALTER TABLE health_profiles ADD COLUMN bp_systolic DOUBLE', 'SELECT \"Column bp_systolic already exists\"');
PREPARE stmt FROM @sql;
EXECUTE stmt;

SET @exist := (SELECT count(*) FROM information_schema.columns WHERE table_schema = 'thinking_help' AND table_name = 'health_profiles' AND column_name = 'activity_level');
SET @sql := IF(@exist = 0, 'ALTER TABLE health_profiles ADD COLUMN activity_level VARCHAR(20)', 'SELECT \"Column activity_level already exists\"');
PREPARE stmt FROM @sql;
EXECUTE stmt;

SET @exist := (SELECT count(*) FROM information_schema.columns WHERE table_schema = 'thinking_help' AND table_name = 'health_profiles' AND column_name = 'goal');
SET @sql := IF(@exist = 0, 'ALTER TABLE health_profiles ADD COLUMN goal VARCHAR(20)', 'SELECT \"Column goal already exists\"');
PREPARE stmt FROM @sql;
EXECUTE stmt;

SET @exist := (SELECT count(*) FROM information_schema.columns WHERE table_schema = 'thinking_help' AND table_name = 'health_profiles' AND column_name = 'exercise_frequency');
SET @sql := IF(@exist = 0, 'ALTER TABLE health_profiles ADD COLUMN exercise_frequency INT', 'SELECT \"Column exercise_frequency already exists\"');
PREPARE stmt FROM @sql;
EXECUTE stmt;

SET @exist := (SELECT count(*) FROM information_schema.columns WHERE table_schema = 'thinking_help' AND table_name = 'health_profiles' AND column_name = 'exercise_duration');
SET @sql := IF(@exist = 0, 'ALTER TABLE health_profiles ADD COLUMN exercise_duration INT', 'SELECT \"Column exercise_duration already exists\"');
PREPARE stmt FROM @sql;
EXECUTE stmt;

SET @exist := (SELECT count(*) FROM information_schema.columns WHERE table_schema = 'thinking_help' AND table_name = 'health_profiles' AND column_name = 'bp_diastolic');
SET @sql := IF(@exist = 0, 'ALTER TABLE health_profiles ADD COLUMN bp_diastolic DOUBLE', 'SELECT \"Column bp_diastolic already exists\"');
PREPARE stmt FROM @sql;
EXECUTE stmt;

SET @exist := (SELECT count(*) FROM information_schema.columns WHERE table_schema = 'thinking_help' AND table_name = 'health_profiles' AND column_name = 'fasting_glucose');
SET @sql := IF(@exist = 0, 'ALTER TABLE health_profiles ADD COLUMN fasting_glucose DOUBLE', 'SELECT \"Column fasting_glucose already exists\"');
PREPARE stmt FROM @sql;
EXECUTE stmt;

SET @exist := (SELECT count(*) FROM information_schema.columns WHERE table_schema = 'thinking_help' AND table_name = 'health_profiles' AND column_name = 'hba1c');
SET @sql := IF(@exist = 0, 'ALTER TABLE health_profiles ADD COLUMN hba1c DOUBLE', 'SELECT \"Column hba1c already exists\"');
PREPARE stmt FROM @sql;
EXECUTE stmt;

SET @exist := (SELECT count(*) FROM information_schema.columns WHERE table_schema = 'thinking_help' AND table_name = 'health_profiles' AND column_name = 'total_cholesterol');
SET @sql := IF(@exist = 0, 'ALTER TABLE health_profiles ADD COLUMN total_cholesterol DOUBLE', 'SELECT \"Column total_cholesterol already exists\"');
PREPARE stmt FROM @sql;
EXECUTE stmt;

SET @exist := (SELECT count(*) FROM information_schema.columns WHERE table_schema = 'thinking_help' AND table_name = 'health_profiles' AND column_name = 'triglycerides');
SET @sql := IF(@exist = 0, 'ALTER TABLE health_profiles ADD COLUMN triglycerides DOUBLE', 'SELECT \"Column triglycerides already exists\"');
PREPARE stmt FROM @sql;
EXECUTE stmt;

SET @exist := (SELECT count(*) FROM information_schema.columns WHERE table_schema = 'thinking_help' AND table_name = 'health_profiles' AND column_name = 'hdl');
SET @sql := IF(@exist = 0, 'ALTER TABLE health_profiles ADD COLUMN hdl DOUBLE', 'SELECT \"Column hdl already exists\"');
PREPARE stmt FROM @sql;
EXECUTE stmt;

SET @exist := (SELECT count(*) FROM information_schema.columns WHERE table_schema = 'thinking_help' AND table_name = 'health_profiles' AND column_name = 'ldl');
SET @sql := IF(@exist = 0, 'ALTER TABLE health_profiles ADD COLUMN ldl DOUBLE', 'SELECT \"Column ldl already exists\"');
PREPARE stmt FROM @sql;
EXECUTE stmt;

SET @exist := (SELECT count(*) FROM information_schema.columns WHERE table_schema = 'thinking_help' AND table_name = 'health_profiles' AND column_name = 'uric_acid');
SET @sql := IF(@exist = 0, 'ALTER TABLE health_profiles ADD COLUMN uric_acid DOUBLE', 'SELECT \"Column uric_acid already exists\"');
PREPARE stmt FROM @sql;
EXECUTE stmt;

SET @exist := (SELECT count(*) FROM information_schema.columns WHERE table_schema = 'thinking_help' AND table_name = 'health_profiles' AND column_name = 'alt');
SET @sql := IF(@exist = 0, 'ALTER TABLE health_profiles ADD COLUMN alt DOUBLE', 'SELECT \"Column alt already exists\"');
PREPARE stmt FROM @sql;
EXECUTE stmt;

SET @exist := (SELECT count(*) FROM information_schema.columns WHERE table_schema = 'thinking_help' AND table_name = 'health_profiles' AND column_name = 'ast');
SET @sql := IF(@exist = 0, 'ALTER TABLE health_profiles ADD COLUMN ast DOUBLE', 'SELECT \"Column ast already exists\"');
PREPARE stmt FROM @sql;
EXECUTE stmt;

SET @exist := (SELECT count(*) FROM information_schema.columns WHERE table_schema = 'thinking_help' AND table_name = 'health_profiles' AND column_name = 'creatinine');
SET @sql := IF(@exist = 0, 'ALTER TABLE health_profiles ADD COLUMN creatinine DOUBLE', 'SELECT \"Column creatinine already exists\"');
PREPARE stmt FROM @sql;
EXECUTE stmt;

SET @exist := (SELECT count(*) FROM information_schema.columns WHERE table_schema = 'thinking_help' AND table_name = 'health_profiles' AND column_name = 'bun');
SET @sql := IF(@exist = 0, 'ALTER TABLE health_profiles ADD COLUMN bun DOUBLE', 'SELECT \"Column bun already exists\"');
PREPARE stmt FROM @sql;
EXECUTE stmt;

-- Patch 12: Create health profile history table
CREATE TABLE IF NOT EXISTS health_profile_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    report_date VARCHAR(20),
    profile_json LONGTEXT,
    created_at DATETIME,
    updated_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Patch 13: Create AI configs table
CREATE TABLE IF NOT EXISTS ai_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(50) NOT NULL UNIQUE,
    api_key VARCHAR(200),
    base_url VARCHAR(200),
    model VARCHAR(100),
    temperature DOUBLE,
    max_tokens INT,
    enabled TINYINT DEFAULT 1,
    created_at DATETIME,
    updated_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Patch 14: Create meal plan async task table
CREATE TABLE IF NOT EXISTS meal_plan_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    range_type VARCHAR(20),
    requirements TEXT,
    status VARCHAR(20),
    plan_json LONGTEXT,
    error_message TEXT,
    created_at DATETIME,
    updated_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Patch 15: Create meal plan feedback table
CREATE TABLE IF NOT EXISTS meal_plan_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    meal_type VARCHAR(20),
    direction VARCHAR(20),
    note VARCHAR(200),
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

SET @exist := (SELECT count(*) FROM information_schema.columns WHERE table_schema = 'thinking_help' AND table_name = 'diet_logs' AND column_name = 'carbs_grams');
SET @sql := IF(@exist = 0, 'ALTER TABLE diet_logs ADD COLUMN carbs_grams DOUBLE', 'SELECT "Column carbs_grams already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;

SET @exist := (SELECT count(*) FROM information_schema.columns WHERE table_schema = 'thinking_help' AND table_name = 'diet_logs' AND column_name = 'sugar_grams');
SET @sql := IF(@exist = 0, 'ALTER TABLE diet_logs ADD COLUMN sugar_grams DOUBLE', 'SELECT "Column sugar_grams already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;

SET @exist := (SELECT count(*) FROM information_schema.columns WHERE table_schema = 'thinking_help' AND table_name = 'diet_logs' AND column_name = 'carbs_source');
SET @sql := IF(@exist = 0, 'ALTER TABLE diet_logs ADD COLUMN carbs_source VARCHAR(20)', 'SELECT "Column carbs_source already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;

SET @exist := (SELECT count(*) FROM information_schema.columns WHERE table_schema = 'thinking_help' AND table_name = 'diet_logs' AND column_name = 'sugar_source');
SET @sql := IF(@exist = 0, 'ALTER TABLE diet_logs ADD COLUMN sugar_source VARCHAR(20)', 'SELECT "Column sugar_source already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;

SET @exist := (SELECT count(*) FROM information_schema.columns WHERE table_schema = 'thinking_help' AND table_name = 'diet_logs' AND column_name = 'calories_source');
SET @sql := IF(@exist = 0, 'ALTER TABLE diet_logs ADD COLUMN calories_source VARCHAR(30)', 'SELECT "Column calories_source already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;

-- Patch 16: Create glucose records table
CREATE TABLE IF NOT EXISTS glucose_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    glucose_value DOUBLE NOT NULL,
    measure_type VARCHAR(30) NOT NULL,
    event_type VARCHAR(20),
    medication_note VARCHAR(255),
    symptoms VARCHAR(255),
    related_meal VARCHAR(255),
    recorded_at DATETIME,
    created_at DATETIME,
    KEY idx_glucose_user_time (user_id, recorded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Patch 17: Create glucose follow-up tasks table
CREATE TABLE IF NOT EXISTS glucose_followup_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    task_type VARCHAR(30) NOT NULL,
    title VARCHAR(120) NOT NULL,
    note VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    due_at DATETIME,
    completed_at DATETIME,
    created_at DATETIME,
    KEY idx_glucose_task_user_status (user_id, status, due_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Patch 18: Create food nutrition cache table (AI estimate cache)
CREATE TABLE IF NOT EXISTS food_nutrition_cache (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    normalized_name VARCHAR(128) NOT NULL,
    display_name VARCHAR(128),
    calories_per_100g DOUBLE,
    carbs_per_100g DOUBLE,
    sugar_per_100g DOUBLE,
    data_source VARCHAR(20),
    created_at DATETIME,
    updated_at DATETIME,
    UNIQUE KEY uk_food_nutrition_cache_name (normalized_name),
    KEY idx_food_nutrition_cache_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Patch 19: Ensure user_settings exists and has dashboard_cards
CREATE TABLE IF NOT EXISTS user_settings (
    user_id BIGINT NOT NULL PRIMARY KEY,
    font_size INT DEFAULT 0,
    theme VARCHAR(20) DEFAULT 'light',
    ai_persona VARCHAR(20) DEFAULT 'gentle',
    notification_enabled BOOLEAN DEFAULT TRUE,
    dashboard_cards VARCHAR(500) DEFAULT '[\"bmi\",\"bp\",\"meal\",\"record\",\"glucoseAvg\",\"pendingTasks\",\"profileCompletion\"]',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @exist := (SELECT count(*) FROM information_schema.columns WHERE table_schema = 'thinking_help' AND table_name = 'user_settings' AND column_name = 'dashboard_cards');
SET @sql := IF(@exist = 0,
    'ALTER TABLE user_settings ADD COLUMN dashboard_cards VARCHAR(500) DEFAULT ''[\"bmi\",\"bp\",\"meal\",\"record\",\"glucoseAvg\",\"pendingTasks\",\"profileCompletion\"]''',
    'SELECT \"Column dashboard_cards already exists\"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
