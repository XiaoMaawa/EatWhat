import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册', public: true }
  },
  {
    path: '/find-password',
    name: 'findPassword',
    component: () => import('@/views/FindPassword.vue'),
    meta: { title: '找回密码', public: true }
  },
  {
    path: '/',
    component: () => import('@/layouts/ConsumerLayout.vue'),
    meta: { roles: ['CONSUMER'] },
    children: [
      { path: '', name: 'home', component: () => import('@/views/consumer/Home.vue'), meta: { title: '首页' } },
      { path: 'shop/:id', name: 'shopDetail', component: () => import('@/views/consumer/ShopDetail.vue'), meta: { title: '店铺详情' } },
      { path: 'cart', name: 'cart', component: () => import('@/views/consumer/Cart.vue'), meta: { title: '购物车' } },
      { path: 'orders', name: 'orders', component: () => import('@/views/consumer/Orders.vue'), meta: { title: '我的订单' } },
      { path: 'orders/:id', name: 'orderDetail', component: () => import('@/views/consumer/OrderDetail.vue'), meta: { title: '订单详情' } },
      { path: 'reviews', name: 'myReviews', component: () => import('@/views/consumer/Reviews.vue'), meta: { title: '我的评价' } },
      { path: 'favorites', name: 'favorites', component: () => import('@/views/consumer/Favorites.vue'), meta: { title: '我的收藏' } },
      { path: 'after-sales', name: 'afterSales', component: () => import('@/views/consumer/AfterSales.vue'), meta: { title: '退款售后' } },
      { path: 'messages', name: 'messages', component: () => import('@/views/consumer/Messages.vue'), meta: { title: '消息中心' } },
      { path: 'profile', name: 'profile', component: () => import('@/views/consumer/Profile.vue'), meta: { title: '个人中心' } },
      { path: 'address', name: 'address', component: () => import('@/views/consumer/Address.vue'), meta: { title: '收货地址' } }
    ]
  },
  {
    path: '/merchant',
    component: () => import('@/layouts/MerchantLayout.vue'),
    meta: { roles: ['MERCHANT', 'ADMIN'] },
    children: [
      { path: '', redirect: '/merchant/dashboard' },
      { path: 'apply', name: 'merchantApply', component: () => import('@/views/merchant/Apply.vue'), meta: { title: '商家入驻' } },
      { path: 'dashboard', name: 'merchantDashboard', component: () => import('@/views/merchant/Dashboard.vue'), meta: { title: '经营概览' } },
      { path: 'orders', name: 'merchantOrders', component: () => import('@/views/merchant/Orders.vue'), meta: { title: '订单管理' } },
      { path: 'dishes', name: 'merchantDishes', component: () => import('@/views/merchant/Dishes.vue'), meta: { title: '菜品管理' } },
      { path: 'shop', name: 'merchantShop', component: () => import('@/views/merchant/Shop.vue'), meta: { title: '店铺管理' } },
      { path: 'after-sales', name: 'merchantAfterSales', component: () => import('@/views/merchant/AfterSales.vue'), meta: { title: '退款售后' } },
      { path: 'reviews', name: 'merchantReviews', component: () => import('@/views/merchant/Reviews.vue'), meta: { title: '评价管理' } },
      { path: 'analytics', name: 'merchantAnalytics', component: () => import('@/views/merchant/Analytics.vue'), meta: { title: '经营分析' } },
      { path: 'preview', name: 'merchantPreview', component: () => import('@/views/merchant/Preview.vue'), meta: { title: '顾客视角预览' } },
      { path: 'qrcode', name: 'merchantQrCode', component: () => import('@/views/merchant/QrCode.vue'), meta: { title: '店铺二维码' } },
      { path: 'account', name: 'merchantAccount', component: () => import('@/views/common/Account.vue'), meta: { title: '账号设置' } }
    ]
  },
  {
    path: '/rider',
    component: () => import('@/layouts/RiderLayout.vue'),
    meta: { roles: ['RIDER', 'ADMIN'] },
    children: [
      { path: '', redirect: '/rider/hall' },
      { path: 'apply', name: 'riderApply', component: () => import('@/views/rider/Apply.vue'), meta: { title: '骑手认证' } },
      { path: 'hall', name: 'riderHall', component: () => import('@/views/rider/Hall.vue'), meta: { title: '抢单大厅' } },
      { path: 'tasks', name: 'riderTasks', component: () => import('@/views/rider/Tasks.vue'), meta: { title: '我的任务' } },
      { path: 'exceptions', name: 'riderExceptions', component: () => import('@/views/rider/Exceptions.vue'), meta: { title: '异常记录' } },
      { path: 'stats', name: 'riderStats', component: () => import('@/views/rider/Stats.vue'), meta: { title: '收益统计' } },
      { path: 'notices', name: 'riderNotices', component: () => import('@/views/rider/Notices.vue'), meta: { title: '系统通知' } },
      { path: 'messages', name: 'riderMessages', component: () => import('@/views/rider/Messages.vue'), meta: { title: '订单消息' } },
      { path: 'account', name: 'riderAccount', component: () => import('@/views/common/Account.vue'), meta: { title: '账号设置' } }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { roles: ['ADMIN'] },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'adminDashboard', component: () => import('@/views/admin/Dashboard.vue'), meta: { title: '数据看板' } },
      { path: 'admins', name: 'adminAccounts', component: () => import('@/views/admin/Admins.vue'), meta: { title: '管理员账号' } },
      { path: 'statistics', name: 'adminStatistics', component: () => import('@/views/admin/Statistics.vue'), meta: { title: '平台统计' } },
      { path: 'daily-stats', name: 'adminDailyStats', component: () => import('@/views/admin/DailyStats.vue'), meta: { title: '每日汇总' } },
      { path: 'users', name: 'adminUsers', component: () => import('@/views/admin/Users.vue'), meta: { title: '用户管理' } },
      { path: 'merchants', name: 'adminMerchants', component: () => import('@/views/admin/Merchants.vue'), meta: { title: '商家审核' } },
      { path: 'riders', name: 'adminRiders', component: () => import('@/views/admin/Riders.vue'), meta: { title: '骑手审核' } },
      { path: 'orders', name: 'adminOrders', component: () => import('@/views/admin/Orders.vue'), meta: { title: '订单监管' } },
      { path: 'dishes', name: 'adminDishes', component: () => import('@/views/admin/Dishes.vue'), meta: { title: '菜品监管' } },
      { path: 'after-sales', name: 'adminAfterSales', component: () => import('@/views/admin/AfterSales.vue'), meta: { title: '售后仲裁' } },
      { path: 'delivery-exceptions', name: 'adminDeliveryExceptions', component: () => import('@/views/admin/DeliveryExceptions.vue'), meta: { title: '配送异常' } },
      { path: 'logs', name: 'adminLogs', component: () => import('@/views/admin/OperationLogs.vue'), meta: { title: '操作日志' } },
      { path: 'notices', name: 'adminNotices', component: () => import('@/views/admin/Notices.vue'), meta: { title: '发布公告' } },
    { path: 'announcements', name: 'adminAnnouncements', component: () => import('@/views/admin/Announcements.vue'), meta: { title: '公告管理' } },
    { path: 'sensitive-words', name: 'adminSensitiveWords', component: () => import('@/views/admin/SensitiveWords.vue'), meta: { title: '敏感词管理' } },
    { path: 'configs', name: 'adminConfigs', component: () => import('@/views/admin/Configs.vue'), meta: { title: '平台参数' } },
    { path: 'account', name: 'adminAccount', component: () => import('@/views/common/Account.vue'), meta: { title: '账号设置' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

const HOME_BY_ROLE = {
  CONSUMER: '/',
  MERCHANT: '/merchant',
  RIDER: '/rider',
  ADMIN: '/admin'
}

router.beforeEach((to, from, next) => {
  document.title = to.meta?.title ? `${to.meta.title} · EatWhat` : 'EatWhat · 今天吃啥'

  const userStore = useUserStore()
  const roles = to.meta?.roles

  if (!roles) {
    return next()
  }

  if (!userStore.isLogin) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }

  if (!roles.includes(userStore.role)) {
    // 角色不匹配：引导到自身角色的首页
    return next(HOME_BY_ROLE[userStore.role] || '/login')
  }

  return next()
})

export default router
