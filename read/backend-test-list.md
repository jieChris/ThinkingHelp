# 后端接口测试列表

> 用途: 验证后端所有主要接口可用性与数据正确性。  
> 建议在后端启动后执行，记录结果填入 `read/verification_report.md`。

## 0. 基础准备
- 后端启动成功，端口 8080 可访问
- 数据库已迁移(执行 `backend/patch_db.sql`)
- 记录一个可用账号(管理员 `1000000/admin123` 或新注册账号)

---

## 1. 认证与鉴权
### 1.1 登录
- 接口: `POST /api/auth/login`
- 请求体(JSON):
```json
{"username":"1000000","password":"admin123"}
```
- 预期: `code=200`, 返回 `token` 与 `user`

### 1.2 注册(可选)
- 接口: `POST /api/auth/register`
- 请求体(JSON):
```json
{"username":"1234567","password":"abc12345","nickname":"test","gender":1}
```
- 预期: `code=200`

### 1.3 鉴权拦截
- 随便访问受保护接口(不带 token)
- 预期: HTTP 401/403

---

## 2. 健康档案
> 注意: token 需要最新(后端重启后旧 token 会失效)。

### 2.1 获取档案
- 接口: `GET /api/health/profile`
- Header: `Authorization: Bearer <token>`
- 预期: `code=200`, `data` 为对象或 `null`

### 2.2 保存档案
- 接口: `POST /api/health/profile`
- Header: `Authorization: Bearer <token>`
- 请求体(JSON):
```json
{
  "name": "TestUser",
  "gender": "MALE",
  "age": 30,
  "height": 175,
  "weight": 70,
  "bmi": "22.9",
  "diseases": ["hypertension"],
  "allergies": ["海鲜/虾蟹"],
  "otherRestrictions": "不吃香菜"
}
```
- 预期: `code=200`

### 2.3 保存后再获取
- 接口: `GET /api/health/profile`
- 预期: 返回刚保存的数据

---

## 3. 健康记录(综合)
### 3.1 新增记录
- 接口: `POST /api/health/records`
- 请求体(JSON):
```json
{
  "userId": 1,
  "height": 175,
  "weight": 70,
  "systolic": 120,
  "diastolic": 80,
  "glucose": 5.5,
  "heartRate": 72
}
```
- 预期: `code=200`

### 3.2 获取列表
- 接口: `GET /api/health/records?userId=1`
- 预期: 返回数组(按时间倒序)

### 3.3 获取最新
- 接口: `GET /api/health/records/latest?userId=1`
- 预期: 返回最新一条记录

### 3.4 删除记录(可选)
- 接口: `DELETE /api/health/records/{id}`
- 预期: `code=200`

---

## 4. 饮食记录
### 4.1 新增记录
- 接口: `POST /api/diet/logs`
- Header: `Authorization: Bearer <token>`
- 请求体(JSON):
```json
{"mealType":"LUNCH","foodId":101,"unit":"BOWL","count":1}
```
- 预期: `code=200`

### 4.2 获取列表
- 接口: `GET /api/diet/logs`
- Header: `Authorization: Bearer <token>`
- 预期: 返回数组(按时间倒序)

---

## 5. AI 对话
### 5.1 非流式
- 接口: `POST /api/chat/ask`
- Header: `Authorization: Bearer <token>`
- Body: 纯文本
- 预期: 返回一段文本

### 5.2 流式
- 接口: `POST /api/chat/stream`
- Header: `Authorization: Bearer <token>`
- Body: 纯文本
- 预期: 流式返回文本，末尾包含 `[DONE]`

---

## 6. 食谱中心
### 6.1 生成周食谱
- 接口: `GET /api/meal-plan/weekly`
- Header: `Authorization: Bearer <token>`
- 预期: `code=200`, 包含 `title/advice/weeklyPlan/shoppingList`

### 6.2 导出 PDF
- 接口: `GET /api/export/pdf`
- Header: `Authorization: Bearer <token>`
- 预期: 下载 `meal_plan.pdf`

---

## 7. 用户设置
### 7.1 获取设置
- 接口: `GET /api/user/settings`
- Header: `Authorization: Bearer <token>`
- 预期: `code=200`, 返回配置对象

### 7.2 更新设置
- 接口: `PUT /api/user/settings`
- Header: `Authorization: Bearer <token>`
- 请求体(JSON):
```json
{"fontSize":1,"theme":"light","aiPersona":"gentle","notificationEnabled":true}
```
- 预期: `code=200`

### 7.3 导出数据
- 接口: `POST /api/user/export-data`
- Header: `Authorization: Bearer <token>`
- 预期: 下载 `user_data.txt`

---

## 8. 管理员接口
### 8.1 用户列表
- 接口: `GET /api/admin/users?page=1&size=10`
- Header: `Authorization: Bearer <token>`
- 预期: 返回分页结果

### 8.2 修改会员等级
- 接口: `PUT /api/admin/users/{id}/member-level?level=1`
- Header: `Authorization: Bearer <token>`
- 预期: `code=200`

---

## 9. 头像上传
### 9.1 上传头像
- 接口: `POST /api/user/upload/avatar`
- Header: `Authorization: Bearer <token>`
- 表单: `file`(png/jpg, <1MB)
- 预期: `code=200`, 返回头像 URL

---

## 10. 重点错误排查
- `403/401`: token 失效或未带 Authorization
- `500` with `Unknown column`: 数据库表结构未同步
- `timeout`: AI 模型调用失败或网络异常
