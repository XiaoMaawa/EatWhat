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
      <div class="filter-bar">
        <span class="filter-label">排序</span>
        <el-radio-group v-model="sortBy" size="small" @change="load">
          <el-radio-button value="">默认</el-radio-button>
          <el-radio-button value="distance">距离优先</el-radio-button>
          <el-radio-button value="fee">收益优先</el-radio-button>
        </el-radio-group>
        <span class="filter-label">距离</span>
        <el-select v-model="distanceRange" size="small" style="width: 110px" @change="applyDistanceFilter">
          <el-option label="全部" value="" />
          <el-option label="3 公里内" value="0-3" />
          <el-option label="3-5 公里" value="3-5" />
          <el-option label="5 公里以上" value="5-" />
        </el-select>
        <span class="filter-label">配送费</span>
        <el-select v-model="feeRange" size="small" style="width: 110px" @change="applyFeeFilter">
          <el-option label="全部" value="" />
          <el-option label="5 元以下" value="-5" />
          <el-option label="5-8 元" value="5-8" />
          <el-option label="8 元以上" value="8-" />
        </el-select>
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

        <el-empty v-if="!loading && tasks.length === 0" description="暂时没有可抢的订单" />
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

    <el-dialog v-model="filterVisible" title="订单筛选" width="420px">
      <el-form label-width="90px">
        <el-form-item label="配送距离">
          <div class="filter-row">
            <el-input-number v-model="filter.minDistance" :min="0" :max="20" :step="0.5" controls-position="right" />
            <span class="unit">至</span>
            <el-input-number v-model="filter.maxDistance" :min="0" :max="20" :step="0.5" controls-position="right" />
            <span class="unit">公里</span>
          </div>
        </el-form-item>
        <el-form-item label="配送费">
          <div class="filter-row">
            <el-input-number v-model="filter.minFee" :min="0" :max="50" :step="0.5" controls-position="right" />
            <span class="unit">至</span>
            <el-input-number v-model="filter.maxFee" :min="0" :max="50" :step="0.5" controls-position="right" />
            <span class="unit">元</span>
          </div>
        </el-form-item>
        <el-form-item label="">
          <el-button link type="primary" @click="resetFilter">清空筛选条件</el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="filterVisible = false">取消</el-button>
        <el-button type="primary" @click="applyFilter">应用</el-button>
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

// 骑手-09/13：接单偏好排序与区间筛选
const sortBy = ref('')
const distanceRange = ref('')
const feeRange = ref('')
const filterVisible = ref(false)
const filter = reactive({ minDistance: undefined, maxDistance: undefined, minFee: undefined, maxFee: undefined })

// 快捷区间下拉解析
function applyDistanceFilter() {
  const v = distanceRange.value
  if (v === '') { filter.minDistance = undefined; filter.maxDistance = undefined }
  else if (v === '0-3') { filter.minDistance = undefined; filter.maxDistance = 3 }
  else if (v === '3-5') { filter.minDistance = 3; filter.maxDistance = 5 }
  else if (v === '5-') { filter.minDistance = 5; filter.maxDistance = undefined }
  load()
}
function applyFeeFilter() {
  const v = feeRange.value
  if (v === '') { filter.minFee = undefined; filter.maxFee = undefined }
  else if (v === '-5') { filter.minFee = undefined; filter.maxFee = 5 }
  else if (v === '5-8') { filter.minFee = 5; filter.maxFee = 8 }
  else if (v === '8-') { filter.minFee = 8; filter.maxFee = undefined }
  load()
}

let timer = null

async function load() {
  loading.value = true
  try {
    const params = {}
    if (sortBy.value) params.sortBy = sortBy.value
    if (filter.minDistance != null) params.minDistance = filter.minDistance
    if (filter.maxDistance != null) params.maxDistance = filter.maxDistance
    if (filter.minFee != null) params.minFee = filter.minFee
    if (filter.maxFee != null) params.maxFee = filter.maxFee
    tasks.value = (await riderApi.available(params)) || []
  } catch {
    tasks.value = []
  } finally {
    loading.value = false
  }
}

function applyFilter() {
  filterVisible.value = false
  load()
}

function resetFilter() {
  filter.minDistance = undefined
  filter.maxDistance = undefined
  filter.minFee = undefined
  filter.maxFee = undefined
  load()
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
  } catch (e) {
    // 抢单失败（订单已结束 / 已被抢走）时刷新列表，避免僵尸任务一直挂在页面上
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

.filter-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-row .unit {
  color: #909399;
  font-size: 12px;
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

.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 12px 0 4px;
  flex-wrap: wrap;
}
.filter-label {
  color: #909399;
  font-size: 13px;
}
</style>
