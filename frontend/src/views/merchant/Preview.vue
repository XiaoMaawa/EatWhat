<template>
  <div class="ew-page">
    <div class="ew-card">
      <div class="ew-flex-between">
        <h3 class="ew-title">顾客视角预览</h3>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
      <p class="ew-muted tip">
        以下为顾客在手机端看到的店铺页效果（只读预览，加购与下单不可用）。用于核对店铺信息、公告与菜品展示是否合适。
      </p>

      <el-empty v-if="!loading && !shop" description="店铺信息加载失败，请先在「店铺管理」完善资料" />

      <div v-if="shop" class="phone">
        <div class="phone-notch" />
        <div class="phone-screen">
          <div class="shop-header">
            <div class="logo">
              <img v-if="shop.logo" :src="shop.logo" alt="" />
              <span v-else>🍜</span>
            </div>
            <div class="info">
              <h4>
                {{ shop.name }}
                <el-tag v-if="!shop.isOpen" type="info" size="small">休息中</el-tag>
              </h4>
              <p class="ew-muted small">{{ shop.description || '暂无简介' }}</p>
              <div class="meta">
                <el-rate :model-value="Number(shop.avgScore) || 0" disabled size="small" allow-half />
                <span class="ew-muted small">{{ shop.avgScore }} 分 · 月售 {{ shop.monthSales }}</span>
              </div>
              <div class="ew-muted small">
                起送 ¥{{ shop.minOrderAmount }} / 配送 ¥{{ shop.deliveryFee }}
                <template v-if="Number(shop.packagingFee) > 0"> / 打包 ¥{{ shop.packagingFee }}</template>
              </div>
            </div>
          </div>

          <p v-if="shop.notice" class="notice">📢 {{ shop.notice }}</p>
          <p v-if="!shop.isOpen" class="closed-tip">
            🌙 {{ shop.closedTip || '商家休息中，暂时无法下单，可先浏览菜品' }}
          </p>

          <div v-for="cat in shop.categories" :key="cat.id" class="cat-block">
            <p class="cat-name">{{ cat.name }}</p>
            <div v-for="dish in cat.dishes" :key="dish.id" class="dish-row">
              <div class="dish-img">
                <img v-if="dish.image" :src="dish.image" alt="" />
                <span v-else>🍱</span>
              </div>
              <div class="dish-info">
                <p class="dish-name">{{ dish.name }}</p>
                <p class="ew-muted tiny">{{ dish.description || '—' }}</p>
                <p class="ew-muted tiny">
                  月售 {{ dish.monthSales }}
                  <template v-if="dish.goodRate != null"> · 好评 {{ dish.goodRate }}%</template>
                </p>
                <div class="dish-bottom">
                  <span class="ew-price small">
                    <template v-if="dish.specs && dish.specs.length">¥{{ specMin(dish) }} 起</template>
                    <template v-else>¥{{ dish.price }}</template>
                  </span>
                  <el-tag v-if="dish.status === 0" size="small" type="info">已下架</el-tag>
                  <el-tag v-else-if="dish.status === 2 || dish.stock <= 0" size="small" type="warning">售罄</el-tag>
                  <el-button v-else size="small" type="primary" disabled>加入</el-button>
                </div>
              </div>
            </div>
          </div>

          <el-empty v-if="!shop.categories || !shop.categories.length" description="暂无上架菜品" :image-size="60" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { merchantApi } from '@/api'

const loading = ref(false)
const shop = ref(null)

async function load() {
  loading.value = true
  try {
    const mine = await merchantApi.shop().catch(() => null)
    if (!mine) {
      shop.value = null
      return
    }
    shop.value = await merchantApi.previewShop(mine.id)
  } finally {
    loading.value = false
  }
}

function specMin(dish) {
  const prices = (dish.specs || []).map((s) => Number(s.price)).filter((n) => !Number.isNaN(n))
  return prices.length ? Math.min(...prices).toFixed(2) : dish.price
}

onMounted(load)
</script>

<style scoped>
.tip {
  margin-bottom: 16px;
}

.phone {
  width: 400px;
  margin: 0 auto;
  border: 10px solid #303133;
  border-radius: 32px;
  background: #f5f6f8;
  position: relative;
  padding-top: 16px;
}

.phone-notch {
  position: absolute;
  top: 4px;
  left: 50%;
  transform: translateX(-50%);
  width: 110px;
  height: 14px;
  background: #303133;
  border-radius: 0 0 12px 12px;
}

.phone-screen {
  max-height: 640px;
  overflow-y: auto;
  padding: 12px;
}

.shop-header {
  display: flex;
  gap: 12px;
  background: #fff;
  border-radius: 12px;
  padding: 12px;
}

.logo {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  background: #f0f2f5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  overflow: hidden;
  flex-shrink: 0;
}

.logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.info h4 {
  margin: 0 0 4px;
  font-size: 15px;
}

.small {
  font-size: 12px;
}

.tiny {
  font-size: 11px;
}

.meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 4px 0;
}

.notice,
.closed-tip {
  font-size: 12px;
  padding: 6px 10px;
  border-radius: 8px;
  margin-top: 8px;
}

.notice {
  color: #b88230;
  background: #fdf6ec;
}

.closed-tip {
  color: #909399;
  background: #eef0f3;
}

.cat-block {
  background: #fff;
  border-radius: 12px;
  padding: 10px 12px;
  margin-top: 10px;
}

.cat-name {
  font-weight: 600;
  font-size: 13px;
  margin-bottom: 8px;
}

.dish-row {
  display: flex;
  gap: 10px;
  padding: 8px 0;
  border-top: 1px dashed #ebeef5;
}

.dish-row:first-of-type {
  border-top: none;
}

.dish-img {
  width: 54px;
  height: 54px;
  border-radius: 8px;
  background: #f0f2f5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  overflow: hidden;
  flex-shrink: 0;
}

.dish-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.dish-info {
  flex: 1;
  min-width: 0;
}

.dish-name {
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 2px;
}

.dish-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
}
</style>
