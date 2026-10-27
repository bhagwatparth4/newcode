import { useEffect, useState } from 'react'
import { createKitchenUser,getCashEnabled,setCashEnabled, getPrepTime, setPrepTime } from '../api/staff'
import { ErrorNote, Field, btn, input } from '../components/ui'
import { useToast } from '../components/Toast'
import { useAdmin } from './AdminContext'
import { bulkChangePrices } from '../api/staff'
const MODE_TEXT = {
  SMALL_CAFE: 'Small cafe: you manage every order from this dashboard. There is no kitchen login.',
  LARGE_CAFE: 'Large cafe: the kitchen prepares orders on its own screen; you complete them here.',
  RESTAURANT: 'Restaurant: the kitchen prepares orders on its own screen; you complete them here.',
}

export default function SettingsPage() {
  const { businessType, hasKitchen } = useAdmin()
  const toast = useToast()
  const [minutes, setMinutes] = useState('')
  const [prepError, setPrepError] = useState('')

  useEffect(() => {
    getPrepTime().then((m) => setMinutes(String(m ?? ''))).catch((e) => setPrepError(e.message))
  }, [])

  const savePrep = async (e) => {
    e.preventDefault()
    setPrepError('')
    try {
      await setPrepTime(Number(minutes))
      toast('Preparation time saved')
    } catch (err) {
      setPrepError(err.message)
    }
  }

  return (
    <div className="max-w-2xl space-y-6">
      <h1 className="font-display text-3xl font-semibold">Settings</h1>

      <section className="rounded-3xl bg-white p-5 shadow-sm">
        <h2 className="font-semibold">Business mode</h2>
        <p className="mt-1 text-sm text-stone-600">{MODE_TEXT[businessType] || businessType}</p>
        <p className="mt-2 text-xs text-stone-500">Set by your software provider. Contact them to change it.</p>
      </section>

      <section className="rounded-3xl bg-white p-5 shadow-sm">
        <h2 className="font-semibold">Preparation time</h2>
        <p className="mt-1 text-sm text-stone-500">The estimate customers see after paying. Used for new orders.</p>
        <form onSubmit={savePrep} className="mt-4 flex items-end gap-2">
          <Field label="Minutes">
            <input className={`${input} w-32`} type="number" min="1" max="180" required value={minutes} onChange={(e) => setMinutes(e.target.value)} />
          </Field>
          <button className={btn.primary}>Save</button>
        </form>
        <div className="mt-3"><ErrorNote>{prepError}</ErrorNote></div>
      </section>
      <CashSetting />

      <BulkPriceSetting />

      {hasKitchen && <KitchenAccountForm />}
    </div>
  )
}

function KitchenAccountForm() {
  const toast = useToast()
  const [form, setForm] = useState({ name: '', email: '', password: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      await createKitchenUser(form)
      toast('Kitchen account created')
      setForm({ name: '', email: '', password: '' })
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="rounded-3xl bg-white p-5 shadow-sm">
      <h2 className="font-semibold">Create a kitchen login</h2>
      <p className="mt-1 text-sm text-stone-500">Kitchen staff can only see and prepare orders. They cannot see sales, change the menu or complete orders.</p>
      <form onSubmit={submit} className="mt-4 grid gap-3 sm:grid-cols-2">
        <Field label="Name"><input className={input} required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Field>
        <Field label="Email"><input className={input} required type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></Field>
        <Field label="Password" hint="At least 8 characters">
          <input className={input} required type="password" minLength={8} autoComplete="new-password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
        </Field>
        <div className="sm:col-span-2"><ErrorNote>{error}</ErrorNote></div>
        <div><button className={btn.primary} disabled={busy}>Create kitchen login</button></div>
      </form>
    </section>
  )
}



function CashSetting() {
  const toast = useToast()
  const [enabled, setEnabled] = useState(null)

  useEffect(() => {
    getCashEnabled().then((r) => setEnabled(r.cashEnabled)).catch(() => {})
  }, [])

  const toggle = async () => {
    try {
      const r = await setCashEnabled(!enabled)
      setEnabled(r.cashEnabled)
      toast(r.cashEnabled ? 'Cash payments turned on' : 'Cash payments turned off')
    } catch (e) {
      toast(e.message, 'error')
    }
  }

  if (enabled === null) return null

  return (
    <section className="rounded-3xl bg-white p-5 shadow-sm">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h2 className="font-semibold">Cash payments</h2>
          <p className="mt-1 text-sm text-stone-500">
            Let customers choose "Pay cash at counter". Coupons work only on online payment.
          </p>
        </div>
        <button className={enabled ? btn.primary : btn.ghost} onClick={toggle}>
          {enabled ? 'On' : 'Off'}
        </button>
      </div>
    </section>
  )
}

function BulkPriceSetting() {
  const toast = useToast()
  const [percent, setPercent] = useState('')
  const [busy, setBusy] = useState(false)

  const apply = async (sign) => {
    const p = Number(percent)
    const max = sign > 0 ? 200 : 90
    if (!p || p <= 0 || p > max) {
      toast(`Enter a percent between 1 and ${max}`, 'error')
      return
    }
    if (!window.confirm(`${sign > 0 ? 'Increase' : 'Decrease'} ALL menu prices by ${p}%?`)) return
    setBusy(true)
    try {
      const r = await bulkChangePrices(sign * p)
      toast(`${r.updated} item prices updated`)
      setPercent('')
    } catch (e) {
      toast(e.message, 'error')
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="rounded-3xl bg-white p-5 shadow-sm">
      <h2 className="font-semibold">Change all prices</h2>
      <p className="mt-1 text-sm text-stone-500">
        Raises or lowers every menu item by a percentage, rounded to the nearest rupee. Old orders and receipts keep their prices.
        Note: +10% then −10% does not give the original price back.
      </p>
      <div className="mt-4 flex flex-wrap items-end gap-2">
        <Field label="Percent (%)">
          <input className={`${input} w-32`} type="number" min="1" max="200" value={percent} onChange={(e) => setPercent(e.target.value)} />
        </Field>
        <button className={btn.primary} disabled={busy} onClick={() => apply(1)}>Increase</button>
        <button className={btn.ghost} disabled={busy} onClick={() => apply(-1)}>Decrease</button>
      </div>
    </section>
  )
}
