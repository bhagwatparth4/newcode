/*
 * Everything the customer picks while browsing, kept in sessionStorage so a
 * page refresh does not lose it. Nothing here is trusted by the backend:
 * prices, totals and discounts are always recalculated on the server.
 */
const QR = 'rh_qr'
const NICK = 'rh_nickname'
const CART = 'rh_cart'
const DIET = 'rh_diet'
const ATTEMPT = 'rh_checkout_attempt'
const LAST_ORDER = 'rh_last_order' // localStorage: survives closing the tab

function readJson(storage, key, fallback) {
  try {
    const value = JSON.parse(storage.getItem(key))
    return value ?? fallback
  } catch {
    return fallback
  }
}

// ---- QR context: { qrToken, orderType, tableNumber } ----
export const getQr = () => readJson(sessionStorage, QR, null)
export const saveQr = (qr) => sessionStorage.setItem(QR, JSON.stringify(qr))

// ---- Nickname ----
export const getNickname = () => sessionStorage.getItem(NICK) || ''
export const saveNickname = (name) => sessionStorage.setItem(NICK, name)

// ---- Dietary filter: ALL | VEG | NON_VEG (kept for this browsing session) ----
export const getDiet = () => sessionStorage.getItem(DIET) || 'ALL'
export const saveDiet = (diet) => sessionStorage.setItem(DIET, diet)

// ---- Cart: { [menuItemId]: quantity } ----
export const getCart = () => readJson(sessionStorage, CART, {})
export const saveCart = (cart) => sessionStorage.setItem(CART, JSON.stringify(cart))
export const clearCart = () => { sessionStorage.removeItem(CART); sessionStorage.removeItem(CUSTOM) }

// ---- Items with add-ons: [{ key, menuItemId, name, addonIds, addonsText, unitPrice, qty }] ----
const CUSTOM = 'rh_cart_custom'
export const getCustomCart = () => readJson(sessionStorage, CUSTOM, [])
export const saveCustomCart = (list) => sessionStorage.setItem(CUSTOM, JSON.stringify(list))

/*
 * Idempotency key for order creation.
 * The same cart + details reuse the same key, so double-clicks, refreshes and
 * network retries return the SAME order from the backend instead of new ones.
 * A different cart gets a new key.
 */
export function checkoutKeyFor(signature) {
  const attempt = readJson(sessionStorage, ATTEMPT, null)
  if (attempt && attempt.signature === signature) return attempt.key
  const key = crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(36).slice(2)}`
  sessionStorage.setItem(ATTEMPT, JSON.stringify({ signature, key }))
  return key
}
export const clearCheckoutAttempt = () => sessionStorage.removeItem(ATTEMPT)

// ---- Last order (for "Track your order" shortcut) ----
export const getLastOrder = () => readJson(localStorage, LAST_ORDER, null)
export const saveLastOrder = (order) => localStorage.setItem(LAST_ORDER, JSON.stringify(order))
export const clearLastOrder = () => localStorage.removeItem(LAST_ORDER)

// The receipt printing animation plays only once per order
export const wasReceiptPrinted = (token) => sessionStorage.getItem(`rh_printed_${token}`) === '1'
export const markReceiptPrinted = (token) => sessionStorage.setItem(`rh_printed_${token}`, '1')

// ---- Remembered mobile number (saved on this phone only) ----
export const getSavedMobile = () => localStorage.getItem('rh_mobile') || ''
export const saveMobile = (mobile) => localStorage.setItem('rh_mobile', mobile)

//---- This phone's orders from today (max 5) ----
export function getMyOrders() {
    const since = new Date().setHours(0, 0, 0, 0)
  return readJson(localStorage, 'rh_orders', []).filter((o) => o.at > since)
}
export function addMyOrder(order) {
  const list = getMyOrders().filter((o) => o.token !== order.token)
  list.unshift({ ...order, at: Date.now() })
  localStorage.setItem('rh_orders', JSON.stringify(list.slice(0, 5)))
}


