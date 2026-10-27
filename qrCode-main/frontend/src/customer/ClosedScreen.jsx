import { Link } from 'react-router-dom'
import { BRAND } from '../config/brand'
import { Logo } from '../components/ui'
import { getMyOrders } from '../lib/customerSession'

// Shown instead of the menu when the admin has switched off orders
export default function ClosedScreen() {
  return (
    <main className="mx-auto flex min-h-dvh max-w-md flex-col items-center justify-center px-6 text-center">
      <div className="coin">
        <Logo size={150} className="shadow-[0_20px_40px_-18px_rgba(43,33,28,0.55)]" />
      </div>
      <span className="mt-8 rounded-full bg-espresso px-4 py-1.5 text-sm font-semibold text-mint">Closed right now</span>
      <h1 className="mt-4 font-display text-3xl font-semibold">We'll be back soon</h1>
      <p className="mt-2 text-espresso/70">
        {BRAND.name} isn't taking orders at the moment. Please check again in a little while, or ask our staff.
      </p>
       {getMyOrders().slice(0, 3).map((o) => (
        <Link key={o.token} to={`/order/${o.token}`} className="mt-4 w-full rounded-2xl bg-mint/30 px-4 py-3 text-sm">
          Track order {o.orderNumber}
        </Link>
      ))}
    </main>
  )
}