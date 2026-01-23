const axios = require('axios');
const fs = require('fs');
const path = require('path');

const BASE_URL = 'http://localhost:8080';
let TOKEN = '';
const RESULTS = [];

const logResult = (moduleName, testName, status, details) => {
    console.log(`[${status}] ${moduleName} - ${testName}`);
    RESULTS.push({ module: moduleName, test: testName, status: status, details: details });
};

async function runTests() {
    try {
        // 1. Auth
        await testLogin();
        
        if (!TOKEN) {
            console.error('Login failed, aborting subsequent tests.');
            return;
        }

        // 2. Health Profile
        await testGetProfile();
        await testSaveProfile();
        await testGetProfileAfterSave();

        // 3. Health Records
        await testAddHealthRecord();
        await testGetHealthRecords();
        await testGetLatestHealthRecord();

        // 4. Diet Logs
        await testAddDietLog();
        await testGetDietLogs();

        // 5. AI Chat
        await testAiChatNonStream();

        // 6. Meal Plan
        await testGenerateMealPlan();
        
        // 7. User Settings
        await testGetSettings();
        await testUpdateSettings();
        
        // 8. Admin
        await testAdminUserList();

        generateReport();

    } catch (e) {
        console.error('Global error:', e);
    }
}

async function testLogin() {
    try {
        const res = await axios.post(`${BASE_URL}/api/auth/login`, {
            username: '1000000',
            password: 'admin123'
        });
        if (res.data.code === 200 && res.data.data.token) {
            TOKEN = res.data.data.token;
            logResult('1. 认证', '1.1 登录', 'PASS', 'Token received');
        } else {
            logResult('1. 认证', '1.1 登录', 'FAIL', JSON.stringify(res.data));
        }
    } catch (error) {
        logResult('1. 认证', '1.1 登录', 'FAIL', error.message);
    }
}

async function testGetProfile() {
    try {
        const res = await axios.get(`${BASE_URL}/api/health/profile`, { headers: { Authorization: `Bearer ${TOKEN}` } });
        logResult('2. 健康档案', '2.1 获取档案', 'PASS', `Code: ${res.data.code}, Data: ${JSON.stringify(res.data.data)}`);
    } catch (error) {
        logResult('2. 健康档案', '2.1 获取档案', 'FAIL', error.message);
    }
}

async function testSaveProfile() {
    try {
        const res = await axios.post(`${BASE_URL}/api/health/profile`, {
            name: "TestUser",
            gender: "MALE",
            age: 30,
            height: 175,
            weight: 70,
            bmi: "22.9",
            diseases: ["hypertension"],
            allergies: ["海鲜/虾蟹"],
            otherRestrictions: "不吃香菜"
        }, { headers: { Authorization: `Bearer ${TOKEN}` } });
        
        if(res.data.code === 200) {
            logResult('2. 健康档案', '2.2 保存档案', 'PASS', 'Success');
        } else {
             logResult('2. 健康档案', '2.2 保存档案', 'FAIL', JSON.stringify(res.data));
        }
    } catch (error) {
        logResult('2. 健康档案', '2.2 保存档案', 'FAIL', error.message);
    }
}

async function testGetProfileAfterSave() {
    try {
        const res = await axios.get(`${BASE_URL}/api/health/profile`, { headers: { Authorization: `Bearer ${TOKEN}` } });
        // Check if diseases contains hypertension
        const data = res.data.data;
        if (data && data.diseases && JSON.stringify(data.diseases).includes('hypertension')) {
             logResult('2. 健康档案', '2.3 保存后再获取', 'PASS', `Verified persistence. Data: ${JSON.stringify(data)}`);
        } else {
             logResult('2. 健康档案', '2.3 保存后再获取', 'FAIL', `Persistence check failed. Data: ${JSON.stringify(data)}`);
        }
    } catch (error) {
        logResult('2. 健康档案', '2.3 保存后再获取', 'FAIL', error.message);
    }
}

async function testAddHealthRecord() {
    try {
        const res = await axios.post(`${BASE_URL}/api/health/records`, {
            userId: 1, // Note: usually userId is taken from token, but spec said userId in body for this endpoint? Spec says userId:1 in body. Let's try.
            height: 175,
            weight: 70,
            systolic: 120,
            diastolic: 80,
            glucose: 5.5,
            heartRate: 72
        }, { headers: { Authorization: `Bearer ${TOKEN}` } });

         if(res.data.code === 200) {
            logResult('3. 健康记录', '3.1 新增记录', 'PASS', 'Success');
        } else {
             logResult('3. 健康记录', '3.1 新增记录', 'FAIL', JSON.stringify(res.data));
        }
    } catch (error) {
        logResult('3. 健康记录', '3.1 新增记录', 'FAIL', error.message);
    }
}

async function testGetHealthRecords() {
    try {
        const res = await axios.get(`${BASE_URL}/api/health/records?userId=1`, { headers: { Authorization: `Bearer ${TOKEN}` } });
        if(res.data.code === 200 && Array.isArray(res.data.data)) {
            logResult('3. 健康记录', '3.2 获取列表', 'PASS', `Count: ${res.data.data.length}`);
        } else {
            logResult('3. 健康记录', '3.2 获取列表', 'FAIL', JSON.stringify(res.data));
        }
    } catch (error) {
        logResult('3. 健康记录', '3.2 获取列表', 'FAIL', error.message);
    }
}

async function testGetLatestHealthRecord() {
    try {
        const res = await axios.get(`${BASE_URL}/api/health/records/latest?userId=1`, { headers: { Authorization: `Bearer ${TOKEN}` } });
        if(res.data.code === 200 && res.data.data) {
            logResult('3. 健康记录', '3.3 获取最新', 'PASS', `ID: ${res.data.data.id}`);
        } else {
            logResult('3. 健康记录', '3.3 获取最新', 'FAIL', JSON.stringify(res.data));
        }
    } catch (error) {
        logResult('3. 健康记录', '3.3 获取最新', 'FAIL', error.message);
    }
}

async function testAddDietLog() {
     try {
        const res = await axios.post(`${BASE_URL}/api/diet/logs`, {
            mealType: "LUNCH",
            foodId: 101,
            unit: "BOWL",
            count: 1
        }, { headers: { Authorization: `Bearer ${TOKEN}` } });

         if(res.data.code === 200) {
            logResult('4. 饮食记录', '4.1 新增记录', 'PASS', 'Success');
        } else {
             logResult('4. 饮食记录', '4.1 新增记录', 'FAIL', JSON.stringify(res.data));
        }
    } catch (error) {
        logResult('4. 饮食记录', '4.1 新增记录', 'FAIL', error.message);
    }
}

async function testGetDietLogs() {
    try {
         const res = await axios.get(`${BASE_URL}/api/diet/logs`, { headers: { Authorization: `Bearer ${TOKEN}` } });
        if(res.data.code === 200 && Array.isArray(res.data.data)) {
            logResult('4. 饮食记录', '4.2 获取列表', 'PASS', `Count: ${res.data.data.length}`);
        } else {
            logResult('4. 饮食记录', '4.2 获取列表', 'FAIL', JSON.stringify(res.data));
        }
    } catch (error) {
        logResult('4. 饮食记录', '4.2 获取列表', 'FAIL', error.message);
    }
}

async function testAiChatNonStream() {
    try {
        const res = await axios.post(`${BASE_URL}/api/chat/ask`, "你好，你是谁？", { 
            headers: { 
                Authorization: `Bearer ${TOKEN}`,
                'Content-Type': 'text/plain'
            } 
        });
        if(res.status === 200 && res.data) {
             logResult('5. AI 对话', '5.1 普通问答', 'PASS', `Response length: ${res.data.length}`);
        } else {
             logResult('5. AI 对话', '5.1 普通问答', 'FAIL', 'No data returned');
        }
    } catch (error) {
        logResult('5. AI 对话', '5.1 普通问答', 'FAIL', error.message);
    }
}

async function testGenerateMealPlan() {
    try {
        const res = await axios.get(`${BASE_URL}/api/meal-plan/weekly`, { headers: { Authorization: `Bearer ${TOKEN}` } });
         if(res.data.code === 200 && res.data.data) {
             logResult('6. 食谱中心', '6.1 生成周食谱', 'PASS', 'Success');
        } else {
             logResult('6. 食谱中心', '6.1 生成周食谱', 'FAIL', JSON.stringify(res.data));
        }
    } catch (error) {
        // This might timeout
        logResult('6. 食谱中心', '6.1 生成周食谱', 'FAIL', error.message);
    }
}

async function testGetSettings() {
    try {
        const res = await axios.get(`${BASE_URL}/api/user/settings`, { headers: { Authorization: `Bearer ${TOKEN}` } });
         if(res.data.code === 200) {
             logResult('7. 用户设置', '7.1 获取设置', 'PASS', 'Success');
        } else {
             logResult('7. 用户设置', '7.1 获取设置', 'FAIL', JSON.stringify(res.data));
        }
    } catch (error) {
        logResult('7. 用户设置', '7.1 获取设置', 'FAIL', error.message);
    }
}

async function testUpdateSettings() {
     try {
        const res = await axios.put(`${BASE_URL}/api/user/settings`, {
            fontSize: 1,
            theme: "light",
            aiPersona: "gentle",
            notificationEnabled: true
        }, { headers: { Authorization: `Bearer ${TOKEN}` } });

         if(res.data.code === 200) {
            logResult('7. 用户设置', '7.2 更新设置', 'PASS', 'Success');
        } else {
             logResult('7. 用户设置', '7.2 更新设置', 'FAIL', JSON.stringify(res.data));
        }
    } catch (error) {
        logResult('7. 用户设置', '7.2 更新设置', 'FAIL', error.message);
    }
}

async function testAdminUserList() {
    try {
        const res = await axios.get(`${BASE_URL}/api/admin/users?page=1&size=10`, { headers: { Authorization: `Bearer ${TOKEN}` } });
         if(res.data.code === 200) {
             logResult('8. 管理员', '8.1 用户列表', 'PASS', 'Success');
        } else {
             logResult('8. 管理员', '8.1 用户列表', 'FAIL', JSON.stringify(res.data));
        }
    } catch (error) {
        logResult('8. 管理员', '8.1 用户列表', 'FAIL', error.message);
    }
}


function generateReport() {
    let md = '# 后端接口测试结果报告\\n\\n';
    md += `> 测试时间: ${new Date().toLocaleString()}\\n\\n`;
    
    md += '| 模块 | 测试点 | 结果 | 详情 |\\n';
    md += '| :--- | :--- | :--- | :--- |\\n';
    
    RESULTS.forEach(r => {
        const icon = r.status === 'PASS' ? '✅' : '❌';
        // Excape pipes checks
        const details = r.details.replace(/\|/g, '\\|').replace(/\n/g, ' ');
        md += `| ${r.module} | ${r.test} | ${icon} ${r.status} | ${details} |\\n`;
    });

    try {
        fs.writeFileSync('read/backend-test-result.md', md);
        console.log('Report generated at read/backend-test-result.md');
    } catch(err) {
        console.error('Failed to write report:', err);
    }
}

runTests();
