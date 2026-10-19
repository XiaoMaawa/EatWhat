<template>
  <div class="ew-page" v-loading="loading">
    <el-alert
      v-if="profile && profile.auditStatus !== 1"
      type="warning"
      show-icon
      :closable="false"
      class="audit-alert"
      :title="profile.auditStatus === 0 ? '资质审核中，审核通过后即可营业' : '资质审核未通过'"
    >
      <template #default>
        {{ profile.auditRemark || '请联系平台管理员补充资料' }}
      </template>
    </el-alert>

    <div class="ew-card">
      <div class="ew-flex-between">
        <h3 class="ew-title">经营概览</h3>
        <el-radio-group v-model="type" size="small" @change="loadStats">
          <el-radio-button value="day">今日</el-radio-button>
          <el-radio-button value="week">近 7 天</el-radio-button>
          <el-radio-button value="month">本月</el-radio-button>
        </el-radio-group>
      </div>

      <div class="stats">
        <div class="stat">
          <div class="stat-label">营业额</div>
          <div class="stat-value ew-price">¥{{ money(stats.totalAmount) }}</div>
        </div>
        <div class="stat">
          <div class="stat-label">订单量</div>
          <div class="stat-value">{{ stats.totalOrders ?? 0 }}</div>
        </div>
        <div class="stat">
          <div class="stat-label">客单价</div>
          <div class="stat-value ew-price">¥{{ money(stats.avgOrderAmount) }}</div>
        </div>
        <div class="stat">
          <div class="stat-label">店铺状态</div>
          <div class="stat-value">
            <el-tag :type="shop?.isOpen === 1 ? 'success' : 'info'">
              {{ shop?.isOpen === 1 ? '营业中' : '已打烊' }}
            </el-tag>
          </div>
        </div>
      </div>
    </div>

    <div class="ew-card">
      <h3 class="ew-title">热销菜品 TOP 5</h3>
      <el-table :data="stats.hotDishes || []" size="default">
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="dishName" label="菜品" />
        <el-table-column prop="sales" label="销量" width="120" />
        <el-table-column label="占比" width="220">
          <template #default="{ row }">
            <el-progress :percentage="percentOf(row.sales)" :stroke-width="12" />
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!(stats.hotDishes || []).length" description="暂无销量数据" :image-size="70" />
    </div>

    <div class="ew-card">
      <h3 class="ew-title">待处理订单</h3>
      <el-table :data="pendingOrders" size="default">
        <el-table-column prop="orderNo" label="订单号" width="200" />
        <el-table-column label="菜品" show-overflow-tooltip>
          <template #default="{ row }">{{ row.itemSummary || '—' }}</template>
        </el-table-column>
        <el-table-column label="金额" width="110">
          <template #default="{ row }">¥{{ money(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="orderStatusType(row.status)">{{ orderStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" size="small" type="success" @click="accept(row)">接单</el-button>
            <el-button v-if="row.status === 2" size="small" type="primary" @click="ready(row)">出餐</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!pendingOrders.length" description="暂无待处理订单" :image-size="70" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { merchantApi } from '@/api'
import { money } from '@/utils/format'
import { orderStatusText, orderStatusType } from '@/utils/orderStatus'

const loading = ref(false)
const type = ref('day')
const stats = ref({})
const shop = ref(null)
const profile = ref(null)
const pendingOrders = ref([])

const maxSales = computed(() =>
  Math.max(1, ...(stats.value.hotDishes || []).map((d) => Number(d.sales || 0)))
)

function percentOf(sales) {
  return Math.round((Number(sales || 0) / maxSales.value) * 100)
}

async function loadStats() {
  loading.value = true
  try {
    stats.value = (await merchantApi.stats(type.value)) || {}
  } finally {
    loading.value = false
  }
}

async function loadPending() {
  const data = await merchantApi.orders({ page: 1, size: 5 })
  pendingOrders.value = (data?.records || []).filter((o) => o.status === 1 || o.status === 2)
}

async function loadBase() {
  try {
    profile.value = await merchantApi.profile()
  } catch {
    profile.value = null
  }
  try {
    shop.value = await merchantApi.shop()
  } catch {
    shop.value = null
  }
}

async function accept(row) {
  await merchantApi.accept(row.id)
  ElMessage.success('已接单')
  await Promise.all([loadStats(), loadPending()])
}

async function ready(row) {
  await merchantApi.ready(row.id)
  ElMessage.success('已出餐，等待骑手接单')
  await Promise.all([loadStats(), loadPending()])
}

onMounted(async () => {
  await loadBase()
  await Promise.all([loadStats(), loadPending()])
})
</script>

<style scoped>
.audit-alert {
  margin-bottom: 12px;
}

.stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-top: 8px;
}

.stat {
  background: #fafbfc;
  border-radius: 10px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-label {
  font-size: 13px;
  color: #909399;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
}

@media (max-width: 900px) {
  .stats {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
