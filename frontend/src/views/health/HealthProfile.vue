<template>
  <div class="health-profile-root">
  <div class="health-profile">
    <el-tabs v-model="activeTab" type="border-card">
      <!-- Tab 1: Health Profile (Original Wizard) -->
      <el-tab-pane label="健康档案" name="profile">
          <div class="profile-wizard">
              <el-steps :active="activeStep" finish-status="success" simple style="margin-bottom: 30px">
                <el-step title="基础信息" icon="User" />
                <el-step title="慢病标签" icon="FirstAidKit" />
                <el-step title="过敏禁忌" icon="Warning" />
                <el-step title="体检指标" icon="DataAnalysis" />
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
                    <el-row :gutter="20">
                        <el-col :span="12">
                             <el-form-item label="日常活动量">
                                <el-select v-model="form.activityLevel" placeholder="请选择">
                                    <el-option label="久坐/少动" value="SEDENTARY" />
                                    <el-option label="轻度活动" value="LIGHT" />
                                    <el-option label="中度活动" value="MODERATE" />
                                    <el-option label="高强度活动" value="ACTIVE" />
                                </el-select>
                            </el-form-item>
                        </el-col>
                        <el-col :span="12">
                             <el-form-item label="健康目标">
                                <el-select v-model="form.goal" placeholder="请选择">
                                    <el-option label="维持体重" value="MAINTAIN" />
                                    <el-option label="减脂/减重" value="LOSE" />
                                    <el-option label="增肌/增重" value="GAIN" />
                                </el-select>
                            </el-form-item>
                        </el-col>
                    </el-row>
                    <el-row :gutter="20">
                        <el-col :span="12">
                             <el-form-item label="运动频率">
                                <el-input v-model="form.exerciseFrequency" type="number" placeholder="例如 3">
                                    <template #append>次/周</template>
                                </el-input>
                            </el-form-item>
                        </el-col>
                        <el-col :span="12">
                             <el-form-item label="单次时长">
                                <el-input v-model="form.exerciseDuration" type="number" placeholder="例如 30">
                                    <template #append>分钟</template>
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

                <!-- Step 4: Exam Metrics -->
                <div v-if="activeStep === 3" class="exam-step">
                    <el-form label-width="120px" size="large">
                        <el-form-item label="体检日期">
                            <el-date-picker v-model="form.reportDate" type="date" placeholder="选择日期" format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
                        </el-form-item>

                        <el-divider>血压</el-divider>
                        <el-row :gutter="20">
                            <el-col :span="12">
                                <el-form-item label="收缩压">
                                    <el-input v-model="form.bpSystolic" type="number" placeholder="120">
                                        <template #append>mmHg</template>
                                    </el-input>
                                </el-form-item>
                            </el-col>
                            <el-col :span="12">
                                <el-form-item label="舒张压">
                                    <el-input v-model="form.bpDiastolic" type="number" placeholder="80">
                                        <template #append>mmHg</template>
                                    </el-input>
                                </el-form-item>
                            </el-col>
                        </el-row>

                        <el-divider>血糖</el-divider>
                        <el-row :gutter="20">
                            <el-col :span="12">
                                <el-form-item label="空腹血糖">
                                    <el-input v-model="form.fastingGlucose" type="number" placeholder="5.6">
                                        <template #append>mmol/L</template>
                                    </el-input>
                                </el-form-item>
                            </el-col>
                            <el-col :span="12">
                                <el-form-item label="糖化血红蛋白">
                                    <el-input v-model="form.hba1c" type="number" placeholder="5.8">
                                        <template #append>%</template>
                                    </el-input>
                                </el-form-item>
                            </el-col>
                        </el-row>

                        <el-divider>血脂</el-divider>
                        <el-row :gutter="20">
                            <el-col :span="12">
                                <el-form-item label="总胆固醇">
                                    <el-input v-model="form.totalCholesterol" type="number" placeholder="4.8">
                                        <template #append>mmol/L</template>
                                    </el-input>
                                </el-form-item>
                            </el-col>
                            <el-col :span="12">
                                <el-form-item label="甘油三酯">
                                    <el-input v-model="form.triglycerides" type="number" placeholder="1.2">
                                        <template #append>mmol/L</template>
                                    </el-input>
                                </el-form-item>
                            </el-col>
                            <el-col :span="12">
                                <el-form-item label="HDL-C">
                                    <el-input v-model="form.hdl" type="number" placeholder="1.2">
                                        <template #append>mmol/L</template>
                                    </el-input>
                                </el-form-item>
                            </el-col>
                            <el-col :span="12">
                                <el-form-item label="LDL-C">
                                    <el-input v-model="form.ldl" type="number" placeholder="2.6">
                                        <template #append>mmol/L</template>
                                    </el-input>
                                </el-form-item>
                            </el-col>
                        </el-row>

                        <el-divider>尿酸 / 肝肾功能</el-divider>
                        <el-row :gutter="20">
                            <el-col :span="12">
                                <el-form-item label="尿酸">
                                    <el-input v-model="form.uricAcid" type="number" placeholder="300">
                                        <template #append>µmol/L</template>
                                    </el-input>
                                </el-form-item>
                            </el-col>
                            <el-col :span="12">
                                <el-form-item label="ALT">
                                    <el-input v-model="form.alt" type="number" placeholder="25">
                                        <template #append>U/L</template>
                                    </el-input>
                                </el-form-item>
                            </el-col>
                            <el-col :span="12">
                                <el-form-item label="AST">
                                    <el-input v-model="form.ast" type="number" placeholder="22">
                                        <template #append>U/L</template>
                                    </el-input>
                                </el-form-item>
                            </el-col>
                            <el-col :span="12">
                                <el-form-item label="肌酐">
                                    <el-input v-model="form.creatinine" type="number" placeholder="80">
                                        <template #append>µmol/L</template>
                                    </el-input>
                                </el-form-item>
                            </el-col>
                            <el-col :span="12">
                                <el-form-item label="尿素氮">
                                    <el-input v-model="form.bun" type="number" placeholder="5.0">
                                        <template #append>mmol/L</template>
                                    </el-input>
                                </el-form-item>
                            </el-col>
                        </el-row>

                        <el-alert
                            v-if="riskTips.length > 0"
                            type="warning"
                            show-icon
                            title="风险提示（参考范围，仅供提示）"
                            :closable="false"
                            class="risk-alert"
                        >
                            <template #default>
                                <ul>
                                    <li v-for="tip in riskTips" :key="tip">{{ tip }}</li>
                                </ul>
                            </template>
                        </el-alert>
                    </el-form>
                </div>
              </div>

              <div class="step-footer">
                <el-button v-if="activeStep > 0" @click="activeStep--">上一步</el-button>
                <el-button v-if="activeStep < 3" type="primary" @click="activeStep++">下一步</el-button>
                <el-button v-if="activeStep === 3" type="success" @click="submitProfile" icon="Check">保存档案</el-button>
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
                        
                        <el-col :span="8">
                            <el-form-item label="血糖记录">
                                <el-button type="primary" plain @click="activeTab = 'glucose'">去控糖管理记录</el-button>
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

      <!-- Tab 3: Glucose Management -->
      <el-tab-pane label="控糖管理" name="glucose">
          <div class="glucose-container">
              <div class="glucose-form-card">
                  <div class="list-header">
                      <h3>🩸 血糖专项记录</h3>
                  </div>
                  <el-form label-width="120px" size="large">
                      <el-row :gutter="20">
                          <el-col :span="8">
                              <el-form-item label="记录时间">
                                  <el-date-picker
                                      v-model="glucoseForm.recordedAt"
                                      type="datetime"
                                      format="YYYY-MM-DD HH:mm"
                                      value-format="YYYY-MM-DD HH:mm:ss"
                                  />
                              </el-form-item>
                          </el-col>
                          <el-col :span="8">
                              <el-form-item label="血糖类型">
                                  <el-select v-model="glucoseForm.measureType">
                                      <el-option label="空腹" value="FASTING" />
                                      <el-option label="餐后2小时" value="POST_MEAL_2H" />
                                      <el-option label="睡前" value="BEFORE_SLEEP" />
                                      <el-option label="随机" value="RANDOM" />
                                  </el-select>
                              </el-form-item>
                          </el-col>
                          <el-col :span="8">
                              <el-form-item label="血糖值">
                                  <el-input v-model="glucoseForm.glucoseValue" type="number" step="0.1">
                                      <template #append>mmol/L</template>
                                  </el-input>
                              </el-form-item>
                          </el-col>
                          <el-col :span="8">
                              <el-form-item label="相关餐次">
                                  <el-input v-model="glucoseForm.relatedMeal" placeholder="如：早餐后、晚餐前" />
                              </el-form-item>
                          </el-col>
                          <el-col :span="8">
                              <el-form-item label="低/高血糖事件">
                                  <el-select v-model="glucoseForm.eventType" placeholder="自动判定">
                                      <el-option label="正常" value="NORMAL" />
                                      <el-option label="偏高" value="HIGH" />
                                      <el-option label="偏低" value="LOW" />
                                  </el-select>
                              </el-form-item>
                          </el-col>
                          <el-col :span="8">
                              <el-form-item label="用药备注">
                                  <el-input v-model="glucoseForm.medicationNote" placeholder="如：二甲双胍 0.5g" />
                              </el-form-item>
                          </el-col>
                          <el-col :span="24">
                              <el-form-item label="症状描述">
                                  <el-input v-model="glucoseForm.symptoms" type="textarea" :rows="2" placeholder="如：头晕、心慌、出汗" />
                              </el-form-item>
                          </el-col>
                      </el-row>
                      <div class="form-actions">
                          <el-button type="primary" :loading="glucoseSubmitting" @click="submitGlucoseRecord">保存血糖记录</el-button>
                      </div>
                  </el-form>
              </div>

              <el-divider />

              <div class="glucose-summary">
                  <el-row :gutter="16">
                      <el-col :span="6">
                          <el-card shadow="never">
                              <div class="summary-title">平均血糖</div>
                              <div class="summary-value">{{ displayNum(glucoseSummary.avgGlucose) }}</div>
                          </el-card>
                      </el-col>
                      <el-col :span="6">
                          <el-card shadow="never">
                              <div class="summary-title">空腹均值</div>
                              <div class="summary-value">{{ displayNum(glucoseSummary.fastingAvg) }}</div>
                          </el-card>
                      </el-col>
                      <el-col :span="6">
                          <el-card shadow="never">
                              <div class="summary-title">高血糖次数</div>
                              <div class="summary-value warning">{{ glucoseSummary.highCount || 0 }}</div>
                          </el-card>
                      </el-col>
                      <el-col :span="6">
                          <el-card shadow="never">
                              <div class="summary-title">低血糖次数</div>
                              <div class="summary-value danger">{{ glucoseSummary.lowCount || 0 }}</div>
                          </el-card>
                      </el-col>
                  </el-row>
              </div>

              <el-alert
                  v-if="glucoseAnalysis.alerts.length"
                  :title="`风险等级：${glucoseRiskLabel(glucoseAnalysis.riskLevel)}`"
                  type="warning"
                  show-icon
                  :closable="false"
                  class="risk-alert"
              >
                  <template #default>
                      <ul class="analysis-alerts">
                          <li v-for="item in glucoseAnalysis.alerts" :key="item">{{ item }}</li>
                      </ul>
                  </template>
              </el-alert>

              <el-card class="glucose-chart-card" shadow="never">
                  <template #header>
                      <div class="list-header">
                          <h3>📈 血糖趋势</h3>
                      </div>
                  </template>
                  <div ref="glucoseChartRef" class="glucose-chart"></div>
              </el-card>

              <div class="history-list">
                  <div class="list-header">
                      <h3>📋 控糖历史</h3>
                      <div class="list-actions">
                          <el-button size="small" @click="applyGlucosePreset('day')">今天</el-button>
                          <el-button size="small" @click="applyGlucosePreset('3d')">近3天</el-button>
                          <el-button size="small" @click="applyGlucosePreset('week')">本周</el-button>
                          <el-button size="small" @click="applyGlucosePreset('month')">本月</el-button>
                          <el-button size="small" @click="applyGlucosePreset('all')">全部</el-button>
                          <el-button circle icon="Refresh" @click="fetchGlucoseRecords" />
                      </div>
                  </div>
                  <div class="filter-row">
                      <el-date-picker
                          v-model="glucoseRange"
                          type="daterange"
                          range-separator="至"
                          start-placeholder="开始日期"
                          end-placeholder="结束日期"
                          format="YYYY-MM-DD"
                          value-format="YYYY-MM-DD"
                          @change="fetchGlucoseRecords"
                      />
                  </div>
                  <el-table :data="glucoseRecords" style="width: 100%" v-loading="glucoseLoading">
                      <el-table-column prop="recordedAt" label="记录时间" width="180" :formatter="formatDate" />
                      <el-table-column label="类型" width="130">
                          <template #default="scope">{{ glucoseMeasureLabel(scope.row.measureType) }}</template>
                      </el-table-column>
                      <el-table-column label="血糖值" width="120">
                          <template #default="scope">{{ scope.row.glucoseValue }} mmol/L</template>
                      </el-table-column>
                      <el-table-column label="事件" width="120">
                          <template #default="scope">
                              <el-tag :type="glucoseEventTagType(scope.row.eventType)" size="small">
                                  {{ glucoseEventLabel(scope.row.eventType) }}
                              </el-tag>
                          </template>
                      </el-table-column>
                      <el-table-column prop="relatedMeal" label="相关餐次" />
                      <el-table-column prop="medicationNote" label="用药备注" />
                      <el-table-column label="操作" width="100">
                          <template #default="scope">
                              <el-button link type="danger" @click="deleteGlucoseRecord(scope.row.id)">删除</el-button>
                          </template>
                      </el-table-column>
                  </el-table>
              </div>

              <el-card class="glucose-impact-card" shadow="never">
                  <template #header>
                      <div class="list-header">
                          <h3>🍽️ 饮食关联分析（餐后高血糖）</h3>
                      </div>
                  </template>
                  <el-table :data="glucoseAnalysis.mealImpacts" style="width: 100%">
                      <el-table-column prop="glucoseTime" label="血糖时间" width="170" />
                      <el-table-column prop="glucoseValue" label="血糖值" width="110">
                          <template #default="scope">
                              {{ scope.row.glucoseValue }} mmol/L
                          </template>
                      </el-table-column>
                      <el-table-column prop="mealTime" label="关联餐次时间" width="170" />
                      <el-table-column prop="mealType" label="餐次" width="100" />
                      <el-table-column prop="mealName" label="餐食内容" />
                      <el-table-column prop="suggestion" label="建议" min-width="220" />
                  </el-table>
              </el-card>

              <el-card class="glucose-impact-card" shadow="never">
                  <template #header>
                      <div class="list-header">
                          <h3>⏰ 随访提醒任务</h3>
                          <div class="list-actions">
                              <el-button size="small" :loading="glucoseTaskGenerating" @click="generateGlucoseTasks">自动生成任务</el-button>
                              <el-button size="small" @click="fetchGlucoseTasks">刷新</el-button>
                          </div>
                      </div>
                  </template>
                  <el-table :data="glucoseTasks" style="width: 100%" v-loading="glucoseTaskLoading">
                      <el-table-column prop="title" label="任务" min-width="220" />
                      <el-table-column prop="note" label="说明" min-width="200" />
                      <el-table-column prop="dueAt" label="截止时间" width="170">
                          <template #default="scope">{{ formatTaskDate(scope.row.dueAt) }}</template>
                      </el-table-column>
                      <el-table-column prop="status" label="状态" width="100">
                          <template #default="scope">
                              <el-tag :type="scope.row.status === 'DONE' ? 'success' : 'warning'" size="small">
                                  {{ scope.row.status === 'DONE' ? '已完成' : '待完成' }}
                              </el-tag>
                          </template>
                      </el-table-column>
                      <el-table-column label="操作" width="140">
                          <template #default="scope">
                              <el-button
                                  v-if="scope.row.status !== 'DONE'"
                                  link
                                  type="primary"
                                  @click="completeGlucoseTask(scope.row.id)"
                              >完成</el-button>
                              <el-button link type="danger" @click="deleteGlucoseTask(scope.row.id)">删除</el-button>
                          </template>
                      </el-table-column>
                  </el-table>
              </el-card>
          </div>
      </el-tab-pane>

      <!-- Tab 3: Profile History -->
      <el-tab-pane label="档案历史" name="history">
          <div class="history-container">
              <div class="list-header">
                  <h3>🗂️ 历史档案</h3>
                  <el-button size="small" @click="fetchHistory">刷新</el-button>
              </div>
              <el-table :data="historyList" style="width: 100%" v-loading="historyLoading">
                  <el-table-column prop="createdAt" label="保存时间" width="180">
                      <template #default="scope">
                          {{ formatHistoryDate(scope.row.createdAt) }}
                      </template>
                  </el-table-column>
                  <el-table-column prop="reportDate" label="体检日期" width="140" />
                  <el-table-column prop="summary" label="摘要" />
                  <el-table-column label="操作" width="200">
                      <template #default="scope">
                          <el-button link type="primary" @click="openHistory(scope.row.id)">查看/编辑</el-button>
                          <el-button link type="success" @click="applyHistory(scope.row.id)">应用为当前</el-button>
                      </template>
                  </el-table-column>
              </el-table>
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

  <el-dialog v-model="historyDialogVisible" title="历史档案详情" width="720px">
      <el-form label-width="110px">
          <el-divider>基础信息</el-divider>
          <el-row :gutter="20">
              <el-col :span="12">
                  <el-form-item label="姓名">
                      <el-input v-model="historyForm.name" />
                  </el-form-item>
              </el-col>
              <el-col :span="12">
                  <el-form-item label="性别">
                      <el-radio-group v-model="historyForm.gender">
                          <el-radio label="MALE">男</el-radio>
                          <el-radio label="FEMALE">女</el-radio>
                      </el-radio-group>
                  </el-form-item>
              </el-col>
              <el-col :span="12">
                  <el-form-item label="年龄">
                      <el-input-number v-model="historyForm.age" :min="1" :max="120" />
                  </el-form-item>
              </el-col>
              <el-col :span="12">
                  <el-form-item label="身高(cm)">
                      <el-input v-model="historyForm.height" type="number" />
                  </el-form-item>
              </el-col>
              <el-col :span="12">
                  <el-form-item label="体重(kg)">
                      <el-input v-model="historyForm.weight" type="number" />
                  </el-form-item>
              </el-col>
              <el-col :span="12">
                  <el-form-item label="BMI">
                      <el-input v-model="historyForm.bmi" />
                  </el-form-item>
              </el-col>
          </el-row>
          <el-row :gutter="20">
              <el-col :span="12">
                  <el-form-item label="日常活动量">
                      <el-select v-model="historyForm.activityLevel" placeholder="请选择">
                          <el-option label="久坐/少动" value="SEDENTARY" />
                          <el-option label="轻度活动" value="LIGHT" />
                          <el-option label="中度活动" value="MODERATE" />
                          <el-option label="高强度活动" value="ACTIVE" />
                      </el-select>
                  </el-form-item>
              </el-col>
              <el-col :span="12">
                  <el-form-item label="健康目标">
                      <el-select v-model="historyForm.goal" placeholder="请选择">
                          <el-option label="维持体重" value="MAINTAIN" />
                          <el-option label="减脂/减重" value="LOSE" />
                          <el-option label="增肌/增重" value="GAIN" />
                      </el-select>
                  </el-form-item>
              </el-col>
          </el-row>
          <el-row :gutter="20">
              <el-col :span="12">
                  <el-form-item label="运动频率">
                      <el-input v-model="historyForm.exerciseFrequency" type="number">
                          <template #append>次/周</template>
                      </el-input>
                  </el-form-item>
              </el-col>
              <el-col :span="12">
                  <el-form-item label="单次时长">
                      <el-input v-model="historyForm.exerciseDuration" type="number">
                          <template #append>分钟</template>
                      </el-input>
                  </el-form-item>
              </el-col>
          </el-row>

          <el-divider>慢病/过敏</el-divider>
          <el-form-item label="慢病">
              <el-select
                  v-model="historyForm.diseases"
                  multiple
                  filterable
                  allow-create
                  default-first-option
                  style="width: 100%"
              >
                  <el-option label="高血压" value="hypertension" />
                  <el-option label="二型糖尿病" value="diabetes_2" />
                  <el-option label="高尿酸/痛风" value="gout" />
                  <el-option label="高血脂" value="hyperlipidemia" />
                  <el-option label="冠心病" value="chd" />
              </el-select>
          </el-form-item>
          <el-form-item label="过敏">
              <el-select
                  v-model="historyForm.allergies"
                  multiple
                  filterable
                  allow-create
                  default-first-option
                  style="width: 100%"
              />
          </el-form-item>
          <el-form-item label="忌口">
              <el-input v-model="historyForm.otherRestrictions" type="textarea" />
          </el-form-item>

          <el-divider>体检指标</el-divider>
          <el-form-item label="体检日期">
              <el-date-picker v-model="historyForm.reportDate" type="date" format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
          </el-form-item>
          <el-row :gutter="20">
              <el-col :span="12">
                  <el-form-item label="收缩压">
                      <el-input v-model="historyForm.bpSystolic" type="number">
                          <template #append>mmHg</template>
                      </el-input>
                  </el-form-item>
              </el-col>
              <el-col :span="12">
                  <el-form-item label="舒张压">
                      <el-input v-model="historyForm.bpDiastolic" type="number">
                          <template #append>mmHg</template>
                      </el-input>
                  </el-form-item>
              </el-col>
              <el-col :span="12">
                  <el-form-item label="空腹血糖">
                      <el-input v-model="historyForm.fastingGlucose" type="number">
                          <template #append>mmol/L</template>
                      </el-input>
                  </el-form-item>
              </el-col>
              <el-col :span="12">
                  <el-form-item label="糖化血红蛋白">
                      <el-input v-model="historyForm.hba1c" type="number">
                          <template #append>%</template>
                      </el-input>
                  </el-form-item>
              </el-col>
              <el-col :span="12">
                  <el-form-item label="总胆固醇">
                      <el-input v-model="historyForm.totalCholesterol" type="number">
                          <template #append>mmol/L</template>
                      </el-input>
                  </el-form-item>
              </el-col>
              <el-col :span="12">
                  <el-form-item label="甘油三酯">
                      <el-input v-model="historyForm.triglycerides" type="number">
                          <template #append>mmol/L</template>
                      </el-input>
                  </el-form-item>
              </el-col>
              <el-col :span="12">
                  <el-form-item label="HDL-C">
                      <el-input v-model="historyForm.hdl" type="number">
                          <template #append>mmol/L</template>
                      </el-input>
                  </el-form-item>
              </el-col>
              <el-col :span="12">
                  <el-form-item label="LDL-C">
                      <el-input v-model="historyForm.ldl" type="number">
                          <template #append>mmol/L</template>
                      </el-input>
                  </el-form-item>
              </el-col>
              <el-col :span="12">
                  <el-form-item label="尿酸">
                      <el-input v-model="historyForm.uricAcid" type="number">
                          <template #append>µmol/L</template>
                      </el-input>
                  </el-form-item>
              </el-col>
              <el-col :span="12">
                  <el-form-item label="ALT">
                      <el-input v-model="historyForm.alt" type="number">
                          <template #append>U/L</template>
                      </el-input>
                  </el-form-item>
              </el-col>
              <el-col :span="12">
                  <el-form-item label="AST">
                      <el-input v-model="historyForm.ast" type="number">
                          <template #append>U/L</template>
                      </el-input>
                  </el-form-item>
              </el-col>
              <el-col :span="12">
                  <el-form-item label="肌酐">
                      <el-input v-model="historyForm.creatinine" type="number">
                          <template #append>µmol/L</template>
                      </el-input>
                  </el-form-item>
              </el-col>
              <el-col :span="12">
                  <el-form-item label="尿素氮">
                      <el-input v-model="historyForm.bun" type="number">
                          <template #append>mmol/L</template>
                      </el-input>
                  </el-form-item>
              </el-col>
          </el-row>
      </el-form>
      <template #footer>
          <el-button @click="historyDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="historySaving" @click="saveHistory">保存修改</el-button>
      </template>
  </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, reactive, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
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
const historyLoading = ref(false)
const historyDialogVisible = ref(false)
const historySaving = ref(false)
const historyList = ref<any[]>([])
const activeHistoryId = ref<number | null>(null)
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
    reportDate: '',
    activityLevel: '',
    goal: '',
    exerciseFrequency: '',
    exerciseDuration: '',
    bpSystolic: '',
    bpDiastolic: '',
    fastingGlucose: '',
    hba1c: '',
    totalCholesterol: '',
    triglycerides: '',
    hdl: '',
    ldl: '',
    uricAcid: '',
    alt: '',
    ast: '',
    creatinine: '',
    bun: '',
    diseases: [] as string[],
    allergies: [] as string[],
    otherRestrictions: ''
})

const historyForm = reactive({
    name: '',
    gender: 'MALE',
    age: 0,
    height: '',
    weight: '',
    bmi: '',
    reportDate: '',
    activityLevel: '',
    goal: '',
    exerciseFrequency: '',
    exerciseDuration: '',
    bpSystolic: '',
    bpDiastolic: '',
    fastingGlucose: '',
    hba1c: '',
    totalCholesterol: '',
    triglycerides: '',
    hdl: '',
    ldl: '',
    uricAcid: '',
    alt: '',
    ast: '',
    creatinine: '',
    bun: '',
    diseases: [] as string[],
    allergies: [] as string[],
    otherRestrictions: ''
})

const glucoseRange = ref<[string, string] | null>(null)
const glucoseLoading = ref(false)
const glucoseSubmitting = ref(false)
const glucoseRecords = ref<any[]>([])
const glucoseChartRef = ref<HTMLElement | null>(null)
let glucoseChart: echarts.ECharts | null = null

const glucoseForm = reactive({
    recordedAt: dayjs().format('YYYY-MM-DD HH:mm:ss'),
    measureType: 'FASTING',
    glucoseValue: '',
    eventType: '',
    medicationNote: '',
    symptoms: '',
    relatedMeal: ''
})

const glucoseSummary = reactive({
    avgGlucose: null as number | null,
    fastingAvg: null as number | null,
    postMealAvg: null as number | null,
    beforeSleepAvg: null as number | null,
    randomAvg: null as number | null,
    latestGlucose: null as number | null,
    highCount: 0,
    lowCount: 0,
    normalCount: 0,
    totalCount: 0
})

const glucoseAnalysis = reactive({
    riskLevel: 'LOW',
    alerts: [] as string[],
    mealImpacts: [] as any[]
})
const glucoseTasks = ref<any[]>([])
const glucoseTaskLoading = ref(false)
const glucoseTaskGenerating = ref(false)

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
            form.reportDate = data.reportDate || ''
            form.activityLevel = data.activityLevel || ''
            form.goal = data.goal || ''
            form.exerciseFrequency = data.exerciseFrequency ?? ''
            form.exerciseDuration = data.exerciseDuration ?? ''
            form.bpSystolic = data.bpSystolic ?? ''
            form.bpDiastolic = data.bpDiastolic ?? ''
            form.fastingGlucose = data.fastingGlucose ?? ''
            form.hba1c = data.hba1c ?? ''
            form.totalCholesterol = data.totalCholesterol ?? ''
            form.triglycerides = data.triglycerides ?? ''
            form.hdl = data.hdl ?? ''
            form.ldl = data.ldl ?? ''
            form.uricAcid = data.uricAcid ?? ''
            form.alt = data.alt ?? ''
            form.ast = data.ast ?? ''
            form.creatinine = data.creatinine ?? ''
            form.bun = data.bun ?? ''
            form.diseases = data.diseases || []
            form.allergies = data.allergies || []
            form.otherRestrictions = data.otherRestrictions || ''
        }
    } catch (e) {
        console.error(e)
    }
}

const buildProfilePayload = () => {
    return buildPayload(form)
}

const buildPayload = (source: typeof form | typeof historyForm) => {
    return {
        name: source.name,
        gender: source.gender,
        age: source.age,
        height: toNumberOrNull(source.height),
        weight: toNumberOrNull(source.weight),
        bmi: source.bmi,
        reportDate: source.reportDate || null,
        activityLevel: source.activityLevel || null,
        goal: source.goal || null,
        exerciseFrequency: toNumberOrNull(source.exerciseFrequency as any),
        exerciseDuration: toNumberOrNull(source.exerciseDuration as any),
        bpSystolic: toNumberOrNull(source.bpSystolic),
        bpDiastolic: toNumberOrNull(source.bpDiastolic),
        fastingGlucose: toNumberOrNull(source.fastingGlucose),
        hba1c: toNumberOrNull(source.hba1c),
        totalCholesterol: toNumberOrNull(source.totalCholesterol),
        triglycerides: toNumberOrNull(source.triglycerides),
        hdl: toNumberOrNull(source.hdl),
        ldl: toNumberOrNull(source.ldl),
        uricAcid: toNumberOrNull(source.uricAcid),
        alt: toNumberOrNull(source.alt),
        ast: toNumberOrNull(source.ast),
        creatinine: toNumberOrNull(source.creatinine),
        bun: toNumberOrNull(source.bun),
        diseases: source.diseases,
        allergies: source.allergies,
        otherRestrictions: source.otherRestrictions
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
        heart_rate: ''
    } as Record<string, string | number>
})

const formatDate = (row: any) => {
    return dayjs(row.recordedAt).format('YYYY-MM-DD HH:mm')
}

const toNumberOrNull = (value: string | number | null | undefined) => {
    if (value === null || value === undefined || value === '') return null
    const num = Number(value)
    return Number.isFinite(num) ? num : null
}

const riskTips = computed(() => {
    const tips: string[] = []
    const gender = form.gender || 'MALE'
    const systolic = toNumberOrNull(form.bpSystolic)
    const diastolic = toNumberOrNull(form.bpDiastolic)
    const fastingGlucose = toNumberOrNull(form.fastingGlucose)
    const hba1c = toNumberOrNull(form.hba1c)
    const totalCholesterol = toNumberOrNull(form.totalCholesterol)
    const triglycerides = toNumberOrNull(form.triglycerides)
    const hdl = toNumberOrNull(form.hdl)
    const ldl = toNumberOrNull(form.ldl)
    const uricAcid = toNumberOrNull(form.uricAcid)
    const alt = toNumberOrNull(form.alt)
    const ast = toNumberOrNull(form.ast)
    const creatinine = toNumberOrNull(form.creatinine)
    const bun = toNumberOrNull(form.bun)

    if (systolic !== null && (systolic < 90 || systolic >= 140)) {
        tips.push(`收缩压异常：${systolic} mmHg`)
    }
    if (diastolic !== null && (diastolic < 60 || diastolic >= 90)) {
        tips.push(`舒张压异常：${diastolic} mmHg`)
    }
    if (fastingGlucose !== null && (fastingGlucose < 3.9 || fastingGlucose >= 6.1)) {
        tips.push(`空腹血糖异常：${fastingGlucose} mmol/L`)
    }
    if (hba1c !== null && (hba1c < 4.0 || hba1c >= 6.5)) {
        tips.push(`糖化血红蛋白异常：${hba1c}%`)
    }
    if (totalCholesterol !== null && totalCholesterol >= 5.2) {
        tips.push(`总胆固醇偏高：${totalCholesterol} mmol/L`)
    }
    if (triglycerides !== null && triglycerides >= 1.7) {
        tips.push(`甘油三酯偏高：${triglycerides} mmol/L`)
    }
    if (hdl !== null) {
        const minHdl = gender === 'FEMALE' ? 1.3 : 1.0
        if (hdl < minHdl) {
            tips.push(`HDL-C 偏低：${hdl} mmol/L`)
        }
    }
    if (ldl !== null && ldl >= 3.4) {
        tips.push(`LDL-C 偏高：${ldl} mmol/L`)
    }
    if (uricAcid !== null) {
        const minUric = gender === 'FEMALE' ? 143 : 202
        const maxUric = gender === 'FEMALE' ? 357 : 416
        if (uricAcid < minUric || uricAcid > maxUric) {
            tips.push(`尿酸异常：${uricAcid} µmol/L`)
        }
    }
    if (alt !== null && (alt < 7 || alt > 40)) {
        tips.push(`ALT 异常：${alt} U/L`)
    }
    if (ast !== null && (ast < 13 || ast > 35)) {
        tips.push(`AST 异常：${ast} U/L`)
    }
    if (creatinine !== null) {
        const minCr = gender === 'FEMALE' ? 53 : 62
        const maxCr = gender === 'FEMALE' ? 97 : 115
        if (creatinine < minCr || creatinine > maxCr) {
            tips.push(`肌酐异常：${creatinine} µmol/L`)
        }
    }
    if (bun !== null && (bun < 2.9 || bun > 7.5)) {
        tips.push(`尿素氮异常：${bun} mmol/L`)
    }
    return tips
})

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
        batchForm.values.heart_rate = latest.heartRate || ''
    } else {
        // Defaults
        batchForm.values.weight = 60
        batchForm.values.systolic = 120
        batchForm.values.diastolic = 80
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

const displayNum = (val: number | null | undefined) => {
    if (val === null || val === undefined) return '--'
    return `${Number(val).toFixed(2)} mmol/L`
}

const glucoseMeasureLabel = (type: string) => {
    const map: Record<string, string> = {
        FASTING: '空腹',
        POST_MEAL_2H: '餐后2小时',
        BEFORE_SLEEP: '睡前',
        RANDOM: '随机'
    }
    return map[type] || type || '--'
}

const glucoseEventLabel = (type: string) => {
    const map: Record<string, string> = {
        NORMAL: '正常',
        HIGH: '偏高',
        LOW: '偏低'
    }
    return map[type] || '正常'
}

const glucoseEventTagType = (type: string) => {
    if (type === 'HIGH') return 'warning'
    if (type === 'LOW') return 'danger'
    return 'success'
}

const glucoseRiskLabel = (riskLevel: string) => {
    const map: Record<string, string> = {
        LOW: '低',
        MEDIUM: '中',
        HIGH: '高'
    }
    return map[riskLevel] || riskLevel || '--'
}

const applyGlucosePreset = (preset: 'day' | '3d' | 'week' | 'month' | 'all') => {
    if (preset === 'all') {
        glucoseRange.value = null
        fetchGlucoseRecords()
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
    glucoseRange.value = [start.format('YYYY-MM-DD'), end.format('YYYY-MM-DD')]
    fetchGlucoseRecords()
}

const fetchGlucoseRecords = async () => {
    if (!userStore.user?.id) return
    glucoseLoading.value = true
    try {
        const params: any = { userId: userStore.user.id }
        if (glucoseRange.value) {
            params.start = `${glucoseRange.value[0]} 00:00:00`
            params.end = `${glucoseRange.value[1]} 23:59:59`
        }
        const res: any = await request.get('/health/glucose-records', { params })
        if (res.code === 200) {
            glucoseRecords.value = Array.isArray(res.data) ? res.data : []
            renderGlucoseChart()
        }
        await fetchGlucoseSummary()
        await fetchGlucoseAnalysis()
        await fetchGlucoseTasks()
    } catch (e) {
        console.error(e)
    } finally {
        glucoseLoading.value = false
    }
}

const fetchGlucoseSummary = async () => {
    if (!userStore.user?.id) return
    try {
        const params: any = { userId: userStore.user.id }
        if (glucoseRange.value) {
            params.start = `${glucoseRange.value[0]} 00:00:00`
            params.end = `${glucoseRange.value[1]} 23:59:59`
        }
        const res: any = await request.get('/health/glucose-records/summary', { params })
        if (res.code === 200 && res.data) {
            Object.assign(glucoseSummary, res.data)
        }
    } catch (e) {
        console.error(e)
    }
}

const fetchGlucoseTasks = async () => {
    if (!userStore.user?.id) return
    glucoseTaskLoading.value = true
    try {
        const res: any = await request.get('/health/glucose-tasks', {
            params: { userId: userStore.user.id }
        })
        if (res.code === 200) {
            glucoseTasks.value = Array.isArray(res.data) ? res.data : []
        }
    } catch (e) {
        console.error(e)
    } finally {
        glucoseTaskLoading.value = false
    }
}

const generateGlucoseTasks = async () => {
    if (!userStore.user?.id) return
    glucoseTaskGenerating.value = true
    try {
        const res: any = await request.post('/health/glucose-tasks/auto-generate', null, {
            params: { userId: userStore.user.id }
        })
        if (res.code === 200) {
            const count = Array.isArray(res.data) ? res.data.length : 0
            ElMessage.success(count > 0 ? `已新增 ${count} 条任务` : '当前暂无新任务')
            fetchGlucoseTasks()
        }
    } catch (e) {
        console.error(e)
    } finally {
        glucoseTaskGenerating.value = false
    }
}

const completeGlucoseTask = async (id: number) => {
    try {
        const res: any = await request.put(`/health/glucose-tasks/${id}/complete`)
        if (res.code === 200) {
            ElMessage.success('任务已完成')
            fetchGlucoseTasks()
        }
    } catch (e) {
        console.error(e)
    }
}

const deleteGlucoseTask = async (id: number) => {
    try {
        const res: any = await request.delete(`/health/glucose-tasks/${id}`)
        if (res.code === 200) {
            ElMessage.success('删除成功')
            fetchGlucoseTasks()
        }
    } catch (e) {
        console.error(e)
    }
}

const formatTaskDate = (value: string) => {
    if (!value) return '--'
    return dayjs(value).format('YYYY-MM-DD HH:mm')
}

const fetchGlucoseAnalysis = async () => {
    if (!userStore.user?.id) return
    try {
        const params: any = { userId: userStore.user.id }
        if (glucoseRange.value) {
            params.start = `${glucoseRange.value[0]} 00:00:00`
            params.end = `${glucoseRange.value[1]} 23:59:59`
        }
        const res: any = await request.get('/health/glucose-records/analysis', { params })
        if (res.code === 200 && res.data) {
            glucoseAnalysis.riskLevel = res.data.riskLevel || 'LOW'
            glucoseAnalysis.alerts = Array.isArray(res.data.alerts) ? res.data.alerts : []
            glucoseAnalysis.mealImpacts = Array.isArray(res.data.mealImpacts) ? res.data.mealImpacts : []
        }
    } catch (e) {
        console.error(e)
    }
}

const submitGlucoseRecord = async () => {
    if (!userStore.user?.id) return
    const glucoseValue = Number(glucoseForm.glucoseValue)
    if (!Number.isFinite(glucoseValue) || glucoseValue <= 0) {
        ElMessage.warning('请输入有效血糖值')
        return
    }
    glucoseSubmitting.value = true
    try {
        const payload = {
            userId: userStore.user.id,
            glucoseValue,
            measureType: glucoseForm.measureType,
            eventType: glucoseForm.eventType || null,
            medicationNote: glucoseForm.medicationNote || null,
            symptoms: glucoseForm.symptoms || null,
            relatedMeal: glucoseForm.relatedMeal || null,
            recordedAt: glucoseForm.recordedAt || dayjs().format('YYYY-MM-DD HH:mm:ss')
        }
        const res: any = await request.post('/health/glucose-records', payload)
        if (res.code === 200) {
            ElMessage.success('血糖记录已保存')
            glucoseForm.glucoseValue = ''
            glucoseForm.eventType = ''
            glucoseForm.medicationNote = ''
            glucoseForm.symptoms = ''
            glucoseForm.relatedMeal = ''
            glucoseForm.recordedAt = dayjs().format('YYYY-MM-DD HH:mm:ss')
            fetchGlucoseRecords()
        } else {
            ElMessage.error(res.msg || '保存失败')
        }
    } catch (e) {
        console.error(e)
    } finally {
        glucoseSubmitting.value = false
    }
}

const deleteGlucoseRecord = async (id: number) => {
    try {
        await request.delete(`/health/glucose-records/${id}`)
        ElMessage.success('删除成功')
        fetchGlucoseRecords()
    } catch (e) {
        console.error(e)
    }
}

const renderGlucoseChart = async () => {
    if (activeTab.value !== 'glucose') return
    await nextTick()
    if (!glucoseChartRef.value) return
    if (!glucoseChart) {
        glucoseChart = echarts.init(glucoseChartRef.value)
    }
    const sorted = [...glucoseRecords.value].sort((a, b) => {
        const ta = dayjs(a.recordedAt).valueOf()
        const tb = dayjs(b.recordedAt).valueOf()
        return ta - tb
    })
    const xAxis = sorted.map((item) => dayjs(item.recordedAt).format('MM-DD HH:mm'))
    const values = sorted.map((item) => item.glucoseValue)
    glucoseChart.setOption({
        tooltip: { trigger: 'axis' },
        grid: { left: 30, right: 20, top: 20, bottom: 50 },
        xAxis: {
            type: 'category',
            data: xAxis,
            axisLabel: { rotate: 30 }
        },
        yAxis: {
            type: 'value',
            name: 'mmol/L'
        },
        series: [
            {
                name: '血糖',
                type: 'line',
                smooth: true,
                data: values,
                itemStyle: { color: '#16a34a' },
                lineStyle: { width: 2 }
            }
        ]
    })
}

onMounted(() => {
    fetchProfile()
    loadLongTermData()
    fetchMetrics()
    fetchHistory()
    applyGlucosePreset('week')
})

onUnmounted(() => {
    if (glucoseChart) {
        glucoseChart.dispose()
        glucoseChart = null
    }
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

watch(
    () => activeTab.value,
    (tab) => {
        if (tab === 'history') {
            fetchHistory()
        }
    }
)

watch(
    () => activeTab.value,
    (tab) => {
        if (tab === 'glucose') {
            renderGlucoseChart()
            fetchGlucoseTasks()
        }
    }
)

const fetchHistory = async () => {
    historyLoading.value = true
    try {
        const res: any = await request.get('/health/profile/history')
        if (res.code === 200) {
            historyList.value = res.data || []
        }
    } catch (e) {
        console.error(e)
    } finally {
        historyLoading.value = false
    }
}

const openHistory = async (id: number) => {
    try {
        const res: any = await request.get(`/health/profile/history/${id}`)
        if (res.code === 200 && res.data) {
            fillHistoryForm(res.data)
            activeHistoryId.value = id
            historyDialogVisible.value = true
        }
    } catch (e) {
        console.error(e)
    }
}

const applyHistory = async (id: number) => {
    try {
        const res: any = await request.get(`/health/profile/history/${id}`)
        if (res.code === 200 && res.data) {
            fillCurrentForm(res.data)
            await submitProfile()
            ElMessage.success('已应用到当前档案')
        }
    } catch (e) {
        console.error(e)
    }
}

const saveHistory = async () => {
    if (!activeHistoryId.value) return
    historySaving.value = true
    try {
        const res: any = await request.put(`/health/profile/history/${activeHistoryId.value}`, buildPayload(historyForm))
        if (res.code === 200) {
            ElMessage.success('历史记录已更新')
            historyDialogVisible.value = false
            fetchHistory()
        } else {
            ElMessage.error(res.msg || '更新失败')
        }
    } catch (e) {
        console.error(e)
    } finally {
        historySaving.value = false
    }
}

const fillHistoryForm = (data: any) => {
    historyForm.name = data.name || ''
    historyForm.gender = data.gender || 'MALE'
    historyForm.age = data.age || 0
    historyForm.height = data.height ?? ''
    historyForm.weight = data.weight ?? ''
    historyForm.bmi = data.bmi || ''
    historyForm.reportDate = data.reportDate || ''
    historyForm.activityLevel = data.activityLevel || ''
    historyForm.goal = data.goal || ''
    historyForm.exerciseFrequency = data.exerciseFrequency ?? ''
    historyForm.exerciseDuration = data.exerciseDuration ?? ''
    historyForm.bpSystolic = data.bpSystolic ?? ''
    historyForm.bpDiastolic = data.bpDiastolic ?? ''
    historyForm.fastingGlucose = data.fastingGlucose ?? ''
    historyForm.hba1c = data.hba1c ?? ''
    historyForm.totalCholesterol = data.totalCholesterol ?? ''
    historyForm.triglycerides = data.triglycerides ?? ''
    historyForm.hdl = data.hdl ?? ''
    historyForm.ldl = data.ldl ?? ''
    historyForm.uricAcid = data.uricAcid ?? ''
    historyForm.alt = data.alt ?? ''
    historyForm.ast = data.ast ?? ''
    historyForm.creatinine = data.creatinine ?? ''
    historyForm.bun = data.bun ?? ''
    historyForm.diseases = data.diseases || []
    historyForm.allergies = data.allergies || []
    historyForm.otherRestrictions = data.otherRestrictions || ''
}

const fillCurrentForm = (data: any) => {
    form.name = data.name || ''
    form.gender = data.gender || 'MALE'
    form.age = data.age || 0
    form.height = data.height || 0
    form.weight = data.weight || 0
    form.bmi = data.bmi || ''
    form.reportDate = data.reportDate || ''
    form.activityLevel = data.activityLevel || ''
    form.goal = data.goal || ''
    form.exerciseFrequency = data.exerciseFrequency ?? ''
    form.exerciseDuration = data.exerciseDuration ?? ''
    form.bpSystolic = data.bpSystolic ?? ''
    form.bpDiastolic = data.bpDiastolic ?? ''
    form.fastingGlucose = data.fastingGlucose ?? ''
    form.hba1c = data.hba1c ?? ''
    form.totalCholesterol = data.totalCholesterol ?? ''
    form.triglycerides = data.triglycerides ?? ''
    form.hdl = data.hdl ?? ''
    form.ldl = data.ldl ?? ''
    form.uricAcid = data.uricAcid ?? ''
    form.alt = data.alt ?? ''
    form.ast = data.ast ?? ''
    form.creatinine = data.creatinine ?? ''
    form.bun = data.bun ?? ''
    form.diseases = data.diseases || []
    form.allergies = data.allergies || []
    form.otherRestrictions = data.otherRestrictions || ''
}

const formatHistoryDate = (val: string) => {
    if (!val) return '--'
    return dayjs(val).format('YYYY-MM-DD HH:mm')
}
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

.exam-step {
    padding: 10px 0 20px;
    .risk-alert {
        margin-top: 10px;
    }
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

.glucose-container {
    padding: 20px;
    .list-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12px;
        h3 { margin: 0; }
        .list-actions {
            display: flex;
            gap: 8px;
            flex-wrap: wrap;
            align-items: center;
        }
    }
}

.glucose-form-card {
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    padding: 16px;
}

.glucose-summary {
    margin: 10px 0 20px;
    .summary-title {
        color: #6b7280;
        font-size: 13px;
    }
    .summary-value {
        margin-top: 6px;
        font-size: 22px;
        font-weight: 600;
        color: #1f2937;
    }
    .summary-value.warning {
        color: #b45309;
    }
    .summary-value.danger {
        color: #b91c1c;
    }
}

.glucose-chart-card {
    margin-bottom: 20px;
}

.glucose-chart {
    width: 100%;
    height: 320px;
}

.glucose-impact-card {
    margin-top: 20px;
}

.analysis-alerts {
    margin: 8px 0 0 18px;
    padding: 0;
    li {
        margin: 4px 0;
    }
}

.history-container {
    padding: 20px;
    .list-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 12px;
        h3 { margin: 0; }
    }
}
</style>
