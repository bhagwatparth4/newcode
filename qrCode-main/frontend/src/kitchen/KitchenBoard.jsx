import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { BRAND } from '../config/brand'
import { ackCancelKitchen, getKitchenOrders, setKitchenOrderStatus } from '../api/staff'
import { clearAuth, getAuth } from '../auth/authStore'
import { ErrorNote, Logo } from '../components/ui'
import { useToast } from '../components/Toast'
import OrderAlerts from '../components/OrderAlerts'
import { useRealtime } from '../lib/useRealtime'
import { useOrderBell } from '../lib/useOrderBell'
import { useNow } from '../lib/useNow'
import { formatTime, parseTime, placeLabel } from '../lib/format'
import CancelledOrderCard from '../components/CancelledOrderCard'
import { printKot } from '../lib/print'
/*
 * Kitchen screen (LARGE_CAFE / RESTAURANT only). Tablet-first, big cards.
 * Kitchen can do:  PAID -> PREPARING -> READY.  Never COMPLETED
 * (the backend rejects it even if someone tries).
 */
const NEXT = {
  PAID: { to: 'PREPARING', label: 'Start preparing', style: 'bg-espresso text-mint' },
  PREPARING: { to: 'READY', label: 'Mark ready', style: 'bg-mint text-espresso' },
}

export default function KitchenBoard() {
  const navigate = useNavigate()
  const toast = useToast()
  const session = getAuth()
  const bell = useOrderBell()
  const now = useNow(30000)
  const [orders, setOrders] = useState([])
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState(null)

  const load = useCallback(() => {
    getKitchenOrders().then((data) => { setOrders(data); setError('') }).catch((e) => setError(e.message))
  }, [])
  useEffect(() => { load() }, [load])

  const connected = useRealtime({
    topics: ['/topic/kitchen/orders', '/topic/kitchen/order-updates'],
    token: session?.token,
    onConnect: load,
    onMessage: (topic, event) => {
      if (topic === '/topic/kitchen/orders') bell.announce(event)
      load()
    },
  })

  const advance = async (order) => {
    const next = NEXT[order.status]
    if (!next) return
    setBusyId(order.id)
    try {
      await setKitchenOrderStatus(order.id, next.to)
      load()
    } catch (e) {
      toast(e.message, 'error')
      load()
    } finally {
      setBusyId(null)
    }
  }

    const ack = async (order) => {
      try { await ackCancelKitchen(order.id); load() } catch (e) { toast(e.message, 'error') }
    }

  const logout = () => {
    clearAuth()
    navigate('/login', { replace: true })
  }

  const newCount = orders.filter((o) => o.status === 'PAID').length

  return (
    <div className="min-h-dvh bg-stone-900 text-white">
      <header className="flex items-center gap-3 border-b border-white/10 px-5 py-3">
        <Logo size={40} />
        <div className="flex-1">
          <p className="font-display text-xl font-semibold">{BRAND.name} · Kitchen</p>
          <p className="text-sm text-white/60">
            {newCount} new · {orders.length - newCount} preparing
            <span className={`ml-3 inline-flex items-center gap-1.5 ${connected ? 'text-mint' : 'text-amber-400'}`}>
              <span className={`h-2 w-2 rounded-full ${connected ? 'bg-mint' : 'bg-amber-400'}`} />
              {connected ? 'Live' : 'Reconnecting…'}
            </span>
          </p>
        </div>
        <button onClick={logout} className="rounded-xl px-3 py-2 text-sm text-white/70 hover:bg-white/10">Log out</button>
      </header>

      <main className="p-4 sm:p-6">
        <ErrorNote>{error}</ErrorNote>
        {orders.length === 0 && !error && (
          <p className="mt-24 text-center text-xl text-white/50">No orders right now. New paid orders appear here automatically.</p>
        )}

        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {orders.map((order) => {
                      if (order.status === 'CANCELLED') {
                        return <CancelledOrderCard key={order.id} order={order} onOk={() => ack(order)} />
                      }
            const next = NEXT[order.status]
            const readyAt = parseTime(order.estimatedReadyAt)
            const late = order.status === 'PREPARING' && readyAt && readyAt.getTime() < now
            return (
              <article
                key={order.id}
                                className={`flex flex-col rounded-3xl p-5 ${order.status === 'CANCELLED' ? 'bg-red-600 text-white' : order.status === 'PAID' ? 'bg-white text-espresso ring-4 ring-amber-400' : 'bg-white/95 text-espresso'}`}
              >
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="font-display text-3xl font-bold">{placeLabel(order.orderType, order.tableNumber)}</p>
                    <p className="text-base text-stone-600">{order.orderNumber} · {order.nickname}</p>
                  </div>
                  <span className={`rounded-full px-3 py-1 text-sm font-bold ${order.status === 'PAID' ? 'bg-amber-400' : 'bg-sky-100 text-sky-800'}`}>
                                        {order.status === 'CANCELLED' ? 'CANCELLED' : order.status === 'PAID' ? 'NEW' : 'Preparing'}
                  </span>
                </div>

                <ul className="my-4 flex-1 space-y-1.5 text-lg">
                  {order.items.map((item, i) => (
                                       <li key={i}>
                                         <span className="font-bold">{item.quantity} ×</span> {item.itemName}
                                         {item.addonsText && <span className="block pl-7 text-base font-bold text-amber-700">+ {item.addonsText}</span>}
                                       </li>
                  ))}
                </ul>

                                {order.cookingNote && (
                                  <p className="mb-3 rounded-xl bg-amber-100 px-3 py-2 font-semibold text-amber-900">📝 {order.cookingNote}</p>
                                )}

                <p className={`text-sm ${late ? 'font-semibold text-red-700' : 'text-stone-500'}`}>
                  Ordered {formatTime(order.createdAt)} · {late ? 'Past estimate' : `Est. ready ${formatTime(order.estimatedReadyAt)}`}
                </p>

                                                <button onClick={() => printKot(order)} className="mt-3 w-full rounded-xl border border-stone-300 py-2 text-sm font-semibold text-stone-700">
                                                  🖨️ Print KOT
                                                </button>


                               {next && (
                                 <button
                                   onClick={() => advance(order)}
                                   disabled={busyId === order.id}
                                   className={`mt-4 w-full rounded-2xl py-4 text-lg font-bold transition active:scale-[0.98] disabled:opacity-50 ${next.style}`}
                                 >
                                   {next.label}
                                 </button>
                               )}

                               {order.status === 'CANCELLED' && (
                                 <button onClick={() => ack(order)} className="mt-4 w-full rounded-2xl bg-white py-4 text-lg font-bold text-red-700">
                                   OK – Don't make
                                 </button>
                               )}
                             </article>
            )
          })}
        </div>
      </main>

      <OrderAlerts alerts={bell.alerts} onDismiss={bell.dismiss} soundEnabled={bell.soundEnabled} onEnableSound={bell.enableSound} />
    </div>
  )
}
