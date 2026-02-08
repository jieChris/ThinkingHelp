<template>
  <div class="meal-plan-page">
  <div class="meal-plan">
    <el-card>
        <template #header>
            <div class="card-header">
            <span>智能食谱中心</span>
            </div>
        </template>
        
        <div class="action-toggle" v-if="generatedPlan">
            <el-button size="small" @click="toggleActions">
                {{ showActions ? '隐藏生成/导入' : '显示生成/导入' }}
            </el-button>
        </div>

        <div class="action-area" v-show="showActions">
            <div class="intro">
                <el-icon class="intro-icon"><MagicStick /></el-icon>
                <p>基于您的健康档案（如：<strong>高血压</strong>、<strong>痛风</strong>），智能营养师将为您生成专属食谱。</p>
            </div>

            <div class="requirements">
                <el-input
                    v-model="requirements"
                    type="textarea"
                    :rows="3"
                    placeholder="请输入需求（如：低盐、少油、口味偏清淡、预算、份数、忌口）"
                />
                <div class="history-row" v-if="requirementHistory.length">
                    <span class="history-label">历史需求：</span>
                    <div class="history-tags">
                        <el-tag
                            v-for="item in requirementHistory"
                            :key="item"
                            size="small"
                            effect="plain"
                            class="history-tag"
                            @click="applyRequirement(item)"
                        >
                            {{ item }}
                        </el-tag>
                    </div>
                </div>
                <div class="import-row">
                    <el-input v-model="importTitle" placeholder="导入标题（可选）" />
                    <el-upload
                        class="import-upload"
                        :show-file-list="false"
                        accept=".xlsx,.pdf"
                        :http-request="handleImport"
                    >
                        <el-button type="info" size="large" :loading="importing">导入表格或文档</el-button>
                    </el-upload>
                </div>
            </div>

            <el-alert
                v-if="feedbackPreviewNotes.length"
                type="info"
                :closable="false"
                class="preview-alert"
                title="下次生成预计调整（基于最近30天反馈）"
            >
                <template #default>
                    <ul class="preview-list">
                        <li v-for="note in feedbackPreviewNotes" :key="note">{{ note }}</li>
                    </ul>
                </template>
            </el-alert>
            <div v-else-if="!feedbackPreviewLoading" class="preview-empty">暂无历史反馈调整项</div>
            
            <div class="btn-group">
                <el-radio-group v-model="rangeType" class="range-selector">
                    <el-radio-button label="MEAL">一顿饭</el-radio-button>
                    <el-radio-button label="DAY">一天</el-radio-button>
                    <el-radio-button label="THREE_DAYS">三天</el-radio-button>
                    <el-radio-button label="WEEK">一周</el-radio-button>
                </el-radio-group>
                <el-button type="primary" size="large" @click="generatePlan" :loading="loading || generating" class="generate-btn">
                    {{ generating ? '生成中...' : '生成推荐食谱' }}
                </el-button>
                <el-tag v-if="generatedPlan" type="success" size="large" effect="plain">
                    已自动保存到食谱中心
                </el-tag>
                <el-button
                    v-if="generatedPlan"
                    type="warning"
                    size="large"
                    @click="downloadPdf()"
                    icon="Download"
                    :loading="exportingPdf && exportingPlanId === null"
                >
                    导出食谱文档
                </el-button>
            </div>
        </div>

        <el-divider v-if="generatedPlan" />

        <div v-if="generatedPlan" class="plan-preview">
            <div class="plan-header">
                <h3><el-icon><Calendar /></el-icon> {{ generatedPlan.title || '智能推荐食谱' }}</h3>
                <el-alert :title="generatedPlan.advice || '请结合自身情况参考执行'" type="success" :closable="false" effect="dark" />
                <div v-if="targetSummary.length" class="target-row">
                    <el-tag v-for="item in targetSummary" :key="item.label" size="large" effect="plain" type="info">
                        {{ item.label }}: {{ item.value }}
                    </el-tag>
                </div>
                <el-alert
                    v-if="adjustmentNotes.length"
                    type="info"
                    :closable="false"
                    class="adjustment-alert"
                    title="本次食谱已根据你的反馈做了调整"
                >
                    <template #default>
                        <ul class="adjustment-list">
                            <li v-for="note in adjustmentNotes" :key="note">{{ note }}</li>
                        </ul>
                    </template>
                </el-alert>
            </div>
            
            <el-table :data="planRows" border stripe style="margin-top: 20px">
                <el-table-column prop="day" label="日期" width="100" align="center" />
                <el-table-column label="早餐">
                    <template #default="scope">
                        <div class="meal-cell">
                            <span class="meal-name">{{ mealName(scope.row.breakfast) }}</span>
                            <span class="meal-cal">{{ mealCalories(scope.row.breakfast) }}</span>
                            <span class="meal-nutrients">{{ mealNutritionSummary(scope.row.breakfast) }}</span>
                            <div class="feedback-row">
                                <el-button
                                    text
                                    size="small"
                                    @click="sendFeedback('BREAKFAST', 'MORE')"
                                    :loading="feedbackLoading['BREAKFAST_MORE']"
                                >不够吃</el-button>
                                <el-button
                                    text
                                    size="small"
                                    type="warning"
                                    @click="sendFeedback('BREAKFAST', 'LESS')"
                                    :loading="feedbackLoading['BREAKFAST_LESS']"
                                >太多了</el-button>
                            </div>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column label="午餐">
                    <template #default="scope">
                         <div class="meal-cell">
                            <span class="meal-name">{{ mealName(scope.row.lunch) }}</span>
                            <span class="meal-cal">{{ mealCalories(scope.row.lunch) }}</span>
                            <span class="meal-nutrients">{{ mealNutritionSummary(scope.row.lunch) }}</span>
                            <div class="feedback-row">
                                <el-button
                                    text
                                    size="small"
                                    @click="sendFeedback('LUNCH', 'MORE')"
                                    :loading="feedbackLoading['LUNCH_MORE']"
                                >不够吃</el-button>
                                <el-button
                                    text
                                    size="small"
                                    type="warning"
                                    @click="sendFeedback('LUNCH', 'LESS')"
                                    :loading="feedbackLoading['LUNCH_LESS']"
                                >太多了</el-button>
                            </div>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column label="晚餐">
                    <template #default="scope">
                         <div class="meal-cell">
                            <span class="meal-name">{{ mealName(scope.row.dinner) }}</span>
                            <span class="meal-cal">{{ mealCalories(scope.row.dinner) }}</span>
                            <span class="meal-nutrients">{{ mealNutritionSummary(scope.row.dinner) }}</span>
                            <div class="feedback-row">
                                <el-button
                                    text
                                    size="small"
                                    @click="sendFeedback('DINNER', 'MORE')"
                                    :loading="feedbackLoading['DINNER_MORE']"
                                >不够吃</el-button>
                                <el-button
                                    text
                                    size="small"
                                    type="warning"
                                    @click="sendFeedback('DINNER', 'LESS')"
                                    :loading="feedbackLoading['DINNER_LESS']"
                                >太多了</el-button>
                            </div>
                        </div>
                    </template>
                </el-table-column>
            </el-table>
            
            <el-collapse v-model="shoppingCollapse" class="shopping-collapse">
                <el-collapse-item name="batch">
                    <template #title>
                        <el-icon><ShoppingCart /></el-icon> 按批次采购（可选）
                    </template>
                    <div class="batch-planner" v-if="dailyShopping.length">
                        <div class="batch-controls">
                            <el-radio-group v-model="purchaseMode" size="small">
                                <el-radio-button label="FULL">全周期一次采购</el-radio-button>
                                <el-radio-button label="FOUR_THREE">4天+剩余天数</el-radio-button>
                                <el-radio-button label="CUSTOM">自定义批次</el-radio-button>
                            </el-radio-group>
                        </div>
                        <div v-if="purchaseMode === 'CUSTOM'" class="custom-batch-editor">
                            <el-select v-model="customStartIndex" placeholder="开始日期" style="width: 160px">
                                <el-option
                                    v-for="opt in dayIndexOptions"
                                    :key="`start-${opt.value}`"
                                    :label="opt.label"
                                    :value="opt.value"
                                />
                            </el-select>
                            <el-select v-model="customEndIndex" placeholder="结束日期" style="width: 160px">
                                <el-option
                                    v-for="opt in dayIndexOptions"
                                    :key="`end-${opt.value}`"
                                    :label="opt.label"
                                    :value="opt.value"
                                />
                            </el-select>
                            <el-button type="primary" @click="addCustomBatch">添加批次</el-button>
                            <el-button @click="clearCustomBatches" :disabled="!customBatches.length">清空</el-button>
                        </div>

                        <div v-if="purchaseMode === 'CUSTOM' && customBatches.length" class="custom-batch-list">
                            <el-tag
                                v-for="batch in customBatches"
                                :key="batch.id"
                                closable
                                effect="plain"
                                @close="removeCustomBatch(batch.id)"
                            >
                                第{{ batch.id }}批：{{ formatBatchRange(batch.startIndex, batch.endIndex) }}
                            </el-tag>
                        </div>

                        <div v-if="purchaseBatches.length" class="batch-result-list">
                            <div v-for="batch in purchaseBatches" :key="batch.key" class="batch-result-card">
                                <div class="batch-result-title">
                                    <span>{{ batch.title }}</span>
                                    <span class="batch-result-subtitle">{{ batch.subtitle }}</span>
                                </div>
                                <div class="tags-container">
                                    <el-tag v-for="item in batch.items" :key="item" size="large" effect="plain" class="shop-tag">
                                        {{ formatShoppingText(item) }}
                                    </el-tag>
                                </div>
                            </div>
                        </div>
                        <div v-else class="empty-note">当前没有可用批次，请先添加自定义批次</div>
                    </div>
                    <div v-else class="empty-note">暂无可分批的采购数据</div>
                </el-collapse-item>
                <el-collapse-item name="daily">
                    <template #title>
                        <el-icon><ShoppingCart /></el-icon> 每日采购清单
                    </template>
                    <div v-if="dailyShopping.length" class="daily-shopping">
                        <div v-for="daily in dailyShopping" :key="daily.day" class="daily-block">
                            <div class="daily-title">{{ daily.day }}</div>
                            <div class="tags-container">
                                <el-tag v-for="item in daily.items" :key="item" size="large" effect="plain" class="shop-tag">
                                    {{ formatShoppingText(item) }}
                                </el-tag>
                            </div>
                        </div>
                    </div>
                    <div v-else class="empty-note">暂无每日采购清单</div>
                </el-collapse-item>
                <el-collapse-item name="pantry">
                    <template #title>
                        <el-icon><ShoppingCart /></el-icon> 长期调料（常备）
                    </template>
                    <div class="tags-container" v-if="pantryItems.length">
                        <el-tag v-for="item in pantryItems" :key="item" size="large" effect="plain" class="shop-tag">
                            {{ formatShoppingText(item) }}
                        </el-tag>
                    </div>
                    <div v-else class="empty-note">暂无调料清单</div>
                </el-collapse-item>
            </el-collapse>
        </div>
    </el-card>

    <el-card class="saved-card">
        <template #header>
            <div class="card-header">
                <span>已保存食谱</span>
                <div class="saved-actions">
                    <el-radio-group v-model="savedPlanFilter" size="small">
                        <el-radio-button label="ALL">全部</el-radio-button>
                        <el-radio-button label="ADJUSTED">仅已调整</el-radio-button>
                        <el-radio-button label="PLAIN">仅未调整</el-radio-button>
                    </el-radio-group>
                    <el-select v-model="savedPlanTimeFilter" size="small" style="width: 140px">
                        <el-option label="全部时间" value="ALL" />
                        <el-option label="本周" value="WEEK" />
                        <el-option label="近30天" value="DAYS_30" />
                    </el-select>
                    <el-input
                        v-model="savedPlanKeyword"
                        size="small"
                        style="width: 180px"
                        clearable
                        placeholder="搜索标题/调整说明"
                    />
                    <el-select v-model="savedPlanSort" size="small" style="width: 120px">
                        <el-option label="时间倒序" value="DESC" />
                        <el-option label="时间正序" value="ASC" />
                    </el-select>
                    <el-button size="small" @click="resetSavedPlanFilters">重置筛选</el-button>
                    <el-button size="small" @click="fetchSavedPlans">刷新</el-button>
                </div>
            </div>
        </template>
        <el-table :data="savedPlans" style="width: 100%">
            <el-table-column prop="title" label="标题" />
            <el-table-column prop="rangeType" label="周期" width="120">
                <template #default="scope">
                    {{ rangeLabel(scope.row.rangeType) }}
                </template>
            </el-table-column>
            <el-table-column label="反馈调整" min-width="260">
                <template #default="scope">
                    <div v-if="scope.row.hasAdjustment" class="plan-adjustment">
                        <el-tag size="small" type="success" effect="plain">已调整</el-tag>
                        <span class="adjustment-text">{{ scope.row.adjustmentSummary || '已按历史反馈调整' }}</span>
                    </div>
                    <span v-else class="no-adjustment">无</span>
                </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="180">
                <template #default="scope">
                    {{ formatDate(scope.row.createdAt) }}
                </template>
            </el-table-column>
            <el-table-column label="操作" width="240">
                <template #default="scope">
                    <el-button link type="primary" @click="loadPlan(scope.row.id)">查看</el-button>
                    <el-button
                        link
                        type="warning"
                        @click="downloadPdf(scope.row.id)"
                        :loading="exportingPdf && exportingPlanId === scope.row.id"
                    >导出</el-button>
                    <el-popconfirm
                        title="确认删除该食谱？"
                        confirm-button-text="删除"
                        cancel-button-text="取消"
                        @confirm="deletePlan(scope.row.id)"
                    >
                        <template #reference>
                            <el-button link type="danger">删除</el-button>
                        </template>
                    </el-popconfirm>
                </template>
            </el-table-column>
        </el-table>
        <el-pagination
            v-if="savedPlansTotal > 0"
            v-model:current-page="savedPlanPage"
            v-model:page-size="savedPlanPageSize"
            class="saved-pagination"
            background
            layout="total, sizes, prev, pager, next"
            :total="savedPlansTotal"
            :page-sizes="[10, 20, 50]"
        />
    </el-card>
  </div>

  <el-dialog v-model="profilePromptVisible" title="生成食谱前的提醒" width="520px">
      <div class="prompt-body">
          <p>推荐先完善健康档案中的关键数据，再生成更贴合你的食谱。</p>
          <p v-if="missingProfileLabels.length" class="missing-title">建议补充：</p>
          <ul v-if="missingProfileLabels.length" class="missing-list">
              <li v-for="item in missingProfileLabels" :key="item">{{ item }}</li>
          </ul>
          <p class="prompt-note">你也可以选择继续生成，但准确度可能降低。</p>
      </div>
      <template #footer>
          <el-button @click="goToHealthProfile">去记录</el-button>
          <el-button type="primary" @click="continueGenerate">继续生成</el-button>
      </template>
  </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import request from '../../api/request'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const generatedPlan = ref<any>(null)
const rangeType = ref('WEEK')
const savedPlans = ref<any[]>([])
const savedPlanFilter = ref<'ALL' | 'ADJUSTED' | 'PLAIN'>('ALL')
const savedPlanTimeFilter = ref<'ALL' | 'WEEK' | 'DAYS_30'>('ALL')
const savedPlanKeyword = ref('')
const savedPlanSort = ref<'DESC' | 'ASC'>('DESC')
const savedPlanPage = ref(1)
const savedPlanPageSize = ref(10)
const savedPlansTotal = ref(0)
const currentPlanId = ref<number | null>(null)
const requirements = ref('')
const requirementHistory = ref<string[]>([])
const importTitle = ref('')
const importing = ref(false)
const generating = ref(false)
const exportingPdf = ref(false)
const exportingPlanId = ref<number | null>(null)
const pendingTaskId = ref<number | null>(null)
const pollTimer = ref<number | null>(null)
const showActions = ref(true)
const shoppingCollapse = ref<string[]>([])
const feedbackLoading = ref<Record<string, boolean>>({})
const profilePromptVisible = ref(false)
const missingProfileLabels = ref<string[]>([])
const pendingGenerate = ref(false)
const feedbackPreview = ref<any>(null)
const feedbackPreviewLoading = ref(false)
const suppressSavedPlanWatch = ref(false)
const purchaseMode = ref<'FULL' | 'FOUR_THREE' | 'CUSTOM'>('FOUR_THREE')
const customStartIndex = ref<number | null>(null)
const customEndIndex = ref<number | null>(null)
const customBatches = ref<Array<{ id: number; startIndex: number; endIndex: number }>>([])
const customBatchSeq = ref(1)

const TASK_KEY = 'meal_plan_tasks'
const PROFILE_PROMPT_KEY = 'meal_plan_profile_prompted'
const SAVED_PLAN_FILTER_KEY = 'meal_plan_saved_filters'

const planRows = computed(() => generatedPlan.value?.weeklyPlan || [])
const dailyShopping = computed(() => {
    if (generatedPlan.value?.dailyShopping?.length) {
        return generatedPlan.value.dailyShopping
    }
    return buildDailyShoppingFromPlan(generatedPlan.value)
})

const pantryItems = computed(() => {
    if (generatedPlan.value?.pantryItems?.length) {
        return generatedPlan.value.pantryItems
    }
    return buildPantryFromPlan(generatedPlan.value)
})

const dayIndexOptions = computed(() => {
    return dailyShopping.value.map((item: any, index: number) => ({
        label: item?.day || `第${index + 1}天`,
        value: index
    }))
})

const purchaseBatches = computed(() => {
    const dayCount = dailyShopping.value.length
    if (!dayCount) return []
    if (purchaseMode.value === 'FULL') {
        return [buildPurchaseBatch(0, dayCount - 1, '全周期采购')]
    }
    if (purchaseMode.value === 'FOUR_THREE') {
        if (dayCount <= 4) {
            return [buildPurchaseBatch(0, dayCount - 1, '批次1')]
        }
        return [
            buildPurchaseBatch(0, Math.min(3, dayCount - 1), '批次1'),
            buildPurchaseBatch(4, dayCount - 1, '批次2')
        ].filter((item) => item.items.length > 0)
    }
    return customBatches.value
        .map((batch) => buildPurchaseBatch(batch.startIndex, batch.endIndex, `第${batch.id}批`))
        .filter((item) => item.items.length > 0)
})

const generatePlan = async () => {
    const allow = await ensureProfileReady()
    if (!allow) {
        pendingGenerate.value = true
        return
    }
    await generatePlanDirectly()
}

const generatePlanDirectly = async () => {
    loading.value = true
    try {
        currentPlanId.value = null
        const reqText = requirements.value.trim()
        if (reqText) {
            prependRequirementHistory(reqText)
        }
        const params: Record<string, string> = { range: rangeType.value }
        if (reqText) {
            params.requirements = reqText
        }
        const res: any = await request.post('/meal-plan/generate-async', null, { params })
        if (res.code === 200) {
            const taskId = res.data?.taskId
            if (taskId) {
                pendingTaskId.value = taskId
                generating.value = true
                addTask(taskId)
                startPolling(taskId)
                ElMessage.success('已开始生成，完成后会提醒')
            }
        } else {
            ElMessage.error(res.msg || '生成失败')
        }
    } catch (e) {
        console.error(e)
    } finally {
        loading.value = false
    }
}

const fetchSavedPlans = async () => {
    try {
        const keyword = savedPlanKeyword.value.trim()
        const res: any = await request.get('/meal-plan/page', {
            params: {
                status: savedPlanFilter.value,
                time: savedPlanTimeFilter.value,
                keyword: keyword || undefined,
                sort: savedPlanSort.value,
                page: savedPlanPage.value,
                pageSize: savedPlanPageSize.value
            }
        })
        if (res.code === 200) {
            savedPlans.value = res.data?.records || []
            savedPlansTotal.value = Number(res.data?.total || 0)
            // 删除后可能出现当前页越界，自动回退到可用页
            if (!savedPlans.value.length && savedPlansTotal.value > 0 && savedPlanPage.value > 1) {
                suppressSavedPlanWatch.value = true
                savedPlanPage.value = Math.max(1, savedPlanPage.value - 1)
                suppressSavedPlanWatch.value = false
                await fetchSavedPlans()
            }
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
            currentPlanId.value = id
            const matched = savedPlans.value.find((item) => item.id === id)
            if (matched?.rangeType) {
                rangeType.value = matched.rangeType
            }
            showActions.value = false
            shoppingCollapse.value = []
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
            currentPlanId.value = res.data.planId || null
            if (res.data.rangeType) {
                rangeType.value = res.data.rangeType
            }
            ElMessage.success('导入成功')
            showActions.value = false
            shoppingCollapse.value = []
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

const deletePlan = async (id: number) => {
    try {
        const res: any = await request.delete(`/meal-plan/${id}`)
        if (res.code === 200) {
            ElMessage.success('删除成功')
            if (currentPlanId.value === id) {
                generatedPlan.value = null
                currentPlanId.value = null
                showActions.value = true
                shoppingCollapse.value = []
            }
            await fetchSavedPlans()
        } else {
            ElMessage.error(res.msg || '删除失败')
        }
    } catch (e) {
        console.error(e)
        ElMessage.error('删除失败')
    }
}

const downloadPdf = async (planId?: number) => {
    if (exportingPdf.value) return
    const token = localStorage.getItem('token') || sessionStorage.getItem('token')
    const params = new URLSearchParams()
    if (planId) {
        params.set('planId', String(planId))
    } else {
        params.set('range', rangeType.value)
    }

    exportingPdf.value = true
    exportingPlanId.value = planId ?? null
    ElMessage.info('正在导出文档，请稍候...')

    try {
        const response = await fetch(`/api/export/pdf?${params.toString()}`, {
            method: 'GET',
            headers: {
                ...(token ? { Authorization: `Bearer ${token}` } : {})
            }
        })
        if (response.status === 401) {
            ElMessage.error('登录已失效，请重新登录后再导出')
            return
        }
        if (response.status === 403) {
            ElMessage.error('当前账号暂无导出权限，或会话状态异常，请稍后重试')
            return
        }
        if ([500, 502, 503, 504].includes(response.status)) {
            ElMessage.error('服务器暂时异常，导出失败，请稍后重试')
            return
        }
        const contentType = String(response.headers.get('content-type') || '').toLowerCase()
        if (!response.ok || !contentType.includes('application/pdf')) {
            let message = `导出失败(${response.status})`
            try {
                const text = await response.text()
                if (text) {
                    message = text.length > 120 ? `${text.slice(0, 120)}...` : text
                }
            } catch (_e) {
                // ignore
            }
            throw new Error(message)
        }
        const blob = await response.blob()
        if (blob.size <= 0) {
            throw new Error('导出结果为空')
        }
        const url = window.URL.createObjectURL(blob)
        const element = document.createElement('a')
        element.href = url
        element.download = planId ? `食谱_${planId}.pdf` : '食谱.pdf'
        document.body.appendChild(element)
        element.click()
        document.body.removeChild(element)
        window.URL.revokeObjectURL(url)
        ElMessage.success('导出完成')
    } catch (e: any) {
        console.error(e)
        ElMessage.error(e?.message || '导出失败，请稍后重试')
    } finally {
        exportingPdf.value = false
        exportingPlanId.value = null
    }
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
    if (!meal) return ''
    const numeric = Number(meal.caloriesKcal)
    if (Number.isFinite(numeric) && numeric > 0) {
        return `${Math.round(numeric)} 千卡`
    }
    if (!meal.calories) return ''
    return String(meal.calories).replace(/kcal/gi, '千卡')
}

const mealNutritionSummary = (meal: any) => {
    if (!meal) return ''
    const carbs = Number(meal.carbsGrams)
    const protein = Number(meal.proteinGrams)
    const fat = Number(meal.fatGrams)
    const hasAny = [carbs, protein, fat].some((v) => Number.isFinite(v) && v > 0)
    if (!hasAny) return '营养数据待补充'
    const parts: string[] = []
    if (Number.isFinite(carbs) && carbs > 0) {
        parts.push(`碳水 ${Math.round(carbs)}克`)
    }
    if (Number.isFinite(protein) && protein > 0) {
        parts.push(`蛋白 ${Math.round(protein)}克`)
    }
    if (Number.isFinite(fat) && fat > 0) {
        parts.push(`脂肪 ${Math.round(fat)}克`)
    }
    return parts.join(' | ')
}

const formatTargetValue = (value: any, unit: string) => {
    const num = Number(value)
    if (!Number.isFinite(num) || num <= 0) return ''
    return `${Math.round(num)} ${unit}`
}

const targetSummary = computed(() => {
    if (!generatedPlan.value) return []
    const items: { label: string; value: string }[] = []
    const cal = formatTargetValue(generatedPlan.value.targetCalories, '千卡/天')
    if (cal) items.push({ label: '目标能量', value: cal })
    const carbs = formatTargetValue(generatedPlan.value.targetCarbs, '克')
    if (carbs) items.push({ label: '碳水', value: carbs })
    const protein = formatTargetValue(generatedPlan.value.targetProtein, '克')
    if (protein) items.push({ label: '蛋白质', value: protein })
    const fat = formatTargetValue(generatedPlan.value.targetFat, '克')
    if (fat) items.push({ label: '脂肪', value: fat })
    return items
})

const adjustmentNotes = computed(() => {
    if (!generatedPlan.value) return []
    const notes = generatedPlan.value.adjustmentNotes
    return Array.isArray(notes) ? notes.filter((item: any) => !!item) : []
})

const feedbackPreviewNotes = computed(() => {
    const notes = feedbackPreview.value?.notes
    return Array.isArray(notes) ? notes.filter((item: any) => !!item) : []
})

const ensureProfileReady = async () => {
    if (sessionStorage.getItem(PROFILE_PROMPT_KEY)) {
        return true
    }
    try {
        const res: any = await request.get('/health/profile')
        const data = res?.data
        const missing = getMissingProfileFields(data)
        if (missing.length > 0) {
            missingProfileLabels.value = missing
            profilePromptVisible.value = true
            return false
        }
        return true
    } catch (e) {
        console.error(e)
        return true
    }
}

const getMissingProfileFields = (data: any) => {
    const missing: string[] = []
    if (!data) {
        return ['性别', '年龄', '身高', '体重', '日常活动量', '健康目标', '运动频率', '单次时长']
    }
    if (!data.gender) missing.push('性别')
    if (!data.age) missing.push('年龄')
    if (!data.height) missing.push('身高')
    if (!data.weight) missing.push('体重')
    if (!data.activityLevel) missing.push('日常活动量')
    if (!data.goal) missing.push('健康目标')
    if (data.exerciseFrequency === null || data.exerciseFrequency === undefined || data.exerciseFrequency === '') {
        missing.push('运动频率')
    }
    if (data.exerciseDuration === null || data.exerciseDuration === undefined || data.exerciseDuration === '') {
        missing.push('单次时长')
    }
    return missing
}

const goToHealthProfile = () => {
    profilePromptVisible.value = false
    pendingGenerate.value = false
    sessionStorage.setItem(PROFILE_PROMPT_KEY, '1')
    router.push('/health')
}

const continueGenerate = async () => {
    profilePromptVisible.value = false
    sessionStorage.setItem(PROFILE_PROMPT_KEY, '1')
    if (pendingGenerate.value) {
        pendingGenerate.value = false
        await generatePlanDirectly()
    }
}

const sendFeedback = async (mealType: string, direction: string) => {
    const key = `${mealType}_${direction}`
    if (feedbackLoading.value[key]) return
    feedbackLoading.value[key] = true
    try {
        const res: any = await request.post('/meal-plan/feedback', { mealType, direction })
        if (res.code === 200) {
            ElMessage.success('已记录反馈，将影响后续食谱')
            fetchFeedbackPreview()
        } else {
            ElMessage.error(res.msg || '反馈失败')
        }
    } catch (e) {
        console.error(e)
        ElMessage.error('反馈失败')
    } finally {
        feedbackLoading.value[key] = false
    }
}

const fetchFeedbackPreview = async () => {
    feedbackPreviewLoading.value = true
    try {
        const res: any = await request.get('/meal-plan/feedback/summary')
        if (res.code === 200) {
            feedbackPreview.value = res.data || null
        }
    } catch (e) {
        console.error(e)
    } finally {
        feedbackPreviewLoading.value = false
    }
}

const fetchRequirementHistory = async () => {
    try {
        const res: any = await request.get('/meal-plan/requirements/history', { params: { limit: 12 } })
        if (res.code === 200 && Array.isArray(res.data)) {
            requirementHistory.value = res.data
                .map((item: any) => String(item || '').trim())
                .filter((item: string) => item.length > 0)
                .slice(0, 12)
        }
    } catch (e) {
        console.error(e)
    }
}

const prependRequirementHistory = (text: string) => {
    const value = String(text || '').trim()
    if (!value) return
    const next = [value, ...requirementHistory.value.filter((item) => item !== value)]
    requirementHistory.value = next.slice(0, 12)
}

const applyRequirement = (text: string) => {
    requirements.value = text
}

const resetBatchPlanner = () => {
    purchaseMode.value = 'FOUR_THREE'
    customStartIndex.value = null
    customEndIndex.value = null
    customBatches.value = []
    customBatchSeq.value = 1
}

const addCustomBatch = () => {
    if (customStartIndex.value === null || customEndIndex.value === null) {
        ElMessage.warning('请先选择开始和结束日期')
        return
    }
    const start = Math.min(customStartIndex.value, customEndIndex.value)
    const end = Math.max(customStartIndex.value, customEndIndex.value)
    const dayCount = dailyShopping.value.length
    if (start < 0 || end >= dayCount) {
        ElMessage.warning('批次日期范围无效')
        return
    }
    const overlapped = customBatches.value.some((item) => !(end < item.startIndex || start > item.endIndex))
    if (overlapped) {
        ElMessage.warning('批次区间不能重叠，请调整后再添加')
        return
    }
    customBatches.value.push({
        id: customBatchSeq.value++,
        startIndex: start,
        endIndex: end
    })
}

const removeCustomBatch = (id: number) => {
    customBatches.value = customBatches.value.filter((item) => item.id !== id)
}

const clearCustomBatches = () => {
    customBatches.value = []
    customBatchSeq.value = 1
}

const formatBatchRange = (startIndex: number, endIndex: number) => {
    const startLabel = dayIndexOptions.value[startIndex]?.label || '未知'
    const endLabel = dayIndexOptions.value[endIndex]?.label || '未知'
    if (startIndex === endIndex) return startLabel
    return `${startLabel} ~ ${endLabel}`
}

const buildPurchaseBatch = (startIndex: number, endIndex: number, title: string) => {
    const days = dailyShopping.value.slice(startIndex, endIndex + 1)
    const merged = mergeShoppingItems(days)
    return {
        key: `${title}-${startIndex}-${endIndex}`,
        title,
        subtitle: `${formatBatchRange(startIndex, endIndex)}（共${days.length}天）`,
        items: merged
    }
}

const mergeShoppingItems = (days: any[]) => {
    const map = new Map<string, { displayName: string; grams: number; unknown: boolean }>()
    days.forEach((day) => {
        const items = Array.isArray(day?.items) ? day.items : []
        items.forEach((rawItem: string) => {
            const parsed = parseShoppingItem(rawItem)
            if (!parsed.name) return
            const key = parsed.name.toLowerCase()
            const current = map.get(key) || { displayName: parsed.name, grams: 0, unknown: false }
            if (parsed.grams > 0) {
                current.grams += parsed.grams
            } else {
                current.unknown = true
            }
            map.set(key, current)
        })
    })
    const result: string[] = []
    map.forEach((item) => {
        if (item.grams > 0) {
            let text = `${item.displayName} ${formatGrams(item.grams)}`
            if (item.unknown) {
                text += '（部分数量未标注）'
            }
            result.push(text)
        } else {
            result.push(item.displayName)
        }
    })
    return result
}

const formatShoppingText = (raw: string) => {
    const parsed = parseShoppingItem(raw)
    if (parsed.name && parsed.grams > 0) {
        return `${parsed.name} ${formatGrams(parsed.grams)}`
    }
    const text = String(raw || '').trim()
    if (!text) return '--'
    return text
        .replace(/\bkg\b/gi, '千克')
        .replace(/([0-9.]+)\s*g\b/gi, '$1克')
}

const parseShoppingItem = (raw: string) => {
    const text = String(raw || '').trim()
    if (!text) {
        return { name: '', grams: 0 }
    }
    const matched = text.match(/^(.*?)(?:\s+([0-9]+(?:\.[0-9]+)?)\s*(kg|g|克|千克))?$/i)
    if (!matched) {
        return { name: text, grams: 0 }
    }
    const name = String(matched[1] || '').trim()
    const amount = matched[2] ? Number(matched[2]) : NaN
    const unit = String(matched[3] || '').toLowerCase()
    if (!Number.isFinite(amount) || amount <= 0) {
        return { name, grams: 0 }
    }
    if (unit === 'kg' || unit === '千克') {
        return { name, grams: amount * 1000 }
    }
    return { name, grams: amount }
}

const formatGrams = (grams: number) => {
    if (!Number.isFinite(grams) || grams <= 0) return ''
    if (grams >= 1000) {
        const kg = Math.round((grams / 1000) * 10) / 10
        return Number.isInteger(kg) ? `${kg}千克` : `${kg.toFixed(1)}千克`
    }
    const value = Math.round(grams * 10) / 10
    return Number.isInteger(value) ? `${value}克` : `${value.toFixed(1)}克`
}

const buildDailyShoppingFromPlan = (plan: any) => {
    const result: { day: string; items: string[] }[] = []
    if (!plan?.weeklyPlan) return result
    const pantrySet = new Set(buildPantryFromPlan(plan))
    plan.weeklyPlan.forEach((day: any) => {
        const itemsSet = new Set<string>()
        const dayLabel = day?.day || '当天'
        ;['breakfast', 'lunch', 'dinner', 'snack'].forEach((key) => {
            const meal = day?.[key]
            const foods = normalizeFoods(meal)
            foods.forEach((food) => {
                if (!pantrySet.has(food)) {
                    itemsSet.add(food)
                }
            })
        })
        result.push({ day: dayLabel, items: Array.from(itemsSet) })
    })
    return result
}

const buildPantryFromPlan = (plan: any) => {
    const pantry = new Set<string>()
    if (!plan?.weeklyPlan) return []
    plan.weeklyPlan.forEach((day: any) => {
        ;['breakfast', 'lunch', 'dinner', 'snack'].forEach((key) => {
            const meal = day?.[key]
            const foods = normalizeFoods(meal)
            foods.forEach((food) => {
                if (isPantryItem(food)) {
                    pantry.add(food)
                }
            })
        })
    })
    return Array.from(pantry)
}

const normalizeFoods = (meal: any) => {
    if (!meal) return []
    let foods: string[] = Array.isArray(meal.foods) ? meal.foods : []
    if (!foods.length && meal.name) {
        foods = String(meal.name).split(/[、,，;+|/]/).map((v: string) => v.trim()).filter(Boolean)
    }
    return foods.map((item) => item.replace(/\s+/g, '').replace(/（.*?）/g, '').replace(/\(.*?\)/g, ''))
}

const isPantryItem = (name: string) => {
    const pantryKeywords = ['盐', '酱油', '生抽', '老抽', '醋', '糖', '味精', '鸡精', '胡椒', '花椒', '辣椒', '辣椒粉', '孜然', '香油', '芝麻油', '食用油', '油', '料酒', '蚝油', '豆瓣酱', '黄豆酱', '葱', '姜', '蒜', '八角', '香叶', '桂皮']
    return pantryKeywords.some((keyword) => name.includes(keyword))
}

const formatDate = (val: string) => {
    if (!val) return '--'
    return dayjs(val).format('YYYY-MM-DD HH:mm')
}

const restoreSavedPlanFilters = () => {
    try {
        const raw = localStorage.getItem(SAVED_PLAN_FILTER_KEY)
        if (!raw) return
        const parsed = JSON.parse(raw) as {
            status?: string
            time?: string
            keyword?: string
            sort?: string
            pageSize?: number
            page?: number
        }
        if (parsed.status && ['ALL', 'ADJUSTED', 'PLAIN'].includes(parsed.status)) {
            savedPlanFilter.value = parsed.status as 'ALL' | 'ADJUSTED' | 'PLAIN'
        }
        if (parsed.time && ['ALL', 'WEEK', 'DAYS_30'].includes(parsed.time)) {
            savedPlanTimeFilter.value = parsed.time as 'ALL' | 'WEEK' | 'DAYS_30'
        }
        if (typeof parsed.keyword === 'string') {
            savedPlanKeyword.value = parsed.keyword
        }
        if (parsed.sort && ['DESC', 'ASC'].includes(parsed.sort)) {
            savedPlanSort.value = parsed.sort as 'DESC' | 'ASC'
        }
        if (typeof parsed.pageSize === 'number' && [10, 20, 50].includes(parsed.pageSize)) {
            savedPlanPageSize.value = parsed.pageSize
        }
        if (typeof parsed.page === 'number' && parsed.page > 0) {
            savedPlanPage.value = parsed.page
        }
    } catch (e) {
        console.error(e)
    }
}

const persistSavedPlanFilters = () => {
    const payload = {
        status: savedPlanFilter.value,
        time: savedPlanTimeFilter.value,
        keyword: savedPlanKeyword.value,
        sort: savedPlanSort.value,
        pageSize: savedPlanPageSize.value,
        page: savedPlanPage.value
    }
    localStorage.setItem(SAVED_PLAN_FILTER_KEY, JSON.stringify(payload))
}

const restoreSavedPlanFiltersFromRoute = () => {
    const status = route.query.spStatus
    const time = route.query.spTime
    const keyword = route.query.spKeyword
    const sort = route.query.spSort
    const pageSize = route.query.spPageSize
    const page = route.query.spPage
    if (typeof status === 'string' && ['ALL', 'ADJUSTED', 'PLAIN'].includes(status)) {
        savedPlanFilter.value = status as 'ALL' | 'ADJUSTED' | 'PLAIN'
    }
    if (typeof time === 'string' && ['ALL', 'WEEK', 'DAYS_30'].includes(time)) {
        savedPlanTimeFilter.value = time as 'ALL' | 'WEEK' | 'DAYS_30'
    }
    if (typeof keyword === 'string') {
        savedPlanKeyword.value = keyword
    }
    if (typeof sort === 'string' && ['DESC', 'ASC'].includes(sort)) {
        savedPlanSort.value = sort as 'DESC' | 'ASC'
    }
    const parsedPageSize = Number(pageSize)
    if (Number.isFinite(parsedPageSize) && [10, 20, 50].includes(parsedPageSize)) {
        savedPlanPageSize.value = parsedPageSize
    }
    const parsedPage = Number(page)
    if (Number.isFinite(parsedPage) && parsedPage > 0) {
        savedPlanPage.value = parsedPage
    }
}

const syncSavedPlanFiltersToRoute = () => {
    const nextQuery: Record<string, any> = { ...route.query }
    nextQuery.spStatus = savedPlanFilter.value
    nextQuery.spTime = savedPlanTimeFilter.value
    nextQuery.spSort = savedPlanSort.value
    nextQuery.spPageSize = String(savedPlanPageSize.value)
    nextQuery.spPage = String(savedPlanPage.value)
    if (savedPlanKeyword.value.trim()) {
        nextQuery.spKeyword = savedPlanKeyword.value
    } else {
        delete nextQuery.spKeyword
    }
    router.replace({ query: nextQuery })
}

const resetSavedPlanFilters = () => {
    suppressSavedPlanWatch.value = true
    savedPlanFilter.value = 'ALL'
    savedPlanTimeFilter.value = 'ALL'
    savedPlanKeyword.value = ''
    savedPlanSort.value = 'DESC'
    savedPlanPageSize.value = 10
    savedPlanPage.value = 1
    suppressSavedPlanWatch.value = false
    persistSavedPlanFilters()
    syncSavedPlanFiltersToRoute()
    fetchSavedPlans()
}

onMounted(() => {
    suppressSavedPlanWatch.value = true
    restoreSavedPlanFilters()
    restoreSavedPlanFiltersFromRoute()
    suppressSavedPlanWatch.value = false
    fetchSavedPlans()
    fetchFeedbackPreview()
    fetchRequirementHistory()
})

onUnmounted(() => {
    stopPolling()
})

const readTasks = () => {
    try {
        const raw = localStorage.getItem(TASK_KEY)
        return raw ? (JSON.parse(raw) as number[]) : []
    } catch {
        return []
    }
}

const writeTasks = (tasks: number[]) => {
    localStorage.setItem(TASK_KEY, JSON.stringify(tasks))
}

const addTask = (id: number) => {
    const tasks = readTasks()
    if (!tasks.includes(id)) {
        tasks.push(id)
        writeTasks(tasks)
    }
}

const removeTask = (id: number) => {
    const tasks = readTasks().filter((task) => task !== id)
    writeTasks(tasks)
}

const startPolling = (id: number) => {
    stopPolling()
    pollTimer.value = window.setInterval(async () => {
        try {
            const res: any = await request.get(`/meal-plan/task/${id}`)
            if (res.code !== 200 || !res.data) return
            const status = res.data.status
            if (status === 'SUCCESS') {
                generatedPlan.value = res.data.plan
                generating.value = false
                pendingTaskId.value = null
                removeTask(id)
                stopPolling()
                ElMessage.success('食谱已生成')
                showActions.value = false
                shoppingCollapse.value = []
                fetchRequirementHistory()
                fetchSavedPlans()
            } else if (status === 'FAILED') {
                generating.value = false
                pendingTaskId.value = null
                removeTask(id)
                stopPolling()
                ElMessage.error(res.data.errorMessage || '生成失败')
            }
        } catch (e) {
            console.error(e)
        }
    }, 3000)
}

const stopPolling = () => {
    if (pollTimer.value) {
        clearInterval(pollTimer.value)
        pollTimer.value = null
    }
}

const toggleActions = () => {
    showActions.value = !showActions.value
}

watch([savedPlanFilter, savedPlanTimeFilter, savedPlanKeyword, savedPlanSort, savedPlanPageSize], () => {
    if (suppressSavedPlanWatch.value) return
    if (savedPlanPage.value !== 1) {
        savedPlanPage.value = 1
        return
    }
    persistSavedPlanFilters()
    syncSavedPlanFiltersToRoute()
    fetchSavedPlans()
})

watch(savedPlanPage, () => {
    if (suppressSavedPlanWatch.value) return
    persistSavedPlanFilters()
    syncSavedPlanFiltersToRoute()
    fetchSavedPlans()
})

watch(dailyShopping, () => {
    resetBatchPlanner()
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

.action-toggle {
    display: flex;
    justify-content: flex-end;
    margin-bottom: 10px;
}

.requirements {
    max-width: 880px;
    margin: 0 auto 18px auto;
    text-align: left;

    .history-row {
        margin-top: 10px;
        display: flex;
        align-items: flex-start;
        gap: 8px;
    }

    .history-label {
        color: #6b7280;
        font-size: 13px;
        line-height: 28px;
        white-space: nowrap;
    }

    .history-tags {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
    }

    .history-tag {
        cursor: pointer;
        user-select: none;
    }

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

.preview-alert {
    max-width: 880px;
    margin: 0 auto 12px auto;
    text-align: left;
}

.preview-list {
    margin: 6px 0 0 18px;
    padding: 0;
    li { margin: 4px 0; }
}

.preview-empty {
    max-width: 880px;
    margin: 0 auto 12px auto;
    color: #6b7280;
    font-size: 13px;
    text-align: left;
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

    .target-row {
        margin-top: 10px;
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
    }

    .adjustment-alert {
        margin-top: 10px;
    }

    .adjustment-list {
        margin: 6px 0 0 18px;
        padding: 0;
        li { margin: 4px 0; }
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
        .meal-nutrients {
            font-size: 12px;
            color: #606266;
            margin-top: 2px;
            line-height: 1.4;
        }
        .feedback-row {
            margin-top: 6px;
            display: flex;
            gap: 6px;
            flex-wrap: wrap;
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

.shopping-collapse {
    margin-top: 30px;
}

.shopping-collapse .tags-container {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
}

.shopping-collapse .shop-tag {
    margin-right: 0;
}

.batch-planner {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.batch-controls {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.custom-batch-editor {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
}

.custom-batch-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.batch-result-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.batch-result-card {
    background: #f8fafc;
    border: 1px solid #e5e7eb;
    border-radius: 10px;
    padding: 10px 12px;
}

.batch-result-title {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 6px;
    margin-bottom: 8px;
    color: #1f2937;
    font-weight: 600;
}

.batch-result-subtitle {
    color: #6b7280;
    font-size: 12px;
    font-weight: 400;
}

.daily-shopping {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.daily-block {
    background: #ffffff;
    border-radius: 10px;
    padding: 12px;
    border: 1px dashed #cbd5f5;
}

.daily-title {
    font-weight: 600;
    margin-bottom: 10px;
    color: #1f2937;
}

.pantry {
    background: #fff7ed;
    border-color: #fed7aa;
}

.empty-note {
    color: #6b7280;
    font-size: 13px;
}

.saved-card {
    margin-top: 20px;
}

.saved-actions {
    display: flex;
    gap: 8px;
    align-items: center;
    flex-wrap: wrap;
    justify-content: flex-end;
}

.saved-pagination {
    margin-top: 12px;
    justify-content: flex-end;
}

.plan-adjustment {
    display: flex;
    align-items: center;
    gap: 8px;
}

.adjustment-text {
    color: #374151;
}

.no-adjustment {
    color: #9ca3af;
}

.prompt-body {
    color: #374151;
    line-height: 1.6;
}

.missing-title {
    margin-top: 10px;
    font-weight: 600;
}

.missing-list {
    margin: 6px 0 0 18px;
    padding: 0;
}

.prompt-note {
    margin-top: 10px;
    color: #6b7280;
    font-size: 13px;
}

@keyframes fadeIn {
    from { opacity: 0; transform: translateY(10px); }
    to { opacity: 1; transform: translateY(0); }
}
</style>
