<template>
  <div class="register-page">
    <div class="register-box">
      <div class="brand">
        <span class="logo">🍜</span>
        <h1>注册 EatWhat</h1>
        <p>先选个身份，开启你的 EatWhat 之旅</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" label-position="top">
        <el-form-item label="我是" prop="role">
          <el-radio-group v-model="form.role" class="roles">
            <el-radio-button value="CONSUMER">消费者</el-radio-button>
            <el-radio-button value="MERCHANT">商家</el-radio-button>
            <el-radio-button value="RIDER">骑手</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="11 位手机号" clearable />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="不填自动生成" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="8-20 位，含字母与数字" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="再输一次" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="submit" :loading="loading" @click="submit">注 册</el-button>
        </el-form-item>
      </el-form>

      <div class="footer">
        <span>已有账号？</span>
        <router-link to="/login">去登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  role: 'CONSUMER',
  phone: '',
  nickname: '',
  password: '',
  confirmPassword: ''
})

const rules = {
  role: [{ required: true, message: '请选择身份', trigger: 'change' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*#?&._-]{8,20}$/, message: '8-20 位，需同时包含字母和数字', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authApi.register({ ...form, nickname: form.nickname || undefined })
    // 注册成功后自动登录，免去重复输入密码
    try {
      await userStore.login({ phone: form.phone, password: form.password })
      ElMessage.success('注册成功，已自动登录')
      router.push(nextPath(form.role))
    } catch {
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    }
  } finally {
    loading.value = false
  }
}

function nextPath(role) {
  if (role === 'MERCHANT') return '/merchant/apply'
  if (role === 'RIDER') return '/rider/apply'
  return '/'
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ffb27a 0%, #ff7a45 55%, #f2612c 100%);
  padding: 24px;
}

.register-box {
  width: 440px;
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.16);
}

.brand {
  text-align: center;
  margin-bottom: 20px;
}

.brand .logo {
  font-size: 40px;
}

.brand h1 {
  margin: 6px 0 4px;
  font-size: 22px;
}

.brand p {
  margin: 0;
  color: #909399;
  font-size: 13px;
}

.roles {
  width: 100%;
  display: flex;
}

.roles :deep(.el-radio-button) {
  flex: 1;
}

.roles :deep(.el-radio-button__inner) {
  width: 100%;
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
</style>
