<template>
  <div class="ew-page" v-loading="loading">
    <div class="ew-card">
      <div class="ew-flex-between">
        <h3 class="ew-title">收货地址</h3>
        <div class="ew-flex ew-gap8">
          <el-input
            v-model="keyword"
            placeholder="搜索联系人/手机号/地址/标签"
            clearable
            style="width: 260px"
            :prefix-icon="Search"
            @input="onSearch"
          />
          <el-button type="primary" @click="openDialog()">新增地址</el-button>
        </div>
      </div>

      <div class="addresses">
        <div v-for="addr in list" :key="addr.id" class="address">
          <div class="address-main">
            <div class="address-head">
              <span class="name">{{ addr.contactName }}</span>
              <span class="ew-muted">{{ addr.phone }}</span>
              <el-tag v-if="addr.isDefault === 1" size="small" type="success">默认</el-tag>
              <el-tag v-if="addr.tag" size="small" type="warning" effect="plain">{{ addr.tag }}</el-tag>
            </div>
            <div class="address-detail">
              {{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detail }}
            </div>
          </div>
          <div class="address-actions">
            <el-button v-if="addr.isDefault !== 1" link type="primary" @click="setDefault(addr)">设为默认</el-button>
            <el-button link type="primary" @click="openDialog(addr)">编辑</el-button>
            <el-button link type="danger" @click="remove(addr)">删除</el-button>
          </div>
        </div>
        <el-empty v-if="!loading && list.length === 0" :description="keyword ? '没有匹配的地址' : '还没有收货地址，先加一个吧'" />
      </div>
    </div>

    <el-dialog v-model="visible" :title="form.id ? '编辑地址' : '新增地址'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="form.contactName" maxlength="20" placeholder="收货人姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" maxlength="11" placeholder="11 位手机号" />
        </el-form-item>
        <el-form-item label="省市区" required>
          <div class="region-row">
            <el-form-item prop="province" class="region-item">
              <el-input v-model="form.province" placeholder="省" />
            </el-form-item>
            <el-form-item prop="city" class="region-item">
              <el-input v-model="form.city" placeholder="市" />
            </el-form-item>
            <el-form-item prop="district" class="region-item">
              <el-input v-model="form.district" placeholder="区/县" />
            </el-form-item>
          </div>
        </el-form-item>
        <el-form-item label="详细地址" prop="detail">
          <el-input v-model="form.detail" type="textarea" :rows="2" maxlength="120" show-word-limit placeholder="街道、门牌号、楼栋等" />
        </el-form-item>
        <el-form-item label="地址标签">
          <el-radio-group v-model="form.tag">
            <el-radio-button value="家">家</el-radio-button>
            <el-radio-button value="公司">公司</el-radio-button>
            <el-radio-button value="学校">学校</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { userApi } from '@/api'
import { Search } from '@element-plus/icons-vue'

const loading = ref(false)
const submitting = ref(false)
const visible = ref(false)
const list = ref([])
const formRef = ref()

// 顾客-16：地址搜索定位（300ms 防抖，走后端 /users/me/addresses/search）
const keyword = ref('')
let searchTimer = null
function onSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(async () => {
    const kw = keyword.value.trim()
    if (!kw) {
      await load()
      return
    }
    list.value = (await userApi.searchAddresses(kw)) || []
  }, 300)
}

const form = reactive({
  id: null,
  contactName: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  isDefault: 0,
  tag: '家'
})

const rules = {
  contactName: [{ required: true, message: '请填写联系人', trigger: 'blur' }],
  phone: [
    { required: true, message: '请填写手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  province: [{ required: true, message: '请填写省份', trigger: 'blur' }],
  city: [{ required: true, message: '请填写城市', trigger: 'blur' }],
  district: [{ required: true, message: '请填写区县', trigger: 'blur' }],
  detail: [
    { required: true, message: '请填写详细地址', trigger: 'blur' },
    { min: 5, message: '详细地址至少 5 个字', trigger: 'blur' }
  ]
}

async function load() {
  loading.value = true
  try {
    list.value = (await userApi.addresses()) || []
  } finally {
    loading.value = false
  }
}

function openDialog(addr) {
  Object.assign(form, {
    id: null,
    contactName: '',
    phone: '',
    province: '',
    city: '',
    district: '',
    detail: '',
    isDefault: list.value.length === 0 ? 1 : 0,
    tag: '家'
  })
  if (addr) {
    Object.assign(form, addr)
    if (!form.tag) {
      form.tag = '家'
    }
  }
  visible.value = true
}

async function submit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload = { ...form }
    delete payload.id
    if (form.id) {
      await userApi.updateAddress(form.id, payload)
    } else {
      await userApi.addAddress(payload)
    }
    ElMessage.success('已保存')
    visible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

async function setDefault(addr) {
  await userApi.updateAddress(addr.id, { ...addr, isDefault: 1 })
  ElMessage.success('已设为默认地址')
  await load()
}

async function remove(addr) {
  await ElMessageBox.confirm(`确定删除「${addr.contactName}」的地址吗？`, '删除地址', { type: 'warning' })
  await userApi.deleteAddress(addr.id)
  ElMessage.success('已删除')
  await load()
}

onMounted(load)
</script>

<style scoped>
.addresses {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 100px;
}

.address {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid var(--ew-border);
  border-radius: 10px;
}

.address-main {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.address-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.name {
  font-weight: 600;
}

.address-detail {
  font-size: 14px;
  color: #606266;
}

.address-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.region-row {
  display: flex;
  gap: 8px;
  width: 100%;
}

.region-item {
  flex: 1;
  margin-bottom: 0;
}
</style>
