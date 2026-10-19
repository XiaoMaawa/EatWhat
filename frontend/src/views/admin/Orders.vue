<template>
  <div class="ew-page">
    <div class="ew-card">
      <div class="ew-flex-between">
        <h3 class="ew-title">订单监管</h3>
        <div class="ew-flex ew-gap8">
          <el-input v-model="query.keyword" placeholder="订单号 / 手机号" clearable style="width: 200px" @keyup.enter="search" />
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px" @change="search">
            <el-option v-for="item in ORDER_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-button type="primary" @click="search">查询</el-button>
        </div>
        <div class="abnormal-bar">
          <span class="abnormal-label">异常订单</span>
          <el-radio-group v-model="abnormalType" size="small" @change="loadAbnormal">
            <el-radio-button :value="''">全部</el-radio-button>
            <el-radio-button value="timeout_accept">超时未接单</el-radio-button>
            <el-radio-button value="timeout_take">待配送超时</el-radio-button>
            <el-radio-button value="timeout_deliver">超时未送达</el-radio-button>
            <el-radio-button value="refunding">退款中</el-radio-button>
          </el-radio-group>
          <el-tag v-if="abnormalType" type="warning" size="small" effect="plain">
            共 {{ total }} 条异常订单
          </el-tag>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" class="table">
        <el-table-column prop="orderNo" label="订单号" width="190" />
        <el-table-column label="金额" width="110">
          <template #default="{ row }"><span class="ew-price">¥{{ money(row.totalAmount) }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="orderStatusType(row.status)">{{ orderStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="下单时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="detail(row)">详情</el-button>
            <el-button size="small" type="danger" @click="intervene(row)">干预</el-button>
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

    <el-dialog v-model="detailVisible" title="订单详情" width="600px">
      <div v-if="current" class="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ current.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ orderStatusText(current.status) }}</el-descriptions-item>
          <el-descriptions-item label="商品金额">¥{{ money(current.goodsAmount) }}</el-descriptions-item>
          <el-descriptions-item label="配送费">¥{{ money(current.deliveryFee) }}</el-descriptions-item>
          <el-descriptions-item label="订单金额">¥{{ money(current.totalAmount) }}</el-descriptions-item>
          <el-descriptions-item label="收货人">{{ current.receiverName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ current.receiverPhone }}</el-descriptions-item>
          <el-descriptions-item label="收货地址" :span="2">{{ current.receiverAddress }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ current.remark || '—' }}</el-descriptions-item>
        </el-descriptions>

        <el-table :data="current.items || []" size="small" class="items">
          <el-table-column prop="dishName" label="菜品" />
          <el-table-column prop="price" label="单价" width="90">
            <template #default="{ row }">¥{{ money(row.price) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="70" />
          <el-table-column label="小计" width="100">
            <template #default="{ row }">¥{{ money(row.subtotal) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>

    <el-dialog v-model="interveneVisible" title="订单干预" width="460px">
      <el-form label-width="90px">
        <el-form-item label="干预动作">
          <el-radio-group v-model="interveneForm.action">
            <el-radio value="FORCE_REFUND">强制退款</el-radio>
            <el-radio value="FORCE_COMPLETE">强制完成</el-radio>
            <el-radio value="FORCE_CANCEL">强制取消</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="原因说明">
          <el-input
            v-model="interveneForm.reason"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            placeholder="请说明干预原因，将记录到订单日志"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="interveneVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="submitIntervene">确认干预</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api'
import { money } from '@/utils/format'
import { ORDER_STATUS_OPTIONS, orderStatusText, orderStatusType } from '@/utils/orderStatus'

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '', status: null })
const detailVisible = ref(false)
const interveneVisible = ref(false)
const current = ref(null)
const interveneForm = reactive({ action: 'FORCE_REFUND', reason: '' })

const abnormalType = ref('')

async function load() {
  loading.value = true
  try {
    const data = abnormalType.value
      ? await adminApi.abnormalOrders({ type: abnormalType.value, page: query.page, size: query.size })
      : await adminApi.orders({ ...query })
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

function loadAbnormal() {
  query.page = 1
  load()
}

async function detail(row) {
  const data = await adminApi.orderDetail(row.id)
  const order = data?.order || {}
  current.value = {
    ...order,
    items: data?.items || order.items || [],
    receiverAddress: order.receiverAddress || order.address
  }
  detailVisible.value = true
}

function intervene(row) {
  current.value = row
  interveneForm.action = 'FORCE_REFUND'
  interveneForm.reason = ''
  interveneVisible.value = true
}

async function submitIntervene() {
  if (!interveneForm.reason.trim()) {
    ElMessage.warning('请填写干预原因')
    return
  }
  submitting.value = true
  try {
    await adminApi.intervene(current.value.id, {
      action: interveneForm.action,
      reason: interveneForm.reason.trim()
    })
    ElMessage.success('干预成功')
    interveneVisible.value = false
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

.detail {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.abnormal-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #ebeef5;
}

.abnormal-label {
  font-size: 13px;
  color: #606266;
}
</style>
