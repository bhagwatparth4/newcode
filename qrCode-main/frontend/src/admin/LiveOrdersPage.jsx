import { useCallback, useEffect, useState } from 'react'
import { ackCancelAdmin,cancelCashOrder, getActiveOrders, markCashPaid, setAdminOrderStatus } from '../api/staff'
import { ErrorNote, PageLoader, btn } from '../components/ui'
import { useToast } from '../components/Toast'
import { minutesAgo, money, placeLabel } from '../lib/format'
import { adminNextAction, useAdmin } from './AdminContext'
import OrderDetailModal, { paymentInfo } from './OrderDetailModal'
import CancelledOrderCard from '../components/CancelledOrderCard'
import { printBill, printKot } from '../lib/print'
const COLUMNS = [
  { status: 'PAID', title: 'New' },
  { status: 'PREPARING', title: 'Preparing' },
  { status: 'READY', title: 'Ready' },
]

// Live board of paid orders. Refreshes instantly on every realtime event.
export default function LiveOrdersPage() {
  const { hasKitchen, liveVersion } = useAdmin()
  const toast = useToast()
  const [orders, setOrders] = useState(null)
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState(null)
  const [selected, setSelected] = useState(null)

  const load = useCallback(() => {
    getActiveOrders().then((data) => { setOrders(data); setError('') }).catch((e) => setError(e.message))
  }, [])

  useEffect(() => { load() }, [load, liveVersion])

  const advance = async (order, to) => {
    setBusyId(order.id)
    try {
      await setAdminOrderStatus(order.id, to)
      load()
    } catch (e) {
      toast(e.message, 'error')
      load()
    } finally {
      setBusyId(null)
    }
  }

    const run = async (order, action, okMessage) => {
      setBusyId(order.id)
      try {
        await action()
        toast(okMessage)
        load()
      } catch (e) {
        toast(e.message, 'error')
        load()
      } finally {
        setBusyId(null)
      }
    }

    const markPaid = (order) => {
      if (!window.confirm(`Collected ${money(order.totalAmount)} cash for ${order.orderNumber}?`)) return
      run(order, () => markCashPaid(order.id), 'Marked as paid')
    }

    const cancel = (order) => {
      const msg = order.status === 'PREPARING'
        ? 'The kitchen is already preparing this order. Cancel anyway?'
        : 'Cancel this unpaid cash order?'
      if (!window.confirm(msg)) return
      run(order, () => cancelCashOrder(order.id), 'Order cancelled')
    }

  if (!orders && !error) return <PageLoader label="Loading live orders" />

  return (
    <div>
      <div className="mb-6">
        <h1 className="font-display text-3xl font-semibold">Live orders</h1>
        <p className="text-stone-500">
          {hasKitchen ? 'The kitchen prepares orders. Complete them here once they are ready.' : 'Paid orders, oldest first.'}
        </p>
      </div>
      <ErrorNote>{error}</ErrorNote>



           <div className="grid gap-3 md:grid-cols-3 lg:gap-5">
        {COLUMNS.map((column) => {
                    const list = (orders || []).filter((o) => o.status === column.status || (column.status === 'PAID' && o.status === 'CANCELLED'))
          return (
            <section key={column.status} className="rounded-3xl bg-stone-100/70 p-3">
              <h2 className="flex items-center justify-between px-2 pb-3 pt-1 font-semibold">
                {column.title}
                <span className="rounded-full bg-white px-2.5 py-0.5 text-sm">{list.length}</span>
              </h2>
              <div className="space-y-3">
                {list.length === 0 && <p className="px-2 pb-3 text-sm text-stone-500">No orders here.</p>}
                {list.map((order) => {
                                  if (order.status === 'CANCELLED') {
                                    return <CancelledOrderCard key={order.id} order={order} busy={busyId === order.id} onOk={() => run(order, () => ackCancelAdmin(order.id), 'Removed')} />
                                  }
                  const next = adminNextAction(order.status, hasKitchen)
                  const cashDue = order.paymentMethod === 'CASH' && order.orderPaymentStatus !== 'PAID'
                  return (
                      <article key={order.id} className={`rounded-2xl bg-white p-3 shadow-sm ${order.cookingNote ? 'ring-2 ring-amber-400' : ''}`}>
                      <button className="w-full text-left" onClick={() => setSelected(order)}>
                        <div className="flex items-start justify-between gap-2">
                          <div>
                            <p className="font-semibold">{order.orderNumber}</p>
                            <p className="text-sm text-stone-500">{order.nickname} · {minutesAgo(order.createdAt)}</p>
                          </div>
                          <span className="rounded-lg bg-espresso px-2.5 py-1 text-sm font-semibold text-mint">
                            {placeLabel(order.orderType, order.tableNumber)}
                          </span>
                        </div>
                        <ul className="mt-3 space-y-0.5 text-sm">
                          {order.items.map((item, i) => (
                                                        <li key={i}>
                                                          <span className="font-semibold">{item.quantity} ×</span> {item.itemName}
                                                          {item.addonsText && <span className="block pl-5 text-xs font-semibold text-amber-800">+ {item.addonsText}</span>}
                                                        </li>
                          ))}
                        </ul>
                                                {order.cookingNote && (
                                                  <p className="mt-2 rounded-lg bg-amber-100 px-2.5 py-1.5 text-sm font-semibold text-amber-900">📝 {order.cookingNote}</p>
                                                )}

                        <p className="mt-2 text-sm text-stone-500">{money(order.totalAmount)}</p>
                                                {order.paymentMethod === 'CASH' && (
                                                  <span className={`mt-2 inline-block rounded-lg px-2.5 py-1 text-xs font-bold ${cashDue ? 'bg-orange-100 text-orange-800' : 'bg-green-100 text-green-800'}`}>
                                                                                {paymentInfo(order).label}
                                                  </span>
                                                )}

                                                                        {order.placedBy && (
                                                                          <span className="ml-1 mt-2 inline-block rounded-lg bg-stone-100 px-2.5 py-1 text-xs font-bold text-stone-600">🧑‍💼 Counter order</span>
                                                                        )}
                      </button>
                    {next && !(cashDue && next.to === 'COMPLETED') && (
                        <button
                          className={`${btn.primary} mt-2 w-full py-2 text-sm`}
                          disabled={busyId === order.id}
                          onClick={() => advance(order, next.to)}
                        >
                          {next.label}
                        </button>
                      )}
                                                                 {cashDue && (
                                                                   <button className={`${btn.mint} mt-2 w-full py-2 text-sm`} disabled={busyId === order.id} onClick={() => markPaid(order)}>
                                                                     Mark paid · {money(order.totalAmount)}
                                                                   </button>
                                                                 )}
                                                                 <div className="mt-2 flex gap-1.5">
                                                                   <button onClick={() => printKot(order)} className="flex-1 rounded-lg border border-stone-200 py-1.5 text-xs font-medium hover:bg-stone-50">🖨️ KOT</button>
                                                                   <button onClick={() => printBill(order)} className="flex-1 rounded-lg border border-stone-200 py-1.5 text-xs font-medium hover:bg-stone-50">🧾 Bill</button>
                                                                   {cashDue && (
                                                                     <button onClick={() => cancel(order)} disabled={busyId === order.id}
                                                                       className="rounded-lg border border-red-200 px-2.5 py-1.5 text-xs font-semibold text-red-700 hover:bg-red-50">✕ Cancel</button>
                                                                   )}
                                                                 </div>
                    </article>
                  )
                })}
              </div>
            </section>
          )
        })}
      </div>

      {selected && (
        <OrderDetailModal
          order={selected}
          onClose={() => setSelected(null)}
          onChanged={(updated) => { setSelected(updated); load() }}
        />
      )}
    </div>
  )
}
