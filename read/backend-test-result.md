# 后端接口测试结果报告

> 测试时间: 2026/1/23 18:16:56

| 模块 | 测试点 | 结果 | 详情 |
| :--- | :--- | :--- | :--- |
| 1. 认证 | 1.1 登录 | ✅ PASS | Token received |
| 2. 健康档案 | 2.1 获取档案 | ✅ PASS | Code: 200, Data: {"name":"TestUser","gender":"MALE","age":30,"height":175,"weight":70,"bmi":"22.9","diseases":["hypertension"],"allergies":["海鲜/虾蟹"],"otherRestrictions":"不吃香菜"} |
| 2. 健康档案 | 2.2 保存档案 | ✅ PASS | Success |
| 2. 健康档案 | 2.3 保存后再获取 | ✅ PASS | Verified persistence. Data: {"name":"TestUser","gender":"MALE","age":30,"height":175,"weight":70,"bmi":"22.9","diseases":["hypertension"],"allergies":["海鲜/虾蟹"],"otherRestrictions":"不吃香菜"} |
| 3. 健康记录 | 3.1 新增记录 | ✅ PASS | Success |
| 3. 健康记录 | 3.2 获取列表 | ✅ PASS | Count: 7 |
| 3. 健康记录 | 3.3 获取最新 | ✅ PASS | ID: 13 |
| 4. 饮食记录 | 4.1 新增记录 | ✅ PASS | Success |
| 4. 饮食记录 | 4.2 获取列表 | ✅ PASS | Count: 2 |
| 5. AI 对话 | 5.1 普通问答 | ✅ PASS | Response length: 452 |
| 6. 食谱中心 | 6.1 生成周食谱 | ✅ PASS | Success |
| 7. 用户设置 | 7.1 获取设置 | ❌ FAIL | <pre>{<br>&nbsp;&nbsp;"code":&nbsp;500,<br>&nbsp;&nbsp;"message":&nbsp;"\r\n###&nbsp;Error&nbsp;querying&nbsp;database.&nbsp;&nbsp;Cause:&nbsp;java.sql.SQLSyntaxErrorException:&nbsp;Table&nbsp;'thinking_help.user_settings'&nbsp;doesn't&nbsp;exist\r\n###&nbsp;The&nbsp;error&nbsp;may&nbsp;exist&nbsp;in&nbsp;com/thinkinghelp/system/mapper/UserSettingsMapper.java&nbsp;(best&nbsp;guess)\r\n###&nbsp;The&nbsp;error&nbsp;may&nbsp;involve&nbsp;defaultParameterMap\r\n###&nbsp;The&nbsp;error&nbsp;occurred&nbsp;while&nbsp;setting&nbsp;parameters\r\n###&nbsp;SQL:&nbsp;SELECT&nbsp;user_id,font_size,theme,ai_persona,notification_enabled,updated_at&nbsp;FROM&nbsp;user_settings&nbsp;WHERE&nbsp;user_id=?\r\n###&nbsp;Cause:&nbsp;java.sql.SQLSyntaxErrorException:&nbsp;Table&nbsp;'thinking_help.user_settings'&nbsp;doesn't&nbsp;exist\n;&nbsp;bad&nbsp;SQL&nbsp;grammar&nbsp;[]",<br>&nbsp;&nbsp;"data":&nbsp;null<br>}</pre> |
| 7. 用户设置 | 7.2 更新设置 | ❌ FAIL | <pre>{<br>&nbsp;&nbsp;"code":&nbsp;500,<br>&nbsp;&nbsp;"message":&nbsp;"\r\n###&nbsp;Error&nbsp;querying&nbsp;database.&nbsp;&nbsp;Cause:&nbsp;java.sql.SQLSyntaxErrorException:&nbsp;Table&nbsp;'thinking_help.user_settings'&nbsp;doesn't&nbsp;exist\r\n###&nbsp;The&nbsp;error&nbsp;may&nbsp;exist&nbsp;in&nbsp;com/thinkinghelp/system/mapper/UserSettingsMapper.java&nbsp;(best&nbsp;guess)\r\n###&nbsp;The&nbsp;error&nbsp;may&nbsp;involve&nbsp;defaultParameterMap\r\n###&nbsp;The&nbsp;error&nbsp;occurred&nbsp;while&nbsp;setting&nbsp;parameters\r\n###&nbsp;SQL:&nbsp;SELECT&nbsp;COUNT(&nbsp;*&nbsp;)&nbsp;AS&nbsp;total&nbsp;FROM&nbsp;user_settings&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;WHERE&nbsp;&nbsp;(user_id&nbsp;=&nbsp;?)\r\n###&nbsp;Cause:&nbsp;java.sql.SQLSyntaxErrorException:&nbsp;Table&nbsp;'thinking_help.user_settings'&nbsp;doesn't&nbsp;exist\n;&nbsp;bad&nbsp;SQL&nbsp;grammar&nbsp;[]",<br>&nbsp;&nbsp;"data":&nbsp;null<br>}</pre> |
| 8. 管理员 | 8.1 用户列表 | ✅ PASS | Success |
