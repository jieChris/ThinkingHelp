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
    unit VARCHAR(20),
    count DOUBLE,
    recorded_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
