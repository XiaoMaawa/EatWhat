<template>
  <div class="announcements-page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="搜索标题/内容"
          clearable
          class="w-220"
          @keyup.enter="search"
        />
        <el-select v-model="query.status" placeholder="状态" clearable class="w-140">
          <el-option label="已发布" :value="1" />
          <el-option label="已下线" :value="0" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
        <el-button type="success" class="right" @click="openDialog()">发布公告</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="typeTag(row.type)" effect="plain">{{ typeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="可见范围" width="100">
          <template #default="{ row }">{{ roleText(row.targetRole) }}</template>
        </el-table-column>
        <el-table-column prop="startTime" label="生效时间" width="160" />
        <el-table-column label="失效时间" width="160">
          <template #default="{ row }">{{ row.endTime || '长期有效' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="86">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '已发布' : '已下线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="toggle(row)">
              {{ row.status === 1 ? '下线' : '上线' }}
            </el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pager"
        layout="total, prev, pager, next, sizes"
        :total="total"
        :current-page="query.page"
        :page-size="query.size"
        :page-sizes="[10, 20, 50]"
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑公告' : '发布公告'" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="正文" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="6"
            maxlength="2000"
            show-word-limit
            placeholder="请输入公告内容"
          />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">系统公告</el-radio>
            <el-radio :value="2">平台活动</el-radio>
            <el-radio :value="3">维护通知</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="可见范围" prop="targetRole">
          <el-select v-model="form.targetRole" class="w-full">
            <el-option label="全部用户" value="ALL" />
            <el-option label="仅消费者" value="CONSUMER" />
            <el-option label="仅商家" value="MERCHANT" />
            <el-option label="仅骑手" value="RIDER" />
          </el-select>
        </el-form-item>
        <el-form-item label="生效时间" prop="startTime">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择生效时间"
            class="w-full"
          />
        </el-form-item>
        <el-form-item label="失效时间">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="留空表示长期有效"
            clearable
            class="w-full"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'

const loading = ref(false)
const submitting = ref(false)
const rows = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const formRef = ref(null)

const query = reactive({ keyword: '', status: null, page: 1, size: 10 })

const form = reactive({
  id: null,
  title: '',
  content: '',
  type: 1,
  targetRole: 'ALL',
  startTime: '',
  endTime: null
})

const rules = {
  title: [
    { required: true, message: '请输入公告标题', trigger: 'blur' },
    { max: 100, message: '标题不能超过 100 字', trigger: 'blur' }
  ],
  content: [{ required: true, message: '请输入公告正文', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择生效时间', trigger: 'change' }]
}

const TYPE_TEXT = { 1: '系统公告', 2: '平台活动', 3: '维护通知' }
const TYPE_TAG = { 1: 'primary', 2: 'warning', 3: 'danger' }
const ROLE_TEXT = { ALL: '全部用户', CONSUMER: '消费者', MERCHANT: '商家', RIDER: '骑手' }

function typeText(t) {
  return TYPE_TEXT[t] || '公告'
}

function typeTag(t) {
  return TYPE_TAG[t] || 'info'
}

function roleText(r) {
  return ROLE_TEXT[r] || r
}

async function load() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.status !== null && query.status !== '') params.status = query.status
    const data = await adminApi.announcements(params)
    rows.value = data?.records || []
    total.value = data?.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  load()
}

function resetQuery() {
  query.keyword = ''
  query.status = null
  query.page = 1
  load()
}

function onPageChange(p) {
  query.page = p
  load()
}

function onSizeChange(s) {
  query.size = s
  query.page = 1
  load()
}

function openDialog(row) {
  if (row) {
    Object.assign(form, {
      id: row.id,
      title: row.title,
      content: row.content,
      type: row.type,
      targetRole: row.targetRole || 'ALL',
      startTime: row.startTime,
      endTime: row.endTime
    })
  } else {
    Object.assign(form, {
      id: null,
      title: '',
      content: '',
      type: 1,
      targetRole: 'ALL',
      startTime: nowText(),
      endTime: null
    })
  }
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

function nowText() {
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:00`
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload = {
      title: form.title,
      content: form.content,
      type: form.type,
      targetRole: form.targetRole,
      startTime: form.startTime,
      endTime: form.endTime || null
    }
    if (form.id) {
      await adminApi.updateAnnouncement(form.id, payload)
      ElMessage.success('公告已更新')
    } else {
      await adminApi.createAnnouncement(payload)
      ElMessage.success('公告已发布')
    }
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function toggle(row) {
  const target = row.status === 1 ? 0 : 1
  await adminApi.changeAnnouncementStatus(row.id, target)
  ElMessage.success(target === 1 ? '公告已上线' : '公告已下线')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除公告「${row.title}」吗？`, '提示', { type: 'warning' })
  await adminApi.deleteAnnouncement(row.id)
  ElMessage.success('公告已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.right {
  margin-left: auto;
}

.w-220 {
  width: 220px;
}

.w-140 {
  width: 140px;
}

.w-full {
  width: 100%;
}

.pager {
  margin-top: 14px;
  justify-content: flex-end;
}
</style>
