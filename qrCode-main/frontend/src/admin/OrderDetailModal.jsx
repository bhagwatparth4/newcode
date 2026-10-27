import { useState } from 'react'
import { ErrorNote, Modal, StatusBadge, btn } from '../components/ui'
import ReceiptPaper from '../components/ReceiptPaper'
import { formatDateTime, money, placeLabel } from '../lib/format'
import { downloadReceiptPdf, toReceipt } from '../lib/receipt'
import { setAdminOrderStatus } from '../api/staff'
import { adminNextAction, useAdmin } from './AdminContext'

// Works for both cash and online orders
export function paymentInfo(order) {
  const isCash = order.paymentMethod === 'CASH'
  const paid = isCash ? order.orderPaymentStatus === 'PAID' : order.paymentStatus === 'SUCCESS'
  // Counter orders keep how the customer actually paid (label only, all treated as cash)
  const mode = { UPI: '📱 UPI', CARD: '💳 Card' }[order.counterPaymentMode] || '💵 Cash'
  const label = isCash
    ? `${mode} · ${paid ? 'Paid' : 'Pending'}`
    : (paid ? '💳 Online · Paid' : `💳 Online · ${order.paymentStatus || 'Not paid'}`)
  return { isCash, paid, label, counter: Boolean(order.placedBy) }
}


// Full order details for Admin: customer, items, payment, actions, receipt.

export default function OrderDetailModal({ order, onClose, onChanged }) {
  const { hasKitchen } = useAdmin()
  const [showReceipt, setShowReceipt] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  if (!order) return null

  const next = adminNextAction(order.status, hasKitchen)
  const receipt = toReceipt(order, { forAdmin: true })
  const pay = paymentInfo(order)
  const advance = async () => {
    setBusy(true)
    setError('')
    try {
      const updated = await setAdminOrderStatus(order.id, next.to)
      onChanged?.(updated)
    } catch (e) {
      setError(e.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal open onClose={onClose} title={`Order ${order.orderNumber}`} wide>
      {showReceipt ? (
        <div className="flex flex-col items-center gap-4">
          <ReceiptPaper receipt={receipt} />
          <div className="flex w-full max-w-[320px] gap-2">
            <button className={`${btn.ghost} flex-1`} onClick={() => setShowReceipt(false)}>Back</button>
            <button className={`${btn.primary} flex-1`} onClick={() => downloadReceiptPdf(receipt)}>Download receipt</button>
          </div>
        </div>
      ) : (
        <div className="space-y-5 text-sm">
          <div className="flex flex-wrap items-center gap-2">
            <StatusBadge status={order.status} />
            <span className="rounded-full bg-stone-100 px-2.5 py-1 text-xs font-semibold">
              {placeLabel(order.orderType, order.tableNumber)}
            </span>
            <span className="text-stone-500">{formatDateTime(order.createdAt)}</span>
          </div>

          <dl className="grid grid-cols-2 gap-x-4 gap-y-2 rounded-2xl bg-stone-50 p-4">
            <dt className="text-stone-500">Nickname</dt><dd className="font-medium">{order.nickname}</dd>
            <dt className="text-stone-500">Mobile</dt><dd className="font-medium">{order.mobileNumber || '-'}</dd>
                       <dt className="text-stone-500">Payment</dt>
                       <dd className={`font-medium ${pay.paid ? 'text-mint-deep' : 'text-amber-700'}`}>{pay.label}</dd>
                                   {order.placedBy && (
                                     <>
                                       <dt className="text-stone-500">Order taken by</dt>
                                       <dd className="break-all font-medium">🧑‍💼 {order.placedBy}</dd>
                                     </>
                                   )}
                       {pay.isCash ? (
                         <>
                           <dt className="text-stone-500">Cash received at</dt><dd className="font-medium">{order.paidAt ? formatDateTime(order.paidAt) : '-'}</dd>
                           <dt className="text-stone-500">Cash taken by</dt><dd className="break-all font-medium">{order.paidBy || '-'}</dd>
                         </>
                       ) : (
                         <>
                           <dt className="text-stone-500">Payment ID</dt><dd className="break-all font-medium">{order.razorpayPaymentId || '-'}</dd>
                           <dt className="text-stone-500">Ref / UPI RRN</dt><dd className="break-all font-medium">{order.transactionReference || '-'}</dd>
                           <dt className="text-stone-500">Razorpay order</dt><dd className="break-all font-medium">{order.razorpayOrderId || '-'}</dd>
                         </>
                       )}
            <dt className="text-stone-500">Est. ready</dt><dd className="font-medium">{formatDateTime(order.estimatedReadyAt)}</dd>
            <dt className="text-stone-500">Last update</dt><dd className="font-medium">{formatDateTime(order.updatedAt)}</dd>
          </dl>

          <table className="w-full">
            <thead className="text-left text-stone-500">
              <tr><th className="py-1 font-medium">Item</th><th className="font-medium">Qty</th><th className="font-medium">Price</th><th className="text-right font-medium">Amount</th></tr>
            </thead>
            <tbody>
              {order.items.map((item, i) => (
                <tr key={i} className="border-t border-stone-100">
                                   <td className="py-1.5">{item.itemName}{item.addonsText && <span className="block text-xs text-stone-500">+ {item.addonsText}</span>}</td>
                  <td>{item.quantity}</td>
                  <td>{money(item.unitPrice)}</td>
                  <td className="text-right">{money(item.subtotal)}</td>
                </tr>
              ))}
            </tbody>
          </table>

                    {order.cookingNote && (
                      <p className="rounded-xl bg-amber-50 px-3 py-2">📝 Cooking request: <strong>{order.cookingNote}</strong></p>
                    )}

          <div className="space-y-1 border-t border-stone-200 pt-3">
            <div className="flex justify-between"><span>Subtotal</span><span>{money(order.subtotalAmount)}</span></div>
            {Number(order.discountAmount) > 0 && (
              <div className="flex justify-between"><span>Discount {order.couponCode ? `(${order.couponCode})` : ''}</span><span>− {money(order.discountAmount)}</span></div>
            )}
            <div className="flex justify-between text-base font-semibold"><span>Total</span><span>{money(order.totalAmount)}</span></div>
          </div>

          <ErrorNote>{error}</ErrorNote>

          <div className="flex flex-wrap gap-2">
                       {pay.paid && (
              <button className={btn.ghost} onClick={() => setShowReceipt(true)}>View receipt</button>
            )}
            {next && (
              <button className={`${btn.primary} ml-auto`} onClick={advance} disabled={busy}>{next.label}</button>
            )}
          </div>
          {hasKitchen && ['PAID', 'PREPARING'].includes(order.status) && (
            <p className="text-xs text-stone-500">The kitchen moves this order to Ready. You can complete it after that.</p>
          )}
        </div>
      )}
    </Modal>
  )
}
