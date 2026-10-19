<template>
  <div class="ew-page" v-loading="loading">
    <div class="shop-header" v-if="shop">
      <div class="logo">
        <img v-if="shop.logo" :src="shop.logo" alt="" />
        <span v-else>🍜</span>
      </div>
      <div class="info">
        <h2>
          {{ shop.name }}
          <el-tag v-if="!shop.isOpen" type="info" size="small">休息中</el-tag>
        </h2>
        <p class="ew-muted">{{ shop.description }}</p>
        <div class="meta">
          <el-rate :model-value="Number(shop.avgScore) || 0" disabled size="small" allow-half />
          <span class="ew-muted">{{ shop.avgScore }} 分 · 月售 {{ shop.monthSales }}</span>
          <el-divider direction="vertical" />
          <span class="ew-muted">起送 ¥{{ shop.minOrderAmount }} / 配送 ¥{{ shop.deliveryFee }}</span>
        </div>
        <p v-if="shop.notice" class="notice">📢 {{ shop.notice }}</p>
      </div>
      <div class="fav-box">
        <el-button
          :type="favorite ? 'danger' : 'default'"
          :plain="!favorite"
          :loading="favLoading"
          @click="toggleFavorite"
        >
          <span class="fav-icon">{{ favorite ? '💙' : '🤍' }}</span>
          {{ favorite ? '已收藏' : '收藏' }}
        </el-button>
      </div>
    </div>

    <div class="body">
      <div class="categories">
        <div
          v-for="cat in categories"
          :key="cat.id"
          class="cat"
          :class="{ active: activeCat === cat.id }"
          @click="activeCat = cat.id"
        >
          {{ cat.name }}
        </div>
        <div class="cat" :class="{ active: activeCat === 'review' }" @click="activeCat = 'review'">
          评价
        </div>
      </div>

      <div class="content">
        <template v-if="activeCat === 'review'">
          <div class="ew-card">
            <div class="ew-flex-between">
              <h3 class="ew-title">店铺评价</h3>
              <el-select v-model="reviewScore" placeholder="全部评分" clearable style="width: 130px" @change="loadReviews">
                <el-option v-for="s in [5, 4, 3, 2, 1]" :key="s" :label="`${s} 星`" :value="s" />
              </el-select>
            </div>
            <div v-for="rv in reviews" :key="rv.id" class="review-item">
              <div class="ew-flex-between">
                <span class="review-user">{{ rv.userNickname || (rv.isAnonymous ? '匿名用户' : '用户') }}</span>
                <span class="ew-muted">{{ rv.createdAt }}</span>
              </div>
              <el-rate :model-value="rv.shopScore" disabled size="small" />
              <p class="review-content">{{ rv.content }}</p>
              <div v-if="rv.reply" class="reply">商家回复：{{ rv.reply }}</div>
            </div>
            <el-empty v-if="reviews.length === 0" description="还没有评价" />
            <div class="pager">
              <el-pagination
                v-model:current-page="reviewPage"
                :page-size="10"
                :total="reviewTotal"
                layout="prev, pager, next"
                small
                @current-change="loadReviews"
              />
            </div>
          </div>
        </template>

        <template v-else>
          <div v-for="cat in visibleCategories" :key="cat.id" class="ew-card cat-block">
            <h3 class="ew-title">{{ cat.name }}</h3>
            <div v-for="dish in cat.dishes" :key="dish.id" class="dish-item">
              <div class="dish-img">
                <img v-if="dish.image" :src="dish.image" alt="" />
                <span v-else>🍱</span>
              </div>
              <div class="dish-info">
                <div class="dish-name">{{ dish.name }}</div>
                <div class="ew-muted text-ellipsis">{{ dish.description || '—' }}</div>
                <div class="ew-muted">月售 {{ dish.monthSales }} · 库存 {{ dish.stock }}</div>
                <div class="ew-flex-between ew-mt12">
                  <span class="ew-price">{{ dish.price }}</span>
                  <div class="ew-flex ew-gap8">
                    <el-tag v-if="dish.status === 0" size="small" type="info">已下架</el-tag>
                    <el-tag v-else-if="dish.status === 2 || dish.stock <= 0" size="small" type="warning">售罄</el-tag>
                    <el-button
                      v-else
                      type="primary"
                      size="small"
                      :disabled="!shop?.isOpen"
                      @click="addToCart(dish)"
                    >
                      加入购物车
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
            <el-empty v-if="cat.dishes.length === 0" description="该分类暂无菜品" :image-size="60" />
          </div>
        </template>
      </div>
    </div>

    <div class="cart-bar" v-if="cartCount > 0">
      <span class="ew-flex ew-gap12">
        <el-icon size="22"><ShoppingCart /></el-icon>
        <span>购物车 {{ cartCount }} 件</span>
        <span class="ew-price">{{ cartTotal }}</span>
      </span>
      <el-button type="primary" size="large" @click="goCart">去结算</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { cartApi, favoriteApi, shopApi } from '@/api'
import { ShoppingCart } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const shopId = Number(route.params.id)

const loading = ref(false)
const shop = ref(null)
const categories = ref([])
const activeCat = ref(null)

const favorite = ref(false)
const favLoading = ref(false)

const reviews = ref([])
const reviewTotal = ref(0)
const reviewPage = ref(1)
const reviewScore = ref(null)

const cartCount = ref(0)
const cartTotal = ref('0.00')

const visibleCategories = computed(() => {
  if (activeCat.value === null || activeCat.value === 'review') return categories.value
  return categories.value.filter((c) => c.id === activeCat.value)
})

async function load() {
  loading.value = true
  try {
    const data = await shopApi.detail(shopId)
    shop.value = data
    categories.value = data.categories || []
    activeCat.value = categories.value[0]?.id ?? null
    await refreshCart()
    await loadFavorite()
  } finally {
    loading.value = false
  }
}

async function loadFavorite() {
  try {
    const data = await favoriteApi.status(shopId)
    favorite.value = Boolean(data?.favorite)
  } catch {
    favorite.value = false
  }
}

async function toggleFavorite() {
  favLoading.value = true
  try {
    if (favorite.value) {
      await favoriteApi.remove(shopId)
      favorite.value = false
      ElMessage.success('已取消收藏')
    } else {
      await favoriteApi.add(shopId)
      favorite.value = true
      ElMessage.success('收藏成功，下次点餐更方便啦')
    }
  } finally {
    favLoading.value = false
  }
}

async function refreshCart() {
  try {
    const cart = await cartApi.get()
    const items = cart?.items || []
    cartCount.value = items.reduce((sum, item) => sum + item.quantity, 0)
    cartTotal.value = cart?.totalAmount || '0.00'
  } catch {
    cartCount.value = 0
  }
}

async function addToCart(dish) {
  await cartApi.add({ dishId: dish.id, quantity: 1 })
  ElMessage.success(`已加入：${dish.name}`)
  await refreshCart()
}

async function loadReviews() {
  const data = await shopApi.reviews(shopId, { page: reviewPage.value, size: 10, score: reviewScore.value || undefined })
  reviews.value = data?.records || []
  reviewTotal.value = data?.total || 0
}

function goCart() {
  router.push('/cart')
}

onMounted(async () => {
  await load()
  await loadReviews()
})
</script>

<style scoped>
.shop-header {
  background: #fff;
  border-radius: var(--ew-radius);
  box-shadow: var(--ew-shadow);
  padding: 20px;
  display: flex;
  gap: 18px;
  margin-bottom: 16px;
}

.fav-box {
  margin-left: auto;
  align-self: flex-start;
  flex-shrink: 0;
}

.fav-icon {
  margin-right: 4px;
}

.logo {
  width: 100px;
  height: 100px;
  border-radius: 12px;
  background: #fff4ee;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  flex-shrink: 0;
  overflow: hidden;
}

.logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.info h2 {
  margin: 0 0 6px;
  font-size: 22px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.info p {
  margin: 4px 0;
  font-size: 13px;
}

.meta {
  display: flex;
  align-items: center;
  gap: 4px;
}

.notice {
  color: #b88230;
  background: #fdf6ec;
  padding: 6px 10px;
  border-radius: 6px;
  margin-top: 8px !important;
}

.body {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.categories {
  width: 140px;
  background: #fff;
  border-radius: var(--ew-radius);
  box-shadow: var(--ew-shadow);
  overflow: hidden;
  flex-shrink: 0;
  position: sticky;
  top: 76px;
}

.cat {
  padding: 14px 12px;
  font-size: 14px;
  cursor: pointer;
  border-left: 3px solid transparent;
  transition: all 0.15s;
}

.cat:hover {
  background: #fff8f4;
}

.cat.active {
  background: #fff4ee;
  color: var(--ew-primary-dark);
  font-weight: 600;
  border-left-color: var(--ew-primary);
}

.content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cat-block {
  padding-bottom: 8px;
}

.dish-item {
  display: flex;
  gap: 12px;
  padding: 12px 0;
  border-top: 1px solid var(--ew-border);
}

.dish-item:first-of-type {
  border-top: none;
}

.dish-img {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  background: #fafafa;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  flex-shrink: 0;
  overflow: hidden;
}

.dish-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.dish-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.dish-name {
  font-weight: 600;
  font-size: 15px;
}

.review-item {
  padding: 12px 0;
  border-top: 1px solid var(--ew-border);
}

.review-item:first-of-type {
  border-top: none;
}

.review-user {
  font-weight: 600;
  font-size: 14px;
}

.review-content {
  margin: 6px 0 0;
  font-size: 14px;
  line-height: 1.6;
}

.reply {
  margin-top: 8px;
  background: #f7f8fa;
  border-radius: 6px;
  padding: 8px 10px;
  font-size: 13px;
  color: #606266;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 12px;
}

.cart-bar {
  position: fixed;
  left: 50%;
  transform: translateX(-50%);
  bottom: 24px;
  width: min(760px, calc(100% - 32px));
  background: #303133;
  color: #fff;
  border-radius: 14px;
  padding: 12px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.24);
  z-index: 50;
}

.cart-bar .ew-price {
  color: #ffd666;
  font-size: 17px;
}
</style>
