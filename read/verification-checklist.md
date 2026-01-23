# 验证清单与接口预期结果

## 认证与鉴权
- 登录
  - 接口: `POST /api/auth/login`
  - 预期: `code=200`, `data.token` 与 `data.user` 不为空
- 注册
  - 接口: `POST /api/auth/register`
  - 预期: `code=200`
- 未登录访问受保护接口
  - 接口: 任意受保护接口
  - 预期: HTTP 401, 前端跳转 `/login`

## 健康档案
- 获取档案
  - 接口: `GET /api/health/profile`
  - 预期: 无档案返回 `data=null`, 有档案返回对象
- 保存档案
  - 接口: `POST /api/health/profile`
  - 预期: `code=200`
- 保存后再次获取
  - 接口: `GET /api/health/profile`
  - 预期: 返回刚才保存的内容(含 `diseases`/`allergies` 列表)

## 健康记录(综合)
- 新增记录
  - 接口: `POST /api/health/records`
  - 预期: `code=200`
- 获取列表
  - 接口: `GET /api/health/records?userId=...`
  - 预期: 返回按时间倒序的记录数组
- 获取最新
  - 接口: `GET /api/health/records/latest?userId=...`
  - 预期: 返回最新一条记录

## 饮食记录
- 新增记录
  - 接口: `POST /api/diet/logs`
  - 预期: `code=200`
- 获取列表
  - 接口: `GET /api/diet/logs`
  - 预期: 返回按时间倒序的记录数组

## AI 对话
- 流式问答
  - 接口: `POST /api/chat/stream` (text/plain)
  - 预期: 返回 SSE 流式文本
- 普通问答
  - 接口: `POST /api/chat/ask`
  - 预期: 返回文本答案

## 食谱中心
- 生成周食谱
  - 接口: `GET /api/meal-plan/weekly`
  - 预期: `code=200`, 返回 `title`/`advice`/`weeklyPlan`/`shoppingList`
- 导出 PDF
  - 接口: `GET /api/export/pdf`
  - 预期: 下载 `meal_plan.pdf`

## 用户设置
- 获取设置
  - 接口: `GET /api/user/settings`
  - 预期: `code=200`, 返回 `fontSize`/`theme`/`aiPersona`/`notificationEnabled`
- 更新设置
  - 接口: `PUT /api/user/settings`
  - 预期: `code=200`
- 导出数据
  - 接口: `POST /api/user/export-data`
  - 预期: 下载 `user_data.txt`

## 管理员
- 用户列表
  - 接口: `GET /api/admin/users`
  - 预期: 分页返回 records/total
- 修改会员等级
  - 接口: `PUT /api/admin/users/{id}/member-level?level=1`
  - 预期: `code=200`
