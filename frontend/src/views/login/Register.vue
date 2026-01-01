<template>
  <div class="login-container">
    <div class="login-box">
      <!-- Left Side: Illustration -->
      <div class="illustration-side">
        <div class="content">
          <div class="logo">
            <el-icon><Apple /></el-icon> ThinkingHelp
          </div>
          <h2>您的私人 AI 健康管家</h2>
          <p>加入我们需要创建一个新账号，开启您的健康之旅。</p>
          <img src="https://cdni.iconscout.com/illustration/premium/thumb/healthy-eating-illustration-download-in-svg-png-gif-file-formats--diet-fruit-food-health-pack-healthcare-medical-illustrations-4379051.png" alt="Healthy Life" />
        </div>
      </div>
      
      <!-- Right Side: Form -->
      <div class="form-side">
        <div class="form-header">
          <h3>注册 Register</h3>
          <p>创建您的 ThinkingHelp 账号</p>
        </div>
        
        <el-form ref="formRef" :model="form" :rules="rules" size="large" class="login-form">
          <!-- 账号 (Login ID) -->
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="请输入账号（7-12位纯数字）">
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <!-- 昵称 (Display Name) -->
          <el-form-item prop="nickname">
            <el-input v-model="form.nickname" placeholder="请输入您的称呼（昵称）">
              <template #prefix>
                <el-icon><Postcard /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <!-- 性别 -->
          <el-form-item prop="gender">
             <el-radio-group v-model="form.gender">
                <el-radio :label="1">先生</el-radio>
                <el-radio :label="0">女士</el-radio>
                <!-- <el-radio :label="2">保密</el-radio> -->
             </el-radio-group>
          </el-form-item>
          
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="设置密码 (字母+数字, 8位以上)" show-password>
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" show-password @keyup.enter="handleRegister">
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          
          <el-button type="primary" class="submit-btn" :loading="loading" @click="handleRegister">
            立即注册
          </el-button>
          
          <div class="register-link">
            已有账号? <el-button link type="primary" @click="goLogin">返回登录</el-button>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '../../api/auth'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '', // Account
  nickname: '', // Display Name
  gender: 1,    // Default Male
  password: '',
  confirmPassword: ''
})

const validatePass = (rule: any, value: any, callback: any) => {
  if (value === '') {
    callback(new Error('请输入密码'))
  } else {
    // Password Strength Check Regex: Letters + Numbers, Min 8 chars
    const strongRegex = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*?&]{8,}$/
    if (!strongRegex.test(value)) {
        callback(new Error('密码需包含字母和数字，且长度不少于8位'))
    } else {
        callback()
    }
  }
}

const validatePass2 = (rule: any, value: any, callback: any) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

const validateAccount = (rule: any, value: any, callback: any) => {
    if (!value) {
        return callback(new Error('请输入账号'))
    }
    if (!/^\d{7,12}$/.test(value)) {
        return callback(new Error('账号必须为 7-12 位纯数字'))
    }
    callback()
}

const rules = {
  username: [{ validator: validateAccount, trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  password: [{ validator: validatePass, trigger: 'blur' }],
  confirmPassword: [{ validator: validatePass2, trigger: 'blur' }]
}

const handleRegister = async () => {
    if (!formRef.value) return
    await formRef.value.validate(async (valid) => {
        if (valid) {
            loading.value = true
            try {
               const res = await register({
                   username: form.username,
                   nickname: form.nickname,
                   gender: form.gender,
                   password: form.password
               })
               if (res.code === 200) {
                   ElMessage.success({
                       message: '注册成功！正在跳转至登录页...',
                       duration: 3000
                   })
                   setTimeout(() => {
                       router.push('/login')
                   }, 3000)
               } else {
                   ElMessage.error(res.msg || '注册失败')
               }
            } catch (e) {
                console.error(e)
            } finally {
                loading.value = false
            }
        }
    })
}

const goLogin = () => {
    router.push('/login')
}
</script>

<style scoped lang="scss">
.login-container {
    height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #f3f4f6;
    background-image: radial-gradient(#e5e7eb 1px, transparent 1px);
    background-size: 20px 20px;
}

.login-box {
    width: 1000px;
    height: 600px;
    background: #fff;
    border-radius: 20px;
    box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
    display: flex;
    overflow: hidden;
    
    .illustration-side {
        flex: 1;
        background: linear-gradient(135deg, #059669 0%, #34d399 100%);
        padding: 40px;
        display: flex;
        flex-direction: column;
        justify-content: center;
        color: #fff;
        position: relative;
        
        .content {
            z-index: 2;
            text-align: center;
            
            .logo {
                font-size: 24px;
                font-weight: bold;
                display: flex;
                align-items: center;
                justify-content: center;
                gap: 10px;
                margin-bottom: 20px;
            }
            
            h2 {
                font-size: 32px;
                margin-bottom: 10px;
            }
            
            p {
                font-size: 16px;
                opacity: 0.9;
                margin-bottom: 40px;
            }
            
            img {
                max-width: 80%;
                filter: drop-shadow(0 10px 15px rgba(0,0,0,0.2));
            }
        }
        
        &::after {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.1'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
            z-index: 1;
        }
    }
    
    .form-side {
        flex: 1;
        padding: 60px;
        display: flex;
        flex-direction: column;
        justify-content: center;
        
        .form-header {
            margin-bottom: 40px;
            h3 {
                font-size: 28px;
                color: #111827;
                margin-bottom: 8px;
            }
            p {
                color: #6B7280;
            }
        }
        
        .submit-btn {
            width: 100%;
            height: 44px;
            font-size: 16px;
            margin-bottom: 20px;
            margin-top: 20px;
        }
        
        .register-link {
            text-align: center;
            font-size: 14px;
            color: #6B7280;
        }
    }
}
</style>
