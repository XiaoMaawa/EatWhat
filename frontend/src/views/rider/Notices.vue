<template>
  <div class="ew-page" v-loading="loading">
    <div class="ew-card">
      <div class="ew-flex-between">
        <h3 class="ew-title">系统通知</h3>
        <el-button link type="primary" :loading="loading" @click="load">刷新</el-button>
      </div>
      <p class="ew-muted">平台面向骑手发布的公告与通知，按发布时间倒序展示。</p>

      <el-empty v-if="!loading && notices.length === 0" description="暂时没有新的系统通知" />

      <div v-for="item in notices" :key="item.id" class="notice-item">
        <div class="notice-head">
          <div class="notice-title">
            <el-tag size="small" effect="plain" :type="typeTag(item.type)">{{ typeText(item.type) }}</el-tag>
            <span>{{ item.title }}</span>
          </div>
          <span class="ew-muted">{{ item.startTime || item.createdAt }}</span>
        </div>
        <div class="notice-body">{{ item.content }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { commonApi } from '@/api'

const loading = ref(false)
const notices = ref([])

const TYPE_TEXT = { 1: '系统公告', 2: '平台活动', 3: '维护通知' }
const TYPE_TAG = { 1: 'info', 2: 'success', 3: 'warning' }

function typeText(type) {
  return TYPE_TEXT[type] || '系统公告'
}

function typeTag(type) {
  return TYPE_TAG[type] || 'info'
}

async function load() {
  loading.value = true
  try {
    const data = await commonApi.announcements({ role: 'RIDER', limit: 20 })
    notices.value = data || []
  } catch {
    notices.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.notice-item {
  padding: 14px 0;
  border-bottom: 1px dashed var(--el-border-color-lighter);
}

.notice-item:last-child {
  border-bottom: none;
}

.notice-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.notice-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.notice-body {
  margin-top: 8px;
  color: var(--el-text-color-regular);
  line-height: 1.7;
  white-space: pre-wrap;
}
</style>
