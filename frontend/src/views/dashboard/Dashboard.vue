<template>
  <div class="dashboard">
    <div class="welcome-section">
      <h2>{{ welcomeText }} 🌞</h2>
      <p class="subtitle">{{ subtitleText }}</p>
      <el-alert
        v-if="showOnboardingTip"
        title="欢迎使用！先完善健康档案，再记录今天饮食和健康数据，仪表盘会自动生成更准确的分析。"
        type="info"
        :closable="false"
        show-icon
        class="risk-banner"
      />
      <el-alert
        v-if="glucoseRiskLevel !== 'LOW' || pendingTaskCount > 0"
        :title="`控糖风险：${glucoseRiskText}，待处理任务 ${pendingTaskCount} 条`"
        type="warning"
        :closable="false"
        show-icon
        class="risk-banner"
      />
    </div>

    <el-row :gutter="20" class="cards-row">
      <el-col v-if="isCardVisible('bmi')" :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="health-card green">
          <div class="card-title">BMI 指数</div>
          <div class="card-value">{{ bmiValue }}</div>
          <div class="card-status" :class="bmiStatusClass">{{ bmiStatusText }}</div>
          <el-icon class="card-icon"><ScaleToOriginal /></el-icon>
        </el-card>
      </el-col>
      <el-col v-if="isCardVisible('bp')" :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="health-card blue">
          <div class="card-title">最近血压 (mmHg)</div>
          <div class="card-value">{{ bloodPressureValue }}</div>
          <div class="card-status" :class="bpStatusClass">{{ bpStatusText }}</div>
          <el-icon class="card-icon"><Timer /></el-icon>
        </el-card>
      </el-col>
      <el-col v-if="isCardVisible('meal')" :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="health-card orange">
          <div class="card-title">今日餐次记录</div>
          <div class="card-value">{{ mealCount }} / {{ mealGoal }}</div>
          <div class="card-status" :class="mealStatusClass">{{ mealStatusText }}</div>
          <el-progress :percentage="mealProgress" :color="'#F59E0B'" :show-text="false" class="card-progress" />
        </el-card>
      </el-col>
      <el-col v-if="isCardVisible('record')" :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="health-card cyan">
          <div class="card-title">今日健康记录</div>
          <div class="card-value">{{ recordCount }} 条</div>
          <div class="card-status" :class="recordStatusClass">{{ recordStatusText }}</div>
          <el-progress :percentage="recordProgress" :color="'#06b6d4'" :show-text="false" class="card-progress" />
        </el-card>
      </el-col>
      <el-col v-if="isCardVisible('glucoseAvg')" :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="health-card green">
          <div class="card-title">本周平均血糖</div>
          <div class="card-value">{{ glucoseAvgValue }}</div>
          <div class="card-status" :class="glucoseAvgStatusClass">{{ glucoseAvgStatusText }}</div>
          <el-icon class="card-icon"><ScaleToOriginal /></el-icon>
        </el-card>
      </el-col>
      <el-col v-if="isCardVisible('pendingTasks')" :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="health-card blue">
          <div class="card-title">待处理控糖任务</div>
          <div class="card-value">{{ pendingTaskCount }} 条</div>
          <div class="card-status" :class="pendingTaskStatusClass">{{ pendingTaskStatusText }}</div>
          <el-icon class="card-icon"><Timer /></el-icon>
        </el-card>
      </el-col>
      <el-col v-if="isCardVisible('profileCompletion')" :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="health-card cyan">
          <div class="card-title">健康档案完整度</div>
          <div class="card-value">{{ profileCompletion }}%</div>
          <div class="card-status" :class="profileCompletionStatusClass">{{ profileCompletionStatusText }}</div>
          <el-progress :percentage="profileCompletion" :show-text="false" class="card-progress" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="charts-row">
      <el-col :xs="24" :lg="16">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header">
              <span>一周健康趋势</span>
              <el-tag size="small" :type="trendTagType">{{ trendTagText }}</el-tag>
            </div>
          </template>
          <div ref="lineChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header">
              <span>今日热量结构</span>
              <div class="card-header-extra">
                <el-tag size="small" type="info">{{ pieModeText }}</el-tag>
                <span class="mode-tip">点击环图切换</span>
              </div>
            </div>
          </template>
          <div ref="pieChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <div class="action-bar">
      <el-button type="primary" size="large" @click="goDiet" icon="Food">
        记录饮食
      </el-button>
      <el-button type="success" size="large" @click="goChat" icon="ChatDotRound">
        询问 AI 营养师
      </el-button>
      <el-button type="warning" size="large" @click="goPlan" icon="Calendar">
        查看今日食谱
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import * as echarts from 'echarts'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import request from '../../api/request'
import dayjs from 'dayjs'

const router = useRouter()
const userStore = useUserStore()

const lineChartRef = ref<HTMLElement | null>(null)
const pieChartRef = ref<HTMLElement | null>(null)
const lineChart = ref<echarts.ECharts | null>(null)
const pieChart = ref<echarts.ECharts | null>(null)
const defaultDashboardCards = ['bmi', 'bp', 'meal', 'record', 'glucoseAvg', 'pendingTasks', 'profileCompletion']

const bmiValue = ref('--')
const bmiStatusText = ref('暂无数据')
const bmiStatusClass = ref('')

const bloodPressureValue = ref('--')
const bpStatusText = ref('暂无数据')
const bpStatusClass = ref('')

const mealCount = ref(0)
const mealGoal = 3
const recordCount = ref(0)

const mealStatusText = ref('今天还没有饮食记录')
const mealStatusClass = ref('warning')
const recordStatusText = ref('今天还没有健康记录')
const recordStatusClass = ref('warning')

const glucoseRiskLevel = ref('LOW')
const pendingTaskCount = ref(0)
const pendingTaskStatusText = ref('暂无待办')
const pendingTaskStatusClass = ref('normal')

const glucoseAvgValue = ref('--')
const glucoseAvgStatusText = ref('暂无数据')
const glucoseAvgStatusClass = ref('warning')

const profileCompletion = ref(0)
const profileCompletionStatusText = ref('建议先完善档案')
const profileCompletionStatusClass = ref('warning')

const hasProfileData = ref(false)
const hasHealthRecordData = ref(false)
const hasDietData = ref(false)

const trendTagText = ref('暂无数据')
const trendTagType = ref<'success' | 'warning' | 'info' | 'danger'>('info')
const pieMode = ref<'meal' | 'foodType'>('meal')
const todayDietLogs = ref<any[]>([])

const enabledDashboardCards = computed(() => {
  const configured = userStore.settings?.dashboardCards
  if (!Array.isArray(configured) || configured.length === 0) {
    return defaultDashboardCards
  }
  const normalized = configured.filter((key) => defaultDashboardCards.includes(key))
  return normalized.length > 0 ? normalized : defaultDashboardCards
})

const isCardVisible = (cardKey: string) => enabledDashboardCards.value.includes(cardKey)

const glucoseRiskText = computed(() => {
  if (glucoseRiskLevel.value === 'HIGH') return '高'
  if (glucoseRiskLevel.value === 'MEDIUM') return '中'
  return '低'
})

const greetingPrefix = computed(() => {
  const hour = new Date().getHours()
  if (hour < 11) return '早安'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const userDisplayName = computed(() => {
  const name = userStore.user?.nickname || userStore.user?.username || '用户'
  return String(name).trim() || '用户'
})

const userTitle = computed(() => {
  const role = String(userStore.user?.role || '').toUpperCase()
  if (role === 'ADMIN') return ''
  const gender = userStore.user?.gender
  if (gender === 1 || gender === '1' || String(gender).toUpperCase() === 'MALE') return '先生'
  if (gender === 0 || gender === '0' || String(gender).toUpperCase() === 'FEMALE') return '女士'
  return '同学'
})

const welcomeText = computed(() => `${greetingPrefix.value}，${userDisplayName.value}${userTitle.value}`)
const mealProgress = computed(() => Math.min(100, Math.round((mealCount.value / mealGoal) * 100)))
const recordProgress = computed(() => Math.min(100, Math.round((recordCount.value / 3) * 100)))

const showOnboardingTip = computed(() => !hasProfileData.value && !hasHealthRecordData.value && !hasDietData.value)
const pieModeText = computed(() => (pieMode.value === 'meal' ? '按餐次' : '按食物类型'))
const subtitleText = computed(() => {
  if (showOnboardingTip.value) {
    return '先完成一次健康档案填写，仪表盘会基于你的真实数据更新。'
  }
  if (!hasDietData.value && !hasHealthRecordData.value) {
    return '你已完成基础信息，接下来记录今天的饮食和健康数据吧。'
  }
  return '已根据你的最新记录更新今日概览。'
})

const goDiet = () => router.push('/diet')
const goChat = () => router.push('/chat')
const goPlan = () => router.push('/plan')

onMounted(() => {
  initCharts()
  fetchSummary()
  window.addEventListener('resize', resizeCharts)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeCharts)
  lineChart.value?.dispose()
  pieChart.value?.dispose()
  lineChart.value = null
  pieChart.value = null
})

const resizeCharts = () => {
  lineChart.value?.resize()
  pieChart.value?.resize()
}

const initCharts = () => {
  if (lineChartRef.value && !lineChart.value) {
    lineChart.value = echarts.init(lineChartRef.value)
  }
  if (pieChartRef.value && !pieChart.value) {
    pieChart.value = echarts.init(pieChartRef.value)
    bindPieChartInteraction()
  }
  renderLineChart([], [])
  renderPieChart([])
}

const bindPieChartInteraction = () => {
  if (!pieChart.value) return
  pieChart.value.off('click')
  pieChart.value.on('click', (params: any) => {
    if (params?.seriesType !== 'pie') return
    pieMode.value = pieMode.value === 'meal' ? 'foodType' : 'meal'
    renderPieChart(todayDietLogs.value)
  })
}

const round2 = (v: number) => Math.round(v * 100) / 100

const renderLineChart = (records: any[], glucoseRecords: any[]) => {
  if (!lineChart.value) return

  const days = Array.from({ length: 7 }).map((_, idx) => dayjs().subtract(6 - idx, 'day'))
  const labels = days.map((d) => d.format('MM-DD'))

  const glucoseData: Array<number | null> = []
  const systolicData: Array<number | null> = []

  for (const d of days) {
    const key = d.format('YYYY-MM-DD')
    const dayRecords = records.filter((item: any) => item.recordedAt && dayjs(item.recordedAt).format('YYYY-MM-DD') === key)
    const dayGlucoseRecords = glucoseRecords.filter((item: any) => item.recordedAt && dayjs(item.recordedAt).format('YYYY-MM-DD') === key)
    const fastingGlucoseList = dayGlucoseRecords
      .filter((item: any) => String(item.measureType || '').toUpperCase() === 'FASTING')
      .map((item: any) => Number(item.glucoseValue))
      .filter((v: number) => Number.isFinite(v))
    const glucoseList = fastingGlucoseList.length
      ? fastingGlucoseList
      : dayGlucoseRecords.map((item: any) => Number(item.glucoseValue)).filter((v: number) => Number.isFinite(v))
    const systolicList = dayRecords.map((item: any) => Number(item.systolic)).filter((v: number) => Number.isFinite(v))
    glucoseData.push(glucoseList.length ? round2(glucoseList.reduce((a: number, b: number) => a + b, 0) / glucoseList.length) : null)
    systolicData.push(systolicList.length ? round2(systolicList.reduce((a: number, b: number) => a + b, 0) / systolicList.length) : null)
  }

  const hasTrendData = glucoseData.some((v) => v !== null) || systolicData.some((v) => v !== null)
  if (!hasTrendData) {
    trendTagText.value = '暂无数据'
    trendTagType.value = 'info'
  } else {
    const first = glucoseData.find((v) => v !== null) ?? systolicData.find((v) => v !== null)
    const last = [...glucoseData].reverse().find((v) => v !== null) ?? [...systolicData].reverse().find((v) => v !== null)
    if (first !== null && last !== null) {
      const delta = last - first
      if (Math.abs(delta) <= 0.3) {
        trendTagText.value = '平稳'
        trendTagType.value = 'success'
      } else if (delta > 0) {
        trendTagText.value = '上升'
        trendTagType.value = 'warning'
      } else {
        trendTagText.value = '下降'
        trendTagType.value = 'success'
      }
    }
  }

  lineChart.value.setOption(
    {
      tooltip: { trigger: 'axis' },
      legend: {
        data: ['空腹血糖', '收缩压'],
        left: 'center',
        bottom: 6
      },
      grid: { left: '3%', right: '4%', bottom: 52, containLabel: true },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: labels
      },
      yAxis: { type: 'value' },
      series: [
        {
          name: '空腹血糖',
          type: 'line',
          data: glucoseData,
          itemStyle: { color: '#059669' },
          smooth: true
        },
        {
          name: '收缩压',
          type: 'line',
          data: systolicData,
          itemStyle: { color: '#3B82F6' },
          smooth: true
        }
      ]
    },
    true
  )
}

const renderPieChart = (todayLogs: any[]) => {
  if (!pieChart.value) return

  const mealMap: Record<string, number> = {
    早餐: 0,
    午餐: 0,
    晚餐: 0,
    加餐: 0
  }

  for (const item of todayLogs) {
    const mealType = String(item.mealType || '').toUpperCase()
    let key = '加餐'
    if (mealType === 'BREAKFAST') key = '早餐'
    if (mealType === 'LUNCH') key = '午餐'
    if (mealType === 'DINNER') key = '晚餐'
    const calories = Number(item.calories)
    mealMap[key] += Number.isFinite(calories) && calories > 0 ? calories : 0
  }

  const foodTypeMap: Record<string, number> = {
    碳水类: 0,
    蛋白质类: 0,
    脂肪类: 0,
    蔬果类: 0,
    其他类: 0
  }
  for (const item of todayLogs) {
    const calories = Number(item.calories)
    if (!Number.isFinite(calories) || calories <= 0) continue
    const category = resolveFoodCategory(item)
    foodTypeMap[category] += calories
  }

  const mealData = [
    { value: round2(mealMap['早餐']), name: '早餐', itemStyle: { color: '#F59E0B' } },
    { value: round2(mealMap['午餐']), name: '午餐', itemStyle: { color: '#EF4444' } },
    { value: round2(mealMap['晚餐']), name: '晚餐', itemStyle: { color: '#10B981' } },
    { value: round2(mealMap['加餐']), name: '加餐', itemStyle: { color: '#3B82F6' } }
  ].filter((item) => item.value > 0)
  const foodTypeData = [
    { value: round2(foodTypeMap['碳水类']), name: '碳水类', itemStyle: { color: '#F59E0B' } },
    { value: round2(foodTypeMap['蛋白质类']), name: '蛋白质类', itemStyle: { color: '#3B82F6' } },
    { value: round2(foodTypeMap['脂肪类']), name: '脂肪类', itemStyle: { color: '#EF4444' } },
    { value: round2(foodTypeMap['蔬果类']), name: '蔬果类', itemStyle: { color: '#10B981' } },
    { value: round2(foodTypeMap['其他类']), name: '其他类', itemStyle: { color: '#8B5CF6' } }
  ].filter((item) => item.value > 0)

  const selectedData = pieMode.value === 'meal' ? mealData : foodTypeData
  const hasCalories = selectedData.length > 0
  const data = hasCalories ? selectedData : [{ value: 1, name: '暂无数据', itemStyle: { color: '#D1D5DB' } }]
  const seriesName = pieMode.value === 'meal' ? '餐次热量' : '食物类型热量'

  pieChart.value.setOption(
    {
      tooltip: { trigger: 'item' },
      legend: { bottom: '5%', left: 'center' },
      series: [
        {
          name: seriesName,
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: { show: false, position: 'center' },
          emphasis: {
            label: { show: true, fontSize: 20, fontWeight: 'bold' }
          },
          labelLine: { show: false },
          data
        }
      ]
    },
    true
  )
}

const resolveFoodCategory = (item: any): string => {
  const name = String(item.foodName || '').toLowerCase()
  const carbKeywords = ['米', '饭', '面', '粥', '馒头', '包子', '面包', '粉', '饺', '馄饨', '年糕', '燕麦', '玉米', '红薯', '土豆']
  const proteinKeywords = ['鸡', '鸭', '鱼', '虾', '牛肉', '猪肉', '羊肉', '鸡蛋', '蛋', '豆腐', '豆浆', '牛奶', '酸奶']
  const fatKeywords = ['坚果', '核桃', '腰果', '花生', '芝麻', '牛油果', '黄油', '奶油', '肥肉', '培根', '食用油', '橄榄油']
  const vegetableFruitKeywords = ['西兰花', '菠菜', '生菜', '黄瓜', '番茄', '青椒', '洋葱', '胡萝卜', '苹果', '香蕉', '橙', '水果', '蔬菜']

  if (carbKeywords.some((keyword) => name.includes(keyword))) return '碳水类'
  if (proteinKeywords.some((keyword) => name.includes(keyword))) return '蛋白质类'
  if (fatKeywords.some((keyword) => name.includes(keyword))) return '脂肪类'
  if (vegetableFruitKeywords.some((keyword) => name.includes(keyword))) return '蔬果类'

  const carbs = Number(item.carbsGrams)
  const calories = Number(item.calories)
  if (Number.isFinite(carbs) && carbs >= 20) return '碳水类'
  if (Number.isFinite(carbs) && Number.isFinite(calories) && calories > 0 && (carbs * 4) / calories >= 0.5) return '碳水类'
  return '其他类'
}

const fetchSummary = async () => {
  if (!userStore.user?.id) return

  try {
    const userId = userStore.user.id
    const todayStart = dayjs().startOf('day').format('YYYY-MM-DD HH:mm:ss')
    const todayEnd = dayjs().endOf('day').format('YYYY-MM-DD HH:mm:ss')
    const weekStart = dayjs().subtract(6, 'day').startOf('day').format('YYYY-MM-DD HH:mm:ss')
    const weekEnd = dayjs().endOf('day').format('YYYY-MM-DD HH:mm:ss')

    const [settingsRet, profileRet, latestRet, recordsRet, dietRet, glucoseAnalysisRet, glucoseTaskRet, glucoseSummaryRet, glucoseListRet] = await Promise.allSettled([
      request.get('/user/settings'),
      request.get('/health/profile'),
      request.get('/health/records/latest', { params: { userId } }),
      request.get('/health/records', { params: { userId, start: weekStart, end: weekEnd } }),
      request.get('/diet/logs', { params: { start: todayStart, end: todayEnd } }),
      request.get('/health/glucose-records/analysis', { params: { userId } }),
      request.get('/health/glucose-tasks', { params: { userId, status: 'PENDING' } }),
      request.get('/health/glucose-records/summary', { params: { userId, start: weekStart, end: weekEnd } }),
      request.get('/health/glucose-records', { params: { userId, start: weekStart, end: weekEnd } })
    ])

    const settingsRes = settingsRet.status === 'fulfilled' ? settingsRet.value : null
    const profileRes = profileRet.status === 'fulfilled' ? profileRet.value : null
    const latestRes = latestRet.status === 'fulfilled' ? latestRet.value : null
    const recordsRes = recordsRet.status === 'fulfilled' ? recordsRet.value : null
    const dietRes = dietRet.status === 'fulfilled' ? dietRet.value : null
    const glucoseAnalysisRes = glucoseAnalysisRet.status === 'fulfilled' ? glucoseAnalysisRet.value : null
    const glucoseTaskRes = glucoseTaskRet.status === 'fulfilled' ? glucoseTaskRet.value : null
    const glucoseSummaryRes = glucoseSummaryRet.status === 'fulfilled' ? glucoseSummaryRet.value : null
    const glucoseListRes = glucoseListRet.status === 'fulfilled' ? glucoseListRet.value : null

    if (settingsRes?.data) {
      userStore.updateSettings({
        dashboardCards: normalizeDashboardCards(settingsRes.data.dashboardCards)
      })
    }

    const profile = profileRes?.data || null
    const latest = latestRes?.data || null
    const weekRecords = Array.isArray(recordsRes?.data) ? recordsRes?.data : []
    const weekGlucoseRecords = Array.isArray(glucoseListRes?.data) ? glucoseListRes?.data : []
    const todayLogs = Array.isArray(dietRes?.data) ? dietRes?.data : []
    todayDietLogs.value = todayLogs

    hasProfileData.value = !!profile
    hasHealthRecordData.value = !!latest || weekRecords.length > 0 || weekGlucoseRecords.length > 0
    hasDietData.value = todayLogs.length > 0

    const completion = calculateProfileCompletion(profile)
    profileCompletion.value = completion
    if (completion === 0) {
      profileCompletionStatusText.value = '建议先完善档案'
      profileCompletionStatusClass.value = 'warning'
    } else if (completion < 70) {
      profileCompletionStatusText.value = '可继续补充信息'
      profileCompletionStatusClass.value = 'warning'
    } else {
      profileCompletionStatusText.value = '档案较完整'
      profileCompletionStatusClass.value = 'normal'
    }

    const bmiHeight = Number(profile?.height ?? latest?.height)
    const bmiWeight = Number(profile?.weight ?? latest?.weight)
    if (bmiHeight > 0 && bmiWeight > 0) {
      const heightM = bmiHeight / 100
      const bmi = bmiWeight / (heightM * heightM)
      bmiValue.value = bmi.toFixed(1)
      if (bmi >= 24) {
        bmiStatusText.value = '偏高'
        bmiStatusClass.value = 'warning'
      } else if (bmi < 18.5) {
        bmiStatusText.value = '偏低'
        bmiStatusClass.value = 'warning'
      } else {
        bmiStatusText.value = '正常'
        bmiStatusClass.value = 'normal'
      }
    } else {
      bmiValue.value = '--'
      bmiStatusText.value = '未记录身高体重'
      bmiStatusClass.value = 'warning'
    }

    const systolic = Number(profile?.bpSystolic ?? latest?.systolic)
    const diastolic = Number(profile?.bpDiastolic ?? latest?.diastolic)
    if (systolic > 0 && diastolic > 0) {
      bloodPressureValue.value = `${Math.round(systolic)}/${Math.round(diastolic)}`
      if (systolic >= 140 || diastolic >= 90) {
        bpStatusText.value = '偏高'
        bpStatusClass.value = 'warning'
      } else {
        bpStatusText.value = '正常'
        bpStatusClass.value = 'normal'
      }
    } else {
      bloodPressureValue.value = '--'
      bpStatusText.value = '未记录血压'
      bpStatusClass.value = 'warning'
    }

    const todayKey = dayjs().format('YYYY-MM-DD')
    const todayRecords = weekRecords.filter((item: any) => item.recordedAt && dayjs(item.recordedAt).format('YYYY-MM-DD') === todayKey)
    recordCount.value = todayRecords.length
    if (recordCount.value === 0) {
      recordStatusText.value = '今天还没有健康记录'
      recordStatusClass.value = 'warning'
    } else if (recordCount.value < 3) {
      recordStatusText.value = '建议补充记录'
      recordStatusClass.value = 'warning'
    } else {
      recordStatusText.value = '记录充分'
      recordStatusClass.value = 'normal'
    }

    const mealTypesToday = new Set(
      todayLogs
        .map((item: any) => String(item.mealType || '').toUpperCase())
        .filter((v: string) => !!v)
    )
    mealCount.value = mealTypesToday.size || todayLogs.length
    if (mealCount.value === 0) {
      mealStatusText.value = '今天还没有饮食记录'
      mealStatusClass.value = 'warning'
    } else if (mealCount.value < mealGoal) {
      mealStatusText.value = '继续记录中'
      mealStatusClass.value = 'warning'
    } else {
      mealStatusText.value = '已完成今日记录'
      mealStatusClass.value = 'normal'
    }

    if (glucoseAnalysisRes?.data) {
      glucoseRiskLevel.value = glucoseAnalysisRes.data.riskLevel || 'LOW'
    } else {
      glucoseRiskLevel.value = 'LOW'
    }

    if (Array.isArray(glucoseTaskRes?.data)) {
      pendingTaskCount.value = glucoseTaskRes.data.length
      if (pendingTaskCount.value === 0) {
        pendingTaskStatusText.value = '暂无待办'
        pendingTaskStatusClass.value = 'normal'
      } else if (pendingTaskCount.value <= 2) {
        pendingTaskStatusText.value = '建议尽快处理'
        pendingTaskStatusClass.value = 'warning'
      } else {
        pendingTaskStatusText.value = '待办较多'
        pendingTaskStatusClass.value = 'warning'
      }
    } else {
      pendingTaskCount.value = 0
      pendingTaskStatusText.value = '暂无待办'
      pendingTaskStatusClass.value = 'normal'
    }

    const avgGlucose = Number(glucoseSummaryRes?.data?.avgGlucose)
    if (Number.isFinite(avgGlucose) && avgGlucose > 0) {
      glucoseAvgValue.value = `${round2(avgGlucose)} mmol/L`
      if (avgGlucose >= 7.0) {
        glucoseAvgStatusText.value = '偏高'
        glucoseAvgStatusClass.value = 'warning'
      } else if (avgGlucose < 3.9) {
        glucoseAvgStatusText.value = '偏低'
        glucoseAvgStatusClass.value = 'warning'
      } else {
        glucoseAvgStatusText.value = '正常'
        glucoseAvgStatusClass.value = 'normal'
      }
    } else {
      glucoseAvgValue.value = '--'
      glucoseAvgStatusText.value = '暂无血糖数据'
      glucoseAvgStatusClass.value = 'warning'
    }

    renderLineChart(weekRecords, weekGlucoseRecords)
    renderPieChart(todayDietLogs.value)
  } catch (e) {
    console.error(e)
  }
}

const calculateProfileCompletion = (profile: any) => {
  if (!profile || typeof profile !== 'object') {
    return 0
  }
  const fields = [
    'name',
    'gender',
    'age',
    'height',
    'weight',
    'bpSystolic',
    'bpDiastolic',
    'fastingGlucose',
    'hba1c',
    'goal',
    'activityLevel',
    'otherRestrictions'
  ]
  let filled = 0
  for (const key of fields) {
    const value = profile[key]
    if (value !== null && value !== undefined && String(value).trim() !== '') {
      filled++
    }
  }
  const diseases = Array.isArray(profile.diseases) ? profile.diseases : []
  const allergies = Array.isArray(profile.allergies) ? profile.allergies : []
  if (diseases.length > 0) filled++
  if (allergies.length > 0) filled++
  return Math.round((filled / (fields.length + 2)) * 100)
}

const normalizeDashboardCards = (raw: unknown): string[] => {
  if (Array.isArray(raw)) {
    const list = raw.filter((item) => typeof item === 'string') as string[]
    return list.length > 0 ? list : defaultDashboardCards
  }
  if (typeof raw === 'string') {
    try {
      const parsed = JSON.parse(raw)
      if (Array.isArray(parsed)) {
        const list = parsed.filter((item) => typeof item === 'string') as string[]
        return list.length > 0 ? list : defaultDashboardCards
      }
    } catch (e) {
      console.warn('parse dashboardCards failed', e)
    }
  }
  return defaultDashboardCards
}
</script>

<style scoped lang="scss">
.dashboard {
  .cards-row {
    margin-bottom: 28px;
    row-gap: 20px;

    :deep(.el-col) {
      display: flex;
    }
  }

  .welcome-section {
    margin-bottom: 24px;
    h2 {
      margin: 0;
      color: #111827;
      font-size: 24px;
    }
    .subtitle {
      color: #6b7280;
      margin: 8px 0 0;
    }
    .risk-banner {
      margin-top: 12px;
    }
  }

  .health-card {
    width: 100%;
    min-height: 176px;
    height: 100%;
    position: relative;
    border-radius: 12px;
    border: none;

    :deep(.el-card__body) {
      height: 100%;
      overflow: visible;
      display: flex;
      flex-direction: column;
      box-sizing: border-box;
      padding: 18px 20px;
    }

    .card-title {
      font-size: 14px;
      color: #6b7280;
    }
    .card-value {
      font-size: 28px;
      font-weight: bold;
      color: #111827;
      margin: 12px 0;
    }
    .card-status {
      display: inline-block;
      padding: 2px 8px;
      border-radius: 4px;
      font-size: 12px;
      &.normal {
        background-color: #ecfdf5;
        color: #059669;
      }
      &.warning {
        background-color: #fff7ed;
        color: #f97316;
      }
    }
    .card-icon {
      position: absolute;
      right: 20px;
      top: 20px;
      font-size: 40px;
      opacity: 0.1;
    }
    .card-progress {
      margin-top: auto;
      padding-top: 10px;
    }
  }

  .charts-row {
    margin-top: 0;
    .chart-card {
      border-radius: 12px;
      .card-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 8px;
      }
      .card-header-extra {
        display: flex;
        align-items: center;
        gap: 8px;
      }
      .mode-tip {
        font-size: 12px;
        color: #6b7280;
        white-space: nowrap;
      }
      .chart-container {
        height: 300px;
      }
    }
  }

  .action-bar {
    margin-top: 24px;
    text-align: center;
    padding: 20px;
    background: #fff;
    border-radius: 12px;

    .el-button {
      margin: 0 10px;
      padding: 12px 30px;
      font-weight: bold;
    }
  }
}
</style>
