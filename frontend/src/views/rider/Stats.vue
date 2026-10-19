<template>
  <div class="ew-page" v-loading="loading">
    <div class="ew-card">
      <h3 class="ew-title">收益统计</h3>
      <div class="stats">
        <div class="stat">
          <div class="stat-label">今日接单</div>
          <div class="stat-value">{{ stats.todayOrders ?? 0 }}</div>
        </div>
        <div class="stat">
          <div class="stat-label">今日收益</div>
          <div class="stat-value ew-price">¥{{ money(stats.todayIncome) }}</div>
        </div>
        <div class="stat">
          <div class="stat-label">累计接单</div>
          <div class="stat-value">{{ stats.totalOrders ?? 0 }}</div>
        </div>
        <div class="stat">
          <div class="stat-label">累计收益</div>
          <div class="stat-value ew-price">¥{{ money(stats.totalIncome) }}</div>
        </div>
      </div>
    </div>

    <div class="ew-card">
      <div class="ew-flex-between">
        <h3 class="ew-title">收入明细</h3>
        <el-radio-group v-model="period" size="small">
          <el-radio-button value="week">本周</el-radio-button>
          <el-radio-button value="month">本月</el-radio-button>
        </el-radio-group>
      </div>
      <div class="stats period">
        <div class="stat">
          <div class="stat-label">{{ period === 'week' ? '本周' : '本月' }}完成单量</div>
          <div class="stat-value">{{ period === 'week' ? (stats.weekOrders ?? 0) : (stats.monthOrders ?? 0) }}</div>
        </div>
        <div class="stat">
          <div class="stat-label">{{ period === 'week' ? '本周' : '本月' }}收入</div>
          <div class="stat-value ew-price">
            ¥{{ money(period === 'week' ? stats.weekIncome : stats.monthIncome) }}
          </div>
        </div>
        <div class="stat">
          <div class="stat-label">日均收入</div>
          <div class="stat-value ew-price">¥{{ money(avgIncome) }}</div>
        </div>
        <div class="stat">
          <div class="stat-label">单均收入</div>
          <div class="stat-value ew-price">¥{{ money(avgFee) }}</div>
        </div>
      </div>

      <div class="trend">
        <div v-for="point in stats.incomeTrend || []" :key="point.date" class="bar-col">
          <div class="bar-wrap">
            <div class="bar" :style="{ height: barHeight(point.income) }" :title="`¥${money(point.income)}`"></div>
          </div>
          <div class="bar-label">{{ point.date.slice(5) }}</div>
          <div class="bar-value">¥{{ money(point.income) }}</div>
        </div>
        <el-empty v-if="!(stats.incomeTrend || []).length" description="近 7 日暂无收入" :image-size="60" />
      </div>
    </div>

    <div class="ew-card">
      <h3 class="ew-title">骑手资料</h3>
      <el-descriptions v-if="profile" :column="2" border>
        <el-descriptions-item label="姓名">{{ profile.realName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ profile.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="审核状态">
          <el-tag :type="AUDIT_STATUS[profile.auditStatus]?.type">
            {{ AUDIT_STATUS[profile.auditStatus]?.text || '未知' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="审核备注">{{ profile.auditRemark || '—' }}</el-descriptions-item>
        <el-descriptions-item label="加入时间">{{ profile.createdAt }}</el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="还没有提交骑手认证" :image-size="70" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { riderApi } from '@/api'
import { money } from '@/utils/format'
import { AUDIT_STATUS } from '@/utils/orderStatus'

const loading = ref(false)
const stats = ref({})
const profile = ref(null)
const period = ref('week')

const avgIncome = computed(() => {
  const total = period.value === 'week' ? stats.value.weekIncome : stats.value.monthIncome
  const days = period.value === 'week' ? 7 : new Date().getDate()
  const value = Number(total || 0)
  return days > 0 ? value / days : 0
})

const avgFee = computed(() => {
  const orders = period.value === 'week' ? stats.value.weekOrders : stats.value.monthOrders
  const total = period.value === 'week' ? stats.value.weekIncome : stats.value.monthIncome
  const count = Number(orders || 0)
  return count > 0 ? Number(total || 0) / count : 0
})

const maxTrend = computed(() => {
  const points = stats.value.incomeTrend || []
  return Math.max(1, ...points.map((p) => Number(p.income || 0)))
})

function barHeight(income) {
  const ratio = Number(income || 0) / maxTrend.value
  return `${Math.max(4, Math.round(ratio * 100))}%`
}

onMounted(async () => {
  loading.value = true
  try {
    stats.value = (await riderApi.stats()) || {}
    profile.value = await riderApi.profile().catch(() => null)
  } finally {
    loading.value = false
  }
})
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
}

@media (max-width: 900px) {
  .stats {
    grid-template-columns: repeat(2, 1fr);
  }
}

.stats.period {
  margin-top: 6px;
}

.trend {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  margin-top: 20px;
  min-height: 150px;
  padding: 0 4px;
}

.bar-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.bar-wrap {
  width: 100%;
  height: 100px;
  display: flex;
  align-items: flex-end;
}

.bar {
  width: 100%;
  border-radius: 6px 6px 0 0;
  background: linear-gradient(180deg, #6ea8fe, #3b7ddd);
  transition: height 0.3s;
}

.bar-label {
  font-size: 12px;
  color: #909399;
}

.bar-value {
  font-size: 11px;
  color: #606266;
}
</style>
