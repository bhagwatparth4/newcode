import { useCallback, useEffect, useRef, useState } from 'react'

/*
 * "Order Received!" alert + bell sound for NEW PAID orders.
 *
 * - The sound file is a fixed local asset (/public/sounds/bell.wav).
 * - Browsers block sound until the user has clicked the page once
 *   (autoplay policy). We unlock audio on the first click anywhere, and
 *   the dashboard shows an "Enable sound" button until that happens.
 * - Each order rings only once: announced order IDs are remembered in
 *   localStorage. The server never replays old events, and even a duplicate
 *   event for the same order is ignored, so refresh / reconnect never re-rings.
 */
const ANNOUNCED_KEY = 'rh_announced_orders'
const MAX_REMEMBERED = 300

function readAnnounced() {
  try {
    return JSON.parse(localStorage.getItem(ANNOUNCED_KEY)) || []
  } catch {
    return []
  }
}

function rememberAnnounced(orderId) {
  const list = readAnnounced().filter((id) => id !== orderId)
  list.push(orderId)
  localStorage.setItem(ANNOUNCED_KEY, JSON.stringify(list.slice(-MAX_REMEMBERED)))
}

export function useOrderBell() {
  const audioRef = useRef(null)
  const [soundEnabled, setSoundEnabled] = useState(false)
  const [alerts, setAlerts] = useState([])

  const enableSound = useCallback(async () => {
    if (!audioRef.current) audioRef.current = new Audio('/sounds/bell.wav')
    const audio = audioRef.current
    try {
      // Play silently once inside a user gesture: this unlocks later playback
      audio.muted = true
      await audio.play()
      audio.pause()
      audio.currentTime = 0
      audio.muted = false
      setSoundEnabled(true)
    } catch {
      setSoundEnabled(false)
    }
  }, [])

  // Unlock automatically on the first click/tap anywhere on the page
  useEffect(() => {
    if (soundEnabled) return undefined
    const unlock = () => enableSound()
    window.addEventListener('pointerdown', unlock, { once: true })
    return () => window.removeEventListener('pointerdown', unlock)
  }, [soundEnabled, enableSound])

  const dismiss = useCallback((orderId) => {
    setAlerts((current) => current.filter((a) => a.orderId !== orderId))
  }, [])

  // Returns true when this is the first time we see this order
  const announce = useCallback(
    (notification) => {
      if (!notification?.orderId) return false
      if (readAnnounced().includes(notification.orderId)) return false
      rememberAnnounced(notification.orderId)

      setAlerts((current) => [...current.filter((a) => a.orderId !== notification.orderId), notification].slice(-4))
      setTimeout(() => dismiss(notification.orderId), 10000)

      const audio = audioRef.current
      if (audio && soundEnabled) {
        audio.currentTime = 0
        audio.play().catch(() => {})
      }
      return true
    },
    [soundEnabled, dismiss],
  )


  return { soundEnabled, enableSound, alerts, announce, dismiss }
}
