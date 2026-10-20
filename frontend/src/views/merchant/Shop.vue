<template>
  <div class="ew-page" v-loading="loading">
    <el-alert
      v-if="!shop && profile"
      type="info"
      show-icon
      :closable="false"
      class="audit-alert"
      title="还没有店铺信息"
    >
      <template #default>
        当前账号的商家资质状态：{{ AUDIT_STATUS[profile.auditStatus]?.text || '未知' }}。
        店铺由平台在审核通过后创建，审核备注：{{ profile.auditRemark || '无' }}
      </template>
    </el-alert>

    <div class="ew-card" v-if="shop">
      <div class="ew-flex-between">
        <h3 class="ew-title">店铺管理</h3>
        <div class="ew-flex ew-gap8">
          <span class="ew-muted">营业状态</span>
          <el-switch
            v-model="shop.isOpen"
            :active-value="1"
            :inactive-value="0"
            active-text="营业中"
            inactive-text="已打烊"
            @change="toggleOpen"
          />
        </div>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" class="form">
        <el-form-item label="店铺名称" prop="name">
          <el-input v-model="form.name" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="店铺 Logo">
          <div class="logo-row">
            <div class="logo-box">
              <img v-if="form.logo" :src="form.logo" alt="" />
              <span v-else>🍜</span>
            </div>
            <el-upload :show-file-list="false" :before-upload="beforeUpload" :http-request="uploadLogo" accept="image/*">
              <el-button size="small" :loading="uploading">上传 Logo</el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="店铺简介">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="店铺公告">
          <el-input v-model="form.notice" type="textarea" :rows="2" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item label="店铺地址" prop="address">
          <div class="addr-row">
            <el-input v-model="form.address" maxlength="255" placeholder="先填详细地址，再点右侧在地图上选点" />
            <el-button :icon="Location" @click="openMap">地图选点</el-button>
          </div>
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" maxlength="20" />
        </el-form-item>
        <el-form-item label="坐标">
          <div class="coord-row">
            <span class="coord-item">
              经度 <b>{{ form.longitude ?? '未设置' }}</b>
            </span>
            <span class="coord-item">
              纬度 <b>{{ form.latitude ?? '未设置' }}</b>
            </span>
            <el-tag v-if="hasCoord" size="small" type="success" effect="plain">已定位</el-tag>
            <el-tag v-else size="small" type="warning" effect="plain">未定位</el-tag>
            <span class="ew-muted coord-tip">用于计算配送距离与配送范围</span>
          </div>
        </el-form-item>
        <el-form-item label="起送价" prop="minOrderAmount">
          <el-input-number v-model="form.minOrderAmount" :min="0" :precision="2" :step="1" style="width: 180px" />
          <span class="unit">元</span>
        </el-form-item>
        <el-form-item label="配送费" prop="deliveryFee">
          <el-input-number v-model="form.deliveryFee" :min="0" :precision="2" :step="1" style="width: 180px" />
          <span class="unit">元</span>
        </el-form-item>
        <el-form-item label="打包费" prop="packagingFee">
          <el-input-number v-model="form.packagingFee" :min="0" :max="20" :precision="2" :step="0.5" style="width: 180px" />
          <span class="unit">元（0-20，随订单收取）</span>
        </el-form-item>
        <el-form-item label="计费方式">
          <el-radio-group v-model="form.deliveryFeeType">
            <el-radio :value="0">固定配送费</el-radio>
            <el-radio :value="1">按距离阶梯计价</el-radio>
          </el-radio-group>
          <div v-if="form.deliveryFeeType === 1" class="unit fee-tip">
            3 公里内收 {{ form.deliveryFee }} 元，每超出 1 公里加收 1 元
          </div>
        </el-form-item>
        <el-form-item label="配送范围" prop="deliveryRange">
          <el-input-number v-model="form.deliveryRange" :min="100" :step="100" style="width: 180px" />
          <span class="unit">米</span>
        </el-form-item>
        <el-form-item label="营业时间">
          <div class="ew-flex ew-gap8">
            <el-time-picker v-model="openTime" format="HH:mm" value-format="HH:mm" placeholder="开始" />
            <span>—</span>
            <el-time-picker v-model="closeTime" format="HH:mm" value-format="HH:mm" placeholder="结束" />
          </div>
          <div class="ew-muted tip-line">未配置下方「按星期营业时段」时，以此处的每日营业时间为准</div>
        </el-form-item>
        <el-form-item label="按星期时段">
          <div class="weekly">
            <div class="weekly-tip ew-muted">
              配置后按星期生效，同一天可设置多个时段（如午市、晚市），留空则沿用上方营业时间
            </div>
            <div v-for="row in businessHours" :key="row.key" class="weekly-row">
              <el-select v-model="row.dayOfWeek" placeholder="星期" style="width: 110px">
                <el-option v-for="d in WEEK_OPTIONS" :key="d.value" :label="d.label" :value="d.value" />
              </el-select>
              <el-time-picker v-model="row.openTime" format="HH:mm" value-format="HH:mm" placeholder="开始" />
              <span>—</span>
              <el-time-picker v-model="row.closeTime" format="HH:mm" value-format="HH:mm" placeholder="结束" />
              <el-button link type="danger" @click="removeHour(row)">删除</el-button>
            </div>
            <div class="weekly-actions">
              <el-button size="small" @click="addHour">添加时段</el-button>
              <el-button size="small" type="primary" :loading="savingHours" @click="saveHours">保存时段</el-button>
              <span class="ew-muted">共 {{ businessHours.length }} 个时段</span>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="打烊提示语">
          <el-input
            v-model="form.closedTip"
            maxlength="200"
            show-word-limit
            placeholder="打烊后顾客在店铺页看到的提示，如：今日已打烊，明早 9 点见"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="save">保存店铺信息</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-dialog v-model="mapVisible" title="在地图上选择店铺位置" width="720px" destroy-on-close>
      <MapPicker
        :longitude="form.longitude"
        :latitude="form.latitude"
        :address="form.address"
        @confirm="onMapConfirm"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Location } from '@element-plus/icons-vue'
import { merchantApi, fileApi } from '@/api'
import { AUDIT_STATUS } from '@/utils/orderStatus'
import MapPicker from '@/components/MapPicker.vue'

const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const mapVisible = ref(false)
const shop = ref(null)
const profile = ref(null)
const formRef = ref()
const openTime = ref('09:00')
const closeTime = ref('21:00')
const savingHours = ref(false)

const hasCoord = computed(() => form.longitude != null && form.latitude != null)

function openMap() {
  mapVisible.value = true
}

function onMapConfirm({ longitude, latitude }) {
  form.longitude = longitude
  form.latitude = latitude
  mapVisible.value = false
  ElMessage.success(`已选点：${longitude}, ${latitude}`)
}

const WEEK_OPTIONS = [
  { value: 1, label: '周一' },
  { value: 2, label: '周二' },
  { value: 3, label: '周三' },
  { value: 4, label: '周四' },
  { value: 5, label: '周五' },
  { value: 6, label: '周六' },
  { value: 7, label: '周日' }
]
let hourKey = 0
const businessHours = ref([])

const form = reactive({
  name: '',
  logo: '',
  description: '',
  notice: '',
  address: '',
  phone: '',
  longitude: null,
  latitude: null,
  minOrderAmount: 20,
  deliveryFee: 3,
  packagingFee: 0,
  deliveryFeeType: 0,
  deliveryRange: 3000,
  closedTip: ''
})

const rules = {
  name: [{ required: true, message: '请填写店铺名称', trigger: 'blur' }],
  address: [{ required: true, message: '请填写店铺地址', trigger: 'blur' }],
  phone: [{ required: true, message: '请填写联系电话', trigger: 'blur' }],
  minOrderAmount: [{ required: true, message: '请填写起送价', trigger: 'blur' }],
  deliveryFee: [{ required: true, message: '请填写配送费', trigger: 'blur' }],
  deliveryRange: [{ required: true, message: '请填写配送范围', trigger: 'blur' }]
}

function trimClock(value) {
  if (!value) return '00:00'
  return String(value).slice(0, 5)
}

async function load() {
  loading.value = true
  try {
    profile.value = await merchantApi.profile().catch(() => null)
    shop.value = await merchantApi.shop().catch(() => null)
    if (shop.value) {
      Object.assign(form, {
        name: shop.value.name,
        logo: shop.value.logo,
        description: shop.value.description,
        notice: shop.value.notice,
        address: shop.value.address,
        phone: shop.value.phone,
        longitude: shop.value.longitude == null ? null : Number(shop.value.longitude),
        latitude: shop.value.latitude == null ? null : Number(shop.value.latitude),
        minOrderAmount: Number(shop.value.minOrderAmount ?? 0),
        deliveryFee: Number(shop.value.deliveryFee ?? 0),
        packagingFee: Number(shop.value.packagingFee ?? 0),
        deliveryFeeType: Number(shop.value.deliveryFeeType ?? 0),
        deliveryRange: shop.value.deliveryRange ?? 3000,
        closedTip: shop.value.closedTip || ''
      })
      openTime.value = trimClock(shop.value.openTime)
      closeTime.value = trimClock(shop.value.closeTime)
    }
    const hours = await merchantApi.businessHours().catch(() => [])
    businessHours.value = (hours || []).map((h) => ({
      key: ++hourKey,
      dayOfWeek: h.dayOfWeek,
      openTime: trimClock(h.openTime),
      closeTime: trimClock(h.closeTime)
    }))
  } finally {
    loading.value = false
  }
}

async function save() {
  await formRef.value.validate()
  if (!hasCoord.value) {
    ElMessage.warning('请先在地图上选择店铺位置，否则无法计算配送距离')
    return
  }
  saving.value = true
  try {
    await merchantApi.updateShop({
      ...form,
      openTime: openTime.value,
      closeTime: closeTime.value
    })
    ElMessage.success('店铺信息已保存')
    await load()
  } finally {
    saving.value = false
  }
}

async function toggleOpen(value) {
  try {
    await merchantApi.toggleShop(value)
    ElMessage.success(value === 1 ? '店铺已开始营业' : '店铺已打烊')
  } catch {
    shop.value.isOpen = value === 1 ? 0 : 1
  }
}

function addHour() {
  if (businessHours.value.length >= 21) {
    ElMessage.warning('营业时段最多 21 条')
    return
  }
  businessHours.value.push({
    key: ++hourKey,
    dayOfWeek: new Date().getDay() === 0 ? 7 : new Date().getDay(),
    openTime: '09:00',
    closeTime: '21:00'
  })
}

function removeHour(row) {
  businessHours.value = businessHours.value.filter((h) => h.key !== row.key)
}

async function saveHours() {
  for (const row of businessHours.value) {
    if (!row.dayOfWeek) {
      ElMessage.warning('请选择每个时段的星期')
      return
    }
    if (!row.openTime || !row.closeTime) {
      ElMessage.warning('请填写每个时段的起止时间')
      return
    }
    if (row.openTime === row.closeTime) {
      ElMessage.warning('营业起止时间不能相同')
      return
    }
  }
  savingHours.value = true
  try {
    const data = await merchantApi.saveBusinessHours({
      items: businessHours.value.map((h) => ({
        dayOfWeek: h.dayOfWeek,
        openTime: h.openTime,
        closeTime: h.closeTime
      }))
    })
    businessHours.value = (data || []).map((h) => ({
      key: ++hourKey,
      dayOfWeek: h.dayOfWeek,
      openTime: trimClock(h.openTime),
      closeTime: trimClock(h.closeTime)
    }))
    ElMessage.success(businessHours.value.length ? '营业时段已保存' : '已清空营业时段，恢复每日营业时间')
  } finally {
    savingHours.value = false
  }
}

function beforeUpload(file) {
  if (!file.type.startsWith('image/')) {
    ElMessage.error('只能上传图片')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片不能超过 5MB')
    return false
  }
  return true
}

async function uploadLogo(options) {
  uploading.value = true
  try {
    const data = await fileApi.upload(options.file)
    form.logo = data.url
    ElMessage.success('Logo 已上传，记得保存')
  } finally {
    uploading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.audit-alert {
  margin-bottom: 12px;
}

.form {
  max-width: 620px;
}

.logo-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.logo-box {
  width: 64px;
  height: 64px;
  border-radius: 10px;
  background: #fafafa;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  overflow: hidden;
}

.logo-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.unit {
  margin-left: 8px;
  color: #909399;
}
.fee-tip {
  width: 100%;
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
}

.tip-line {
  width: 100%;
  font-size: 12px;
  margin-top: 4px;
}

.weekly {
  width: 100%;
}

.weekly-tip {
  font-size: 12px;
  margin-bottom: 8px;
}

.weekly-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.weekly-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 4px;
}

.addr-row {
  display: flex;
  gap: 8px;
  width: 100%;
}

.coord-row {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.coord-item {
  font-size: 13px;
  color: #606266;
}

.coord-item b {
  color: #303133;
  font-weight: 600;
}

.coord-tip {
  font-size: 12px;
}
</style>
