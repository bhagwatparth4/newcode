import { BRAND } from '../config/brand'
import { formatDate, formatTime, placeLabel } from './format'

/*
 * ONE receipt format used everywhere:
 *   - customer receipt preview + PDF download
 *   - admin "View receipt" + PDF download
 * Only safe, customer-facing data goes on a receipt: no secrets, no tokens,
 * no internal IDs. The customer copy shows a masked mobile number.
 */
export function toReceipt(order, { forAdmin = false } = {}) {
    const items = (order.items || []).map((item) => {
      // "Large|120.00;Extra cheese|40.00" -> [{ name, price }]
      const addons = (item.addonsDetail || '').split(';').filter(Boolean).map((part) => {
        const [name, price] = part.split('|')
        return { name, price: Number(price || 0) }
      })
      const addonsPerUnit = addons.reduce((s, a) => s + a.price, 0)
      return {
        name: !addons.length && item.addonsText ? `${item.itemName} (${item.addonsText})` : item.itemName,
        quantity: Number(item.quantity),
        unitPrice: Number(item.unitPrice) - addonsPerUnit,   // base price only
        addons,
        subtotal: Number(item.subtotal),
      }
    })
  const itemsTotal = items.reduce((sum, i) => sum + i.subtotal, 0)
  const discount = Number(order.discountAmount || 0)

  return {
    orderNumber: order.orderNumber,
    date: formatDate(order.createdAt),
    time: formatTime(order.createdAt),
    place: placeLabel(order.orderType, order.tableNumber),
    nickname: order.nickname,
    mobile: forAdmin ? order.mobileNumber : order.maskedMobileNumber,
    items,
    subtotal: order.subtotalAmount != null ? Number(order.subtotalAmount) : itemsTotal,
    discount,
    couponCode: order.couponCode || '',
    total: Number(order.totalAmount || 0),
    cash: order.paymentMethod === 'CASH',
    paid: order.paymentMethod === 'CASH'
    ? order.orderPaymentStatus === 'PAID'
    : order.paymentStatus === 'SUCCESS',
    paymentId: order.razorpayPaymentId || '',
    reference: order.transactionReference || '',
    prepMinutes: order.estimatedPreparationMinutes,
        note: order.cookingNote || '',
  }
}

const amount = (n) => Number(n || 0).toFixed(2)

/* ------------------------------------------------------------------
 * Tiny PDF writer (no extra library): an 80mm thermal-style receipt
 * using the built-in Courier font plus the greyscale logo JPEG.
 * ------------------------------------------------------------------ */

const PAGE_W = 226.77 // 80 mm in PDF points
const MARGIN = 14
const FONT_SIZE = 8.5
const CHAR_W = FONT_SIZE * 0.6 // Courier is monospaced
const COLS = Math.floor((PAGE_W - MARGIN * 2) / CHAR_W)
const LEADING = 11.5

function pdfSafe(text) {
  return String(text ?? '')
    .replace(/₹/g, 'Rs.')
    .replace(/[\u2013\u2014]/g, '-')
    .replace(/[\u2018\u2019]/g, "'")
    .replace(/[\u201c\u201d]/g, '"')
    .replace(/[^\x20-\xff]/g, '?')
}

function escapePdf(text) {
  return text.replace(/\\/g, '\\\\').replace(/\(/g, '\\(').replace(/\)/g, '\\)')
}

function wrap(text, width) {
  const words = pdfSafe(text).split(' ')
  const lines = []
  let line = ''
  for (const word of words) {
    if ((line + ' ' + word).trim().length > width) {
      if (line) lines.push(line)
      line = word.length > width ? word.slice(0, width) : word
    } else {
      line = (line + ' ' + word).trim()
    }
  }
  if (line) lines.push(line)
  return lines
}

function pair(left, right) {
  const l = pdfSafe(left)
  const r = pdfSafe(right)
  const space = Math.max(1, COLS - l.length - r.length)
  return (l + ' '.repeat(space) + r).slice(0, Math.max(COLS, l.length + r.length + 1))
}

function buildLines(r) {
  const lines = []
  const add = (text, opts = {}) => lines.push({ text: pdfSafe(text), ...opts })
  const rule = () => add('-'.repeat(COLS))

  add(BRAND.name, { bold: true, align: 'center', size: 11 })
  if (BRAND.tagline) add(BRAND.tagline, { align: 'center' })
  if (BRAND.address) wrap(BRAND.address, COLS).forEach((l) => add(l, { align: 'center' }))
  if (BRAND.phone) add(`Ph: ${BRAND.phone}`, { align: 'center' })
  rule()
  add('RECEIPT', { bold: true, align: 'center' })
  add(pair('Order', r.orderNumber))
  add(pair('Date', `${r.date} ${r.time}`))
  add(pair('Place', r.place))
  add(pair('Name', r.nickname || '-'))
  if (r.mobile) add(pair('Mobile', r.mobile))
  rule()
  add(pair('Item', 'Amount'), { bold: true })
  for (const item of r.items) {
    wrap(item.name, COLS).forEach((l) => add(l))
        add(pair(`  ${item.quantity} x ${amount(item.unitPrice)}`, amount(item.quantity * item.unitPrice)))
        item.addons.forEach((a) => add(pair(`  + ${a.name}`, amount(item.quantity * a.price))))
  }
  if (r.note) wrap(`Note: ${r.note}`, COLS).forEach((l) => add(l))
  rule()
  add(pair('Subtotal', amount(r.subtotal)))
  if (r.discount > 0) add(pair(`Discount${r.couponCode ? ` (${r.couponCode})` : ''}`, `-${amount(r.discount)}`))
  add(pair('TOTAL (Rs.)', amount(r.total)), { bold: true })
  rule()
  add(pair('Payment', r.paid ? (r.cash ? 'PAID (Cash)' : 'PAID (Razorpay)') : (r.cash ? 'CASH - TO PAY' : 'PENDING')))
  if (r.paymentId) add(pair('Payment ID', r.paymentId))
  if (r.reference) add(pair('Ref / UPI RRN', r.reference))
  if (r.prepMinutes) add(pair('Est. preparation', `~${r.prepMinutes} min`))
  rule()
  add('Thank you! Visit again.', { align: 'center' })
  add('Preparation time is an estimate.', { align: 'center' })
  return lines
}

function jpegSize(bytes) {
  // Walk JPEG markers until a Start-Of-Frame marker (holds width/height)
  let i = 2
  while (i < bytes.length) {
    if (bytes[i] !== 0xff) return null
    const marker = bytes[i + 1]
    const length = (bytes[i + 2] << 8) + bytes[i + 3]
    if (marker >= 0xc0 && marker <= 0xcf && ![0xc4, 0xc8, 0xcc].includes(marker)) {
      return { height: (bytes[i + 5] << 8) + bytes[i + 6], width: (bytes[i + 7] << 8) + bytes[i + 8] }
    }
    i += 2 + length
  }
  return null
}

async function loadLogo() {
  try {
    const response = await fetch(BRAND.receiptLogo)
    if (!response.ok) return null
    const bytes = new Uint8Array(await response.arrayBuffer())
    const size = jpegSize(bytes)
    return size ? { bytes, ...size } : null
  } catch {
    return null
  }
}

const latin1 = (s) => Uint8Array.from(s, (c) => c.charCodeAt(0) & 0xff)

export async function buildReceiptPdf(receipt) {
  const lines = buildLines(receipt)
  const logo = await loadLogo()
  const logoSize = logo ? 64 : 0

  const height = MARGIN + logoSize + (logo ? 10 : 0) + lines.length * LEADING + MARGIN + 6

  // Page content: draw from the top down (PDF y=0 is the bottom)
  let y = height - MARGIN
  let content = ''
  if (logo) {
    const x = (PAGE_W - logoSize) / 2
    content += `q ${logoSize} 0 0 ${logoSize} ${x.toFixed(2)} ${(y - logoSize).toFixed(2)} cm /Logo Do Q\n`
    y -= logoSize + 10
  }
  for (const line of lines) {
    const size = line.size || FONT_SIZE
    const width = line.text.length * size * 0.6
    let x = MARGIN
    if (line.align === 'center') x = (PAGE_W - width) / 2
    if (line.align === 'right') x = PAGE_W - MARGIN - width
    y -= LEADING
    content += `BT /${line.bold ? 'F2' : 'F1'} ${size} Tf ${x.toFixed(2)} ${y.toFixed(2)} Td (${escapePdf(line.text)}) Tj ET\n`
  }

  const objects = []
  objects.push('<< /Type /Catalog /Pages 2 0 R >>')
  objects.push('<< /Type /Pages /Kids [3 0 R] /Count 1 >>')
  objects.push(
    `<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${PAGE_W} ${height.toFixed(2)}] ` +
      `/Resources << /Font << /F1 4 0 R /F2 5 0 R >>${logo ? ' /XObject << /Logo 7 0 R >>' : ''} >> /Contents 6 0 R >>`,
  )
  objects.push('<< /Type /Font /Subtype /Type1 /BaseFont /Courier /Encoding /WinAnsiEncoding >>')
  objects.push('<< /Type /Font /Subtype /Type1 /BaseFont /Courier-Bold /Encoding /WinAnsiEncoding >>')
  objects.push({ dict: `<< /Length ${content.length} >>`, stream: latin1(content) })
  if (logo) {
    objects.push({
      dict:
        `<< /Type /XObject /Subtype /Image /Width ${logo.width} /Height ${logo.height} ` +
        `/ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${logo.bytes.length} >>`,
      stream: logo.bytes,
    })
  }

  const parts = []
  let offset = 0
  const push = (chunk) => {
    const bytes = typeof chunk === 'string' ? latin1(chunk) : chunk
    parts.push(bytes)
    offset += bytes.length
  }
  const xref = []

  push('%PDF-1.4\n%\xe2\xe3\xcf\xd3\n')
  objects.forEach((obj, index) => {
    xref.push(offset)
    push(`${index + 1} 0 obj\n`)
    if (typeof obj === 'string') {
      push(obj + '\n')
    } else {
      push(obj.dict + '\nstream\n')
      push(obj.stream)
      push('\nendstream\n')
    }
    push('endobj\n')
  })

  const xrefStart = offset
  push(`xref\n0 ${objects.length + 1}\n0000000000 65535 f \n`)
  xref.forEach((o) => push(`${String(o).padStart(10, '0')} 00000 n \n`))
  push(`trailer\n<< /Size ${objects.length + 1} /Root 1 0 R >>\nstartxref\n${xrefStart}\n%%EOF\n`)

  return new Blob(parts, { type: 'application/pdf' })
}

export async function downloadReceiptPdf(receipt) {
  const blob = await buildReceiptPdf(receipt)
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${BRAND.name.replace(/\s+/g, '')}_Receipt_${receipt.orderNumber}.pdf`
  document.body.appendChild(link)
  link.click()
  link.remove()
  setTimeout(() => URL.revokeObjectURL(url), 2000)
}
