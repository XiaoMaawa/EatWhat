<template>
  <div class="ew-page" v-loading="loading">
    <!-- 店铺信息区 -->
    <div class="shop-header" v-if="shop">
      <div class="logo">
        <img v-if="shop.logo" :src="shop.logo" alt="" />
        <span v-else>🍜</span>
      </div>
      <div class="info">
        <h2>
          {{ shop.name }}
          <el-tag v-if="!shop.isOpen" type="info" size="small">休息中</el-tag>
          <el-tag v-else type="success" size="small" effect="plain">营业中</el-tag>
        </h2>
        <p v-if="shop.description" class="ew-muted">{{ shop.description }}</p>
        <div class="meta">
          <el-rate :model-value="Number(shop.avgScore) || 0" disabled size="small" allow-half />
          <span class="ew-muted">{{ shop.avgScore }} 分 · 月售 {{ shop.monthSales }}</span>
          <el-divider direction="vertical" />
          <span class="ew-muted">起送 ¥{{ shop.minOrderAmount }} / 配送 ¥{{ shop.deliveryFee }}</span>
          <template v-if="shop.distance != null">
            <el-divider direction="vertical" />
            <span class="ew-muted">距离 {{ shop.distance }} km</span>
          </template>
        </div>

        <!-- 地址 / 营业时间 / 电话 -->
        <div class="shop-facts">
          <div class="fact">
            <el-icon class="fact-icon"><Location /></el-icon>
            <span class="fact-label">地址</span>
            <span class="fact-value">{{ shop.address || '商家暂未填写地址' }}</span>
          </div>
          <div class="fact">
            <el-icon class="fact-icon"><Clock /></el-icon>
            <span class="fact-label">营业时间</span>
            <span class="fact-value">{{ businessTimeText }}</span>
            <el-button v-if="weeklyHours.length" link type="primary" size="small" @click="hoursVisible = true">
              查看全部
            </el-button>
          </div>
          <div class="fact">
            <el-icon class="fact-icon"><Phone /></el-icon>
            <span class="fact-label">联系电话</span>
            <span class="fact-value">
              <a v-if="shop.phone" :href="`tel:${shop.phone}`">{{ shop.phone }}</a>
              <span v-else>—</span>
            </span>
          </div>
        </div>

        <p v-if="shop.notice" class="notice">📢 {{ shop.notice }}</p>
        <p v-if="!shop.isOpen" class="closed-tip">
          🌙 {{ shop.closedTip || '商家休息中，暂时无法下单，可先浏览菜品' }}
        </p>
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

    <!-- 评价区：独立成栏，不再混在菜品菜单里 -->
    <div class="ew-card review-card">
      <div class="ew-flex-between">
        <h3 class="ew-title">店铺评价</h3>
        <div class="ew-flex ew-gap8">
          <el-select v-model="reviewScore" placeholder="全部评分" clearable style="width: 120px" @change="reloadReviews">
            <el-option v-for="s in [5, 4, 3, 2, 1]" :key="s" :label="`${s} 星`" :value="s" />
          </el-select>
          <span class="ew-muted">共 {{ reviewTotal }} 条</span>
        </div>
      </div>
      <div class="review-list" v-loading="reviewLoading">
        <div v-for="rv in reviews" :key="rv.id" class="review-item">
          <div class="ew-flex-between">
            <span class="review-user">{{ rv.userNickname || (rv.isAnonymous ? '匿名用户' : '用户') }}</span>
            <span class="ew-muted">{{ rv.createdAt }}</span>
          </div>
          <div class="review-scores">
            <span class="score-label">餐品</span>
            <el-rate :model-value="rv.shopScore" disabled size="small" />
            <span class="score-label">配送</span>
            <el-rate :model-value="rv.deliveryScore" disabled size="small" />
          </div>
          <p class="review-content">{{ rv.content }}</p>
          <div v-if="rv.reply" class="reply">商家回复：{{ rv.reply }}</div>
        </div>
        <el-empty v-if="!reviewLoading && reviews.length === 0" description="还没有评价" :image-size="70" />
      </div>
      <div v-if="reviewTotal > 10" class="pager">
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

    <!-- 菜单区：左侧分类锚点 + 右侧连续滚动 -->
    <div class="body">
      <div class="categories">
        <div class="search-box">
          <el-input
            v-model="searchKey"
            placeholder="搜索本店菜品"
            clearable
            size="small"
            :prefix-icon="Search"
          />
        </div>
        <template v-if="!searchKey">
          <div
            v-for="cat in categories"
            :key="cat.id"
            class="cat"
            :class="{ active: activeCat === cat.id }"
            @click="scrollToCategory(cat.id)"
          >
            {{ cat.name }}
          </div>
        </template>
        <div v-else class="search-hint">搜索到 {{ searchHits.length }} 道相关菜品</div>
      </div>

      <div ref="contentRef" class="content" @scroll.passive="onContentScroll">
        <template v-if="searchKey">
          <div class="ew-card">
            <template v-if="searchHits.length">
              <div v-for="hit in searchHits" :key="hit.dish.id" class="dish-item">
                <div class="dish-img">
                  <img v-if="hit.dish.image" :src="hit.dish.image" alt="" />
                  <span v-else>🍱</span>
                </div>
                <div class="dish-info">
                  <div class="dish-name" v-html="highlight(hit.dish.name)"></div>
                  <div class="ew-muted text-ellipsis">{{ hit.dish.description || '—' }}</div>
                  <div class="ew-muted">
                    {{ hit.catName }} · 月售 {{ hit.dish.monthSales }} · 库存 {{ hit.dish.stock }}
                    <span v-if="hit.dish.goodRate != null" class="good-rate">好评率 {{ hit.dish.goodRate }}%</span>
                  </div>
                  <div class="ew-flex-between ew-mt12">
                    <span class="ew-price">
                      <template v-if="hit.dish.specs && hit.dish.specs.length">
                        ¥{{ specMinPrice(hit.dish) }} 起
                      </template>
                      <template v-else>¥{{ hit.dish.price }}</template>
                    </span>
                    <div class="ew-flex ew-gap8">
                      <el-tag v-if="hit.dish.status === 0" size="small" type="info">已下架</el-tag>
                      <el-tag v-else-if="hit.dish.status === 2 || hit.dish.stock <= 0" size="small" type="warning">售罄</el-tag>
                      <el-button
                        v-else
                        type="primary"
                        size="small"
                        :disabled="!shop?.isOpen"
                        @click="openDish(hit.dish)"
                      >
                        加入购物车
                      </el-button>
                    </div>
                  </div>
                </div>
              </div>
            </template>
            <el-empty v-else description="没有找到相关菜品，换个关键词试试" :image-size="70" />
          </div>
        </template>

        <template v-else>
          <div
            v-for="cat in categories"
            :key="cat.id"
            :ref="(el) => setCatRef(cat.id, el)"
            :data-cat-id="cat.id"
            class="ew-card cat-block"
          >
            <h3 class="ew-title">{{ cat.name }}</h3>
            <div v-for="dish in cat.dishes" :key="dish.id" class="dish-item">
              <div class="dish-img">
                <img v-if="dish.image" :src="dish.image" alt="" />
                <span v-else>🍱</span>
              </div>
              <div class="dish-info">
                <div class="dish-name">
                  {{ dish.name }}
                  <el-tag v-if="dish.isCombo === 1" size="small" type="warning" effect="plain">套餐</el-tag>
                </div>
                <div class="ew-muted text-ellipsis">{{ dish.description || '—' }}</div>
                <div v-if="dish.isCombo === 1 && dish.comboItems && dish.comboItems.length" class="combo-items">
                  含
                  <span v-for="(ci, idx) in dish.comboItems" :key="ci.id || idx" class="combo-item">
                    {{ ci.dishName }} × {{ ci.quantity }}
                  </span>
                </div>
                <div class="ew-muted">
                  月售 {{ dish.monthSales }} · 库存 {{ dish.stock }}
                  <span v-if="dish.goodRate != null" class="good-rate">好评率 {{ dish.goodRate }}%</span>
                </div>
                <div class="ew-flex-between ew-mt12">
                  <span class="ew-price">
                    <template v-if="dish.specs && dish.specs.length">
                      ¥{{ specMinPrice(dish) }} 起
                    </template>
                    <template v-else>{{ dish.price }}</template>
                  </span>
                  <div class="ew-flex ew-gap8">
                    <el-tag v-if="dish.status === 0" size="small" type="info">已下架</el-tag>
                    <el-tag v-else-if="dish.status === 2 || dish.stock <= 0" size="small" type="warning">售罄</el-tag>
                    <template v-else>
                      <span v-if="dishQuantity(dish.id) > 0" class="picked-count">已选 {{ dishQuantity(dish.id) }}</span>
                      <el-button
                        type="primary"
                        size="small"
                        :disabled="!shop?.isOpen"
                        @click="openDish(dish)"
                      >
                        {{ dish.specs && dish.specs.length ? '选规格' : '加入' }}
                      </el-button>
                    </template>
                  </div>
                </div>
              </div>
            </div>
            <el-empty v-if="cat.dishes.length === 0" description="该分类暂无菜品" :image-size="60" />
          </div>
        </template>
      </div>
    </div>

    <!-- 规格与加料选择 -->
    <el-dialog v-model="dishVisible" :title="activeDish?.name || '选择规格'" width="480px">
      <div v-if="activeDish" class="dish-picker">
        <div class="picker-dish">
          <div class="dish-img">
            <img v-if="activeDish.image" :src="activeDish.image" alt="" />
            <span v-else>🍱</span>
          </div>
          <div class="picker-dish-info">
            <div class="dish-name">{{ activeDish.name }}</div>
            <div class="ew-muted text-ellipsis">{{ activeDish.description || '—' }}</div>
          </div>
        </div>

        <div v-if="activeDish.specs && activeDish.specs.length" class="picker-block">
          <div class="picker-label">规格 <span class="required">必选</span></div>
          <el-radio-group v-model="selectedSpecId" class="spec-group">
            <el-radio v-for="spec in activeDish.specs" :key="spec.id" :value="spec.id" border>
              {{ spec.name }} ¥{{ spec.price }}
            </el-radio>
          </el-radio-group>
        </div>

        <div v-if="activeDish.addons && activeDish.addons.length" class="picker-block">
          <div class="picker-label">加料 <span class="ew-muted">可多选，最多 5 项</span></div>
          <el-checkbox-group v-model="selectedAddonIds" class="addon-group">
            <el-checkbox
              v-for="addon in activeDish.addons"
              :key="addon.id"
              :value="addon.id"
              :disabled="selectedAddonIds.length >= 5 && !selectedAddonIds.includes(addon.id)"
              border
            >
              {{ addon.name }} +¥{{ addon.price }}
            </el-checkbox>
          </el-checkbox-group>
        </div>

        <div class="picker-block">
          <div class="picker-label">数量</div>
          <el-input-number v-model="pickQuantity" :min="1" :max="Math.min(99, activeDish.stock || 99)" />
        </div>

        <div class="picker-total">
          <span>小计</span>
          <span class="ew-price">{{ pickTotal }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="dishVisible = false">取消</el-button>
        <el-button type="primary" :loading="adding" @click="confirmAdd">加入购物车</el-button>
      </template>
    </el-dialog>

    <!-- 结算弹窗：地址 / 备注 / 餐具 -->
    <el-dialog v-model="checkoutVisible" title="确认订单" width="560px">
      <div class="checkout">
        <div class="checkout-shop">{{ shop?.name }}</div>
        <div class="checkout-items">
          <div v-for="item in cart.items" :key="item.id" class="checkout-item">
            <span class="ci-name">
              {{ item.dishName }}
              <span v-if="item.specName" class="ci-tag">{{ item.specName }}</span>
              <span v-if="item.addonNames" class="ci-tag">加料 {{ item.addonNames }}</span>
            </span>
            <span class="ew-muted">× {{ item.quantity }}</span>
            <span class="ci-price">¥{{ (Number(item.price) * item.quantity).toFixed(2) }}</span>
          </div>
        </div>

        <div class="checkout-block">
          <div class="ew-flex-between">
            <span class="block-title">收货地址</span>
            <el-button link type="primary" @click="$router.push('/address')">管理地址</el-button>
          </div>
          <el-radio-group v-if="addresses.length" v-model="addressId" class="address-list" @change="refreshFee">
            <el-radio v-for="addr in addresses" :key="addr.id" :value="addr.id" class="address-item">
              <span class="addr-name">{{ addr.contactName }} {{ addr.phone }}</span>
              <span class="ew-muted">{{ formatAddress(addr) }}</span>
              <el-tag v-if="addr.isDefault === 1" size="small" type="success">默认</el-tag>
            </el-radio>
          </el-radio-group>
          <el-empty v-else description="还没有收货地址，先添加一个" :image-size="60">
            <el-button type="primary" size="small" @click="$router.push('/address')">去添加</el-button>
          </el-empty>
        </div>

        <div class="checkout-block">
          <div class="block-title">订单备注</div>
          <el-input
            v-model="remark"
            type="textarea"
            :rows="2"
            maxlength="200"
            show-word-limit
            placeholder="口味、偏好等要求（选填）"
          />
          <div class="tableware">
            <span class="block-title">餐具份数</span>
            <el-input-number v-model="tablewareCount" :min="0" :max="20" size="small" />
            <span class="ew-muted">0 表示不需要一次性餐具</span>
          </div>
        </div>

        <div class="checkout-amounts">
          <div class="ca-row"><span class="ew-muted">商品金额</span><span>¥{{ cart.goodsAmount }}</span></div>
          <div class="ca-row"><span class="ew-muted">配送费</span><span>¥{{ cart.deliveryFee }}</span></div>
          <div v-if="Number(cart.packagingFee) > 0" class="ca-row">
            <span class="ew-muted">打包费</span><span>¥{{ cart.packagingFee }}</span>
          </div>
          <div class="ca-row ca-total">
            <span>合计</span><span class="ew-price">{{ cart.totalAmount }}</span>
          </div>
          <div v-if="belowMin" class="tip">
            还差 ¥{{ (Number(cart.minOrderAmount) - Number(cart.goodsAmount)).toFixed(2) }} 起送
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="checkoutVisible = false">再逛逛</el-button>
        <el-button
          type="primary"
          :disabled="belowMin || !addressId"
          :loading="submitting"
          @click="submitOrder"
        >
          提交订单
        </el-button>
      </template>
    </el-dialog>

    <!-- 营业时间详情 -->
    <el-dialog v-model="hoursVisible" title="营业时间" width="420px">
      <div class="hours-table">
        <div v-for="day in 7" :key="day" class="hours-row">
          <span class="hours-day">{{ WEEK_TEXT[day] }}</span>
          <span class="hours-value">{{ hoursOfDay(day) }}</span>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="hoursVisible = false">知道了</el-button>
      </template>
    </el-dialog>

    <!-- 底部购物车栏 -->
    <div class="cart-bar" v-if="cartCount > 0">
      <span class="ew-flex ew-gap12">
        <el-icon size="22"><ShoppingCart /></el-icon>
        <span>已选 {{ cartCount }} 件</span>
        <span class="ew-price">{{ cartTotal }}</span>
        <el-button link class="clear-btn" @click="clearCart">清空</el-button>
      </span>
      <el-button type="primary" size="large" @click="openCheckout">去结算</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock, Location, Phone, Search, ShoppingCart } from '@element-plus/icons-vue'
import { cartApi, favoriteApi, orderApi, shopApi, userApi } from '@/api'
import { formatAddress } from '@/utils/region'

const route = useRoute()
const router = useRouter()
const shopId = Number(route.params.id)

const loading = ref(false)
const shop = ref(null)
const categories = ref([])
const activeCat = ref(null)

const favorite = ref(false)
const favLoading = ref(false)

// 评价
const reviews = ref([])
const reviewTotal = ref(0)
const reviewPage = ref(1)
const reviewScore = ref(null)
const reviewLoading = ref(false)

// 购物车
const cart = ref({ items: [] })
const cartCount = computed(() =>
  (cart.value.items || []).reduce((sum, item) => sum + (item.quantity || 0), 0)
)
const cartTotal = computed(() => cart.value.totalAmount || '0.00')

// 结算
const checkoutVisible = ref(false)
const addresses = ref([])
const addressId = ref(null)
const remark = ref('')
const tablewareCount = ref(0)
const submitting = ref(false)
const belowMin = computed(() => {
  const goods = Number(cart.value.goodsAmount || 0)
  const min = Number(cart.value.minOrderAmount || 0)
  return goods < min
})

// 营业时间
const hoursVisible = ref(false)
const WEEK_TEXT = { 1: '周一', 2: '周二', 3: '周三', 4: '周四', 5: '周五', 6: '周六', 7: '周日' }
const weeklyHours = computed(() => shop.value?.businessHours || [])

function hhmm(value) {
  if (!value) return ''
  // 后端返回 HH:mm，兜底兼容 HH:mm:ss
  return String(value).slice(0, 5)
}

function todayOfWeek() {
  const day = new Date().getDay()
  return day === 0 ? 7 : day
}

const businessTimeText = computed(() => {
  const today = todayOfWeek()
  const rows = weeklyHours.value.filter((h) => h.dayOfWeek === today)
  if (rows.length) {
    return rows.map((h) => `${hhmm(h.openTime)} - ${hhmm(h.closeTime)}`).join('，')
  }
  if (shop.value?.openTime && shop.value?.closeTime) {
    return `${hhmm(shop.value.openTime)} - ${hhmm(shop.value.closeTime)}`
  }
  return '商家暂未设置营业时间'
})

function hoursOfDay(day) {
  const rows = weeklyHours.value.filter((h) => h.dayOfWeek === day)
  if (rows.length) return rows.map((h) => `${hhmm(h.openTime)} - ${hhmm(h.closeTime)}`).join('，')
  return shop.value?.openTime && shop.value?.closeTime
    ? `每日 ${hhmm(shop.value.openTime)} - ${hhmm(shop.value.closeTime)}`
    : '休息'
}

// 顾客-29：店内菜品搜索（跨分类匹配 + 关键词高亮）
const searchKey = ref('')
const searchHits = computed(() => {
  const kw = searchKey.value.trim().toLowerCase()
  if (!kw) return []
  const hits = []
  for (const cat of categories.value) {
    for (const dish of cat.dishes || []) {
      const inName = (dish.name || '').toLowerCase().includes(kw)
      const inDesc = (dish.description || '').toLowerCase().includes(kw)
      const inSpec = (dish.specs || []).some((s) => (s.name || '').toLowerCase().includes(kw))
      if (inName || inDesc || inSpec) hits.push({ dish, catName: cat.name })
    }
  }
  return hits
})

function escapeHtml(str) {
  return String(str ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function highlight(name) {
  const kw = searchKey.value.trim()
  if (!kw) return escapeHtml(name)
  const safe = escapeHtml(name)
  const safeKw = escapeHtml(kw).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return safe.replace(new RegExp(safeKw, 'gi'), (m) => `<span class="kw-hit">${m}</span>`)
}

// ===== 滚动联动 =====
const contentRef = ref(null)
const catRefs = reactive({})
let scrollLockTimer = null
let programmaticScroll = false

function setCatRef(id, el) {
  if (el) catRefs[id] = el
  else delete catRefs[id]
}

/** 右侧滚动时，把第一个进入视口顶部的分类设为高亮 */
function onContentScroll() {
  if (programmaticScroll || searchKey.value) return
  const container = contentRef.value
  if (!container) return
  const threshold = container.scrollTop + 12
  let current = null
  for (const cat of categories.value) {
    const el = catRefs[cat.id]
    if (!el) continue
    if (el.offsetTop - container.offsetTop <= threshold) current = cat.id
  }
  if (current != null && current !== activeCat.value) {
    activeCat.value = current
  }
}

/** 点击左侧分类：平滑滚动到对应菜品块 */
function scrollToCategory(catId) {
  const container = contentRef.value
  const el = catRefs[catId]
  if (!container || !el) {
    activeCat.value = catId
    return
  }
  activeCat.value = catId
  programmaticScroll = true
  const top = el.offsetTop - container.offsetTop
  container.scrollTo({ top, behavior: 'smooth' })
  clearTimeout(scrollLockTimer)
  scrollLockTimer = setTimeout(() => {
    programmaticScroll = false
  }, 500)
}

// ===== 数据加载 =====
async function load() {
  loading.value = true
  try {
    const data = await shopApi.detail(shopId)
    shop.value = data
    categories.value = data.categories || []
    activeCat.value = categories.value[0]?.id ?? null
    await refreshCart()
    await loadFavorite()
    await nextTick()
    onContentScroll()
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
    const data = await cartApi.get(addressId.value || undefined)
    cart.value = data || { items: [] }
  } catch {
    cart.value = { items: [] }
  }
}

/** 某个菜品在购物车里已选的数量（用于菜品卡片上的角标） */
function dishQuantity(dishId) {
  return (cart.value.items || [])
    .filter((item) => item.dishId === dishId)
    .reduce((sum, item) => sum + (item.quantity || 0), 0)
}

// ===== 规格与加料 =====
const dishVisible = ref(false)
const activeDish = ref(null)
const selectedSpecId = ref(null)
const selectedAddonIds = ref([])
const pickQuantity = ref(1)
const adding = ref(false)

const pickTotal = computed(() => {
  const dish = activeDish.value
  if (!dish) return '0.00'
  let unit = Number(dish.price || 0)
  const spec = (dish.specs || []).find((s) => s.id === selectedSpecId.value)
  if (spec) unit = Number(spec.price)
  for (const addon of dish.addons || []) {
    if (selectedAddonIds.value.includes(addon.id)) unit += Number(addon.price || 0)
  }
  return (unit * pickQuantity.value).toFixed(2)
})

function specMinPrice(dish) {
  const prices = (dish.specs || []).map((s) => Number(s.price))
  if (!prices.length) return Number(dish.price || 0).toFixed(2)
  return Math.min(...prices).toFixed(2)
}

function openDish(dish) {
  activeDish.value = dish
  const defaultSpec = (dish.specs || []).find((s) => s.isDefault === 1) || (dish.specs || [])[0]
  selectedSpecId.value = defaultSpec ? defaultSpec.id : null
  selectedAddonIds.value = []
  pickQuantity.value = 1
  dishVisible.value = true
}

async function confirmAdd() {
  const dish = activeDish.value
  if (!dish) return
  adding.value = true
  try {
    await cartApi.add({
      dishId: dish.id,
      quantity: pickQuantity.value,
      specId: selectedSpecId.value || undefined,
      addonIds: selectedAddonIds.value.length ? selectedAddonIds.value : undefined
    })
    ElMessage.success('已加入购物车')
    dishVisible.value = false
    await refreshCart()
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    adding.value = false
  }
}

async function clearCart() {
  try {
    await ElMessageBox.confirm('确定清空已选的菜品吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  await cartApi.clear()
  await refreshCart()
}

// ===== 结算下单 =====
async function openCheckout() {
  if (!shop.value?.isOpen) {
    ElMessage.warning(shop.value?.closedTip || '商家休息中，暂时无法下单')
    return
  }
  if (belowMin.value) {
    const diff = (Number(cart.value.minOrderAmount) - Number(cart.value.goodsAmount)).toFixed(2)
    ElMessage.warning(`还差 ¥${diff} 起送，再点一些吧`)
    return
  }
  addresses.value = (await userApi.addresses()) || []
  const def = addresses.value.find((a) => a.isDefault === 1)
  addressId.value = def?.id || addresses.value[0]?.id || null
  await refreshFee()
  checkoutVisible.value = true
}

async function refreshFee() {
  const data = await cartApi.get(addressId.value || undefined)
  cart.value = data || { items: [] }
}

async function submitOrder() {
  submitting.value = true
  try {
    const data = await orderApi.create({
      shopId: shop.value.id,
      addressId: addressId.value,
      remark: remark.value || undefined,
      tablewareCount: tablewareCount.value
    })
    ElMessage.success('下单成功，请尽快支付')
    checkoutVisible.value = false
    await refreshCart()
    router.push(`/orders/${data.orderId}`)
  } finally {
    submitting.value = false
  }
}

// ===== 评价 =====
async function loadReviews() {
  reviewLoading.value = true
  try {
    const data = await shopApi.reviews(shopId, {
      page: reviewPage.value,
      size: 10,
      score: reviewScore.value || undefined
    })
    reviews.value = data?.records || []
    reviewTotal.value = data?.total || 0
  } finally {
    reviewLoading.value = false
  }
}

function reloadReviews() {
  reviewPage.value = 1
  loadReviews()
}

onMounted(async () => {
  await load()
  await loadReviews()
})

onUnmounted(() => {
  clearTimeout(scrollLockTimer)
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

.info {
  flex: 1;
  min-width: 0;
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
  flex-wrap: wrap;
}

.shop-facts {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.fact {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  line-height: 1.6;
}

.fact-icon {
  color: var(--ew-text-light);
  flex-shrink: 0;
}

.fact-label {
  color: var(--ew-text-light);
  width: 60px;
  flex-shrink: 0;
}

.fact-value {
  color: #606266;
  word-break: break-all;
}

.notice {
  color: #b88230;
  background: #fdf6ec;
  padding: 6px 10px;
  border-radius: 6px;
  margin-top: 8px !important;
}

.closed-tip {
  color: #909399;
  background: #f4f4f5;
  padding: 6px 10px;
  border-radius: 6px;
  margin-top: 8px !important;
}

/* 评价区 */
.review-card {
  margin-bottom: 16px;
}

.review-list {
  min-height: 60px;
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

.review-scores {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
}

.score-label {
  font-size: 12px;
  color: var(--ew-text-light);
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

/* 菜单区 */
.body {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.search-box {
  padding: 4px 10px 12px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 8px;
}

.search-hint {
  padding: 10px 12px;
  color: #909399;
  font-size: 13px;
}

:deep(.kw-hit) {
  color: var(--el-color-primary);
  font-weight: 600;
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
  max-height: calc(100vh - 120px);
  overflow-y: auto;
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
  max-height: calc(100vh - 120px);
  overflow-y: auto;
  padding-right: 4px;
  scroll-behavior: smooth;
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

.picked-count {
  font-size: 12px;
  color: var(--ew-primary-dark);
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 12px;
}

.good-rate {
  margin-left: 8px;
  color: #e6a23c;
}

.combo-items {
  margin-top: 4px;
  font-size: 12px;
  color: #e6a23c;
  line-height: 1.6;
}

.combo-item {
  display: inline-block;
  margin-right: 8px;
  padding: 0 6px;
  background: #fdf6ec;
  border-radius: 4px;
}

/* 规格弹窗 */
.dish-picker .picker-dish {
  display: flex;
  gap: 12px;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f2f5;
}

.dish-picker .picker-dish .dish-img {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
}

.dish-picker .picker-dish .dish-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.dish-picker .picker-dish-info {
  flex: 1;
  min-width: 0;
}

.picker-block {
  margin-top: 16px;
}

.picker-label {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
}

.picker-label .required {
  color: #f56c6c;
  font-size: 12px;
}

.spec-group,
.addon-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.spec-group .el-radio,
.addon-group .el-checkbox {
  margin-right: 0;
}

.picker-total {
  margin-top: 18px;
  padding-top: 12px;
  border-top: 1px dashed #ebeef5;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
}

/* 结算弹窗 */
.checkout-shop {
  font-size: 15px;
  font-weight: 600;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--ew-border);
}

.checkout-items {
  padding: 10px 0;
  border-bottom: 1px solid var(--ew-border);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.checkout-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
}

.ci-name {
  flex: 1;
  min-width: 0;
}

.ci-tag {
  margin-left: 6px;
  font-size: 12px;
  color: #909399;
}

.ci-price {
  min-width: 70px;
  text-align: right;
}

.checkout-block {
  padding: 12px 0;
  border-bottom: 1px solid var(--ew-border);
}

.block-title {
  font-size: 14px;
  font-weight: 600;
}

.address-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 10px;
  width: 100%;
}

.address-item {
  height: auto;
  padding: 8px 10px;
  border: 1px solid var(--ew-border);
  border-radius: 8px;
  width: 100%;
  margin-right: 0;
}

.address-item.is-checked {
  border-color: var(--ew-primary);
  background: #fff8f4;
}

.addr-name {
  font-weight: 600;
  margin-right: 8px;
}

.tableware {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}

.checkout-amounts {
  padding-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.ca-row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
}

.ca-total {
  font-size: 16px;
  font-weight: 600;
  padding-top: 6px;
}

.tip {
  text-align: right;
  color: #e6a23c;
  font-size: 13px;
}

/* 营业时间 */
.hours-table {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.hours-row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  padding: 6px 0;
  border-bottom: 1px dashed #f0f0f0;
}

.hours-day {
  color: var(--ew-text-light);
}

/* 底部购物车 */
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

.clear-btn {
  color: #c0c4cc;
  font-size: 13px;
}
</style>
