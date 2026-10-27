import { minutesAgo, money, placeLabel } from '../lib/format'

// Same layout as a normal order card, in red, until staff taps OK
export default function CancelledOrderCard({ order, onOk, busy }) {
  return (
    <article className="flex flex-col rounded-2xl border-2 border-red-500 bg-red-50 p-4 text-red-900">
      <div className="flex items-start justify-between gap-2">
        <div>
          <p className="font-semibold line-through">{order.orderNumber}</p>
          <p className="text-sm text-red-700">{order.nickname} · {minutesAgo(order.createdAt)}</p>
        </div>
        <span className="rounded-lg bg-red-700 px-2.5 py-1 text-sm font-semibold text-red-50">
          {placeLabel(order.orderType, order.tableNumber)}
        </span>
      </div>
      <p className="mt-3 rounded-lg bg-red-500 px-3 py-1.5 text-center font-semibold text-white">✕ Cancelled – don't make</p>
      <ul className="mt-3 space-y-0.5 text-sm line-through">
        {order.items.map((item, i) => (
          <li key={i}><span className="font-semibold">{item.quantity} ×</span> {item.itemName}{item.addonsText && <span className="block pl-5 text-xs">+ {item.addonsText}</span>}</li>
        ))}
      </ul>
      <p className="mt-2 text-sm text-red-700">{money(order.totalAmount)} · cash, unpaid</p>
      <button className="mt-3 w-full rounded-xl bg-red-700 py-3 font-semibold text-white disabled:opacity-50" disabled={busy} onClick={onOk}>
        OK, got it
      </button>
    </article>
  )
}