<template>
  <div class="after-sale-page">
    <div class="page-head">
      <h2>售后与退款</h2>
      <el-radio-group v-model="status" size="small" @change="reload">
        <el-radio-button :value="null">全部</el-radio-button>
        <el-radio-button :value="0">待商家处理</el-radio-button>
        <el-radio-button :value="4">待平台仲裁</el-radio-button>
        <el-radio-button :value="1">已完成</el-radio-button>
      </el-radio-group>
    </div>

    <el-empty v-if="!loading && list.length === 0" description="还没有售后记录" />

    <el-card v-for="item in list" :key="item.id" class="as-card" shadow="hover">
      <div class="ew-flex-between as-head">
        <div class="ew-flex ew-gap8">
          <el-tag :type="item.type === 1 ? 'warning' : 'primary'" size="small" effect="plain">
            {{ item.typeDesc }}
          </el-tag>
          <span class="ew-muted">{{ item.afterSaleNo }}</span>
        </div>
        <el-tag :type="statusType(item.status)" size="small">{{ item.statusDesc }}</el-tag>
      </div>

      <div class="as-body">
        <div class="as-shop">{{ item.shopName || '店铺' }}</div>
        <div class="ew-muted">订单号 {{ item.orderNo }}</div>
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
        <span v-if="item.type === 1 && item.amount > 0" class="ew-price">{{ item.amount }}</span>
        <div class="ew-flex ew-gap8">
          <el-button v-if="item.status === 0" size="small" @click="cancel(item)">撤销申请</el-button>
          <el-button
            v-if="item.status === 2"
            size="small"
            type="primary"
            plain
            @click="escalate(item)"
          >申请平台介入</el-button>
          <el-button size="small" link type="primary" @click="$router.push(`/orders/${item.orderId}`)">
            查看订单
          </el-button>
        </div>
      </div>
    </el-card>

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
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { afterSaleApi } from '@/api'

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

async function load() {
  loading.value = true
  try {
    const data = await afterSaleApi.mine({ status: status.value, page: page.value, size })
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

async function cancel(item) {
  await ElMessageBox.confirm('确认撤销这笔售后申请？', '撤销申请', { type: 'warning' })
  await afterSaleApi.cancel(item.id)
  ElMessage.success('已撤销')
  await load()
}

async function escalate(item) {
  const { value } = await ElMessageBox.prompt('请补充说明情况，便于平台判定', '申请平台介入', {
    inputPlaceholder: '例如：餐品撒漏严重，商家拒绝退款'
  })
  await afterSaleApi.escalate(item.id)
  ElMessage.success(value ? '已提交平台介入' : '已提交平台介入')
  await load()
}

onMounted(load)
</script>

<style scoped>
.after-sale-page {
  padding: 16px 0;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.page-head h2 {
  margin: 0;
  font-size: 18px;
}

.as-card {
  margin-bottom: 12px;
}

.as-head {
  margin-bottom: 12px;
}

.as-shop {
  font-weight: 600;
  margin-bottom: 4px;
}

.as-reason {
  margin: 10px 0;
  padding: 10px 12px;
  background: #f7f8fa;
  border-radius: 6px;
  font-size: 14px;
  line-height: 1.6;
}

.as-images {
  flex-wrap: wrap;
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

.pager {
  display: flex;
  justify-content: center;
  padding: 12px 0;
}
</style>
