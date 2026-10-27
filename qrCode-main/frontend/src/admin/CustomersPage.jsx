import { useCallback, useEffect, useState } from 'react'
import { createCampaign, getCampaigns, getCustomers, sendCampaign, updateCustomerPreferences } from '../api/staff'
import { ErrorNote, Field, PageLoader, btn, input } from '../components/ui'
import { useToast } from '../components/Toast'
import { formatDateTime } from '../lib/format'

/*
 * Customers (created automatically from orders) and marketing messages.
 * Marketing only goes to customers who opted in and did not opt out.
 * Order updates (preparing / ready) are separate and not marketing.
 */
export default function CustomersPage() {
  const toast = useToast()
  const [customers, setCustomers] = useState(null)
  const [campaigns, setCampaigns] = useState([])
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')

  const load = useCallback(() => {
    Promise.all([getCustomers(), getCampaigns()])
      .then(([c, camps]) => { setCustomers(c); setCampaigns(camps.sort((a, b) => b.id - a.id)); setError('') })
      .catch((e) => setError(e.message))
  }, [])
  useEffect(() => { load() }, [load])

  const toggleOptOut = async (customer) => {
    try {
      await updateCustomerPreferences({
        mobileNumber: customer.mobileNumber,
        smsOptIn: customer.smsOptIn,
        whatsappOptIn: customer.whatsappOptIn,
        marketingOptOut: !customer.marketingOptOut,
      })
      toast(customer.marketingOptOut ? 'Marketing allowed again' : 'Customer opted out of marketing')
      load()
    } catch (e) {
      toast(e.message, 'error')
    }
  }

  if (error) return <ErrorNote>{error}</ErrorNote>
  if (!customers) return <PageLoader label="Loading customers" />

  const q = search.trim().toLowerCase()
  const shown = customers.filter((c) => !q || (c.name || '').toLowerCase().includes(q) || c.mobileNumber.includes(q))
  const reachable = customers.filter((c) => !c.marketingOptOut && (c.smsOptIn || c.whatsappOptIn)).length

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-3xl font-semibold">Customers</h1>
        <p className="text-stone-500">{customers.length} customers · {reachable} agreed to receive offers</p>
      </div>

      <section className="rounded-3xl bg-white shadow-sm">
        <div className="p-5 pb-3">
          <input className={`${input} max-w-xs`} placeholder="Search name or mobile" value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
        <div className="overflow-x-auto">
          <table className="w-full min-w-[620px] text-sm">
            <thead className="border-y border-stone-200 text-left text-stone-500">
              <tr>
                <th className="px-5 py-2.5 font-medium">Name</th>
                <th className="px-3 py-2.5 font-medium">Mobile</th>
                <th className="px-3 py-2.5 font-medium">SMS offers</th>
                <th className="px-3 py-2.5 font-medium">WhatsApp offers</th>
                <th className="px-5 py-2.5 font-medium">Marketing</th>
              </tr>
            </thead>
            <tbody>
              {shown.map((c) => (
                <tr key={c.id} className="border-b border-stone-100">
                  <td className="px-5 py-3">{c.name || '-'}</td>
                  <td className="px-3 py-3">{c.mobileNumber}</td>
                  <td className="px-3 py-3">{c.smsOptIn ? 'Yes' : 'No'}</td>
                  <td className="px-3 py-3">{c.whatsappOptIn ? 'Yes' : 'No'}</td>
                  <td className="px-5 py-3">
                    <button className={c.marketingOptOut ? btn.ghost : btn.danger} onClick={() => toggleOptOut(c)}>
                      {c.marketingOptOut ? 'Opted out · allow' : 'Opt out'}
                    </button>
                  </td>
                </tr>
              ))}
              {shown.length === 0 && <tr><td colSpan="5" className="px-5 py-10 text-center text-stone-500">No customers yet.</td></tr>}
            </tbody>
          </table>
        </div>
      </section>

      <Campaigns campaigns={campaigns} reload={load} />
    </div>
  )
}

function Campaigns({ campaigns, reload }) {
  const toast = useToast()
  const [form, setForm] = useState({ name: '', message: '', channel: 'SMS' })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  const create = async (e) => {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      await createCampaign(form)
      setForm({ name: '', message: '', channel: 'SMS' })
      toast('Draft saved')
      reload()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  const send = async (campaign) => {
    if (!window.confirm(`Send "${campaign.name}" now to all opted-in customers? This cannot be undone.`)) return
    try {
      await sendCampaign(campaign.id)
      toast('Campaign sent')
      reload()
    } catch (e) {
      toast(e.message, 'error')
    }
  }

  return (
    <section className="rounded-3xl bg-white p-5 shadow-sm">
      <h2 className="font-semibold">Offers (SMS / WhatsApp)</h2>
      <p className="mt-1 text-sm text-stone-500">
        Needs the SMS / WhatsApp provider to be set up for this cafe (approved sender and templates). Sent only to customers who opted in.
      </p>

      <form onSubmit={create} className="mt-4 grid gap-3 sm:grid-cols-3">
        <Field label="Name"><input className={input} required maxLength={150} value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Field>
        <Field label="Channel">
          <select className={input} value={form.channel} onChange={(e) => setForm({ ...form, channel: e.target.value })}>
            <option value="SMS">SMS</option>
            <option value="WHATSAPP">WhatsApp</option>
            <option value="BOTH">Both</option>
          </select>
        </Field>
        <div className="sm:col-span-3">
          <Field label="Message"><textarea className={input} required rows={3} maxLength={2000} value={form.message} onChange={(e) => setForm({ ...form, message: e.target.value })} /></Field>
        </div>
        <div className="sm:col-span-3"><ErrorNote>{error}</ErrorNote></div>
        <div><button className={btn.primary} disabled={busy}>Save as draft</button></div>
      </form>

      <ul className="mt-6 divide-y divide-stone-100 text-sm">
        {campaigns.map((c) => (
          <li key={c.id} className="flex flex-wrap items-center gap-3 py-3">
            <div className="min-w-0 flex-1">
              <p className="font-medium">{c.name} <span className="text-stone-500">· {c.channel}</span></p>
              <p className="text-xs text-stone-500">{formatDateTime(c.createdAt)} · {c.status} · sent {c.sentCount}, failed {c.failedCount}</p>
            </div>
            {c.status === 'DRAFT' && <button className={btn.ghost} onClick={() => send(c)}>Send now</button>}
          </li>
        ))}
      </ul>
    </section>
  )
}
