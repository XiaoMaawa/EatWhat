<template>
  <div class="ew-page" v-loading="loading">
    <div class="ew-card">
      <div class="ew-flex-between">
        <h3 class="ew-title">平台统计</h3>
        <div class="toolbar">
          <el-radio-group v-model="period" size="small" @change="load">
            <el-radio-button value="day">今日</el-radio-button>
            <el-radio-button value="week">近 7 天</el-radio-button>
            <el-radio-button value="month">近 30 天</el-radio-button>
          </el-radio-group>
          <el-button :loading="loading" @click="load">刷新</el-button>
        </div>
      </div>

      <div class="stats">
        <div class="stat">
          <div class="stat-label">交易额</div>
          <div class="stat-value ew-price">¥{{ money(data.totalAmount) }}</div>
          <div class="stat-sub">
            <span>上期 ¥{{ money(data.prevAmount) }}</span>
            <span :class="growthClass(data.amountGrowth)">{{ growthText(data.amountGrowth) }}</span>
          </div>
        </div>
        <div class="stat">
          <div class="stat-label">订单量</div>
          <div class="stat-value">{{ data.orderCount ?? 0 }}</div>
          <div class="stat-sub">
            <span>上期 {{ data.prevOrderCount ?? 0 }}</span>
            <span :class="growthClass(data.orderGrowth)">{{ growthText(data.orderGrowth) }}</span>
          </div>
        </div>
        <div class="stat">
          <div class="stat-label">完成率</div>
          <div class="stat-value">{{ data.completeRate ?? 0 }}%</div>
          <div class="stat-sub">
            <span>已完成 {{ data.completedCount ?? 0 }} 单</span>
            <span>上期 {{ data.prevCompleteRate ?? 0 }}%</span>
          </div>
        </div>
        <div class="stat">
          <div class="stat-label">新增用户</div>
          <div class="stat-value">{{ data.newUsers ?? 0 }}</div>
          <div class="stat-sub"><span>{{ data.startDate }} 至 {{ data.endDate }}</span></div>
        </div>
      </div>
    </div>

    <div class="ew-card">
      <h3 class="ew-title">交易趋势</h3>
      <div v-if="trend.length" class="trend">
        <div v-for="item in trend" :key="item.date" class="trend-row">
          <span class="date">{{ item.date }}</span>
          <div class="bar-wrap">
            <div class="bar" :style="{ width: `${barWidth(item.amount)}%` }" />
          </div>
          <span class="count">{{ item.count }} 单</span>
          <span class="amount ew-price">¥{{ money(item.amount) }}</span>
          <span class="new-users">新增 {{ item.newUsers ?? 0 }}</span>
        </div>
      </div>
      <el-empty v-else description="暂无统计数据" :image-size="70" />
    </div>

    <div class="ew-card">
      <h3 class="ew-title">商家交易额 TOP10</h3>
      <el-table v-if="topMerchants.length" :data="topMerchants" size="small">
        <el-table-column label="排名" width="80">
          <template #default="{ $index }">
            <el-tag :type="rankType($index)" size="small" effect="dark">{{ $index + 1 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="shopName" label="店铺" min-width="160" />
        <el-table-column prop="orderCount" label="订单量" width="110" />
        <el-table-column label="交易额" width="140">
          <template #default="{ row }">
            <span class="ew-price">¥{{ money(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="占比" width="140">
          <template #default="{ row }">
            <div class="share-wrap">
              <div class="share-bar" :style="{ width: `${shareWidth(row.amount)}%` }" />
            </div>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="该周期暂无成交订单" :image-size="70" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { adminApi } from '@/api'
import { money } from '@/utils/format'

const loading = ref(false)
const period = ref('week')
const data = ref({})

const trend = computed(() => data.value.trend || [])
const topMerchants = computed(() => data.value.topMerchants || [])
const maxAmount = computed(() =>
  Math.max(1, ...trend.value.map((i) => Number(i.amount || 0)))
)
const totalTopAmount = computed(() =>
  Math.max(
    1,
    topMerchants.value.reduce((sum, i) => sum + Number(i.amount || 0), 0)
  )
)

function barWidth(amount) {
  return Math.max(4, Math.round((Number(amount || 0) / maxAmount.value) * 100))
}

function shareWidth(amount) {
  return Math.max(4, Math.round((Number(amount || 0) / totalTopAmount.value) * 100))
}

function growthText(value) {
  const n = Number(value || 0)
  if (!n) return '持平'
  return `${n > 0 ? '+' : ''}${n}%`
}

function growthClass(value) {
  const n = Number(value || 0)
  if (!n) return 'flat'
  return n > 0 ? 'up' : 'down'
}

function rankType(index) {
  if (index === 0) return 'danger'
  if (index === 1) return 'warning'
  if (index === 2) return 'success'
  return 'info'
}

async function load() {
  loading.value = true
  try {
    data.value = (await adminApi.statistics({ period: period.value })) || {}
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
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
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.stat-sub {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
}

.up {
  color: #f56c6c;
}

.down {
  color: #67c23a;
}

.flat {
  color: #909399;
}

.trend {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 10px;
}

.trend-row {
  display: grid;
  grid-template-columns: 110px 1fr 80px 110px 90px;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: #606266;
}

.date {
  color: #909399;
}

.bar-wrap {
  height: 10px;
  background: #f0f2f5;
  border-radius: 6px;
  overflow: hidden;
}

.bar {
  height: 100%;
  background: linear-gradient(90deg, #ff9a44, #fc6076);
  border-radius: 6px;
  transition: width 0.4s ease;
}

.count {
  text-align: right;
}

.amount {
  text-align: right;
}

.new-users {
  text-align: right;
  color: #909399;
}

.share-wrap {
  height: 8px;
  background: #f0f2f5;
  border-radius: 5px;
  overflow: hidden;
}

.share-bar {
  height: 100%;
  background: linear-gradient(90deg, #409eff, #79bbff);
  border-radius: 5px;
}
</style>
