<template>
  <div class="ew-page" v-loading="loading">
    <div class="ew-card">
      <h3 class="ew-title">个人中心</h3>

      <div class="profile">
        <el-avatar :size="72" :src="form.avatar">{{ (form.nickname || 'U').slice(0, 1) }}</el-avatar>
        <div class="profile-info">
          <div class="nickname">{{ form.nickname }}</div>
          <div class="ew-muted">{{ profile.phone }}</div>
          <el-tag size="small" type="success">{{ roleText }}</el-tag>
        </div>
        <el-upload
          :show-file-list="false"
          :before-upload="beforeUpload"
          :http-request="uploadAvatar"
          accept="image/*"
        >
          <el-button size="small" :loading="uploading">更换头像</el-button>
        </el-upload>
      </div>

      <el-divider />

      <el-form :model="form" label-width="90px" class="form">
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input :model-value="profile.phone" disabled />
        </el-form-item>
        <el-form-item label="注册时间">
          <el-input :model-value="profile.createdAt" disabled />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="saveProfile">保存资料</el-button>
        </el-form-item>
      </el-form>

      <el-divider />

      <div class="ew-flex-between">
        <h4 class="sub-title">修改密码</h4>
        <el-button link type="primary" @click="$router.push('/address')">管理收货地址</el-button>
      </div>

      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="90px" class="form">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6-20 位，含字母和数字" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
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
import { ElMessage } from 'element-plus'
import { fileApi, userApi } from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const saving = ref(false)
const changing = ref(false)
const uploading = ref(false)
const profile = ref({})
const form = reactive({ nickname: '', avatar: '' })
const pwdFormRef = ref()
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const ROLE_TEXT = {
  CONSUMER: '消费者',
  MERCHANT: '商家',
  RIDER: '骑手',
  ADMIN: '管理员'
}

const roleText = computed(() => ROLE_TEXT[profile.value.role] || profile.value.role)

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*#?&]{6,20}$/, message: '6-20 位，需同时包含字母和数字', trigger: 'blur' }
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
    profile.value = await userApi.profile()
    form.nickname = profile.value.nickname
    form.avatar = profile.value.avatar
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  if (!form.nickname?.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }
  saving.value = true
  try {
    await userApi.updateProfile({ nickname: form.nickname, avatar: form.avatar })
    userStore.setUserInfo({ nickname: form.nickname, avatar: form.avatar })
    ElMessage.success('资料已更新')
    await load()
  } finally {
    saving.value = false
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

async function uploadAvatar(options) {
  uploading.value = true
  try {
    const data = await fileApi.upload(options.file)
    form.avatar = data.url
    await userApi.updateProfile({ nickname: form.nickname, avatar: form.avatar })
    userStore.setUserInfo({ avatar: form.avatar })
    ElMessage.success('头像已更新')
    await load()
  } finally {
    uploading.value = false
  }
}

async function changePassword() {
  await pwdFormRef.value.validate()
  changing.value = true
  try {
    await userApi.changePassword({ ...pwdForm })
    ElMessage.success('密码已修改，请重新登录')
    pwdFormRef.value.resetFields()
    userStore.logout()
    location.href = '/login'
  } finally {
    changing.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.profile {
  display: flex;
  align-items: center;
  gap: 18px;
}

.profile-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: flex-start;
}

.nickname {
  font-size: 18px;
  font-weight: 600;
}

.form {
  max-width: 460px;
}

.sub-title {
  margin: 0;
  font-size: 15px;
}
</style>
