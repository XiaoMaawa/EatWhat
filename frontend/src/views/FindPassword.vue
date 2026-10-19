<template>
  <div class="login-page">
    <div class="login-box">
      <div class="brand">
        <span class="logo">🐋</span>
        <h1>找回密码</h1>
        <p>输入手机号与验证码，就能重新设密码啦</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="submit">
        <el-form-item prop="phone">
          <el-input v-model="form.phone" placeholder="注册时的手机号" prefix-icon="Iphone" clearable />
        </el-form-item>

        <el-form-item prop="captchaCode">
          <div class="captcha-row">
            <el-input v-model="form.captchaCode" placeholder="图形验证码" prefix-icon="Key" maxlength="4" />
            <img
              v-if="captchaImage"
              :src="captchaImage"
              class="captcha-img"
              title="点击刷新"
              @click="loadCaptcha"
            />
            <el-button v-else size="small" @click="loadCaptcha">获取</el-button>
          </div>
        </el-form-item>

        <el-form-item prop="newPassword">
          <el-input
            v-model="form.newPassword"
            type="password"
            placeholder="新密码（6-20 位，含字母与数字）"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="确认新密码"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" class="submit" :loading="loading" @click="submit">重置密码</el-button>
        </el-form-item>
      </el-form>

      <div class="footer">
        <span>想起来了？</span>
        <router-link to="/login">返回登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const captchaImage = ref('')

const form = reactive({
  phone: '',
  captchaKey: '',
  captchaCode: '',
  newPassword: '',
  confirmPassword: ''
})

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  captchaCode: [{ required: true, message: '请输入图形验证码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需在 6 至 20 位之间', trigger: 'blur' },
    {
      pattern: /^(?=.*[A-Za-z])(?=.*\d).+$/,
      message: '密码需同时包含字母与数字',
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== form.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

async function loadCaptcha() {
  const data = await authApi.captcha().catch(() => null)
  if (data) {
    captchaImage.value = data.captchaImage
    form.captchaKey = data.captchaKey
    form.captchaCode = ''
  }
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  loading.value = true
  try {
    await authApi.resetPassword({ ...form })
    ElMessage.success('密码已重置，请用新密码登录')
    router.replace('/login')
  } catch (error) {
    if (error?.code === 1007 || error?.code === 3002) {
      await loadCaptcha()
    }
    throw error
  } finally {
    loading.value = false
  }
}

onMounted(loadCaptcha)
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
  font-size: 40px;
  line-height: 1;
}

.brand h1 {
  margin: 8px 0 4px;
  font-size: 22px;
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

.captcha-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.captcha-img {
  height: 40px;
  width: 110px;
  border-radius: 6px;
  border: 1px solid var(--ew-border);
  cursor: pointer;
  object-fit: cover;
}
</style>
