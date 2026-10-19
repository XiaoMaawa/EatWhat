<template>
  <div class="ew-page">
    <div class="ew-card">
      <div class="ew-flex-between">
        <h3 class="ew-title">我的订单</h3>
        <el-button link type="primary" :loading="loading" @click="load">刷新</el-button>
      </div>

      <el-tabs v-model="status" @tab-change="onTabChange">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="待支付" name="0" />
        <el-tab-pane label="待接单" name="1" />
        <el-tab-pane label="进行中" name="2,3,4" />
        <el-tab-pane label="已完成" name="5" />
        <el-tab-pane label="已取消" name="6" />
      </el-tabs>

      <div v-loading="loading" class="orders">
        <div v-for="order in orders" :key="order.id" class="order">
          <div class="order-head">
            <div class="ew-flex ew-gap12">
              <span class="shop-logo">🍜</span>
              <div>
                <div class="shop-name">{{ order.shopName }}</div>
                <div class="ew-muted">订单号 {{ order.orderNo }}</div>
              </div>
            </div>
            <el-tag :type="orderStatusType(order.status)">{{ order.statusDesc || orderStatusText(order.status) }}</el-tag>
          </div>

          <div class="order-body">
            <div class="items-text">{{ order.itemSummary || '—' }}</div>
            <div class="amount">
              <span class="ew-muted">实付</span>
              <span class="ew-price">{{ order.totalAmount }}</span>
            </div>
          </div>

          <div class="order-foot">
            <span class="ew-muted">{{ order.createdAt }}</span>
            <div class="ew-flex ew-gap8">
              <el-button size="small" @click="$router.push(`/orders/${order.id}`)">详情</el-button>
              <el-button
                v-if="order.status === 0"
                size="small"
                type="primary"
                @click="pay(order)"
              >
                去支付
              </el-button>
              <el-button
                v-if="order.status === 0 || order.status === 1"
                size="small"
                @click="cancel(order)"
              >
                取消
              </el-button>
              <el-button
                v-if="order.status === 4"
                size="small"
                type="success"
                @click="confirm(order)"
              >
                确认收货
              </el-button>
              <el-button
                v-if="order.status === 5"
                size="small"
                type="warning"
                @click="goReview(order)"
              >
                去评价
              </el-button>
            </div>
          </div>
        </div>

        <el-empty v-if="!loading && orders.length === 0" description="还没有订单" />
      </div>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          :page-size="10"
          :total="total"
          layout="prev, pager, next"
          background
          @current-change="load"
        />
      </div>
    </div>

    <!-- 评价弹窗 -->
    <el-dialog v-model="reviewVisible" title="评价订单" width="480px">
      <el-form label-width="80px">
        <el-form-item label="店铺评分">
          <el-rate v-model="reviewForm.shopScore" />
        </el-form-item>
        <el-form-item label="配送评分">
          <el-rate v-model="reviewForm.deliveryScore" />
        </el-form-item>
        <el-form-item label="评价内容">
          <el-input
            v-model="reviewForm.content"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="10-500 字，说说这次的体验"
          />
        </el-form-item>
        <el-form-item label="匿名评价">
          <el-switch v-model="reviewForm.isAnonymous" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewSubmitting" @click="submitReview">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderApi, reviewApi } from '@/api'
import { orderStatusText, orderStatusType } from '@/utils/orderStatus'

const loading = ref(false)
const orders = ref([])
const total = ref(0)
const page = ref(1)
const status = ref('all')

const reviewVisible = ref(false)
const reviewSubmitting = ref(false)
const reviewOrderId = ref(null)
const reviewForm = reactive({
  shopScore: 5,
  deliveryScore: 5,
  content: '',
  isAnonymous: 0
})

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, size: 10 }
    if (status.value !== 'all') {
      params.status = status.value
    }
    const data = await orderApi.list(params)
    orders.value = data?.records || []
    total.value = data?.total || 0
  } finally {
    loading.value = false
  }
}

function onTabChange() {
  page.value = 1
  load()
}

async function pay(order) {
  await ElMessageBox.confirm(
    `本次需支付 ¥${order.totalAmount}，确认支付？`,
    '模拟支付',
    { type: 'info' }
  )
  await orderApi.pay(order.id, 1)
  ElMessage.success('支付成功')
  await load()
}

async function cancel(order) {
  const { value } = await ElMessageBox.prompt('请输入取消原因', '取消订单', {
    inputValue: '不想要了',
    inputValidator: (v) => (v && v.length <= 100 ? true : '请填写 100 字以内的原因')
  })
  await orderApi.cancel(order.id, value)
  ElMessage.success('订单已取消')
  await load()
}

async function confirm(order) {
  await ElMessageBox.confirm('确认已收到餐品？', '确认收货', { type: 'info' })
  await orderApi.confirm(order.id)
  ElMessage.success('已确认收货')
  await load()
}

function goReview(order) {
  reviewOrderId.value = order.id
  reviewForm.shopScore = 5
  reviewForm.deliveryScore = 5
  reviewForm.content = ''
  reviewForm.isAnonymous = 0
  reviewVisible.value = true
}

async function submitReview() {
  if (reviewForm.content.trim().length < 10) {
    ElMessage.warning('评价内容至少 10 个字')
    return
  }
  reviewSubmitting.value = true
  try {
    await reviewApi.create({ orderId: reviewOrderId.value, ...reviewForm })
    ElMessage.success('评价成功，感谢反馈')
    reviewVisible.value = false
    await load()
  } finally {
    reviewSubmitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.orders {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 120px;
}

.order {
  border: 1px solid var(--ew-border);
  border-radius: 10px;
  overflow: hidden;
}

.order-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  background: #fafbfc;
}

.shop-logo {
  font-size: 22px;
}

.shop-name {
  font-weight: 600;
  font-size: 15px;
}

.order-body {
  padding: 12px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.items-text {
  flex: 1;
  font-size: 14px;
  color: #606266;
}

.amount {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.amount .ew-price {
  font-size: 18px;
}

.order-foot {
  padding: 10px 14px;
  border-top: 1px solid var(--ew-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
