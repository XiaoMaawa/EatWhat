<template>
  <div class="ew-page">
    <div class="ew-card">
      <div class="ew-flex-between">
        <h3 class="ew-title">我的任务</h3>
        <el-radio-group v-model="tab" size="small" @change="search">
          <el-radio-button value="ongoing">进行中</el-radio-button>
          <el-radio-button value="history">历史任务</el-radio-button>
        </el-radio-group>
      </div>

      <el-table v-loading="loading" :data="list" size="default" class="table">
        <el-table-column prop="orderNo" label="订单号" width="190" />
        <el-table-column label="取餐地址" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.pickupAddress }}</template>
        </el-table-column>
        <el-table-column label="送达地址" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.deliveryAddress }}</template>
        </el-table-column>
        <el-table-column label="距离" width="100">
          <template #default="{ row }">{{ distance(row.distance) }}</template>
        </el-table-column>
        <el-table-column label="收益" width="100">
          <template #default="{ row }">¥{{ money(row.fee) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="TASK_STATUS[row.status]?.type">
              {{ TASK_STATUS[row.status]?.text || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="340" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 1 || row.status === 2" size="small" type="info" @click="navigate(row)">导航</el-button>
            <el-button v-if="row.status === 1" size="small" type="primary" @click="pickup(row)">确认取餐</el-button>
            <el-button v-if="row.status === 2" size="small" type="success" @click="deliver(row)">确认送达</el-button>
            <template v-if="row.status === 1 || row.status === 2">
              <el-button size="small" @click="openDialog(row, 'transfer')">转单</el-button>
              <el-button size="small" type="warning" plain @click="openDialog(row, 'report')">异常上报</el-button>
            </template>
            <span v-if="row.status >= 3" class="ew-muted">已完成</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && list.length === 0" description="没有任务" />
    </div>

    <el-dialog v-model="visible" :title="dialogType === 'transfer' ? '转单申请' : '配送异常上报'" width="520px">
      <el-alert
        v-if="dialogType === 'transfer'"
        type="warning"
        :closable="false"
        show-icon
        title="转单后本单将退回订单池，由其他骑手重新接单，本单收益不再计入。"
        class="tips"
      />
      <el-form :model="form" label-width="90px" class="form">
        <el-form-item label="异常原因">
          <el-select v-model="form.reasonType" placeholder="请选择原因" style="width: 100%">
            <el-option v-for="item in REASON_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="情况说明">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请描述具体情况，至少 5 个字，便于平台核实"
          />
        </el-form-item>
        <el-form-item label="凭证图片">
          <el-upload
            v-model:file-list="fileList"
            :http-request="uploadImage"
            :limit="3"
            list-type="picture-card"
            accept="image/*"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="ew-muted tip-text">选填，最多 3 张</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">{{ dialogType === 'transfer' ? '确认转单' : '提交上报' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { fileApi, riderApi } from '@/api'
import { distance, money } from '@/utils/format'

const TASK_STATUS = {
  0: { text: '待抢单', type: 'warning' },
  1: { text: '已接单', type: 'primary' },
  2: { text: '已取餐', type: 'info' },
  3: { text: '已送达', type: 'success' }
}

const REASON_OPTIONS = [
  { value: 1, label: '联系不上顾客' },
  { value: 2, label: '地址错误' },
  { value: 3, label: '餐品破损' },
  { value: 4, label: '车辆故障' },
  { value: 5, label: '天气原因' },
  { value: 6, label: '其他' }
]

const loading = ref(false)
const list = ref([])
const tab = ref('ongoing')

const visible = ref(false)
const submitting = ref(false)
const dialogType = ref('report')
const currentTask = ref(null)
const fileList = ref([])
const form = reactive({ reasonType: 1, description: '' })

async function load() {
  loading.value = true
  try {
    const data = await riderApi.tasks({ status: tab.value, page: 1, size: 50 })
    list.value = data?.records || []
  } finally {
    loading.value = false
  }
}

function search() {
  load()
}

function navigate(row) {
  const q = encodeURIComponent(row.deliveryAddress || '')
  window.open(`https://amap.com/dir?to=${q}`, '_blank')
}

async function pickup(row) {
  await ElMessageBox.confirm('确认已从商家取到餐品？', '确认取餐', { type: 'info' })
  await riderApi.pickup(row.id)
  ElMessage.success('已取餐，开始配送')
  await load()
}

async function deliver(row) {
  await ElMessageBox.confirm('确认已将餐品送达顾客？', '确认送达', { type: 'info' })
  await riderApi.deliver(row.id)
  ElMessage.success('已送达，收益已计入')
  await load()
}

function openDialog(row, type) {
  currentTask.value = row
  dialogType.value = type
  form.reasonType = type === 'transfer' ? 2 : 1
  form.description = ''
  fileList.value = []
  visible.value = true
}

async function uploadImage(options) {
  const data = await fileApi.upload(options.file)
  fileList.value = fileList.value.filter((f) => f.uid !== options.file.uid)
  fileList.value.push({ name: data.originalName || '凭证', url: data.url, uid: options.file.uid })
}

async function submit() {
  if (!form.description || form.description.trim().length < 5) {
    ElMessage.warning('请填写至少 5 个字的情况说明')
    return
  }
  submitting.value = true
  try {
    const payload = {
      type: dialogType.value === 'transfer' ? 1 : 2,
      reasonType: form.reasonType,
      description: form.description.trim(),
      images: fileList.value.map((f) => f.url).filter(Boolean)
    }
    if (dialogType.value === 'transfer') {
      try {
        await ElMessageBox.confirm('转单后本单将退回订单池，确定要转单吗？', '确认转单', {
          type: 'warning',
          confirmButtonText: '确定转单',
          cancelButtonText: '再想想'
        })
      } catch {
        return
      }
      await riderApi.transfer(currentTask.value.id, payload)
      ElMessage.success('已提交转单申请，订单已退回订单池')
    } else {
      await riderApi.reportException(currentTask.value.id, payload)
      ElMessage.success('异常已上报，平台将尽快处理')
    }
    visible.value = false
    await load()
  } catch {
    /* 错误提示已由 request 拦截器统一弹出 */
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
.tips {
  margin-bottom: 16px;
}
.form {
  margin-top: 4px;
}
.tip-text {
  font-size: 12px;
  margin-top: 4px;
}
</style>
