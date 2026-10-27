import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { createOrder, createRazorpayOrder, getMenuItems, getPaymentOptions, validateCoupon, verifyPayment } from '../api/customer'
import { ErrorNote, PageLoader, Spinner, VegMark, btn, input } from '../components/ui'
import { money, placeLabel } from '../lib/format'
import { loadRazorpay, openCheckout } from '../lib/razorpay'
import {
  getCustomCart, saveCustomCart,checkoutKeyFor, clearCart, clearCheckoutAttempt, getCart, getNickname, getQr,
  saveCart,saveLastOrder, getSavedMobile, saveMobile,addMyOrder,
} from '../lib/customerSession'

/*
 * Customer flow: cart -> coupon -> mobile number -> Razorpay -> verify.
 *
 * Safety notes (the backend enforces all of these, the UI just cooperates):
 * - Prices, discount and the amount charged are calculated by the server.
 * - The same cart re-uses the same idempotency key, so double taps,
 *   refreshes and retries never create a second order or payment.
 * - "Paid" is only shown after the backend verified the payment (or the
 *   Razorpay webhook confirmed it).
 */
const MOBILE_RE = /^[6-9]\d{9}$/

export default function CartPage() {
  const navigate = useNavigate()
  const qr = getQr()
  const nickname = getNickname()

  const [menu, setMenu] = useState(null)
  const [cart, setCart] = useState(getCart)
    const [custom, setCustom] = useState(getCustomCart)
  const [loadError, setLoadError] = useState('')

  const [couponCode, setCouponCode] = useState('')
  const [coupon, setCoupon] = useState(null) // validated coupon preview
  const [couponMessage, setCouponMessage] = useState('')
  const [checkingCoupon, setCheckingCoupon] = useState(false)

  const [mobile, setMobile] = useState(getSavedMobile)
  const [editMobile, setEditMobile] = useState(!getSavedMobile())
  const [smsOptIn, setSmsOptIn] = useState(false)
  const [whatsappOptIn, setWhatsappOptIn] = useState(false)
  const [note, setNote] = useState('')

  const [busy, setBusy] = useState(false)
  const [stage, setStage] = useState('') // text shown on the pay button while working
  const [error, setError] = useState('')
  const [payMethod, setPayMethod] = useState(null)
  const [cashEnabled, setCashEnabled] = useState(false)

  useEffect(() => {
    getMenuItems().then(setMenu).catch((e) => setLoadError(e.message))
  }, [])

     useEffect(() => {
       getPaymentOptions()
         .then((o) => {
           setCashEnabled(!!o.cashEnabled)
           if (!o.cashEnabled) setPayMethod('ONLINE')
         })
         .catch(() => setPayMethod('ONLINE'))
     }, [])

  // Cart lines using CURRENT menu data (unavailable/removed items are flagged)
  const lines = useMemo(() => {
    if (!menu) return []
    return Object.entries(cart).map(([id, quantity]) => {
      const item = menu.find((m) => String(m.id) === String(id))
      return { id: Number(id), quantity, item, problem: !item ? 'removed' : !item.available ? 'soldout' : null }
    })
  }, [menu, cart])

  const okLines = lines.filter((l) => !l.problem)
    const customLines = custom.map((l) => {
      const item = (menu || []).find((m) => String(m.id) === String(l.menuItemId))
      return { ...l, item, problem: !item ? 'removed' : !item.available ? 'soldout' : null }
    })
    const customOk = customLines.filter((l) => !l.problem)
   const subtotal = okLines.reduce((sum, l) => sum + l.quantity * Number(l.item.price), 0)
     + customOk.reduce((sum, l) => sum + l.qty * Number(l.unitPrice), 0)
  const discount = coupon ? Number(coupon.discountAmount) : 0
  const total = Math.max(0, subtotal - discount)

  // If the cart total changes, the coupon preview must be checked again
  useEffect(() => {
    if (coupon && Number(coupon.orderAmount) !== subtotal) {
      setCoupon(null)
      setCouponMessage('Your cart changed. Tap Apply again to update the discount.')
    }
  }, [subtotal, coupon])

  if (!qr || !nickname) {
    return (
      <main className="mx-auto max-w-md p-6 text-center">
        <p className="mt-20 text-espresso/70">Please scan the QR code on your table or at the counter first.</p>
      </main>
    )
  }

  const setQuantity = (id, quantity) => {
    setCart((current) => {
      const next = { ...current }
      if (quantity <= 0) delete next[id]
      else next[id] = Math.min(50, quantity)
      saveCart(next)
      return next
    })
  }

    const setCustomQty = (key, qty) => {
      setCustom((cur) => {
        const next = cur.map((l) => (l.key === key ? { ...l, qty: Math.min(50, qty) } : l)).filter((l) => l.qty > 0)
        saveCustomCart(next)
        return next
      })
    }

  const removeProblemItems = () => {
    const next = { ...cart }
    lines.filter((l) => l.problem).forEach((l) => delete next[l.id])
    saveCart(next)
    setCart(next)

        const nextCustom = custom.filter((l) => !customLines.find((c) => c.key === l.key)?.problem)
        saveCustomCart(nextCustom)
        setCustom(nextCustom)
  }

  const applyCoupon = async () => {
    const code = couponCode.trim().toUpperCase()
    if (!code) return
    setCheckingCoupon(true)
    setCouponMessage('')
    try {
      const result = await validateCoupon(code, subtotal)
      if (result.valid) {
        setCoupon(result)
        setCouponMessage(`${result.couponCode} applied. You save ${money(result.discountAmount)}.`)
      } else {
        setCoupon(null)
        setCouponMessage(result.message || 'This coupon cannot be used.')
      }
    } catch (e) {
      setCoupon(null)
      setCouponMessage(e.message)
    } finally {
      setCheckingCoupon(false)
    }
  }


    const chooseMethod = (method) => {
      setPayMethod(method)
      setError('')
      if (method === 'CASH' && (coupon || couponCode)) {
        setCoupon(null)
        setCouponCode('')
        setCouponMessage('Coupon removed – coupons are valid only on online payment.')
      } else {
        setCouponMessage('')
      }
    }



        const goToOrder = (token, orderNumber, state = { justPaid: true }) => {
      saveLastOrder({ token, orderNumber })
      addMyOrder({ token, orderNumber })
    clearCart()
    clearCheckoutAttempt()
        navigate(`/order/${token}`, { replace: true, state })
  }

  const pay = async () => {
    setError('')
        if (lines.some((l) => l.problem) || customLines.some((l) => l.problem)) {
      setError('Some items are no longer available. Remove them to continue.')
      return
    }
    if (!MOBILE_RE.test(mobile)) {
      setError('Please enter your 10-digit mobile number.')
      return
    }

    setBusy(true)
    try {
           const items = [
             ...okLines.map((l) => ({ menuItemId: l.id, quantity: l.quantity })),
             ...customOk.map((l) => ({ menuItemId: l.menuItemId, quantity: l.qty, addonIds: l.addonIds })),
           ]
            const couponToSend = coupon && payMethod === 'ONLINE' ? coupon.couponCode : null
            const signature = JSON.stringify({ items, couponToSend, mobile, nickname, qr: qr.qrToken, smsOptIn, whatsappOptIn, note, payMethod })

      setStage('Placing order…')
      const order = await createOrder({
        nickname,
        mobileNumber: mobile,
        qrToken: qr.qrToken,
        idempotencyKey: checkoutKeyFor(signature),
        couponCode: couponToSend,
        items,
        smsOptIn,
        whatsappOptIn,
        cookingNote: note.trim() || null,
        paymentMethod: payMethod,
      })
      const token = order.customerNotificationToken
//       saveLastOrder({ token, orderNumber: order.orderNumber })
      saveMobile(mobile)
//             addMyOrder({ token, orderNumber: order.orderNumber })
            if (payMethod === 'CASH') {
              goToOrder(token, order.orderNumber, { cashOrder: true })
              return
            }
      setStage('Opening payment…')
      let payment
      try {
        payment = await createRazorpayOrder(order.id, token)
      } catch (e) {
        // Already paid (e.g. confirmed by the webhook): just show the order
        if (e.code === 'PAYMENT_NOT_ALLOWED' || e.code === 'PAYMENT_ALREADY_COMPLETED') {
          goToOrder(token, order.orderNumber)
          return
        }
        throw e
      }

      const loaded = await loadRazorpay()
      if (!loaded) throw new Error('Could not open the payment window. Check your internet connection and try again.')

      setStage('Waiting for payment…')
      const result = await openCheckout({
        keyId: payment.keyId,
        razorpayOrderId: payment.razorpayOrderId,
        amount: payment.amount,
        orderNumber: order.orderNumber,
        nickname,
        mobile,
        onFailure: (reason) => setError(reason),
      })

      if (result.status !== 'paid') {
        setError((current) => current || 'Payment was not completed. You can try again; you will not be charged twice.')
        return
      }

      setStage('Confirming payment…')
      try {
        await verifyPayment({
          orderId: order.id,
          notificationToken: token,
          razorpayOrderId: result.response.razorpay_order_id,
          razorpayPaymentId: result.response.razorpay_payment_id,
          razorpaySignature: result.response.razorpay_signature,
        })
      } catch {
        // Money may still be captured: the Razorpay webhook will confirm it.
        // The order page waits for the backend's confirmation either way.
      }
     goToOrder(token, order.orderNumber)
    } catch (e) {
      setError(e.message)
    } finally {
      setBusy(false)
      setStage('')
    }
  }

  if (loadError) return <main className="mx-auto max-w-md p-6"><ErrorNote>{loadError}</ErrorNote></main>
  if (!menu) return <PageLoader label="Loading your cart" />

  return (
    <div className="mx-auto min-h-dvh max-w-xl pb-36">
      <header className="flex items-center gap-3 px-5 pb-2 pt-5">
        <Link to="/menu" className="rounded-full bg-white p-2 shadow-sm" aria-label="Back to menu">
          <svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="2"><path d="M12 4l-6 6 6 6" /></svg>
        </Link>
        <div>
          <h1 className="font-display text-2xl font-semibold">Your order</h1>
          <p className="text-sm text-espresso/60">
            {nickname} · {placeLabel(qr.orderType, qr.tableNumber)}
          </p>
        </div>
      </header>

           {lines.length === 0 && customLines.length === 0 ? (
        <div className="px-5 pt-16 text-center">
          <p className="text-espresso/70">Your cart is empty.</p>
          <Link to="/menu" className={`${btn.primary} mt-6`}>Browse the menu</Link>
        </div>
      ) : (
        <main className="space-y-5 px-5 pt-3">
          <ul className="divide-y divide-espresso/10 rounded-2xl bg-white shadow-sm">
            {lines.map((line) => (
              <li key={line.id} className="flex items-center gap-3 p-4">
                {line.item && <VegMark type={line.item.dietaryType} />}
                <div className="min-w-0 flex-1">
                  <p className={`font-medium ${line.problem ? 'text-espresso/50 line-through' : ''}`}>
                    {line.item ? line.item.name : 'Item no longer on the menu'}
                  </p>
                  {line.problem ? (
                    <p className="text-sm font-medium text-red-700">{line.problem === 'soldout' ? 'Sold out' : 'Removed'}</p>
                  ) : (
                    <p className="text-sm text-espresso/60">{money(line.item.price)} each</p>
                  )}
                </div>
                {!line.problem && (
                  <div className="flex items-center rounded-xl border border-espresso/15">
                    <button className="px-3 py-1.5" onClick={() => setQuantity(line.id, line.quantity - 1)} aria-label="Decrease">−</button>
                    <span className="w-6 text-center text-sm font-semibold">{line.quantity}</span>
                    <button className="px-3 py-1.5" onClick={() => setQuantity(line.id, line.quantity + 1)} aria-label="Increase">+</button>
                  </div>
                )}
                <span className="w-20 text-right font-medium">
                  {line.problem ? '' : money(line.quantity * Number(line.item.price))}
                </span>
              </li>
            ))}

                        {customLines.map((l) => (
                          <li key={l.key} className="flex items-center gap-3 p-4">
                            {l.item && <VegMark type={l.item.dietaryType} />}
                            <div className="min-w-0 flex-1">
                              <p className={`font-medium ${l.problem ? 'text-espresso/50 line-through' : ''}`}>{l.name}</p>
                              {l.addonsText && <p className="text-xs text-espresso/60">{l.addonsText}</p>}
                              {l.problem
                                ? <p className="text-sm font-medium text-red-700">{l.problem === 'soldout' ? 'Sold out' : 'Removed'}</p>
                                : <p className="text-sm text-espresso/60">{money(l.unitPrice)} each</p>}
                            </div>
                            {!l.problem && (
                              <div className="flex items-center rounded-xl border border-espresso/15">
                                <button className="px-3 py-1.5" onClick={() => setCustomQty(l.key, l.qty - 1)} aria-label="Decrease">−</button>
                                <span className="w-6 text-center text-sm font-semibold">{l.qty}</span>
                                <button className="px-3 py-1.5" onClick={() => setCustomQty(l.key, l.qty + 1)} aria-label="Increase">+</button>
                              </div>
                            )}
                            <span className="w-20 text-right font-medium">{l.problem ? '' : money(l.qty * l.unitPrice)}</span>
                          </li>
                        ))}

          </ul>

                    {(lines.some((l) => l.problem) || customLines.some((l) => l.problem)) && (
            <button onClick={removeProblemItems} className={`${btn.ghost} w-full`}>
              Remove unavailable items
            </button>
          )}

                    {cashEnabled && (
                      <section className="rounded-2xl bg-white p-4 shadow-sm">
                        <p className="text-sm font-medium text-espresso/80">How would you like to pay?</p>
                        <div className="mt-2 grid grid-cols-2 gap-2">
                          {[['ONLINE', 'Pay Online', 'UPI, cards'], ['CASH', 'Pay Cash', 'At the counter']].map(([value, label, sub]) => (
                            <button
                              key={value}
                              type="button"
                              onClick={() => chooseMethod(value)}
                              className={`rounded-xl border-2 p-3 text-left ${payMethod === value ? 'border-mint-deep bg-mint-deep/5' : 'border-espresso/15'}`}
                            >
                              <span className="block font-semibold">{label}</span>
                              <span className="block text-xs text-espresso/60">{sub}</span>
                            </button>
                          ))}
                        </div>
                      </section>
                    )}

          {payMethod === null ? null : payMethod === 'ONLINE' ? (

          <section className="rounded-2xl bg-white p-4 shadow-sm">
            <label htmlFor="coupon" className="text-sm font-medium text-espresso/80">Coupon</label>
            <div className="mt-2 flex gap-2">
              <input
                id="coupon"
                className={`${input} uppercase`}
                placeholder="Enter code"
                value={couponCode}
                maxLength={50}
                onChange={(e) => {
                  setCouponCode(e.target.value)
                  if (coupon) setCoupon(null)
                  setCouponMessage('')
                }}
              />
              <button className={btn.ghost} onClick={applyCoupon} disabled={checkingCoupon || !couponCode.trim()}>
                {checkingCoupon ? <Spinner /> : 'Apply'}
              </button>
            </div>
            {couponMessage && (
              <p className={`mt-2 text-sm ${coupon ? 'text-mint-deep' : 'text-red-700'}`}>{couponMessage}</p>
            )}
          </section>

                    ) : (
                      <section className="rounded-2xl bg-white p-4 text-sm shadow-sm">
                        🏷️ {couponMessage || 'Coupons are valid only on online payment.'}
                      </section>
                    )}

          <section className="rounded-2xl bg-white p-4 shadow-sm">
            <label htmlFor="mobile" className="text-sm font-medium text-espresso/80">Mobile number</label>
            {editMobile ? (
            <div className="mt-2 flex items-center rounded-xl border border-espresso/15 focus-within:border-mint-deep">
              <span className="pl-3.5 text-espresso/60">+91</span>
              <input
                id="mobile"
                inputMode="numeric"
                autoComplete="tel-national"
                className="w-full rounded-xl bg-transparent px-3 py-2.5 focus:outline-none"
                placeholder="10-digit number"
                value={mobile}
                onChange={(e) => setMobile(e.target.value.replace(/\D/g, '').slice(0, 10))}
              />
            </div>
                      ) : (
                          <div className="mt-2 flex items-center justify-between rounded-xl bg-mist px-3.5 py-2.5">
                            <span>+91 ******{mobile.slice(-4)}</span>
                            <button type="button" className="text-sm font-semibold text-mint-deep" onClick={() => setEditMobile(true)}>
                              Change
                            </button>
                          </div>
                        )}
            <p className="mt-1.5 text-xs text-espresso/50">Used for your payment and receipt. No account needed.</p>
                        <label htmlFor="note" className="mt-4 block text-sm font-medium text-espresso/80">Cooking request (optional)</label>
                        <input
                          id="note"
                          className={`${input} mt-2`}
                          placeholder="Less spicy, no onion"
                          maxLength={50}
                          value={note}
                          onChange={(e) => setNote(e.target.value)}
                        />
                        <p className="mt-1 text-right text-xs text-espresso/50">{note.length}/50</p>
            <div className="mt-3 space-y-2 text-sm text-espresso/80">
              <label className="flex items-start gap-2">
                <input type="checkbox" className="mt-0.5 h-4 w-4 accent-espresso" checked={smsOptIn} onChange={(e) => setSmsOptIn(e.target.checked)} />
                Send me offers by SMS (optional)
              </label>
              <label className="flex items-start gap-2">
                <input type="checkbox" className="mt-0.5 h-4 w-4 accent-espresso" checked={whatsappOptIn} onChange={(e) => setWhatsappOptIn(e.target.checked)} />
                Send me offers on WhatsApp (optional)
              </label>
            </div>
          </section>

          <section className="space-y-1.5 rounded-2xl bg-white p-4 text-sm shadow-sm">
            <div className="flex justify-between"><span>Subtotal</span><span>{money(subtotal)}</span></div>
            {discount > 0 && (
              <div className="flex justify-between text-mint-deep"><span>Discount ({coupon.couponCode})</span><span>− {money(discount)}</span></div>
            )}
            <div className="flex justify-between pt-1 text-base font-semibold"><span>To pay</span><span>{money(total)}</span></div>
            <p className="pt-1 text-xs text-espresso/50">The final amount is confirmed securely before payment.</p>
          </section>

          <ErrorNote>{error}</ErrorNote>
        </main>
      )}

           {(okLines.length > 0 || customOk.length > 0) && (
        <div className="fixed inset-x-0 bottom-0 z-30 bg-gradient-to-t from-mist via-mist to-transparent px-4 pb-[max(1rem,env(safe-area-inset-bottom))] pt-6">
                    <button onClick={pay} disabled={busy || !payMethod} className={`${btn.primary} mx-auto flex w-full max-w-xl py-4 text-base`}>
                        {busy ? (<><Spinner /> {stage}</>) : payMethod === 'CASH' ? `Place order · Pay ${money(total)} at counter` : `Pay ${money(total)}`}
          </button>
                    <p className="mt-2 text-center text-xs text-espresso/50">
                      {payMethod === 'CASH'
                        ? 'Please pay at the counter. Unpaid orders may be cancelled by the cafe.'
                        : 'UPI, cards and netbanking via Razorpay · Online paid orders cannot be cancelled or refunded.'}
                    </p>
        </div>
      )}
    </div>
  )
}
