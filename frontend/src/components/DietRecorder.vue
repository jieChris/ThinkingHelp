<template>
  <div class="diet-recorder">
    <h3>饮食记录 (Diet Recorder)</h3>
    
    <el-form label-width="100px">
        <el-form-item label="食物 (Food)">
            <el-select v-model="form.foodId" placeholder="选择食物" style="width: 100%">
                <!-- 模拟数据，应替换为 API 调用 -->
                <el-option label="米饭 (Rice)" :value="101" />
                <el-option label="鸡胸肉 (Chicken)" :value="102" />
                <el-option label="西兰花 (Broccoli)" :value="103" />
            </el-select>
        </el-form-item>

        <el-form-item label="分量 (Unit)">
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
                </div>
            </div>
        </el-form-item>

        <el-form-item label="数量 (Count)">
             <el-input-number v-model="form.count" :min="1" :max="10" />
        </el-form-item>

        <el-form-item>
            <el-button type="primary" @click="submitLog">记录 (Log)</el-button>
        </el-form-item>
    </el-form>

    <div v-if="lastLog" class="log-preview">
        <el-alert title="记录成功" type="success" :description="lastLog" show-icon />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'

const form = reactive({
    foodId: null,
    unit: '',
    count: 1
})

const lastLog = ref('')

const units = [
    { value: 'FIST', label: '拳头 (Fist)', icon: '👊' },
    { value: 'PALM', label: '手掌 (Palm)', icon: '✋' },
    { value: 'BOWL', label: '碗 (Bowl)', icon: '🥣' }
]

const submitLog = async () => {
    if (!form.foodId || !form.unit) {
        ElMessage.warning('请完整填写')
        return
    }
    // TODO: 调用 API
    // await api.post('/diet/log', form)
    
    console.log('正在提交:', form)
    lastLog.value = `已记录: 食物ID ${form.foodId}, 分量 ${form.count} x ${form.unit}`
    ElMessage.success('记录成功')
}
</script>

<style scoped>
.fuzzy-selector {
    display: flex;
    gap: 15px;
}
.unit-card {
    border: 2px solid #ddd;
    border-radius: 8px;
    padding: 10px;
    cursor: pointer;
    text-align: center;
    width: 80px;
    transition: all 0.3s;
}
.unit-card:hover {
    border-color: #a0cfff;
}
.unit-card.active {
    border-color: #409EFF;
    background-color: #ecf5ff;
}
.icon {
    font-size: 24px;
    margin-bottom: 5px;
}
</style>
