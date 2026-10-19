<template>
  <div class="ew-page">
    <div class="ew-card">
      <div class="ew-flex-between">
        <h3 class="ew-title">菜品监管</h3>
        <div class="ew-flex ew-gap8">
          <el-input v-model="query.keyword" placeholder="菜品名称" clearable style="width: 200px" @keyup.enter="search" />
          <el-input v-model="query.shopId" placeholder="店铺 ID" clearable style="width: 120px" @keyup.enter="search" />
          <el-button type="primary" @click="search">查询</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" class="table">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="图片" width="80">
          <template #default="{ row }">
            <el-image v-if="row.image" :src="row.image" fit="cover" class="thumb" />
            <span v-else>🍚</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="菜品名称" min-width="140" show-overflow-tooltip />
        <el-table-column label="价格" width="100">
          <template #default="{ row }"><span class="ew-price">¥{{ money(row.price) }}</span></template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="90" />
        <el-table-column label="销量" width="90">
          <template #default="{ row }">{{ row.monthSales ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="DISH_STATUS[row.status]?.type">
              {{ DISH_STATUS[row.status]?.text || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              type="danger"
              :disabled="row.status === 0"
              @click="offline(row)"
            >
              强制下架
            </el-button>
            <el-button
              size="small"
              type="success"
              :disabled="row.status !== 0"
              @click="online(row)"
            >
              恢复上架
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          :page-size="query.size"
          :total="total"
          layout="total, prev, pager, next"
          background
          @current-change="load"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'
import { money } from '@/utils/format'
import { DISH_STATUS } from '@/utils/orderStatus'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '', shopId: '' })

async function load() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.shopId) params.shopId = Number(query.shopId)
    const data = await adminApi.dishes(params)
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

async function offline(row) {
  await ElMessageBox.confirm(`确定要强制下架菜品「${row.name}」吗？`, '强制下架', { type: 'warning' })
  await adminApi.offlineDish(row.id)
  ElMessage.success('已下架')
  await load()
}

async function online(row) {
  await ElMessageBox.confirm(
    `确定要恢复菜品「${row.name}」吗？库存为 0 时将恢复为「售罄」状态。`,
    '恢复上架',
    { type: 'info' }
  )
  await adminApi.onlineDish(row.id)
  ElMessage.success('已恢复上架')
  await load()
}

onMounted(load)
</script>

<style scoped>
.table {
  margin-top: 12px;
}

.thumb {
  width: 44px;
  height: 44px;
  border-radius: 6px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>
