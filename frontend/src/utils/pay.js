/**
 * 模拟支付流程。
 *
 * 后端设计是「两段式」：
 *  1. POST /orders/{id}/pay        → 返回 mock 支付链接与订单号，不改订单状态；
 *  2. POST /orders/pay/callback    → 支付网关回调，真正把订单从待支付改为待接单。
 *
 * 因为本项目没有真实支付网关，前端在拿到支付链接后立即模拟一次回调，
 * 否则用户会看到「支付成功」但订单仍停留在待支付状态。
 */
import { orderApi } from '@/api'

/** 生成一个模拟支付流水号，形如 MOCK1792428349811 */
function mockTradeNo() {
  return `MOCK${Date.now()}${Math.floor(Math.random() * 1000)}`
}

/**
 * 对待支付订单发起一次模拟支付。
 * @param {number|string} orderId 订单 ID
 * @param {string} amount 应付金额
 * @returns {Promise<void>}
 */
export async function mockPay(orderId, amount) {
  const payInfo = await orderApi.pay(orderId, 1)
  await orderApi.payCallback({
    orderNo: payInfo.orderNo,
    tradeNo: mockTradeNo(),
    amount: Number(amount)
  })
}
