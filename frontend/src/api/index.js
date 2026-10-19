import request from '@/utils/request'

// ===== 认证 =====
export const authApi = {
  login: (data) => request.post('/auth/login', data),
  register: (data) => request.post('/auth/register', data),
  refresh: (refreshToken) => request.post('/auth/refresh', { refreshToken }),
  logout: () => request.post('/auth/logout'),
  captcha: () => request.get('/captcha'),
  resetPassword: (data) => request.post('/auth/reset-password', data)
}

// ===== 用户中心 =====
export const userApi = {
  profile: () => request.get('/users/me'),
  updateProfile: (data) => request.put('/users/me', data),
  changePassword: (data) => request.put('/users/me/password', data),
  addresses: () => request.get('/users/me/addresses'),
  addAddress: (data) => request.post('/users/me/addresses', data),
  updateAddress: (id, data) => request.put(`/users/me/addresses/${id}`, data),
  deleteAddress: (id) => request.delete(`/users/me/addresses/${id}`)
}

// ===== 店铺 / 菜品（公开） =====
export const shopApi = {
  list: (params) => request.get('/shops', { params }),
  detail: (id) => request.get(`/shops/${id}`),
  dishes: (id, params) => request.get(`/shops/${id}/dishes`, { params }),
  reviews: (id, params) => request.get(`/shops/${id}/reviews`, { params })
}

// ===== 店铺收藏 =====
export const favoriteApi = {
  list: (params) => request.get('/favorites', { params }),
  add: (shopId) => request.post(`/favorites/${shopId}`),
  remove: (shopId) => request.delete(`/favorites/${shopId}`),
  status: (shopId) => request.get(`/favorites/${shopId}`)
}

// ===== 购物车 =====
export const cartApi = {
  get: () => request.get('/cart'),
  add: (data) => request.post('/cart', data),
  update: (id, quantity) => request.put(`/cart/${id}`, { quantity }),
  remove: (id) => request.delete(`/cart/${id}`),
  clear: () => request.delete('/cart')
}

// ===== 订单 =====
export const orderApi = {
  create: (data) => request.post('/orders', data),
  pay: (id, payType = 1) => request.post(`/orders/${id}/pay`, { payType }),
  list: (params) => request.get('/orders', { params }),
  detail: (id) => request.get(`/orders/${id}`),
  updateStatus: (id, data) => request.patch(`/orders/${id}/status`, data),
  confirm: (id) => request.post(`/orders/${id}/confirm`),
  cancel: (id, reason) => request.patch(`/orders/${id}/status`, { targetStatus: 6, reason })
}

// ===== 商家端 =====
export const merchantApi = {
  apply: (data) => request.post('/merchant/apply', data),
  profile: () => request.get('/merchant/profile'),
  shop: () => request.get('/merchant/shop'),
  updateShop: (data) => request.put('/merchant/shop', data),
  toggleShop: (isOpen) => request.patch('/merchant/shop/status', { isOpen }),

  categories: () => request.get('/merchant/dish-categories'),
  addCategory: (data) => request.post('/merchant/dish-categories', data),
  updateCategory: (id, data) => request.put(`/merchant/dish-categories/${id}`, data),
  deleteCategory: (id) => request.delete(`/merchant/dish-categories/${id}`),

  dishes: (params) => request.get('/merchant/dishes', { params }),
  addDish: (data) => request.post('/merchant/dishes', data),
  updateDish: (id, data) => request.put(`/merchant/dishes/${id}`, data),
  deleteDish: (id) => request.delete(`/merchant/dishes/${id}`),
  updateDishStatus: (id, status) => request.patch(`/merchant/dishes/${id}/status`, { status }),
  batchStatus: (ids, status) => request.patch('/merchant/dishes/batch-status', { ids, status }),

  orders: (params) => request.get('/merchant/orders', { params }),
  accept: (id) => request.patch(`/merchant/orders/${id}/accept`),
  reject: (id, reason) => request.patch(`/merchant/orders/${id}/reject`, { reason }),
  ready: (id) => request.patch(`/merchant/orders/${id}/ready`),
  stats: (type) => request.get('/merchant/stats', { params: { type } }),

  reviews: (params) => request.get('/merchant/reviews', { params }),
  replyReview: (id, reply) => request.post(`/merchant/reviews/${id}/reply`, { reply })
}

// ===== 骑手端 =====
export const riderApi = {
  apply: (data) => request.post('/rider/apply', data),
  profile: () => request.get('/rider/profile'),
  available: () => request.get('/rider/tasks/available'),
  grab: (taskId) => request.post(`/rider/tasks/${taskId}/grab`),
  pickup: (taskId) => request.patch(`/rider/tasks/${taskId}/pickup`),
  deliver: (taskId) => request.patch(`/rider/tasks/${taskId}/deliver`),
  tasks: (params) => request.get('/rider/tasks', { params }),
  updateSettings: (data) => request.patch('/rider/settings', data),
  stats: () => request.get('/rider/stats')
}

// ===== 评价 =====
export const reviewApi = {
  create: (data) => request.post('/reviews', data),
  mine: () => request.get('/reviews/mine'),
  reply: (id, reply) => request.post(`/reviews/${id}/reply`, { reply })
}

// ===== 文件上传 =====
export const fileApi = {
  upload: (file) => {
    const form = new FormData()
    form.append('file', file)
    return request.post('/files/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}

// ===== 管理端 =====
export const adminApi = {
  users: (params) => request.get('/admin/users', { params }),
  userDetail: (id) => request.get(`/admin/users/${id}`),
  updateUserStatus: (id, status) => request.patch(`/admin/users/${id}/status`, { status }),

  merchants: (params) => request.get('/admin/merchants', { params }),
  merchantDetail: (id) => request.get(`/admin/merchants/${id}`),
  auditMerchant: (id, data) => request.patch(`/admin/merchants/${id}/audit`, data),
  freezeMerchant: (id, status) => request.patch(`/admin/merchants/${id}/freeze`, { status }),

  riders: (params) => request.get('/admin/riders', { params }),
  riderDetail: (id) => request.get(`/admin/riders/${id}`),
  auditRider: (id, data) => request.patch(`/admin/riders/${id}/audit`, data),

  orders: (params) => request.get('/admin/orders', { params }),
  orderDetail: (id) => request.get(`/admin/orders/${id}`),
  intervene: (id, data) => request.post(`/admin/orders/${id}/intervene`, data),

  dishes: (params) => request.get('/admin/dishes', { params }),
  offlineDish: (id) => request.patch(`/admin/dishes/${id}/offline`),

  dashboard: () => request.get('/admin/dashboard'),
  operationLogs: (params) => request.get('/admin/operation-logs', { params })
}
