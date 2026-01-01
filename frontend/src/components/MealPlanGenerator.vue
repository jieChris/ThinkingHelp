<template>
  <div class="meal-plan-generator">
    <h3>智能食谱生成 (Start AI Meal Plan)</h3>
    
    <div class="action-area">
        <p>基于您的健康档案（如：高血压、痛风），AI 将为您生成专属的一周食谱。</p>
        <el-button type="success" size="large" @click="generatePlan" :loading="loading">
            生成本周食谱 (Generate)
        </el-button>
        <el-button v-if="generatedPlan" type="warning" size="large" @click="downloadPdf">
            导出 PDF (Export)
        </el-button>
    </div>

    <div v-if="generatedPlan" class="plan-preview">
        <h4>{{ generatedPlan.title }}</h4>
        <el-alert :title="generatedPlan.advice" type="info" :closable="false" />
        
        <el-table :data="generatedPlan.weeklyPlan" border style="margin-top: 20px">
            <el-table-column prop="day" label="日期" width="100" />
            <el-table-column label="早餐">
                <template #default="scope">
                    <b>{{ scope.row.breakfast.name }}</b>
                </template>
            </el-table-column>
            <el-table-column label="午餐">
                <template #default="scope">
                    <b>{{ scope.row.lunch.name }}</b>
                </template>
            </el-table-column>
             <el-table-column label="晚餐">
                <template #default="scope">
                    <b>{{ scope.row.dinner.name }}</b>
                </template>
            </el-table-column>
        </el-table>
        
        <div class="shopping-list">
            <h4>采购清单 (Shopping List)</h4>
            <el-tag v-for="(qty, item) in generatedPlan.shoppingList" :key="item" style="margin: 5px">
                {{ item }}: {{ qty }}
            </el-tag>
        </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const loading = ref(false)
const generatedPlan = ref(null)

const generatePlan = async () => {
    loading.value = true
    setTimeout(() => {
        // 模拟数据
        generatedPlan.value = {
            title: '高血压控制周食谱',
            advice: '本周食谱重点在于控制钠摄入，增加钾的摄入。',
            weeklyPlan: [
                { 
                    day: 'Mon', 
                    breakfast: { name: '燕麦粥', calories: '300kcal' },
                    lunch: { name: '清蒸鱼 + 糙米饭', calories: '600kcal' },
                    dinner: { name: '蔬菜沙拉', calories: '200kcal' }
                },
                { 
                    day: 'Tue', 
                    breakfast: { name: '全麦面包 + 牛奶', calories: '350kcal' },
                    lunch: { name: '鸡胸肉 + 红薯', calories: '550kcal' },
                    dinner: { name: '番茄鸡蛋汤', calories: '250kcal' }
                }
            ],
            shoppingList: {
                '燕麦': '500g',
                '鸡胸肉': '1kg',
                '西兰花': '2颗'
            }
        }
        loading.value = false
    }, 2000)
}

const downloadPdf = () => {
    window.open('http://localhost:8080/api/export/pdf', '_blank')
}
</script>

<style scoped>
.action-area {
    text-align: center;
    padding: 30px;
    background: #fdf6ec;
    border-radius: 8px;
    margin-bottom: 20px;
}
.shopping-list {
    margin-top: 20px;
    padding: 20px;
    background: #f0f9eb;
    border-radius: 8px;
}
</style>
