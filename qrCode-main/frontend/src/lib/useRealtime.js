import { useEffect, useRef, useState } from 'react'
import { Client } from '@stomp/stompjs'
import { WS_URL } from '../config/brand'

/*
 * Live updates over WebSocket (STOMP), used by the customer, admin and kitchen.
 *
 * - One connection per screen, subscriptions created on every (re)connect,
 *   so there are never duplicate subscriptions.
 * - `onConnect` runs after every connect/reconnect: screens use it to
 *   re-fetch the current state from the backend (the source of truth),
 *   so nothing is missed while the connection was down.
 * - Old events are NOT replayed by the server after a reconnect, so a
 *   refresh never re-rings the bell for old orders.
 */
export function useRealtime({ topics, onMessage, onConnect, token, enabled = true }) {
  const [connected, setConnected] = useState(false)
  const onMessageRef = useRef(onMessage)
  const onConnectRef = useRef(onConnect)
  onMessageRef.current = onMessage
  onConnectRef.current = onConnect

  const topicKey = (topics || []).join('|')

  useEffect(() => {
    if (!enabled || !topicKey) return undefined

    const client = new Client({
      brokerURL: WS_URL,
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 4000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: () => {},
    })

    client.onConnect = () => {
      setConnected(true)
      for (const topic of topicKey.split('|')) {
        client.subscribe(topic, (frame) => {
          let payload = null
          try {
            payload = JSON.parse(frame.body)
          } catch {
            payload = frame.body
          }
          onMessageRef.current?.(topic, payload)
        })
      }
      onConnectRef.current?.()
    }

    client.onWebSocketClose = () => setConnected(false)
    client.onStompError = () => setConnected(false)

    client.activate()

    return () => {
      client.deactivate()
      setConnected(false)
    }
  }, [topicKey, token, enabled])

  return connected
}
