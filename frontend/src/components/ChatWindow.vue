<template>
  <div class="chat-window">
    <div class="chat-history" ref="historyRef">
        <div v-for="(msg, index) in messages" :key="index" class="message" :class="msg.role">
            <div class="avatar">{{ msg.role === 'user' ? '👤' : '🤖' }}</div>
            <div class="content">
                <div v-if="msg.role === 'assistant'" class="source-badge">
                    <el-tag v-if="msg.hasSource" type="success" effect="dark">
                        <el-icon><SuccessFilled /></el-icon> 官方指南认证
                    </el-tag>
                    <el-tag v-else type="warning" effect="dark">
                        <el-icon><WarningFilled /></el-icon> AI 生成结果
                    </el-tag>
                </div>
                <div class="text" v-html="renderMarkdown(msg.content)"></div>
            </div>
        </div>
    </div>
    
    <div class="input-area">
        <el-input 
            v-model="inputRaw" 
            placeholder="请输入您的健康问题..." 
            @keyup.enter="sendMessage"
        >
            <template #append>
                <el-button @click="sendMessage" :loading="loading">发送</el-button>
            </template>
        </el-input>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import MarkdownIt from 'markdown-it'
// import { fetchEventSource } from '@microsoft/fetch-event-source' // 使用原生 EventSource (简单实现) 或 polyfill

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

    // 创建一个模拟的助手消息用于流式传输
    const assistantMsg = ref<Message>({ role: 'assistant', content: '', hasSource: true }) // 默认为有来源，如有需要稍后更新
    messages.value.push(assistantMsg.value)

    try {
        // SSE 实现
        // 真实后端: /api/rag/stream?question=...
        // 演示中，我们模拟或假设后端在 localhost:8080
        
        // 使用原生 EventSource? 原生 ES 只支持 GET.
        /*
        const es = new EventSource(`http://localhost:8080/api/rag/stream?question=${encodeURIComponent(question)}`);
        
        es.onmessage = (event) => {
            const chunk = event.data;
            if (chunk.includes("免责声明")) assistantMsg.value.hasSource = false; // Simple heuristic
            assistantMsg.value.content += chunk;
            scrollToBottom();
        };
        
        es.onerror = (e) => {
             es.close();
             loading.value = false;
        };
        */
       
        // 暂时模拟流式传输，因为后端并未实际运行
        await simulateStream(assistantMsg.value)

    } catch (e) {
        console.error(e)
    } finally {
        loading.value = false
    }
}

const simulateStream = async (msg: Message) => {
    const text = "根据《中国居民膳食指南(2022)》，高血压患者每日盐摄入量应低于5克。\n\n建议多食用富含钾的食物，如：\n- 香蕉\n- 土豆\n- 菠菜\n\n(模拟流式输出...)"
    const chunks = text.split('')
    for (const char of chunks) {
        msg.content += char
        await new Promise(r => setTimeout(r, 50))
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

<style scoped>
.chat-window {
    display: flex;
    flex-direction: column;
    height: 600px;
}
.chat-history {
    flex: 1;
    overflow-y: auto;
    padding: 20px;
    border: 1px solid #eee;
    margin-bottom: 20px;
    border-radius: 4px;
}
.message {
    display: flex;
    margin-bottom: 20px;
}
.message.user {
    flex-direction: row-reverse;
}
.avatar {
    font-size: 24px;
    padding: 0 10px;
}
.content {
    background: #f4f4f5;
    padding: 10px 15px;
    border-radius: 8px;
    max-width: 70%;
}
.message.user .content {
    background: #d9ecff;
}
.source-badge {
    margin-bottom: 5px;
    font-size: 12px;
}
</style>
