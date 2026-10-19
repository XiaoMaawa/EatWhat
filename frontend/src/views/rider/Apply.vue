<template>
  <div class="ew-page" v-loading="loading">
    <el-alert
      v-if="status === 'rejected'"
      type="error"
      show-icon
      :closable="false"
      class="tip"
      title="骑手认证未通过"
    >
      <template #default>
        驳回原因：{{ profile?.auditRemark || '平台未填写具体原因' }}。修改资料后可以重新提交。
      </template>
    </el-alert>

    <el-alert
      v-else-if="status === 'pending'"
      type="warning"
      show-icon
      :closable="false"
      class="tip"
      title="认证资料已提交，等待平台审核"
    >
      <template #default>审核通过后就能在抢单大厅接单啦。</template>
    </el-alert>

    <el-alert
      v-else-if="status === 'passed'"
      type="success"
      show-icon
      :closable="false"
      class="tip"
      title="骑手认证已通过"
    >
      <template #default>
        快去
        <el-link type="primary" :underline="false" @click="router.push('/rider/hall')">抢单大厅</el-link>
        接单吧～
      </template>
    </el-alert>

    <el-alert v-else type="info" show-icon :closable="false" class="tip" title="还没有提交骑手认证">
      <template #default>需要提交实名信息与健康证，平台审核通过后才可接单。</template>
    </el-alert>

    <div class="ew-card">
      <h3 class="ew-title">{{ status === 'passed' ? '认证资料' : '提交骑手认证' }}</h3>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="110px"
        class="form"
        :disabled="status === 'pending' || status === 'passed'"
      >
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" maxlength="50" placeholder="与身份证一致" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="form.idCard" maxlength="18" placeholder="18 位身份证号" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" maxlength="11" placeholder="11 位手机号" />
        </el-form-item>
        <el-form-item label="健康证" prop="healthImg">
          <div class="upload-row">
            <div class="upload-box">
              <img v-if="form.healthImg" :src="form.healthImg" alt="健康证" />
              <span v-else>🩺</span>
            </div>
            <div class="upload-side">
              <el-upload
                :show-file-list="false"
                :before-upload="beforeUpload"
                :http-request="uploadHealth"
                accept="image/*"
              >
                <el-button size="small" :loading="uploading">上传图片</el-button>
              </el-upload>
              <div class="ew-muted hint">支持 JPG / PNG / WEBP，不超过 5MB</div>
              <el-input v-model="form.healthImg" size="small" placeholder="也可以直接粘贴图片地址" />
            </div>
          </div>
        </el-form-item>

        <el-form-item v-if="status !== 'pending' && status !== 'passed'">
          <el-button type="primary" :loading="submitting" @click="submit">
            {{ status === 'rejected' ? '重新提交' : '提交认证' }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { riderApi, fileApi } from '@/api'

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const uploading = ref(false)
const profile = ref(null)
const formRef = ref()

const form = reactive({
  realName: '',
  idCard: '',
  contactPhone: '',
  healthImg: ''
})

const rules = {
  realName: [{ required: true, message: '请填写真实姓名', trigger: 'blur' }],
  idCard: [
    { required: true, message: '请填写身份证号', trigger: 'blur' },
    { pattern: /^\d{17}[\dXx]$/, message: '身份证号格式不正确', trigger: 'blur' }
  ],
  contactPhone: [
    { required: true, message: '请填写联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  healthImg: [{ required: true, message: '请上传健康证', trigger: 'change' }]
}

/** none 未认证 / pending 待审核 / passed 已通过 / rejected 已驳回 */
const status = computed(() => {
  const p = profile.value
  if (!p) return 'none'
  if (p.auditStatus === 1) return 'passed'
  if (p.auditStatus === 2) return 'rejected'
  return 'pending'
})

async function load() {
  loading.value = true
  try {
    profile.value = await riderApi.profile()
    if (profile.value) {
      form.realName = profile.value.realName || ''
      form.contactPhone = profile.value.contactPhone || ''
      form.healthImg = profile.value.healthImg || ''
    }
  } finally {
    loading.value = false
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

async function uploadHealth(options) {
  uploading.value = true
  try {
    const data = await fileApi.upload(options.file)
    form.healthImg = data.url
    ElMessage.success('健康证已上传')
  } finally {
    uploading.value = false
  }
}

async function submit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    await riderApi.apply({ ...form })
    ElMessage.success('认证申请已提交，请等待平台审核')
    await load()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}

.form {
  max-width: 620px;
}

.upload-row {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.upload-box {
  width: 120px;
  height: 90px;
  border-radius: 10px;
  background: #fafafa;
  border: 1px dashed #dcdfe6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  overflow: hidden;
  flex: none;
}

.upload-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-side {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.hint {
  font-size: 12px;
  line-height: 1.4;
}
</style>
