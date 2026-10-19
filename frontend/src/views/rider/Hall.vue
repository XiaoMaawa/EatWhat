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
        <div class="ew-flex ew-gap8">
          <div v-if="profile" class="accept-switch">
            <span class="switch-label">{{ settings.accepting ? '接单中' : '已暂停接单' }}</span>
            <el-switch
              v-model="settings.accepting"
              :active-value="1"
              :inactive-value="0"
              :loading="savingSettings"
              @change="saveSettings"
            />
          </div>
          <el-button @click="settingsVisible = true">接单设置</el-button>
          <el-button :loading="loading" @click="load">刷新</el-button>
        </div>
      </div>

      <div v-if="profile" class="settings-hint">
        接单半径 {{ settings.maxDistance }} 公里 · 最多同时 {{ settings.maxOrders }} 单 · 每 30 秒自动刷新
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

    <el-dialog v-model="settingsVisible" title="接单设置" width="420px">
      <el-form label-width="110px">
        <el-form-item label="接单状态">
          <el-switch v-model="settings.accepting" :active-value="1" :inactive-value="0" active-text="接单中" />
        </el-form-item>
        <el-form-item label="接单半径">
          <el-slider v-model="settings.maxDistance" :min="1" :max="10" :step="0.5" show-input :show-input-controls="false" />
          <span class="unit">公里</span>
        </el-form-item>
        <el-form-item label="同时接单上限">
          <el-input-number v-model="settings.maxOrders" :min="1" :max="10" />
          <span class="unit">单</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="settingsVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingSettings" @click="saveSettings(true)">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { riderApi } from '@/api'
import { distance, money } from '@/utils/format'

const loading = ref(false)
const grabbing = ref(null)
const tasks = ref([])
const profile = ref(null)
const settingsVisible = ref(false)
const savingSettings = ref(false)
const settings = reactive({ accepting: 1, maxDistance: 5, maxOrders: 5 })

let timer = null

async function load() {
  loading.value = true
  try {
    tasks.value = (await riderApi.available()) || []
  } catch {
    tasks.value = []
  } finally {
    loading.value = false
  }
}

async function saveSettings(closeDialog = false) {
  savingSettings.value = true
  try {
    await riderApi.updateSettings({
      accepting: settings.accepting,
      maxDistance: settings.maxDistance,
      maxOrders: settings.maxOrders
    })
    ElMessage.success('接单设置已保存')
    if (closeDialog) {
      settingsVisible.value = false
    }
    await load()
  } finally {
    savingSettings.value = false
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
  if (profile.value) {
    settings.accepting = profile.value.accepting ?? 1
    settings.maxDistance = Number(profile.value.maxDistance ?? 5)
    settings.maxOrders = profile.value.maxOrders ?? 5
  }
  await load()
  // 每 30 秒自动刷新订单池
  timer = setInterval(load, 30000)
})

onBeforeUnmount(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}

.accept-switch {
  display: flex;
  align-items: center;
  gap: 8px;
}

.switch-label {
  font-size: 13px;
  color: var(--ew-text-sub);
}

.settings-hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--ew-text-sub);
}

.unit {
  margin-left: 8px;
  font-size: 13px;
  color: var(--ew-text-sub);
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
