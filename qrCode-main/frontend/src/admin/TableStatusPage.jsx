import { useCallback, useEffect, useState } from 'react'
import { getOrdersBetween, getTables } from '../api/staff'
import { ErrorNote, PageLoader } from '../components/ui'
import { formatTime, money, parseTime } from '../lib/format'
import { useNow } from '../lib/useNow'
import { useAdmin } from './AdminContext'

const ACTIVE = ['PAID', 'PREPARING', 'READY']
const STATUS_TEXT = { PAID: 'Received', PREPARING: 'Preparing', READY: 'Ready' }
const EAT_MINUTES = 45   // table stays "Eating" this long after the last order is completed
const LONG_MINUTES = 60  // highlight tables occupied longer than this
const CLEARED_KEY = 'rh_table_cleared'

const pad = (n) => String(n).padStart(2, '0')
const localIso = (d) =>
  `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
const clock = (ms) => {
  const s = Math.max(0, Math.floor(ms / 1000))
  return `${Math.floor(s / 3600)}:${pad(Math.floor((s % 3600) / 60))}:${pad(s % 60)}`
}
const time = (v) => parseTime(v)?.getTime() || 0

// "Mark free" taps, saved on this device: { [tableNumber]: timestamp }
export function readCleared() {
  try { return JSON.parse(localStorage.getItem(CLEARED_KEY)) || {} } catch { return {} }
}

/*
 * Works out one table's current visit from today's orders.
 * A visit lasts while any order is cooking, plus EAT_MINUTES after the last
 * one is completed. An order arriving after that starts a new visit.
 */
export function tableVisit(orders, clearedAt, now) {
  const eatMs = EAT_MINUTES * 60000
  const list = orders
    // "Mark free" hides finished orders only; a running order is never hidden
    .filter((o) => !(o.status === 'COMPLETED' && time(o.updatedAt) <= clearedAt))
    .sort((a, b) => time(a.createdAt) - time(b.createdAt))

  let start = null
  let busyUntil = 0
  let visit = []
  for (const o of list) {
    const created = time(o.createdAt)
    if (start === null || created > busyUntil) {
      start = created
      visit = []
    }
    visit.push(o)
           // Bill settled after eating = cash taken well after the order was placed, and the order closed right then
           const paidLater = o.paidAt && time(o.paidAt) - time(o.createdAt) > 5 * 60000
           const settledAtEnd = o.status === 'COMPLETED' && o.paymentMethod === 'CASH' && paidLater
             && Math.abs(time(o.updatedAt) - time(o.paidAt)) <= 3 * 60000
        if (ACTIVE.includes(o.status)) {
          busyUntil = Infinity
        } else if (settledAtEnd) {
          if (busyUntil !== Infinity) busyUntil = time(o.updatedAt) // table frees right away
        } else {
          busyUntil = Math.max(busyUntil, time(o.updatedAt) + eatMs)
        }
  }

  if (start === null || busyUntil <= now) return null
  const cooking = visit.filter((o) => ACTIVE.includes(o.status))
  const latest = visit[visit.length - 1]
  return {
    start,
    state: cooking.length > 0 ? 'occupied' : 'eating',
    freeIn: cooking.length > 0 ? null : Math.ceil((busyUntil - now) / 60000),
    hasFinished: visit.some((o) => o.status === 'COMPLETED'),
    count: visit.length,
    status: latest.status,
    total: visit.reduce((s, o) => s + Number(o.totalAmount || 0), 0),
    cashDue: cooking
      .filter((o) => o.paymentMethod === 'CASH' && o.orderPaymentStatus !== 'PAID')
      .reduce((s, o) => s + Number(o.totalAmount || 0), 0),
  }
}

export default function TableStatusPage() {
  const { liveVersion } = useAdmin()
  const now = useNow(1000)
  const [tables, setTables] = useState(null)
  const [orders, setOrders] = useState([])
  const [cleared, setCleared] = useState(readCleared)
  const [error, setError] = useState('')

  const load = useCallback(() => {
    const start = new Date()
    start.setHours(0, 0, 0, 0)
    const end = new Date()
    end.setHours(23, 59, 59, 0)
    Promise.all([getTables(), getOrdersBetween(localIso(start), localIso(end))])
      .then(([t, o]) => {
        setTables(t)
        setOrders(o.filter((x) => x.orderType === 'TABLE' && x.status !== 'CANCELLED' && x.status !== 'PAYMENT_PENDING'))
        setError('')
      })
      .catch((e) => setError(e.message))
  }, [])

  useEffect(() => { load() }, [load, liveVersion])

  const markFree = (tableNumber) => {
    const next = { ...cleared, [tableNumber]: Date.now() }
    setCleared(next)
    try { localStorage.setItem(CLEARED_KEY, JSON.stringify(next)) } catch { /* optional */ }
  }

  if (!tables && !error) return <PageLoader label="Loading tables" />

  const cards = (tables || [])
    .filter((t) => t.active !== false)
    .map((t) => ({
      t,
      visit: tableVisit(orders.filter((o) => o.tableNumber === t.tableNumber), cleared[t.tableNumber] || 0, now),
    }))
    .sort((a, b) => String(a.t.tableNumber).localeCompare(String(b.t.tableNumber), undefined, { numeric: true }))

  const busy = cards.filter((c) => c.visit)
  const eating = busy.filter((c) => c.visit.state === 'eating').length
  const avgMin = busy.length ? Math.round(busy.reduce((s, c) => s + (now - c.visit.start), 0) / busy.length / 60000) : 0

  return (
    <div>
      <div className="mb-5 flex flex-wrap items-end justify-between gap-2">
        <div>
          <h1 className="font-display text-3xl font-semibold">Tables</h1>
          <p className="text-stone-500">Live occupancy · a table frees itself {EAT_MINUTES} min after its last order is completed</p>
        </div>
        <div className="flex flex-wrap gap-2 text-xs font-medium">
          <span className="rounded-full bg-[#E1F5EE] px-2.5 py-1 text-[#085041]">🟢 Free</span>
          <span className="rounded-full bg-[#EEE3D6] px-2.5 py-1 text-[#4A3B30]">🟤 Occupied</span>
          <span className="rounded-full bg-sky-100 px-2.5 py-1 text-sky-800">🍽️ Eating</span>
          <span className="rounded-full bg-amber-100 px-2.5 py-1 text-amber-800">⏰ Over {LONG_MINUTES} min</span>
        </div>
      </div>
      <ErrorNote>{error}</ErrorNote>

      <div className="mb-5 grid grid-cols-2 gap-3 sm:grid-cols-4">
        {[['Occupied', busy.length - eating], ['Eating', eating], ['Free', cards.length - busy.length], ['Avg. time', `${avgMin} min`]].map(([label, value]) => (
          <div key={label} className="rounded-2xl border border-stone-200 bg-white px-4 py-3">
            <p className="text-xs text-stone-500">{label}</p>
            <p className="text-2xl font-semibold">{value}</p>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
        {cards.map(({ t, visit: v }) => {
          const minutes = v ? Math.floor((now - v.start) / 60000) : 0
          const long = v && minutes >= LONG_MINUTES
          const isEating = v?.state === 'eating'

          const box = !v
            ? 'border border-stone-200 bg-white'
            : long ? 'border-2 border-amber-400 bg-amber-50'
            : isEating ? 'border border-sky-200 bg-sky-50'
            : 'border border-stone-200 bg-[#F7F1EA]'
          const pill = !v
            ? ['bg-[#E1F5EE] text-[#085041]', 'Free']
            : long ? ['bg-amber-100 text-amber-800', `⏰ ${minutes} min`]
            : isEating ? ['bg-sky-100 text-sky-800', '🍽️ Eating']
            : ['bg-[#EEE3D6] text-[#4A3B30]', 'Occupied']

          return (
            <article key={t.id ?? t.tableNumber} className={`rounded-2xl p-3 ${box}`}>
              <div className="flex items-center justify-between">
                <span className="text-lg font-semibold">{t.tableNumber}</span>
                <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${pill[0]}`}>{pill[1]}</span>
              </div>

              {!v ? (
                <p className="mt-2 text-2xl font-semibold text-stone-300">—</p>
              ) : (
                <>
                  <p className="mt-2 text-2xl font-semibold tabular-nums">{clock(now - v.start)}</p>
                  <p className="text-xs text-stone-500">
                    Since {formatTime(new Date(v.start).toISOString())} · {v.count} order{v.count > 1 ? 's' : ''} · {money(v.total)}
                  </p>
                  <p className="text-xs">
                    {isEating ? `All served · frees in ~${v.freeIn} min` : STATUS_TEXT[v.status] || 'In progress'}
                  </p>
                  {v.cashDue > 0 && <p className="mt-1 text-xs font-semibold text-orange-800">💵 {money(v.cashDue)} cash due</p>}

                  {(isEating || v.hasFinished) && (
                    <button onClick={() => markFree(t.tableNumber)} className="mt-2 text-xs font-semibold text-mint-deep underline-offset-2 hover:underline">
                      {isEating ? '✓ Mark free' : '↺ New guests? Reset'}
                    </button>
                  )}
                </>
              )}
            </article>
          )
        })}
      </div>
    </div>
  )
}