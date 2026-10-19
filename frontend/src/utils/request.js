import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/stores/user'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 15000
})

// 请求拦截：注入访问令牌
request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截：统一解包 R<T>
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 0) {
      return res.data
    }

    // 令牌失效：清理登录态并跳转登录页
    if (res.code === 1004 || res.code === 1005 || res.code === 2000) {
      const userStore = useUserStore()
      userStore.logout()
      const current = router.currentRoute.value
      if (current.path !== '/login') {
        ElMessage.error(res.message || '登录已过期，请重新登录')
        router.replace({ path: '/login', query: { redirect: current.fullPath } })
      }
      return Promise.reject(new Error(res.message))
    }

    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    const status = error.response?.status
    const body = error.response?.data
    // 认证失效（HTTP 401 或业务码 1004/1005/2000）：清理登录态并跳转登录页
    if (status === 401 || body?.code === 1004 || body?.code === 1005 || body?.code === 2000) {
      const userStore = useUserStore()
      userStore.logout()
      const current = router.currentRoute.value
      if (current.path !== '/login') {
        ElMessage.error(body?.message || '登录已过期，请重新登录')
        router.replace({ path: '/login', query: { redirect: current.fullPath } })
      }
      return Promise.reject(new Error(body?.message || '登录已过期，请重新登录'))
    }
    if (status === 403) {
      ElMessage.error(body?.message || '无访问权限')
    } else if (status === 404) {
      ElMessage.error(body?.message || '请求的资源不存在')
    } else if (status >= 500) {
      ElMessage.error('服务器开小差了，请稍后重试')
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请检查网络')
    } else {
      ElMessage.error(body?.message || error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

export default request
