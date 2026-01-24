import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
    baseURL: '/api', // Vite proxy will forward this to localhost:8080
    timeout: 5000
})

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
                    msg = '未授权，请重新登录'
                    // specific logic to redirect to login could go here
                    localStorage.removeItem('token')
                    sessionStorage.removeItem('token')
                    if (window.location.pathname !== '/login') {
                        window.location.href = '/login'
                    }
                    break;
                case 403:
                    msg = '登录已失效，请重新登录'
                    localStorage.removeItem('token')
                    sessionStorage.removeItem('token')
                    if (window.location.pathname !== '/login') {
                        window.location.href = '/login'
                    }
                    break;
                case 404:
                    msg = '请求资源不存在'
                    break;
                case 500:
                    msg = '服务器内部错误'
                    break;
            }
        }
        ElMessage.error(msg)
        return Promise.reject(error)
    }
)

export default service
