<template>
  <div class="messages-page">
    <div class="page-head">
      <h2 class="title">消息中心</h2>
      <div class="head-actions">
        <el-tag v-if="unread.total > 0" type="danger" effect="plain" round>
          {{ unread.total > 99 ? '99+' : unread.total }} 条未读
        </el-tag>
        <el-button size="small" :disabled="unread.total === 0" @click="readAll">全部标为已读</el-button>
      </div>
    </div>

    <el-tabs v-model="type" @tab-change="reload">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane name="1">
        <template #label>
          订单消息
          <el-badge v-if="unread.order > 0" :value="unread.order" :max="99" class="tab-badge" />
        </template>
      </el-tab-pane>
      <el-tab-pane name="2">
        <template #label>
          系统公告
          <el-badge v-if="unread.system > 0" :value="unread.system" :max="99" class="tab-badge" />
        </template>
      </el-tab-pane>
      <el-tab-pane name="3">
        <template #label>
          售后消息
          <el-badge v-if="unread.afterSale > 0" :value="unread.afterSale" :max="99" class="tab-badge" />
        </template>
      </el-tab-pane>
    </el-tabs>

    <div v-loading="loading" class="msg-list">
      <div
        v-for="item in list"
        :key="item.id"
        class="msg-card"
        :class="{ unread: item.isRead !== 1 }"
        @click="openMessage(item)"
      >
        <div class="msg-icon">{{ typeIcon(item.type) }}</div>
        <div class="msg-body">
          <div class="msg-head">
            <span class="msg-title">{{ item.title }}</span>
            <el-tag size="small" effect="plain" :type="typeTag(item.type)">{{ item.typeDesc }}</el-tag>
            <span class="msg-time">{{ item.createdAt }}</span>
          </div>
          <div class="msg-content">{{ item.content }}</div>
          <div v-if="item.orderNo" class="msg-foot">
            订单号：{{ item.orderNo }}
            <el-button link type="primary" size="small" @click.stop="goOrder(item)">查看订单</el-button>
          </div>
        </div>
        <div class="msg-ops">
          <el-button v-if="item.isRead !== 1" link type="primary" size="small" @click.stop="markRead(item)">
            标为已读
          </el-button>
          <el-button link type="danger" size="small" @click.stop="remove(item)">删除</el-button>
        </div>
      </div>

      <el-empty v-if="!loading && list.length === 0" description="暂时没有消息" />

      <div v-if="total > size" class="pager">
        <el-pagination
          layout="prev, pager, next"
          :current-page="page"
          :page-size="size"
          :total="total"
          @current-change="onPageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { messageApi } from '@/api'

const router = useRouter()

const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const type = ref('all')
const unread = reactive({ total: 0, order: 0, system: 0, afterSale: 0 })

function typeIcon(t) {
  if (t === 1) return '🍱'
  if (t === 2) return '📢'
  if (t === 3) return '🧾'
  return '💬'
}

function typeTag(t) {
  if (t === 1) return 'primary'
  if (t === 2) return 'warning'
  if (t === 3) return 'danger'
  return 'info'
}

async function loadUnread() {
  const data = await messageApi.unreadCount()
  unread.total = data?.total || 0
  unread.order = data?.order || 0
  unread.system = data?.system || 0
  unread.afterSale = data?.afterSale || 0
}

async function reload() {
  page.value = 1
  await load()
}

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (type.value !== 'all') params.type = Number(type.value)
    const data = await messageApi.list(params)
    list.value = data?.records || []
    total.value = data?.total || 0
  } finally {
    loading.value = false
  }
  await loadUnread()
}

function onPageChange(p) {
  page.value = p
  load()
}

async function openMessage(item) {
  if (item.isRead !== 1) {
    await markRead(item)
  }
  if (item.orderId) {
    router.push(`/orders/${item.orderId}`)
  }
}

async function markRead(item) {
  await messageApi.markRead(item.id)
  item.isRead = 1
  await loadUnread()
}

async function readAll() {
  const data = await messageApi.readAll()
  ElMessage.success(`已标记 ${data?.count || 0} 条消息为已读`)
  await load()
}

async function remove(item) {
  await ElMessageBox.confirm('确定删除这条消息吗？', '提示', { type: 'warning' })
  await messageApi.remove(item.id)
  ElMessage.success('已删除')
  await load()
}

function goOrder(item) {
  if (item.orderId) {
    router.push(`/orders/${item.orderId}`)
  }
}

onMounted(async () => {
  await load()
})
</script>

<style scoped>
.messages-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px 16px 48px;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.tab-badge {
  margin-left: 4px;
}

.msg-list {
  min-height: 200px;
}

.msg-card {
  display: flex;
  gap: 12px;
  padding: 14px 16px;
  background: #fff;
  border-radius: 10px;
  margin-bottom: 10px;
  border: 1px solid #ebeef5;
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.msg-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.msg-card.unread {
  border-left: 3px solid #409eff;
  background: #f7fbff;
}

.msg-icon {
  font-size: 22px;
  line-height: 1.2;
}

.msg-body {
  flex: 1;
  min-width: 0;
}

.msg-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.msg-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.msg-time {
  margin-left: auto;
  font-size: 12px;
  color: #909399;
}

.msg-content {
  margin-top: 6px;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  word-break: break-all;
}

.msg-foot {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.msg-ops {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 18px;
}
</style>
