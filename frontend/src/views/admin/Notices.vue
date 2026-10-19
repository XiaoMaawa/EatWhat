<template>
  <div class="notices-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-head">
          <span class="card-title">发布系统公告</span>
          <span class="card-tip">公告会推送到所有正常状态的消费者账号的消息中心</span>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" class="notice-form">
        <el-form-item label="公告标题" prop="title">
          <el-input
            v-model="form.title"
            maxlength="100"
            show-word-limit
            placeholder="例如：平台服务升级通知"
          />
        </el-form-item>
        <el-form-item label="公告内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="6"
            maxlength="500"
            show-word-limit
            placeholder="请输入公告正文，最多 500 字"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submit">发布公告</el-button>
          <el-button @click="reset">清空</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="preview-card">
      <template #header><span class="card-title">预览效果</span></template>
      <div class="msg-card">
        <div class="msg-icon">📢</div>
        <div class="msg-body">
          <div class="msg-head">
            <span class="msg-title">{{ form.title || '公告标题' }}</span>
            <el-tag size="small" effect="plain" type="warning">系统公告</el-tag>
          </div>
          <div class="msg-content">{{ form.content || '公告内容会显示在这里，顾客可在「消息中心 → 系统公告」中查看。' }}</div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api'

const formRef = ref(null)
const submitting = ref(false)

const form = reactive({
  title: '',
  content: ''
})

const rules = {
  title: [
    { required: true, message: '请输入公告标题', trigger: 'blur' },
    { max: 100, message: '标题不能超过 100 字', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入公告内容', trigger: 'blur' },
    { max: 500, message: '内容不能超过 500 字', trigger: 'blur' }
  ]
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    await adminApi.publishNotice({ title: form.title, content: form.content })
    ElMessage.success('公告已发布，所有消费者账号都将收到消息')
    reset()
  } finally {
    submitting.value = false
  }
}

function reset() {
  form.title = ''
  form.content = ''
  formRef.value?.clearValidate()
}
</script>

<style scoped>
.notices-page {
  max-width: 780px;
}

.card-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.card-tip {
  font-size: 12px;
  color: #909399;
}

.notice-form {
  max-width: 640px;
}

.preview-card {
  margin-top: 16px;
}

.msg-card {
  display: flex;
  gap: 12px;
  padding: 14px 16px;
  background: #f7fbff;
  border-radius: 10px;
  border: 1px solid #ebeef5;
  border-left: 3px solid #e6a23c;
}

.msg-icon {
  font-size: 22px;
}

.msg-body {
  flex: 1;
}

.msg-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.msg-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.msg-content {
  margin-top: 6px;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  white-space: pre-wrap;
}
</style>
