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
                        <!-- Height -->
                        <el-col :span="8">
                            <el-form-item label="身高">
                                <el-input v-model="batchForm.values.height" type="number" step="0.1">
                                    <template #append>cm</template>
                                </el-input>
                            </el-form-item>
                        </el-col>
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
                     <el-button circle icon="Refresh" @click="fetchMetrics" />
                 </div>
                 
                 <el-table :data="metricsList" style="width: 100%" v-loading="loadingMetrics">
                    <el-table-column prop="recordedAt" label="记录时间" width="160" :formatter="formatDate" />
                    <el-table-column prop="height" label="身高(cm)" width="100" />
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
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../stores/user'
import request from '../../api/request'
import dayjs from 'dayjs'

const userStore = useUserStore()
const activeTab = ref('profile')
const activeStep = ref(0)
const ocrProgress = ref(0)
const defaultTime = new Date(2000, 1, 1, 8, 0, 0)

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

const submitProfile = () => {
    ElMessage.success('档案保存成功！即将为您更新个性化食谱...')
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
        height: '',
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
        const res: any = await request.get('/health/records', {
            params: { userId: userStore.user.id }
        })
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
        batchForm.values.height = latest.height || ''
        batchForm.values.weight = latest.weight || ''
        batchForm.values.systolic = latest.systolic || ''
        batchForm.values.diastolic = latest.diastolic || ''
        batchForm.values.glucose = latest.glucose || ''
        batchForm.values.heart_rate = latest.heartRate || ''
    } else {
        // Defaults
        batchForm.values.height = 170
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
            height: batchForm.values.height ? Number(batchForm.values.height) : null,
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
    fetchMetrics()
})
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
}
</style>
