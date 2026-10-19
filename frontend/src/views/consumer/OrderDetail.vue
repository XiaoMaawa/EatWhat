<template>
  <div class="ew-page" v-loading="loading">
    <div class="ew-card" v-if="order">
      <div class="ew-flex-between">
        <h3 class="ew-title">订单详情</h3>
        <el-tag :type="orderStatusType(order.status)" size="large">
          {{ order.statusDesc || orderStatusText(order.status) }}
        </el-tag>
      </div>

      <el-steps :active="stepActive" finish-status="success" class="steps">
        <el-step title="已下单" :description="order.createdAt" />
        <el-step title="已支付" :description="order.payTime || '待支付'" />
        <el-step title="商家接单" :description="order.status >= 1 ? '已接单' : '等待中'" />
        <el-step title="骑手配送" :description="order.status >= 4 ? '配送中' : '等待中'" />
        <el-step title="已完成" :description="order.completedAt || '—'" />
      </el-steps>

      <el-divider />

      <div class="section">
        <span class="label">店铺</span>
        <span>{{ order.shop?.name }} <span class="ew-muted">（{{ order.shop?.phone }}）</span></span>
      </div>

      <div class="section">
        <span class="label">收货信息</span>
        <span>{{ order.receiverName }} {{ order.receiverPhone }}</span>
      </div>
      <div class="section">
        <span class="label">收货地址</span>
        <span>{{ order.receiverAddress }}</span>
      </div>

      <el-divider />

      <div class="items">
        <div v-for="item in order.items" :key="item.dishId" class="item">
          <span class="item-name">{{ item.dishName }}</span>
          <span class="ew-muted">× {{ item.quantity }}</span>
          <span class="item-price">¥{{ item.subtotal }}</span>
        </div>
      </div>

      <div class="amounts">
        <div class="row"><span class="ew-muted">商品金额</span><span>¥{{ order.goodsAmount }}</span></div>
        <div class="row"><span class="ew-muted">配送费</span><span>¥{{ order.deliveryFee }}</span></div>
        <div class="row total"><span>实付金额</span><span class="ew-price">{{ order.totalAmount }}</span></div>
      </div>

      <div v-if="order.remark" class="section ew-mt12">
        <span class="label">备注</span>
        <span>{{ order.remark }}</span>
      </div>
      <div v-if="order.tablewareCount > 0" class="section ew-mt12">
        <span class="label">餐具</span>
        <span>{{ order.tablewareCount }} 份</span>
      </div>

      <div v-if="order.rider" class="section">
        <span class="label">骑手</span>
        <span>{{ order.rider.name }} {{ order.rider.phone }}</span>
      </div>

      <el-divider />

      <h4 class="sub-title">订单进度</h4>
      <el-timeline>
        <el-timeline-item
          v-for="(log, index) in order.statusLogs || []"
          :key="index"
          :timestamp="log.createdAt"
          :type="index === 0 ? 'primary' : ''"
        >
          {{ log.statusDesc || orderStatusText(log.toStatus) }}
          <span v-if="log.remark" class="ew-muted"> · {{ log.remark }}</span>
        </el-timeline-item>
      </el-timeline>

      <div class="actions">
        <el-button v-if="order.status === 0" type="primary" @click="pay">去支付</el-button>
        <el-button v-if="order.status === 0 || order.status === 1" @click="cancel">取消订单</el-button>
        <el-button v-if="order.status === 4" type="success" @click="confirm">确认收货</el-button>
        <el-button @click="$router.back()">返回</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderApi } from '@/api'
import { orderStatusText, orderStatusType } from '@/utils/orderStatus'

const route = useRoute()
const orderId = Number(route.params.id)
const loading = ref(false)
const order = ref(null)

const stepActive = computed(() => {
  const status = order.value?.status ?? 0
  if (status === 0) return 1
  if (status === 1) return 2
  if (status === 2 || status === 3) return 3
  if (status === 4) return 4
  if (status === 5) return 5
  return 1
})

async function load() {
  loading.value = true
  try {
    order.value = await orderApi.detail(orderId)
  } finally {
    loading.value = false
  }
}

async function pay() {
  await ElMessageBox.confirm(`本次需支付 ¥${order.value.totalAmount}，确认支付？`, '模拟支付', { type: 'info' })
  await orderApi.pay(orderId, 1)
  ElMessage.success('支付成功')
  await load()
}

async function cancel() {
  const { value } = await ElMessageBox.prompt('请输入取消原因', '取消订单', {
    inputValue: '不想要了',
    inputValidator: (v) => (v && v.length <= 100 ? true : '请填写 100 字以内的原因')
  })
  await orderApi.cancel(orderId, value)
  ElMessage.success('订单已取消')
  await load()
}

async function confirm() {
  await ElMessageBox.confirm('确认已收到餐品？', '确认收货', { type: 'info' })
  await orderApi.confirm(orderId)
  ElMessage.success('已确认收货')
  await load()
}

onMounted(load)
</script>

<style scoped>
.steps {
  margin-bottom: 8px;
}

.section {
  display: flex;
  gap: 12px;
  font-size: 14px;
  padding: 5px 0;
}

.label {
  width: 76px;
  color: #909399;
  flex-shrink: 0;
}

.items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
}

.item-name {
  flex: 1;
}

.item-price {
  min-width: 70px;
  text-align: right;
}

.amounts {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: flex-end;
}

.row {
  display: flex;
  gap: 24px;
  font-size: 14px;
}

.row.total {
  font-size: 16px;
  font-weight: 600;
  align-items: baseline;
}

.sub-title {
  margin: 0 0 12px;
  font-size: 15px;
}

.actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 8px;
}
</style>
