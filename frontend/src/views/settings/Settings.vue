<template>
  <div class="settings-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>个性化设置</span>
        </div>
      </template>

      <el-tabs :tab-position="tabPosition" class="settings-tabs">
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

                    <el-divider />

                    <h3>首页卡片展示</h3>
                    <el-form-item label="可选卡片">
                        <el-checkbox-group v-model="settings.dashboardCards" @change="saveSettings">
                            <el-checkbox
                                v-for="item in dashboardCardOptions"
                                :key="item.key"
                                :label="item.key"
                            >
                                {{ item.label }}
                            </el-checkbox>
                        </el-checkbox-group>
                        <p class="hint">固定展示：一周健康趋势、今日餐次热量结构（不可移除）。</p>
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
                        <el-switch v-model="medicationReminder" @change="syncMedicationReminder" />
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
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '../../stores/user'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../api/request'

const userStore = useUserStore()
const router = useRouter()
const settings = computed(() => userStore.settings)
const medicationReminder = ref(localStorage.getItem('medicationReminder') === 'true')
const tabPosition = ref<'left' | 'top'>('left')
const defaultDashboardCards = ['bmi', 'bp', 'meal', 'record', 'glucoseAvg', 'pendingTasks', 'profileCompletion']
const dashboardCardOptions = [
    { key: 'bmi', label: 'BMI 指数' },
    { key: 'bp', label: '最近血压' },
    { key: 'meal', label: '今日餐次记录' },
    { key: 'record', label: '今日健康记录' },
    { key: 'glucoseAvg', label: '本周平均血糖' },
    { key: 'pendingTasks', label: '待处理控糖任务' },
    { key: 'profileCompletion', label: '健康档案完整度' }
]

const previewSize = computed(() => {
    const sizes = [14, 17.5, 21] // 14px * 1.0, 1.25, 1.5
    return sizes[settings.value.fontSize]
})

const updateFont = async (size: number) => {
    userStore.updateSettings({ fontSize: size })
    await saveSettingsToServer()
    ElMessage.success('字号已调整')
}

const saveSettings = async () => {
    userStore.updateSettings({ ...settings.value })
    await saveSettingsToServer()
    ElMessage.success('设置已自动保存')
}

const saveSettingsToServer = async () => {
    try {
        const payload = {
            ...settings.value,
            dashboardCards: JSON.stringify(settings.value.dashboardCards || defaultDashboardCards)
        }
        await request.put('/user/settings', payload)
    } catch (e) {
        console.error(e)
    }
}

const exportData = async () => {
    ElMessage.success('正在准备数据导出，请稍候...')
    try {
        const token = localStorage.getItem('token') || sessionStorage.getItem('token')
        const response = await fetch('/api/user/export-data', {
            method: 'POST',
            headers: {
                ...(token ? { Authorization: `Bearer ${token}` } : {})
            }
        })
        if (!response.ok) throw new Error(response.statusText)
        const blob = await response.blob()
        const url = window.URL.createObjectURL(blob)
        const element = document.createElement('a')
        element.href = url
        element.download = 'user_data.txt'
        document.body.appendChild(element)
        element.click()
        document.body.removeChild(element)
        window.URL.revokeObjectURL(url)
    } catch (e) {
        console.error(e)
        ElMessage.error('导出失败，请稍后重试')
    }
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

const loadSettings = async () => {
    try {
        const res: any = await request.get('/user/settings')
        if (res.code === 200 && res.data) {
            const serverData = { ...res.data }
            serverData.dashboardCards = normalizeDashboardCards(serverData.dashboardCards)
            userStore.updateSettings(serverData)
        }
    } catch (e) {
        console.error(e)
    }
}

const normalizeDashboardCards = (raw: unknown): string[] => {
    if (Array.isArray(raw)) {
        const list = raw.filter((item) => typeof item === 'string') as string[]
        return list.length > 0 ? list : [...defaultDashboardCards]
    }
    if (typeof raw === 'string') {
        try {
            const parsed = JSON.parse(raw)
            if (Array.isArray(parsed)) {
                const list = parsed.filter((item) => typeof item === 'string') as string[]
                return list.length > 0 ? list : [...defaultDashboardCards]
            }
        } catch (e) {
            console.warn('parse dashboardCards failed', e)
        }
    }
    return [...defaultDashboardCards]
}

const syncMedicationReminder = () => {
    localStorage.setItem('medicationReminder', String(medicationReminder.value))
}

const updateTabPosition = () => {
    tabPosition.value = window.innerWidth <= 900 ? 'top' : 'left'
}

onMounted(() => {
    loadSettings()
    updateTabPosition()
    window.addEventListener('resize', updateTabPosition)
})

onUnmounted(() => {
    window.removeEventListener('resize', updateTabPosition)
})
</script>

<style scoped lang="scss">
.settings-page {
    /* Full width handled by MainLayout now */
}

.settings-tabs {
    min-height: 400px;
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
