<template>
  <div class="sensitive-page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="搜索敏感词"
          clearable
          class="w-200"
          @keyup.enter="search"
        />
        <el-select v-model="query.status" placeholder="状态" clearable class="w-130">
          <el-option label="已启用" :value="1" />
          <el-option label="已停用" :value="0" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
        <el-button class="right" @click="importVisible = true">批量导入</el-button>
        <el-button type="success" @click="openDialog()">新增敏感词</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="word" label="敏感词" min-width="150" />
        <el-table-column label="适用范围" width="120">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" :type="categoryTag(row.category)">
              {{ categoryText(row.category) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              @change="(v) => toggleStatus(row, v)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="165" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
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

    <el-card shadow="never" class="test-card">
      <template #header><span class="card-title">文本检测工具</span></template>
      <div class="test-body">
        <el-input
          v-model="testText"
          type="textarea"
          :rows="3"
          placeholder="输入一段文本，检测其中是否命中敏感词"
        />
        <div class="test-actions">
          <el-select v-model="testCategory" class="w-140">
            <el-option label="全部范围" value="ALL" />
            <el-option label="评价" value="REVIEW" />
            <el-option label="备注" value="REMARK" />
            <el-option label="昵称" value="NICKNAME" />
          </el-select>
          <el-button type="primary" :loading="checking" @click="check">检测</el-button>
          <span v-if="checked" class="result">
            <template v-if="hitWords.length">
              命中 <el-tag v-for="w in hitWords" :key="w" size="small" type="danger" class="hit-tag">{{ w }}</el-tag>
            </template>
            <template v-else><el-tag size="small" type="success">未发现敏感词</el-tag></template>
          </span>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑敏感词' : '新增敏感词'" width="460px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="敏感词" prop="word">
          <el-input v-model="form.word" maxlength="50" placeholder="请输入敏感词" />
        </el-form-item>
        <el-form-item label="适用范围" prop="category">
          <el-select v-model="form.category" class="w-full">
            <el-option label="全部范围" value="ALL" />
            <el-option label="评价内容" value="REVIEW" />
            <el-option label="订单备注" value="REMARK" />
            <el-option label="用户昵称" value="NICKNAME" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importVisible" title="批量导入敏感词" width="520px">
      <el-input
        v-model="importText"
        type="textarea"
        :rows="6"
        placeholder="每行一个敏感词，或用英文逗号分隔"
      />
      <div class="import-row">
        <span class="label">适用范围：</span>
        <el-select v-model="importCategory" class="w-140">
          <el-option label="全部范围" value="ALL" />
          <el-option label="评价内容" value="REVIEW" />
          <el-option label="订单备注" value="REMARK" />
          <el-option label="用户昵称" value="NICKNAME" />
        </el-select>
      </div>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="doImport">导入</el-button>
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
const importing = ref(false)
const checking = ref(false)
const checked = ref(false)
const hitWords = ref([])
const rows = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const importVisible = ref(false)
const importText = ref('')
const importCategory = ref('REVIEW')
const testText = ref('')
const testCategory = ref('REVIEW')
const formRef = ref(null)

const query = reactive({ keyword: '', status: null, page: 1, size: 10 })

const form = reactive({ id: null, word: '', category: 'REVIEW', status: 1 })

const rules = {
  word: [{ required: true, message: '请输入敏感词', trigger: 'blur' }]
}

const CATEGORY_TEXT = { ALL: '全部范围', REVIEW: '评价内容', REMARK: '订单备注', NICKNAME: '用户昵称' }
const CATEGORY_TAG = { ALL: 'info', REVIEW: 'warning', REMARK: 'primary', NICKNAME: 'success' }

function categoryText(c) {
  return CATEGORY_TEXT[c] || c
}

function categoryTag(c) {
  return CATEGORY_TAG[c] || 'info'
}

async function load() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.status !== null && query.status !== '') params.status = query.status
    const data = await adminApi.sensitiveWords(params)
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
    Object.assign(form, { id: row.id, word: row.word, category: row.category, status: row.status })
  } else {
    Object.assign(form, { id: null, word: '', category: 'REVIEW', status: 1 })
  }
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload = { word: form.word.trim(), category: form.category }
    if (form.id) {
      await adminApi.updateSensitiveWord(form.id, { ...payload, status: form.status })
      ElMessage.success('敏感词已更新')
    } else {
      await adminApi.createSensitiveWord(payload)
      ElMessage.success('敏感词已新增')
    }
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row, enabled) {
  await adminApi.updateSensitiveWord(row.id, {
    word: row.word,
    category: row.category,
    status: enabled ? 1 : 0
  })
  ElMessage.success(enabled ? '敏感词已启用' : '敏感词已停用')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除敏感词「${row.word}」吗？`, '提示', { type: 'warning' })
  await adminApi.deleteSensitiveWord(row.id)
  ElMessage.success('敏感词已删除')
  load()
}

async function doImport() {
  const words = importText.value
    .split(/[\n,，]/)
    .map((w) => w.trim())
    .filter(Boolean)
  if (!words.length) {
    ElMessage.warning('请至少输入一个敏感词')
    return
  }
  importing.value = true
  try {
    const data = await adminApi.importSensitiveWords({ words, category: importCategory.value })
    ElMessage.success(`已导入 ${data?.imported ?? 0} 个敏感词`)
    importVisible.value = false
    importText.value = ''
    query.page = 1
    load()
  } finally {
    importing.value = false
  }
}

async function check() {
  if (!testText.value.trim()) {
    ElMessage.warning('请先输入待检测文本')
    return
  }
  checking.value = true
  try {
    const data = await adminApi.checkSensitiveWords({
      text: testText.value,
      category: testCategory.value
    })
    hitWords.value = data?.words || []
    checked.value = true
  } finally {
    checking.value = false
  }
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

.w-130 {
  width: 130px;
}

.w-140 {
  width: 140px;
}

.w-200 {
  width: 200px;
}

.w-full {
  width: 100%;
}

.pager {
  margin-top: 14px;
  justify-content: flex-end;
}

.test-card {
  margin-top: 16px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
}

.test-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.test-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.result {
  font-size: 13px;
  color: #606266;
}

.hit-tag {
  margin-left: 6px;
}

.import-row {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.import-row .label {
  font-size: 13px;
  color: #606266;
}
</style>
