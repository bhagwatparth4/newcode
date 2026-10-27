import { placeLabel, money } from '../lib/format'

/*
 * "Order Received!" banners (top of the Admin/Kitchen screen) plus the
 * "Enable sound" button needed because browsers block sound until a click.
 */
export default function OrderAlerts({ alerts, onDismiss, soundEnabled, onEnableSound }) {
  return (
    <>
      {!soundEnabled && (
        <button
          onClick={onEnableSound}
          className="fixed bottom-4 right-4 z-40 flex items-center gap-2 rounded-full bg-amber-400 px-4 py-2.5 text-sm font-semibold text-espresso shadow-lg"
        >
          <span aria-hidden="true">🔔</span> Tap to enable order sound
        </button>
      )}

      <div className="pointer-events-none fixed inset-x-0 top-3 z-50 flex flex-col items-center gap-2 px-3" aria-live="assertive">
        {alerts.map((alert) => (
          <div
            key={alert.orderId}
            className="alert-drop pointer-events-auto flex w-full max-w-md items-center gap-4 rounded-2xl bg-espresso px-5 py-4 text-paper shadow-2xl"
          >
            <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-mint text-xl" aria-hidden="true">🔔</span>
            <div className="min-w-0 flex-1">
              <p className="font-display text-lg font-semibold">Order Received!</p>
              <p className="truncate text-sm text-paper/80">
                {alert.orderNumber} · {placeLabel(alert.orderType, alert.tableNumber)}
                {alert.nickname ? ` · ${alert.nickname}` : ''}
                {alert.totalAmount != null ? ` · ${money(alert.totalAmount)}` : ''}
              </p>
            </div>
            <button onClick={() => onDismiss(alert.orderId)} className="rounded-full px-2 text-paper/70 hover:text-paper" aria-label="Dismiss">
              ✕
            </button>
          </div>
        ))}
      </div>
    </>
  )
}
