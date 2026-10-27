import { useEffect, useState } from 'react'
import { getCafeStatus } from '../api/customer'
import { setAcceptingOrders } from '../api/staff'

// Sidebar switch: open / close the cafe for new customer orders
export default function OpenClosedSwitch() {
  const [open, setOpen] = useState(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    getCafeStatus().then((s) => setOpen(s?.acceptingOrders !== false)).catch(() => {})
  }, [])

  const toggle = async () => {
    if (open && !window.confirm('Stop taking new orders? Customers will see "Closed right now".')) return
    setBusy(true)
    try {
      const result = await setAcceptingOrders(!open)
      setOpen(result.acceptingOrders)
    } catch {
      /* keep the old state */
    } finally {
      setBusy(false)
    }
  }

  if (open === null) return null

  return (
    <button
      onClick={toggle}
      disabled={busy}
      className={`mt-6 flex w-full items-center justify-between rounded-xl px-3.5 py-2.5 text-sm font-semibold ${
        open ? 'bg-mint/20 text-mint' : 'bg-red-500/20 text-red-300'
      }`}
    >
      {open ? 'Open · taking orders' : 'Closed · no orders'}
      <span className={`h-2.5 w-2.5 rounded-full ${open ? 'bg-mint' : 'bg-red-400'}`} />
    </button>
  )
}