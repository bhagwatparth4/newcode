import { parseTime } from './format'

export const FEEDBACK_DELAY_MIN = 12

// When we may ask for feedback: right away if cash was settled at the end, else 12 min after completion
export function feedbackReadyAt(order) {
  const completedAt = parseTime(order.updatedAt)?.getTime() || 0
  const paidAt = parseTime(order.paidAt)?.getTime() || 0
  const settledAtEnd = order.paymentMethod === 'CASH' && paidAt && Math.abs(completedAt - paidAt) <= 3 * 60000
  return completedAt + (settledAtEnd ? 0 : FEEDBACK_DELAY_MIN * 60000)
}

const flag = (key) => { try { return localStorage.getItem(key) === '1' } catch { return false } }
const setFlag = (key) => { try { localStorage.setItem(key, '1') } catch { /* optional */ } }

export const isFeedbackSkipped = (token) => flag(`rh_fb_skip_${token}`)
export const wasNudged = (token) => flag(`rh_fb_nudged_${token}`)
export const markNudged = (token) => setFlag(`rh_fb_nudged_${token}`)