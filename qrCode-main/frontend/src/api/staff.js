import { downloadFile, request } from './client'

/*
 * Admin + Kitchen API calls. All require login (JWT). The backend checks
 * the role on every call: hiding a button here is NOT the security.
 */
const get = (path) => request(path, { auth: true })
const send = (method, path, body) => request(path, { method, body, auth: true })

// ---- Auth ----
export const login = (email, password) =>
  request('/api/admin/auth/login', { method: 'POST', body: { email, password } })

// ---- Orders ----
function orderQuery({ status, orderType, from, to } = {}) {
  const q = new URLSearchParams()
  if (status) q.set('status', status)
  if (orderType) q.set('orderType', orderType)
  if (from) q.set('from', `${from}T00:00:00`)
  if (to) q.set('to', `${to}T23:59:59`)
  const s = q.toString()
  return s ? `?${s}` : ''
}
export const getOrders = (filters) => get(`/api/admin/orders${orderQuery(filters)}`)
export const getActiveOrders = () => get('/api/admin/orders/active')
export const getOrder = (id) => get(`/api/admin/orders/${id}`)
export const setAdminOrderStatus = (id, status) =>
  send('PATCH', `/api/admin/orders/${id}/status?status=${encodeURIComponent(status)}`)
export const downloadOrdersExcel = (filters) =>
  downloadFile(`/api/admin/orders/export${orderQuery(filters)}`, 'RushHour_Orders.xlsx')

// ---- Sales ----
export const getSalesSummary = () => get('/api/admin/sales/summary')
const range = (from, to) => `?from=${from}&to=${to}`
export const getDailySales = (from, to) => get(`/api/admin/sales/daily${range(from, to)}`)
export const getTopItems = (from, to) => get(`/api/admin/sales/top-items${range(from, to)}`)
export const getDietarySales = (from, to) => get(`/api/admin/sales/dietary${range(from, to)}`)
export const getOrderTypeSales = (from, to) => get(`/api/admin/sales/order-types${range(from, to)}`)

// ---- Menu ----
export const getAdminCategories = () => get('/api/admin/categories')
export const saveCategory = (category) =>
  category.id ? send('PUT', `/api/admin/categories/${category.id}`, category) : send('POST', '/api/admin/categories', category)
export const deleteCategory = (id) => send('DELETE', `/api/admin/categories/${id}`)
export const getAdminMenuItems = () => get('/api/admin/menu-items')
export const saveMenuItem = (item) =>
  item.id ? send('PUT', `/api/admin/menu-items/${item.id}`, item) : send('POST', '/api/admin/menu-items', item)
export const deleteMenuItem = (id) => send('DELETE', `/api/admin/menu-items/${id}`)

// ---- Tables & QR ----
export const getTables = () => get('/api/admin/tables')
export const saveTable = (table) =>
  table.id ? send('PUT', `/api/admin/tables/${table.id}`, table) : send('POST', '/api/admin/tables', table)
export const deleteTable = (id) => send('DELETE', `/api/admin/tables/${id}`)
export const downloadTableQr = (table) => downloadFile(`/api/admin/tables/${table.id}/qr`, `${table.tableNumber}-QR.png`)
export const getCounterQr = () => get('/api/admin/counter-qr')
export const createCounterQr = () => send('POST', '/api/admin/counter-qr')
export const setCounterQrActive = (id, active) => send('PUT', `/api/admin/counter-qr/${id}/${active ? 'activate' : 'deactivate'}`)
export const downloadCounterQr = (id) => downloadFile(`/api/admin/counter-qr/${id}/qr`, 'COUNTER-QR.png')

// ---- Coupons ----
export const getCoupons = () => get('/api/admin/coupons')
export const saveCoupon = (coupon) =>
  coupon.id ? send('PUT', `/api/admin/coupons/${coupon.id}`, coupon) : send('POST', '/api/admin/coupons', coupon)
export const setCouponActive = (id, active) => send('PATCH', `/api/admin/coupons/${id}/status?active=${active}`)

// ---- Customers & marketing ----
export const getCustomers = () => get('/api/admin/customers')
export const updateCustomerPreferences = (prefs) => send('PATCH', '/api/customer/marketing/preferences', prefs)
export const getCampaigns = () => get('/api/admin/marketing/campaigns')
export const createCampaign = (campaign) => send('POST', '/api/admin/marketing/campaigns', campaign)
export const sendCampaign = (id) => send('POST', `/api/admin/marketing/campaigns/${id}/send`)

// ---- Settings ----
export const getBusinessType = () => get('/api/admin/settings/business-type')
export const getPrepTime = () => get('/api/admin/settings/preparation-time')
export const setPrepTime = (minutes) => send('PUT', '/api/admin/settings/preparation-time', { minutes })
export const createKitchenUser = (user) => send('POST', '/api/admin/users/kitchen', user)

// ---- Kitchen ----
export const getKitchenOrders = () => get('/api/kitchen/orders')
export const setKitchenOrderStatus = (id, status) =>
  send('PATCH', `/api/kitchen/orders/${id}/status?status=${encodeURIComponent(status)}`)


export const setAcceptingOrders = (acceptingOrders) =>
  send('PUT', '/api/admin/settings/accepting-orders', { acceptingOrders })

export const getCashEnabled = () => get('/api/admin/settings/cash-enabled')

export const setCashEnabled = (enabled) =>
  send('PUT', '/api/admin/settings/cash-enabled', { cashEnabled: enabled })


export const markCashPaid = (id) => send('PATCH', `/api/admin/orders/${id}/mark-paid`)
export const cancelCashOrder = (id) => send('PATCH', `/api/admin/orders/${id}/cancel-cash`)


export const ackCancelAdmin = (id) => send('PATCH', `/api/admin/orders/${id}/cancel-seen`)
export const ackCancelKitchen = (id) => send('PATCH', `/api/kitchen/orders/${id}/cancel-seen`) // kitchen prefix check


export const bulkChangePrices = (percent) => send('POST', '/api/admin/menu/bulk-price', { percent })


export const getOrdersBetween = (from, to) =>
  get(`/api/admin/orders?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`)


export const getItemAddons = (itemId) => get(`/api/admin/menu/items/${itemId}/addons`)
export const saveItemAddons = (itemId, groups) => send('PUT', `/api/admin/menu/items/${itemId}/addons`, groups)

export const createCounterOrder = (body) => send('POST', '/api/admin/orders/counter', body)

export const putAcceptingOrders = (accepting) =>
  send('PUT', '/api/admin/settings/accepting-orders', { acceptingOrders: accepting })