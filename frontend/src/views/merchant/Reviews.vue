<template>
  <div class="ew-page">
    <div class="ew-card">
      <div class="ew-flex-between">
        <h3 class="ew-title">本店评价</h3>
        <el-select v-model="score" placeholder="全部评分" clearable style="width: 140px" @change="search">
          <el-option v-for="n in 5" :key="n" :label="`${n} 星`" :value="n" />
        </el-select>
      </div>

      <div v-loading="loading" class="reviews">
        <div v-for="review in list" :key="review.id" class="review">
          <div class="review-head">
            <el-avatar :size="34">{{ (review.nickname || '匿').slice(0, 1) }}</el-avatar>
            <div class="review-user">
              <div class="name">{{ review.nickname || '匿名用户' }}</div>
              <div class="ew-muted">{{ review.createdAt }}</div>
            </div>
            <div class="scores">
              <el-rate :model-value="review.shopScore" disabled size="small" />
              <span class="ew-muted">配送 {{ review.deliveryScore }} 分</span>
            </div>
          </div>

          <div class="review-content">{{ review.content }}</div>

          <div v-if="review.imageList?.length" class="review-images">
            <el-image
              v-for="(img, i) in review.imageList"
              :key="i"
              :src="img"
              :preview-src-list="review.imageList"
              :initial-index="i"
              fit="cover"
              class="review-img"
            />
          </div>

          <div v-if="review.reply" class="reply">
            <span class="reply-tag">商家回复</span>
            {{ review.reply }}
          </div>
          <div v-else class="reply-action">
            <el-button size="small" link type="primary" @click="openReply(review)">回复评价</el-button>
          </div>
        </div>

        <el-empty v-if="!loading && list.length === 0" description="还没有顾客评价" />
      </div>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          :page-size="10"
          :total="total"
          layout="prev, pager, next"
          background
          @current-change="load"
        />
      </div>
    </div>

    <el-dialog v-model="replyVisible" title="回复评价" width="480px">
      <div class="quote">{{ currentReview?.content }}</div>
      <el-input v-model="replyText" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="感谢您的评价～" />
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="replying" @click="submitReply">提交回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { merchantApi } from '@/api'

const loading = ref(false)
const replying = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const score = ref(null)
const replyVisible = ref(false)
const currentReview = ref(null)
const replyText = ref('')

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, size: 10 }
    if (score.value) params.score = score.value
    const data = await merchantApi.reviews(params)
    list.value = data?.records || []
    total.value = data?.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

function openReply(review) {
  currentReview.value = review
  replyText.value = ''
  replyVisible.value = true
}

async function submitReply() {
  if (!replyText.value.trim()) {
    ElMessage.warning('请填写回复内容')
    return
  }
  replying.value = true
  try {
    await merchantApi.replyReview(currentReview.value.id, replyText.value.trim())
    ElMessage.success('回复成功')
    replyVisible.value = false
    await load()
  } finally {
    replying.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.reviews {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 100px;
  margin-top: 12px;
}

.review {
  border: 1px solid var(--ew-border);
  border-radius: 10px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.review-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.review-user {
  flex: 1;
}

.name {
  font-weight: 600;
  font-size: 14px;
}

.scores {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  font-size: 12px;
}

.review-content {
  font-size: 14px;
  color: #303133;
  line-height: 1.6;
}

.review-images {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.review-img {
  width: 76px;
  height: 76px;
  border-radius: 8px;
}

.reply {
  background: #f7f8fa;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 13px;
  color: #606266;
}

.reply-tag {
  color: var(--ew-primary-dark);
  font-weight: 600;
  margin-right: 6px;
}

.quote {
  background: #f7f8fa;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 13px;
  color: #909399;
  margin-bottom: 12px;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
