/**
 * 地图相关工具（商家-06 / 顾客-16）。
 *
 * 底图用高德公开瓦片（无需 Key），坐标使用 GCJ-02（火星坐标）。
 * 地点搜索用 Photon（基于 OpenStreetMap，免 Key），它返回 WGS-84 坐标，
 * 展示到高德底图上需要做 WGS-84 → GCJ-02 纠偏，否则会有几百米偏移。
 */

/** 高德瓦片底图（免 Key） */
export const AMAP_TILE_URL =
  'https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}'

export const AMAP_TILE_SUBDOMAINS = ['1', '2', '3', '4']

/** 高德底图最大可用层级 */
export const AMAP_MAX_ZOOM = 18

/** 中国境内判断（境外不做纠偏） */
function outOfChina(lon, lat) {
  return lon < 72.004 || lon > 137.8347 || lat < 0.8293 || lat > 55.8271
}

function transformLat(x, y) {
  let ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x))
  ret += ((20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0) / 3.0
  ret += ((20.0 * Math.sin(y * Math.PI) + 40.0 * Math.sin((y / 3.0) * Math.PI)) * 2.0) / 3.0
  ret += ((160.0 * Math.sin((y / 12.0) * Math.PI) + 320 * Math.sin((y * Math.PI) / 30.0)) * 2.0) / 3.0
  return ret
}

function transformLon(x, y) {
  let ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x))
  ret += ((20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0) / 3.0
  ret += ((20.0 * Math.sin(x * Math.PI) + 40.0 * Math.sin((x / 3.0) * Math.PI)) * 2.0) / 3.0
  ret += ((150.0 * Math.sin((x / 12.0) * Math.PI) + 300.0 * Math.sin((x / 30.0) * Math.PI)) * 2.0) / 3.0
  return ret
}

/**
 * WGS-84 → GCJ-02。
 * @param {number} lon 经度
 * @param {number} lat 纬度
 * @returns {{ longitude: number, latitude: number }}
 */
export function wgs84ToGcj02(lon, lat) {
  if (lon == null || lat == null || outOfChina(lon, lat)) {
    return { longitude: lon, latitude: lat }
  }
  const a = 6378245.0
  const ee = 0.00669342162296594323
  let dLat = transformLat(lon - 105.0, lat - 35.0)
  let dLon = transformLon(lon - 105.0, lat - 35.0)
  const radLat = (lat / 180.0) * Math.PI
  let magic = Math.sin(radLat)
  magic = 1 - ee * magic * magic
  const sqrtMagic = Math.sqrt(magic)
  dLat = (dLat * 180.0) / (((a * (1 - ee)) / (magic * sqrtMagic)) * Math.PI)
  dLon = (dLon * 180.0) / ((a / sqrtMagic) * Math.cos(radLat) * Math.PI)
  return { longitude: lon + dLon, latitude: lat + dLat }
}

/**
 * GCJ-02 → WGS-84（近似逆变换，误差在 1 米内，足够展示用）。
 */
export function gcj02ToWgs84(lon, lat) {
  if (lon == null || lat == null || outOfChina(lon, lat)) {
    return { longitude: lon, latitude: lat }
  }
  const gcj = wgs84ToGcj02(lon, lat)
  return {
    longitude: lon * 2 - gcj.longitude,
    latitude: lat * 2 - gcj.latitude
  }
}

/** 数值保留 6 位小数（约 0.1 米精度） */
export function round6(value) {
  if (value == null || Number.isNaN(Number(value))) return null
  return Number(Number(value).toFixed(6))
}

/**
 * 地点搜索：Photon（OSM），免 Key。
 * 返回统一结构，坐标已纠偏为 GCJ-02。
 */
export async function searchPlace(keyword, limit = 8) {
  const kw = (keyword || '').trim()
  if (!kw) return []
  const url = `https://photon.komoot.io/api/?q=${encodeURIComponent(kw)}&limit=${limit}`
  const res = await fetch(url)
  if (!res.ok) {
    throw new Error(`地点搜索失败（${res.status}）`)
  }
  const json = await res.json()
  const features = json.features || []
  return features
    .filter((f) => Array.isArray(f.geometry?.coordinates))
    .map((f) => {
      const [wgsLon, wgsLat] = f.geometry.coordinates
      const gcj = wgs84ToGcj02(wgsLon, wgsLat)
      const p = f.properties || {}
      const parts = [p.state, p.city, p.district, p.street, p.name]
        .filter((v) => v && String(v).trim())
        .map((v) => String(v).trim())
      // 去重相邻重复项（Photon 常把街道名同时放在 street 和 name）
      const dedup = parts.filter((v, i) => i === 0 || v !== parts[i - 1])
      return {
        name: p.name || p.street || kw,
        address: dedup.join(''),
        longitude: round6(gcj.longitude),
        latitude: round6(gcj.latitude),
        raw: p
      }
    })
}
