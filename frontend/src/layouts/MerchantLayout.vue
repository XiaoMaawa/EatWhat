<template>
  <el-container class="layout">
    <el-aside width="210px" class="aside">
      <div class="brand">
        <span class="logo">🍜</span>
        <span class="brand-text">EatWhat 商家</span>
      </div>
      <el-menu :default-active="activeMenu" router class="menu">
        <el-menu-item index="/merchant/apply">
          <el-icon><Postcard /></el-icon>
          <span>商家入驻</span>
        </el-menu-item>
        <el-menu-item index="/merchant/dashboard" :disabled="menuDisabled('/merchant/dashboard')">
          <el-icon><DataLine /></el-icon>
          <span>经营概览</span>
        </el-menu-item>
        <el-menu-item index="/merchant/orders" :disabled="menuDisabled('/merchant/orders')">
          <el-icon><List /></el-icon>
          <span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="/merchant/dishes" :disabled="menuDisabled('/merchant/dishes')">
          <el-icon><Dish /></el-icon>
          <span>菜品管理</span>
        </el-menu-item>
        <el-menu-item index="/merchant/shop" :disabled="menuDisabled('/merchant/shop')">
          <el-icon><Shop /></el-icon>
          <span>店铺管理</span>
        </el-menu-item>
        <el-menu-item index="/merchant/reviews" :disabled="menuDisabled('/merchant/reviews')">
          <el-icon><ChatDotSquare /></el-icon>
          <span>评价管理</span>
        </el-menu-item>
        <el-menu-item index="/merchant/after-sales" :disabled="menuDisabled('/merchant/after-sales')">
          <el-icon><Wallet /></el-icon>
          <span>退款售后</span>
        </el-menu-item>
        <el-menu-item index="/merchant/analytics" :disabled="menuDisabled('/merchant/analytics')">
          <el-icon><TrendCharts /></el-icon>
          <span>经营分析</span>
        </el-menu-item>
        <el-menu-item index="/merchant/preview" :disabled="menuDisabled('/merchant/preview')">
          <el-icon><View /></el-icon>
          <span>顾客视角</span>
        </el-menu-item>
        <el-menu-item index="/merchant/qrcode" :disabled="menuDisabled('/merchant/qrcode')">
          <el-icon><Iphone /></el-icon>
          <span>店铺二维码</span>
        </el-menu-item>
        <el-menu-item index="/merchant/account" :disabled="menuDisabled('/merchant/account')">
          <el-icon><Setting /></el-icon>
          <span>账号设置</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <span class="title">{{ route.meta?.title }}</span>
        <el-dropdown trigger="click" @command="onCommand">
          <span class="user">
            <el-avatar :size="28">{{ (userStore.nickname || 'M').slice(0, 1) }}</el-avatar>
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
import { merchantApi } from '@/api'
import { ArrowDown, ChatDotSquare, DataLine, Dish, Iphone, List, Postcard, Setting, Shop, TrendCharts, View, Wallet } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const applied = ref(true)

// 未提交入驻申请时，除「商家入驻」外的菜单全部禁用，避免点进去满屏报错
const menuDisabled = (path) => path !== '/merchant/apply' && !applied.value

async function checkApplied() {
  try {
    const profile = await merchantApi.profile()
    applied.value = Boolean(profile?.applied) && profile.auditStatus === 1
  } catch {
    applied.value = false
  }
}

onMounted(checkApplied)

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
  background: #1f2d3d;
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
  font-size: 22px;
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
  background: var(--ew-primary);
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
