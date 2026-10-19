<template>
  <div class="ew-page" v-loading="loading">
    <div class="ew-card">
      <div class="ew-flex-between">
        <h3 class="ew-title">经营分析</h3>
        <el-radio-group v-model="type" size="small" @change="load">
          <el-radio-button value="day">今日</el-radio-button>
          <el-radio-button value="week">近 7 天</el-radio-button>
          <el-radio-button value="month">近 30 天</el-radio-button>
        </el-radio-group>
      </div>

      <div class="stats">
        <div class="stat">
          <div class="stat-label">客单价（商家-55）</div>
          <div class="stat-value ew-price">¥{{ money(data.avgOrderAmount) }}</div>
          <div class="stat-sub"><span>{{ data.totalOrders ?? 0 }} 单 / ¥{{ money(data.totalAmount) }}</span></div>
        </div>
        <div class="stat">
          <div class="stat-label">综合评分（商家-56）</div>
          <div class="stat-value">{{ num(data.score?.avgShopScore) }}</div>
          <div class="stat-sub"><span>共 {{ data.score?.totalReviews ?? 0 }} 条评价</span></div>
        </div>
        <div class="stat">
          <div class="stat-label">近 30 天评分（商家-51）</div>
          <div class="stat-value">{{ num(data.recentReviews?.avgShopScore) }}</div>
          <div class="stat-sub"><span>近 30 天 {{ data.recentReviews?.totalReviews ?? 0 }} 条评价</span></div>
        </div>
        <div class="stat">
          <div class="stat-label">高峰时段（商家-57）</div>
          <div class="stat-value">{{ data.peakHour || '—' }}</div>
          <div class="stat-sub"><span>{{ data.peakHourOrders ?? 0 }} 单</span></div>
        </div>
      </div>
    </div>

    <div class="ew-card">
      <h3 class="ew-title">评分维度</h3>
      <div class="score-rows">
        <div class="score-row">
          <span class="score-label">店铺评分</span>
          <el-rate :model-value="Number(data.score?.avgShopScore || 0)" disabled allow-half />
          <span class="ew-price">{{ num(data.score?.avgShopScore) }}</span>
        </div>
        <div class="score-row">
          <span class="score-label">配送评分</span>
          <el-rate :model-value="Number(data.score?.avgDeliveryScore || 0)" disabled allow-half />
          <span class="ew-price">{{ num(data.score?.avgDeliveryScore) }}</span>
        </div>
        <div class="score-row">
          <span class="score-label">口味评分</span>
          <el-rate :model-value="Number(data.score?.avgTasteScore || 0)" disabled allow-half />
          <span class="ew-price">{{ num(data.score?.avgTasteScore) }}</span>
        </div>
        <div class="score-row muted">
          <span class="score-label">已回复</span>
          <span>{{ data.score?.repliedReviews ?? 0 }} / {{ data.score?.totalReviews ?? 0 }} 条</span>
        </div>
      </div>
    </div>

    <div class="ew-card">
      <div class="ew-flex-between">
        <h3 class="ew-title">营业时段分析</h3>
        <span v-if="data.peakHour" class="peak-tip">
          高峰 {{ data.peakHour }}（{{ data.peakHourOrders }} 单）
        </span>
      </div>
      <div v-if="hourly.length" class="hour-chart">
        <div v-for="item in hourly" :key="item.hour" class="hour-row">
          <span class="hour-label">{{ item.hour }}</span>
          <div class="hour-bar-wrap">
            <div
              class="hour-bar"
              :class="{ peak: item.hour === data.peakHour }"
              :style="{ width: `${barWidth(item.orderCount)}%` }"
            />
          </div>
          <span class="hour-count">{{ item.orderCount }}</span>
        </div>
      </div>
      <el-empty v-else description="该周期暂无成交订单" :image-size="70" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { merchantApi } from '@/api'
import { money } from '@/utils/format'

const loading = ref(false)
const type = ref('month')
const data = ref({})

const hourly = computed(() => data.value.hourly || [])
const maxCount = computed(() => Math.max(1, ...hourly.value.map((i) => Number(i.orderCount || 0))))

function barWidth(count) {
  return Math.max(2, Math.round((Number(count || 0) / maxCount.value) * 100))
}

function num(value) {
  const n = Number(value || 0)
  return Number.isInteger(n) ? n.toFixed(1) : String(n)
}

async function load() {
  loading.value = true
  try {
    data.value = (await merchantApi.analytics(type.value)) || {}
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
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

.stat-sub {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
}

.score-rows {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 6px;
}

.score-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.score-row.muted {
  color: #909399;
  font-size: 13px;
}

.score-label {
  width: 80px;
  color: #606266;
  font-size: 14px;
}

.peak-tip {
  font-size: 13px;
  color: #e6a23c;
}

.hour-chart {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 10px;
}

.hour-row {
  display: grid;
  grid-template-columns: 60px 1fr 50px;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: #606266;
}

.hour-bar-wrap {
  height: 12px;
  background: #f0f2f5;
  border-radius: 6px;
  overflow: hidden;
}

.hour-bar {
  height: 100%;
  background: linear-gradient(90deg, #409eff, #79bbff);
  border-radius: 6px;
  transition: width 0.4s ease;
}

.hour-bar.peak {
  background: linear-gradient(90deg, #ff9a44, #fc6076);
}

.hour-count {
  text-align: right;
}

@media (max-width: 900px) {
  .stats {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
