<template>
  <div class="layout">
    <header class="header">
      <div class="header-inner">
        <router-link to="/" class="logo">
          <span class="logo-icon">🍜</span>
          <span class="logo-text">EatWhat</span>
        </router-link>

        <nav class="nav">
          <router-link to="/" class="nav-item">首页</router-link>
          <router-link to="/favorites" class="nav-item">我的收藏</router-link>
          <router-link to="/orders" class="nav-item">我的订单</router-link>
          <router-link to="/reviews" class="nav-item">我的评价</router-link>
          <router-link to="/after-sales" class="nav-item">退款售后</router-link>
          <router-link to="/messages" class="nav-item">
            消息
            <el-badge v-if="unreadCount > 0" :value="unreadCount" :max="99" class="badge" />
          </router-link>
          <router-link to="/cart" class="nav-item">
            购物车
            <el-badge v-if="cartCount > 0" :value="cartCount" class="badge" />
          </router-link>
        </nav>

        <div class="right">
          <el-dropdown trigger="click" @command="onCommand">
            <span class="user">
              <el-avatar :size="28" :src="userStore.userInfo?.avatar">
                {{ (userStore.nickname || 'U').slice(0, 1) }}
              </el-avatar>
              <span class="nickname">{{ userStore.nickname }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="address">收货地址</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <main class="main">
      <router-view v-slot="{ Component }">
        <component :is="Component" />
      </router-view>
    </main>

    <footer class="footer">
      <span>EatWhat · 今天吃啥 —— 课程设计演示项目</span>
    </footer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { cartApi, messageApi } from '@/api'
import { ArrowDown } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const cartCount = ref(0)
const unreadCount = ref(0)

const role = computed(() => userStore.role)

onMounted(async () => {
  try {
    const cart = await cartApi.get()
    cartCount.value = cart?.items?.reduce((sum, item) => sum + item.quantity, 0) || 0
  } catch {
    cartCount.value = 0
  }
  try {
    const unreadData = await messageApi.unreadCount()
    unreadCount.value = unreadData?.total || 0
  } catch {
    unreadCount.value = 0
  }
})

async function onCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'address') {
    router.push('/address')
  } else if (command === 'logout') {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
    await userStore.logout()
    router.replace('/login')
  }
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  background: #fff;
  box-shadow: 0 1px 8px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 60px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  gap: 32px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 20px;
  font-weight: 700;
  color: var(--ew-primary-dark);
}

.logo-icon {
  font-size: 24px;
}

.nav {
  display: flex;
  gap: 4px;
  flex: 1;
}

.nav-item {
  padding: 8px 14px;
  border-radius: 8px;
  font-size: 15px;
  position: relative;
  transition: all 0.2s;
}

.nav-item:hover {
  background: #fff4ee;
  color: var(--ew-primary-dark);
}

.nav-item.router-link-exact-active {
  color: var(--ew-primary-dark);
  font-weight: 600;
  background: #fff4ee;
}

.badge {
  position: absolute;
  top: 0;
  right: 0;
}

.user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.nickname {
  font-size: 14px;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.main {
  flex: 1;
}

.footer {
  text-align: center;
  padding: 20px;
  color: #b0b3b8;
  font-size: 12px;
}
</style>
