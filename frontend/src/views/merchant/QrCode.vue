<template>
  <div class="ew-page">
    <div class="ew-card">
      <h3 class="ew-title">店铺二维码</h3>
      <p class="ew-muted tip">顾客扫描二维码即可直达本店点餐页，适合张贴在餐桌、门贴与外卖包装上。</p>

      <el-empty v-if="!shop" description="店铺信息加载中…" />
      <template v-else>
        <div class="qr-wrap">
          <img v-if="qrDataUrl" :src="qrDataUrl" class="qr-img" alt="店铺二维码" />
          <el-skeleton v-else :rows="4" animated class="qr-skeleton" />
          <div class="qr-info">
            <p class="shop-name">{{ shop.name }}</p>
            <p class="ew-muted">{{ shop.address }}</p>
            <p class="link-label">店铺链接</p>
            <el-input v-model="shopUrl" readonly>
              <template #append>
                <el-button @click="copyLink">复制</el-button>
              </template>
            </el-input>
            <div class="actions">
              <el-button type="primary" @click="download">下载二维码</el-button>
              <el-button @click="openShop">预览店铺页</el-button>
            </div>
            <p class="ew-muted hint">
              提示：二维码指向本站店铺详情页，部署到公网后扫描即可打开；本地演示请用同一台设备的浏览器预览。
            </p>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import QRCode from 'qrcode'
import { merchantApi } from '@/api'

const shop = ref(null)
const qrDataUrl = ref('')

const shopUrl = computed(() =>
  shop.value ? `${window.location.origin}/shop/${shop.value.id}` : ''
)

async function render() {
  if (!shopUrl.value) return
  qrDataUrl.value = await QRCode.toDataURL(shopUrl.value, {
    width: 480,
    margin: 2,
    errorCorrectionLevel: 'M',
    color: { dark: '#303133', light: '#ffffff' }
  })
}

async function load() {
  shop.value = await merchantApi.shop().catch(() => null)
  await render()
}

async function copyLink() {
  try {
    await navigator.clipboard.writeText(shopUrl.value)
    ElMessage.success('店铺链接已复制')
  } catch {
    ElMessage.warning('复制失败，请手动选择链接复制')
  }
}

function download() {
  if (!qrDataUrl.value) return
  const a = document.createElement('a')
  a.href = qrDataUrl.value
  a.download = `${shop.value?.name || 'shop'}-二维码.png`
  a.click()
}

function openShop() {
  window.open(shopUrl.value, '_blank')
}

onMounted(load)
</script>

<style scoped>
.tip {
  margin-bottom: 16px;
}

.qr-wrap {
  display: flex;
  gap: 32px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.qr-img {
  width: 240px;
  height: 240px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 12px;
  background: #fff;
  image-rendering: pixelated;
}

.qr-skeleton {
  width: 264px;
  height: 264px;
}

.qr-info {
  flex: 1;
  min-width: 320px;
}

.shop-name {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 6px;
}

.link-label {
  margin: 16px 0 6px;
  font-weight: 600;
}

.actions {
  margin-top: 14px;
}

.hint {
  margin-top: 14px;
  font-size: 12px;
  line-height: 1.7;
}
</style>
