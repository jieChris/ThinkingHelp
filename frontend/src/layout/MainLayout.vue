<template>
  <el-container class="layout-container">
    <el-aside :width="isCollapse ? '64px' : '240px'" class="aside">
      <div class="logo">
        <el-icon class="logo-icon"><Apple /></el-icon>
        <span v-show="!isCollapse">ThinkingHelp</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="el-menu-vertical"
        background-color="#ffffff"
        text-color="#333"
        active-text-color="#059669"
        :collapse="isCollapse"
        :collapse-transition="false"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title>首页仪表盘</template>
        </el-menu-item>
        <el-menu-item index="/health">
          <el-icon><FirstAidKit /></el-icon>
          <template #title>健康档案</template>
        </el-menu-item>
        <el-menu-item index="/chat">
          <el-icon><ChatDotRound /></el-icon>
          <template #title>AI 营养师</template>
        </el-menu-item>
        <el-menu-item index="/diet">
          <el-icon><Food /></el-icon>
          <template #title>饮食记录</template>
        </el-menu-item>
        <el-menu-item index="/plan">
          <el-icon><Calendar /></el-icon>
          <template #title>食谱中心</template>
        </el-menu-item>
        
        <!-- Admin Only -->
        <el-menu-item index="/admin/users" v-if="userStore.user?.role === 'ADMIN'">
          <el-icon><UserFilled /></el-icon>
          <template #title>用户管理</template>
        </el-menu-item>
      </el-menu>
    </el-aside>
    
    <el-container>
      <el-header class="header">
        <div class="breadcrumb">
          <el-icon class="hamburger" @click="toggleCollapse">
             <Expand v-if="isCollapse" />
             <Fold v-else />
          </el-icon>
          <span class="page-title">{{ currentRouteTitle }}</span>
        </div>
        <div class="user-info">
          <!-- Avatar Click -> Profile -->
          <div class="avatar-wrapper" @click="goProfile">
               <img v-if="userStore.user?.avatar" :src="fullAvatarUrl(userStore.user.avatar)" class="header-avatar-img" />
               <el-avatar v-else :size="32" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
          </div>
          <span class="user-name" @click="goProfile">{{ userStore.user?.nickname || '用户' }}</span>
          <el-icon class="setting-icon" @click="goSettings"><Setting /></el-icon>
        </div>
      </el-header>
      
      <el-main class="main-viewport">
        <div class="scroll-container">
            <div class="content-wrapper">
                <router-view v-slot="{ Component }">
                    <transition name="fade" mode="out-in">
                        <component :is="Component" />
                    </transition>
                </router-view>
            </div>
        </div>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { Fold, Expand } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const activeMenu = computed(() => route.path)
const currentRouteTitle = computed(() => route.meta.title || 'ThinkingHelp')

const isCollapse = ref(false)
const toggleCollapse = () => {
    isCollapse.value = !isCollapse.value
}

const goSettings = () => {
    router.push('/settings')
}

const goProfile = () => {
    router.push('/user/profile')
}

const fullAvatarUrl = (url: string) => {
    if (!url) return ''
    if (url.startsWith('http')) return url
    return `http://localhost:8080${url}`
}
</script>

<style scoped lang="scss">
.layout-container {
  height: 100vh;
  
  .aside {
    background-color: #fff;
    border-right: 1px solid #e6e6e6;
    display: flex;
    flex-direction: column;
    transition: width 0.3s;
    overflow: hidden;
    
    .logo {
      height: 60px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
      font-weight: bold;
      color: #059669;
      border-bottom: 1px solid #f0f0f0;
      
      .logo-icon {
        margin-right: 8px;
        font-size: 24px;
      }
    }
    
    .el-menu-vertical {
      border-right: none;
      flex: 1;
      
      :deep(.el-menu-item) {
        font-size: 16px;
        height: 56px;
        
        &.is-active {
          background-color: #ecfdf5; // Light green bg
          border-right: 3px solid #059669;
        }
      }
    }
  }
  
  .header {
    background-color: #fff;
    border-bottom: 1px solid #e6e6e6;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 24px;
    height: 60px;
    
    .breadcrumb {
      display: flex;
      align-items: center;
      font-size: 18px;
      color: #333;
      .hamburger {
        margin-right: 12px;
        cursor: pointer;
        color: #666;
      }
    }
    
    .user-info {
      display: flex;
      align-items: center;
      cursor: pointer;
      
      .header-avatar-img {
          width: 32px;
          height: 32px;
          border-radius: 50%;
          object-fit: cover;
      }
      
      .user-name {
        margin-left: 8px;
        font-size: 16px;
        color: #666;
      }
      
      .setting-icon {
        margin-left: 16px;
        color: #999;
        font-size: 18px;
      }
    }
  }
  
  .main-viewport {
    padding: 0;
    overflow: hidden; /* Prevent global scrollbar on main */
    background-color: #F3F4F6;
    display: flex;
    flex-direction: column;
    
    .scroll-container {
        flex: 1;
        overflow-y: auto; /* Scrollbar belongs here */
        padding: 24px;
        display: flex;
        flex-direction: column;
        min-height: 0;
        
        .content-wrapper {
            /* max-width removed for full-width layout */
            margin: 0;
            width: 100%;
            flex: 1;
            min-height: 0;
            display: flex;
            flex-direction: column;
        }
    }
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
