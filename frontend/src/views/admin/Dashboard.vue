<template>
  <div class="ew-page" v-loading="loading">
    <div class="ew-card">
      <div class="ew-flex-between">
        <h3 class="ew-title">数据看板</h3>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>

      <div class="stats">
        <div class="stat">
          <div class="stat-label">今日订单</div>
          <div class="stat-value">{{ data.todayOrders ?? 0 }}</div>
        </div>
        <div class="stat">
          <div class="stat-label">今日交易额</div>
          <div class="stat-value ew-price">¥{{ money(data.todayAmount) }}</div>
        </div>
        <div class="stat">
          <div class="stat-label">用户总数</div>
          <div class="stat-value">{{ data.totalUsers ?? 0 }}</div>
        </div>
        <div class="stat">
          <div class="stat-label">商家总数</div>
          <div class="stat-value">{{ data.totalMerchants ?? 0 }}</div>
        </div>
        <div class="stat">
          <div class="stat-label">骑手总数</div>
          <div class="stat-value">{{ data.totalRiders ?? 0 }}</div>
        </div>
      </div>
    </div>

    <div class="ew-card">
      <h3 class="ew-title">近 7 天订单趋势</h3>
      <div v-if="trend.length" class="trend">
        <div v-for="item in trend" :key="item.date" class="trend-row">
          <span class="date">{{ item.date }}</span>
          <div class="bar-wrap">
            <div class="bar" :style="{ width: `${barWidth(item.count)}%` }" />
          </div>
          <span class="count">{{ item.count }} 单</span>
          <span class="amount ew-price">¥{{ money(item.amount) }}</span>
        </div>
      </div>
      <el-empty v-else description="暂无订单数据" :image-size="70" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { adminApi } from '@/api'
import { money } from '@/utils/format'

const loading = ref(false)
const data = ref({})

const trend = computed(() => data.value.orderTrend || [])
const maxCount = computed(() => Math.max(1, ...trend.value.map((i) => Number(i.count || 0))))

function barWidth(count) {
  return Math.max(4, Math.round((Number(count || 0) / maxCount.value) * 100))
}

async function load() {
  loading.value = true
  try {
    data.value = (await adminApi.dashboard()) || {}
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.stats {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
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
}

.trend {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.trend-row {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
}

.date {
  width: 90px;
  color: #909399;
}

.bar-wrap {
  flex: 1;
  background: #f5f6f8;
  border-radius: 6px;
  height: 14px;
  overflow: hidden;
}

.bar {
  height: 100%;
  background: linear-gradient(90deg, #67c23a, #95d475);
  border-radius: 6px;
  transition: width 0.3s ease;
}

.count {
  width: 70px;
  text-align: right;
}

.amount {
  width: 90px;
  text-align: right;
}

@media (max-width: 1100px) {
  .stats {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>
