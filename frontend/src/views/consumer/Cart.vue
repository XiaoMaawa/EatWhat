<template>
  <div class="ew-page" v-loading="loading">
    <div class="ew-card">
      <h3 class="ew-title">购物车</h3>

      <template v-if="cart.items?.length">
        <div class="shop-row">
          <span class="shop-name">{{ cart.shopName }}</span>
          <el-button link type="danger" @click="clearCart">清空购物车</el-button>
        </div>

        <div class="items">
          <div v-for="item in cart.items" :key="item.id" class="item">
            <div class="item-img">
              <img v-if="item.dishImage" :src="item.dishImage" alt="" />
              <span v-else>🍱</span>
            </div>
            <div class="item-info">
              <div class="item-name">{{ item.dishName }}</div>
              <div class="ew-muted">单价 ¥{{ item.price }} · 库存 {{ item.stock }}</div>
              <el-tag v-if="!item.available" size="small" type="warning">暂不可售</el-tag>
            </div>
            <div class="item-actions">
              <span class="ew-price">{{ (item.price * item.quantity).toFixed(2) }}</span>
              <el-input-number
                v-model="item.quantity"
                :min="1"
                :max="Math.min(item.stock || 99, 99)"
                size="small"
                @change="(val) => updateQuantity(item, val)"
              />
              <el-button link type="danger" @click="removeItem(item)">删除</el-button>
            </div>
          </div>
        </div>

        <div class="summary">
          <div class="summary-line">
            <span class="ew-muted">商品金额</span>
            <span>¥{{ cart.goodsAmount }}</span>
          </div>
          <div class="summary-line">
            <span class="ew-muted">配送费</span>
            <span>¥{{ cart.deliveryFee }}</span>
          </div>
          <div class="summary-line total">
            <span>合计</span>
            <span class="ew-price">{{ cart.totalAmount }}</span>
          </div>
          <div v-if="belowMin" class="tip">
            差 ¥{{ (Number(cart.minOrderAmount) - Number(cart.goodsAmount)).toFixed(2) }} 起送，再点一些即可下单
          </div>
        </div>

        <div class="address-section">
          <div class="ew-flex-between">
            <span class="section-title">收货地址</span>
            <el-button link type="primary" @click="$router.push('/address')">管理地址</el-button>
          </div>
          <el-radio-group v-model="addressId" class="address-list" @change="refreshFee">
            <el-radio v-for="addr in addresses" :key="addr.id" :value="addr.id" class="address-item">
              <span class="addr-name">{{ addr.contactName }} {{ addr.phone }}</span>
              <span class="ew-muted">
                {{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detail }}
              </span>
              <el-tag v-if="addr.isDefault === 1" size="small" type="success">默认</el-tag>
            </el-radio>
          </el-radio-group>
          <el-empty v-if="addresses.length === 0" description="还没有收货地址" :image-size="60" />
        </div>

        <div class="remark">
          <span class="section-title">订单备注</span>
          <el-input v-model="remark" type="textarea" :rows="2" maxlength="200" show-word-limit placeholder="口味、偏好等要求（选填）" />
          <div class="tableware">
            <span class="section-title">餐具份数</span>
            <el-input-number v-model="tablewareCount" :min="0" :max="20" size="small" />
            <span class="ew-muted">0 表示不需要一次性餐具</span>
          </div>
        </div>

        <div class="footer">
          <span>
            合计 <span class="ew-price total-price">{{ cart.totalAmount }}</span>
          </span>
          <el-button
            type="primary"
            size="large"
            :disabled="belowMin || !addressId"
            :loading="submitting"
            @click="submit"
          >
            提交订单
          </el-button>
        </div>
      </template>

      <el-empty v-else description="购物车还是空的，去挑几道喜欢的菜吧">
        <el-button type="primary" @click="$router.push('/')">去逛逛</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cartApi, orderApi, userApi } from '@/api'

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const cart = ref({ items: [] })
const addresses = ref([])
const addressId = ref(null)
const remark = ref('')
const tablewareCount = ref(0)

const belowMin = computed(() => {
  const goods = Number(cart.value.goodsAmount || 0)
  const min = Number(cart.value.minOrderAmount || 0)
  return goods < min
})

async function load() {
  loading.value = true
  try {
    addresses.value = (await userApi.addresses()) || []
    const def = addresses.value.find((a) => a.isDefault === 1)
    addressId.value = def?.id || addresses.value[0]?.id || null
    cart.value = await cartApi.get(addressId.value)
  } finally {
    loading.value = false
  }
}

// 切换收货地址后重新试算配送费与合计
async function refreshFee() {
  cart.value = await cartApi.get(addressId.value)
}

async function updateQuantity(item, quantity) {
  if (!quantity) return
  await cartApi.update(item.id, quantity)
  await load()
}

async function removeItem(item) {
  await cartApi.remove(item.id)
  ElMessage.success('已删除')
  await load()
}

async function clearCart() {
  await ElMessageBox.confirm('确定清空购物车吗？', '提示', { type: 'warning' })
  await cartApi.clear()
  await load()
}

async function submit() {
  submitting.value = true
  try {
    const data = await orderApi.create({
      shopId: cart.value.shopId,
      addressId: addressId.value,
      remark: remark.value || undefined,
      tablewareCount: tablewareCount.value
    })
    ElMessage.success('下单成功，请尽快支付')
    router.push(`/orders/${data.orderId}`)
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.shop-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--ew-border);
}

.shop-name {
  font-size: 15px;
  font-weight: 600;
}

.items {
  display: flex;
  flex-direction: column;
}

.item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--ew-border);
}

.item-img {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  background: #fafafa;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  flex-shrink: 0;
  overflow: hidden;
}

.item-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.item-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.item-name {
  font-weight: 600;
}

.item-actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.summary {
  padding: 14px 0;
  border-bottom: 1px solid var(--ew-border);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.summary-line {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
}

.summary-line.total {
  font-weight: 600;
  font-size: 15px;
  padding-top: 4px;
}

.tip {
  text-align: right;
  color: #e6a23c;
  font-size: 13px;
}

.address-section,
.remark {
  padding: 14px 0;
  border-bottom: 1px solid var(--ew-border);
}

.section-title {
  font-weight: 600;
  font-size: 15px;
}

.address-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 10px;
  width: 100%;
}

.address-item {
  height: auto;
  padding: 8px 10px;
  border: 1px solid var(--ew-border);
  border-radius: 8px;
  width: 100%;
  margin-right: 0;
}

.address-item.is-checked {
  border-color: var(--ew-primary);
  background: #fff8f4;
}

.addr-name {
  font-weight: 600;
  margin-right: 8px;
}

.remark {
  display: flex;
  flex-direction: column;
  gap: 8px;
  border-bottom: none;
}

.footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 18px;
  padding-top: 16px;
}

.total-price {
  font-size: 22px;
}
.tableware {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}
</style>
