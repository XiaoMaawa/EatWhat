<template>
  <div class="after-sale-admin">
    <div class="ew-flex-between page-head">
      <div>
        <h2>售后仲裁</h2>
        <p class="ew-muted">商家拒绝后顾客可申请平台介入，裁定支持顾客将触发全额退款</p>
      </div>
      <div class="ew-flex ew-gap8">
        <el-input
          v-model="keyword"
          placeholder="订单号"
          clearable
          style="width: 200px"
          @keyup.enter="reload"
        />
        <el-button type="primary" @click="reload">查询</el-button>
      </div>
    </div>

    <el-radio-group v-model="status" size="small" class="status-bar" @change="reload">
      <el-radio-button :value="null">全部</el-radio-button>
      <el-radio-button :value="0">待商家处理</el-radio-button>
      <el-radio-button :value="2">商家已拒绝</el-radio-button>
      <el-radio-button :value="4">待仲裁</el-radio-button>
      <el-radio-button :value="5">支持顾客</el-radio-button>
      <el-radio-button :value="6">支持商家</el-radio-button>
    </el-radio-group>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="afterSaleNo" label="售后单号" width="200" />
      <el-table-column label="类型" width="80">
        <template #default="{ row }">
          <el-tag :type="row.type === 1 ? 'warning' : 'primary'" size="small" effect="plain">
            {{ row.typeDesc }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="orderNo" label="订单号" width="200" />
      <el-table-column prop="nickname" label="顾客" width="110" />
      <el-table-column prop="shopName" label="店铺" width="140" show-overflow-tooltip />
      <el-table-column prop="reason" label="申请原因" min-width="200" show-overflow-tooltip />
      <el-table-column label="金额" width="100">
        <template #default="{ row }">¥{{ row.orderAmount ?? row.amount }}</template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ row.statusDesc }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="申请时间" width="170" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 2 || row.status === 4"
            size="small"
            type="primary"
            @click="openArbitrate(row)"
          >仲裁</el-button>
          <el-button size="small" link type="primary" @click="openDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        layout="total, prev, pager, next"
        :total="total"
        :page-size="size"
        :current-page="page"
        @current-change="onPage"
      />
    </div>

    <el-dialog v-model="detailVisible" title="售后详情" width="560px">
      <template v-if="current">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="售后单号">{{ current.afterSaleNo }}</el-descriptions-item>
          <el-descriptions-item label="订单号">{{ current.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ current.typeDesc }}</el-descriptions-item>
          <el-descriptions-item label="顾客">{{ current.nickname }}</el-descriptions-item>
          <el-descriptions-item label="店铺">{{ current.shopName }}</el-descriptions-item>
          <el-descriptions-item label="申请原因">{{ current.reason }}</el-descriptions-item>
          <el-descriptions-item label="商家处理">
            {{ current.merchantReply || '（未处理）' }}
          </el-descriptions-item>
          <el-descriptions-item label="当前状态">{{ current.statusDesc }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="current.imageList && current.imageList.length" class="images">
          <el-image
            v-for="(img, index) in current.imageList"
            :key="index"
            :src="img"
            :preview-src-list="current.imageList"
            fit="cover"
            class="image"
          />
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="arbitrateVisible" title="平台仲裁" width="480px">
      <el-form label-width="90px">
        <el-form-item label="裁定结果">
          <el-radio-group v-model="arbitrateForm.supportCustomer">
            <el-radio :value="true">支持顾客</el-radio>
            <el-radio :value="false">支持商家</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="裁定说明">
          <el-input
            v-model="arbitrateForm.verdict"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="说明判定依据，将同步给顾客与商家"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="arbitrateVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitArbitrate">提交裁定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api'

const status = ref(null)
const keyword = ref('')
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
const loading = ref(false)
const submitting = ref(false)
const detailVisible = ref(false)
const arbitrateVisible = ref(false)
const current = ref(null)
const arbitrateForm = ref({ supportCustomer: true, verdict: '' })

function statusType(value) {
  if (value === 0) return 'warning'
  if (value === 1 || value === 5) return 'success'
  if (value === 2) return 'danger'
  if (value === 4) return 'primary'
  return 'info'
}

async function load() {
  loading.value = true
  try {
    const data = await adminApi.afterSales({
      status: status.value,
      keyword: keyword.value || undefined,
      page: page.value,
      size
    })
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function reload() {
  page.value = 1
  load()
}

function onPage(value) {
  page.value = value
  load()
}

function openDetail(row) {
  current.value = row
  detailVisible.value = true
}

function openArbitrate(row) {
  current.value = row
  arbitrateForm.value = { supportCustomer: true, verdict: '' }
  arbitrateVisible.value = true
}

async function submitArbitrate() {
  submitting.value = true
  try {
    await adminApi.arbitrate(current.value.id, {
      supportCustomer: arbitrateForm.value.supportCustomer,
      verdict: arbitrateForm.value.verdict.trim()
    })
    ElMessage.success('裁定已提交')
    arbitrateVisible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page-head {
  margin-bottom: 16px;
}

.page-head h2 {
  margin: 0 0 4px;
  font-size: 18px;
}

.status-bar {
  margin-bottom: 12px;
}

.images {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.image {
  width: 72px;
  height: 72px;
  border-radius: 6px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding: 12px 0;
}
</style>
