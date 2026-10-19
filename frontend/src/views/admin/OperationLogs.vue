<template>
  <div class="ew-page">
    <div class="ew-card">
      <div class="ew-flex-between">
        <h3 class="ew-title">操作日志</h3>
        <div class="ew-flex ew-gap8">
          <el-select v-model="query.operatorRole" placeholder="全部角色" clearable style="width: 130px" @change="search">
            <el-option v-for="(text, role) in ROLE_TEXT" :key="role" :label="text" :value="role" />
          </el-select>
          <el-select v-model="query.result" placeholder="全部结果" clearable style="width: 120px" @change="search">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
          <el-date-picker
            v-model="range"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 240px"
            @change="search"
          />
          <el-button type="primary" @click="search">查询</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" class="table">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="操作人" width="150">
          <template #default="{ row }">
            {{ ROLE_TEXT[row.operatorRole] || row.operatorRole || '-' }} #{{ row.operatorId ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="action" label="操作" min-width="160" />
        <el-table-column label="请求" min-width="230">
          <template #default="{ row }">
            <span class="method">{{ row.requestMethod }}</span>
            <span class="uri">{{ row.requestUri }}</span>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.result === 1 ? 'success' : 'danger'">
              {{ row.result === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="90">
          <template #default="{ row }">{{ row.costMs != null ? `${row.costMs} ms` : '-' }}</template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="130" />
        <el-table-column prop="createdAt" label="时间" width="170" />
        <el-table-column label="详情" width="80" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="showDetail(row)">查看</el-button>
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

    <el-dialog v-model="detailVisible" title="日志详情" width="560px">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="操作人">
          {{ ROLE_TEXT[detail.operatorRole] || detail.operatorRole || '-' }} #{{ detail.operatorId ?? '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="操作">{{ detail.action }}</el-descriptions-item>
        <el-descriptions-item label="请求">{{ detail.requestMethod }} {{ detail.requestUri }}</el-descriptions-item>
        <el-descriptions-item label="参数">
          <pre class="params">{{ detail.requestParams || '-' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="结果">{{ detail.result === 1 ? '成功' : '失败' }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.errorMsg" label="错误">{{ detail.errorMsg }}</el-descriptions-item>
        <el-descriptions-item label="IP">{{ detail.ip || '-' }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ detail.costMs != null ? `${detail.costMs} ms` : '-' }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ detail.createdAt }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { adminApi } from '@/api'
import { ROLE_TEXT } from '@/utils/orderStatus'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const range = ref(null)
const detailVisible = ref(false)
const detail = ref({})
const query = reactive({ page: 1, size: 20, operatorRole: null, result: null, startDate: null, endDate: null })

async function load() {
  loading.value = true
  try {
    query.startDate = range.value?.[0] || null
    query.endDate = range.value?.[1] || null
    const data = await adminApi.operationLogs({ ...query })
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

function showDetail(row) {
  detail.value = row
  detailVisible.value = true
}

onMounted(load)
</script>

<style scoped>
.table {
  margin-top: 12px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.method {
  display: inline-block;
  min-width: 54px;
  font-size: 12px;
  color: var(--ew-primary);
  font-weight: 600;
}

.uri {
  font-size: 12px;
  color: var(--ew-text-sub);
  word-break: break-all;
}

.params {
  margin: 0;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 200px;
  overflow: auto;
}
</style>
