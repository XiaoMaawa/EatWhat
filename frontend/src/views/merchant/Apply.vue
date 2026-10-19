<template>
  <div class="ew-page" v-loading="loading">
    <el-alert
      v-if="status === 'rejected'"
      type="error"
      show-icon
      :closable="false"
      class="tip"
      title="入驻申请未通过"
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
      title="入驻资料已提交，等待平台审核"
    >
      <template #default>
        审核通常在 1 个工作日内完成，通过后系统会自动为你创建店铺，届时即可上架菜品。
      </template>
    </el-alert>

    <el-alert
      v-else-if="status === 'passed'"
      type="success"
      show-icon
      :closable="false"
      class="tip"
      title="入驻审核已通过"
    >
      <template #default>
        店铺已经开通，快去
        <el-link type="primary" :underline="false" @click="router.push('/merchant/shop')">完善店铺信息</el-link>
        吧
      </template>
    </el-alert>

    <el-alert
      v-else
      type="info"
      show-icon
      :closable="false"
      class="tip"
      title="还没有提交入驻申请"
    >
      <template #default>
        成为 EatWhat 商家需要先提交实名与营业执照信息，平台审核通过后即可开店。
      </template>
    </el-alert>

    <div class="ew-card">
      <h3 class="ew-title">{{ status === 'passed' ? '入驻资料' : '提交入驻申请' }}</h3>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="110px"
        class="form"
        :disabled="status === 'pending' || status === 'passed'"
      >
        <el-form-item label="经营者姓名" prop="realName">
          <el-input v-model="form.realName" maxlength="50" placeholder="与身份证一致" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="form.idCard" maxlength="18" placeholder="18 位身份证号" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" maxlength="11" placeholder="11 位手机号" />
        </el-form-item>
        <el-form-item label="营业执照" prop="licenseImg">
          <div class="upload-row">
            <div class="upload-box">
              <img v-if="form.licenseImg" :src="form.licenseImg" alt="营业执照" />
              <span v-else>📄</span>
            </div>
            <div class="upload-side">
              <el-upload
                :show-file-list="false"
                :before-upload="beforeUpload"
                :http-request="uploadLicense"
                accept="image/*"
              >
                <el-button size="small" :loading="uploading">上传图片</el-button>
              </el-upload>
              <div class="ew-muted hint">支持 JPG / PNG / WEBP，不超过 5MB</div>
              <el-input v-model="form.licenseImg" size="small" placeholder="也可以直接粘贴图片地址" />
            </div>
          </div>
        </el-form-item>

        <el-form-item v-if="status !== 'pending' && status !== 'passed'">
          <el-checkbox v-model="agreed">
            我已阅读并同意
            <el-link type="primary" :underline="false" @click.prevent.stop="agreementVisible = true">
              《EatWhat 平台商家入驻协议》
            </el-link>
          </el-checkbox>
        </el-form-item>

        <el-form-item v-if="status !== 'pending' && status !== 'passed'">
          <el-button type="primary" :loading="submitting" :disabled="!agreed" @click="submit">
            {{ status === 'rejected' ? '重新提交' : '提交申请' }}
          </el-button>
          <span v-if="!agreed" class="ew-muted agree-hint">请先勾选同意入驻协议</span>
        </el-form-item>
      </el-form>
    </div>

    <!-- 商家-05：入驻协议 -->
    <el-dialog v-model="agreementVisible" title="EatWhat 平台商家入驻协议" width="640px">
      <div class="agreement">
        <p>欢迎您入驻 EatWhat 外卖平台。在提交入驻申请前，请您仔细阅读以下条款：</p>
        <p><b>一、入驻资质</b><br />商家应提供真实、合法、有效的营业执照与经营者身份证件，并对所售食品的安全与质量承担全部责任。</p>
        <p><b>二、经营范围</b><br />商家应在营业执照核准的经营范围内开展经营活动，不得销售法律法规禁止销售的食品。</p>
        <p><b>三、订单履约</b><br />商家应及时处理顾客订单，按承诺时间完成出餐；因商家原因导致的订单取消与退款，平台有权从结算款中扣减相应费用。</p>
        <p><b>四、配送与包装</b><br />商家应使用符合食品安全标准的包装材料，并按订单实际需求收取打包费。</p>
        <p><b>五、价格与结算</b><br />商家自主设置菜品价格、起送价与配送费，平台按约定周期结算货款，商家应依法纳税。</p>
        <p><b>六、评价与售后</b><br />商家应妥善处理顾客评价与售后申请，不得以任何形式干扰顾客的正常评价。</p>
        <p><b>七、协议变更与终止</b><br />平台有权根据经营情况调整本协议并在平台公示；商家可随时申请退出平台，但应完成已产生订单的履约。</p>
      </div>
      <template #footer>
        <el-button @click="agreementVisible = false">关闭</el-button>
        <el-button type="primary" @click="agreeAndClose">同意并关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { merchantApi, fileApi } from '@/api'

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const uploading = ref(false)
const profile = ref(null)
const formRef = ref()
const agreed = ref(false)
const agreementVisible = ref(false)

function agreeAndClose() {
  agreed.value = true
  agreementVisible.value = false
}

const form = reactive({
  realName: '',
  idCard: '',
  contactPhone: '',
  licenseImg: ''
})

const rules = {
  realName: [{ required: true, message: '请填写经营者姓名', trigger: 'blur' }],
  idCard: [
    { required: true, message: '请填写身份证号', trigger: 'blur' },
    { pattern: /^\d{17}[\dXx]$/, message: '身份证号格式不正确', trigger: 'blur' }
  ],
  contactPhone: [
    { required: true, message: '请填写联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  licenseImg: [{ required: true, message: '请上传营业执照', trigger: 'change' }]
}

/** none 未申请 / pending 待审核 / passed 已通过 / rejected 已驳回 */
const status = computed(() => {
  const p = profile.value
  if (!p || !p.applied) return 'none'
  if (p.auditStatus === 1) return 'passed'
  if (p.auditStatus === 2) return 'rejected'
  return 'pending'
})

async function load() {
  loading.value = true
  try {
    profile.value = await merchantApi.profile()
    if (profile.value?.applied) {
      form.realName = profile.value.realName || ''
      form.idCard = profile.value.idCard || ''
      form.contactPhone = profile.value.contactPhone || ''
      form.licenseImg = profile.value.licenseImg || ''
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

async function uploadLicense(options) {
  uploading.value = true
  try {
    const data = await fileApi.upload(options.file)
    form.licenseImg = data.url
    ElMessage.success('营业执照已上传')
  } finally {
    uploading.value = false
  }
}

async function submit() {
  if (!agreed.value) {
    ElMessage.warning('请先阅读并勾选同意《EatWhat 平台商家入驻协议》')
    return
  }
  await formRef.value.validate()
  submitting.value = true
  try {
    await merchantApi.apply({ ...form })
    ElMessage.success('入驻申请已提交，请等待平台审核')
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

.agree-hint {
  margin-left: 12px;
  font-size: 12px;
}

.agreement {
  max-height: 460px;
  overflow-y: auto;
  line-height: 1.9;
  font-size: 14px;
  color: #606266;
}

.agreement p {
  margin: 0 0 10px;
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
