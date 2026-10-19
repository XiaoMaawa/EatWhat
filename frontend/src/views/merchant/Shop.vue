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
              <span v-else>🐋</span>
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
          <el-input v-model="form.address" maxlength="255" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" maxlength="20" />
        </el-form-item>
        <el-form-item label="经度">
          <el-input-number v-model="form.longitude" :precision="6" :step="0.000001" :controls="false" style="width: 200px" />
        </el-form-item>
        <el-form-item label="纬度">
          <el-input-number v-model="form.latitude" :precision="6" :step="0.000001" :controls="false" style="width: 200px" />
        </el-form-item>
        <el-form-item label="起送价" prop="minOrderAmount">
          <el-input-number v-model="form.minOrderAmount" :min="0" :precision="2" :step="1" style="width: 180px" />
          <span class="unit">元</span>
        </el-form-item>
        <el-form-item label="配送费" prop="deliveryFee">
          <el-input-number v-model="form.deliveryFee" :min="0" :precision="2" :step="1" style="width: 180px" />
          <span class="unit">元</span>
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
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="save">保存店铺信息</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { merchantApi, fileApi } from '@/api'
import { AUDIT_STATUS } from '@/utils/orderStatus'

const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const shop = ref(null)
const profile = ref(null)
const formRef = ref()
const openTime = ref('09:00')
const closeTime = ref('21:00')

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
  deliveryFeeType: 0,
  deliveryRange: 3000
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
        longitude: Number(shop.value.longitude ?? 0),
        latitude: Number(shop.value.latitude ?? 0),
        minOrderAmount: Number(shop.value.minOrderAmount ?? 0),
        deliveryFee: Number(shop.value.deliveryFee ?? 0),
        deliveryFeeType: Number(shop.value.deliveryFeeType ?? 0),
        deliveryRange: shop.value.deliveryRange ?? 3000
      })
      openTime.value = trimClock(shop.value.openTime)
      closeTime.value = trimClock(shop.value.closeTime)
    }
  } finally {
    loading.value = false
  }
}

async function save() {
  await formRef.value.validate()
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
</style>
