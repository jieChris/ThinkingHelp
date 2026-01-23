import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import { createPinia } from 'pinia'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import router from './router'
import { ElMessage } from 'element-plus'
import './api/mock' // We might need this for mock data later, but for now just leave it out or placeholder

const app = createApp(App)

// 注册图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

const recordRuntimeError = (err: unknown, info?: string) => {
    try {
        const payload = {
            message: String((err as any)?.message || err),
            stack: (err as any)?.stack,
            info,
            time: new Date().toISOString()
        }
        sessionStorage.setItem('last_runtime_error', JSON.stringify(payload))
    } catch {
        // ignore
    }
}

app.config.errorHandler = (err, instance, info) => {
    console.error('Vue runtime error:', err, info)
    recordRuntimeError(err, info)
    ElMessage.error('页面出现异常，请刷新后重试')
}

window.addEventListener('error', (event) => {
    console.error('Window error:', event.error || event.message)
    recordRuntimeError(event.error || event.message)
})

window.addEventListener('unhandledrejection', (event) => {
    console.error('Unhandled rejection:', event.reason)
    recordRuntimeError(event.reason)
})

app.mount('#app')
