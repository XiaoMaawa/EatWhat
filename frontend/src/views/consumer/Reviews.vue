<template>
  <div class="reviews-page">
    <div class="page-head">
      <h3 class="ew-title">我的评价</h3>
      <span class="ew-sub">共 {{ total }} 条评价</span>
    </div>

    <div v-loading="loading" class="review-list">
      <div v-for="item in list" :key="item.id" class="ew-card review-item">
        <div class="review-head">
          <div class="shop-line">
            <span class="shop-name">{{ item.shopName || '店铺' }}</span>
            <span class="ew-sub">{{ item.createdAt }}</span>
          </div>
          <el-tag v-if="item.reply" type="success" size="small" effect="plain">商家已回复</el-tag>
          <el-tag v-else type="info" size="small" effect="plain">待回复</el-tag>
        </div>

        <div class="score-line">
          <span class="score-label">餐品</span>
          <el-rate :model-value="item.shopScore" disabled size="small" />
          <span class="score-label">配送</span>
          <el-rate :model-value="item.deliveryScore" disabled size="small" />
        </div>

        <p class="review-content">{{ item.content }}</p>

        <div v-if="item.imageList && item.imageList.length" class="image-line">
          <el-image
            v-for="(img, idx) in item.imageList"
            :key="idx"
            :src="img"
            :preview-src-list="item.imageList"
            :initial-index="idx"
            fit="cover"
            class="review-img"
          />
        </div>

        <div v-if="item.reply" class="reply-box">
          <span class="reply-label">商家回复：</span>{{ item.reply }}
        </div>
      </div>

      <el-empty v-if="!loading && list.length === 0" description="还没有发表过评价，去订单里评价一下吧" />
    </div>

    <div v-if="total > size" class="pager">
      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        background
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { reviewApi } from '@/api'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = 10

async function load() {
  loading.value = true
  try {
    const data = await reviewApi.mine({ page: page.value, size })
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.reviews-page {
  padding: 4px 0 40px;
}

.page-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 16px;
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 120px;
}

.review-item {
  padding: 18px 20px;
}

.review-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.shop-line {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.shop-name {
  font-weight: 600;
  font-size: 15px;
}

.score-line {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.score-label {
  font-size: 12px;
  color: var(--ew-text-sub);
}

.review-content {
  margin: 0 0 10px;
  line-height: 1.7;
  color: var(--ew-text);
  white-space: pre-wrap;
}

.image-line {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.review-img {
  width: 84px;
  height: 84px;
  border-radius: 8px;
}

.reply-box {
  background: var(--ew-bg-soft, #f7f8fa);
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 13px;
  color: var(--ew-text-sub);
  line-height: 1.6;
}

.reply-label {
  color: var(--ew-primary);
  font-weight: 600;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
