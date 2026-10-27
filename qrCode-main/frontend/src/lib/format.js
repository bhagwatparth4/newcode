const inr = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  minimumFractionDigits: 0,
  maximumFractionDigits: 2,
})

export function money(value) {
  const n = Number(value ?? 0)
  return inr.format(Number.isFinite(n) ? n : 0)
}

/*
 * The backend sends times like "2026-09-23T14:05:00" (cafe local time, no zone).
 * The browser reads such a string as local time, which is correct for
 * customers and staff in the same country as the cafe.
 */
export function parseTime(value) {
  if (!value) return null
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? null : d
}

export function formatDate(value) {
  const d = parseTime(value)
  return d ? d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : '-'
}

export function formatTime(value) {
  const d = parseTime(value)
  return d ? d.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' }) : '-'
}

export function formatDateTime(value) {
  const d = parseTime(value)
  return d ? `${formatDate(value)}, ${formatTime(value)}` : '-'
}

// "YYYY-MM-DD" for <input type="date"> and the sales API
export function isoDate(date) {
  const d = new Date(date)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

export function placeLabel(orderType, tableNumber) {
  return orderType === 'TABLE' && tableNumber ? `Table ${tableNumber}` : 'Counter'
}

export function minutesAgo(value) {
  const d = parseTime(value)
  if (!d) return ''
  const mins = Math.max(0, Math.round((Date.now() - d.getTime()) / 60000))
  if (mins < 1) return 'just now'
  if (mins < 60) return `${mins} min ago`
  return `${Math.floor(mins / 60)} h ${mins % 60} min ago`
}

export const STATUS_LABEL = {
  PAYMENT_PENDING: 'Awaiting payment',
  PAID: 'New',
  PREPARING: 'Preparing',
  READY: 'Ready',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
}
