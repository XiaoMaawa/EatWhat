<template>
  <div class="configs-page">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="平台参数由运营统一维护，修改后立即生效（部分参数需重启对应业务才生效）"
      class="tip"
    />

    <el-card shadow="never">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="搜索参数键/名称"
          clearable
          class="w-220"
          @keyup.enter="search"
        />
        <el-select v-model="query.group" placeholder="分组" clearable class="w-150">
          <el-option label="订单 ORDER" value="ORDER" />
          <el-option label="配送 DELIVERY" value="DELIVERY" />
          <el-option label="平台 PLATFORM" value="PLATFORM" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
        <el-button type="success" class="right" @click="openDialog()">新增参数</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="configKey" label="参数键" min-width="180" show-overflow-tooltip />
        <el-table-column prop="configName" label="参数名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="configValue" label="参数值" width="110" show-overflow-tooltip />
        <el-table-column label="分组" width="90">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ groupText(row.configGroup) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="说明" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link type="warning" @click="editValue(row)">改值</el-button>
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
        :page-sizes="[20, 50, 100]"
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑参数' : '新增参数'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="参数键" prop="configKey">
          <el-input
            v-model="form.configKey"
            maxlength="64"
            :disabled="Boolean(form.id)"
            placeholder="例如：order.pay.timeout.minutes"
          />
        </el-form-item>
        <el-form-item label="参数名称" prop="configName">
          <el-input v-model="form.configName" maxlength="100" placeholder="例如：订单支付超时时间（分钟）" />
        </el-form-item>
        <el-form-item label="参数值" prop="configValue">
          <el-input v-model="form.configValue" maxlength="500" placeholder="请输入参数值" />
        </el-form-item>
        <el-form-item label="分组" prop="configGroup">
          <el-select v-model="form.configGroup" class="w-full">
            <el-option label="订单 ORDER" value="ORDER" />
            <el-option label="配送 DELIVERY" value="DELIVERY" />
            <el-option label="平台 PLATFORM" value="PLATFORM" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" />
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

const query = reactive({ keyword: '', group: '', page: 1, size: 20 })

const form = reactive({
  id: null,
  configKey: '',
  configValue: '',
  configName: '',
  configGroup: 'PLATFORM',
  remark: ''
})

const rules = {
  configKey: [{ required: true, message: '请输入参数键', trigger: 'blur' }],
  configName: [{ required: true, message: '请输入参数名称', trigger: 'blur' }],
  configValue: [{ required: true, message: '请输入参数值', trigger: 'blur' }]
}

const GROUP_TEXT = { ORDER: '订单', DELIVERY: '配送', PLATFORM: '平台' }

function groupText(g) {
  return GROUP_TEXT[g] || g
}

async function load() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.group) params.group = query.group
    const data = await adminApi.configs(params)
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
  query.group = ''
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
      configKey: row.configKey,
      configValue: row.configValue,
      configName: row.configName,
      configGroup: row.configGroup,
      remark: row.remark || ''
    })
  } else {
    Object.assign(form, {
      id: null,
      configKey: '',
      configValue: '',
      configName: '',
      configGroup: 'PLATFORM',
      remark: ''
    })
  }
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    if (form.id) {
      await adminApi.updateConfig(form.id, {
        configValue: form.configValue,
        configName: form.configName,
        remark: form.remark
      })
      ElMessage.success('参数已更新')
    } else {
      await adminApi.createConfig({ ...form })
      ElMessage.success('参数已新增')
    }
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function editValue(row) {
  const { value } = await ElMessageBox.prompt(
    `${row.configName}（${row.configKey}）`,
    '快捷改值',
    {
      inputValue: row.configValue,
      inputPattern: /\S+/,
      inputErrorMessage: '参数值不能为空'
    }
  )
  await adminApi.updateConfig(row.id, {
    configValue: value,
    configName: row.configName,
    remark: row.remark
  })
  ElMessage.success('参数已更新')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除参数「${row.configKey}」吗？`, '提示', { type: 'warning' })
  await adminApi.deleteConfig(row.id)
  ElMessage.success('参数已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}

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

.w-150 {
  width: 150px;
}

.w-220 {
  width: 220px;
}

.w-full {
  width: 100%;
}

.pager {
  margin-top: 14px;
  justify-content: flex-end;
}
</style>
