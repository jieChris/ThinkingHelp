<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8"/>
    <title>${plan.title}</title>
    <style>
        body { font-family: 'CJK', 'SimHei', 'Microsoft YaHei', 'PingFang SC', sans-serif; }
        h1 { color: #2c3e50; text-align: center; }
        .advice { background: #ecf0f1; padding: 10px; border-radius: 5px; margin-bottom: 20px; }
        .targets { margin-bottom: 20px; }
        .target-tag { display: inline-block; background: #f8fafc; border: 1px solid #cbd5f5; color: #374151; padding: 6px 10px; margin-right: 6px; border-radius: 6px; }
        table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
        th, td { border: 1px solid #bdc3c7; padding: 8px; text-align: left; }
        th { background-color: #34495e; color: white; }
        .shopping-list { margin-top: 24px; }
        .daily-grid { width: 100%; }
        .daily-card { display: inline-block; vertical-align: top; width: 48%; margin: 0 1% 10px 1%; border: 1px solid #e5e7eb; border-radius: 8px; padding: 8px 10px; box-sizing: border-box; }
        .daily-day { font-weight: bold; color: #111827; margin-bottom: 4px; }
        .daily-items { font-size: 12px; color: #374151; line-height: 1.5; }
        .pantry-inline { font-size: 12px; color: #374151; line-height: 1.7; }
        .pantry-item { display: inline-block; margin-right: 10px; margin-bottom: 6px; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 6px; padding: 3px 8px; }
    </style>
</head>
<body>
    <h1>${plan.title}</h1>
    
    <div class="advice">
        <strong>营养建议：</strong> ${plan.advice}
    </div>
    <#if plan.targetCalories??>
    <div class="targets">
        <span class="target-tag">目标能量: ${plan.targetCalories?c} 千卡/天</span>
        <#if plan.targetCarbs??><span class="target-tag">碳水: ${plan.targetCarbs?c} 克</span></#if>
        <#if plan.targetProtein??><span class="target-tag">蛋白质: ${plan.targetProtein?c} 克</span></#if>
        <#if plan.targetFat??><span class="target-tag">脂肪: ${plan.targetFat?c} 克</span></#if>
    </div>
    </#if>
    <#if plan.adjustmentNotes?? && (plan.adjustmentNotes?size > 0)>
    <div class="targets">
        <strong>本次按反馈已调整：</strong>
        <ul>
            <#list plan.adjustmentNotes as note>
                <li>${note}</li>
            </#list>
        </ul>
    </div>
    </#if>

    <h2>每周食谱安排</h2>
    <table>
        <thead>
            <tr>
                <th>日期</th>
                <th>早餐</th>
                <th>午餐</th>
                <th>晚餐</th>
            </tr>
        </thead>
        <tbody>
            <#list plan.weeklyPlan as day>
            <tr>
                <td>${day.day}</td>
                <td>
                    <strong>${day.breakfast.name}</strong><br/>
                    <small>${(day.breakfast.calories!'')?replace('kcal', '千卡')?replace('KCAL', '千卡')}</small><br/>
                    <small>
                        碳水: ${(day.breakfast.carbsGrams!0)?string("0")}克 /
                        蛋白: ${(day.breakfast.proteinGrams!0)?string("0")}克 /
                        脂肪: ${(day.breakfast.fatGrams!0)?string("0")}克
                    </small>
                </td>
                <td>
                    <strong>${day.lunch.name}</strong><br/>
                    <small>${(day.lunch.calories!'')?replace('kcal', '千卡')?replace('KCAL', '千卡')}</small><br/>
                    <small>
                        碳水: ${(day.lunch.carbsGrams!0)?string("0")}克 /
                        蛋白: ${(day.lunch.proteinGrams!0)?string("0")}克 /
                        脂肪: ${(day.lunch.fatGrams!0)?string("0")}克
                    </small>
                </td>
                <td>
                    <strong>${day.dinner.name}</strong><br/>
                    <small>${(day.dinner.calories!'')?replace('kcal', '千卡')?replace('KCAL', '千卡')}</small><br/>
                    <small>
                        碳水: ${(day.dinner.carbsGrams!0)?string("0")}克 /
                        蛋白: ${(day.dinner.proteinGrams!0)?string("0")}克 /
                        脂肪: ${(day.dinner.fatGrams!0)?string("0")}克
                    </small>
                </td>
            </tr>
            </#list>
        </tbody>
    </table>

    <div class="shopping-list">
        <h2>每日采购清单</h2>
        <#if plan.dailyShopping?? && (plan.dailyShopping?size > 0)>
            <div class="daily-grid">
                <#list plan.dailyShopping as daily>
                    <div class="daily-card">
                        <div class="daily-day">${daily.day}</div>
                        <div class="daily-items">
                            <#if daily.items?? && (daily.items?size > 0)>
                                ${daily.items?join("、")}
                            <#else>
                                无
                            </#if>
                        </div>
                    </div>
                </#list>
            </div>
        <#else>
            <p>暂无每日采购清单</p>
        </#if>
    </div>

    <div class="shopping-list">
        <h2>长期调料（建议常备）</h2>
        <#if plan.pantryItems?? && (plan.pantryItems?size > 0)>
            <div class="pantry-inline">
                <#list plan.pantryItems as item>
                    <span class="pantry-item">${item}</span>
                </#list>
            </div>
        <#else>
            <p>暂无调料清单</p>
        </#if>
    </div>
</body>
</html>
