import { useCallback, useEffect, useState } from 'react'
import { getCoupons, saveCoupon, setCouponActive } from '../api/staff'
import { ErrorNote, Field, Modal, PageLoader, btn, input } from '../components/ui'
import { useToast } from '../components/Toast'
import { formatDateTime, money } from '../lib/format'

const toInput = (value) => (value ? String(value).slice(0, 16) : '')

// Coupons. Discounts are always recalculated by the server at checkout.
export default function CouponsPage() {
  const toast = useToast()
  const [coupons, setCoupons] = useState(null)
  const [error, setError] = useState('')
  const [editing, setEditing] = useState(null)

  const load = useCallback(() => {
    getCoupons().then((c) => { setCoupons(c.sort((a, b) => b.id - a.id)); setError('') }).catch((e) => setError(e.message))
  }, [])
  useEffect(() => { load() }, [load])

  const toggle = async (coupon) => {
    try {
      await setCouponActive(coupon.id, !coupon.active)
      load()
    } catch (e) {
      toast(e.message, 'error')
    }
  }

  if (error) return <ErrorNote>{error}</ErrorNote>
  if (!coupons) return <PageLoader label="Loading coupons" />

  const now = Date.now()

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="font-display text-3xl font-semibold">Coupons</h1>
          <p className="text-stone-500">Each successful payment uses one coupon use.</p>
        </div>
        <button
          className={btn.primary}
          onClick={() => setEditing({ code: '', discountType: 'PERCENTAGE', discountValue: '', minimumOrderAmount: '', maximumDiscountAmount: '', validFrom: '', validUntil: '', usageLimit: 100 })}
        >
          Create coupon
        </button>
      </div>

      <div className="overflow-x-auto rounded-3xl bg-white shadow-sm">
        <table className="w-full min-w-[760px] text-sm">
          <thead className="border-b border-stone-200 text-left text-stone-500">
            <tr>
              <th className="px-5 py-3 font-medium">Code</th>
              <th className="px-3 py-3 font-medium">Discount</th>
              <th className="px-3 py-3 font-medium">Min. order</th>
              <th className="px-3 py-3 font-medium">Valid</th>
              <th className="px-3 py-3 font-medium">Used</th>
              <th className="px-3 py-3 font-medium">Active</th>
              <th className="px-5 py-3" />
            </tr>
          </thead>
          <tbody>
            {coupons.length === 0 && <tr><td colSpan="7" className="px-5 py-10 text-center text-stone-500">No coupons yet.</td></tr>}
            {coupons.map((c) => {
              const expired = new Date(c.validUntil).getTime() < now
              return (
                <tr key={c.id} className="border-b border-stone-100">
                  <td className="px-5 py-3 font-semibold">{c.code}</td>
                  <td className="px-3 py-3">
                    {c.discountType === 'PERCENTAGE' ? `${Number(c.discountValue)}%` : money(c.discountValue)}
                    {c.maximumDiscountAmount ? <span className="block text-xs text-stone-500">up to {money(c.maximumDiscountAmount)}</span> : null}
                  </td>
                  <td className="px-3 py-3">{c.minimumOrderAmount ? money(c.minimumOrderAmount) : '-'}</td>
                  <td className="px-3 py-3">
                    {formatDateTime(c.validFrom)}<span className="block text-xs text-stone-500">to {formatDateTime(c.validUntil)}{expired ? ' (expired)' : ''}</span>
                  </td>
                  <td className="px-3 py-3">{c.usedCount} / {c.usageLimit}</td>
                  <td className="px-3 py-3">
                    <button onClick={() => toggle(c)} className={`rounded-full px-3 py-1 text-xs font-semibold ${c.active ? 'bg-mint/40 text-mint-deep' : 'bg-stone-100 text-stone-500'}`}>
                      {c.active ? 'Active' : 'Off'}
                    </button>
                  </td>
                  <td className="px-5 py-3 text-right">
                    <button className="font-medium underline-offset-2 hover:underline" onClick={() => setEditing(c)}>Edit</button>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      {editing && <CouponForm coupon={editing} onClose={() => setEditing(null)} onSaved={() => { setEditing(null); toast('Coupon saved'); load() }} />}
    </div>
  )
}

function CouponForm({ coupon, onClose, onSaved }) {
  const [form, setForm] = useState({
    ...coupon,
    validFrom: toInput(coupon.validFrom),
    validUntil: toInput(coupon.validUntil),
    minimumOrderAmount: coupon.minimumOrderAmount ?? '',
    maximumDiscountAmount: coupon.maximumDiscountAmount ?? '',
  })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }))
  const optional = (v) => (v === '' || v === null ? null : Number(v))

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      await saveCoupon({
        id: form.id,
        code: form.code.trim().toUpperCase(),
        discountType: form.discountType,
        discountValue: Number(form.discountValue),
        minimumOrderAmount: optional(form.minimumOrderAmount),
        maximumDiscountAmount: form.discountType === 'PERCENTAGE' ? optional(form.maximumDiscountAmount) : null,
        validFrom: `${form.validFrom}:00`,
        validUntil: `${form.validUntil}:00`,
        usageLimit: Number(form.usageLimit),
      })
      onSaved()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal open onClose={onClose} title={form.id ? 'Edit coupon' : 'Create coupon'}>
      <form onSubmit={submit} className="space-y-4">
        <Field label="Code"><input className={`${input} uppercase`} required maxLength={50} value={form.code} onChange={set('code')} /></Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Type">
            <select className={input} value={form.discountType} onChange={set('discountType')}>
              <option value="PERCENTAGE">Percentage (%)</option>
              <option value="FIXED">Fixed amount (₹)</option>
            </select>
          </Field>
          <Field label={form.discountType === 'PERCENTAGE' ? 'Percent off' : 'Amount off (₹)'}>
            <input className={input} required type="number" min="0.01" step="0.01" max={form.discountType === 'PERCENTAGE' ? 100 : undefined} value={form.discountValue} onChange={set('discountValue')} />
          </Field>
          <Field label="Minimum order (₹, optional)"><input className={input} type="number" min="0" step="0.01" value={form.minimumOrderAmount} onChange={set('minimumOrderAmount')} /></Field>
          {form.discountType === 'PERCENTAGE' && (
            <Field label="Maximum discount (₹, optional)"><input className={input} type="number" min="1" step="0.01" value={form.maximumDiscountAmount} onChange={set('maximumDiscountAmount')} /></Field>
          )}
          <Field label="Valid from"><input className={input} required type="datetime-local" value={form.validFrom} onChange={set('validFrom')} /></Field>
          <Field label="Valid until"><input className={input} required type="datetime-local" value={form.validUntil} onChange={set('validUntil')} /></Field>
          <Field label="Total uses allowed"><input className={input} required type="number" min="1" step="1" value={form.usageLimit} onChange={set('usageLimit')} /></Field>
        </div>
        <ErrorNote>{error}</ErrorNote>
        <button className={`${btn.primary} w-full`} disabled={busy}>Save coupon</button>
      </form>
    </Modal>
  )
}
