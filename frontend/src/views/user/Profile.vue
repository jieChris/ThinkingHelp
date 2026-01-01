<template>
  <div class="profile-container">
    <h2>个人中心</h2>
    <div class="profile-card">
      <div class="avatar-section">
        <el-upload
          class="avatar-uploader"
          :action="uploadUrl"
          :show-file-list="false"
          :on-success="handleAvatarSuccess"
          :before-upload="beforeAvatarUpload"
          :headers="headers"
          name="file"
        >
          <img v-if="form.avatar" :src="fullAvatarUrl(form.avatar)" class="avatar" />
          <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          <div class="hover-mask">
            <el-icon><Camera /></el-icon>
            <span>更换头像</span>
          </div>
        </el-upload>
        <div class="avatar-tip">点击上传头像，支持 jpg/png，小于 1MB</div>
        
        <div class="system-avatars">
             <span>或选择系统头像：</span>
             <div class="avatar-list">
                 <img v-for="sys in systemAvatars" :key="sys" :src="sys" 
                      class="sys-avatar" @click="selectSystemAvatar(sys)" />
             </div>
        </div>
      </div>

      <el-form :model="form" label-width="80px" class="profile-form">
        <el-form-item label="账号">
          <el-input v-model="form.username" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="请输入昵称 (2-16位)" maxlength="16" show-word-limit />
        </el-form-item>
        <el-form-item label="性别">
            <el-radio-group v-model="form.gender">
                <el-radio :label="1">男</el-radio>
                <el-radio :label="0">女</el-radio>
            </el-radio-group>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="saveProfile" :loading="loading">保存修改</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '../../stores/user'
import request from '../../api/request'
import { ElMessage } from 'element-plus'
import { Plus, Camera } from '@element-plus/icons-vue'

const userStore = useUserStore()
const loading = ref(false)
const uploadUrl = '/api/user/upload/avatar' // Vite proxy will handle host
const headers = {
    Authorization: `Bearer ${userStore.token}`
}

const form = reactive({
    id: 0,
    username: '',
    nickname: '',
    avatar: '',
    gender: 2
})

// System avatars (Using public readable images or static assets)
const systemAvatars = [
    'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
    'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
    'https://cube.elemecdn.com/9/c2/f0ee8a3c7c9638a54940382568c9dpng.png'
]

onMounted(() => {
    if (userStore.user) {
        form.id = userStore.user.id
        form.username = userStore.user.username
        form.nickname = userStore.user.nickname
        form.avatar = userStore.user.avatar || systemAvatars[0]
        form.gender = userStore.user.gender ?? 2
    }
})

const fullAvatarUrl = (url: string) => {
    if (!url) return ''
    if (url.startsWith('http')) return url
    // If it's a relative path from our backend uploads
    return `http://localhost:8080${url}` 
}

const beforeAvatarUpload = (rawFile: any) => {
  if (rawFile.size / 1024 / 1024 > 1) {
    ElMessage.error('Avatar picture size can not exceed 1MB!')
    return false
  }
  return true
}

const handleAvatarSuccess = (response: any) => {
    if (response.code === 200) {
        form.avatar = response.data
        ElMessage.success('上传成功')
    } else {
        ElMessage.error(response.msg || '上传失败')
    }
}

const selectSystemAvatar = (url: string) => {
    form.avatar = url
}

const saveProfile = async () => {
    if (form.nickname.length < 2) {
        ElMessage.warning('昵称长度至少2位')
        return
    }
    
    loading.value = true
    try {
        const res: any = await request.put('/user/profile', {
            id: form.id,
            nickname: form.nickname,
            avatar: form.avatar,
            gender: form.gender
        })
        if (res.code === 200) {
            ElMessage.success('保存成功')
            // Update store
            userStore.user!.nickname = form.nickname
            userStore.user!.avatar = form.avatar
            userStore.user!.gender = form.gender
            // Persist if using localstorage sync mechanism
        } else {
            ElMessage.error(res.msg || '保存失败')
        }
    } catch (e) {
        console.error(e)
    } finally {
        loading.value = false
    }
}
</script>

<style scoped lang="scss">
.profile-container {
    padding: 20px 40px;
    
    h2 {
        margin-bottom: 30px;
        color: #333;
    }
    
    .profile-card {
        background: #fff;
        padding: 40px;
        border-radius: 12px;
        display: flex;
        gap: 60px;
        align-items: flex-start;
        box-shadow: 0 4px 12px rgba(0,0,0,0.05);

        .avatar-section {
            width: 200px;
            display: flex;
            flex-direction: column;
            align-items: center;

            .avatar-uploader {
                width: 120px;
                height: 120px;
                border-radius: 50%;
                overflow: hidden;
                position: relative;
                cursor: pointer;
                border: 2px solid #e0e0e0;

                .avatar {
                    width: 100%;
                    height: 100%;
                    object-fit: cover;
                }

                .avatar-uploader-icon {
                    font-size: 28px;
                    color: #8c939d;
                    width: 120px;
                    height: 120px;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    background: #f5f7fa;
                }

                .hover-mask {
                    position: absolute;
                    top: 0; left: 0; right: 0; bottom: 0;
                    background: rgba(0,0,0,0.5);
                    color: #fff;
                    display: flex;
                    flex-direction: column;
                    justify-content: center;
                    align-items: center;
                    opacity: 0;
                    transition: opacity 0.3s;
                }

                &:hover .hover-mask {
                    opacity: 1;
                }
            }
            
            .avatar-tip {
                margin-top: 10px;
                font-size: 12px;
                color: #999;
                text-align: center;
            }
            
            .system-avatars {
                 margin-top: 20px;
                 width: 100%;
                 
                 span {
                     font-size: 13px;
                     color: #666;
                     margin-bottom: 8px;
                     display: block;
                 }
                 
                 .avatar-list {
                     display: flex;
                     gap: 10px;
                     justify-content: center;
                     
                     .sys-avatar {
                         width: 40px;
                         height: 40px;
                         border-radius: 50%;
                         cursor: pointer;
                         border: 2px solid transparent;
                         
                         &:hover {
                             border-color: #059669;
                         }
                     }
                 }
            }
        }

        .profile-form {
            flex: 1;
            max-width: 500px;
        }
    }
}
</style>
