import { BRAND } from '../config/brand'

/*
 * Razorpay Standard Checkout.
 * UPI, cards, netbanking and wallets are all shown by Razorpay itself;
 * we do not restrict payment methods, so UPI is always available.
 *
 * Only the PUBLIC key id (sent by our backend) is used here.
 * The secret key never reaches the browser.
 */
let scriptPromise = null

export function loadRazorpay() {
  if (window.Razorpay) return Promise.resolve(true)
  if (!scriptPromise) {
    scriptPromise = new Promise((resolve) => {
      const script = document.createElement('script')
      script.src = 'https://checkout.razorpay.com/v1/checkout.js'
      script.onload = () => resolve(true)
      script.onerror = () => {
        scriptPromise = null
        resolve(false)
      }
      document.body.appendChild(script)
    })
  }
  return scriptPromise
}

/*
 * Opens the payment window. Resolves with:
 *   { status: 'paid', response }   -> send response to backend /verify
 *   { status: 'dismissed' }        -> customer closed the window
 * Payment failures inside the window are shown by Razorpay, and the customer
 * can retry there; we only report the last failure reason.
 */
export function openCheckout({ keyId, razorpayOrderId, amount, orderNumber, nickname, mobile, onFailure }) {
  return new Promise((resolve) => {
    let settled = false
    const finish = (result) => {
      if (!settled) {
        settled = true
        resolve(result)
      }
    }

    const checkout = new window.Razorpay({
      key: keyId,
      order_id: razorpayOrderId,
      amount: Math.round(Number(amount) * 100),
      currency: 'INR',
      name: BRAND.name,
      description: `Order ${orderNumber}`,
      image: window.location.origin + BRAND.logo,
      prefill: { name: nickname, contact: mobile },
      theme: { color: BRAND.razorpayColor },
      retry: { enabled: true },
      handler: (response) => finish({ status: 'paid', response }),
      modal: {
        ondismiss: () => finish({ status: 'dismissed' }),
        confirm_close: true,
      },
    })

    checkout.on('payment.failed', (event) => {
      onFailure?.(event?.error?.description || 'Payment failed. You can try again.')
    })

    checkout.open()
  })
}
