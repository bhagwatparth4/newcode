import { useEffect, useState } from 'react'
import { getCafeStatus } from '../api/customer'
import { putAcceptingOrders } from '../api/staff'
import { useToast } from '../components/Toast'

// Small one-tap Open / Closed switch for the dashboard
export default function OpenClosedPill() {
  const toast = useToast()
  const [open, setOpen] = useState(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    getCafeStatus().then((s) => setOpen(s?.acceptingOrders !== false)).catch(() => {})
  }, [])

  const toggle = async () => {
    if (open && !window.confirm('Stop taking new orders? Customers will see "Closed".')) return
    setBusy(true)
    try {
      const r = await putAcceptingOrders(!open)
      setOpen(r.acceptingOrders)
      toast(r.acceptingOrders ? '🟢 Cafe is open' : '🔴 Cafe is closed')
    } catch (e) {
      toast(e.message, 'error')
    } finally {
      setBusy(false)
    }
  }

  if (open === null) return null
  return (
    <button onClick={toggle} disabled={busy}
      className={`flex items-center gap-2 rounded-full px-3 py-1.5 text-sm font-semibold ${open ? 'bg-[#E1F5EE] text-[#085041]' : 'bg-red-50 text-red-700'}`}>
      <span className={`relative h-4 w-7 rounded-full transition ${open ? 'bg-mint-deep' : 'bg-stone-300'}`}>
        <span className={`absolute top-0.5 h-3 w-3 rounded-full bg-white transition ${open ? 'left-[14px]' : 'left-0.5'}`} />
      </span>
      {open ? 'Open' : 'Closed'}
    </button>
  )
}