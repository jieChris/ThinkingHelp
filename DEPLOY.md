# ThinkingHelp 项目部署指南

本指南将帮助您在本地或服务器上配置并运行 ThinkingHelp 项目。

## 一、 环境准备 (Prerequisites)

在开始之前，请确保您的环境已安装以下软件：

### 1. 后端依赖
*   **JDK 17** 或更高版本
*   **Maven 3.6+**
*   **MySQL 5.7** 或 **8.0**

### 2. 前端依赖
*   **Node.js 18+** (推荐 20 LTS)
*   **npm** 或 **yarn**

---

## 二、 数据库配置 (Database Setup)

1.  **创建数据库**
    登录 MySQL，创建一个名为 `thinking_help` 的数据库：
    ```sql
    CREATE DATABASE thinking_help DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    ```

2.  **执行初始化脚本**
    请按顺序执行以下两个 SQL 脚本（位于 `backend/src/main/resources/db` 或项目根目录）：
    *   `backend/src/main/resources/db/schema.sql` (创建基础表结构)
    *   `backend/fix_schema.sql` (**重要**: 修复字段缺失问题，必须执行！)

    *注：如果您找不到 `fix_schema.sql`，请查看文档附件或重新生成。*

3.  **验证表结构**
    确保数据库中包含以下表：`users`, `health_profiles`, `health_records`, `diet_logs`, `food_nutrition`, `user_settings` 等。

---

## 三、 后端配置 (Backend Configuration)

1.  **修改配置文件**
    打开 `backend/src/main/resources/application.yml`，修改以下关键配置：

    ```yaml
    spring:
      datasource:
        url: jdbc:mysql://localhost:3306/thinking_help?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
        username: root      # 您的数据库账号
        password: your_password # 您的数据库密码

    ai:
      openai:
        api-key: sk-xxxxxxxxx # 您的 OpenAI/DeepSeek API Key
        base-url: https://api.openai.com # 或第三方中转地址
    ```

2.  **启动后端**
    在 `backend` 目录下运行：
    ```bash
    mvn spring-boot:run
    ```
    或者使用 IDE (IntelliJ IDEA) 打开 `backend` 目录，运行 `ThinkingHelpApplication.java`。

    启动成功后，后端将在 `http://localhost:8080` 运行。
    API 文档地址: `http://localhost:8080/doc.html`

---

## 四、 前端配置 (Frontend Setup)

1.  **安装依赖**
    在 `frontend` 目录下运行：
    ```bash
    npm install
    ```

2.  **修改 API 地址 (可选)**
    如果您的后端不在本地或端口不是 8080，请修改 `frontend/vite.config.js` 中的代理配置：
    ```javascript
    server: {
      proxy: {
        '/api': {
          target: 'http://localhost:8080', // 修改为您的后端地址
          changeOrigin: true
        }
      }
    }
    ```

3.  **启动前端**
    ```bash
    npm run dev
    ```
    前端将在 `http://localhost:5173` 运行。

---

## 五、 常见问题 (Troubleshooting)

*   **Q: 登录失败或提示 500 错误？**
    *   A: 检查数据库连接配置是否正确。
    *   A: 确认是否执行了 `fix_schema.sql` 脚本，缺少字段会导致 SQL 错误。

*   **Q: AI 对话超时？**
    *   A: 检查 API Key 是否有效，或者网络是否能访问 OpenAI 接口 (可能需要配置代理或更换 Base URL)。

*   **Q: 页面显示 "Network Error"？**
    *   A: 确保后端服务已启动并运行在 8080 端口。

---

## 六、 部署到服务器 (Production)

1.  **后端打包**:
    ```bash
    cd backend
    mvn clean package -DskipTests
    java -jar target/thinking-help-backend-0.0.1-SNAPSHOT.jar
    ```

2.  **前端打包**:
    ```bash
    cd frontend
    npm run build
    ```
    将 `frontend/dist` 目录下的静态文件部署到 Nginx 或 Web 服务器，并配置反向代理指向后端 8080 端口。
