import { Link } from 'react-router-dom'
import { BRAND } from '../config/brand'
import { Logo } from '../components/ui'
import { getLastOrder } from '../lib/customerSession'

/*
 * Shown when someone opens the site without scanning a table/counter QR.
 * Ordering always starts from a QR, because the QR tells us where to serve.
 */
export default function ScanPrompt({ message }) {
  const lastOrder = getLastOrder()

  return (
    <main className="mx-auto flex min-h-dvh max-w-md flex-col items-center justify-center px-6 text-center">
      <div className="coin">
        <Logo size={168} className="shadow-[0_20px_40px_-18px_rgba(43,33,28,0.55)]" />
      </div>
      <h1 className="mt-8 font-display text-3xl font-semibold">{BRAND.name}</h1>
      <p className="mt-3 text-espresso/70">
        {message || 'Scan the QR code on your table, or the one at the counter, to see the menu and order.'}
      </p>

      {lastOrder?.token && (
        <Link
          to={`/order/${lastOrder.token}`}
          className="mt-8 w-full rounded-2xl border border-espresso/10 bg-white px-5 py-4 text-left shadow-sm"
        >
          <span className="block text-sm text-espresso/60">Your last order</span>
          <span className="font-semibold">Track {lastOrder.orderNumber}</span>
        </Link>
      )}

    </main>
  )
}
