<template>
  <div class="chat-window">
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
        >
            <template #append>
                <el-button @click="sendMessage" :loading="loading" icon="Position">发送</el-button>
            </template>
        </el-input>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import MarkdownIt from 'markdown-it'

const md = new MarkdownIt()
const inputRaw = ref('')
const loading = ref(false)
const historyRef = ref<HTMLElement | null>(null)

interface Message {
    role: 'user' | 'assistant';
    content: string;
    hasSource?: boolean;
}

const messages = ref<Message[]>([
    { role: 'assistant', content: '您好，我是您的专属营养师。请问有什么可以帮您？', hasSource: true }
])

const renderMarkdown = (text: string) => {
    return md.render(text)
}

const sendMessage = async () => {
    if (!inputRaw.value.trim()) return
    
    const question = inputRaw.value
    messages.value.push({ role: 'user', content: question })
    inputRaw.value = ''
    loading.value = true

    // Placeholder for assistant message
    const assistantMsg = ref<Message>({ role: 'assistant', content: '', hasSource: true }) 
    messages.value.push(assistantMsg.value)

    try {
        const response = await fetch('/api/chat/stream', {
            method: 'POST',
            headers: {
                'Content-Type': 'text/plain', // Sending raw string body
                // Add Authorization if needed: 'Authorization': `Bearer ${userStore.token}`
            },
            body: question
        })

        if (!response.ok) throw new Error(response.statusText)
        if (!response.body) throw new Error('No response body')

        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        
        while (true) {
            const { done, value } = await reader.read()
            if (done) break
            const chunk = decoder.decode(value, { stream: true })
            assistantMsg.value.content += chunk
            scrollToBottom()
        }
    } catch (e) {
        console.error(e)
        assistantMsg.value.content += '\n\n(网络请求失败，请检查后端服务是否启动)'
    } finally {
        loading.value = false
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
</script>

<style scoped lang="scss">
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
    height: calc(100vh - 108px); 
    background: #fff;
    border-radius: 8px;
    padding: 20px;
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
    box-sizing: border-box;
}
.chat-history {
    flex: 1;
    overflow-y: auto;
    padding: 10px;
    border: 1px solid #f3f4f6;
    margin-bottom: 20px;
    border-radius: 8px;
    background: #f9fafb;
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
</style>
