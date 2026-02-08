<template>
  <div class="chat-shell">
    <aside class="chat-sidebar">
        <div class="sidebar-header">
            <span class="sidebar-title">对话记录</span>
            <el-button type="primary" size="small" @click="createNewSession" icon="Plus">新建</el-button>
        </div>
        <div class="session-list">
            <div
                v-for="session in sessions"
                :key="session.id"
                class="session-item"
                :class="{ active: session.id === activeSessionId }"
                @click="selectSession(session.id)"
            >
                <div class="session-main">
                    <div class="session-title">{{ session.title }}</div>
                    <div class="session-time">{{ formatTime(session.updatedAt) }}</div>
                </div>
                <el-button
                    class="session-delete"
                    text
                    size="small"
                    icon="Delete"
                    @click.stop="deleteSession(session.id)"
                />
            </div>
            <el-empty v-if="sessions.length === 0" description="暂无记录" />
        </div>
    </aside>

    <div class="chat-window">
        <div class="chat-header">
            <div class="chat-title">
                <template v-if="renaming">
                    <el-input
                        v-model="renameValue"
                        size="small"
                        class="rename-input"
                        maxlength="20"
                        show-word-limit
                        @keyup.enter="confirmRename"
                    />
                    <el-button size="small" type="primary" @click="confirmRename">保存</el-button>
                    <el-button size="small" @click="cancelRename">取消</el-button>
                </template>
                <template v-else>
                    <span class="title-text">{{ activeSessionTitle }}</span>
                    <el-button text size="small" icon="Edit" @click="startRename">重命名</el-button>
                </template>
            </div>
        </div>
        <div class="chat-history" ref="historyRef">
            <div v-for="(msg, index) in messages" :key="index" class="message" :class="msg.role">
                <div class="avatar">{{ msg.role === 'user' ? '👤' : '🤖' }}</div>
                <div class="content">
                    <div v-if="msg.role === 'assistant'" class="source-badge">
                        <el-tag v-if="msg.hasSource" type="success" effect="dark" class="source-tag">
                            <el-icon class="tag-icon"><SuccessFilled /></el-icon>
                            <span>官方指南认证</span>
                        </el-tag>
                        <el-tag v-else type="warning" effect="dark" class="source-tag">
                            <el-icon class="tag-icon"><WarningFilled /></el-icon>
                            <span>AI 生成结果</span>
                        </el-tag>
                    </div>
                    <!-- 这里的 v-html 需要确保内容安全，或者使用 vue-markdown 等组件 -->
                    <div class="text" v-html="renderMarkdown(msg.content)"></div>
                </div>
            </div>
        </div>
        
        <div class="input-area">
            <el-input 
                v-model="inputRaw" 
                placeholder="请输入您的健康问题..." 
                @keyup.enter="sendMessage"
                size="large"
                :disabled="loading"
            >
                <template #append>
                    <el-button v-if="loading" type="danger" @click="stopStreaming" icon="Close">停止</el-button>
                    <el-button v-else @click="sendMessage" icon="Position">发送</el-button>
                </template>
            </el-input>
        </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, computed } from 'vue'
import MarkdownIt from 'markdown-it'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../../stores/user'

const md = new MarkdownIt()
const inputRaw = ref('')
const loading = ref(false)
const historyRef = ref<HTMLElement | null>(null)
const userStore = useUserStore()

interface Message {
    role: 'user' | 'assistant';
    content: string;
    hasSource?: boolean;
}

interface ChatSession {
    id: string;
    title: string;
    messages: Message[];
    updatedAt: string;
}

const sessions = ref<ChatSession[]>([])
const activeSessionId = ref('')
const messages = ref<Message[]>([])
const saveTimer = ref<number | null>(null)
const abortController = ref<AbortController | null>(null)
const activeReader = ref<ReadableStreamDefaultReader<Uint8Array> | null>(null)
const renaming = ref(false)
const renameValue = ref('')

const activeSessionTitle = computed(() => {
    const session = sessions.value.find((s) => s.id === activeSessionId.value)
    return session?.title || '新对话'
})

const storageKey = () => {
    const userId = userStore.user?.id || 'guest'
    return `chat_sessions_${userId}`
}

const createSession = (title = '新对话') => {
    return {
        id: `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
        title,
        messages: [
            { role: 'assistant', content: '您好，我是您的专属营养师。请问有什么可以帮您？', hasSource: true }
        ],
        updatedAt: new Date().toISOString()
    }
}

const loadSessions = () => {
    const raw = localStorage.getItem(storageKey())
    if (raw) {
        try {
            const parsed = JSON.parse(raw)
            sessions.value = parsed.sessions || []
            activeSessionId.value = parsed.activeSessionId || (sessions.value[0]?.id || '')
        } catch (e) {
            console.error(e)
            sessions.value = []
        }
    }
    if (sessions.value.length === 0) {
        const session = createSession()
        sessions.value = [session]
        activeSessionId.value = session.id
    }
    const active = sessions.value.find((s) => s.id === activeSessionId.value) || sessions.value[0]
    activeSessionId.value = active.id
    messages.value = active.messages
}

const saveSessions = () => {
    localStorage.setItem(
        storageKey(),
        JSON.stringify({
            activeSessionId: activeSessionId.value,
            sessions: sessions.value
        })
    )
}

const scheduleSave = () => {
    if (saveTimer.value) {
        window.clearTimeout(saveTimer.value)
    }
    saveTimer.value = window.setTimeout(() => {
        saveSessions()
        saveTimer.value = null
    }, 300)
}

const selectSession = (id: string) => {
    if (activeSessionId.value === id) return
    const session = sessions.value.find((s) => s.id === id)
    if (!session) return
    renaming.value = false
    activeSessionId.value = id
    messages.value = session.messages
    scheduleSave()
    scrollToBottom()
}

const createNewSession = () => {
    if (loading.value) {
        stopStreaming()
    }
    const session = createSession()
    sessions.value.unshift(session)
    renaming.value = false
    activeSessionId.value = session.id
    messages.value = session.messages
    scheduleSave()
    scrollToBottom()
}

const updateSessionMeta = (title?: string) => {
    const session = sessions.value.find((s) => s.id === activeSessionId.value)
    if (!session) return
    if (title && session.title === '新对话') {
        session.title = title
    }
    session.updatedAt = new Date().toISOString()
    scheduleSave()
}

const startRename = () => {
    const session = sessions.value.find((s) => s.id === activeSessionId.value)
    if (!session) return
    renameValue.value = session.title
    renaming.value = true
}

const confirmRename = () => {
    const session = sessions.value.find((s) => s.id === activeSessionId.value)
    if (!session) return
    const nextTitle = renameValue.value.trim()
    if (nextTitle) {
        session.title = nextTitle
        session.updatedAt = new Date().toISOString()
        scheduleSave()
    }
    renaming.value = false
}

const cancelRename = () => {
    renaming.value = false
}

const deleteSession = async (id: string) => {
    const session = sessions.value.find((s) => s.id === id)
    if (!session) return

    try {
        await ElMessageBox.confirm('确定删除该对话记录吗？', '提示', {
            confirmButtonText: '删除',
            cancelButtonText: '取消',
            type: 'warning'
        })
    } catch {
        return
    }

    if (loading.value && activeSessionId.value === id) {
        await stopStreaming()
    }

    sessions.value = sessions.value.filter((s) => s.id !== id)
    renaming.value = false

    if (sessions.value.length === 0) {
        const newSession = createSession()
        sessions.value = [newSession]
        activeSessionId.value = newSession.id
        messages.value = newSession.messages
    } else if (activeSessionId.value === id) {
        const next = sessions.value[0]
        activeSessionId.value = next.id
        messages.value = next.messages
    }

    scheduleSave()
    ElMessage.success('已删除')
}

const renderMarkdown = (text: string) => {
    return md.render(text)
}

const formatTime = (iso: string) => {
    return dayjs(iso).format('MM-DD HH:mm')
}

const stopStreaming = async () => {
    if (abortController.value) {
        abortController.value.abort()
        abortController.value = null
    }
    if (activeReader.value) {
        try {
            await activeReader.value.cancel()
        } catch {
            // ignore
        } finally {
            activeReader.value = null
        }
    }
    loading.value = false
}

const forceLogoutForServerError = (message: string) => {
    userStore.logout()
    ElMessage.error(message)
    if (window.location.pathname !== '/login') {
        window.location.href = '/login'
    }
}

const sendMessage = async () => {
    if (!inputRaw.value.trim() || loading.value) return
    
    const question = inputRaw.value
    const session = sessions.value.find((s) => s.id === activeSessionId.value)
    if (!session) return

    session.messages.push({ role: 'user', content: question })
    updateSessionMeta(question.slice(0, 12))
    messages.value = session.messages
    inputRaw.value = ''
    loading.value = true

    // Placeholder for assistant message
    const assistantMsg = ref<Message>({ role: 'assistant', content: '', hasSource: true }) 
    session.messages.push(assistantMsg.value)
    messages.value = session.messages
    scheduleSave()

    try {
        const token = localStorage.getItem('token') || sessionStorage.getItem('token')
        abortController.value = new AbortController()
        const response = await fetch('/api/chat/stream', {
            method: 'POST',
            headers: {
                'Content-Type': 'text/plain', // Sending raw string body
                ...(token ? { Authorization: `Bearer ${token}` } : {})
            },
            body: question,
            signal: abortController.value.signal
        })

        if (response.status === 401 || response.status === 403) {
            forceLogoutForServerError('登录已失效，请重新登录')
            return
        }
        if ([500, 502, 503, 504].includes(response.status)) {
            forceLogoutForServerError('服务器异常，已自动退出，请重新登录')
            return
        }
        if (!response.ok) throw new Error(response.statusText)
        if (!response.body) throw new Error('No response body')

        const reader = response.body.getReader()
        activeReader.value = reader
        const decoder = new TextDecoder()
        let buffer = ''
        let streamDone = false
        
        while (true) {
            const { done, value } = await reader.read()
            if (done) break
            const chunk = decoder.decode(value, { stream: true })
            buffer += chunk
            const parts = buffer.split(/\r?\n\r?\n/)
            buffer = parts.pop() || ''
            for (const part of parts) {
                const lines = part.split(/\r?\n/)
                const dataLines: string[] = []
                for (const line of lines) {
                    if (line.startsWith('data:')) {
                        dataLines.push(line.replace(/^data:\s?/, ''))
                    }
                }
                const data = dataLines.join('\n')
                if (!data) continue
                if (data === '[DONE]') {
                    streamDone = true
                    break
                }
                assistantMsg.value.content += data
            }
            if (streamDone) {
                await reader.cancel()
                break
            }
            updateSessionMeta()
            scrollToBottom()
        }
    } catch (e: any) {
        if (e?.name !== 'AbortError') {
            console.error(e)
            assistantMsg.value.content += '\n\n(网络请求失败，请检查后端服务是否启动)'
        }
    } finally {
        loading.value = false
        abortController.value = null
        activeReader.value = null
        updateSessionMeta()
        scrollToBottom()
    }
}

const scrollToBottom = () => {
    nextTick(() => {
        if (historyRef.value) {
            historyRef.value.scrollTop = historyRef.value.scrollHeight
        }
    })
}

onMounted(() => {
    loadSessions()
    scrollToBottom()
})
</script>

<style scoped lang="scss">
.chat-shell {
    display: flex;
    gap: 16px;
    flex: 1;
    min-height: 0;
    height: 100%;
}

.chat-sidebar {
    width: 240px;
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
    padding: 16px;
    display: flex;
    flex-direction: column;
}

.sidebar-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
}

.sidebar-title {
    font-weight: 600;
    color: #111827;
}

.session-list {
    flex: 1;
    overflow-y: auto;
}

.session-item {
    padding: 10px 12px;
    border-radius: 8px;
    cursor: pointer;
    margin-bottom: 8px;
    background: #f9fafb;
    border: 1px solid transparent;
    transition: all 0.2s ease;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
}

.session-item:hover {
    border-color: #d1fae5;
}

.session-item.active {
    background: #ecfdf5;
    border-color: #059669;
}

.session-main {
    flex: 1;
    min-width: 0;
}

.session-title {
    font-size: 14px;
    font-weight: 600;
    color: #111827;
    margin-bottom: 4px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.session-time {
    font-size: 12px;
    color: #6b7280;
}

.session-delete {
    color: #9ca3af;
}

.session-delete:hover {
    color: #ef4444;
}

.chat-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-bottom: 12px;
    border-bottom: 1px solid #f3f4f6;
    margin-bottom: 16px;
}

.chat-title {
    display: flex;
    align-items: center;
    gap: 10px;
}

.title-text {
    font-size: 16px;
    font-weight: 600;
    color: #111827;
}

.rename-input {
    width: 220px;
}

.chat-window {
    display: flex;
    flex-direction: column;
    /* 
       Height Calculation:
       100vh (Viewport) 
       - 60px (Header) 
       - 48px (Main Padding: 24px top + 24px bottom) 
       = calc(100vh - 108px)
    */
    flex: 1;
    height: 100%;
    min-height: 0;
    background: #fff;
    border-radius: 8px;
    padding: 20px;
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
    box-sizing: border-box;
}
.chat-history {
    flex: 1;
    overflow-y: auto;
    min-height: 0;
    padding: 10px;
    border: 1px solid #f3f4f6;
    border-radius: 8px;
    background: #f9fafb;
}

.input-area {
    margin-top: auto;
    padding-top: 12px;
    border-top: 1px solid #f3f4f6;
    background: #fff;
    flex-shrink: 0;
}
.message {
    display: flex;
    margin-bottom: 24px;
    
    &.user {
        flex-direction: row-reverse;
        .content {
            background: #d1fae5; // Green tint
            color: #064e3b;
            border-bottom-right-radius: 2px;
        }
    }
    
    &.assistant {
        .content {
            background: #fff;
            border: 1px solid #e5e7eb;
            border-bottom-left-radius: 2px;
        }
    }
}
.avatar {
    font-size: 28px;
    padding: 0 12px;
    align-self: flex-start;
}
.content {
    padding: 12px 16px;
    border-radius: 12px;
    max-width: 75%;
    position: relative;
    box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
    
    .source-badge {
        margin-bottom: 8px;
        font-size: 12px;
        border-bottom: 1px solid #f3f4f6;
        padding-bottom: 4px;
        
        .source-tag {
            /* Ensure the tag itself behaves */
            vertical-align: middle;
            
            /* Target the inner content wrapper of Element Plus tag */
            :deep(.el-tag__content) {
                display: inline-flex;
                align-items: center;
                
                .tag-icon {
                    margin-right: 4px;
                    font-size: 14px;
                    /* Ensure icon is vertically centered */
                    display: flex;
                    align-items: center;
                }
            }
        }
    }
    
    .text {
        line-height: 1.6;
        font-size: 15px;
    }
}

@media (max-width: 1024px) {
    .chat-shell {
        flex-direction: column;
    }
    .chat-sidebar {
        width: 100%;
    }
}
</style>
