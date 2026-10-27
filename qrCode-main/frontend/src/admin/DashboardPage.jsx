import { useEffect, useState } from 'react'
import { getDailySales, getDietarySales, getOrderTypeSales, getSalesSummary, getTopItems } from '../api/staff'
import { ErrorNote, PageLoader } from '../components/ui'
import { isoDate, money } from '../lib/format'
import { useAdmin } from './AdminContext'
import PaymentsTodayCard from './PaymentsTodayCard'
import OpenClosedPill from './OpenClosedPill'
const RANGES = [
  { days: 7, label: '7 days' },
  { days: 30, label: '30 days' },
  { days: 90, label: '90 days' },
]

// Business overview. Revenue = successful Razorpay payments.
export default function DashboardPage() {
  const { liveVersion } = useAdmin()
  const [days, setDays] = useState(7)
  const [data, setData] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    const to = isoDate(new Date())
    const from = isoDate(new Date(Date.now() - (days - 1) * 86400000))
    Promise.all([
      getSalesSummary(),
      getDailySales(from, to),
      getTopItems(from, to),
      getDietarySales(from, to),
      getOrderTypeSales(from, to),
    ])
      .then(([summary, daily, top, dietary, types]) => {
        setData({ summary, daily, top, dietary, types })
        setError('')
      })
      .catch((e) => setError(e.message))
  }, [days, liveVersion])

  if (error) return <ErrorNote>{error}</ErrorNote>
  if (!data) return <PageLoader label="Loading dashboard" />

  const { summary, daily, top, dietary, types } = data
  const rangeRevenue = daily.reduce((s, d) => s + Number(d.revenue || 0), 0)

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="font-display text-3xl font-semibold">Dashboard</h1>
          <p className="text-stone-500">Today so far, and sales for the selected period.</p>
        </div>

                <div className="flex flex-wrap items-center gap-2">
                  <OpenClosedPill />
                  <div className="inline-flex rounded-xl bg-white p-1 shadow-sm">
                    {/* ...RANGES buttons waise hi... */}
                  </div>
                </div>

        <div className="inline-flex rounded-xl bg-white p-1 shadow-sm">


          {RANGES.map((r) => (
            <button
              key={r.days}
              onClick={() => setDays(r.days)}
              className={`rounded-lg px-3.5 py-1.5 text-sm font-medium ${days === r.days ? 'bg-espresso text-mint' : 'text-stone-600'}`}
            >
              {r.label}
            </button>
          ))}
        </div>
      </div>
       <PaymentsTodayCard />
      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <Stat label="Today's revenue" value={money(summary.todayRevenue)} strong />
        <Stat label="Today's orders" value={summary.todayOrders} note={`${summary.todayCompletedOrders} completed`} />
        <Stat label="Total revenue" value={money(summary.totalRevenue)} />
        <Stat label="Paid orders (all time)" value={summary.paidOrders} note={`${summary.completedOrders} completed`} />
      </section>

      <section className="rounded-3xl bg-white p-5 shadow-sm">
        <div className="mb-4 flex items-baseline justify-between">
          <h2 className="font-semibold">Daily sales</h2>
          <span className="text-sm text-stone-500">{money(rangeRevenue)} in {days} days</span>
        </div>
        <DailyChart daily={daily} />
      </section>

      <div className="grid gap-6 lg:grid-cols-3">
        <section className="rounded-3xl bg-white p-5 shadow-sm lg:col-span-1">
          <h2 className="mb-4 font-semibold">Top-selling items</h2>
          {top.length === 0 ? (
            <p className="text-sm text-stone-500">No sales in this period yet.</p>
          ) : (
            <ol className="space-y-2.5 text-sm">
              {top.slice(0, 8).map((item, i) => (
                <li key={item.itemName} className="flex items-center gap-3">
                  <span className="w-5 text-stone-400">{i + 1}</span>
                  <span className="flex-1 truncate">{item.itemName}</span>
                  <span className="text-stone-500">{item.quantitySold} sold</span>
                </li>
              ))}
            </ol>
          )}
        </section>

        <SplitCard
          title="Veg vs non-veg"
          rows={dietary.map((d) => ({ label: d.dietaryType === 'VEG' ? 'Veg' : 'Non-veg', value: Number(d.revenue), note: `${d.quantitySold} items` }))}
          colors={['bg-veg', 'bg-nonveg']}
        />
        <SplitCard
          title="Table vs counter"
          rows={types.map((t) => ({ label: t.orderType === 'TABLE' ? 'Table' : 'Counter / walk-in', value: Number(t.revenue), note: `${t.orders} orders` }))}
          colors={['bg-espresso', 'bg-bronze']}
        />
      </div>
    </div>
  )
}

function Stat({ label, value, note, strong }) {
  return (
    <div className={`rounded-3xl p-5 shadow-sm ${strong ? 'bg-espresso text-paper' : 'bg-white'}`}>
      <p className={`text-sm ${strong ? 'text-paper/70' : 'text-stone-500'}`}>{label}</p>
      <p className="mt-1 font-display text-3xl font-semibold">{value}</p>
      {note && <p className={`mt-1 text-sm ${strong ? 'text-paper/60' : 'text-stone-500'}`}>{note}</p>}
    </div>
  )
}

function DailyChart({ daily }) {
  const max = Math.max(1, ...daily.map((d) => Number(d.revenue || 0)))
  const showEvery = daily.length > 14 ? Math.ceil(daily.length / 10) : 1
  return (
    <div className="flex h-56 items-end gap-1 overflow-x-auto" role="img" aria-label="Daily revenue chart">
      {daily.map((d, i) => {
        const value = Number(d.revenue || 0)
        const date = new Date(d.date)
        return (
          <div key={d.date} className="flex h-full min-w-[14px] flex-1 flex-col items-center justify-end gap-1" title={`${d.date}: ${money(value)} · ${d.orders} orders`}>
            <div className="w-full rounded-t-md bg-mint" style={{ height: `${Math.max(2, (value / max) * 88)}%` }} />
            <span className="h-4 text-[10px] text-stone-400">
              {i % showEvery === 0 ? date.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' }) : ''}
            </span>
          </div>
        )
      })}
    </div>
  )
}

function SplitCard({ title, rows, colors }) {
  const total = rows.reduce((s, r) => s + r.value, 0)
  return (
    <section className="rounded-3xl bg-white p-5 shadow-sm">
      <h2 className="mb-4 font-semibold">{title}</h2>
      {total === 0 ? (
        <p className="text-sm text-stone-500">No sales in this period yet.</p>
      ) : (
        <>
          <div className="flex h-3 overflow-hidden rounded-full bg-stone-100">
            {rows.map((r, i) => (
              <div key={r.label} className={colors[i % colors.length]} style={{ width: `${(r.value / total) * 100}%` }} />
            ))}
          </div>
          <ul className="mt-4 space-y-2 text-sm">
            {rows.map((r, i) => (
              <li key={r.label} className="flex items-center gap-2">
                <span className={`h-2.5 w-2.5 rounded-full ${colors[i % colors.length]}`} />
                <span className="flex-1">{r.label}</span>
                <span className="text-stone-500">{r.note}</span>
                <span className="w-24 text-right font-medium">{money(r.value)}</span>
              </li>
            ))}
          </ul>
        </>
      )}
    </section>
  )
}
