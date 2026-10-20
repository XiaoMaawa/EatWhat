/**
 * 省 / 市 / 区县 三级数据（内置中国全部行政区划）。
 *
 * 数据源：element-china-area-data 的 regionData（约 31 省 / 337 市 / 2900+ 区县）。
 * 该数据里直辖市的二级节点是「市辖区」，展示时不友好，这里统一替换成直辖市名，
 * 例如 北京市 / 市辖区 / 朝阳区 → 北京市 / 北京市 / 朝阳区。
 *
 * 对外只暴露 label（中文名），因为 user_address 表的 province/city/district
 * 存的就是中文名，value 用行政区划代码仅供 el-cascader 内部定位。
 */
import { regionData } from 'element-china-area-data'

/** 直辖市：二级节点「市辖区」统一替换成城市自身名称 */
const MUNICIPALITIES = new Set(['北京市', '上海市', '天津市', '重庆市'])

/** 二级节点不是真实城市名的占位值 */
const PLACEHOLDER_CITY = new Set(['市辖区', '县', '省直辖县级行政区划', '自治区直辖县级行政区划'])

function normalizeCityName(provinceLabel, cityLabel) {
  if (MUNICIPALITIES.has(provinceLabel)) return provinceLabel
  return cityLabel
}

function buildTree() {
  return regionData.map((province) => ({
    value: province.value,
    label: province.label,
    children: (province.children || []).map((city) => ({
      value: city.value,
      label: normalizeCityName(province.label, city.label),
      children: (city.children || []).map((district) => ({
        value: district.value,
        label: district.label
      }))
    }))
  }))
}

/** el-cascader 用的三级树 */
export const regionTree = buildTree()

/**
 * 由省/市/区县中文名反查 el-cascader 需要的 code 数组。
 * 找不到返回空数组（历史脏数据或手填地址时会出现）。
 */
export function toRegionCodes(province, city, district) {
  if (!province) return []
  const provinceNode = regionTree.find((p) => p.label === province)
  if (!provinceNode) return []
  if (!city) return [provinceNode.value]
  const cityNode = provinceNode.children.find((c) => c.label === city)
  if (!cityNode) return [provinceNode.value]
  if (!district) return [provinceNode.value, cityNode.value]
  const districtNode = cityNode.children.find((d) => d.label === district)
  if (!districtNode) return [provinceNode.value, cityNode.value]
  return [provinceNode.value, cityNode.value, districtNode.value]
}

/**
 * 由 el-cascader 的 code 数组反查省/市/区县中文名。
 * 返回 { province, city, district }，取不到的层级为空串。
 */
export function fromRegionCodes(codes) {
  const empty = { province: '', city: '', district: '' }
  if (!Array.isArray(codes) || codes.length === 0) return empty
  const provinceNode = regionTree.find((p) => p.value === codes[0])
  if (!provinceNode) return empty
  const result = { province: provinceNode.label, city: '', district: '' }
  if (codes.length < 2) return result
  const cityNode = provinceNode.children.find((c) => c.value === codes[1])
  if (!cityNode) return result
  result.city = cityNode.label
  if (codes.length < 3) return result
  const districtNode = cityNode.children.find((d) => d.value === codes[2])
  if (districtNode) result.district = districtNode.label
  return result
}

/** 完整地址文本：省 + 市 + 区 + 详细地址，并去掉直辖市重复的市名 */
export function formatAddress(addr) {
  if (!addr) return ''
  const province = addr.province || ''
  const city = addr.city || ''
  const district = addr.district || ''
  const detail = addr.detail || ''
  const region = city && city === province ? `${province}${district}` : `${province}${city}${district}`
  return `${region}${detail}`
}

/** 判断是否为占位城市名（老数据里可能残留「市辖区」） */
export function isPlaceholderCity(city) {
  return PLACEHOLDER_CITY.has(city)
}
