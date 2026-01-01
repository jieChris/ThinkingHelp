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
          <p>基于大模型的个性化慢病饮食管理平台</p>
          <img src="https://cdni.iconscout.com/illustration/premium/thumb/healthy-eating-illustration-download-in-svg-png-gif-file-formats--diet-fruit-food-health-pack-healthcare-medical-illustrations-4379051.png" alt="Healthy Life" />
        </div>
      </div>
      
      <!-- Right Side: Form -->
      <div class="form-side">
        <div class="form-header">
          <h3>登录 Login</h3>
          <p>欢迎回到 ThinkingHelp</p>
        </div>
        
        <el-form ref="formRef" :model="form" :rules="rules" size="large" class="login-form">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="用户名/手机号">
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="密码" show-password @keyup.enter="handleLogin">
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          
          <div class="form-options">
            <el-checkbox v-model="form.remember">记住我</el-checkbox>
            <el-button link type="primary">忘记密码?</el-button>
          </div>
          
          <el-button type="primary" class="submit-btn" :loading="loading" @click="handleLogin">
            立即登录
          </el-button>
          
          <div class="register-link">
            还没有账号? <el-button link type="primary" @click="goRegister">立即注册</el-button>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { login } from '../../api/auth'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  remember: false
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
    if (!formRef.value) return
    await formRef.value.validate(async (valid) => {
        if (valid) {
            loading.value = true
            try {
                // Real API Call
                const res = await login({ 
                    username: form.username, 
                    password: form.password 
                })
                
                // Result<Map> -> data = { token, user }
                if (res.code === 200) {
                     const { token, user } = res.data
                     userStore.login(token, user)
                     ElMessage.success('登录成功')
                     router.push('/dashboard')
                } else {
                    ElMessage.error(res.msg || '登录失败')
                }
            } catch (error: any) {
                // Error handled by interceptor ideally, but local catch ensures loading reset
                console.error(error)
            } finally {
                loading.value = false
            }
        }
    })
}

const goRegister = () => {
    router.push('/register')
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
        
        .form-options {
            display: flex;
            justify-content: space-between;
            margin-bottom: 24px;
        }
        
        .submit-btn {
            width: 100%;
            height: 44px;
            font-size: 16px;
            margin-bottom: 20px;
        }
        
        .register-link {
            text-align: center;
            font-size: 14px;
            color: #6B7280;
        }
    }
}
</style>
