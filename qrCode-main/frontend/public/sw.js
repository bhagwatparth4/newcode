/*
 * Service worker: only used for Web Push ("your order is ready")
 * when the customer's page is closed. It does not cache anything.
 */
self.addEventListener('push', (event) => {
  let data = {}
  try {
    data = event.data ? event.data.json() : {}
  } catch {
    data = { message: event.data ? event.data.text() : '' }
  }
  const title = data.title || 'Order update'
  event.waitUntil(
    self.registration.showNotification(title, {
      body: data.message || '',
      icon: '/icon-192.png',
      badge: '/favicon.png',
    }),
  )
})

self.addEventListener('notificationclick', (event) => {
  event.notification.close()
  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((list) => {
      for (const client of list) {
        if (client.url.includes('/order/') && 'focus' in client) return client.focus()
      }
      return self.clients.openWindow('/')
    }),
  )
})
