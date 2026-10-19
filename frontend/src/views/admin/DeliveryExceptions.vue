<template>
  <div class="delivery-exception">
    <div class="ew-flex-between page-head">
      <div>
        <h2>配送异常</h2>
        <p class="ew-muted">骑手上报的配送异常与转单申请，处理后请补充说明</p>
      </div>
      <div class="ew-flex ew-gap8">
        <el-input
          v-model="keyword"
          placeholder="订单号 / 情况说明"
          clearable
          style="width: 220px"
          @keyup.enter="reload"
        />
        <el-button type="primary" @click="reload">查询</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-radio-group v-model="type" size="small" @change="reload">
        <el-radio-button :value="null">全部类型</el-radio-button>
        <el-radio-button :value="1">转单申请</el-radio-button>
        <el-radio-button :value="2">异常上报</el-radio-button>
      </el-radio-group>
      <el-radio-group v-model="status" size="small" @change="reload">
        <el-radio-button :value="null">全部状态</el-radio-button>
        <el-radio-button :value="0">待处理</el-radio-button>
        <el-radio-button :value="1">已处理</el-radio-button>
      </el-radio-group>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="orderNo" label="订单号" width="185" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="row.type === 1 ? 'warning' : 'danger'" effect="plain">
            {{ row.typeDesc }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reasonDesc" label="原因" width="120" />
      <el-table-column prop="description" label="情况说明" min-width="200" show-overflow-tooltip />
      <el-table-column prop="riderName" label="骑手" width="100" />
      <el-table-column prop="shopName" label="店铺" width="130" show-overflow-tooltip />
      <el-table-column label="凭证" width="110">
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
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 1 ? 'success' : 'warning'">{{ row.statusDesc }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="上报时间" width="170" />
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 0" size="small" type="primary" @click="openHandle(row)">处理</el-button>
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

    <el-dialog v-model="detailVisible" title="异常详情" width="560px">
      <template v-if="current">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="订单号">{{ current.orderNo || '—' }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ current.typeDesc }}</el-descriptions-item>
          <el-descriptions-item label="原因">{{ current.reasonDesc }}</el-descriptions-item>
          <el-descriptions-item label="情况说明">{{ current.description }}</el-descriptions-item>
          <el-descriptions-item label="骑手">{{ current.riderName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="店铺">{{ current.shopName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="当前状态">{{ current.statusDesc }}</el-descriptions-item>
          <el-descriptions-item label="处理说明">{{ current.handleRemark || '（未处理）' }}</el-descriptions-item>
          <el-descriptions-item label="上报时间">{{ current.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="处理时间">{{ current.handledAt || '—' }}</el-descriptions-item>
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

    <el-dialog v-model="handleVisible" title="处理配送异常" width="480px">
      <el-form label-width="90px">
        <el-form-item label="处理说明">
          <el-input
            v-model="handleRemark"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="例如：已电话联系顾客并确认地址，请骑手继续配送"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitHandle">确认处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api'

const type = ref(null)
const status = ref(null)
const keyword = ref('')
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
const loading = ref(false)
const submitting = ref(false)
const detailVisible = ref(false)
const handleVisible = ref(false)
const current = ref(null)
const handleRemark = ref('')

async function load() {
  loading.value = true
  try {
    const data = await adminApi.deliveryExceptions({
      type: type.value,
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

function openHandle(row) {
  current.value = row
  handleRemark.value = ''
  handleVisible.value = true
}

async function submitHandle() {
  submitting.value = true
  try {
    await adminApi.handleDeliveryException(current.value.id, { remark: handleRemark.value.trim() })
    ElMessage.success('已处理，说明将同步给骑手')
    handleVisible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page-head {
  margin-bottom: 14px;
}

.page-head h2 {
  margin: 0 0 4px;
  font-size: 18px;
}

.filter-bar {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.thumb {
  width: 28px;
  height: 28px;
  border-radius: 4px;
  margin-right: 4px;
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
