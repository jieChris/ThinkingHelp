import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
    baseURL: '/api', // Vite proxy will forward this to localhost:8080
    timeout: 5000
})

let logoutRedirecting = false

const forceLogout = (message: string) => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('user')
    if (!logoutRedirecting) {
        logoutRedirecting = true
        ElMessage.error(message)
        if (window.location.pathname !== '/login') {
            window.location.href = '/login'
        } else {
            setTimeout(() => {
                logoutRedirecting = false
            }, 1000)
        }
    }
}

// Request interceptor
service.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token') || sessionStorage.getItem('token')
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`
        }
        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)

// Response interceptor
service.interceptors.response.use(
    (response) => {
        const res = response.data
        // Backend returns Result<T> with code/message/data
        if (res.code && res.code !== 200) {
            const errorMessage = res.message || res.msg || 'Error'
            if (res.code === 500) {
                forceLogout('服务器内部错误，已自动退出，请重新登录')
                return Promise.reject(new Error(errorMessage))
            }
            ElMessage.error(errorMessage)
            return Promise.reject(new Error(errorMessage))
        }
        return res
    },
    (error) => {
        console.error('err' + error)
        let msg = error.message || 'Request Failed'
        if (error.response) {
            switch (error.response.status) {
                case 401:
                    forceLogout('未授权，请重新登录')
                    return Promise.reject(error)
                case 403:
                    forceLogout('登录已失效，请重新登录')
                    return Promise.reject(error)
                case 404:
                    msg = '请求资源不存在'
                    break;
                case 500:
                case 502:
                case 503:
                case 504:
                    forceLogout('服务器异常，已自动退出，请重新登录')
                    return Promise.reject(error)
            }
        }
        ElMessage.error(msg)
        return Promise.reject(error)
    }
)

export default service
