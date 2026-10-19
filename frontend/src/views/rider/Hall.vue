<template>
  <div class="ew-page" v-loading="loading">
    <el-alert
      v-if="profile && profile.auditStatus !== 1"
      type="warning"
      show-icon
      :closable="false"
      class="tip"
      :title="profile.auditStatus === 0 ? '骑手认证审核中' : '骑手认证未通过'"
    >
      <template #default>{{ profile.auditRemark || '请补充健康证等资料后重新提交' }}</template>
    </el-alert>

    <div class="ew-card">
      <div class="ew-flex-between">
        <h3 class="ew-title">抢单大厅</h3>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>

      <div class="tasks">
        <div v-for="task in tasks" :key="task.id" class="task">
          <div class="task-head">
            <span class="order-no">{{ task.orderNo || `任务 #${task.id}` }}</span>
            <span class="fee ew-price">¥{{ money(task.fee) }}</span>
          </div>
          <div class="route">
            <div class="point">
              <el-tag size="small" type="success" effect="plain">取</el-tag>
              <span>{{ task.pickupAddress }}</span>
            </div>
            <div class="point">
              <el-tag size="small" type="primary" effect="plain">送</el-tag>
              <span>{{ task.deliveryAddress }}</span>
            </div>
          </div>
          <div class="task-foot">
            <span class="ew-muted">
              距离 {{ distance(task.distance) }} · {{ task.createdAt }}
            </span>
            <el-button type="primary" size="small" :loading="grabbing === task.id" @click="grab(task)">
              立即抢单
            </el-button>
          </div>
        </div>

        <el-empty v-if="!loading && tasks.length === 0" description="暂时没有可抢的订单，歇会儿吧～" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { riderApi } from '@/api'
import { distance, money } from '@/utils/format'

const loading = ref(false)
const grabbing = ref(null)
const tasks = ref([])
const profile = ref(null)

async function load() {
  loading.value = true
  try {
    tasks.value = (await riderApi.available()) || []
  } finally {
    loading.value = false
  }
}

async function grab(task) {
  grabbing.value = task.id
  try {
    await riderApi.grab(task.id)
    ElMessage.success('抢单成功，请尽快取餐')
    await load()
  } finally {
    grabbing.value = null
  }
}

onMounted(async () => {
  profile.value = await riderApi.profile().catch(() => null)
  await load()
})
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}

.tasks {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 14px;
  margin-top: 10px;
  min-height: 100px;
}

.task {
  border: 1px solid var(--ew-border);
  border-radius: 10px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.task-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.order-no {
  font-weight: 600;
  font-size: 14px;
}

.fee {
  font-size: 18px;
}

.route {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13px;
  color: #606266;
}

.point {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  line-height: 1.5;
}

.task-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid var(--ew-border);
  padding-top: 10px;
}
</style>
