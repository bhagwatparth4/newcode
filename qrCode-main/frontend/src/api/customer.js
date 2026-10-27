import { request } from './client'

// Public (no login) customer endpoints

export const resolveQr = (qrToken) => request(`/api/qr/${encodeURIComponent(qrToken)}`)

export const getMenuItems = () => request('/api/menu/items')

export const getPaymentOptions = () => request('/api/menu/payment-options')

export const getCategories = () => request('/api/menu/categories')

export const validateCoupon = (code, orderAmount) =>
  request('/api/customer/coupons/validate', { method: 'POST', body: { code, orderAmount } })

export const createOrder = (body) => request('/api/orders', { method: 'POST', body })

export const createRazorpayOrder = (orderId, notificationToken) =>
  request('/api/payments/razorpay/order', { method: 'POST', body: { orderId, notificationToken } })

export const verifyPayment = (body) => request('/api/payments/razorpay/verify', { method: 'POST', body })

export const getOrderDetails = (token) => request(`/api/orders/customer/details/${encodeURIComponent(token)}`)

export const getCafeStatus = () => request('/api/menu/status')

export const cancelMyOrder = (token) =>
  request(`/api/orders/customer/cancel/${encodeURIComponent(token)}`, { method: 'PATCH' })


export const getBestsellers = () => request('/api/menu/bestsellers')

export const getOffer = () => request('/api/menu/offer')

export const sendTableRequest = (qrToken, type) =>
  request('/api/orders/customer/table-request', { method: 'POST', body: { qrToken, type } })

export const sendFeedback = (token, rating, comment) =>
  request(`/api/orders/customer/feedback/${encodeURIComponent(token)}`, { method: 'PATCH', body: { rating, comment } })

export const getAddons = () => request('/api/menu/addons')