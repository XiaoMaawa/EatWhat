<template>
  <div class="ew-page">
    <div class="ew-card">
      <div class="ew-flex-between">
        <h3 class="ew-title">骑手认证审核</h3>
        <div class="ew-flex ew-gap8">
          <el-input v-model="query.keyword" placeholder="姓名 / 手机号" clearable style="width: 200px" @keyup.enter="search" />
          <el-select v-model="query.auditStatus" placeholder="全部审核状态" clearable style="width: 150px" @change="search">
            <el-option v-for="(item, key) in AUDIT_STATUS" :key="key" :label="item.text" :value="Number(key)" />
          </el-select>
          <el-button type="primary" @click="search">查询</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" class="table">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="realName" label="姓名" width="110" />
        <el-table-column prop="contactPhone" label="联系电话" width="130" />
        <el-table-column label="累计接单" width="110">
          <template #default="{ row }">{{ row.totalOrders ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="累计收益" width="120">
          <template #default="{ row }">¥{{ money(row.totalIncome) }}</template>
        </el-table-column>
        <el-table-column label="审核状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="AUDIT_STATUS[row.auditStatus]?.type">
              {{ AUDIT_STATUS[row.auditStatus]?.text || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="申请时间" width="170" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" :disabled="row.auditStatus !== 0" @click="openAudit(row)">
              审核
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

    <el-dialog v-model="auditVisible" title="骑手健康证审核" width="500px">
      <el-descriptions v-if="current" :column="1" border>
        <el-descriptions-item label="骑手 ID">{{ current.id }}</el-descriptions-item>
        <el-descriptions-item label="真实姓名">{{ current.realName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ current.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="健康证">
          <el-image
            v-if="current.healthImg"
            :src="current.healthImg"
            :preview-src-list="[current.healthImg]"
            fit="cover"
            style="width: 120px; height: 90px; border-radius: 6px"
          />
          <span v-else class="ew-muted">未上传</span>
        </el-descriptions-item>
      </el-descriptions>

      <div class="audit-form">
        <div class="label">审核结论</div>
        <el-radio-group v-model="auditForm.auditStatus">
          <el-radio :value="1">通过</el-radio>
          <el-radio :value="2">驳回</el-radio>
        </el-radio-group>
        <div class="label">审核备注</div>
        <el-input
          v-model="auditForm.auditRemark"
          type="textarea"
          :rows="3"
          maxlength="255"
          show-word-limit
          placeholder="驳回时请说明原因"
        />
      </div>

      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAudit">提交审核</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api'
import { money } from '@/utils/format'
import { AUDIT_STATUS } from '@/utils/orderStatus'

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '', auditStatus: null })
const auditVisible = ref(false)
const current = ref(null)
const auditForm = reactive({ auditStatus: 1, auditRemark: '' })

async function load() {
  loading.value = true
  try {
    const data = await adminApi.riders({ ...query })
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

async function openAudit(row) {
  current.value = await adminApi.riderDetail(row.id)
  auditForm.auditStatus = 1
  auditForm.auditRemark = ''
  auditVisible.value = true
}

async function submitAudit() {
  if (auditForm.auditStatus === 2 && !auditForm.auditRemark.trim()) {
    ElMessage.warning('驳回时请填写审核备注')
    return
  }
  submitting.value = true
  try {
    await adminApi.auditRider(current.value.id, {
      auditStatus: auditForm.auditStatus,
      auditRemark: auditForm.auditRemark.trim()
    })
    ElMessage.success('审核完成')
    auditVisible.value = false
    await load()
  } finally {
    submitting.value = false
  }
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

.audit-form {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.label {
  font-size: 13px;
  color: #606266;
  margin-top: 6px;
}
</style>
