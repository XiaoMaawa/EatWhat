<template>
  <div class="ew-page">
    <div class="ew-card">
      <h3 class="ew-title">订单管理</h3>

      <el-form :inline="true" class="filter">
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="item in ORDER_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="订单号">
          <el-input v-model="query.orderNo" placeholder="输入订单号" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="list" size="default">
        <el-table-column prop="orderNo" label="订单号" width="190" />
        <el-table-column label="菜品" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.itemSummary || '—' }}</template>
        </el-table-column>
        <el-table-column label="收货信息" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.receiverName }} · {{ row.receiverAddress }}
          </template>
        </el-table-column>
        <el-table-column label="金额" width="110">
          <template #default="{ row }">¥{{ money(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="orderStatusType(row.status)">{{ orderStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="下单时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" size="small" type="success" @click="accept(row)">接单</el-button>
            <el-button v-if="row.status === 1" size="small" type="danger" @click="reject(row)">拒单</el-button>
            <el-button v-if="row.status === 2" size="small" type="primary" @click="ready(row)">出餐完成</el-button>
            <span v-if="![1, 2].includes(row.status)" class="ew-muted">—</span>
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
import { merchantApi } from '@/api'
import { money } from '@/utils/format'
import { ORDER_STATUS_OPTIONS, orderStatusText, orderStatusType } from '@/utils/orderStatus'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const dateRange = ref([])
const query = reactive({ status: null, orderNo: '', page: 1, size: 10 })

async function load() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.status !== null && query.status !== '') params.status = query.status
    if (query.orderNo) params.orderNo = query.orderNo
    if (dateRange.value?.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const data = await merchantApi.orders(params)
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

function reset() {
  query.status = null
  query.orderNo = ''
  dateRange.value = []
  query.page = 1
  load()
}

async function accept(row) {
  await merchantApi.accept(row.id)
  ElMessage.success('已接单，请尽快备餐')
  await load()
}

async function reject(row) {
  const { value } = await ElMessageBox.prompt('请输入拒单原因', '拒单', {
    inputValue: '菜品已售罄',
    inputValidator: (v) => (v && v.length <= 100 ? true : '请填写 100 字以内的原因')
  })
  await merchantApi.reject(row.id, value)
  ElMessage.success('已拒单，订单将进入退款流程')
  await load()
}

async function ready(row) {
  await merchantApi.ready(row.id)
  ElMessage.success('已出餐，已通知骑手接单')
  await load()
}

onMounted(load)
</script>

<style scoped>
.filter {
  margin-bottom: 6px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>
