import { request } from '../api/client'

/*
 * Web Push: lets us tell the customer "your order is ready" even after they
 * close the page. Completely optional: if the browser does not support it,
 * the server has no keys, or the customer says "Block", ordering and live
 * tracking keep working normally.
 */
export function pushSupported() {
  return 'serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window
}

export function pushPermission() {
  return pushSupported() ? Notification.permission : 'unsupported'
}

export async function getPushPublicKey() {
  try {
    const data = await request('/api/customer/push/public-key')
    return data?.publicKey || ''
  } catch {
    return ''
  }
}

function urlBase64ToUint8Array(base64) {
  const padding = '='.repeat((4 - (base64.length % 4)) % 4)
  const normalized = (base64 + padding).replace(/-/g, '+').replace(/_/g, '/')
  const raw = atob(normalized)
  return Uint8Array.from([...raw].map((c) => c.charCodeAt(0)))
}

function toBase64Url(buffer) {
  const bytes = new Uint8Array(buffer)
  let binary = ''
  bytes.forEach((b) => (binary += String.fromCharCode(b)))
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

// Returns 'subscribed' | 'denied' | 'unavailable'
export async function subscribeToOrderPush({ orderId, notificationToken, publicKey }) {
  if (!pushSupported() || !publicKey) return 'unavailable'

  const permission = await Notification.requestPermission()
  if (permission !== 'granted') return 'denied'

  const registration = await navigator.serviceWorker.register('/sw.js')
  await navigator.serviceWorker.ready

  let subscription = await registration.pushManager.getSubscription()
  if (!subscription) {
    subscription = await registration.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: urlBase64ToUint8Array(publicKey),
    })
  }

  await request('/api/customer/push/subscribe', {
    method: 'POST',
    body: {
      orderId,
      notificationToken,
      endpoint: subscription.endpoint,
      p256dh: toBase64Url(subscription.getKey('p256dh')),
      auth: toBase64Url(subscription.getKey('auth')),
    },
  })

  return 'subscribed'
}
