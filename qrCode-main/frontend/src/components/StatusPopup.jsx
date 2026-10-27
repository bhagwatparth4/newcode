import { useEffect } from 'react'

// Popup at the top of the customer's order page. Hides itself after 5 seconds.
const LOOK = {
  PAID: { icon: '🧾', title: 'Order received', text: "We've got your order." },
  PREPARING: { icon: '👨‍🍳', title: 'Being prepared', text: 'The kitchen has started your order.' },
  READY: { icon: '🔔', title: 'Your order is ready', text: '' },
  LATE: { icon: '⏳', title: 'Taking a little longer', text: 'Sorry for the wait. It will be ready soon.' },
}

export default function StatusPopup({ popup, onClose }) {
  useEffect(() => {
    if (!popup) return undefined
    const timer = setTimeout(() => onClose(), 5000)
    return () => clearTimeout(timer)
  }, [popup])

  if (!popup) return null

  const isLate = popup.orderStatus === 'PREPARING' && (popup.message || '').includes('longer')
  const look = isLate ? LOOK.LATE : LOOK[popup.orderStatus]
  if (!look) return null

  let text = look.text
  if (popup.orderStatus === 'READY') {
    text = popup.orderType === 'TABLE' && popup.tableNumber
      ? `Serving at Table ${popup.tableNumber}.`
      : 'Please collect it from the counter.'
  }

  return (
    <div className="fixed inset-x-0 top-3 z-50 flex justify-center px-3">
      <style>{'@keyframes rh-timer { from { width: 100% } to { width: 0% } }'}</style>
      <button
        key={popup.key}
        onClick={onClose}
        className={`alert-drop w-full max-w-sm overflow-hidden rounded-2xl bg-espresso text-left text-paper shadow-2xl ${
          popup.orderStatus === 'READY' ? 'ring-2 ring-mint' : ''
        }`}
      >
        <div className="flex items-center gap-3 px-4 py-3">
          <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-mint text-2xl">
            {look.icon}
          </span>
          <span className="min-w-0">
            <span className="block font-display text-lg font-semibold">{look.title}</span>
            <span className="block text-sm text-paper/75">{text}</span>
                        {popup.orderNumber && <span className="block text-xs text-paper/50">Order {popup.orderNumber}</span>}
          </span>
        </div>
        <span
          className="block h-1 bg-mint"
          style={{ animation: 'rh-timer 5s linear forwards' }}
        />
      </button>
    </div>
  )
}