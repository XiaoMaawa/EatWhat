/** 订单状态字典：与后端 OrderStatus 枚举保持一致 */
export const ORDER_STATUS = {
  0: { text: '待支付', type: 'warning', actions: ['pay', 'cancel'] },
  1: { text: '待接单', type: 'primary', actions: ['cancel'] },
  2: { text: '备餐中', type: 'info', actions: [] },
  3: { text: '待配送', type: 'info', actions: [] },
  4: { text: '配送中', type: 'success', actions: ['confirm'] },
  5: { text: '已完成', type: 'info', actions: ['review'] },
  6: { text: '已取消', type: 'info', actions: [] },
  7: { text: '退款中', type: 'danger', actions: [] }
}

export const ORDER_STATUS_OPTIONS = Object.entries(ORDER_STATUS).map(([value, item]) => ({
  value: Number(value),
  label: item.text
}))

export function orderStatusText(status) {
  return ORDER_STATUS[status]?.text || '未知状态'
}

export function orderStatusType(status) {
  return ORDER_STATUS[status]?.type || 'info'
}

/** 商家端订单操作按钮 */
export const MERCHANT_ACTIONS = {
  1: [{ key: 'accept', label: '接单', type: 'success' }, { key: 'reject', label: '拒单', type: 'danger' }],
  2: [{ key: 'ready', label: '出餐完成', type: 'success' }]
}

/** 配送任务状态 */
export const TASK_STATUS = {
  0: '待抢单',
  1: '已接单',
  2: '已取餐',
  3: '已送达'
}

/** 审核状态 */
export const AUDIT_STATUS = {
  0: { text: '待审核', type: 'warning' },
  1: { text: '已通过', type: 'success' },
  2: { text: '已驳回', type: 'danger' }
}

/** 菜品状态 */
export const DISH_STATUS = {
  0: { text: '已下架', type: 'info' },
  1: { text: '在售', type: 'success' },
  2: { text: '售罄', type: 'warning' }
}

export const ROLE_TEXT = {
  CONSUMER: '消费者',
  MERCHANT: '商家',
  RIDER: '骑手',
  ADMIN: '管理员'
}
