<template>
  <div class="ew-page">
    <div class="ew-card">
      <div class="ew-flex-between">
        <h3 class="ew-title">退款售后</h3>
        <el-button link type="primary" :loading="loading" @click="load">刷新</el-button>
      </div>

      <el-tabs v-model="tab" @tab-change="onTabChange">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="待商家处理" name="0" />
        <el-tab-pane label="待平台仲裁" name="4" />
        <el-tab-pane label="已处理" name="done" />
      </el-tabs>

      <div v-loading="loading" class="as-list">
        <div v-for="item in list" :key="item.id" class="as-item">
          <div class="as-head">
            <div class="ew-flex ew-gap12">
              <span class="as-logo">🧾</span>
              <div>
                <div class="as-shop">{{ item.shopName || '店铺' }}</div>
                <div class="ew-muted">
                  {{ item.typeDesc }} · {{ item.afterSaleNo }} · 订单号 {{ item.orderNo }}
                </div>
              </div>
            </div>
            <el-tag :type="statusType(item.status)" size="small">{{ item.statusDesc }}</el-tag>
          </div>

          <div class="as-body">
            <div class="as-reason">{{ item.reason }}</div>
            <div class="ew-flex ew-gap8 as-images" v-if="item.imageList && item.imageList.length">
              <el-image
                v-for="(img, index) in item.imageList"
                :key="index"
                :src="img"
                :preview-src-list="item.imageList"
                fit="cover"
                class="as-image"
              />
            </div>
            <div v-if="item.merchantReply" class="as-reply">
              <b>商家回复：</b>{{ item.merchantReply }}
            </div>
            <div v-if="item.adminVerdict" class="as-reply">
              <b>平台裁定：</b>{{ item.adminVerdict }}
            </div>
          </div>

          <div class="as-foot">
            <span class="ew-muted">申请于 {{ item.createdAt }}</span>
            <div class="ew-flex ew-gap8">
              <span v-if="item.type === 1 && item.amount > 0" class="ew-price as-amount">{{ item.amount }}</span>
              <el-button v-if="item.status === 0" size="small" @click="cancel(item)">撤销申请</el-button>
              <el-button
                v-if="item.status === 2"
                size="small"
                type="primary"
                plain
                @click="escalate(item)"
              >
                申请平台介入
              </el-button>
              <el-button size="small" link type="primary" @click="$router.push(`/orders/${item.orderId}`)">
                查看订单
              </el-button>
            </div>
          </div>
        </div>

        <el-empty v-if="!loading && list.length === 0" description="还没有售后记录" :image-size="90" />
      </div>

      <div v-if="total > size" class="pager">
        <el-pagination
          layout="prev, pager, next"
          :total="total"
          :page-size="size"
          :current-page="page"
          @current-change="onPage"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { afterSaleApi } from '@/api'

const tab = ref('all')
const status = ref(null)
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
const loading = ref(false)

function statusType(value) {
  if (value === 0) return 'warning'
  if (value === 1 || value === 5) return 'success'
  if (value === 2) return 'danger'
  if (value === 4) return 'primary'
  return 'info'
}

function statusParamOf(name) {
  if (name === 'all' || name === 'done') return null
  return Number(name)
}

async function load() {
  loading.value = true
  try {
    const data = await afterSaleApi.mine({
      status: status.value,
      page: page.value,
      size
    })
    let records = data.records || []
    if (tab.value === 'done') {
      // 已处理 = 商家同意/拒绝/已撤销/仲裁结束
      records = records.filter((item) => item.status !== 0 && item.status !== 4)
    }
    list.value = records
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function onTabChange(name) {
  status.value = statusParamOf(name)
  page.value = 1
  load()
}

function onPage(value) {
  page.value = value
  load()
}

async function cancel(item) {
  try {
    await ElMessageBox.confirm('确认撤销这笔售后申请？', '撤销申请', { type: 'warning' })
  } catch {
    return
  }
  await afterSaleApi.cancel(item.id)
  ElMessage.success('已撤销')
  await load()
}

async function escalate(item) {
  let value = ''
  try {
    const result = await ElMessageBox.prompt('请补充说明情况，便于平台判定', '申请平台介入', {
      inputPlaceholder: '例如：餐品撒漏严重，商家拒绝退款',
      inputValidator: (v) => (v && v.trim().length >= 5 ? true : '请至少填写 5 个字的说明')
    })
    value = result.value
  } catch {
    return
  }
  await afterSaleApi.escalate(item.id)
  ElMessage.success(value ? '已提交平台介入' : '已提交平台介入')
  await load()
}

onMounted(load)
</script>

<style scoped>
.as-list {
  min-height: 120px;
}

.as-item {
  padding: 16px 0;
  border-top: 1px solid var(--ew-border);
}

.as-item:first-child {
  border-top: none;
  padding-top: 4px;
}

.as-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.as-logo {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #f5f7fa;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.as-shop {
  font-weight: 600;
  font-size: 15px;
}

.as-body {
  margin-top: 10px;
}

.as-reason {
  padding: 10px 12px;
  background: #f7f8fa;
  border-radius: 6px;
  font-size: 14px;
  line-height: 1.6;
  color: #606266;
}

.as-images {
  flex-wrap: wrap;
  margin-top: 10px;
}

.as-image {
  width: 72px;
  height: 72px;
  border-radius: 6px;
}

.as-reply {
  margin-top: 8px;
  font-size: 13px;
  color: var(--ew-text-light);
}

.as-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #eee;
}

.as-amount {
  font-size: 15px;
}

.pager {
  display: flex;
  justify-content: center;
  padding: 12px 0 0;
}
</style>
