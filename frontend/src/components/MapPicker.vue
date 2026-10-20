<template>
  <div class="map-picker">
    <div class="picker-bar">
      <el-input
        v-model="keyword"
        :prefix-icon="Search"
        placeholder="搜索地点，例如：杭州文三路 88 号"
        clearable
        @keyup.enter="doSearch"
      />
      <el-button type="primary" :loading="searching" @click="doSearch">搜索</el-button>
    </div>

    <div v-if="results.length" class="result-list">
      <div
        v-for="(item, index) in results"
        :key="index"
        class="result-item"
        :class="{ active: activeIndex === index }"
        @click="chooseResult(item, index)"
      >
        <div class="result-name">{{ item.name }}</div>
        <div class="result-addr">{{ item.address || '—' }}</div>
      </div>
    </div>
    <el-alert
      v-if="searchError"
      :title="searchError"
      type="warning"
      :closable="false"
      show-icon
      class="picker-alert"
    />

    <div ref="mapEl" class="map-canvas"></div>

    <div class="picker-foot">
      <span class="coord">
        经度 <b>{{ current.longitude ?? '—' }}</b>
        　纬度 <b>{{ current.latitude ?? '—' }}</b>
      </span>
      <div class="foot-actions">
        <el-button size="small" @click="useAddressPoint">用地址定位</el-button>
        <el-button size="small" type="primary" @click="confirm">确定</el-button>
      </div>
    </div>
    <div class="picker-tip">在地图上点击可精确选点；拖动地图后点「用地址定位」可按上方地址重新定位。</div>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { AMAP_MAX_ZOOM, AMAP_TILE_SUBDOMAINS, AMAP_TILE_URL, searchPlace } from '@/utils/map'

const props = defineProps({
  /** 初始经度（GCJ-02） */
  longitude: { type: [Number, String], default: null },
  /** 初始纬度（GCJ-02） */
  latitude: { type: [Number, String], default: null },
  /** 地址文本，用于「用地址定位」 */
  address: { type: String, default: '' }
})

const emit = defineEmits(['confirm'])

const mapEl = ref()
const keyword = ref('')
const searching = ref(false)
const searchError = ref('')
const results = ref([])
const activeIndex = ref(-1)
const current = reactive({ longitude: null, latitude: null })

let map = null
let marker = null

/** 默认中心：杭州西湖区 */
const DEFAULT_CENTER = [30.2741, 120.1551]

function toNum(v) {
  if (v === null || v === undefined || v === '') return null
  const n = Number(v)
  return Number.isNaN(n) ? null : n
}

function setPoint(lon, lat, panTo = true) {
  const lng = toNum(lon)
  const latNum = toNum(lat)
  if (lng === null || latNum === null) return
  current.longitude = Number(lng.toFixed(6))
  current.latitude = Number(latNum.toFixed(6))
  if (!map) return
  if (!marker) {
    marker = L.marker([current.latitude, current.longitude], { draggable: true }).addTo(map)
    marker.on('dragend', () => {
      const p = marker.getLatLng()
      current.latitude = Number(p.lat.toFixed(6))
      current.longitude = Number(p.lng.toFixed(6))
    })
  } else {
    marker.setLatLng([current.latitude, current.longitude])
  }
  if (panTo) {
    map.setView([current.latitude, current.longitude], Math.max(map.getZoom(), 16))
  }
}

function initMap() {
  if (!mapEl.value || map) return
  map = L.map(mapEl.value, {
    center: DEFAULT_CENTER,
    zoom: 14,
    attributionControl: false
  })
  L.tileLayer(AMAP_TILE_URL, {
    subdomains: AMAP_TILE_SUBDOMAINS,
    maxZoom: AMAP_MAX_ZOOM
  }).addTo(map)
  map.on('click', (e) => {
    setPoint(e.latlng.lng, e.latlng.lat, false)
  })
}

async function doSearch() {
  const kw = keyword.value.trim()
  if (!kw) {
    ElMessage.warning('请输入要搜索的地点')
    return
  }
  searching.value = true
  searchError.value = ''
  try {
    const list = await searchPlace(kw)
    results.value = list
    activeIndex.value = -1
    if (!list.length) {
      searchError.value = '没有找到匹配的地点，可以直接在地图上点选'
    }
  } catch (e) {
    results.value = []
    searchError.value = e?.message || '地点搜索失败，请直接在地图上点选'
  } finally {
    searching.value = false
  }
}

function chooseResult(item, index) {
  activeIndex.value = index
  setPoint(item.longitude, item.latitude)
}

/** 按地址文本重新定位（拖动地图后想回到地址点时用） */
async function useAddressPoint() {
  const text = props.address || keyword.value
  if (!text || !String(text).trim()) {
    ElMessage.warning('还没有可用的地址文本')
    return
  }
  keyword.value = String(text).trim()
  await doSearch()
  if (results.value.length) {
    chooseResult(results.value[0], 0)
  }
}

function confirm() {
  if (current.longitude === null || current.latitude === null) {
    ElMessage.warning('请先在地图上选择一个位置')
    return
  }
  emit('confirm', { longitude: current.longitude, latitude: current.latitude })
}

watch(
  () => [props.longitude, props.latitude],
  () => {
    if (props.longitude != null && props.latitude != null) {
      setPoint(props.longitude, props.latitude)
    }
  }
)

onMounted(async () => {
  await nextTick()
  initMap()
  if (props.longitude != null && props.latitude != null) {
    setPoint(props.longitude, props.latitude)
  }
})

onBeforeUnmount(() => {
  if (map) {
    map.remove()
    map = null
    marker = null
  }
})
</script>

<style scoped>
.map-picker {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.picker-bar {
  display: flex;
  gap: 8px;
}

.result-list {
  max-height: 168px;
  overflow-y: auto;
  border: 1px solid var(--ew-border, #ebeef5);
  border-radius: 8px;
}

.result-item {
  padding: 8px 12px;
  cursor: pointer;
  border-bottom: 1px solid var(--ew-border, #f2f6fc);
}

.result-item:last-child {
  border-bottom: none;
}

.result-item:hover,
.result-item.active {
  background: var(--el-color-primary-light-9, #ecf5ff);
}

.result-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.result-addr {
  margin-top: 2px;
  font-size: 12px;
  color: #909399;
  word-break: break-all;
}

.picker-alert {
  margin: 0;
}

.map-canvas {
  width: 100%;
  height: 300px;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f7fa;
  /* Leaflet 容器必须有明确高度，否则瓦片不渲染 */
  z-index: 0;
}

.picker-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.coord {
  font-size: 13px;
  color: #606266;
}

.coord b {
  color: #303133;
  font-weight: 600;
}

.foot-actions {
  display: flex;
  gap: 8px;
}

.picker-tip {
  font-size: 12px;
  color: #909399;
}

/* Leaflet 默认标记图标依赖相对路径，打包后 404，这里用 CSS 圆点替代 */
:deep(.leaflet-marker-icon) {
  background: var(--el-color-primary, #409eff);
  border: 3px solid #fff;
  border-radius: 50%;
  box-shadow: 0 0 0 2px var(--el-color-primary, #409eff);
}
</style>
