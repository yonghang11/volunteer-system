export function parseTime(value) {
  if (!value) return null
  if (value instanceof Date) return value
  const date = new Date(String(value).replace(' ', 'T'))
  return Number.isNaN(date.getTime()) ? null : date
}

export function formatTime(value, withSeconds = false) {
  const date = parseTime(value)
  if (!date) return value || '—'
  const pad = (n) => String(n).padStart(2, '0')
  let text = `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(
    date.getHours()
  )}:${pad(date.getMinutes())}`
  if (withSeconds) text += `:${pad(date.getSeconds())}`
  return text
}

export function formatRemain(ms) {
  const total = Math.max(0, Math.floor(ms / 1000))
  const days = Math.floor(total / 86400)
  const hours = Math.floor((total % 86400) / 3600)
  const minutes = Math.floor((total % 3600) / 60)
  const seconds = total % 60
  const pad = (n) => String(n).padStart(2, '0')
  const clock = `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
  return days > 0 ? `${days} 天 ${clock}` : clock
}

export const PHASE_MAP = {
  OPEN: { text: '进行中', type: 'success' },
  PENDING: { text: '未开始', type: 'warning' },
  CLOSED: { text: '已结束', type: 'info' }
}

export const OFFER_STATUS_MAP = {
  PENDING: { text: '待确认', type: 'warning' },
  CONFIRMED: { text: '已确认', type: 'success' },
  DECLINED: { text: '已放弃', type: 'danger' },
  EXPIRED: { text: '已过期', type: 'danger' },
  FILLED: { text: '已递补', type: 'info' }
}

export const OFFER_TYPE_MAP = {
  OFFER: { text: '拟录取', type: 'success' },
  WAITLIST: { text: '递补', type: 'warning' },
  INNER: { text: '内部调剂', type: 'info' }
}

export const INTERVIEW_STATUS_MAP = {
  PENDING: { text: '待面试', type: 'warning' },
  DONE: { text: '已评分', type: 'success' },
  ABSENT: { text: '缺席', type: 'danger' }
}

export const RESULT_MAP = {
  OFFER: { text: '拟录取', type: 'success' },
  WAITLIST: { text: '递补', type: 'warning' },
  INNER: { text: '内部调剂', type: 'info' },
  REJECT: { text: '不予录取', type: 'danger' }
}

export const PUBLIC_STATUS_MAP = {
  PUBLISHED: { text: '公示中', type: 'warning' },
  OBJECTED: { text: '有异议', type: 'danger' },
  JOINED: { text: '已入组', type: 'success' }
}

export const TASK_STATUS_MAP = {
  PENDING: { text: '待处理', type: 'warning' },
  ASSIGNED: { text: '已指派', type: 'success' },
  ACCEPTED: { text: '已接受', type: 'success' },
  REFUSED: { text: '已拒绝', type: 'danger' }
}

export const PRIORITY_MAP = {
  1: { text: '优先级 1 · 室内调剂', type: 'success' },
  2: { text: '优先级 2 · 跨室调剂', type: 'warning' },
  3: { text: '优先级 3 · 强制安置', type: 'danger' }
}

export function tagOf(map, key) {
  if (map[key]) return map[key]
  return { text: key || '未知', type: 'info' }
}

export function totalRemain(slots) {
  if (!Array.isArray(slots)) return 0
  return slots.reduce((sum, slot) => sum + (Number(slot.remain) || 0), 0)
}
