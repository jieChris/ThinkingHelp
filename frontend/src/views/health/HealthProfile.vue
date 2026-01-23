<template>
  <div class="health-profile">
    <el-tabs v-model="activeTab" type="border-card">
      <!-- Tab 1: Health Profile (Original Wizard) -->
      <el-tab-pane label="健康档案" name="profile">
          <div class="profile-wizard">
              <el-steps :active="activeStep" finish-status="success" simple style="margin-bottom: 30px">
                <el-step title="基础信息" icon="User" />
                <el-step title="慢病标签" icon="FirstAidKit" />
                <el-step title="过敏禁忌" icon="Warning" />
              </el-steps>

              <div class="step-content">
                <!-- Step 1: Basic Info -->
                <el-form v-if="activeStep === 0" label-width="120px" size="large">
                     <div class="ocr-section">
                        <el-upload
                            class="upload-demo"
                            drag
                            action="#"
                            :auto-upload="false"
                            :on-change="handleOcrFile"
                        >
                            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                            <div class="el-upload__text">
                                将体检报告拖到此处，或 <em>点击上传</em> 进行 OCR 识别
                            </div>
                        </el-upload>
                        <el-progress v-if="ocrProgress > 0" :percentage="ocrProgress" :status="ocrProgress === 100 ? 'success' : ''" />
                     </div>
                     
                     <el-divider>或手动填写</el-divider>
                
                    <el-row :gutter="20">
                        <el-col :span="12">
                             <el-form-item label="姓名">
                                <el-input v-model="form.name" placeholder="张三" />
                            </el-form-item>
                        </el-col>
                        <el-col :span="12">
                             <el-form-item label="性别">
                                <el-radio-group v-model="form.gender">
                                    <el-radio label="MALE">男</el-radio>
                                    <el-radio label="FEMALE">女</el-radio>
                                </el-radio-group>
                            </el-form-item>
                        </el-col>
                    </el-row>
                    <el-row :gutter="20">
                        <el-col :span="12">
                             <el-form-item label="年龄">
                                <el-input-number v-model="form.age" :min="1" :max="120" />
                            </el-form-item>
                        </el-col>
                        <el-col :span="12">
                             <el-form-item label="BMI 指数">
                                <el-input v-model="form.bmi" placeholder="自动计算" disabled>
                                     <template #append>kg/m²</template>
                                </el-input>
                            </el-form-item>
                        </el-col>
                    </el-row>
                </el-form>

                <!-- Step 2: Diseases -->
                <div v-if="activeStep === 1" class="tags-step">
                    <p class="instruction">请选择或输入您确诊的慢性疾病，这将直接影响食谱生成：</p>
                    <el-select
                        v-model="form.diseases"
                        multiple
                        filterable
                        allow-create
                        default-first-option
                        placeholder="请选择或输入标签 (如: 二型糖尿病)"
                        style="width: 100%"
                        size="large"
                    >
                        <el-option label="高血压" value="hypertension" />
                        <el-option label="二型糖尿病" value="diabetes_2" />
                        <el-option label="高尿酸/痛风" value="gout" />
                        <el-option label="高血脂" value="hyperlipidemia" />
                        <el-option label="冠心病" value="chd" />
                    </el-select>
                    
                    <div class="common-tags">
                        <span class="label">常见标签：</span>
                        <el-tag 
                            v-for="tag in commonDiseases" 
                            :key="tag.value" 
                            @click="addDisease(tag.value)"
                            class="clickable-tag"
                            :type="form.diseases.includes(tag.value) ? 'success' : 'info'"
                        >
                            {{ tag.label }}
                        </el-tag>
                    </div>
                </div>

                <!-- Step 3: Allergies -->
                <div v-if="activeStep === 2" class="allergies-step">
                    <el-form-item label="过敏源">
                         <el-checkbox-group v-model="form.allergies">
                            <el-checkbox label="海鲜/虾蟹" border />
                            <el-checkbox label="坚果/花生" border />
                            <el-checkbox label="乳糖不耐受" border />
                            <el-checkbox label="芒果" border />
                            <el-checkbox label="麸质" border />
                         </el-checkbox-group>
                    </el-form-item>
                    
                    <el-form-item label="其他忌口">
                        <el-input v-model="form.otherRestrictions" type="textarea" placeholder="例如：不吃香菜，不吃羊肉..." />
                    </el-form-item>
                </div>
              </div>

              <div class="step-footer">
                <el-button v-if="activeStep > 0" @click="activeStep--">上一步</el-button>
                <el-button v-if="activeStep < 2" type="primary" @click="activeStep++">下一步</el-button>
                <el-button v-if="activeStep === 2" type="success" @click="submitProfile" icon="Check">保存档案</el-button>
              </div>
          </div>
      </el-tab-pane>

      <!-- Tab 2: Health Metrics (New Feature) -->
      <el-tab-pane label="数据记录" name="metrics">
         <div class="metrics-container">
             <div class="long-term-card">
                 <div class="card-header">
                     <h3>📌 长期数据</h3>
                     <el-button size="small" @click="openLongTermDialog">修改</el-button>
                 </div>
                 <div class="card-body">
                     <div class="item">身高: <strong>{{ longTerm.height || '--' }}</strong> cm</div>
                     <div class="item">忌口: <strong>{{ longTerm.restrictions || '未填写' }}</strong></div>
                 </div>
             </div>
             <div class="record-form">
                 <div class="form-header">
                    <h3>📝 批量记录数据</h3>
                    <el-date-picker 
                        v-model="batchForm.recordedAt" 
                        type="datetime" 
                        placeholder="记录时间" 
                        :default-time="defaultTime"
                        format="YYYY-MM-DD HH:mm"
                    />
                 </div>
                 
                 <el-form :model="batchForm.values" label-width="100px" class="batch-form-grid">
                    <el-row :gutter="20">
                        <!-- Weight -->
                        <el-col :span="8">
                            <el-form-item label="体重">
                                <el-input v-model="batchForm.values.weight" type="number" step="0.1">
                                    <template #append>kg</template>
                                </el-input>
                            </el-form-item>
                        </el-col>
                        <!-- BMI (Readonly calculation?) - Maybe later, kept simple for now -->
                        
                        <!-- Blood Pressure -->
                        <el-col :span="8">
                             <el-form-item label="收缩压">
                                <el-input v-model="batchForm.values.systolic" type="number">
                                    <template #append>mmHg</template>
                                </el-input>
                            </el-form-item>
                        </el-col>
                        <el-col :span="8">
                             <el-form-item label="舒张压">
                                <el-input v-model="batchForm.values.diastolic" type="number">
                                    <template #append>mmHg</template>
                                </el-input>
                            </el-form-item>
                        </el-col>
                        
                        <!-- Glucose -->
                        <el-col :span="8">
                             <el-form-item label="空腹血糖">
                                <el-input v-model="batchForm.values.glucose" type="number" step="0.1">
                                    <template #append>mmol/L</template>
                                </el-input>
                            </el-form-item>
                        </el-col>
                        
                        <!-- Heart Rate -->
                        <el-col :span="8">
                             <el-form-item label="心率">
                                <el-input v-model="batchForm.values.heart_rate" type="number">
                                    <template #append>bpm</template>
                                </el-input>
                            </el-form-item>
                        </el-col>
                    </el-row>
                    
                    <div class="form-actions">
                        <el-button type="primary" size="large" @click="submitBatchMetrics" :loading="adding" style="width: 200px">
                            一键保存所有数据
                        </el-button>
                    </div>
                 </el-form>
             </div>

             <el-divider />

             <div class="history-list">
                 <div class="list-header">
                     <h3>📅 历史记录</h3>
                     <div class="list-actions">
                         <el-button size="small" @click="applyMetricsPreset('day')">今天</el-button>
                         <el-button size="small" @click="applyMetricsPreset('3d')">近3天</el-button>
                         <el-button size="small" @click="applyMetricsPreset('week')">本周</el-button>
                         <el-button size="small" @click="applyMetricsPreset('month')">本月</el-button>
                         <el-button size="small" @click="applyMetricsPreset('all')">全部</el-button>
                         <el-button circle icon="Refresh" @click="fetchMetrics" />
                     </div>
                 </div>
                 <div class="filter-row">
                     <el-date-picker
                        v-model="metricsRange"
                        type="daterange"
                        range-separator="至"
                        start-placeholder="开始日期"
                        end-placeholder="结束日期"
                        format="YYYY-MM-DD"
                        value-format="YYYY-MM-DD"
                        @change="fetchMetrics"
                     />
                 </div>
                 
                 <el-table :data="metricsList" style="width: 100%" v-loading="loadingMetrics">
                    <el-table-column prop="recordedAt" label="记录时间" width="160" :formatter="formatDate" />
                    <el-table-column prop="weight" label="体重(kg)" width="100" />
                    <el-table-column label="血压(mmHg)" width="140">
                         <template #default="scope">
                            <span v-if="scope.row.systolic && scope.row.diastolic">
                                {{ scope.row.systolic }} / {{ scope.row.diastolic }}
                            </span>
                        </template>
                    </el-table-column>
                    <el-table-column prop="glucose" label="血糖" width="100" />
                    <el-table-column prop="heartRate" label="心率" width="100" />
                    <el-table-column label="操作" min-width="100">
                        <template #default="scope">
                            <el-button link type="danger" @click="deleteMetric(scope.row.id)">删除</el-button>
                        </template>
                    </el-table-column>
                 </el-table>
             </div>
         </div>
      </el-tab-pane>
    </el-tabs>
  </div>

  <el-dialog v-model="longTermDialogVisible" title="长期数据" width="480px">
      <el-form label-width="100px">
          <el-form-item label="身高(cm)">
              <el-input-number v-model="longTermForm.height" :min="50" :max="250" :step="1" />
          </el-form-item>
          <el-form-item label="忌口">
              <el-input v-model="longTermForm.restrictions" type="textarea" placeholder="如：不吃香菜，不吃发物..." />
          </el-form-item>
      </el-form>
      <template #footer>
          <el-button @click="longTermDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveLongTerm">保存</el-button>
      </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../stores/user'
import request from '../../api/request'
import dayjs from 'dayjs'

const userStore = useUserStore()
const activeTab = ref('profile')
const activeStep = ref(0)
const ocrProgress = ref(0)
const defaultTime = new Date(2000, 1, 1, 8, 0, 0)
const metricsRange = ref<[string, string] | null>(null)
const longTermDialogVisible = ref(false)
const longTerm = reactive({
    height: '',
    restrictions: ''
})
const longTermForm = reactive({
    height: 0,
    restrictions: ''
})

// --- Profile Logic ---
const form = reactive({
    name: '',
    gender: 'MALE',
    age: 55,
    height: 175,
    weight: 70,
    bmi: '22.8',
    diseases: [] as string[],
    allergies: [] as string[],
    otherRestrictions: ''
})

const commonDiseases = [
    { label: '高血压', value: 'hypertension' },
    { label: '糖尿病', value: 'diabetes_2' },
    { label: '痛风', value: 'gout' }
]

const addDisease = (val: string) => {
    if (!form.diseases.includes(val)) {
        form.diseases.push(val)
    }
}

const handleOcrFile = () => {
    ocrProgress.value = 0
    const interval = setInterval(() => {
        ocrProgress.value += 10
        if (ocrProgress.value >= 100) {
            clearInterval(interval)
            ElMessage.success('OCR 识别成功，已自动填充数据')
            form.name = '张建国'
            form.age = 58
            form.diseases = ['hypertension', 'hyperlipidemia']
        }
    }, 200)
}

const fetchProfile = async () => {
    try {
        const res: any = await request.get('/health/profile')
        if (res.code === 200 && res.data) {
            const data = res.data
            form.name = data.name || ''
            form.gender = data.gender || 'MALE'
            form.age = data.age || 0
            form.height = data.height || 0
            form.weight = data.weight || 0
            form.bmi = data.bmi || ''
            form.diseases = data.diseases || []
            form.allergies = data.allergies || []
            form.otherRestrictions = data.otherRestrictions || ''
        }
    } catch (e) {
        console.error(e)
    }
}

const buildProfilePayload = () => {
    return {
        name: form.name,
        gender: form.gender,
        age: form.age,
        height: form.height,
        weight: form.weight,
        bmi: form.bmi,
        diseases: form.diseases,
        allergies: form.allergies,
        otherRestrictions: form.otherRestrictions
    }
}

const submitProfile = async () => {
    try {
        const res: any = await request.post('/health/profile', buildProfilePayload())
        if (res.code === 200) {
            ElMessage.success('档案保存成功！')
        } else {
            ElMessage.error(res.msg || '保存失败')
        }
    } catch (e) {
        console.error(e)
    }
}

const saveProfileSilently = async () => {
    try {
        await request.post('/health/profile', buildProfilePayload())
    } catch (e) {
        console.error(e)
    }
}

// --- Metrics Logic ---
interface HealthRecord {
    id: number
    height?: number
    weight?: number
    systolic?: number
    diastolic?: number
    glucose?: number
    heartRate?: number
    recordedAt: string
}

const adding = ref(false)
const loadingMetrics = ref(false)
const metricsList = ref<HealthRecord[]>([])

// Batch Form
const batchForm = reactive({
    recordedAt: new Date(),
    values: {
        weight: '',
        systolic: '',
        diastolic: '',
        glucose: '',
        heart_rate: ''
    } as Record<string, string | number>
})

const formatDate = (row: any) => {
    return dayjs(row.recordedAt).format('YYYY-MM-DD HH:mm')
}

const fetchMetrics = async () => {
    if (!userStore.user?.id) return
    loadingMetrics.value = true
    try {
        const params: any = { userId: userStore.user.id }
        if (metricsRange.value) {
            params.start = `${metricsRange.value[0]} 00:00:00`
            params.end = `${metricsRange.value[1]} 23:59:59`
        }
        const res: any = await request.get('/health/records', { params })
        if (res.code === 200) {
            metricsList.value = res.data
            prefillBatchForm()
        }
    } catch (e) {
        console.error(e)
    } finally {
        loadingMetrics.value = false
    }
}

const prefillBatchForm = () => {
    // Get latest record
    if (metricsList.value.length > 0) {
        const latest = metricsList.value[0]
        batchForm.values.weight = latest.weight || ''
        batchForm.values.systolic = latest.systolic || ''
        batchForm.values.diastolic = latest.diastolic || ''
        batchForm.values.glucose = latest.glucose || ''
        batchForm.values.heart_rate = latest.heartRate || ''
    } else {
        // Defaults
        batchForm.values.weight = 60
        batchForm.values.systolic = 120
        batchForm.values.diastolic = 80
        batchForm.values.glucose = 5.5
        batchForm.values.heart_rate = 75
    }
}

const submitBatchMetrics = async () => {
    if (!userStore.user?.id) return

    adding.value = true
    try {
        // Construct payload manually from form to entity fields
        const payload = {
            userId: userStore.user.id,
            weight: batchForm.values.weight ? Number(batchForm.values.weight) : null,
            systolic: batchForm.values.systolic ? Number(batchForm.values.systolic) : null,
            diastolic: batchForm.values.diastolic ? Number(batchForm.values.diastolic) : null,
            glucose: batchForm.values.glucose ? Number(batchForm.values.glucose) : null,
            heartRate: batchForm.values.heart_rate ? Number(batchForm.values.heart_rate) : null,
            recordedAt: batchForm.recordedAt
        }

        const res: any = await request.post('/health/records', payload)
        if (res.code === 200) {
            ElMessage.success('记录保存成功')
            fetchMetrics()
        } else {
            ElMessage.error(res.msg || '保存失败')
        }
    } catch (e) {
        console.error(e)
    } finally {
        adding.value = false
    }
}

const deleteMetric = async (id: number) => {
    try {
        await request.delete(`/health/records/${id}`)
        ElMessage.success('删除成功')
        fetchMetrics()
    } catch (e) {
        console.error(e)
    }
}

onMounted(() => {
    fetchProfile()
    loadLongTermData()
    fetchMetrics()
})

const loadLongTermData = async () => {
    const userId = userStore.user?.id
    if (!userId) return
    const cacheKey = `long_term_data_${userId}`
    const cached = localStorage.getItem(cacheKey)
    if (cached) {
        try {
            const parsed = JSON.parse(cached)
            longTerm.height = parsed.height || ''
            longTerm.restrictions = parsed.restrictions || ''
            return
        } catch (e) {
            console.error(e)
        }
    }
    try {
        const res: any = await request.get('/health/profile')
        if (res.code === 200 && res.data) {
            longTerm.height = res.data.height ? String(res.data.height) : ''
            longTerm.restrictions = res.data.otherRestrictions || ''
            localStorage.setItem(cacheKey, JSON.stringify(longTerm))
        }
    } catch (e) {
        console.error(e)
    }
}

const openLongTermDialog = () => {
    longTermForm.height = longTerm.height ? Number(longTerm.height) : 0
    longTermForm.restrictions = longTerm.restrictions || ''
    longTermDialogVisible.value = true
}

const saveLongTerm = async () => {
    const userId = userStore.user?.id
    if (!userId) return
    longTerm.height = longTermForm.height ? String(longTermForm.height) : ''
    longTerm.restrictions = longTermForm.restrictions || ''
    localStorage.setItem(`long_term_data_${userId}`, JSON.stringify(longTerm))
    form.height = longTermForm.height || form.height
    form.otherRestrictions = longTermForm.restrictions || form.otherRestrictions
    longTermDialogVisible.value = false
    await saveProfileSilently()
    ElMessage.success('长期数据已保存')
}

const applyMetricsPreset = (preset: 'day' | '3d' | 'week' | 'month' | 'all') => {
    if (preset === 'all') {
        metricsRange.value = null
        fetchMetrics()
        return
    }
    const end = dayjs().endOf('day')
    let start = dayjs().startOf('day')
    if (preset === '3d') {
        start = dayjs().subtract(2, 'day').startOf('day')
    } else if (preset === 'week') {
        const day = dayjs().day()
        const diff = day === 0 ? -6 : 1 - day
        start = dayjs().add(diff, 'day').startOf('day')
    } else if (preset === 'month') {
        start = dayjs().startOf('month')
    }
    metricsRange.value = [start.format('YYYY-MM-DD'), end.format('YYYY-MM-DD')]
    fetchMetrics()
}

watch(
    () => activeTab.value,
    (tab) => {
        if (tab !== 'metrics') return
        const userId = userStore.user?.id
        if (!userId) return
        const promptKey = `long_term_prompted_${userId}`
        if (!localStorage.getItem(promptKey)) {
            localStorage.setItem(promptKey, 'true')
            openLongTermDialog()
        }
    }
)
</script>

<style scoped lang="scss">
.health-profile {
    padding: 20px;
}

.step-content {
    min-height: 300px;
    padding: 20px 40px;
}

.ocr-section {
    margin-bottom: 30px;
    text-align: center;
    .upload-demo {
        width: 100%;
    }
}

.tags-step, .allergies-step {
    padding: 20px 0;
}

.instruction {
    font-size: 16px;
    color: #606266;
    margin-bottom: 15px;
}

.common-tags {
    margin-top: 20px;
    .label {
        color: #909399;
        font-size: 14px;
        margin-right: 10px;
    }
    .clickable-tag {
        margin-right: 10px;
        cursor: pointer;
        transition: all 0.2s;
        &:hover {
            transform: scale(1.05);
        }
    }
}

.step-footer {
    display: flex;
    justify-content: flex-end;
    margin-top: 30px;
    padding-top: 20px;
    border-top: 1px solid #EBEEF5;
}

.metrics-container {
    padding: 20px;

    .long-term-card {
        background: #fff7ed;
        border: 1px solid #fed7aa;
        border-radius: 8px;
        padding: 16px;
        margin-bottom: 20px;

        .card-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 10px;
            h3 { margin: 0; }
        }

        .card-body {
            display: flex;
            flex-direction: column;
            gap: 8px;
            .item {
                color: #7c2d12;
            }
        }
    }
    
    .record-form {
        h3 {
            margin-bottom: 20px;
            color: #333;
        }
        
        .form-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 25px;
            
            h3 { margin: 0; }
        }
        
        .batch-form-grid {
            background: #fcfcfc;
            padding: 24px;
            border-radius: 8px;
            border: 1px solid #ebeef5;
        }
        
        .form-actions {
            margin-top: 20px;
            display: flex;
            justify-content: center;
        }
    }
    
    .history-list {
        margin-top: 20px;
        .list-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 15px;
            
            h3 { margin: 0; }
            .list-actions {
                display: flex;
                gap: 8px;
                flex-wrap: wrap;
                align-items: center;
            }
        }
        
        .metric-value {
            font-weight: bold;
            font-size: 16px;
            color: #059669;
        }
        .unit {
            font-size: 12px;
            color: #999;
            margin-left: 4px;
        }
    }
    .filter-row {
        margin-bottom: 12px;
    }
}
</style>
