import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import request from '@/utils/request'

const TOKEN_KEY = 'eatwhat_token'
const REFRESH_KEY = 'eatwhat_refresh_token'
const USER_KEY = 'eatwhat_user'

function readJson(key) {
  try {
    const raw = localStorage.getItem(key)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const refreshToken = ref(localStorage.getItem(REFRESH_KEY) || '')
  const userInfo = ref(readJson(USER_KEY) || {})

  const isLogin = computed(() => Boolean(token.value))
  const role = computed(() => userInfo.value?.role || '')
  const nickname = computed(() => userInfo.value?.nickname || '未登录')

  function persist() {
    if (token.value) {
      localStorage.setItem(TOKEN_KEY, token.value)
    } else {
      localStorage.removeItem(TOKEN_KEY)
    }
    if (refreshToken.value) {
      localStorage.setItem(REFRESH_KEY, refreshToken.value)
    } else {
      localStorage.removeItem(REFRESH_KEY)
    }
    if (userInfo.value && userInfo.value.id) {
      localStorage.setItem(USER_KEY, JSON.stringify(userInfo.value))
    } else {
      localStorage.removeItem(USER_KEY)
    }
  }

  async function login(form) {
    const data = await request.post('/auth/login', form)
    token.value = data.token
    refreshToken.value = data.refreshToken
    userInfo.value = data.userInfo || {}
    persist()
    return data
  }

  async function register(form) {
    return request.post('/auth/register', form)
  }

  async function loadProfile() {
    if (!token.value) return null
    const data = await request.get('/users/me')
    userInfo.value = { ...userInfo.value, ...data }
    persist()
    return data
  }

  function setUserInfo(info) {
    userInfo.value = { ...userInfo.value, ...info }
    persist()
  }

  async function logout() {
    try {
      if (token.value) {
        await request.post('/auth/logout', { refreshToken: refreshToken.value || null })
      }
    } catch {
      // 忽略退出请求异常，本地状态必须清理
    }
    token.value = ''
    refreshToken.value = ''
    userInfo.value = {}
    persist()
  }

  return {
    token,
    refreshToken,
    userInfo,
    isLogin,
    role,
    nickname,
    login,
    register,
    loadProfile,
    setUserInfo,
    logout
  }
})
