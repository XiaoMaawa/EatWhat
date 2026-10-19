<template>
  <div class="login-page">
    <div class="login-box">
      <div class="brand">
        <span class="logo">🐋</span>
        <h1>EatWhat</h1>
        <p>今天吃啥？本鱼替主人挑好了</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="submit">
        <el-form-item prop="phone">
          <el-input v-model="form.phone" placeholder="手机号" prefix-icon="Iphone" clearable />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="submit" :loading="loading" @click="submit">登 录</el-button>
        </el-form-item>
      </el-form>

      <div class="footer">
        <span>还没有账号？</span>
        <router-link to="/register">立即注册</router-link>
      </div>

      <el-divider><span class="ew-muted">演示账号</span></el-divider>
      <div class="demo">
        <el-tag v-for="item in demoAccounts" :key="item.phone" class="demo-tag" @click="fill(item)">
          {{ item.label }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ phone: '', password: '' })

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const demoAccounts = [
  { label: '消费者', phone: '13800000001', password: 'User@123' },
  { label: '商家', phone: '13800000010', password: 'Merchant@123' },
  { label: '骑手', phone: '13800000020', password: 'Rider@123' },
  { label: '管理员', phone: '13800000000', password: 'Admin@123' }
]

const HOME_BY_ROLE = {
  CONSUMER: '/',
  MERCHANT: '/merchant',
  RIDER: '/rider',
  ADMIN: '/admin'
}

function fill(item) {
  form.phone = item.phone
  form.password = item.password
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const data = await userStore.login({ phone: form.phone, password: form.password })
    ElMessage.success(`欢迎回来，${data.userInfo?.nickname || '主人'}`)
    const redirect = route.query.redirect
    router.replace(redirect || HOME_BY_ROLE[data.userInfo?.role] || '/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ffb27a 0%, #ff7a45 55%, #f2612c 100%);
}

.login-box {
  width: 400px;
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.16);
}

.brand {
  text-align: center;
  margin-bottom: 24px;
}

.brand .logo {
  font-size: 44px;
  line-height: 1;
}

.brand h1 {
  margin: 8px 0 4px;
  font-size: 24px;
  letter-spacing: 1px;
}

.brand p {
  margin: 0;
  color: #909399;
  font-size: 13px;
}

.submit {
  width: 100%;
}

.footer {
  text-align: center;
  font-size: 13px;
  color: #909399;
}

.footer a {
  color: var(--ew-primary-dark);
  margin-left: 4px;
}

.demo {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.demo-tag {
  cursor: pointer;
}
</style>
