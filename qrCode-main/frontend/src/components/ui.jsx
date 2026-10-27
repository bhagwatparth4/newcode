import { BRAND } from '../config/brand'

/*
 * Small shared building blocks. Kept in one file on purpose so there is
 * only one place to look when changing the look of buttons, inputs, etc.
 */

export const btn = {
  primary:
    'inline-flex items-center justify-center gap-2 rounded-xl bg-espresso px-5 py-3 font-semibold text-mint transition active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50',
  mint:
    'inline-flex items-center justify-center gap-2 rounded-xl bg-mint px-5 py-3 font-semibold text-espresso transition active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50',
  ghost:
    'inline-flex items-center justify-center gap-2 rounded-xl border border-espresso/15 bg-white px-4 py-2.5 font-medium text-espresso transition hover:bg-espresso/5 disabled:opacity-50',
  danger:
    'inline-flex items-center justify-center gap-2 rounded-xl border border-red-200 bg-white px-4 py-2.5 font-medium text-red-700 transition hover:bg-red-50 disabled:opacity-50',
}

export const input =
  'w-full rounded-xl border border-espresso/15 bg-white px-3.5 py-2.5 text-espresso placeholder:text-espresso/40 focus:border-mint-deep focus:outline-none'

export function Logo({ size = 56, className = '' }) {
  return (
    <img
      src={BRAND.logo}
      alt={`${BRAND.name} logo`}
      width={size}
      height={size}
      className={`rounded-full ${className}`}
    />
  )
}

// Indian food-label convention: square outline with a dot (green veg / red non-veg)
export function VegMark({ type, className = '' }) {
  const isVeg = type === 'VEG'
  const color = isVeg ? 'border-veg' : 'border-nonveg'
  const dot = isVeg ? 'bg-veg' : 'bg-nonveg'
  return (
    <span
      role="img"
      aria-label={isVeg ? 'Vegetarian' : 'Non-vegetarian'}
      title={isVeg ? 'Veg' : 'Non-veg'}
      className={`inline-flex h-4 w-4 shrink-0 items-center justify-center rounded-[3px] border-2 bg-white ${color} ${className}`}
    >
      <span className={`h-1.5 w-1.5 rounded-full ${dot}`} />
    </span>
  )
}

export function Spinner({ className = '' }) {
  return (
    <span
      className={`inline-block h-5 w-5 animate-spin rounded-full border-2 border-current border-t-transparent ${className}`}
      aria-hidden="true"
    />
  )
}

export function PageLoader({ label = 'Loading' }) {
  return (
    <div className="flex min-h-[50vh] flex-col items-center justify-center gap-3 text-espresso/60">
      <Spinner />
      <span className="text-sm">{label}…</span>
    </div>
  )
}

export function ErrorNote({ children }) {
  if (!children) return null
  return (
    <p role="alert" className="rounded-xl border border-red-200 bg-red-50 px-3.5 py-2.5 text-sm text-red-800">
      {children}
    </p>
  )
}

export function Field({ label, hint, children }) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-sm font-medium text-espresso/80">{label}</span>
      {children}
      {hint && <span className="mt-1 block text-xs text-espresso/50">{hint}</span>}
    </label>
  )
}

export function Modal({ open, onClose, title, children, wide = false }) {
  if (!open) return null
  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-espresso/50 sm:items-center" onClick={onClose}>
      <div
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className={`sheet-up max-h-[92vh] w-full overflow-y-auto rounded-t-3xl bg-white p-5 shadow-2xl sm:rounded-3xl ${
          wide ? 'sm:max-w-2xl' : 'sm:max-w-md'
        }`}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="mb-4 flex items-start justify-between gap-4">
          <h2 className="font-display text-xl font-semibold">{title}</h2>
          <button onClick={onClose} className="rounded-full p-1.5 text-espresso/60 hover:bg-espresso/5" aria-label="Close">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M5 5l10 10M15 5L5 15" />
            </svg>
          </button>
        </div>
        {children}
      </div>
    </div>
  )
}

const STATUS_STYLE = {
  PAYMENT_PENDING: 'bg-stone-100 text-stone-600',
  PAID: 'bg-amber-100 text-amber-800',
  PREPARING: 'bg-sky-100 text-sky-800',
  READY: 'bg-mint/40 text-mint-deep',
  COMPLETED: 'bg-espresso/10 text-espresso/70',
  CANCELLED: 'bg-red-100 text-red-700',
}

const STATUS_TEXT = {
  PAYMENT_PENDING: 'Awaiting payment',
  PAID: 'New',
  PREPARING: 'Preparing',
  READY: 'Ready',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
}

export function StatusBadge({ status }) {
  return (
    <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${STATUS_STYLE[status] || STATUS_STYLE.COMPLETED}`}>
      {STATUS_TEXT[status] || status}
    </span>
  )
}
