import { useCallback, useEffect, useRef, useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { BRAND } from '../config/brand'
import { Logo } from '../components/ui'
import { useToast } from '../components/Toast'
import OrderAlerts from '../components/OrderAlerts'
import { clearAuth, getAuth } from '../auth/authStore'
import { getActiveOrders, getBusinessType } from '../api/staff'
import { useRealtime } from '../lib/useRealtime'
import { useOrderBell } from '../lib/useOrderBell'
import { AdminContext } from './AdminContext'
import OpenClosedSwitch from './OpenClosedSwitch'
import TableRequests, { playDing } from './TableRequests'

const NAV = [
  { to: '/admin', label: 'Dashboard', icon: '📊', end: true },
  { to: '/admin/new-order', label: 'New order', icon: '➕' },
  { to: '/admin/live', label: 'Live orders', short: 'Live', icon: '🍽️' },
  { to: '/admin/table-status', label: 'Table status', short: 'Tables', icon: '🪑' },
  { to: '/admin/day-report', label: 'Day-end report', short: 'Day-end', icon: '🌙' },
  { to: '/admin/orders', label: 'Order history', short: 'History', icon: '📜' },
  { to: '/admin/feedback', label: 'Feedback', icon: '⭐' },
  { to: '/admin/menu', label: 'Menu', icon: '📋' },
  { to: '/admin/tables', label: 'Tables & QR', icon: '🔳' },
  { to: '/admin/coupons', label: 'Coupons', icon: '🏷️' },
  { to: '/admin/customers', label: 'Customers', icon: '👥' },
  { to: '/admin/settings', label: 'Settings', icon: '⚙️' },
]
// Pinned to the bottom bar on tablet/phone; the rest live under "More"
const PINNED = ['/admin/live', '/admin/new-order', '/admin/table-status', '/admin']
const ACTIVE = ['PAID', 'PREPARING', 'READY']

export default function AdminLayout() {
  const navigate = useNavigate()
  const toast = useToast()
  const session = getAuth()
  const [businessType, setBusinessType] = useState(session?.businessType || 'SMALL_CAFE')
  const [liveVersion, setLiveVersion] = useState(0)
  const [moreOpen, setMoreOpen] = useState(false)
  const [liveCount, setLiveCount] = useState(0)
  const hasKitchen = businessType !== 'SMALL_CAFE'
  const bell = useOrderBell()
  const [requests, setRequests] = useState([])
  const lastDing = useRef(0)

  useEffect(() => {
    getBusinessType().then((r) => r?.businessType && setBusinessType(r.businessType)).catch(() => {})
  }, [])

  // Running-orders badge on the "Live" tab
  useEffect(() => {
    getActiveOrders().then((o) => setLiveCount(o.filter((x) => ACTIVE.includes(x.status)).length)).catch(() => {})
  }, [liveVersion])

  const onMessage = useCallback(
    (topic, event) => {
      if (topic === '/topic/admin/requests') {
        const key = `${event.tableNumber}:${event.type}`
        setRequests((list) => [{ ...event, key, at: Date.now() }, ...list.filter((r) => r.key !== key)].slice(0, 6))
        if (Date.now() - lastDing.current > 10000) {
          lastDing.current = Date.now()
          playDing()
        }
        return
      }
      if (topic === '/topic/admin/orders') {
        if (!hasKitchen) bell.announce(event)
      } else if (hasKitchen && event?.orderStatus === 'READY') {
        toast(`${event.orderNumber} is ready to hand over`)
      }
      setLiveVersion((v) => v + 1)
    },
    [hasKitchen, bell, toast],
  )

  useRealtime({
    topics: ['/topic/admin/orders', '/topic/admin/order-updates', '/topic/admin/requests'],
    token: session?.token,
    onMessage,
    onConnect: () => setLiveVersion((v) => v + 1),
  })

  const logout = () => {
    clearAuth()
    navigate('/login', { replace: true })
  }

  const pinned = PINNED.map((to) => NAV.find((n) => n.to === to))
  const more = NAV.filter((n) => !PINNED.includes(n.to))

  return (
    <AdminContext.Provider value={{ businessType, hasKitchen, liveVersion }}>
      <div className="min-h-dvh bg-stone-50 lg:flex">
        {/* Desktop sidebar */}
        <aside className="relative hidden w-64 shrink-0 bg-espresso p-5 text-paper lg:block">
          <div className="flex items-center gap-3">
            <Logo size={40} />
            <div>
              <p className="font-display text-lg font-semibold leading-tight">{BRAND.name}</p>
              <p className="text-xs text-paper/60">{hasKitchen ? 'Admin · with kitchen' : 'Admin'}</p>
            </div>
          </div>
          <OpenClosedSwitch />
          <nav className="mt-6 flex flex-col gap-1 pb-20">
            {NAV.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                className={({ isActive }) =>
                  `flex items-center gap-2.5 rounded-xl px-3.5 py-2.5 text-sm font-medium transition ${
                    isActive ? 'bg-mint text-espresso' : 'text-paper/80 hover:bg-paper/10'
                  }`
                }
              >
                <span>{item.icon}</span>
                {item.label}
                {item.to === '/admin/live' && liveCount > 0 && (
                  <span className="ml-auto rounded-full bg-red-600 px-2 text-xs font-bold text-white">{liveCount}</span>
                )}
              </NavLink>
            ))}
          </nav>
          <div className="absolute inset-x-5 bottom-5 border-t border-paper/10 pt-4 text-sm">
            <p className="truncate text-paper/70">{session?.name}</p>
            <button onClick={logout} className="mt-2 text-paper/60 underline-offset-4 hover:text-paper hover:underline">Log out</button>
          </div>
        </aside>

        <div className="min-w-0 flex-1">
          {/* Tablet/phone header */}
          <header className="sticky top-0 z-20 flex items-center gap-3 border-b border-stone-200 bg-white/90 px-4 py-3 backdrop-blur lg:hidden">
            <Logo size={32} />
            <span className="font-display text-lg font-semibold">{BRAND.name}</span>
          </header>
          <main className="mx-auto max-w-7xl p-4 pb-28 sm:p-6 sm:pb-28 lg:p-8">
            <Outlet />
          </main>
        </div>
      </div>

      {/* Tablet/phone bottom tab bar */}
      <nav className="fixed inset-x-0 bottom-0 z-30 flex border-t border-stone-200 bg-white/95 pb-[env(safe-area-inset-bottom)] backdrop-blur lg:hidden">
        {pinned.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end}
            onClick={() => setMoreOpen(false)}
            className={({ isActive }) =>
              `flex flex-1 flex-col items-center gap-0.5 py-2 text-[11px] font-medium ${isActive ? 'text-mint-deep' : 'text-stone-500'}`
            }
          >
            {({ isActive }) => (
              <>
                <span className={`relative rounded-full px-4 py-0.5 text-lg ${isActive ? 'bg-[#E1F5EE]' : ''}`}>
                  {item.icon}
                  {item.to === '/admin/live' && liveCount > 0 && (
                    <span className="absolute -right-1 -top-1 rounded-full bg-red-600 px-1.5 text-[10px] font-bold text-white">{liveCount}</span>
                  )}
                </span>
                {item.short || item.label}
              </>
            )}
          </NavLink>
        ))}
        <button onClick={() => setMoreOpen(true)} className="flex flex-1 flex-col items-center gap-0.5 py-2 text-[11px] font-medium text-stone-500">
          <span className="px-4 py-0.5 text-lg">☰</span>
          More
        </button>
      </nav>

      {/* "More" sheet */}
      {moreOpen && (
        <div className="fixed inset-0 z-40 flex items-end bg-black/40 lg:hidden" onClick={() => setMoreOpen(false)}>
          <div className="w-full rounded-t-3xl bg-white p-4 pb-[max(1rem,env(safe-area-inset-bottom))]" onClick={(e) => e.stopPropagation()}>
            <div className="mx-auto mb-3 h-1 w-10 rounded-full bg-stone-300" />
            <div className="grid grid-cols-4 gap-2 sm:grid-cols-8">
              {more.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  onClick={() => setMoreOpen(false)}
                  className={({ isActive }) =>
                    `flex flex-col items-center gap-1 rounded-2xl px-1 py-3 text-center text-xs font-medium ${isActive ? 'bg-[#E1F5EE] text-mint-deep' : 'bg-stone-50 text-stone-700'}`
                  }
                >
                  <span className="text-xl">{item.icon}</span>
                  {item.short || item.label}
                </NavLink>
              ))}
            </div>
            <div className="mt-3 flex items-center justify-between text-sm">
              <span className="truncate text-stone-500">{session?.name}</span>
              <button onClick={logout} className="font-medium text-red-600">Log out</button>
            </div>
          </div>
        </div>
      )}

      <TableRequests items={requests} onDone={(key) => setRequests((list) => list.filter((r) => r.key !== key))} />
      <OrderAlerts
        alerts={bell.alerts}
        onDismiss={bell.dismiss}
        soundEnabled={hasKitchen ? true : bell.soundEnabled}
        onEnableSound={bell.enableSound}
      />
    </AdminContext.Provider>
  )
}