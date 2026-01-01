<template>
  <div class="diet-log">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>模糊饮食记录</span>
        </div>
      </template>
      
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
            <el-select v-model="form.foodId" placeholder="请搜索或选择食物" filterable style="width: 100%; max-width: 400px;">
                <el-option-group label="常见主食">
                    <el-option label="米饭" :value="101" />
                    <el-option label="馒头" :value="104" />
                </el-option-group>
                <el-option-group label="肉蛋奶">
                    <el-option label="鸡胸肉" :value="102" />
                    <el-option label="鸡蛋" :value="105" />
                    <el-option label="牛奶" :value="106" />
                </el-option-group>
                <el-option-group label="蔬果">
                    <el-option label="西兰花" :value="103" />
                    <el-option label="苹果" :value="107" />
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
                    @click="form.unit = unit.value"
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

        <el-form-item>
            <el-button type="primary" @click="submitLog" size="large" icon="Check">记录饮食</el-button>
        </el-form-item>
      </el-form>

      <div v-if="lastLog" class="log-preview">
        <el-alert title="记录成功" type="success" :description="lastLog" show-icon />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'

const form = reactive({
    mealType: 'LUNCH',
    foodId: null,
    unit: '',
    count: 1
})

const lastLog = ref('')

const units = [
    { value: 'FIST', label: '一拳', icon: '👊', desc: '~150g (主食/水果)' },
    { value: 'PALM', label: '一掌', icon: '✋', desc: '~120g (肉类)' },
    { value: 'BOWL', label: '一碗', icon: '🥣', desc: '~250ml (汤/粥)' }
]

const submitLog = async () => {
    if (!form.foodId || !form.unit) {
        ElMessage.warning('请选择食物和分量单位')
        return
    }
    
    console.log('正在提交:', form)
    lastLog.value = `已记录 ${form.mealType}: 食物ID ${form.foodId}, 分量 ${form.count} x ${form.unit}`
    ElMessage.success('记录成功，今日热量已更新')
}
</script>

<style scoped lang="scss">
.diet-log {
    /* Standardized in MainLayout */
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
</style>
