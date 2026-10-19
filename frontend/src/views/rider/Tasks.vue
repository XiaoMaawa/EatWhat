<template>
  <div class="ew-page">
    <div class="ew-card">
      <div class="ew-flex-between">
        <h3 class="ew-title">我的任务</h3>
        <el-radio-group v-model="tab" size="small" @change="search">
          <el-radio-button value="ongoing">进行中</el-radio-button>
          <el-radio-button value="history">历史任务</el-radio-button>
        </el-radio-group>
      </div>

      <el-table v-loading="loading" :data="list" size="default" class="table">
        <el-table-column prop="orderNo" label="订单号" width="190" />
        <el-table-column label="取餐地址" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.pickupAddress }}</template>
        </el-table-column>
        <el-table-column label="送达地址" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.deliveryAddress }}</template>
        </el-table-column>
        <el-table-column label="距离" width="100">
          <template #default="{ row }">{{ distance(row.distance) }}</template>
        </el-table-column>
        <el-table-column label="收益" width="100">
          <template #default="{ row }">¥{{ money(row.fee) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="TASK_STATUS[row.status]?.type">
              {{ TASK_STATUS[row.status]?.text || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" size="small" type="primary" @click="pickup(row)">确认取餐</el-button>
            <el-button v-if="row.status === 2" size="small" type="success" @click="deliver(row)">确认送达</el-button>
            <span v-if="row.status >= 3" class="ew-muted">已完成</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && list.length === 0" description="没有任务" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { riderApi } from '@/api'
import { distance, money } from '@/utils/format'

const TASK_STATUS = {
  0: { text: '待抢单', type: 'warning' },
  1: { text: '已接单', type: 'primary' },
  2: { text: '已取餐', type: 'info' },
  3: { text: '已送达', type: 'success' }
}

const loading = ref(false)
const list = ref([])
const tab = ref('ongoing')

async function load() {
  loading.value = true
  try {
    const data = await riderApi.tasks({ status: tab.value, page: 1, size: 50 })
    list.value = data?.records || []
  } finally {
    loading.value = false
  }
}

function search() {
  load()
}

async function pickup(row) {
  await ElMessageBox.confirm('确认已从商家取到餐品？', '确认取餐', { type: 'info' })
  await riderApi.pickup(row.id)
  ElMessage.success('已取餐，开始配送')
  await load()
}

async function deliver(row) {
  await ElMessageBox.confirm('确认已将餐品送达顾客？', '确认送达', { type: 'info' })
  await riderApi.deliver(row.id)
  ElMessage.success('已送达，收益已计入')
  await load()
}

onMounted(load)
</script>

<style scoped>
.table {
  margin-top: 12px;
}
</style>
