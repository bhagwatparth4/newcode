import { useEffect, useState } from 'react'
import { getOrdersBetween } from '../api/staff'
import { ErrorNote, PageLoader } from '../components/ui'
import { formatDate, formatTime, placeLabel } from '../lib/format'
import { useAdmin } from './AdminContext'

const pad = (n) => String(n).padStart(2, '0')
const iso = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
const RANGES = [7, 30, 90]
const FILTERS = [['ALL', 'All orders'], ['GIVEN', 'With feedback'], ['LOW', '😟 Low (1-2★)']]
const Stars = ({ n }) => <span className="text-amber-400">{'★'.repeat(n)}<span className="text-stone-300">{'★'.repeat(5 - n)}</span></span>

export default function FeedbackPage() {
  const { liveVersion } = useAdmin()
  const [days, setDays] = useState(7)
  const [filter, setFilter] = useState('ALL')
  const [orders, setOrders] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    const from = new Date()
    from.setDate(from.getDate() - (days - 1))
    from.setHours(0, 0, 0, 0)
    getOrdersBetween(iso(from), iso(new Date()))
      .then((data) => {
        const done = data.filter((o) => o.status === 'COMPLETED')
        done.sort((a, b) => String(b.createdAt).localeCompare(String(a.createdAt)))
        setOrders(done)
        setError('')
      })
      .catch((e) => setError(e.message))
  }, [days, liveVersion])

  if (error) return <ErrorNote>{error}</ErrorNote>
  if (!orders) return <PageLoader label="Loading feedback" />

  const rated = orders.filter((o) => o.rating != null)
  const avg = rated.length ? rated.reduce((s, o) => s + o.rating, 0) / rated.length : 0
  const shown = orders.filter((o) =>
    filter === 'GIVEN' ? o.rating != null : filter === 'LOW' ? o.rating != null && o.rating <= 2 : true)

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="font-display text-3xl font-semibold">Customer feedback</h1>
          <p className="text-stone-500">Ratings and comments for completed orders.</p>
        </div>
        <div className="inline-flex rounded-xl bg-white p-1 shadow-sm">
          {RANGES.map((d) => (
            <button key={d} onClick={() => setDays(d)} className={`rounded-lg px-3.5 py-1.5 text-sm font-medium ${days === d ? 'bg-espresso text-mint' : 'text-stone-600'}`}>
              {d} days
            </button>
          ))}
        </div>
      </div>

      <section className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-3xl bg-espresso p-5 text-paper">
          <p className="text-sm text-paper/70">Average rating</p>
          <p className="mt-1 font-display text-4xl font-semibold">{rated.length ? avg.toFixed(1) : '–'} <span className="text-2xl text-amber-300">★</span></p>
        </div>
        <div className="rounded-3xl bg-white p-5 shadow-sm">
          <p className="text-sm text-stone-500">Rated orders</p>
          <p className="mt-1 text-3xl font-semibold">{rated.length} <span className="text-base font-normal text-stone-500">of {orders.length}</span></p>
        </div>
        <div className="rounded-3xl bg-white p-5 shadow-sm">
          {[5, 4, 3, 2, 1].map((n) => {
            const count = rated.filter((o) => o.rating === n).length
            return (
              <div key={n} className="flex items-center gap-2 text-xs">
                <span className="w-6">{n}★</span>
                <span className="h-2 flex-1 overflow-hidden rounded-full bg-stone-100">
                  <span className="block h-full bg-amber-400" style={{ width: `${rated.length ? (count / rated.length) * 100 : 0}%` }} />
                </span>
                <span className="w-6 text-right text-stone-500">{count}</span>
              </div>
            )
          })}
        </div>
      </section>

      <div className="flex flex-wrap gap-2">
        {FILTERS.map(([key, label]) => (
          <button key={key} onClick={() => setFilter(key)} className={`rounded-full border px-4 py-1.5 text-sm font-medium ${filter === key ? 'border-espresso bg-espresso text-mint' : 'border-stone-200 bg-white text-stone-600'}`}>
            {label}
          </button>
        ))}
      </div>

      <section className="divide-y divide-stone-100 rounded-3xl bg-white shadow-sm">
        {shown.length === 0 && <p className="p-5 text-sm text-stone-500">Nothing here yet.</p>}
        {shown.map((o) => (
          <article key={o.id} className={`p-4 ${o.rating != null && o.rating <= 2 ? 'bg-red-50/60' : ''}`}>
            <div className="flex flex-wrap items-start justify-between gap-2">
              <div>
                <p className="font-semibold">{o.orderNumber} · {o.nickname}</p>
                <p className="text-xs text-stone-500">
                  {formatDate(o.createdAt)} {formatTime(o.createdAt)} · {placeLabel(o.orderType, o.tableNumber)} · {o.mobileNumber}
                </p>
              </div>
              <div className="text-right text-lg">
                {o.rating != null ? <Stars n={o.rating} /> : <span className="text-sm text-stone-400">Rating not provided</span>}
              </div>
            </div>
            <p className="mt-1 text-xs text-stone-500">{(o.items || []).map((i) => `${i.quantity} × ${i.itemName}`).join(', ')}</p>
            <p className={`mt-2 text-sm ${o.feedback ? '' : 'text-stone-400'}`}>
              {o.feedback ? `💬 “${o.feedback}”` : 'Feedback not provided'}
            </p>
          </article>
        ))}
      </section>
    </div>
  )
}