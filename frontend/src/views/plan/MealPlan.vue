<template>
  <div class="meal-plan">
    <el-card>
        <template #header>
            <div class="card-header">
            <span>智能食谱中心</span>
            </div>
        </template>
        
        <div class="action-area">
            <div class="intro">
                <el-icon class="intro-icon"><MagicStick /></el-icon>
                <p>基于您的健康档案（如：<strong>高血压</strong>、<strong>痛风</strong>），AI 营养师将为您生成专属食谱。</p>
            </div>

            <div class="requirements">
                <el-input
                    v-model="requirements"
                    type="textarea"
                    :rows="3"
                    placeholder="请输入需求（如：低盐、少油、口味偏清淡、预算、份数、忌口）"
                />
                <div class="import-row">
                    <el-input v-model="importTitle" placeholder="导入标题（可选）" />
                    <el-upload
                        class="import-upload"
                        :show-file-list="false"
                        accept=".xlsx,.pdf"
                        :http-request="handleImport"
                    >
                        <el-button type="info" size="large" :loading="importing">导入 Excel/PDF</el-button>
                    </el-upload>
                </div>
            </div>
            
            <div class="btn-group">
                <el-radio-group v-model="rangeType" class="range-selector">
                    <el-radio-button label="MEAL">一顿饭</el-radio-button>
                    <el-radio-button label="DAY">一天</el-radio-button>
                    <el-radio-button label="THREE_DAYS">三天</el-radio-button>
                    <el-radio-button label="WEEK">一周</el-radio-button>
                </el-radio-group>
                <el-button type="primary" size="large" @click="generatePlan" :loading="loading" class="generate-btn">
                    生成推荐食谱
                </el-button>
                <el-button v-if="generatedPlan" type="success" size="large" @click="savePlan" icon="Document">
                    保存到食谱中心
                </el-button>
                <el-button v-if="generatedPlan" type="warning" size="large" @click="downloadPdf()" icon="Download">
                    导出食谱 PDF
                </el-button>
            </div>
        </div>

        <el-divider v-if="generatedPlan" />

        <div v-if="generatedPlan" class="plan-preview">
            <div class="plan-header">
                <h3><el-icon><Calendar /></el-icon> {{ generatedPlan.title || 'AI 推荐食谱' }}</h3>
                <el-alert :title="generatedPlan.advice || '请结合自身情况参考执行'" type="success" :closable="false" effect="dark" />
            </div>
            
            <el-table :data="planRows" border stripe style="margin-top: 20px">
                <el-table-column prop="day" label="日期" width="100" align="center" />
                <el-table-column label="早餐">
                    <template #default="scope">
                        <div class="meal-cell">
                            <span class="meal-name">{{ mealName(scope.row.breakfast) }}</span>
                            <span class="meal-cal">{{ mealCalories(scope.row.breakfast) }}</span>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column label="午餐">
                    <template #default="scope">
                         <div class="meal-cell">
                            <span class="meal-name">{{ mealName(scope.row.lunch) }}</span>
                            <span class="meal-cal">{{ mealCalories(scope.row.lunch) }}</span>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column label="晚餐">
                    <template #default="scope">
                         <div class="meal-cell">
                            <span class="meal-name">{{ mealName(scope.row.dinner) }}</span>
                            <span class="meal-cal">{{ mealCalories(scope.row.dinner) }}</span>
                        </div>
                    </template>
                </el-table-column>
            </el-table>
            
            <div class="shopping-list">
                <h4><el-icon><ShoppingCart /></el-icon> 采购清单</h4>
                <div class="tags-container">
                    <el-tag v-for="([item, qty]) in shoppingEntries" :key="item" size="large" effect="plain" class="shop-tag">
                        {{ item }}: <strong>{{ qty }}</strong>
                    </el-tag>
                </div>
            </div>
        </div>
    </el-card>

    <el-card class="saved-card">
        <template #header>
            <div class="card-header">
                <span>已保存食谱</span>
                <el-button size="small" @click="fetchSavedPlans">刷新</el-button>
            </div>
        </template>
        <el-table :data="savedPlans" style="width: 100%">
            <el-table-column prop="title" label="标题" />
            <el-table-column prop="rangeType" label="周期" width="120">
                <template #default="scope">
                    {{ rangeLabel(scope.row.rangeType) }}
                </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="180">
                <template #default="scope">
                    {{ formatDate(scope.row.createdAt) }}
                </template>
            </el-table-column>
            <el-table-column label="操作" width="180">
                <template #default="scope">
                    <el-button link type="primary" @click="loadPlan(scope.row.id)">查看</el-button>
                    <el-button link type="warning" @click="downloadPdf(scope.row.id)">导出</el-button>
                </template>
            </el-table-column>
        </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import request from '../../api/request'

const loading = ref(false)
const generatedPlan = ref<any>(null)
const rangeType = ref('WEEK')
const savedPlans = ref<any[]>([])
const requirements = ref('')
const importTitle = ref('')
const importing = ref(false)

const planRows = computed(() => generatedPlan.value?.weeklyPlan || [])
const shoppingEntries = computed(() => {
    const list = generatedPlan.value?.shoppingList || {}
    return Object.entries(list)
})

const generatePlan = async () => {
    loading.value = true
    try {
        const params: Record<string, string> = { range: rangeType.value }
        if (requirements.value.trim()) {
            params.requirements = requirements.value.trim()
        }
        const res: any = await request.get('/meal-plan/generate', { params })
        if (res.code === 200) {
            generatedPlan.value = res.data
        } else {
            ElMessage.error(res.msg || '生成失败')
        }
    } catch (e) {
        console.error(e)
    } finally {
        loading.value = false
    }
}

const savePlan = async () => {
    if (!generatedPlan.value) return
    try {
        const res: any = await request.post('/meal-plan', {
            rangeType: rangeType.value,
            plan: generatedPlan.value
        })
        if (res.code === 200) {
            ElMessage.success('已保存到食谱中心')
            fetchSavedPlans()
        } else {
            ElMessage.error(res.msg || '保存失败')
        }
    } catch (e) {
        console.error(e)
    }
}

const fetchSavedPlans = async () => {
    try {
        const res: any = await request.get('/meal-plan')
        if (res.code === 200) {
            savedPlans.value = res.data || []
        }
    } catch (e) {
        console.error(e)
    }
}

const loadPlan = async (id: number) => {
    try {
        const res: any = await request.get(`/meal-plan/${id}`)
        if (res.code === 200) {
            generatedPlan.value = res.data
            const matched = savedPlans.value.find((item) => item.id === id)
            if (matched?.rangeType) {
                rangeType.value = matched.rangeType
            }
        } else {
            ElMessage.error(res.msg || '加载失败')
        }
    } catch (e) {
        console.error(e)
    }
}

const handleImport = async (options: any) => {
    importing.value = true
    try {
        const formData = new FormData()
        formData.append('file', options.file)
        if (importTitle.value.trim()) {
            formData.append('title', importTitle.value.trim())
        }
        const res: any = await request.post('/meal-plan/import', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        })
        if (res.code === 200) {
            generatedPlan.value = res.data.plan
            if (res.data.rangeType) {
                rangeType.value = res.data.rangeType
            }
            ElMessage.success('导入成功')
            fetchSavedPlans()
        }
        if (options.onSuccess) {
            options.onSuccess(res, options.file)
        }
    } catch (e) {
        console.error(e)
        if (options.onError) {
            options.onError(e)
        }
        ElMessage.error('导入失败，请检查文件格式')
    } finally {
        importing.value = false
    }
}

const downloadPdf = (planId?: number) => {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token')
    const params = new URLSearchParams()
    if (planId) {
        params.set('planId', String(planId))
    } else {
        params.set('range', rangeType.value)
    }
    fetch(`/api/export/pdf?${params.toString()}`, {
        method: 'GET',
        headers: {
            ...(token ? { Authorization: `Bearer ${token}` } : {})
        }
    }).then(async (response) => {
        if (!response.ok) throw new Error(response.statusText)
        const blob = await response.blob()
        const url = window.URL.createObjectURL(blob)
        const element = document.createElement('a')
        element.href = url
        element.download = planId ? `meal_plan_${planId}.pdf` : 'meal_plan.pdf'
        document.body.appendChild(element)
        element.click()
        document.body.removeChild(element)
        window.URL.revokeObjectURL(url)
    }).catch((e) => {
        console.error(e)
        ElMessage.error('导出失败，请稍后重试')
    })
}

const rangeLabel = (value: string) => {
    const map: Record<string, string> = {
        MEAL: '一顿饭',
        DAY: '一天',
        THREE_DAYS: '三天',
        WEEK: '一周'
    }
    const key = value ? value.toUpperCase() : ''
    return map[key] || value
}

const mealName = (meal: any) => {
    if (!meal) return '--'
    if (meal.name) return meal.name
    if (Array.isArray(meal.foods) && meal.foods.length) {
        return meal.foods.join('、')
    }
    return '--'
}

const mealCalories = (meal: any) => {
    if (!meal || !meal.calories) return ''
    return `${meal.calories}`
}

const formatDate = (val: string) => {
    if (!val) return '--'
    return dayjs(val).format('YYYY-MM-DD HH:mm')
}

onMounted(() => {
    fetchSavedPlans()
})
</script>

<style scoped lang="scss">
.action-area {
    text-align: center;
    padding: 40px;
    background: linear-gradient(to bottom, #fdf6ec, #fff);
    border-radius: 12px;
    margin-bottom: 20px;
    
    .intro {
        margin-bottom: 24px;
        color: #666;
        .intro-icon {
            font-size: 48px;
            color: #F59E0B;
            margin-bottom: 12px;
        }
    }
    
    .generate-btn {
        padding: 12px 40px;
        font-size: 16px;
    }

    .btn-group {
        display: flex;
        flex-wrap: wrap;
        justify-content: center;
        gap: 12px;
        margin-top: 10px;
    }

    .range-selector {
        margin-right: 8px;
    }
}

.requirements {
    max-width: 880px;
    margin: 0 auto 18px auto;
    text-align: left;

    .import-row {
        display: flex;
        gap: 12px;
        margin-top: 12px;
        flex-wrap: wrap;
        align-items: center;
    }

    .import-upload {
        display: inline-block;
    }
}

.plan-preview {
    animation: fadeIn 0.5s ease;
    
    .plan-header {
        margin-bottom: 20px;
        h3 {
             display: flex;
             align-items: center;
             gap: 10px;
             color: #333;
        }
    }
    
    .meal-cell {
        display: flex;
        flex-direction: column;
        .meal-name {
            font-weight: bold;
            color: #333;
        }
        .meal-cal {
            font-size: 12px;
            color: #999;
        }
    }
}

.shopping-list {
    margin-top: 30px;
    padding: 24px;
    background: #f0f9eb;
    border-radius: 12px;
    border: 1px solid #e1f3d8;
    
    h4 {
         display: flex;
         align-items: center;
         gap: 10px;
         color: #67C23A;
         margin-top: 0;
    }
    
    .tags-container {
        display: flex;
        flex-wrap: wrap;
        gap: 12px;
        
        .shop-tag {
            background-color: #fff;
        }
    }
}

.saved-card {
    margin-top: 20px;
}

@keyframes fadeIn {
    from { opacity: 0; transform: translateY(10px); }
    to { opacity: 1; transform: translateY(0); }
}
</style>
