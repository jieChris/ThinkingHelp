<template>
  <div class="settings-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>个性化设置</span>
        </div>
      </template>

      <el-tabs tab-position="left" style="height: 400px" class="settings-tabs">
        <!-- Tab 1: Account Security -->
        <el-tab-pane label="账号安全">
            <div class="tab-content">
                <h3>修改密码</h3>
                <el-form label-width="100px" style="max-width: 400px">
                    <el-form-item label="旧密码">
                        <el-input type="password" />
                    </el-form-item>
                    <el-form-item label="新密码">
                        <el-input type="password" />
                    </el-form-item>
                    <el-form-item label="确认新密码">
                        <el-input type="password" />
                    </el-form-item>
                    <el-form-item>
                        <el-button type="primary">更新密码</el-button>
                    </el-form-item>
                </el-form>
            </div>
        </el-tab-pane>

        <!-- Tab 2: Display & Preferences -->
        <el-tab-pane label="显示与偏好">
             <div class="tab-content">
                <h3>界面设置</h3>
                <el-form label-width="120px">
                    <el-form-item label="全局字号">
                        <div class="font-size-control">
                            <el-button-group>
                                <el-button :type="settings.fontSize === 0 ? 'primary' : ''" @click="updateFont(0)">标准</el-button>
                                <el-button :type="settings.fontSize === 1 ? 'primary' : ''" @click="updateFont(1)">大号</el-button>
                                <el-button :type="settings.fontSize === 2 ? 'primary' : ''" @click="updateFont(2)">特大</el-button>
                            </el-button-group>
                            <span class="preview-text" :style="{ fontSize: previewSize + 'px' }">预览文字效果</span>
                        </div>
                    </el-form-item>
                    
                    <el-divider />
                    
                    <h3>AI 偏好</h3>
                    <el-form-item label="AI 性格">
                        <el-radio-group v-model="settings.aiPersona" @change="saveSettings">
                            <el-radio label="strict" border>严谨专业</el-radio>
                            <el-radio label="gentle" border>亲切温柔</el-radio>
                        </el-radio-group>
                        <p class="hint">严谨模式下 AI 将更注重医学数据的准确性；温柔模式下 AI 将提供更多情感支持。</p>
                    </el-form-item>
                </el-form>
             </div>
        </el-tab-pane>

        <!-- Tab 3: Notifications -->
        <el-tab-pane label="消息通知">
             <div class="tab-content">
                <h3>通知设置</h3>
                <el-form label-width="120px">
                    <el-form-item label="接收系统通知">
                        <el-switch v-model="settings.notificationEnabled" @change="saveSettings" />
                    </el-form-item>
                     <el-form-item label="用药提醒">
                        <el-switch v-model="medicationReminder" />
                    </el-form-item>
                </el-form>
             </div>
        </el-tab-pane>

        <!-- Tab 4: Privacy & Data -->
        <el-tab-pane label="隐私数据">
             <div class="tab-content">
                <h3>数据管理</h3>
                <p>您可以导出您的所有健康数据，包括体检报告、饮食记录和对话历史。</p>
                <el-button type="info" plain icon="Download" @click="exportData">导出个人数据</el-button>
             </div>
        </el-tab-pane>

        <!-- Tab 5: System -->
        <el-tab-pane label="系统操作">
            <div class="tab-content">
                <h3>退出登录</h3>
                <p class="hint">退出后将无法接收消息提醒。</p>
                <el-button type="danger" size="large" icon="SwitchButton" @click="handleLogout" style="width: 200px">退出当前账号</el-button>
            </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useUserStore } from '../../stores/user'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()
const router = useRouter()
const settings = computed(() => userStore.settings)
const medicationReminder = ref(true)

const previewSize = computed(() => {
    const sizes = [14, 17.5, 21] // 14px * 1.0, 1.25, 1.5
    return sizes[settings.value.fontSize]
})

const updateFont = (size: number) => {
    userStore.updateSettings({ fontSize: size })
    ElMessage.success('字号已调整')
}

const saveSettings = () => {
    userStore.updateSettings({ ...settings.value })
    ElMessage.success('设置已自动保存')
}

const exportData = () => {
    ElMessage.success('正在准备数据导出，请稍候...')
    // Mock export
    setTimeout(() => {
        const element = document.createElement('a');
        element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent('User Data Export Mock'));
        element.setAttribute('download', 'user_data.txt');
        element.style.display = 'none';
        document.body.appendChild(element);
        element.click();
        document.body.removeChild(element);
    }, 1000)
}

const handleLogout = () => {
    ElMessageBox.confirm(
        '确定要退出登录吗?',
        '提示',
        {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
        }
    ).then(() => {
        userStore.logout()
        ElMessage.success('已安全退出')
        router.push('/login')
    }).catch(() => {
        // cancel
    })
}
</script>

<style scoped lang="scss">
.settings-page {
    /* Full width handled by MainLayout now */
}

.tab-content {
    padding: 0 20px;
    h3 {
        margin-top: 0;
        margin-bottom: 20px;
        color: #333;
    }
}

.font-size-control {
    display: flex;
    align-items: center;
    gap: 20px;
    
    .preview-text {
        color: #666;
    }
}

.hint {
    font-size: 12px;
    color: #999;
    margin-top: 5px;
}
</style>
