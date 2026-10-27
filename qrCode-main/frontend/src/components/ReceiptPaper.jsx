import { BRAND } from '../config/brand'
import { money } from '../lib/format'

/*
 * On-screen receipt. Same data as the downloaded PDF (see lib/receipt.js).
 */
function Row({ label, value, strong = false }) {
  return (
    <div className={`flex justify-between gap-3 ${strong ? 'text-[15px] font-semibold' : ''}`}>
      <span>{label}</span>
      <span className="text-right">{value}</span>
    </div>
  )
}

const Rule = () => <div className="my-2 border-t border-dashed border-espresso/40" />

export default function ReceiptPaper({ receipt }) {
  const r = receipt
  return (
    <div className="w-full max-w-[320px] drop-shadow-[0_10px_18px_rgba(43,33,28,0.18)]">
      <div className="bg-paper px-5 pb-4 pt-5 font-mono text-[12px] leading-relaxed text-espresso">
        <div className="flex flex-col items-center text-center">
          <img src={BRAND.receiptLogo} alt="" className="h-14 w-14 rounded-full" />
          <div className="mt-2 font-semibold tracking-wide">{BRAND.name}</div>
          {BRAND.tagline && <div className="text-espresso/70">{BRAND.tagline}</div>}
          {BRAND.address && <div className="text-espresso/70">{BRAND.address}</div>}
        </div>
        <Rule />
        <Row label="Order" value={r.orderNumber} />
        <Row label="Date" value={`${r.date} ${r.time}`} />
        <Row label="Place" value={r.place} />
        <Row label="Name" value={r.nickname || '-'} />
        {r.mobile && <Row label="Mobile" value={r.mobile} />}
        <Rule />
        {r.items.map((item, i) => (
          <div key={i} className="mb-1">
            <div>{item.name}</div>
                       <Row label={`  ${item.quantity} × ${money(item.unitPrice)}`} value={money(item.quantity * item.unitPrice)} />
                       {item.addons.map((a) => (
                         <Row key={a.name} label={`  + ${a.name}`} value={money(item.quantity * a.price)} />
                       ))}
          </div>
        ))}
        {r.note && <p>Note: {r.note}</p>}
        <Rule />
        <Row label="Subtotal" value={money(r.subtotal)} />
        {r.discount > 0 && (
          <Row label={`Discount${r.couponCode ? ` (${r.couponCode})` : ''}`} value={`− ${money(r.discount)}`} />
        )}
        <Row label="Total" value={money(r.total)} strong />
        <Rule />
        <Row label="Payment" value={r.paid ? (r.cash ? 'Paid · Cash' : 'Paid · Razorpay') : (r.cash ? 'Cash · To pay' : 'Pending')} />
        {r.paymentId && <Row label="Payment ID" value={<span className="break-all">{r.paymentId}</span>} />}
        {r.reference && <Row label="Ref / UPI RRN" value={r.reference} />}
        {r.prepMinutes ? <Row label="Est. preparation" value={`~${r.prepMinutes} min`} /> : null}
        <Rule />
        <p className="text-center text-espresso/70">Thank you! Visit again.</p>
      </div>
      <div className="receipt-edge" />
    </div>
  )
}
