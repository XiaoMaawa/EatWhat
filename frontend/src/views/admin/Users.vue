<template>
  <div class="ew-page">
    <div class="ew-card">
      <div class="ew-flex-between">
        <h3 class="ew-title">用户管理</h3>
        <div class="ew-flex ew-gap8">
          <el-input v-model="query.keyword" placeholder="手机号 / 昵称" clearable style="width: 200px" @keyup.enter="search" />
          <el-select v-model="query.role" placeholder="全部角色" clearable style="width: 130px" @change="search">
            <el-option v-for="(text, role) in ROLE_TEXT" :key="role" :label="text" :value="role" />
          </el-select>
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px" @change="search">
            <el-option label="正常" :value="1" />
            <el-option label="已禁用" :value="0" />
          </el-select>
          <el-button type="primary" @click="search">查询</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" class="table">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column label="角色" width="100">
          <template #default="{ row }">{{ ROLE_TEXT[row.role] || row.role }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="170" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              :type="row.status === 1 ? 'danger' : 'success'"
              :disabled="row.role === 'ADMIN'"
              @click="toggle(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
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
import { ROLE_TEXT } from '@/utils/orderStatus'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '', role: null, status: null })

async function load() {
  loading.value = true
  try {
    const data = await adminApi.users({ ...query })
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

async function toggle(row) {
  const target = row.status === 1 ? 0 : 1
  await ElMessageBox.confirm(
    `确定要${target === 0 ? '禁用' : '启用'}用户「${row.nickname || row.phone}」吗？`,
    '提示',
    { type: 'warning' }
  )
  await adminApi.updateUserStatus(row.id, target)
  ElMessage.success('操作成功')
  await load()
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
</style>
