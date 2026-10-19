<template>
  <div class="ew-page">
    <div class="page-head">
      <h2>我的收藏</h2>
      <span class="ew-muted">共 {{ total }} 家店，随手点开就能下单</span>
    </div>

    <div v-loading="loading" class="shop-list">
      <div v-if="!loading && shops.length === 0" class="empty">
        <div class="empty-icon">💙</div>
        <p>还没有收藏任何店铺呢</p>
        <el-button type="primary" @click="$router.push('/')">去逛逛</el-button>
      </div>

      <div v-for="shop in shops" :key="shop.id" class="shop-card">
        <div class="logo" @click="goShop(shop.id)">
          <img v-if="shop.logo" :src="shop.logo" alt="" />
          <span v-else>🍜</span>
        </div>
        <div class="info" @click="goShop(shop.id)">
          <div class="name">
            {{ shop.name }}
            <el-tag v-if="!shop.isOpen" size="small" type="info">休息中</el-tag>
          </div>
          <div class="desc text-ellipsis">{{ shop.description || '暂无简介' }}</div>
          <div class="meta">
            <el-rate :model-value="Number(shop.avgScore) || 0" disabled size="small" allow-half />
            <span class="ew-muted">{{ shop.avgScore }} 分</span>
            <el-divider direction="vertical" />
            <span class="ew-muted">月售 {{ shop.monthSales }}</span>
            <el-divider direction="vertical" />
            <span class="ew-muted">起送 ¥{{ money(shop.minOrderAmount) }}</span>
          </div>
        </div>
        <div class="ops">
          <el-button size="small" @click="goShop(shop.id)">去点餐</el-button>
          <el-button size="small" type="danger" plain @click="remove(shop.id)">取消收藏</el-button>
        </div>
      </div>
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
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { favoriteApi } from '@/api'
import { money } from '@/utils/format'

const router = useRouter()
const shops = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await favoriteApi.list({ page: page.value, size })
    shops.value = data.records || []
    total.value = Number(data.total) || 0
  } finally {
    loading.value = false
  }
}

function goShop(id) {
  router.push(`/shop/${id}`)
}

async function remove(shopId) {
  await ElMessageBox.confirm('确定取消收藏这家店吗？', '提示', { type: 'warning' })
  await favoriteApi.remove(shopId)
  ElMessage.success('已取消收藏')
  await load()
}

onMounted(load)
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 16px;
}

.page-head h2 {
  margin: 0;
  font-size: 20px;
}

.shop-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 140px;
}

.shop-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid var(--ew-border);
  transition: box-shadow 0.2s;
}

.shop-card:hover {
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.06);
}

.logo {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  background: var(--ew-primary-light);
  cursor: pointer;
  flex-shrink: 0;
}

.logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.info {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}

.name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 4px;
}

.desc {
  color: #909399;
  font-size: 13px;
  margin-bottom: 6px;
}

.meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.ops {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.empty {
  text-align: center;
  padding: 60px 0;
  color: #909399;
}

.empty-icon {
  font-size: 44px;
  margin-bottom: 8px;
}

.empty p {
  margin: 0 0 16px;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
