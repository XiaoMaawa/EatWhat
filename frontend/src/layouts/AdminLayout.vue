<template>
  <el-container class="layout">
    <el-aside width="210px" class="aside">
      <div class="brand">
        <span class="logo">🐋</span>
        <span class="brand-text">EatWhat 管理后台</span>
      </div>
      <el-menu :default-active="activeMenu" router class="menu">
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataBoard /></el-icon>
          <span>数据看板</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/merchants">
          <el-icon><Shop /></el-icon>
          <span>商家审核</span>
        </el-menu-item>
        <el-menu-item index="/admin/riders">
          <el-icon><Bicycle /></el-icon>
          <span>骑手审核</span>
        </el-menu-item>
        <el-menu-item index="/admin/orders">
          <el-icon><List /></el-icon>
          <span>订单监管</span>
        </el-menu-item>
        <el-menu-item index="/admin/dishes">
          <el-icon><Dish /></el-icon>
          <span>菜品监管</span>
        </el-menu-item>        <el-menu-item index="/admin/after-sales">
          <el-icon><Wallet /></el-icon>
          <span>售后仲裁</span>
        </el-menu-item>
        <el-menu-item index="/admin/logs">
          <el-icon><Document /></el-icon>
          <span>操作日志</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <span class="title">{{ route.meta?.title }}</span>
        <el-dropdown trigger="click" @command="onCommand">
          <span class="user">
            <el-avatar :size="28">{{ (userStore.nickname || 'A').slice(0, 1) }}</el-avatar>
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
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { ArrowDown, Bicycle, DataBoard, Dish, Document, List, Shop, User, Wallet } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

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
  background: #0f172a;
  display: flex;
  flex-direction: column;
}

.brand {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  color: #fff;
  font-weight: 600;
  font-size: 15px;
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
  color: #cbd5e1;
}

.menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
}

.menu :deep(.el-menu-item.is-active) {
  background: #2563eb;
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
