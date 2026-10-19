<template>
  <div class="ew-page" v-loading="loading">
    <div class="ew-card">
      <h3 class="ew-title">账号信息</h3>

      <el-descriptions :column="2" border class="desc">
        <el-descriptions-item label="手机号">{{ profile.phone || '—' }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ roleText }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ profile.nickname || '—' }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ profile.createdAt || '—' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 商家-58：查看并修改联系人姓名与联系电话 -->
      <template v-if="isMerchant">
        <el-divider />
        <h4 class="sub-title">商家联系人</h4>
        <el-form ref="contactRef" :model="contactForm" :rules="contactRules" label-width="110px" class="form">
          <el-form-item label="联系人姓名" prop="realName">
            <el-input v-model="contactForm.realName" maxlength="50" placeholder="与身份证一致" />
          </el-form-item>
          <el-form-item label="联系电话" prop="contactPhone">
            <el-input v-model="contactForm.contactPhone" maxlength="11" placeholder="11 位手机号" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="savingContact" @click="saveContact">保存联系人</el-button>
          </el-form-item>
        </el-form>
      </template>
    </div>

    <div class="ew-card">
      <h4 class="sub-title">修改密码</h4>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="修改成功后当前登录状态会立即失效，需要使用新密码重新登录。"
        class="pwd-tip"
      />
      <el-form ref="pwdRef" :model="pwdForm" :rules="pwdRules" label-width="110px" class="form">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6-20 位，需同时包含字母和数字" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="changing" @click="changePassword">修改密码</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { merchantApi, userApi } from '@/api'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const ROLE_TEXT = { CONSUMER: '消费者', MERCHANT: '商家', RIDER: '骑手', ADMIN: '管理员' }

const loading = ref(false)
const profile = ref({})
const roleText = computed(() => ROLE_TEXT[profile.value.role] || profile.value.role || '—')
const isMerchant = computed(() => profile.value.role === 'MERCHANT')

// 商家-58 联系人信息
const contactRef = ref()
const savingContact = ref(false)
const contactForm = reactive({ realName: '', contactPhone: '' })
const contactRules = {
  realName: [{ required: true, message: '请填写联系人姓名', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请填写联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ]
}

// 修改密码（商家-59 / 骑手-29 / 管理员-02）
const pwdRef = ref()
const changing = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    {
      pattern: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*#?&]{6,20}$/,
      message: '6-20 位，需同时包含字母和数字',
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

async function load() {
  loading.value = true
  try {
    profile.value = (await userApi.profile()) || {}
    if (isMerchant.value) {
      const data = await merchantApi.profile().catch(() => null)
      contactForm.realName = data?.realName || ''
      contactForm.contactPhone = data?.contactPhone || ''
    }
  } finally {
    loading.value = false
  }
}

async function saveContact() {
  await contactRef.value.validate()
  savingContact.value = true
  try {
    await merchantApi.updateAccount({ ...contactForm })
    ElMessage.success('联系人信息已保存')
    await load()
  } finally {
    savingContact.value = false
  }
}

async function changePassword() {
  await pwdRef.value.validate()
  changing.value = true
  try {
    await userApi.changePassword({ ...pwdForm })
    ElMessage.success('密码已修改，请重新登录')
    pwdRef.value.resetFields()
    await userStore.logout().catch(() => {})
    router.replace('/login')
  } finally {
    changing.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.desc {
  margin-top: 8px;
}

.sub-title {
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 600;
}

.pwd-tip {
  margin-bottom: 14px;
}

.form {
  max-width: 520px;
}
</style>
