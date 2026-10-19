<template>
  <div class="ew-page" v-loading="loading">
    <div class="ew-card">
      <div class="ew-flex-between">
        <h3 class="ew-title">每日经营汇总</h3>
        <div class="toolbar">
          <el-date-picker
            v-model="range"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            size="small"
            @change="search"
          />
          <el-button size="small" type="primary" @click="search">查询</el-button>
          <el-button size="small" :loading="summarizing" @click="summarize">补跑昨日</el-button>
        </div>
      </div>

      <div class="summary">
        <div class="sum-item">
          <div class="sum-label">区间订单量</div>
          <div class="sum-value">{{ totalOf('orderCount') }}</div>
        </div>
        <div class="sum-item">
          <div class="sum-label">区间交易额</div>
          <div class="sum-value ew-price">¥{{ money(totalOf('totalAmount')) }}</div>
        </div>
        <div class="sum-item">
          <div class="sum-label">区间完成单量</div>
          <div class="sum-value">{{ totalOf('completedCount') }}</div>
        </div>
        <div class="sum-item">
          <div class="sum-label">区间退款额</div>
          <div class="sum-value ew-price">¥{{ money(totalOf('refundAmount')) }}</div>
        </div>
        <div class="sum-item">
          <div class="sum-label">区间新增用户</div>
          <div class="sum-value">{{ totalOf('newUsers') }}</div>
        </div>
      </div>

      <el-table :data="list" size="small" class="table">
        <el-table-column prop="statDate" label="统计日期" width="130" />
        <el-table-column prop="orderCount" label="订单量" width="90" />
        <el-table-column prop="completedCount" label="完成单量" width="100" />
        <el-table-column prop="cancelCount" label="取消/退款" width="100" />
        <el-table-column label="交易额" width="130">
          <template #default="{ row }">
            <span class="ew-price">¥{{ money(row.totalAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="退款额" width="120">
          <template #default="{ row }">
            <span class="ew-muted">¥{{ money(row.refundAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="newUsers" label="新增用户" width="100" />
        <el-table-column prop="newMerchants" label="新增商家" width="100" />
        <el-table-column prop="newRiders" label="新增骑手" width="100" />
      </el-table>

      <el-pagination
        v-if="total > 0"
        class="pager"
        layout="total, prev, pager, next"
        :total="total"
        :page-size="query.size"
        :current-page="query.page"
        @current-change="changePage"
      />
      <el-empty v-if="!loading && list.length === 0" description="暂无汇总数据，可点击「补跑昨日」生成" :image-size="70" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api'
import { money } from '@/utils/format'

const loading = ref(false)
const summarizing = ref(false)
const list = ref([])
const total = ref(0)
const range = ref([])
const query = reactive({ page: 1, size: 10 })

function params() {
  const p = { page: query.page, size: query.size }
  if (range.value?.length === 2) {
    p.startDate = range.value[0]
    p.endDate = range.value[1]
  }
  return p
}

async function load() {
  loading.value = true
  try {
    const data = await adminApi.dailyStats(params())
    list.value = data?.records || []
    total.value = data?.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  load()
}

function changePage(page) {
  query.page = page
  load()
}

function totalOf(key) {
  const sum = list.value.reduce((acc, row) => acc + Number(row[key] || 0), 0)
  return key.endsWith('Amount') ? sum.toFixed(2) : sum
}

async function summarize() {
  summarizing.value = true
  try {
    await adminApi.summarizeStats()
    ElMessage.success('昨日数据汇总完成')
    await load()
  } finally {
    summarizing.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.summary {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 14px;
  margin: 16px 0;
}

.sum-item {
  background: #fafbfc;
  border-radius: 10px;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.sum-label {
  font-size: 13px;
  color: #909399;
}

.sum-value {
  font-size: 20px;
  font-weight: 700;
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}

@media (max-width: 1100px) {
  .summary {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>