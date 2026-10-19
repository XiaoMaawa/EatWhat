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
import { onMounted, ref } from 'vue'
import { riderApi } from '@/api'
import { money } from '@/utils/format'
import { AUDIT_STATUS } from '@/utils/orderStatus'

const loading = ref(false)
const stats = ref({})
const profile = ref(null)

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
</style>
