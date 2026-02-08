# 系统配置管理设计文档(模型 + OCR)

## 目标
- 在后台管理面板中可修改 AI 模型与 OCR 的配置(无需改配置文件)。
- 支持配置热更新(或近实时生效)。
- 对敏感信息(API Key/Secret)做脱敏与加密存储。

## 范围
- 管理配置项: AI 模型/Endpoint/API Key, OCR Endpoint/API Key/Secret。
- 后端接口: 仅管理员可读写。
- 前端管理页面: 配置项列表 + 编辑 + 保存 + 测试连接。

## 数据模型
### 表: system_config
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 自增主键 |
| config_key | VARCHAR(128) UNIQUE | 配置键 |
| config_value | TEXT | 明文值(非敏感) |
| encrypted_value | TEXT | 密文值(敏感) |
| is_secret | TINYINT | 是否敏感(1/0) |
| description | VARCHAR(255) | 说明 |
| updated_by | BIGINT | 更新人(管理员用户ID) |
| updated_at | DATETIME | 更新时间 |

### 推荐配置键
AI 模型:
- `ai.provider` (如: dashscope/openai)
- `ai.base_url`
- `ai.api_key` (secret)
- `ai.model` (如: qwen-plus/qwen-turbo)
- `ai.timeout_ms`

OCR:
- `ocr.provider` (如: aliyun)
- `ocr.endpoint`
- `ocr.access_key_id` (secret)
- `ocr.access_key_secret` (secret)
- `ocr.project_id`(可选)
- `ocr.timeout_ms`

## 后端接口设计
仅管理员角色可访问(建议加 `@PreAuthorize("hasRole('ADMIN')")`)

### 1) 查询配置列表
- `GET /api/admin/system-configs`
- 返回脱敏后的数据(对 `is_secret=1` 返回 `****`)

### 2) 更新配置
- `PUT /api/admin/system-configs`
- body: `{ key, value, isSecret }`
- 规则:
  - `isSecret=true` 时写入 `encrypted_value`
  - `isSecret=false` 时写入 `config_value`

### 3) 批量更新
- `PUT /api/admin/system-configs/batch`
- body: `[{ key, value, isSecret }]`

### 4) 测试连接
- `POST /api/admin/system-configs/test`
- body: `{ type: "ai" | "ocr" }`
- 使用当前 DB 配置发起一次轻量请求(如 AI 简单问答/ OCR 空请求)，返回可用性结果。

## 安全与加密
- 对 `is_secret=1` 的配置做加密存储。
- 加密密钥放在环境变量中(如 `CONFIG_ENCRYPT_KEY`)。
- 前端展示时脱敏。
- 审计日志: 记录修改人/时间/配置键(不记录明文值)。

## 配置加载与生效策略
### 方案 A: 缓存 + 定时刷新(推荐)
- `SystemConfigService` 读取 DB -> 缓存在内存(如 Caffeine/Map)。
- TTL 60s 或手动刷新接口。
- AI/OCR 客户端每次调用时读取最新配置。

### 方案 B: 更新时重建客户端
- 更新接口完成后调用 `ConfigReloadService` 重新初始化 AI/OCR 客户端 Bean。
- 复杂度略高但生效即时。

## 前端管理页面(建议)
页面路径: `frontend/src/views/admin/AdminSystemConfig.vue`
- 配置分组: AI / OCR
- 输入框: Base URL, Model, Timeout
- 密钥框: 密文显示(只允许替换)
- 操作: 保存 / 测试连接

## 迁移步骤
1) 创建 `system_config` 表(脚本加入 `backend/patch_db.sql`)  
2) 增加实体/Mapper/Service/Controller  
3) 增加管理员页面与路由  
4) 替换现有配置读取逻辑(从 DB 读取)  

## 回滚策略
- 若配置读取异常，回退到环境变量默认值(作为兜底)。

## 注意事项
- 不建议在日志中打印密钥明文。
- 测试连接接口需要限流，避免被滥用。
