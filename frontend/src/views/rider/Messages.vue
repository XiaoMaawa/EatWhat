<template>
  <div class="ew-page">
    <div class="ew-card">
      <div class="ew-flex-between">
        <h3 class="ew-title">订单消息</h3>
        <div class="ew-flex ew-gap8">
          <el-tag v-if="unread > 0" type="danger" effect="plain" round>
            {{ unread > 99 ? '99+' : unread }} 条未读
          </el-tag>
          <el-button size="small" :disabled="unread === 0" @click="readAll">全部标为已读</el-button>
          <el-button size="small" :loading="loading" @click="load">刷新</el-button>
        </div>
      </div>

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
              <el-button link type="primary" size="small" @click.stop="goTask(item)">查看配送单</el-button>
            </div>
          </div>
          <div class="msg-ops">
            <el-button v-if="item.isRead !== 1" link type="primary" size="small" @click.stop="markRead(item)">
              标为已读
            </el-button>
          </div>
        </div>

        <el-empty v-if="!loading && list.length === 0" description="暂时没有订单消息" />

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
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { messageApi } from '@/api'

const router = useRouter()

const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const unread = ref(0)

function typeIcon(t) {
  if (t === 1) return '🛵'
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
  unread.value = data?.total || 0
}

async function load() {
  loading.value = true
  try {
    const data = await messageApi.list({ page: page.value, size: size.value })
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

async function markRead(item) {
  await messageApi.markRead(item.id)
  item.isRead = 1
  await loadUnread()
}

async function openMessage(item) {
  if (item.isRead !== 1) {
    await markRead(item)
  }
  if (item.orderId) {
    router.push('/rider/tasks')
  }
}

function goTask() {
  router.push('/rider/tasks')
}

async function readAll() {
  const data = await messageApi.readAll()
  ElMessage.success(`已标记 ${data?.count || 0} 条消息为已读`)
  await load()
}

onMounted(load)
</script>

<style scoped>
.msg-list {
  margin-top: 14px;
  min-height: 200px;
}

.msg-card {
  display: flex;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 10px;
  border: 1px solid var(--el-border-color-lighter);
  margin-bottom: 10px;
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.msg-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.msg-card.unread {
  background: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary-light-7);
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
  flex-wrap: wrap;
}

.msg-title {
  font-weight: 600;
  font-size: 14px;
}

.msg-time {
  margin-left: auto;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.msg-content {
  margin-top: 6px;
  font-size: 13px;
  color: var(--el-text-color-regular);
  line-height: 1.6;
  word-break: break-all;
}

.msg-foot {
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.msg-ops {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-end;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 12px;
}
</style>
