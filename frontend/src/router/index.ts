import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import MainLayout from '../layout/MainLayout.vue'
import Dashboard from '../views/dashboard/Dashboard.vue'

const routes: Array<RouteRecordRaw> = [
    {
        path: '/',
        component: MainLayout,
        redirect: '/login',
        children: [
            {
                path: 'dashboard',
                name: 'Dashboard',
                component: Dashboard,
                meta: { title: '首页仪表盘' }
            },
            {
                path: 'health',
                name: 'HealthProfile',
                component: () => import('../views/health/HealthProfile.vue'), // Lazy load
                meta: { title: '健康档案' }
            },
            {
                path: 'chat',
                name: 'AIChat',
                component: () => import('../views/chat/ChatWindow.vue'),
                meta: { title: 'AI 营养师' }
            },
            {
                path: 'diet',
                name: 'DietLog',
                component: () => import('../views/diet/DietLog.vue'),
                meta: { title: '饮食记录' }
            },
            {
                path: 'plan',
                name: 'MealPlan',
                component: () => import('../views/plan/MealPlan.vue'),
                meta: { title: '食谱中心' }
            },
            {
                path: 'settings',
                name: 'Settings',
                component: () => import('../views/settings/Settings.vue'),
                meta: { title: '个性化设置' }
            },
            {
                path: 'admin/users',
                name: 'AdminUserManagement',
                component: () => import('../views/admin/AdminUserManagement.vue'),
                meta: { title: '用户管理' }
            },
            {
                path: 'user/profile',
                name: 'UserProfile',
                component: () => import('../views/user/Profile.vue'),
                meta: { title: '个人中心' }
            }
        ]
    },
    {
        path: '/login',
        name: 'Login',
        component: () => import('../views/login/Login.vue'),
        meta: { title: '用户登录' }
    },
    {
        path: '/register',
        name: 'Register',
        component: () => import('../views/login/Register.vue'),
        meta: { title: '用户注册' }
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

router.beforeEach((to, from, next) => {
    // 1. Update Title
    document.title = (to.meta.title as string) || 'ThinkingHelp'

    // 2. Auth Check
    const token = localStorage.getItem('token')
    const whiteList = ['/login', '/register']

    if (whiteList.includes(to.path)) {
        // If going to login/register, allow
        next()
    } else {
        // If protected route
        if (token) {
            next()
        } else {
            next('/login')
        }
    }
})

export default router
