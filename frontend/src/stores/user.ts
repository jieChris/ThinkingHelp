import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

interface UserSettings {
    fontSize: number // 0, 1, 2
    theme: 'light' | 'dark'
    aiPersona: 'strict' | 'gentle'
    notificationEnabled: boolean
}

// Match backend User entity
interface User {
    id: number
    username: string
    avatar?: string
    role?: string
    memberLevel?: number // 0 or 1
    phone?: string
    email?: string
    settings?: UserSettings
}

export const useUserStore = defineStore('user', () => {
    const getStoredToken = () => localStorage.getItem('token') || sessionStorage.getItem('token')
    const getStoredUser = () => localStorage.getItem('user') || sessionStorage.getItem('user')

    const token = ref<string | null>(getStoredToken())
    const storedUser = getStoredUser()
    const user = ref<User | null>(storedUser ? JSON.parse(storedUser) : null)

    const settings = ref<UserSettings>({
        fontSize: 0,
        theme: 'light',
        aiPersona: 'gentle',
        notificationEnabled: true
    })

    const isLoggedIn = computed(() => !!token.value)

    const fontSizeScale = computed(() => {
        const sizes = [1, 1.25, 1.5]
        return sizes[settings.value.fontSize] || 1
    })

    const login = (newToken: string, userInfo: User, remember = false) => {
        token.value = newToken
        user.value = userInfo
        const primaryStorage = remember ? localStorage : sessionStorage
        const secondaryStorage = remember ? sessionStorage : localStorage
        primaryStorage.setItem('token', newToken)
        primaryStorage.setItem('user', JSON.stringify(userInfo))
        secondaryStorage.removeItem('token')
        secondaryStorage.removeItem('user')

        // If backend sends settings, we should merge them here
        // For now, valid login triggers setting application
        applySettings()
    }

    const logout = () => {
        const userId = user.value?.id
        token.value = null
        user.value = null
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        sessionStorage.removeItem('token')
        sessionStorage.removeItem('user')
        if (userId) {
            localStorage.removeItem(`chat_sessions_${userId}`)
        }
        // Reset styles
        document.documentElement.style.fontSize = ''
    }

    const updateSettings = (newSettings: Partial<UserSettings>) => {
        settings.value = { ...settings.value, ...newSettings }
        applySettings()
        // Here you would call API to save settings
    }

    const applySettings = () => {
        // Apply Font Size scaling to root
        // Note: transforming logic needs to be handled by CSS variables or rem
        // For simplicity demo:
        document.documentElement.style.setProperty('--el-font-size-base', `${14 * fontSizeScale.value}px`)

        // Could also toggle class for theme
        if (settings.value.theme === 'dark') {
            document.documentElement.classList.add('dark')
        } else {
            document.documentElement.classList.remove('dark')
        }
    }

    return {
        token,
        user,
        settings,
        isLoggedIn,
        login,
        logout,
        updateSettings
    }
})
