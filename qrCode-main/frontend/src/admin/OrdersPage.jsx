import { useCallback, useEffect, useState } from 'react'
import { downloadOrdersExcel, getOrders } from '../api/staff'
import { ErrorNote, PageLoader, Spinner, StatusBadge, btn, input } from '../components/ui'
import { useToast } from '../components/Toast'
import { formatDateTime, isoDate, money, placeLabel } from '../lib/format'
import { useAdmin } from './AdminContext'
import OrderDetailModal, { paymentInfo } from './OrderDetailModal'

const STATUSES = ['', 'PAYMENT_PENDING', 'PAID', 'PREPARING', 'READY', 'COMPLETED', 'CANCELLED']
const STATUS_NAMES = { '': 'All statuses', PAYMENT_PENDING: 'Awaiting payment', PAID: 'New', PREPARING: 'Preparing', READY: 'Ready', COMPLETED: 'Completed', CANCELLED: 'Cancelled' }

// All orders with filters, details, receipts and Excel export.
export default function OrdersPage() {
  const { liveVersion } = useAdmin()
  const toast = useToast()
  const today = isoDate(new Date())
  const [filters, setFilters] = useState({ from: today, to: today, status: '', orderType: '' })
  const [orders, setOrders] = useState(null)
  const [error, setError] = useState('')
  const [selected, setSelected] = useState(null)
  const [exporting, setExporting] = useState(false)

  const load = useCallback(() => {
    if (filters.from > filters.to) {
      setError('The start date must be before the end date.')
      return
    }
    getOrders(filters).then((data) => { setOrders(data); setError('') }).catch((e) => setError(e.message))
  }, [filters])

  useEffect(() => { load() }, [load, liveVersion])

  const set = (key) => (e) => setFilters((f) => ({ ...f, [key]: e.target.value }))

  const exportExcel = async () => {
    setExporting(true)
    try {
      await downloadOrdersExcel(filters)
    } catch (e) {
      toast(e.message, 'error')
    } finally {
      setExporting(false)
    }
  }

    const valid = (orders || []).filter((o) => o.status !== 'CANCELLED')
    const paid = valid.filter((o) => paymentInfo(o).paid)
    const revenue = paid.reduce((sum, o) => sum + Number(o.totalAmount), 0)
    const pending = valid.filter((o) => paymentInfo(o).isCash && !paymentInfo(o).paid)
    const pendingAmount = pending.reduce((sum, o) => sum + Number(o.totalAmount), 0)

  return (
    <div>
      <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="font-display text-3xl font-semibold">Order history</h1>
                   <p className="text-stone-500">
                     {paid.length} paid orders · {money(revenue)} in this view
                     {pending.length > 0 && <span className="ml-2 font-medium text-amber-700">· 💵 {money(pendingAmount)} cash pending ({pending.length})</span>}
                   </p>
        </div>
        <button className={btn.primary} onClick={exportExcel} disabled={exporting}>
          {exporting ? <Spinner /> : 'Download Excel'}
        </button>
      </div>

      <div className="mb-5 grid gap-3 rounded-2xl bg-white p-4 shadow-sm sm:grid-cols-4">
        <label className="text-sm">From<input type="date" className={`${input} mt-1`} value={filters.from} max={filters.to} onChange={set('from')} /></label>
        <label className="text-sm">To<input type="date" className={`${input} mt-1`} value={filters.to} min={filters.from} onChange={set('to')} /></label>
        <label className="text-sm">Status
          <select className={`${input} mt-1`} value={filters.status} onChange={set('status')}>
            {STATUSES.map((s) => <option key={s} value={s}>{STATUS_NAMES[s]}</option>)}
          </select>
        </label>
        <label className="text-sm">Type
          <select className={`${input} mt-1`} value={filters.orderType} onChange={set('orderType')}>
            <option value="">Table and counter</option>
            <option value="TABLE">Table</option>
            <option value="WALK_IN">Counter / walk-in</option>
          </select>
        </label>
      </div>

      <ErrorNote>{error}</ErrorNote>
      {!orders && !error && <PageLoader label="Loading orders" />}

      {orders && (
        <div className="overflow-x-auto rounded-2xl bg-white shadow-sm">
          <table className="w-full min-w-[760px] text-sm">
            <thead className="border-b border-stone-200 text-left text-stone-500">
              <tr>
                <th className="px-4 py-3 font-medium">Order</th>
                <th className="px-4 py-3 font-medium">Time</th>
                <th className="px-4 py-3 font-medium">Where</th>
                <th className="px-4 py-3 font-medium">Customer</th>
                <th className="px-4 py-3 font-medium">Total</th>
                <th className="px-4 py-3 font-medium">Payment</th>
                <th className="px-4 py-3 font-medium">Status</th>
              </tr>
            </thead>
            <tbody>
              {orders.length === 0 && (
                <tr><td colSpan="7" className="px-4 py-10 text-center text-stone-500">No orders for these filters.</td></tr>
              )}
              {orders.map((order) => (
                <tr key={order.id} onClick={() => setSelected(order)} className="cursor-pointer border-b border-stone-100 hover:bg-stone-50">
                  <td className="px-4 py-3 font-semibold">{order.orderNumber}</td>
                  <td className="px-4 py-3">{formatDateTime(order.createdAt)}</td>
                  <td className="px-4 py-3">{placeLabel(order.orderType, order.tableNumber)}{order.placedBy && <span className="block text-xs text-stone-500">🧑‍💼 Counter order</span>}</td>
                  <td className="px-4 py-3">{order.nickname}<span className="block text-xs text-stone-500">{order.mobileNumber}</span></td>
                  <td className="px-4 py-3">{money(order.totalAmount)}</td>
                                   <td className="px-4 py-3">
                                     <span className={`whitespace-nowrap rounded-full px-2.5 py-1 text-xs font-semibold ${
                                       paymentInfo(order).paid ? 'bg-[#E1F5EE] text-mint-deep' : 'bg-amber-50 text-amber-800'
                                     }`}>
                                       {order.status === 'CANCELLED' ? '—' : paymentInfo(order).label}
                                     </span>
                                   </td>
                  <td className="px-4 py-3"><StatusBadge status={order.status} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {selected && (
        <OrderDetailModal order={selected} onClose={() => setSelected(null)} onChanged={(updated) => { setSelected(updated); load() }} />
      )}
    </div>
  )
}
