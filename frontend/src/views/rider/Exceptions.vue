<template>
  <div class="ew-page">
    <div class="ew-card">
      <div class="ew-flex-between">
        <h3 class="ew-title">异常与转单记录</h3>
        <el-radio-group v-model="type" size="small" @change="load">
          <el-radio-button :value="0">全部</el-radio-button>
          <el-radio-button :value="1">转单申请</el-radio-button>
          <el-radio-button :value="2">异常上报</el-radio-button>
        </el-radio-group>
      </div>

      <el-table v-loading="loading" :data="list" class="table">
        <el-table-column prop="orderNo" label="订单号" width="185" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.type === 1 ? 'warning' : 'danger'">{{ row.typeDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reasonDesc" label="原因" width="130" />
        <el-table-column prop="description" label="情况说明" min-width="220" show-overflow-tooltip />
        <el-table-column label="凭证" width="120">
          <template #default="{ row }">
            <el-image
              v-for="(img, i) in row.imageList || []"
              :key="i"
              :src="img"
              :preview-src-list="row.imageList"
              :initial-index="i"
              fit="cover"
              class="thumb"
            />
            <span v-if="!row.imageList || row.imageList.length === 0" class="ew-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.statusDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理说明" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.handleRemark || '—' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="提交时间" width="170" />
      </el-table>

      <el-empty v-if="!loading && list.length === 0" description="暂无异常记录" />

      <el-pagination
        v-if="total > size"
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        class="pager"
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { riderApi } from '@/api'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
const type = ref(0)

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, size }
    if (type.value !== 0) {
      params.type = type.value
    }
    const data = await riderApi.exceptions(params)
    list.value = data?.records || []
    total.value = data?.total || 0
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.table {
  margin-top: 12px;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
.thumb {
  width: 30px;
  height: 30px;
  border-radius: 4px;
  margin-right: 4px;
}
</style>
