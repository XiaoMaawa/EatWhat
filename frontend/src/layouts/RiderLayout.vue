<template>
  <el-container class="layout">
    <el-aside width="210px" class="aside">
      <div class="brand">
        <span class="logo">🛵</span>
        <span class="brand-text">EatWhat 骑手</span>
      </div>
      <el-menu :default-active="activeMenu" router class="menu">
        <el-menu-item index="/rider/apply">
          <el-icon><Postcard /></el-icon>
          <span>骑手认证</span>
        </el-menu-item>
        <el-menu-item index="/rider/hall" :disabled="menuDisabled('/rider/hall')">
          <el-icon><Promotion /></el-icon>
          <span>抢单大厅</span>
        </el-menu-item>
        <el-menu-item index="/rider/tasks" :disabled="menuDisabled('/rider/tasks')">
          <el-icon><Tickets /></el-icon>
          <span>我的任务</span>
        </el-menu-item>
        <el-menu-item index="/rider/exceptions" :disabled="menuDisabled('/rider/exceptions')">
          <el-icon><Warning /></el-icon>
          <span>异常记录</span>
        </el-menu-item>
        <el-menu-item index="/rider/stats" :disabled="menuDisabled('/rider/stats')">
          <el-icon><TrendCharts /></el-icon>
          <span>收益统计</span>
        </el-menu-item>
        <el-menu-item index="/rider/notices" :disabled="menuDisabled('/rider/notices')">
          <el-icon><Bell /></el-icon>
          <span>系统通知</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <span class="title">{{ route.meta?.title }}</span>
        <el-dropdown trigger="click" @command="onCommand">
          <span class="user">
            <el-avatar :size="28">{{ (userStore.nickname || 'R').slice(0, 1) }}</el-avatar>
            <span>{{ userStore.nickname }}</span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { riderApi } from '@/api'
import { ArrowDown, Bell, Postcard, Promotion, Tickets, TrendCharts, Warning } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const approved = ref(true)

// 未通过认证时，除「骑手认证」外的菜单全部禁用
const menuDisabled = (path) => path !== '/rider/apply' && !approved.value

async function checkApproved() {
  try {
    const profile = await riderApi.profile()
    approved.value = Boolean(profile) && profile.auditStatus === 1
  } catch {
    approved.value = false
  }
}

onMounted(checkApproved)

async function onCommand(command) {
  if (command === 'logout') {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
    await userStore.logout()
    router.replace('/login')
  }
}
</script>

<style scoped>
.layout {
  height: 100vh;
}

.aside {
  background: #14213d;
  display: flex;
  flex-direction: column;
}

.brand {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px;
  color: #fff;
  font-weight: 600;
  font-size: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo {
  font-size: 20px;
}

.menu {
  border-right: none;
  background: transparent;
  flex: 1;
}

.menu :deep(.el-menu-item) {
  color: #c0c4cc;
}

.menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
}

.menu :deep(.el-menu-item.is-active) {
  background: #2b6cb0;
  color: #fff;
}

.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 8px rgba(0, 0, 0, 0.06);
}

.title {
  font-size: 17px;
  font-weight: 600;
}

.user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
  font-size: 14px;
}

.main {
  background: var(--ew-bg);
  padding: 16px;
}
</style>
