import { useCallback, useRef , useEffect, useMemo, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { BRAND } from '../config/brand'
import { cancelMyOrder, getOrderDetails,sendFeedback } from '../api/customer'
import { ErrorNote, Logo, Modal, PageLoader, btn } from '../components/ui'
import { useToast } from '../components/Toast'
import ThermalPrinter from '../components/ThermalPrinter'
import ReceiptPaper from '../components/ReceiptPaper'
import Hourglass from '../components/Hourglass'
import { money, parseTime, placeLabel } from '../lib/format'
import { downloadReceiptPdf, toReceipt } from '../lib/receipt'
import { useRealtime } from '../lib/useRealtime'
import { useNow } from '../lib/useNow'
import { markReceiptPrinted, wasReceiptPrinted , getMyOrders} from '../lib/customerSession'
import { getPushPublicKey, pushPermission, subscribeToOrderPush } from '../lib/push'
import StatusPopup from '../components/StatusPopup'
import { feedbackReadyAt } from '../lib/feedback'
/*
 * After payment: "Generating bill" printer animation -> receipt -> live tracking.
 * The order state always comes from the backend (fetched on load, on every
 * WebSocket reconnect and on every update), never from the browser alone.
 */
const CASH_PAID_MESSAGE = 'Cash payment received. Thank you!'
const STEPS = ['PAID', 'PREPARING', 'READY', 'COMPLETED']
const STEP_LABEL = { PAID: 'Received', PREPARING: 'Preparing', READY: 'Ready', COMPLETED: 'Done' }

export default function OrderPage() {
  const { token } = useParams()
  const location = useLocation()
  const toast = useToast()

  const [order, setOrder] = useState(null)
  const [error, setError] = useState('')
  const [phase, setPhase] = useState('track') // 'printing' | 'receipt' | 'track'
  const [receiptOpen, setReceiptOpen] = useState(false)
  const [popup, setPopup] = useState(null)
  const justPaid = Boolean(location.state?.justPaid)

    // Latest order, so a payment update (same status) doesn't re-show the status popup
    const orderRef = useRef(null)
    useEffect(() => { orderRef.current = order }, [order])

  const load = useCallback(async () => {
    try {
      const data = await getOrderDetails(token)
      setOrder(data)
      setError('')
    } catch (e) {
      setError(e.status === 404 ? 'We could not find this order.' : e.message)
    }
  }, [token])

  useEffect(() => {
    load()
  }, [load])

  useRealtime({
        topics: [...new Set([token, ...getMyOrders().map((o) => o.token)])].map((t) => `/topic/customer/orders/${t}`),
    onConnect: load,
    onMessage: (_topic, event) => {
                                   const isPaymentUpdate = event?.message === CASH_PAID_MESSAGE
                                   if (event?.orderStatus && event.orderStatus !== 'COMPLETED' && !isPaymentUpdate) {
              setPopup({ ...event, key: Date.now() })
            }
      load()
    },
  })

  // Fallback while the payment is being confirmed (e.g. by the webhook):
  // check a few times, then stop. Normal updates come over WebSocket.
  const pending = order?.status === 'PAYMENT_PENDING'
  useEffect(() => {
    if (!pending) return undefined
    let tries = 0
    const id = setInterval(() => {
      tries += 1
      if (tries > 36) clearInterval(id)
      else load()
    }, 5000)
    return () => clearInterval(id)
  }, [pending, load])

  // Play the printer once, right after a confirmed payment
  useEffect(() => {
    if (order && order.status !== 'PAYMENT_PENDING' && justPaid && !wasReceiptPrinted(token)) {
      markReceiptPrinted(token)
      setPhase('printing')
      setPopup({ orderStatus: 'PAID', key: Date.now() })
    }
  }, [order, justPaid, token])

  const receipt = useMemo(() => (order ? toReceipt(order) : null), [order])

  if (error) {
    return (
      <main className="mx-auto max-w-md p-6 pt-20">
        <ErrorNote>{error}</ErrorNote>
        <Link to="/menu" className={`${btn.ghost} mt-4 w-full`}>Back to menu</Link>
      </main>
    )
  }
  if (!order) return <PageLoader label="Loading your order" />

  if (order.status === 'PAYMENT_PENDING') {
    return (
      <main className="mx-auto flex min-h-dvh max-w-md flex-col items-center justify-center px-6 text-center">
        <Hourglass progress={0.15} size={90} />
        <h1 className="mt-6 font-display text-2xl font-semibold">Confirming your payment</h1>
        <p className="mt-2 text-espresso/70">
          This usually takes a few seconds. Keep this page open. If money was deducted, your order will be confirmed automatically.
        </p>
        <p className="mt-6 text-sm text-espresso/50">Order {order.orderNumber}</p>
        <Link to="/menu" className={`${btn.ghost} mt-8`}>Back to menu</Link>
      </main>
    )
  }

  if (phase === 'printing' || phase === 'receipt') {
    return (
      <main className="mx-auto flex min-h-dvh max-w-md flex-col items-center px-5 py-8">
        <div className="mb-6 text-center">
          <StatusPopup popup={popup} onClose={() => setPopup(null)} />
          <p className="text-sm font-semibold text-mint-deep">Payment successful</p>
          <h1 className="font-display text-2xl font-semibold">Thank you, {order.nickname}</h1>
        </div>
        <ThermalPrinter receipt={receipt} onDone={() => setPhase('receipt')} />
        {phase === 'receipt' && (
          <div className="mt-8 flex w-full max-w-[340px] flex-col gap-3">
            <button className={btn.ghost} onClick={() => downloadReceiptPdf(receipt)}>Download receipt (PDF)</button>
            <button className={btn.primary} onClick={() => setPhase('track')}>Track my order</button>
          </div>
        )}
      </main>
    )
  }

  return (
    <>
          <StatusPopup popup={popup} onClose={() => setPopup(null)} />
      <Tracking order={order} token={token} onChanged={load} onShowReceipt={() => setReceiptOpen(true)} />
      <Modal open={receiptOpen} onClose={() => setReceiptOpen(false)} title="Receipt">
        <div className="flex justify-center">
          <ReceiptPaper receipt={receipt} />
        </div>
        <button className={`${btn.primary} mt-5 w-full`} onClick={() => downloadReceiptPdf(receipt)}>
          Download receipt (PDF)
        </button>
      </Modal>
    </>
  )
}

function Tracking({ order, token, onShowReceipt, onChanged }) {
  const toast = useToast()
  const [cancelling, setCancelling] = useState(false)
  const cashDue = order.paymentMethod === 'CASH' && order.orderPaymentStatus !== 'PAID'

  const cancelOrder = async () => {
    if (!window.confirm('Cancel this order?')) return
    setCancelling(true)
    try {
      await cancelMyOrder(token)
      onChanged()
    } catch (e) {
      toast(e.message, 'error')
      onChanged()
    } finally {
      setCancelling(false)
    }
  }
  const now = useNow(1000)
  const status = order.status
  const stepIndex = STEPS.indexOf(status)
  const isTable = order.orderType === 'TABLE'

  const readyAt = parseTime(order.estimatedReadyAt)
  const minutes = order.estimatedPreparationMinutes || 15
  const startAt = readyAt ? readyAt.getTime() - minutes * 60000 : null
  const cooking = status === 'PAID' || status === 'PREPARING'
  const remainingMs = readyAt ? readyAt.getTime() - now : 0
  const late = cooking && readyAt && remainingMs < 0
  const progress = startAt ? (now - startAt) / (minutes * 60000) : 0

  return (
    <div className="mx-auto min-h-dvh max-w-md px-5 pb-12 pt-5">
      <header className="flex items-center gap-3">
        <Logo size={44} />
        <div className="flex-1">
          <p className="font-display text-lg font-semibold leading-tight">{BRAND.name}</p>
          <p className="text-sm text-espresso/60">Order {order.orderNumber} · {placeLabel(order.orderType, order.tableNumber)}</p>
        </div>
      </header>

            {cashDue && order.status !== 'CANCELLED' && (
              <section className="mt-6 rounded-2xl border-2 border-amber-400 bg-amber-50 p-4 text-center">
                <p className="text-lg font-semibold">💵 Pay {money(order.totalAmount)} at the counter</p>
                <p className="mt-1 text-sm text-espresso/70">Show order {order.orderNumber} to the staff</p>
              </section>
            )}

      <section className="mt-6 flex flex-col items-center rounded-3xl bg-white px-5 py-8 text-center shadow-sm">
        {cooking && (
          <>
            <Hourglass progress={late ? 1 : progress} running={!late} />
            {late ? (
              <>
                <h1 className="mt-5 font-display text-2xl font-semibold">Almost there</h1>
                <p className="mt-2 text-espresso/70">
                  Sorry, your order is taking a little longer than expected. We appreciate your patience.
                </p>
              </>
            ) : (
              <>
                <p className="mt-5 text-sm text-espresso/60">Estimated time left</p>
                <p className="font-display text-5xl font-semibold tabular-nums" aria-live="off">
                  {formatCountdown(remainingMs)}
                </p>
                <p className="mt-2 text-espresso/70">
                  {status === 'PAID' ? 'Order received. The kitchen will start it shortly.' : 'Your order is being prepared.'}
                </p>
                <p className="mt-1 text-xs text-espresso/45">This is an estimate, not a guaranteed time.</p>
              </>
            )}
          </>
        )}

        {status === 'READY' && (
          <>
            <div className="flex h-24 w-24 items-center justify-center rounded-full bg-mint">
              <svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="#2b211c" strokeWidth="2.5"><path d="M5 12.5l4.5 4.5L19 7.5" /></svg>
            </div>
            <h1 className="mt-5 font-display text-3xl font-semibold">Your order is ready</h1>
            <p className="mt-2 text-espresso/70">
              {isTable
                ? `We'll bring it to Table ${order.tableNumber}.`
                : `Please collect it from the counter. Name: ${order.nickname}.`}
            </p>
          </>
        )}

        {status === 'COMPLETED' && (
          <>
            <h1 className="font-display text-3xl font-semibold">Enjoy, {order.nickname}</h1>
            <p className="mt-2 text-espresso/70">Your order is complete. Thank you for visiting us.</p>
          </>
        )}

        {status === 'CANCELLED' && (
          <h1 className="font-display text-2xl font-semibold">This order was cancelled</h1>
        )}
      </section>

      {status === 'COMPLETED' && <FeedbackCard order={order} token={token} />}

      {stepIndex >= 0 && (
        <ol className="mt-6 grid grid-cols-4 gap-2" aria-label="Order progress">
          {STEPS.map((step, i) => (
            <li key={step} className="flex flex-col items-center gap-1.5 text-center">
              <span className={`h-1.5 w-full rounded-full ${i <= stepIndex ? 'bg-espresso' : 'bg-espresso/15'}`} />
              <span className={`text-xs ${i === stepIndex ? 'font-semibold text-espresso' : 'text-espresso/50'}`}>
                {STEP_LABEL[step]}
              </span>
            </li>
          ))}
        </ol>
      )}

           {cooking && <PushButton order={order} token={token} />}

      <section className="mt-6 rounded-2xl bg-white p-4 shadow-sm">
        <ul className="space-y-1.5 text-sm">
          {order.items.map((item, i) => (
            <li key={i} className="flex justify-between gap-3">
                <span>{item.quantity} × {item.itemName}{item.addonsText && <span className="block text-xs text-espresso/60">{item.addonsText}</span>}</span>
              <span>{money(item.subtotal)}</span>
            </li>
          ))}
        </ul>
        <div className="mt-3 flex justify-between border-t border-espresso/10 pt-3 font-semibold">
          <span>{cashDue ? 'To pay at counter' : order.paymentMethod === 'CASH' ? 'Paid · Cash' : 'Paid'}</span>
          <span>{money(order.totalAmount)}</span>
        </div>
      </section>

            <div className="mt-5 grid grid-cols-2 gap-3">
              <button className={btn.ghost} onClick={onShowReceipt} disabled={cashDue}>
                {cashDue ? 'Receipt after payment' : 'View receipt'}
              </button>
              <Link to="/menu" className={btn.ghost}>Order more</Link>
            </div>
            {cashDue && order.status === 'PAID' && (
              <button className="mt-4 w-full text-sm font-semibold text-red-700" onClick={cancelOrder} disabled={cancelling}>
                {cancelling ? 'Cancelling…' : 'Cancel order'}
              </button>
            )}
    </div>
  )
}

function formatCountdown(ms) {
  const total = Math.max(0, Math.floor(ms / 1000))
  const m = Math.floor(total / 60)
  const s = total % 60
  return `${m}:${String(s).padStart(2, '0')}`
}

// Optional browser notification for when the page is closed
function PushButton({ order, token }) {
  const [publicKey, setPublicKey] = useState('')
  const [state, setState] = useState(() => (pushPermission() === 'denied' ? 'denied' : 'idle'))
  const toast = useToast()

  useEffect(() => {
    getPushPublicKey().then(setPublicKey)
  }, [])

  if (!publicKey || pushPermission() === 'unsupported') return null
  if (state === 'subscribed') {
    return <p className="mt-5 text-center text-sm text-mint-deep">We'll notify you on this device when it's ready.</p>
  }
  if (state === 'denied') {
    return <p className="mt-5 text-center text-xs text-espresso/50">Notifications are blocked. Keep this page open for live updates.</p>
  }

  const enable = async () => {
    setState('working')
    try {
      const result = await subscribeToOrderPush({
        orderId: order.orderId,
        notificationToken: token,
        publicKey,
      })
      setState(result === 'subscribed' ? 'subscribed' : result === 'denied' ? 'denied' : 'idle')
    } catch (e) {
      setState('idle')
      toast(e.message || 'Could not turn on notifications.', 'error')
    }
  }

  return (
    <button className={`${btn.mint} mt-5 w-full`} onClick={enable} disabled={state === 'working'}>
      Notify me when it's ready
    </button>
  )
}



// Shows 12 min after the order is completed; customer can rate or skip
function FeedbackCard({ order, token }) {
  const now = useNow(15000)
  const skipKey = `rh_fb_skip_${token}`
  const [skipped, setSkipped] = useState(() => localStorage.getItem(skipKey) === '1')
  const [stars, setStars] = useState(0)
  const [comment, setComment] = useState('')
  const [sent, setSent] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')


  if (order.rating != null && !sent) return null

  if (skipped || now < feedbackReadyAt(order)) return null

  if (sent) {
    return <p className="mt-6 rounded-2xl bg-[#E1F5EE] p-4 text-center font-medium text-mint-deep">🙏 Thanks for your feedback!</p>
  }

  const skip = () => {
    try { localStorage.setItem(skipKey, '1') } catch { /* optional */ }
    setSkipped(true)
  }

  const submit = async () => {
    setBusy(true)
    setError('')
    try {
      await sendFeedback(token, stars, comment.trim())
      setSent(true)
    } catch (e) {
      setError(e.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="mt-6 rounded-2xl bg-white p-5 text-center shadow-sm">
      <p className="text-lg font-semibold">😋 How was your food?</p>
      <div className="mt-3 flex justify-center gap-1" role="radiogroup" aria-label="Rating">
        {[1, 2, 3, 4, 5].map((n) => (
          <button
            key={n}
            role="radio"
            aria-checked={stars === n}
            aria-label={`${n} star${n > 1 ? 's' : ''}`}
            onClick={() => setStars(n)}
            className={`text-4xl transition active:scale-90 ${n <= stars ? 'text-amber-400' : 'text-stone-300'}`}
          >
            ★
          </button>
        ))}
      </div>
      {stars > 0 && (
        <textarea
          className="mt-3 w-full rounded-xl border border-espresso/15 p-3 text-sm focus:border-mint-deep focus:outline-none"
          rows={2}
          maxLength={200}
          placeholder="Anything to tell us? (optional)"
          value={comment}
          onChange={(e) => setComment(e.target.value)}
        />
      )}
      {error && <p className="mt-2 text-sm text-red-700">{error}</p>}
      <div className="mt-3 grid grid-cols-2 gap-3">
        <button className={btn.ghost} onClick={skip}>Skip</button>
        <button className={btn.primary} onClick={submit} disabled={!stars || busy}>{busy ? 'Sending…' : 'Submit'}</button>
      </div>
    </section>
  )
}