import { useCallback, useEffect, useState } from 'react'
import { getOrdersBetween } from '../api/staff'
import { BRAND } from '../config/brand'
import { ErrorNote, PageLoader, btn, input } from '../components/ui'
import { formatTime, money, placeLabel } from '../lib/format'
import { printHtml } from '../lib/print'
import { useAdmin } from './AdminContext'

const pad = (n) => String(n).padStart(2, '0')
const todayStr = () => { const d = new Date(); return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` }
const sum = (list, key = 'totalAmount') => list.reduce((s, o) => s + Number(o[key] || 0), 0)
const amt = (n) => Number(n || 0).toFixed(2)

function buildReport(orders) {
  const valid = orders.filter((o) => o.status !== 'CANCELLED' && o.status !== 'PAYMENT_PENDING')
  const online = valid.filter((o) => o.paymentMethod !== 'CASH')
  const cashPaid = valid.filter((o) => o.paymentMethod === 'CASH' && o.orderPaymentStatus === 'PAID')
  const cashDue = valid.filter((o) => o.paymentMethod === 'CASH' && o.orderPaymentStatus !== 'PAID')
  const cancelled = orders.filter((o) => o.status === 'CANCELLED')

  const items = new Map()
  for (const o of valid) {
    for (const i of o.items || []) {
      const cur = items.get(i.itemName) || { name: i.itemName, qty: 0, total: 0 }
      cur.qty += Number(i.quantity)
      cur.total += Number(i.subtotal)
      items.set(i.itemName, cur)
    }
  }

   const modeSum = (mode) => sum(cashPaid.filter((o) => (o.counterPaymentMode || 'CASH') === mode))

  return {
    valid, online, cashPaid, cashDue, cancelled,
    total: sum(valid),
    modes: { CASH: modeSum('CASH'), UPI: modeSum('UPI'), CARD: modeSum('CARD') },
    discount: sum(valid, 'discountAmount'),
    table: valid.filter((o) => o.orderType === 'TABLE'),
    counter: valid.filter((o) => o.orderType !== 'TABLE'),
    items: [...items.values()].sort((a, b) => b.qty - a.qty),
  }
}

export default function DayReportPage() {
  const { liveVersion } = useAdmin()
  const [day, setDay] = useState(todayStr)
  const [orders, setOrders] = useState(null)
  const [error, setError] = useState('')

  const load = useCallback(() => {
    setOrders(null)
    getOrdersBetween(`${day}T00:00:00`, `${day}T23:59:59`)
      .then((data) => { setOrders(data); setError('') })
      .catch((e) => setError(e.message))
  }, [day])

  useEffect(() => { load() }, [load, liveVersion])

  if (error) return <ErrorNote>{error}</ErrorNote>
  if (!orders) return <PageLoader label="Building the report" />

  const r = buildReport(orders)
  const avg = r.valid.length ? r.total / r.valid.length : 0

  const print = () => {
    const line = (label, value) => `<div class="r"><span>${label}</span><span>${value}</span></div>`
    printHtml(`
      <h1>${BRAND.name}</h1>
      <div class="c b">DAY-END REPORT</div>
      <div class="c">${day}</div><hr>
      ${line('Orders', r.valid.length)}
      ${line('Avg. order', amt(avg))}
      <hr>
      ${line('Online', amt(sum(r.online)))}
      ${line('Cash collected', amt(sum(r.cashPaid)))}
      ${line('  - Notes', amt(r.modes.CASH))}
      ${line('  - UPI (counter)', amt(r.modes.UPI))}
      ${line('  - Card (counter)', amt(r.modes.CARD))}
      ${line('Cash pending', amt(sum(r.cashDue)))}
      <div class="r big"><span>TOTAL</span><span>Rs. ${amt(r.total)}</span></div>
      <hr>
      ${line('Discounts given', amt(r.discount))}
      ${line(`Cancelled (${r.cancelled.length})`, amt(sum(r.cancelled)))}
      ${line(`Table (${r.table.length})`, amt(sum(r.table)))}
      ${line(`Counter (${r.counter.length})`, amt(sum(r.counter)))}
      <hr><div class="b">Items sold</div>
      ${r.items.map((i) => line(`${i.qty} x ${i.name}`, amt(i.total))).join('')}
      <hr><div class="c">Printed ${new Date().toLocaleString('en-IN')}</div>`)
  }

    const downloadCsv = () => {
      const rows = [
        [`${BRAND.name} - Day-end report`, day],
        [],
        ['SUMMARY'],
        ['Total sales', amt(r.total)],
        ['Orders', r.valid.length],
        ['Average order', amt(avg)],
        ['Online', amt(sum(r.online)), `${r.online.length} orders`],
        ['Cash collected', amt(sum(r.cashPaid)), `${r.cashPaid.length} orders`],
        ['Cash pending', amt(sum(r.cashDue)), `${r.cashDue.length} orders`],
        ['Discounts given', amt(r.discount)],
        ['Cancelled', amt(sum(r.cancelled)), `${r.cancelled.length} orders`],
        ['Table', amt(sum(r.table)), `${r.table.length} orders`],
        ['Counter', amt(sum(r.counter)), `${r.counter.length} orders`],
        [],
        ['ITEMS SOLD'],
        ['Item', 'Quantity', 'Amount'],
        ...r.items.map((i) => [i.name, i.qty, amt(i.total)]),
        [],
        ['ORDERS'],
        ['Order', 'Time', 'Place', 'Name', 'Mobile', 'Items', 'Cooking note', 'Status', 'Payment',
          'Payment status', 'Paid at', 'Cash taken by', 'Online payment ID', 'Subtotal', 'Discount', 'Coupon', 'Total'],
        ...orders.map((o) => [
          o.orderNumber,
          formatTime(o.createdAt),
          placeLabel(o.orderType, o.tableNumber),
          o.nickname,
          o.mobileNumber,
          (o.items || []).map((i) => `${i.quantity} x ${i.itemName}`).join('; '),
          o.cookingNote || '',
          o.status,
          o.paymentMethod || 'ONLINE',
          o.orderPaymentStatus || '',
          o.paidAt ? formatTime(o.paidAt) : '',
          o.paidBy || '',
          o.razorpayPaymentId || '',
          amt(o.subtotalAmount),
          amt(o.discountAmount),
          o.couponCode || '',
          amt(o.totalAmount),
        ]),
      ]
         // A cell starting with = + - @ would run as a formula in Excel, so neutralise it
      const safe = (v) => { const s = String(v ?? ''); return /^[=+\-@\t\r]/.test(s) ? `'${s}` : s }
      const csv = rows.map((row) => row.map((v) => `"${safe(v).replace(/"/g, '""')}"`).join(',')).join('\n')
      const link = document.createElement('a')
      link.href = URL.createObjectURL(new Blob(['\ufeff' + csv], { type: 'text/csv' }))
      link.download = `${BRAND.name.replace(/\s+/g, '')}_Report_${day}.csv`
      link.click()
    }

  const tiles = [
    ['🧾 Orders', r.valid.length, `Avg. ${money(avg)}`],
    ['💳 Online', money(sum(r.online)), `${r.online.length} orders`],
    ['💵 Cash collected', money(sum(r.cashPaid)), `Notes ${money(r.modes.CASH)} · UPI ${money(r.modes.UPI)} · Card ${money(r.modes.CARD)}`],
    ['⏳ Cash pending', money(sum(r.cashDue)), `${r.cashDue.length} orders`],
    ['🏷️ Discounts', money(r.discount), 'coupons used'],
    ['❌ Cancelled', money(sum(r.cancelled)), `${r.cancelled.length} orders`],
  ]

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="font-display text-3xl font-semibold">Day-end report</h1>
          <p className="text-stone-500">Everything for one day, ready to print or download.</p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <input type="date" className={`${input} w-44`} value={day} max={todayStr()} onChange={(e) => setDay(e.target.value)} />
          <button className={btn.ghost} onClick={downloadCsv}>⬇️ Excel (CSV)</button>
          <button className={btn.primary} onClick={print}>🖨️ Print</button>
        </div>
      </div>

      <section className="rounded-3xl bg-espresso p-5 text-paper shadow-sm">
        <p className="text-sm text-paper/70">Total sales · {day}</p>
        <p className="mt-1 font-display text-4xl font-semibold">{money(r.total)}</p>
        {sum(r.cashDue) > 0 && <p className="mt-2 text-sm text-amber-300">⚠️ {money(sum(r.cashDue))} cash still to collect</p>}
      </section>

      <section className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {tiles.map(([label, value, note]) => (
          <div key={label} className="rounded-2xl border border-stone-200 bg-white p-4">
            <p className="text-sm text-stone-500">{label}</p>
            <p className="mt-1 text-2xl font-semibold">{value}</p>
            <p className="text-xs text-stone-500">{note}</p>
          </div>
        ))}
      </section>

      <div className="grid gap-6 lg:grid-cols-2">
        <section className="rounded-3xl bg-white p-5 shadow-sm">
          <h2 className="mb-3 font-semibold">🍽️ Items sold</h2>
          {r.items.length === 0 ? <p className="text-sm text-stone-500">No sales on this day.</p> : (
            <ul className="space-y-2 text-sm">
              {r.items.map((i) => (
                <li key={i.name} className="flex justify-between gap-3">
                  <span><b className="font-semibold">{i.qty} ×</b> {i.name}</span>
                  <span className="text-stone-600">{money(i.total)}</span>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="rounded-3xl bg-white p-5 shadow-sm">
          <h2 className="mb-3 font-semibold">📍 Table vs counter</h2>
          <ul className="space-y-2 text-sm">
            <li className="flex justify-between"><span>🪑 Table · {r.table.length} orders</span><span>{money(sum(r.table))}</span></li>
            <li className="flex justify-between"><span>🛍️ Counter · {r.counter.length} orders</span><span>{money(sum(r.counter))}</span></li>
          </ul>
        </section>
      </div>
    </div>
  )
}