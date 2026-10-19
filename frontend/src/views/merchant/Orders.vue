<template>
  <div class="ew-page">
    <div class="ew-card">
      <h3 class="ew-title">订单管理</h3>

      <!-- 商家-37：新订单提醒 -->
      <el-alert
        v-if="pendingCount > 0"
        type="warning"
        show-icon
        :closable="false"
        class="new-order-alert"
      >
        <template #title>
          有 {{ pendingCount }} 笔新订单等待接单，请及时处理（超时 10 分钟将自动取消）
          <el-button size="small" type="warning" link @click="focusPending">查看</el-button>
        </template>
      </el-alert>

      <el-form :inline="true" class="filter">
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="item in ORDER_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="订单号">
          <el-input v-model="query.orderNo" placeholder="输入订单号" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="list" size="default">
        <el-table-column prop="orderNo" label="订单号" width="190" />
        <el-table-column label="菜品" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.itemSummary || '—' }}</template>
        </el-table-column>
        <el-table-column label="收货信息" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.receiverName }} · {{ row.receiverAddress }}
          </template>
        </el-table-column>
        <el-table-column label="金额" width="110">
          <template #default="{ row }">¥{{ money(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="orderStatusType(row.status)">{{ orderStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="下单时间" width="170" />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" size="small" type="success" @click="accept(row)">接单</el-button>
            <el-button v-if="row.status === 1" size="small" type="danger" @click="reject(row)">拒单</el-button>
            <el-button v-if="row.status === 2" size="small" type="primary" @click="ready(row)">出餐完成</el-button>
            <el-button size="small" text @click="showReceipt(row)">小票</el-button>
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

    <el-dialog v-model="receiptVisible" title="订单小票" width="420px">
      <div v-if="receiptData" id="receipt-print" class="receipt">
        <div class="r-center r-shop">{{ receiptData.shopName }}</div>
        <div class="r-center r-small">{{ receiptData.shopAddress }}</div>
        <div class="r-center r-small">电话：{{ receiptData.shopPhone || '—' }}</div>
        <div class="r-divider" />
        <div class="r-line"><span>订单号</span><span>{{ receiptData.orderNo }}</span></div>
        <div class="r-line"><span>下单时间</span><span>{{ receiptData.createdAt }}</span></div>
        <div class="r-divider" />
        <div class="r-line r-head"><span>菜品</span><span>数量 / 金额</span></div>
        <div v-for="(item, idx) in receiptData.items" :key="idx" class="r-line">
          <span class="r-item">
            {{ item.name }}
            <em v-if="item.specName" class="r-sub">{{ item.specName }}</em>
            <em v-if="item.addonNames" class="r-sub">{{ item.addonNames }}</em>
          </span>
          <span>{{ item.quantity }} × ¥{{ money(item.price) }} = ¥{{ money(item.subtotal) }}</span>
        </div>
        <div class="r-divider" />
        <div class="r-line"><span>商品金额</span><span>¥{{ money(receiptData.goodsAmount) }}</span></div>
        <div class="r-line"><span>配送费</span><span>¥{{ money(receiptData.deliveryFee) }}</span></div>
        <div v-if="Number(receiptData.packagingFee) > 0" class="r-line">
          <span>打包费</span><span>¥{{ money(receiptData.packagingFee) }}</span>
        </div>
        <div v-if="Number(receiptData.discountAmount) > 0" class="r-line">
          <span>优惠</span><span>-¥{{ money(receiptData.discountAmount) }}</span>
        </div>
        <div class="r-line r-total"><span>实付</span><span>¥{{ money(receiptData.totalAmount) }}</span></div>
        <div class="r-divider" />
        <div class="r-line"><span>收货人</span><span>{{ receiptData.receiverName }} {{ receiptData.receiverPhone }}</span></div>
        <div class="r-small">{{ receiptData.receiverAddress }}</div>
        <div v-if="Number(receiptData.tablewareCount) > 0" class="r-small">
          餐具份数：{{ receiptData.tablewareCount }}
        </div>
        <div v-if="receiptData.remark" class="r-small">备注：{{ receiptData.remark }}</div>
        <div class="r-divider" />
        <div class="r-center r-small">打印时间 {{ receiptData.printTime }}</div>
        <div class="r-center r-small">谢谢惠顾，欢迎再次光临</div>
      </div>
      <template #footer>
        <el-button @click="receiptVisible = false">关闭</el-button>
        <el-button type="primary" @click="printReceipt">打印</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { merchantApi } from '@/api'
import { money } from '@/utils/format'
import { ORDER_STATUS_OPTIONS, orderStatusText, orderStatusType } from '@/utils/orderStatus'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const dateRange = ref([])
const query = reactive({ status: null, orderNo: '', page: 1, size: 10 })

/** 商家-37：待接单数量与轮询状态 */
const pendingCount = computed(() => list.value.filter((row) => row.status === 1).length)
const knownPendingIds = ref(new Set())
let pollTimer = null
let firstPoll = true
let audioCtx = null

/** 播放新订单提示音（Web Audio，无需外部音频文件） */
function beep() {
  try {
    const Ctx = window.AudioContext || window.webkitAudioContext
    if (!Ctx) return
    if (!audioCtx) audioCtx = new Ctx()
    const ctx = audioCtx
    // 浏览器自动播放策略：未解锁时先静默，等用户首次交互后 resume 再响
    if (ctx.state === 'suspended') {
      ctx.resume().catch(() => {})
    }
    const now = ctx.currentTime
    // 两声短促提示音
    ;[0, 0.28].forEach((offset) => {
      const osc = ctx.createOscillator()
      const gain = ctx.createGain()
      osc.type = 'sine'
      osc.frequency.setValueAtTime(880, now + offset)
      gain.gain.setValueAtTime(0.001, now + offset)
      gain.gain.exponentialRampToValueAtTime(0.25, now + offset + 0.02)
      gain.gain.exponentialRampToValueAtTime(0.001, now + offset + 0.22)
      osc.connect(gain).connect(ctx.destination)
      osc.start(now + offset)
      osc.stop(now + offset + 0.24)
    })
  } catch {
    // 浏览器限制自动播放时静默降级，仅保留弹窗提醒
  }
}

/** 用户首次交互时解锁音频上下文，之后新订单提示音才能正常播放 */
function unlockAudio() {
  try {
    const Ctx = window.AudioContext || window.webkitAudioContext
    if (!Ctx) return
    if (!audioCtx) audioCtx = new Ctx()
    if (audioCtx.state === 'suspended') audioCtx.resume().catch(() => {})
  } catch {
    // 忽略
  }
}

/** 商家-37：轮询待接单订单，出现新订单时弹窗 + 提示音，30 秒未处理重复提醒 */
async function pollNewOrders() {
  try {
    const data = await merchantApi.orders({ status: 1, page: 1, size: 50 })
    const records = data?.records || []
    const currentIds = new Set(records.map((row) => row.id))
    const fresh = records.filter((row) => !knownPendingIds.value.has(row.id))
    if (!firstPoll && fresh.length > 0) {
      beep()
      ElNotification({
        title: '收到新订单',
        message: `${fresh[0].orderNo}（${fresh.length} 笔待接单），请及时处理`,
        type: 'warning',
        duration: 8000
      })
    }
    // 仍有未处理订单时每轮都提醒一次（约 30 秒），符合 30 秒重复提醒要求
    if (!firstPoll && fresh.length === 0 && currentIds.size > 0) {
      beep()
      ElNotification({
        title: '订单待处理',
        message: `仍有 ${currentIds.size} 笔订单未接单，超时 10 分钟将自动取消`,
        type: 'warning',
        duration: 6000
      })
    }
    knownPendingIds.value = currentIds
    firstPoll = false
  } catch {
    // 轮询失败静默忽略，避免打断正常操作
  }
}

function focusPending() {
  query.status = 1
  query.page = 1
  load()
}

async function load() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.status !== null && query.status !== '') params.status = query.status
    if (query.orderNo) params.orderNo = query.orderNo
    if (dateRange.value?.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const data = await merchantApi.orders(params)
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

function reset() {
  query.status = null
  query.orderNo = ''
  dateRange.value = []
  query.page = 1
  load()
}

async function accept(row) {
  await merchantApi.accept(row.id)
  ElMessage.success('已接单，请尽快备餐')
  await load()
}

async function reject(row) {
  const { value } = await ElMessageBox.prompt('请输入拒单原因', '拒单', {
    inputValue: '菜品已售罄',
    inputValidator: (v) => (v && v.length <= 100 ? true : '请填写 100 字以内的原因')
  })
  await merchantApi.reject(row.id, value)
  ElMessage.success('已拒单，订单将进入退款流程')
  await load()
}

async function ready(row) {
  await merchantApi.ready(row.id)
  ElMessage.success('已出餐，已通知骑手接单')
  await load()
}

const receiptVisible = ref(false)
const receiptData = ref(null)

async function showReceipt(row) {
  receiptData.value = await merchantApi.receipt(row.id)
  receiptVisible.value = true
}

function printReceipt() {
  const html = document.getElementById('receipt-print')?.outerHTML
  if (!html) return
  const win = window.open('', '_blank', 'width=420,height=640')
  if (!win) {
    ElMessage.warning('浏览器拦截了打印窗口，请允许弹窗后重试')
    return
  }
  win.document.write(`<html><head><title>小票</title><style>
    body { font-family: "Microsoft YaHei", sans-serif; padding: 12px; color: #222; }
    .receipt { width: 360px; }
    .r-center { text-align: center; }
    .r-shop { font-size: 17px; font-weight: 700; margin-bottom: 4px; }
    .r-small { font-size: 12px; color: #555; line-height: 1.6; }
    .r-line { display: flex; justify-content: space-between; gap: 8px; font-size: 13px; line-height: 1.8; }
    .r-head { font-weight: 600; }
    .r-total { font-size: 15px; font-weight: 700; margin-top: 2px; }
    .r-divider { border-top: 1px dashed #999; margin: 8px 0; }
    .r-sub { display: block; font-size: 11px; color: #666; font-style: normal; }
    .r-item { flex: 1; }
  </style></head><body>${html}</body></html>`)
  win.document.close()
  win.focus()
  win.print()
}

onMounted(() => {
  load()
  pollNewOrders()
  // 商家-37：每 30 秒检查一次待接单订单
  pollTimer = setInterval(pollNewOrders, 30000)
  // 浏览器要求用户交互后才允许播放声音，首次点击/按键时解锁
  window.addEventListener('pointerdown', unlockAudio, { once: true })
  window.addEventListener('keydown', unlockAudio, { once: true })
})

onBeforeUnmount(() => {
  if (pollTimer) clearInterval(pollTimer)
  window.removeEventListener('pointerdown', unlockAudio)
  window.removeEventListener('keydown', unlockAudio)
  if (audioCtx) {
    audioCtx.close().catch(() => {})
    audioCtx = null
  }
})
</script>

<style scoped>
.filter {
  margin-bottom: 6px;
}

.new-order-alert {
  margin-bottom: 12px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.receipt {
  font-size: 13px;
  color: #222;
}

.r-center {
  text-align: center;
}

.r-shop {
  font-size: 17px;
  font-weight: 700;
  margin-bottom: 4px;
}

.r-small {
  font-size: 12px;
  color: #555;
  line-height: 1.6;
}

.r-line {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  line-height: 1.8;
}

.r-head {
  font-weight: 600;
}

.r-total {
  font-size: 15px;
  font-weight: 700;
  margin-top: 2px;
}

.r-divider {
  border-top: 1px dashed #999;
  margin: 8px 0;
}

.r-sub {
  display: block;
  font-size: 11px;
  color: #666;
  font-style: normal;
}

.r-item {
  flex: 1;
}
</style>
