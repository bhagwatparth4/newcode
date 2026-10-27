import { useEffect, useState } from 'react'
import { getOrdersBetween } from '../api/staff'
import { money } from '../lib/format'
import { useAdmin } from './AdminContext'

const pad = (n) => String(n).padStart(2, '0')
const localIso = (d) =>
  `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`

// Today's money split: online, cash collected, cash still to collect
export default function PaymentsTodayCard() {
  const { liveVersion } = useAdmin()
  const [orders, setOrders] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    const start = new Date()
    start.setHours(0, 0, 0, 0)
    getOrdersBetween(localIso(start), localIso(new Date()))
      .then((data) => { setOrders(data); setError('') })
      .catch((e) => setError(e.message))
  }, [liveVersion])

  if (error) return <p className="rounded-2xl bg-red-50 p-4 text-sm text-red-800">Payments: {error}</p>
  if (!orders) return null

  // Ignore cancelled orders and online orders that were never paid
  const counted = orders.filter((o) => o.status !== 'CANCELLED' && o.status !== 'PAYMENT_PENDING')
  const sum = (list) => list.reduce((s, o) => s + Number(o.totalAmount || 0), 0)

  const online = counted.filter((o) => o.paymentMethod !== 'CASH')
  const cashPaid = counted.filter((o) => o.paymentMethod === 'CASH' && o.orderPaymentStatus === 'PAID')
  const cashDue = counted.filter((o) => o.paymentMethod === 'CASH' && o.orderPaymentStatus !== 'PAID')

  const total = sum(online) + sum(cashPaid) + sum(cashDue)
  const pct = (list) => (total ? (sum(list) / total) * 100 : 0)
  const cashPct = Math.round(pct(cashPaid) + pct(cashDue))

  const tiles = [
    { label: 'Online', list: online, box: 'bg-[#E1F5EE]', text: 'text-mint-deep', note: 'orders' },
    { label: 'Cash collected', list: cashPaid, box: 'bg-[#EEE3D6]',text: 'text-bronze', note: 'orders' },
    { label: 'Cash pending', list: cashDue, box: 'border border-amber-400 bg-amber-50', text: 'text-amber-800', note: 'orders to collect' },
  ]

  return (
    <section className="rounded-3xl border border-stone-200 bg-white p-5 shadow-sm">
      <div className="flex items-baseline justify-between">
        <h2 className="font-semibold">Payments today</h2>
        <p className="text-2xl font-semibold">{money(total)}</p>
      </div>

      <div className="mt-3 flex h-2.5 overflow-hidden rounded-full bg-stone-100">
        <span style={{ width: `${pct(online)}%` }} className="bg-mint-deep" />
        <span style={{ width: `${pct(cashPaid)}%` }} className="bg-bronze" />
        <span style={{ width: `${pct(cashDue)}%` }} className="bg-amber-400" />
      </div>
      <div className="mt-1.5 flex justify-between text-xs text-stone-500">
        <span>{total ? 100 - cashPct : 0}% online</span>
        <span>{total ? cashPct : 0}% cash</span>
      </div>

      <div className="mt-4 grid gap-3 sm:grid-cols-3">
        {tiles.map((t) => (
          <div key={t.label} className={`rounded-2xl p-3 ${t.box}`}>
            <p className={`text-xs ${t.text}`}>● {t.label}</p>
            <p className="mt-1 text-lg font-semibold">{money(sum(t.list))}</p>
            <p className="text-xs text-stone-500">{t.list.length} {t.list.length === 1 ? t.note.replace('orders', 'order') : t.note}</p>
          </div>
        ))}
      </div>
    </section>
  )
}