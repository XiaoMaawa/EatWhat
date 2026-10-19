<template>
  <div class="page">
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="手机号 / 昵称" clearable style="width: 220px" @keyup.enter="load" @clear="load" />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px" @change="load">
        <el-option label="正常" :value="1" />
        <el-option label="已禁用" :value="0" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="success" @click="dialogVisible = true">新增管理员</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="phone" label="手机号" min-width="140" />
      <el-table-column prop="nickname" label="昵称" min-width="140" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '已禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lastLoginAt" label="最近登录" width="170" />
      <el-table-column prop="createdAt" label="创建时间" width="170" />
    </el-table>

    <el-pagination
      class="pager"
      background
      layout="total, prev, pager, next"
      :total="total"
      :page-size="query.size"
      :current-page="query.page"
      @current-change="(p) => { query.page = p; load() }"
    />

    <el-dialog v-model="dialogVisible" title="新增管理员" width="460px" @closed="resetForm">
      <el-alert type="info" :closable="false" show-icon title="新管理员将使用手机号 + 初始密码登录管理后台，请提醒对方尽快修改密码。" style="margin-bottom: 16px" />
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="11 位手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="不填自动生成" maxlength="50" />
        </el-form-item>
        <el-form-item label="初始密码" prop="password">
          <el-input v-model="form.password" placeholder="6-20 位，需含字母和数字" show-password maxlength="20" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const query = reactive({ keyword: '', status: null, page: 1, size: 10 })

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({ phone: '', nickname: '', password: '' })
const rules = {
  phone: [{ required: true, message: '请输入手机号' }, { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }],
  password: [
    { required: true, message: '请输入初始密码' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d).{6,20}$/, message: '6-20 位，需同时包含字母和数字' }
  ]
}

async function load() {
  loading.value = true
  try {
    const data = await adminApi.admins({ keyword: query.keyword || undefined, status: query.status ?? undefined, page: query.page, size: query.size })
    records.value = data.records || []
    total.value = Number(data.total || 0)
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.phone = ''
  form.nickname = ''
  form.password = ''
  formRef.value?.clearValidate()
}

async function submit() {
  await formRef.value?.validate()
  saving.value = true
  try {
    await adminApi.createAdmin({ phone: form.phone, nickname: form.nickname || undefined, password: form.password })
    ElMessage.success('管理员已创建')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page { padding: 0; }
.toolbar { display: flex; gap: 12px; align-items: center; margin-bottom: 16px; flex-wrap: wrap; }
.toolbar .el-button { margin-left: 0; }
.pager { margin-top: 16px; justify-content: flex-end; }
</style>