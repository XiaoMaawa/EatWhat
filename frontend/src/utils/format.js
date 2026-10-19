/** 金额格式化：保留两位小数 */
export function money(value) {
  const num = Number(value || 0)
  return num.toFixed(2)
}

/** 距离格式化：米/千米 */
export function distance(value) {
  const num = Number(value || 0)
  if (num <= 0) return '—'
  return num >= 1000 ? `${(num / 1000).toFixed(1)} km` : `${Math.round(num)} m`
}

/** 日期时间简写：MM-DD HH:mm */
export function shortTime(value) {
  if (!value) return '—'
  return String(value).slice(5, 16)
}

/** 手机号脱敏 */
export function maskPhone(phone) {
  if (!phone || phone.length < 11) return phone || '—'
  return `${phone.slice(0, 3)}****${phone.slice(7)}`
}

export function today() {
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}
