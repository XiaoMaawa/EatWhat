<template>
  <div class="ew-page">
    <div class="banner">
      <div class="banner-text">
        <h2>今天吃啥？</h2>
        <p>附近好店已为你整理好，下单即可送达</p>
      </div>
      <span class="banner-icon">🍚</span>
    </div>

    <div v-if="announcements.length" class="notice-bar">
      <span class="notice-icon">📢</span>
      <el-carousel
        :autoplay="true"
        :interval="5000"
        direction="vertical"
        height="22px"
        indicator-position="none"
        class="notice-carousel"
      >
        <el-carousel-item v-for="item in announcements" :key="item.id">
          <span class="notice-text" :title="item.title">{{ item.title }}</span>
        </el-carousel-item>
      </el-carousel>
      <el-tag size="small" effect="plain" type="warning">平台公告</el-tag>
    </div>

    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="搜店铺名称或菜品名称"
        clearable
        class="search"
        @keyup.enter="load"
        @clear="load"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
        <template #append>
          <el-button @click="load">搜索</el-button>
        </template>
      </el-input>

      <el-radio-group v-model="query.sortBy" @change="load">
        <el-radio-button value="score">评分优先</el-radio-button>
        <el-radio-button value="sales">销量优先</el-radio-button>
        <el-radio-button value="distance">距离最近</el-radio-button>
      </el-radio-group>
    </div>

    <div v-loading="loading" class="shop-list">
      <div
        v-for="shop in shops"
        :key="shop.id"
        class="shop-card"
        @click="goShop(shop.id)"
      >
        <div class="shop-logo">
          <img v-if="shop.logo" :src="shop.logo" alt="" />
          <span v-else>🍜</span>
        </div>
        <div class="shop-info">
          <div class="shop-name">
            <span v-html="highlight(shop.name)"></span>
            <el-tag v-if="!shop.isOpen" size="small" type="info">休息中</el-tag>
          </div>
          <div class="shop-desc text-ellipsis">{{ shop.description || '暂无简介' }}</div>
          <div v-if="shop.matchedDishNames && shop.matchedDishNames.length" class="shop-dishes">
            菜品匹配：
            <span v-for="(n, i) in shop.matchedDishNames" :key="n" class="dish-hit">
              <span v-html="highlight(n)"></span>
              <span v-if="i < shop.matchedDishNames.length - 1">、</span>
            </span>
          </div>
          <div class="shop-meta">
            <el-rate :model-value="Number(shop.avgScore) || 0" disabled size="small" allow-half />
            <span class="ew-muted">{{ shop.avgScore }} 分</span>
            <el-divider direction="vertical" />
            <span class="ew-muted">月售 {{ shop.monthSales }}</span>
            <template v-if="shop.distance != null">
              <el-divider direction="vertical" />
              <span class="ew-muted">{{ shop.distance }} km</span>
            </template>
          </div>
          <div class="shop-meta">
            <span class="ew-muted">起送 ¥{{ shop.minOrderAmount }}</span>
            <el-divider direction="vertical" />
            <span class="ew-muted">配送 ¥{{ shop.deliveryFee }}</span>
            <el-divider direction="vertical" />
            <span class="ew-muted">营业 {{ shop.openTime }}-{{ shop.closeTime }}</span>
          </div>
        </div>
        <div class="shop-action">
          <el-button type="primary" plain>去点餐</el-button>
        </div>
      </div>

      <el-empty v-if="!loading && shops.length === 0" description="没有找到店铺" />
    </div>

    <div class="pager">
      <el-pagination
        v-model:current-page="query.page"
        :page-size="query.size"
        :total="total"
        layout="prev, pager, next, jumper"
        background
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { shopApi, commonApi, userApi } from '@/api'
import { Search } from '@element-plus/icons-vue'

const router = useRouter()
const loading = ref(false)
const shops = ref([])
const total = ref(0)
const announcements = ref([])
const defaultAddress = ref(null)

const query = reactive({
  page: 1,
  size: 8,
  keyword: '',
  sortBy: 'score'
})

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    // 顾客-19：带上默认收货地址坐标，列表即可展示距离并按距离排序
    if (defaultAddress.value) {
      params.longitude = defaultAddress.value.longitude
      params.latitude = defaultAddress.value.latitude
    }
    const data = await shopApi.list(params)
    shops.value = data?.records || []
    total.value = data?.total || 0
  } finally {
    loading.value = false
  }
}

// 顾客-19：取默认收货地址用于距离计算
async function loadDefaultAddress() {
  try {
    const list = await userApi.addresses()
    const arr = Array.isArray(list) ? list : list?.records || []
    defaultAddress.value = arr.find((a) => a.isDefault === 1) || arr[0] || null
  } catch {
    defaultAddress.value = null
  }
}

function goShop(id) {
  router.push(`/shop/${id}`)
}

// 顾客-18：搜索关键词高亮
function escapeHtml(text) {
  return String(text == null ? '' : text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function highlight(text) {
  const safe = escapeHtml(text)
  const kw = (query.keyword || '').trim()
  if (!kw) return safe
  const escaped = kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return safe.replace(new RegExp(escaped, 'gi'), (m) => `<span class="kw-hit">${m}</span>`)
}

async function loadAnnouncements() {
  try {
    const data = await commonApi.announcements({ role: 'CONSUMER', limit: 5 })
    announcements.value = data || []
  } catch {
    announcements.value = []
  }
}

onMounted(async () => {
  await loadDefaultAddress()
  load()
  loadAnnouncements()
})
</script>

<style scoped>
.notice-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #fff8e6;
  border: 1px solid #fbe6b5;
  border-radius: 10px;
  padding: 8px 14px;
  margin-bottom: 14px;
}

.notice-icon {
  font-size: 16px;
}

.notice-carousel {
  flex: 1;
  min-width: 0;
}

.notice-text {
  font-size: 13px;
  color: #8a5a00;
  line-height: 22px;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.banner {
  background: linear-gradient(135deg, #ff9a5c, #ff7a45);
  border-radius: 14px;
  padding: 26px 30px;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.banner h2 {
  margin: 0 0 6px;
  font-size: 24px;
}

.banner p {
  margin: 0;
  opacity: 0.9;
  font-size: 14px;
}

.banner-icon {
  font-size: 56px;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
}

.search {
  max-width: 420px;
}

.shop-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 200px;
}

.shop-card {
  background: #fff;
  border-radius: var(--ew-radius);
  box-shadow: var(--ew-shadow);
  padding: 16px;
  display: flex;
  gap: 16px;
  align-items: center;
  cursor: pointer;
  transition: transform 0.18s, box-shadow 0.18s;
}

.shop-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
}

.shop-logo {
  width: 88px;
  height: 88px;
  border-radius: 10px;
  background: #fff4ee;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34px;
  flex-shrink: 0;
  overflow: hidden;
}

.shop-logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.shop-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.shop-name {
  font-size: 17px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.shop-desc {
  color: #909399;
  font-size: 13px;
}

.shop-dishes {
  font-size: 12px;
  color: #909399;
}

.dish-hit {
  color: #e6a23c;
}

.shop-name :deep(.kw-hit),
.shop-dishes :deep(.kw-hit) {
  color: var(--el-color-primary);
  font-weight: 700;
  background: var(--el-color-primary-light-9);
  border-radius: 3px;
  padding: 0 2px;
}

.shop-meta {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 13px;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
