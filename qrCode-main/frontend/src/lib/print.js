import { BRAND } from '../config/brand'
import { formatTime, placeLabel } from './format'
import { toReceipt } from './receipt'

const esc = (s) => String(s ?? '').replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]))
const amt = (n) => Number(n || 0).toFixed(2)

// Prints HTML on an 80mm thermal printer through a hidden iframe
export function printHtml(html) {
  const frame = document.createElement('iframe')
  frame.style.cssText = 'position:fixed;right:0;bottom:0;width:0;height:0;border:0'
  document.body.appendChild(frame)
  const doc = frame.contentWindow.document
  doc.open()
  doc.write(`<!doctype html><html><head><style>
    @page { size: 80mm auto; margin: 0 }
    body { width: 72mm; margin: 0 auto; padding: 4mm 0; font: 12px/1.4 'Courier New', monospace; color: #000 }
    h1 { font-size: 16px; text-align: center; margin: 0 0 4px }
    .c { text-align: center } .b { font-weight: 700 } .big { font-size: 17px; font-weight: 700 }
    .r { display: flex; justify-content: space-between; gap: 6px }
    hr { border: 0; border-top: 1px dashed #000; margin: 6px 0 }
  </style></head><body>${html}</body></html>`)
  doc.close()
  setTimeout(() => {
    frame.contentWindow.focus()
    frame.contentWindow.print()
    setTimeout(() => frame.remove(), 1000)
  }, 200)
}

// Kitchen Order Ticket: big text, no prices
export function printKot(order) {
  const items = (order.items || [])
        .map((i) => `<div class="big">${i.quantity} x ${esc(i.itemName)}</div>${i.addonsText ? `<div class="b">&nbsp;&nbsp;+ ${esc(i.addonsText)}</div>` : ''}`)
    .join('')
  printHtml(`
    <h1>KOT</h1>
    <div class="c big">${esc(placeLabel(order.orderType, order.tableNumber))}</div>
    <div class="c">${esc(order.orderNumber)} · ${esc(order.nickname)}</div>
    <div class="c">${esc(formatTime(order.createdAt))}</div>
    <hr>${items}
    ${order.cookingNote ? `<hr><div class="b">NOTE: ${esc(order.cookingNote)}</div>` : ''}
    <hr>`)
}

// Customer bill (same data as the on-screen receipt)
export function printBill(order) {
  const r = toReceipt(order, { forAdmin: true })
  const items = r.items
        .map((i) => `<div>${esc(i.name)}</div>`
          + `<div class="r"><span>&nbsp;&nbsp;${i.quantity} x ${amt(i.unitPrice)}</span><span>${amt(i.quantity * i.unitPrice)}</span></div>`
          + i.addons.map((a) => `<div class="r"><span>&nbsp;&nbsp;+ ${esc(a.name)}</span><span>${amt(i.quantity * a.price)}</span></div>`).join(''))
   const mode = { UPI: 'UPI', CARD: 'Card' }[order.counterPaymentMode] || 'Cash'
   const payment = r.paid ? (r.cash ? `PAID (${mode})` : 'PAID (Online)') : (r.cash ? 'CASH - TO PAY' : 'PENDING')
  printHtml(`
    <h1>${esc(BRAND.name)}</h1>
    ${BRAND.address ? `<div class="c">${esc(BRAND.address)}</div>` : ''}
    ${BRAND.phone ? `<div class="c">Ph: ${esc(BRAND.phone)}</div>` : ''}
    <hr>
    <div class="r"><span>Order</span><span>${esc(r.orderNumber)}</span></div>
    <div class="r"><span>Date</span><span>${esc(r.date)} ${esc(r.time)}</span></div>
    <div class="r"><span>Place</span><span>${esc(r.place)}</span></div>
    <div class="r"><span>Name</span><span>${esc(r.nickname || '-')}</span></div>
    <hr>${items}<hr>
    <div class="r"><span>Subtotal</span><span>${amt(r.subtotal)}</span></div>
    ${r.discount > 0 ? `<div class="r"><span>Discount${r.couponCode ? ` (${esc(r.couponCode)})` : ''}</span><span>-${amt(r.discount)}</span></div>` : ''}
    <div class="r big"><span>TOTAL</span><span>Rs. ${amt(r.total)}</span></div>
    <hr>
    <div class="r"><span>Payment</span><span>${payment}</span></div>
    <hr>
    <div class="c">Thank you! Visit again.</div>`)
}