<template>
  <div class="diet-log">
    <el-card class="record-card">
      <template #header>
        <div class="card-header">
          <span>饮食记录</span>
        </div>
      </template>

      <div class="plan-import">
        <div class="plan-header">
          <h3>从食谱导入</h3>
          <el-button size="small" @click="fetchMealPlans" :loading="planLoading">刷新食谱</el-button>
        </div>
        <el-form label-width="120px" size="large">
          <el-form-item label="选择食谱">
            <el-select v-model="selectedPlanId" placeholder="请选择已保存的食谱" style="width: 100%;" @change="handlePlanChange">
              <el-option v-for="plan in savedPlans" :key="plan.id" :label="plan.title" :value="plan.id" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="selectedPlan" label="选择日期">
            <el-select v-model="selectedDay" placeholder="请选择日期" style="width: 100%;">
              <el-option v-for="day in dayOptions" :key="day" :label="day" :value="day" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="selectedPlan" label="选择餐次">
            <el-select v-model="selectedMealKey" placeholder="请选择餐次" style="width: 100%;">
              <el-option v-for="item in mealOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="selectedMeal" label="餐次内容">
            <div class="meal-preview">
              <div class="meal-name">{{ selectedMeal.name || (selectedMeal.foods || []).join('、') }}</div>
              <div class="meal-cal">{{ selectedMeal.calories || '—' }}</div>
            </div>
          </el-form-item>
          <el-form-item v-if="selectedMeal">
            <el-button type="primary" @click="applyMealToLog">一键记录该餐</el-button>
          </el-form-item>
        </el-form>
      </div>
      
      <el-form label-width="120px" size="large">
        <el-form-item label="餐别">
             <el-radio-group v-model="form.mealType">
                <el-radio-button label="BREAKFAST">早餐 🥣</el-radio-button>
                <el-radio-button label="LUNCH">午餐 🍱</el-radio-button>
                <el-radio-button label="DINNER">晚餐 🍽️</el-radio-button>
                <el-radio-button label="SNACK">加餐 🍎</el-radio-button>
             </el-radio-group>
        </el-form-item>

        <el-form-item label="食物">
            <el-select
                v-model="form.foodName"
                placeholder="请搜索或输入食物"
                filterable
                allow-create
                default-first-option
                style="width: 100%; max-width: 400px;"
                @change="handleFoodChange"
            >
                <el-option-group label="常见主食">
                    <el-option label="米饭" value="米饭" />
                    <el-option label="馒头" value="馒头" />
                </el-option-group>
                <el-option-group label="肉蛋奶">
                    <el-option label="鸡胸肉" value="鸡胸肉" />
                    <el-option label="鸡蛋" value="鸡蛋" />
                    <el-option label="牛奶" value="牛奶" />
                </el-option-group>
                <el-option-group label="蔬果">
                    <el-option label="西兰花" value="西兰花" />
                    <el-option label="苹果" value="苹果" />
                </el-option-group>
            </el-select>
        </el-form-item>

        <el-form-item label="分量估算">
            <div class="fuzzy-selector">
                <div 
                    v-for="unit in units" 
                    :key="unit.value"
                    class="unit-card"
                    :class="{ active: form.unit === unit.value }"
                    @click="selectUnit(unit.value)"
                >
                    <div class="icon">{{ unit.icon }}</div>
                    <div class="label">{{ unit.label }}</div>
                    <div class="desc">{{ unit.desc }}</div>
                </div>
            </div>
        </el-form-item>

        <el-form-item label="数量">
             <el-input-number v-model="form.count" :min="0.5" :max="10" :step="0.5" />
        </el-form-item>

        <el-form-item label="重量(克)">
            <el-input-number v-model="form.weightGrams" :min="0" :max="5000" :step="10" />
            <span class="hint-text">如已填写重量，将优先使用重量计算</span>
        </el-form-item>

        <el-form-item label="热量(kcal)">
            <el-input-number v-model="form.calories" :min="0" :max="5000" :step="10" @change="handleCaloriesChange" />
            <span class="hint-text">可选，预包装食品可手动填写</span>
        </el-form-item>

        <el-form-item label="碳水(g)">
            <el-input-number v-model="form.carbsGrams" :min="0" :max="500" :step="1" @change="handleCarbsChange" />
            <span class="hint-text">糖尿病管理可填写</span>
        </el-form-item>

        <el-form-item label="糖(g)">
            <el-input-number v-model="form.sugarGrams" :min="0" :max="200" :step="1" @change="handleSugarChange" />
        </el-form-item>

        <el-form-item>
            <el-button type="primary" @click="submitLog" size="large" icon="Check">记录饮食</el-button>
        </el-form-item>
      </el-form>

      <div v-if="lastLog" class="log-preview">
        <el-alert title="记录成功" type="success" :description="lastLog" show-icon />
      </div>
    </el-card>

    <el-card class="history-card">
        <template #header>
            <div class="card-header">
                <span>历史记录</span>
                <div class="filter-actions">
                    <el-button size="small" @click="applyPreset('today')">今天</el-button>
                    <el-button size="small" @click="applyPreset('3d')">近3天</el-button>
                    <el-button size="small" @click="applyPreset('week')">本周</el-button>
                    <el-button size="small" @click="applyPreset('month')">本月</el-button>
                    <el-button size="small" @click="applyPreset('all')">全部</el-button>
                </div>
            </div>
        </template>
        <div class="filter-row">
            <el-date-picker
                v-model="dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                @change="fetchLogs"
            />
            <div class="summary">
                <span>记录数: <strong>{{ logs.length }}</strong></span>
                <span>总热量: <strong>{{ totalCalories }}</strong> kcal</span>
                <span>总碳水: <strong>{{ totalCarbs }}</strong> g</span>
                <span>总糖: <strong>{{ totalSugar }}</strong> g</span>
            </div>
        </div>

        <div v-for="group in groupedLogs" :key="group.key" class="meal-group">
            <div class="meal-group-title">
                <span>{{ group.title }}</span>
                <span class="meal-group-total">总热量: {{ group.totalCalories }} kcal</span>
            </div>
            <el-table :data="group.items" style="width: 100%">
                <el-table-column prop="recordedAt" label="时间" width="160" :formatter="formatDate" />
                <el-table-column label="食物">
                    <template #default="scope">
                        {{ scope.row.foodName || foodLabel(scope.row.foodId) || '自定义' }}
                    </template>
                </el-table-column>
                <el-table-column prop="weightGrams" label="重量(g)" width="120" />
                <el-table-column label="碳水(g)" width="120">
                    <template #default="scope">
                        <span>{{ scope.row.carbsGrams ?? '--' }}</span>
                        <span v-if="scope.row.carbsSource === 'plan'" class="source-tag">食谱</span>
                        <span v-else-if="scope.row.carbsSource === 'ai'" class="source-tag">AI</span>
                        <span v-else-if="scope.row.carbsSource === 'cache'" class="source-tag">缓存</span>
                    </template>
                </el-table-column>
                <el-table-column label="糖(g)" width="120">
                    <template #default="scope">
                        <span>{{ scope.row.sugarGrams ?? '--' }}</span>
                        <span v-if="scope.row.sugarSource === 'plan'" class="source-tag">食谱</span>
                        <span v-else-if="scope.row.sugarSource === 'ai'" class="source-tag">AI</span>
                        <span v-else-if="scope.row.sugarSource === 'cache'" class="source-tag">缓存</span>
                    </template>
                </el-table-column>
                <el-table-column prop="calories" label="热量(kcal)" width="120" />
                <el-table-column label="来源" width="140">
                    <template #default="scope">
                        {{ caloriesSourceLabel(scope.row.caloriesSource) }}
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="140">
                    <template #default="scope">
                        <el-button link type="primary" @click="startEdit(scope.row)">编辑</el-button>
                        <el-button link type="danger" @click="deleteLog(scope.row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </div>
    </el-card>

    <el-dialog v-model="editVisible" title="修改饮食记录" width="480px">
        <el-form label-width="100px">
            <el-form-item label="餐别">
                <el-select v-model="editForm.mealType">
                    <el-option label="早餐" value="BREAKFAST" />
                    <el-option label="午餐" value="LUNCH" />
                    <el-option label="晚餐" value="DINNER" />
                    <el-option label="加餐" value="SNACK" />
                </el-select>
            </el-form-item>
            <el-form-item label="食物">
                <el-input v-model="editForm.foodName" />
            </el-form-item>
            <el-form-item label="分量估算">
                <el-input v-model="editForm.unit" />
            </el-form-item>
            <el-form-item label="重量(g)">
                <el-input-number v-model="editForm.weightGrams" :min="0" :max="5000" :step="10" />
            </el-form-item>
            <el-form-item label="热量(kcal)">
                <el-input-number v-model="editForm.calories" :min="0" :max="5000" :step="10" />
            </el-form-item>
            <el-form-item label="碳水(g)">
                <el-input-number v-model="editForm.carbsGrams" :min="0" :max="500" :step="1" />
            </el-form-item>
            <el-form-item label="糖(g)">
                <el-input-number v-model="editForm.sugarGrams" :min="0" :max="200" :step="1" />
            </el-form-item>
        </el-form>
        <template #footer>
            <el-button @click="editVisible = false">取消</el-button>
            <el-button type="primary" @click="saveEdit">保存</el-button>
        </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../api/request'

const form = reactive({
    mealType: 'LUNCH',
    foodName: '',
    foodId: null,
    unit: '',
    count: 1,
    weightGrams: 0,
    calories: 0,
    carbsGrams: 0,
    sugarGrams: 0,
    carbsSource: '',
    sugarSource: ''
})

const lastLog = ref('')
const logs = ref<any[]>([])
const loading = ref(false)
const dateRange = ref<[string, string] | null>(null)
const editVisible = ref(false)
const planLoading = ref(false)
const savedPlans = ref<any[]>([])
const selectedPlanId = ref<number | null>(null)
const selectedPlan = ref<any>(null)
const selectedDay = ref('')
const selectedMealKey = ref('breakfast')
const editForm = reactive({
    id: null as number | null,
    mealType: '',
    foodName: '',
    unit: '',
    weightGrams: 0,
    calories: 0,
    carbsGrams: 0,
    sugarGrams: 0,
    carbsSource: '',
    sugarSource: ''
})

const weeklyPlan = computed(() => {
    const plan = selectedPlan.value?.weeklyPlan
    return Array.isArray(plan) ? plan : []
})

const dayOptions = computed(() => {
    if (!weeklyPlan.value.length) return []
    return weeklyPlan.value.map((day: any) => day?.day || '当天')
})

const mealOptions = computed(() => {
    return [
        { label: '早餐', value: 'breakfast' },
        { label: '午餐', value: 'lunch' },
        { label: '晚餐', value: 'dinner' },
        { label: '加餐', value: 'snack' }
    ]
})

const selectedMeal = computed(() => {
    if (!weeklyPlan.value.length || !selectedDay.value) return null
    const day = weeklyPlan.value.find((item: any) => (item?.day || '当天') === selectedDay.value)
    if (!day) return null
    return day[selectedMealKey.value] || null
})

const foodCatalog = [
    { id: 101, name: '米饭', kcalPer100g: 116, carbsPer100g: 25.9, sugarPer100g: 0.1 },
    { id: 104, name: '馒头', kcalPer100g: 230, carbsPer100g: 46.6, sugarPer100g: 1.2 },
    { id: 102, name: '鸡胸肉', kcalPer100g: 165, carbsPer100g: 0, sugarPer100g: 0 },
    { id: 105, name: '鸡蛋', kcalPer100g: 143, carbsPer100g: 0.7, sugarPer100g: 0.4 },
    { id: 106, name: '牛奶', kcalPer100g: 54, carbsPer100g: 5, sugarPer100g: 5 },
    { id: 103, name: '西兰花', kcalPer100g: 34, carbsPer100g: 6.6, sugarPer100g: 1.7 },
    { id: 107, name: '苹果', kcalPer100g: 52, carbsPer100g: 13.8, sugarPer100g: 10.4 }
]

const units = [
    { value: 'FIST', label: '一拳', icon: '👊', desc: '~150g (主食/水果)' },
    { value: 'PALM', label: '一掌', icon: '✋', desc: '~120g (肉类)' },
    { value: 'BOWL', label: '一碗', icon: '🥣', desc: '~250ml (汤/粥)' }
]

const totalCalories = computed(() => {
    const list = Array.isArray(logs.value) ? logs.value : []
    return list.reduce((sum, item) => sum + (Number(item.calories) || 0), 0)
})

const totalCarbs = computed(() => {
    const list = Array.isArray(logs.value) ? logs.value : []
    return list.reduce((sum, item) => sum + (Number(item.carbsGrams) || 0), 0)
})

const totalSugar = computed(() => {
    const list = Array.isArray(logs.value) ? logs.value : []
    return list.reduce((sum, item) => sum + (Number(item.sugarGrams) || 0), 0)
})

const groupedLogs = computed(() => {
    const groups: Record<string, { key: string; title: string; items: any[]; totalCalories: number }> = {}
    const list = Array.isArray(logs.value) ? logs.value : []
    list.forEach((item) => {
        const day = item.recordedAt ? dayjs(item.recordedAt).format('YYYY-MM-DD') : '未知日期'
        const mealLabel = mealTypeLabel(item.mealType)
        const key = `${day}_${item.mealType || 'UNKNOWN'}`
        if (!groups[key]) {
            groups[key] = {
                key,
                title: `${day} · ${mealLabel}`,
                items: [],
                totalCalories: 0
            }
        }
        groups[key].items.push(item)
        groups[key].totalCalories += Number(item.calories) || 0
    })
    return Object.values(groups).sort((a, b) => (a.key < b.key ? 1 : -1))
})

const handleFoodChange = () => {
    const matched = foodCatalog.find((f) => f.name === form.foodName)
    form.foodId = matched ? matched.id : null
}

const fetchMealPlans = async () => {
    planLoading.value = true
    try {
        const res: any = await request.get('/meal-plan')
        if (res.code === 200) {
            savedPlans.value = Array.isArray(res.data) ? res.data : []
        }
    } catch (e) {
        console.error(e)
    } finally {
        planLoading.value = false
    }
}

const handlePlanChange = async (id: number) => {
    selectedPlanId.value = id
    selectedPlan.value = null
    selectedDay.value = ''
    selectedMealKey.value = 'breakfast'
    if (!id) return
    try {
        const res: any = await request.get(`/meal-plan/${id}`)
        if (res.code === 200) {
            selectedPlan.value = res.data
            const firstDay = res.data?.weeklyPlan?.[0]?.day || '当天'
            selectedDay.value = firstDay
        } else {
            ElMessage.error(res.msg || '加载食谱失败')
        }
    } catch (e) {
        console.error(e)
    }
}

const applyMealToLog = () => {
    if (!selectedMeal.value) return
    const meal = selectedMeal.value
    const nutrient = extractMealNutrition(meal)
    form.foodName = meal.name || (meal.foods || []).join('、') || '食谱餐次'
    form.foodId = null
    form.unit = ''
    form.count = 1
    form.weightGrams = nutrient.weightGrams
    form.mealType = mapMealType(selectedMealKey.value)
    const calories = nutrient.calories
    if (!calories) {
        ElMessage.warning('该餐次未提供热量信息，请手动补充后再记录')
        return
    }
    form.calories = calories
    form.carbsGrams = nutrient.carbsGrams
    form.sugarGrams = nutrient.sugarGrams
    form.carbsSource = 'plan'
    form.sugarSource = 'plan'
    submitLog()
}

const mapMealType = (key: string) => {
    switch (key) {
        case 'breakfast':
            return 'BREAKFAST'
        case 'lunch':
            return 'LUNCH'
        case 'dinner':
            return 'DINNER'
        case 'snack':
            return 'SNACK'
        default:
            return 'LUNCH'
    }
}

const extractCalories = (value: string | undefined) => {
    if (!value) return 0
    const match = String(value).match(/([0-9]+(?:\\.[0-9]+)?)/)
    if (!match) return 0
    return Math.round(Number(match[1]))
}

const extractMealNutrition = (meal: any) => {
    const caloriesKcal = Number(meal?.caloriesKcal)
    const carbsGrams = Number(meal?.carbsGrams)
    const ingredients = Array.isArray(meal?.ingredients) ? meal.ingredients : []
    const ingredientCalories = ingredients.reduce((sum: number, item: any) => sum + (Number(item?.caloriesKcal) || 0), 0)
    const ingredientCarbs = ingredients.reduce((sum: number, item: any) => sum + (Number(item?.carbsGrams) || 0), 0)
    const ingredientWeight = ingredients.reduce((sum: number, item: any) => sum + (Number(item?.grams) || 0), 0)

    const calories = Number.isFinite(caloriesKcal) && caloriesKcal > 0
        ? Math.round(caloriesKcal)
        : ingredientCalories > 0
            ? Math.round(ingredientCalories)
            : extractCalories(meal?.calories)
    const carbs = Number.isFinite(carbsGrams) && carbsGrams > 0
        ? Math.round(carbsGrams)
        : ingredientCarbs > 0
            ? Math.round(ingredientCarbs)
            : 0
    return {
        calories,
        carbsGrams: carbs,
        sugarGrams: 0,
        weightGrams: ingredientWeight > 0 ? Math.round(ingredientWeight) : 0
    }
}

const foodLabel = (id: number) => {
    const item = foodCatalog.find((f) => f.id === id)
    return item?.name
}

const selectUnit = (value: string) => {
    if (form.unit === value) {
        form.unit = ''
        return
    }
    form.unit = value
    if (form.weightGrams) {
        form.weightGrams = 0
    }
}

watch(
    () => form.weightGrams,
    (val) => {
        if (val && val > 0) {
            form.unit = ''
        }
    }
)

const handleCaloriesChange = (val: number | undefined) => {
    if (val && val > 0) {
        form.unit = ''
    }
}

const handleCarbsChange = (val: number | undefined) => {
    if (val && val > 0) {
        form.carbsSource = 'manual'
    }
}

const handleSugarChange = (val: number | undefined) => {
    if (val && val > 0) {
        form.sugarSource = 'manual'
    }
}

const submitLog = async () => {
    if (!form.foodName.trim()) {
        ElMessage.warning('请选择或输入食物名称')
        return
    }
    if (!form.unit && !form.weightGrams && !form.calories) {
        ElMessage.warning('请选择分量估算、填写重量或填写热量')
        return
    }

    const resolvedWeight = resolveWeightGrams()
    let caloriesSource = ''
    let calories = form.calories
    let carbsGrams = form.carbsGrams
    let sugarGrams = form.sugarGrams
    let carbsSource = form.carbsSource
    let sugarSource = form.sugarSource
    const fromPlanImport = carbsSource === 'plan' || sugarSource === 'plan'
    if (!calories && resolvedWeight && form.foodId) {
        const matched = foodCatalog.find((f) => f.id === form.foodId)
        if (matched) {
            calories = Math.round((resolvedWeight * matched.kcalPer100g) / 100)
            caloriesSource = 'catalog'
        }
    }
    if (calories && !caloriesSource) {
        caloriesSource = form.carbsSource === 'plan' ? 'plan' : 'manual'
    }

    if (resolvedWeight && form.foodId) {
        const matched = foodCatalog.find((f) => f.id === form.foodId)
        if (matched) {
            if (!carbsGrams && matched.carbsPer100g !== undefined) {
                carbsGrams = Math.round((resolvedWeight * matched.carbsPer100g) / 100)
                carbsSource = 'catalog'
            }
            if (!sugarGrams && matched.sugarPer100g !== undefined) {
                sugarGrams = Math.round((resolvedWeight * matched.sugarPer100g) / 100)
                sugarSource = 'catalog'
            }
        }
    }

    if (!fromPlanImport && !calories && resolvedWeight) {
        const estimated = await estimateCaloriesByAi(form.foodName, resolvedWeight)
        if (estimated.calories) {
            calories = estimated.calories
            caloriesSource = estimated.source
        }
    }

    if (!fromPlanImport && (!carbsGrams || !sugarGrams) && resolvedWeight) {
        const macro = await estimateMacrosByAi(form.foodName, resolvedWeight)
        if (!carbsGrams && macro?.carbsGrams) {
            carbsGrams = Math.round(macro.carbsGrams)
            carbsSource = macro.source
        }
        if (!sugarGrams && macro?.sugarGrams) {
            sugarGrams = Math.round(macro.sugarGrams)
            sugarSource = macro.source
        }
    }

    try {
        const res: any = await request.post('/diet/logs', {
            mealType: form.mealType,
            foodId: form.foodId,
            unit: form.unit,
            count: form.count,
            foodName: form.foodName,
            weightGrams: resolvedWeight || null,
            calories: calories || null,
            caloriesSource: caloriesSource || null,
            carbsGrams: carbsGrams || null,
            sugarGrams: sugarGrams || null,
            carbsSource: carbsSource || null,
            sugarSource: sugarSource || null
        })
        if (res.code === 200) {
            const portion = resolvedWeight
                ? `${resolvedWeight}g`
                : `${form.count} x ${form.unit}`
            lastLog.value = `已记录 ${mealTypeLabel(form.mealType)}: ${form.foodName} ${portion}`
            ElMessage.success('记录成功，今日热量已更新')
            fetchLogs()
        } else {
            ElMessage.error(res.msg || '记录失败')
        }
    } catch (e) {
        console.error(e)
    }
}

const formatDate = (row: any) => {
    return dayjs(row.recordedAt).format('YYYY-MM-DD HH:mm')
}

const mealTypeLabel = (value: string) => {
    const map: Record<string, string> = {
        BREAKFAST: '早餐',
        LUNCH: '午餐',
        DINNER: '晚餐',
        SNACK: '加餐'
    }
    return map[value] || value || '未知'
}

const caloriesSourceLabel = (value: string) => {
    const map: Record<string, string> = {
        catalog: '本地计算',
        cache: '本地缓存',
        manual: '手动输入',
        plan: '食谱同步',
        ai: 'AI估算(参考)'
    }
    return map[value] || ''
}

const resolveWeightGrams = () => {
    if (form.weightGrams && form.weightGrams > 0) {
        return Math.round(form.weightGrams)
    }
    if (form.unit) {
        const unitMap: Record<string, number> = {
            FIST: 150,
            PALM: 120,
            BOWL: 250
        }
        const base = unitMap[form.unit] || 0
        if (base && form.count) {
            return Math.round(base * form.count)
        }
    }
    return 0
}

const estimateCaloriesByAi = async (foodName: string, weightGrams: number) => {
    try {
        const res: any = await request.post('/diet/logs/estimate-calories', {
            foodName,
            weightGrams
        })
        if (res.code === 200 && res.data?.calories) {
            const note = String(res.data?.note || '')
            return {
                calories: Math.round(res.data.calories),
                source: note.includes('本地缓存') ? 'cache' : 'ai'
            }
        }
    } catch (e) {
        console.error(e)
    }
    return { calories: 0, source: 'ai' as const }
}

const estimateMacrosByAi = async (foodName: string, weightGrams: number) => {
    try {
        const res: any = await request.post('/diet/logs/estimate-macros', {
            foodName,
            weightGrams
        })
        if (res.code === 200 && res.data) {
            const note = String(res.data?.note || '')
            return {
                carbsGrams: res.data?.carbsGrams,
                sugarGrams: res.data?.sugarGrams,
                source: note.includes('本地缓存') ? 'cache' : 'ai'
            }
        }
    } catch (e) {
        console.error(e)
    }
    return null
}

const applyPreset = (preset: 'today' | '3d' | 'week' | 'month' | 'all') => {
    if (preset === 'all') {
        dateRange.value = null
        fetchLogs()
        return
    }
    const end = dayjs().endOf('day')
    let start = dayjs().startOf('day')
    if (preset === '3d') {
        start = dayjs().subtract(2, 'day').startOf('day')
    } else if (preset === 'week') {
        start = dayjs().startOf('week')
    } else if (preset === 'month') {
        start = dayjs().startOf('month')
    }
    dateRange.value = [start.format('YYYY-MM-DD'), end.format('YYYY-MM-DD')]
    fetchLogs()
}

const fetchLogs = async () => {
    loading.value = true
    try {
        const params: any = {}
        if (dateRange.value) {
            params.start = `${dateRange.value[0]} 00:00:00`
            params.end = `${dateRange.value[1]} 23:59:59`
        }
        const res: any = await request.get('/diet/logs', { params })
        if (res.code === 200) {
            logs.value = Array.isArray(res.data) ? res.data : []
        }
    } catch (e) {
        console.error(e)
    } finally {
        loading.value = false
    }
}

const startEdit = (row: any) => {
    editForm.id = row.id
    editForm.mealType = row.mealType
    editForm.foodName = row.foodName
    editForm.unit = row.unit
    editForm.weightGrams = row.weightGrams || 0
    editForm.calories = row.calories || 0
    editForm.carbsGrams = row.carbsGrams || 0
    editForm.sugarGrams = row.sugarGrams || 0
    editForm.carbsSource = row.carbsSource || ''
    editForm.sugarSource = row.sugarSource || ''
    editVisible.value = true
}

const saveEdit = async () => {
    if (!editForm.id) return
    try {
        const res: any = await request.put(`/diet/logs/${editForm.id}`, {
            mealType: editForm.mealType,
            foodName: editForm.foodName,
            unit: editForm.unit,
            weightGrams: editForm.weightGrams || null,
            calories: editForm.calories || null,
            caloriesSource: editForm.calories ? 'manual' : null,
            carbsGrams: editForm.carbsGrams || null,
            sugarGrams: editForm.sugarGrams || null,
            carbsSource: editForm.carbsSource || null,
            sugarSource: editForm.sugarSource || null
        })
        if (res.code === 200) {
            ElMessage.success('更新成功')
            editVisible.value = false
            fetchLogs()
        } else {
            ElMessage.error(res.msg || '更新失败')
        }
    } catch (e) {
        console.error(e)
    }
}

const deleteLog = async (row: any) => {
    try {
        await ElMessageBox.confirm('确定删除该记录吗？', '提示', {
            confirmButtonText: '删除',
            cancelButtonText: '取消',
            type: 'warning'
        })
    } catch {
        return
    }
    try {
        const res: any = await request.delete(`/diet/logs/${row.id}`)
        if (res.code === 200) {
            ElMessage.success('删除成功')
            fetchLogs()
        } else {
            ElMessage.error(res.msg || '删除失败')
        }
    } catch (e) {
        console.error(e)
    }
}

onMounted(() => {
    applyPreset('week')
    fetchMealPlans()
})
</script>

<style scoped lang="scss">
.diet-log {
    display: flex;
    flex-direction: column;
    gap: 20px;
}
.record-card,
.history-card {
    width: 100%;
}
.plan-import {
    margin-bottom: 20px;
    padding: 16px;
    border: 1px dashed #e5e7eb;
    border-radius: 10px;
    background: #f9fafb;
    .plan-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 12px;
        h3 { margin: 0; color: #1f2937; }
    }
}
.meal-preview {
    display: flex;
    flex-direction: column;
    gap: 4px;
    .meal-name { font-weight: 600; }
    .meal-cal { color: #6b7280; font-size: 12px; }
}
.fuzzy-selector {
    display: flex;
    gap: 20px;
    flex-wrap: wrap;
}
.unit-card {
    border: 2px solid #e5e7eb;
    border-radius: 12px;
    padding: 15px;
    cursor: pointer;
    text-align: center;
    width: 120px;
    transition: all 0.3s ease;
    background: #fff;
    
    &:hover {
        border-color: #a7f3d0;
        transform: translateY(-2px);
    }
    
    &.active {
        border-color: #059669; // Primary green
        background-color: #ecfdf5;
        box-shadow: 0 4px 6px -1px rgba(5, 150, 105, 0.2);
    }
    
    .icon {
        font-size: 32px;
        margin-bottom: 8px;
    }
    .label {
        font-weight: bold;
        color: #374151;
        margin-bottom: 4px;
    }
    .desc {
        font-size: 12px;
        color: #9ca3af;
    }
}
.log-preview {
    margin-top: 24px;
}

.source-tag {
    display: inline-block;
    margin-left: 6px;
    padding: 2px 6px;
    font-size: 12px;
    border-radius: 10px;
    background: #fef3c7;
    color: #92400e;
}
.filter-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 16px;
    flex-wrap: wrap;
}
.filter-actions {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
}
.summary {
    display: flex;
    gap: 16px;
    color: #4b5563;
}
.hint-text {
    margin-left: 12px;
    color: #9ca3af;
    font-size: 12px;
}
.meal-group {
    margin-bottom: 16px;
}
.meal-group-title {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 4px;
    font-weight: 600;
    color: #374151;
}
.meal-group-total {
    font-size: 12px;
    color: #6b7280;
}
</style>
