<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <title>${plan.title}</title>
    <style>
        body { font-family: 'CJK', 'SimHei', 'Microsoft YaHei', 'PingFang SC', sans-serif; }
        h1 { color: #2c3e50; text-align: center; }
        .advice { background: #ecf0f1; padding: 10px; border-radius: 5px; margin-bottom: 20px; }
        table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
        th, td { border: 1px solid #bdc3c7; padding: 8px; text-align: left; }
        th { background-color: #34495e; color: white; }
        .shopping-list { margin-top: 30px; }
        .shopping-list li { margin-bottom: 5px; }
    </style>
</head>
<body>
    <h1>${plan.title}</h1>
    
    <div class="advice">
        <strong>Expert Advice:</strong> ${plan.advice}
    </div>

    <h2>Weekly Schedule</h2>
    <table>
        <thead>
            <tr>
                <th>Day</th>
                <th>Breakfast</th>
                <th>Lunch</th>
                <th>Dinner</th>
            </tr>
        </thead>
        <tbody>
            <#list plan.weeklyPlan as day>
            <tr>
                <td>${day.day}</td>
                <td>
                    <strong>${day.breakfast.name}</strong><br/>
                    <small>${day.breakfast.calories}</small>
                </td>
                <td>
                    <strong>${day.lunch.name}</strong><br/>
                    <small>${day.lunch.calories}</small>
                </td>
                <td>
                    <strong>${day.dinner.name}</strong><br/>
                    <small>${day.dinner.calories}</small>
                </td>
            </tr>
            </#list>
        </tbody>
    </table>

    <div class="shopping-list">
        <h2>每日采购清单</h2>
        <#if plan.dailyShopping?? && (plan.dailyShopping?size > 0)>
            <#list plan.dailyShopping as daily>
                <h3>${daily.day}</h3>
                <ul>
                    <#list daily.items as item>
                        <li>${item}</li>
                    </#list>
                </ul>
            </#list>
        <#else>
            <p>暂无每日采购清单</p>
        </#if>
    </div>

    <div class="shopping-list">
        <h2>长期调料（建议常备）</h2>
        <#if plan.pantryItems?? && (plan.pantryItems?size > 0)>
            <ul>
                <#list plan.pantryItems as item>
                    <li>${item}</li>
                </#list>
            </ul>
        <#else>
            <p>暂无调料清单</p>
        </#if>
    </div>
</body>
</html>
