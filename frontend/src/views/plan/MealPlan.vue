<template>
  <div class="meal-plan">
    <el-card>
        <template #header>
            <div class="card-header">
            <span>智能食谱中心</span>
            </div>
        </template>
        
        <div class="action-area">
            <div class="intro">
                <el-icon class="intro-icon"><MagicStick /></el-icon>
                <p>基于您的健康档案（如：<strong>高血压</strong>、<strong>痛风</strong>），AI 营养师将为您生成专属的一周食谱。</p>
            </div>
            
            <div class="btn-group">
                <el-button type="primary" size="large" @click="generatePlan" :loading="loading" class="generate-btn">
                    生成本周推荐食谱
                </el-button>
                <el-button v-if="generatedPlan" type="warning" size="large" @click="downloadPdf" icon="Download">
                    导出食谱 PDF
                </el-button>
            </div>
        </div>

        <el-divider v-if="generatedPlan" />

        <div v-if="generatedPlan" class="plan-preview">
            <div class="plan-header">
                <h3><el-icon><Calendar /></el-icon> {{ generatedPlan.title }}</h3>
                <el-alert :title="generatedPlan.advice" type="success" :closable="false" effect="dark" />
            </div>
            
            <el-table :data="generatedPlan.weeklyPlan" border stripe style="margin-top: 20px">
                <el-table-column prop="day" label="日期" width="100" align="center" />
                <el-table-column label="早餐">
                    <template #default="scope">
                        <div class="meal-cell">
                            <span class="meal-name">{{ scope.row.breakfast.name }}</span>
                            <span class="meal-cal">{{ scope.row.breakfast.calories }}</span>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column label="午餐">
                    <template #default="scope">
                         <div class="meal-cell">
                            <span class="meal-name">{{ scope.row.lunch.name }}</span>
                            <span class="meal-cal">{{ scope.row.lunch.calories }}</span>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column label="晚餐">
                    <template #default="scope">
                         <div class="meal-cell">
                            <span class="meal-name">{{ scope.row.dinner.name }}</span>
                            <span class="meal-cal">{{ scope.row.dinner.calories }}</span>
                        </div>
                    </template>
                </el-table-column>
            </el-table>
            
            <div class="shopping-list">
                <h4><el-icon><ShoppingCart /></el-icon> 采购清单</h4>
                <div class="tags-container">
                    <el-tag v-for="(qty, item) in generatedPlan.shoppingList" :key="item" size="large" effect="plain" class="shop-tag">
                        {{ item }}: <strong>{{ qty }}</strong>
                    </el-tag>
                </div>
            </div>
        </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const loading = ref(false)
const generatedPlan = ref<any>(null) // Use 'any' for now or define interface

const generatePlan = async () => {
    loading.value = true
    setTimeout(() => {
        // Mock Data
        generatedPlan.value = {
            title: '高血压控制周食谱',
            advice: '本周食谱重点在于控制钠摄入(<5g/day)，增加钾的摄入。建议多喝水。',
            weeklyPlan: [
                { 
                    day: '周一', 
                    breakfast: { name: '燕麦粥 + 煮鸡蛋', calories: '350kcal' },
                    lunch: { name: '清蒸鱼 + 糙米饭 + 炒青菜', calories: '600kcal' },
                    dinner: { name: '蔬菜沙拉 (油醋汁)', calories: '200kcal' }
                },
                { 
                    day: '周二 Tue', 
                    breakfast: { name: '全麦面包 + 脱脂奶', calories: '350kcal' },
                    lunch: { name: '鸡胸肉 + 红薯 + 西兰花', calories: '550kcal' },
                    dinner: { name: '番茄鸡蛋汤 + 玉米段', calories: '250kcal' }
                },
                 { 
                    day: '周三 Wed', 
                    breakfast: { name: '小米粥 + 腐乳(少许)', calories: '300kcal' },
                    lunch: { name: '酱牛肉 + 荞麦面', calories: '650kcal' },
                    dinner: { name: '冬瓜丸子汤 (鸡肉丸)', calories: '250kcal' }
                }
            ],
            shoppingList: {
                '燕麦': '500g',
                '鸡胸肉': '1kg',
                '西兰花': '2颗',
                '全麦面包': '1袋',
                '脱脂奶': '1L'
            }
        }
        loading.value = false
    }, 1500)
}

const downloadPdf = () => {
    window.open('http://localhost:8080/api/export/pdf', '_blank')
}
</script>

<style scoped lang="scss">
.action-area {
    text-align: center;
    padding: 40px;
    background: linear-gradient(to bottom, #fdf6ec, #fff);
    border-radius: 12px;
    margin-bottom: 20px;
    
    .intro {
        margin-bottom: 24px;
        color: #666;
        .intro-icon {
            font-size: 48px;
            color: #F59E0B;
            margin-bottom: 12px;
        }
    }
    
    .generate-btn {
        padding: 12px 40px;
        font-size: 16px;
    }
}

.plan-preview {
    animation: fadeIn 0.5s ease;
    
    .plan-header {
        margin-bottom: 20px;
        h3 {
             display: flex;
             align-items: center;
             gap: 10px;
             color: #333;
        }
    }
    
    .meal-cell {
        display: flex;
        flex-direction: column;
        .meal-name {
            font-weight: bold;
            color: #333;
        }
        .meal-cal {
            font-size: 12px;
            color: #999;
        }
    }
}

.shopping-list {
    margin-top: 30px;
    padding: 24px;
    background: #f0f9eb;
    border-radius: 12px;
    border: 1px solid #e1f3d8;
    
    h4 {
         display: flex;
         align-items: center;
         gap: 10px;
         color: #67C23A;
         margin-top: 0;
    }
    
    .tags-container {
        display: flex;
        flex-wrap: wrap;
        gap: 12px;
        
        .shop-tag {
            background-color: #fff;
        }
    }
}

@keyframes fadeIn {
    from { opacity: 0; transform: translateY(10px); }
    to { opacity: 1; transform: translateY(0); }
}
</style>
