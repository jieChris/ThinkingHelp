<template>
  <div class="ai-config-page">
    <div class="header">
      <h2>AI 配置</h2>
      <el-button type="primary" @click="fetchConfigs" :loading="loading">刷新</el-button>
    </div>

    <el-row :gutter="20">
      <el-col :span="12" v-for="item in configList" :key="item.configKey">
        <el-card class="config-card">
          <template #header>
            <div class="card-header">
              <span>{{ keyLabel(item.configKey) }}</span>
              <el-switch v-model="item.enabled" active-text="启用" inactive-text="停用" />
            </div>
          </template>
          <el-form label-width="110px">
            <el-form-item label="API Key">
              <el-input v-model="item.apiKey" type="password" show-password placeholder="请输入 API Key" />
            </el-form-item>
            <el-form-item label="Base URL">
              <el-input v-model="item.baseUrl" placeholder="https://dashscope.aliyuncs.com/compatible-mode" />
            </el-form-item>
            <el-form-item label="模型">
              <el-input v-model="item.model" placeholder="qwen-plus / qwen-max / qwen2.5" />
            </el-form-item>
            <el-form-item label="温度">
              <el-input-number v-model="item.temperature" :min="0" :max="2" :step="0.1" />
            </el-form-item>
            <el-form-item label="MaxTokens">
              <el-input-number v-model="item.maxTokens" :min="16" :max="8192" :step="32" />
            </el-form-item>
            <el-form-item>
              <el-button type="success" @click="saveConfig(item)" :loading="savingKey === item.configKey">
                保存配置
              </el-button>
              <el-button type="primary" plain @click="testConfig(item)" :loading="testingKey === item.configKey">
                测试
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import request from '../../api/request'
import { ElMessage } from 'element-plus'

interface AiConfig {
  configKey: string
  apiKey: string
  baseUrl: string
  model: string
  temperature: number | null
  maxTokens: number | null
  enabled: number | boolean
}

const loading = ref(false)
const savingKey = ref('')
const testingKey = ref('')
const configList = ref<AiConfig[]>([])

const defaults: AiConfig[] = [
  {
    configKey: 'MEDICAL',
    apiKey: '',
    baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode',
    model: 'qwen-plus',
    temperature: 0.7,
    maxTokens: 2048,
    enabled: 1
  },
  {
    configKey: 'MEAL_PLAN',
    apiKey: '',
    baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode',
    model: 'qwen-plus',
    temperature: 0.7,
    maxTokens: 2048,
    enabled: 1
  }
]

const fetchConfigs = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/admin/ai-config')
    if (res.code === 200) {
      const data = res.data || []
      const map = new Map<string, AiConfig>()
      data.forEach((item: any) => {
        map.set(item.configKey, {
          configKey: item.configKey,
          apiKey: item.apiKey || '',
          baseUrl: item.baseUrl || '',
          model: item.model || '',
          temperature: item.temperature ?? null,
          maxTokens: item.maxTokens ?? null,
          enabled: item.enabled ?? 1
        })
      })
      configList.value = defaults.map((def) => map.get(def.configKey) || { ...def })
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const saveConfig = async (item: AiConfig) => {
  savingKey.value = item.configKey
  try {
    const payload = {
      apiKey: item.apiKey || null,
      baseUrl: item.baseUrl || null,
      model: item.model || null,
      temperature: item.temperature ?? null,
      maxTokens: item.maxTokens ?? null,
      enabled: item.enabled ? 1 : 0
    }
    const res: any = await request.put(`/admin/ai-config/${item.configKey}`, payload)
    if (res.code === 200) {
      ElMessage.success('配置已保存')
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (e) {
    console.error(e)
  } finally {
    savingKey.value = ''
  }
}

const testConfig = async (item: AiConfig) => {
  testingKey.value = item.configKey
  try {
    const res: any = await request.post(`/admin/ai-config/${item.configKey}/test`)
    if (res.code === 200) {
      const text = (res.data || '').toString()
      ElMessage.success(text.slice(0, 60) || '测试成功')
    } else {
      ElMessage.error(res.message || '测试失败')
    }
  } catch (e) {
    console.error(e)
  } finally {
    testingKey.value = ''
  }
}

const keyLabel = (key: string) => {
  return key === 'MEDICAL' ? '医疗建议模型' : '食谱/饮食模型'
}

onMounted(() => {
  fetchConfigs()
})
</script>

<style scoped lang="scss">
.ai-config-page {
  padding: 20px;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.config-card {
  margin-bottom: 20px;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}
</style>
