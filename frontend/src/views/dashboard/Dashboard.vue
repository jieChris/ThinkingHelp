<template>
  <div class="dashboard">
    <!-- Header Welcome -->
    <div class="welcome-section">
      <h2>早安，x先生 🌞</h2>
      <p class="subtitle">今天也是健康的一天，您的上次体检报告已更新。</p>
    </div>

    <!-- Health Cards -->
    <el-row :gutter="20" class="cards-row">
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="health-card green">
          <div class="card-title">BMI 指数</div>
          <div class="card-value">{{ bmiValue }}</div>
          <div class="card-status" :class="bmiStatusClass">{{ bmiStatusText }}</div>
          <el-icon class="card-icon"><ScaleToOriginal /></el-icon>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="health-card blue">
          <div class="card-title">最近血压 (mmHg)</div>
          <div class="card-value">{{ bloodPressureValue }}</div>
          <div class="card-status" :class="bpStatusClass">{{ bpStatusText }}</div>
          <el-icon class="card-icon"><Timer /></el-icon>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="health-card orange">
          <div class="card-title">今日餐次记录</div>
          <div class="card-value">{{ mealCount }} / {{ mealGoal }}</div>
          <el-progress :percentage="mealProgress" :color="'#F59E0B'" :show-text="false" class="card-progress"/>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="health-card cyan">
          <div class="card-title">今日健康记录</div>
          <div class="card-value">{{ recordCount }} 条</div>
          <el-progress :percentage="recordProgress" :color="'#06b6d4'" :show-text="false" class="card-progress"/>
        </el-card>
      </el-col>
    </el-row>

    <!-- Charts Area -->
    <el-row :gutter="20" class="charts-row">
      <el-col :xs="24" :lg="16">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header">
              <span>一周健康趋势</span>
              <el-tag size="small" type="success">平稳</el-tag>
            </div>
          </template>
          <div ref="lineChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header">
              <span>今日膳食结构</span>
            </div>
          </template>
          <div ref="pieChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Action Bar -->
    <div class="action-bar">
      <el-button type="primary" size="large" @click="goDiet" icon="Food">
        记录午餐
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
import { ref, onMounted, computed } from 'vue'
import * as echarts from 'echarts'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import request from '../../api/request'
import dayjs from 'dayjs'

const router = useRouter()
const userStore = useUserStore()
const lineChartRef = ref<HTMLElement | null>(null)
const pieChartRef = ref<HTMLElement | null>(null)

const bmiValue = ref('--')
const bmiStatusText = ref('暂无数据')
const bmiStatusClass = ref('')

const bloodPressureValue = ref('--')
const bpStatusText = ref('暂无数据')
const bpStatusClass = ref('')

const mealCount = ref(0)
const mealGoal = 3
const recordCount = ref(0)

const mealProgress = computed(() => Math.min(100, Math.round((mealCount.value / mealGoal) * 100)))
const recordProgress = computed(() => Math.min(100, Math.round((recordCount.value / 3) * 100)))

const goDiet = () => router.push('/diet')
const goChat = () => router.push('/chat')
const goPlan = () => router.push('/plan')

onMounted(() => {
  initLineChart()
  initPieChart()
  fetchSummary()
})

const initLineChart = () => {
    if (!lineChartRef.value) return
    const myChart = echarts.init(lineChartRef.value)
    const option = {
        tooltip: { trigger: 'axis' },
        legend: { data: ['空腹血糖', '收缩压'] },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: {
            type: 'category',
            boundaryGap: false,
            data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
        },
        yAxis: { type: 'value' },
        series: [
            {
                name: '空腹血糖',
                type: 'line',
                data: [6.1, 5.9, 6.2, 5.8, 6.0, 6.3, 5.9],
                itemStyle: { color: '#059669' },
                smooth: true
            },
            {
                name: '收缩压',
                type: 'line',
                data: [130, 128, 132, 129, 125, 131, 128],
                itemStyle: { color: '#3B82F6' },
                smooth: true
            }
        ]
    }
    myChart.setOption(option)
    window.addEventListener('resize', () => myChart.resize())
}

const initPieChart = () => {
    if (!pieChartRef.value) return
    const myChart = echarts.init(pieChartRef.value)
    const option = {
        tooltip: { trigger: 'item' },
        legend: { bottom: '5%', left: 'center' },
        series: [
            {
                name: '营养来源',
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
                data: [
                    { value: 1048, name: '碳水化合物', itemStyle: { color: '#F59E0B' } },
                    { value: 735, name: '蛋白质', itemStyle: { color: '#EF4444' } },
                    { value: 580, name: '脂肪', itemStyle: { color: '#10B981' } },
                    { value: 300, name: '膳食纤维', itemStyle: { color: '#3B82F6' } }
                ]
            }
        ]
    }
    myChart.setOption(option)
    window.addEventListener('resize', () => myChart.resize())
}

const fetchSummary = async () => {
    if (!userStore.user?.id) return

    try {
        const [latestRes, recordsRes, dietRes] = await Promise.all([
            request.get('/health/records/latest', { params: { userId: userStore.user.id } }),
            request.get('/health/records', { params: { userId: userStore.user.id } }),
            request.get('/diet/logs')
        ])

        if (latestRes.code === 200 && latestRes.data) {
            const latest = latestRes.data
            if (latest.height && latest.weight) {
                const heightM = latest.height / 100
                const bmi = latest.weight / (heightM * heightM)
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
            }

            if (latest.systolic && latest.diastolic) {
                bloodPressureValue.value = `${latest.systolic}/${latest.diastolic}`
                if (latest.systolic >= 140 || latest.diastolic >= 90) {
                    bpStatusText.value = '偏高'
                    bpStatusClass.value = 'warning'
                } else {
                    bpStatusText.value = '正常'
                    bpStatusClass.value = 'normal'
                }
            }
        }

        if (recordsRes.code === 200 && Array.isArray(recordsRes.data)) {
            const today = dayjs().format('YYYY-MM-DD')
            recordCount.value = recordsRes.data.filter((item: any) => {
                return item.recordedAt && dayjs(item.recordedAt).format('YYYY-MM-DD') === today
            }).length
        }

        if (dietRes.code === 200 && Array.isArray(dietRes.data)) {
            const today = dayjs().format('YYYY-MM-DD')
            mealCount.value = dietRes.data.filter((item: any) => {
                return item.recordedAt && dayjs(item.recordedAt).format('YYYY-MM-DD') === today
            }).length
        }
    } catch (e) {
        console.error(e)
    }
}
</script>

<style scoped lang="scss">
.dashboard {
  .welcome-section {
    margin-bottom: 24px;
    h2 {
      margin: 0;
      color: #111827;
      font-size: 24px;
    }
    .subtitle {
      color: #6B7280;
      margin: 8px 0 0;
    }
  }

  .health-card {
    height: 160px;
    position: relative;
    border-radius: 12px;
    border: none;
    
    .card-title {
      font-size: 14px;
      color: #6B7280;
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
        margin-top: 10px;
    }
  }

  .charts-row {
      margin-top: 24px;
      .chart-card {
          border-radius: 12px;
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
