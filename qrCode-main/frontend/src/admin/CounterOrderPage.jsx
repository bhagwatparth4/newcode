import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { createCounterOrder, getOrdersBetween, getTables } from '../api/staff'
import { getAddons, getCategories, getMenuItems } from '../api/customer'
import { ErrorNote, PageLoader, VegMark, input } from '../components/ui'
import { useToast } from '../components/Toast'
import { money } from '../lib/format'
import { useNow } from '../lib/useNow'
import { printBill, printKot } from '../lib/print'
import AddonSheet from '../customer/AddonSheet'
import { useAdmin } from './AdminContext'
import { readCleared, tableVisit } from './TableStatusPage'

const PAY_MODES = [['CASH', '💵 Cash'], ['UPI', '📱 UPI'], ['CARD', '💳 Card']]
const TILE_BG = ['#E8C89A', '#C9A07D', '#D79A6C', '#B89A80', '#D8C2A2', '#E1CDB0']
const TAKEN_KEY = 'rh_taken_by'
const NAMES_KEY = 'rh_taken_names'
const PRINT_KEY = 'rh_pos_print'
const readJson = (key, fallback) => { try { return JSON.parse(localStorage.getItem(key)) ?? fallback } catch { return fallback } }
const saveJson = (key, value) => { try { localStorage.setItem(key, JSON.stringify(value)) } catch { /* optional */ } }
const pad = (n) => String(n).padStart(2, '0')
const iso = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
const newKey = () => (crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random()}`)

// Owner creates counter / phone orders. Treated as CASH; payMode is only a label.
export default function CounterOrderPage() {
  const { liveVersion } = useAdmin()
  const toast = useToast()
  const now = useNow(30000)
  const [items, setItems] = useState(null)
  const [categories, setCategories] = useState([])
  const [addonMap, setAddonMap] = useState({})
  const [tables, setTables] = useState([])
  const [todayOrders, setTodayOrders] = useState([])
  const [error, setError] = useState('')

  const [search, setSearch] = useState('')
  const [cat, setCat] = useState('ALL')
  const [lines, setLines] = useState([])
  const [customizing, setCustomizing] = useState(null)
  const [tableId, setTableId] = useState(null) // null = counter
  const [tableMenu, setTableMenu] = useState(false)
  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')
  const [note, setNote] = useState('')
  const [showNote, setShowNote] = useState(false)
  const [showCustomer, setShowCustomer] = useState(false)
  const [payMode, setPayMode] = useState('CASH')
  const [print, setPrint] = useState(() => readJson(PRINT_KEY, { kot: false, bill: false }))
  const [busy, setBusy] = useState(false)
  const [takenBy, setTakenBy] = useState(() => localStorage.getItem(TAKEN_KEY) || 'Owner')
  const [recentNames, setRecentNames] = useState(() => readJson(NAMES_KEY, ['Owner']))
  const [editingName, setEditingName] = useState(false)
  const idemKey = useRef(newKey())

  useEffect(() => {
    Promise.all([getMenuItems(), getCategories(), getAddons().catch(() => []), getTables()])
      .then(([m, c, a, t]) => {
        setItems(m)
        setCategories(c)
        const map = {}
        a.forEach((g) => { (map[g.menuItemId] = map[g.menuItemId] || []).push(g) })
        setAddonMap(map)
        setTables(t.filter((x) => x.active !== false))
      })
      .catch((e) => setError(e.message))
  }, [])

  const loadOrders = useCallback(() => {
    const start = new Date()
    start.setHours(0, 0, 0, 0)
    getOrdersBetween(iso(start), iso(new Date()))
      .then((o) => setTodayOrders(o.filter((x) => x.orderType === 'TABLE' && x.status !== 'CANCELLED' && x.status !== 'PAYMENT_PENDING')))
      .catch(() => {})
  }, [])
  useEffect(() => { loadOrders() }, [loadOrders, liveVersion])

  const usedCats = useMemo(() => {
    const used = new Set((items || []).map((i) => i.categoryId))
    return categories.filter((c) => used.has(c.id))
  }, [items, categories])

  const shown = useMemo(() => {
    const q = search.trim().toLowerCase()
    return (items || []).filter((i) => (cat === 'ALL' || i.categoryId === cat) && (!q || i.name.toLowerCase().includes(q)))
  }, [items, cat, search])

  const rememberName = (n) => {
    const clean = (n || '').trim()
    if (!clean) return
    const next = [clean, ...recentNames.filter((x) => x !== clean)].slice(0, 5)
    setRecentNames(next)
    saveJson(NAMES_KEY, next)
    try { localStorage.setItem(TAKEN_KEY, clean) } catch { /* optional */ }
  }

  const togglePrint = (key) => {
    const next = { ...print, [key]: !print[key] }
    setPrint(next)
    saveJson(PRINT_KEY, next)
  }

  const addLine = (line) => {
    setLines((cur) => {
      const i = cur.findIndex((l) => l.key === line.key)
      return i >= 0 ? cur.map((l, j) => (j === i ? { ...l, qty: Math.min(50, l.qty + line.qty) } : l)) : [...cur, line]
    })
    setCustomizing(null)
  }
  const tapItem = (item) => {
    if (!item.available) return
    if (addonMap[item.id]) return setCustomizing(item)
    addLine({ key: `${item.id}:`, menuItemId: item.id, name: item.name, addonIds: [], addonsText: '', unitPrice: Number(item.price), qty: 1 })
  }
  const setQty = (key, qty) => setLines((cur) => cur.map((l) => (l.key === key ? { ...l, qty: Math.min(50, qty) } : l)).filter((l) => l.qty > 0))
  const countFor = (id) => lines.filter((l) => l.menuItemId === id).reduce((s, l) => s + l.qty, 0)

  const cleared = readCleared()
  const visitOf = (t) => tableVisit(todayOrders.filter((o) => o.tableNumber === t.tableNumber), cleared[t.tableNumber] || 0, now)

  const chooseTable = (t) => {
    const v = visitOf(t)
    if (v && tableId !== t.id) {
      const mins = Math.floor((now - v.start) / 60000)
      if (!window.confirm(`${t.tableNumber} is occupied (${mins} min, ${v.count} order${v.count > 1 ? 's' : ''}). Add this order to ${t.tableNumber}?`)) return
    }
    setTableId(t.id)
    setTableMenu(false)
  }

  const total = lines.reduce((s, l) => s + l.unitPrice * l.qty, 0)
  const count = lines.reduce((s, l) => s + l.qty, 0)
  const tableNumber = tables.find((t) => t.id === tableId)?.tableNumber
  const place = tableId == null ? 'Counter' : tableNumber
  const by = takenBy.trim() || 'Owner'

  const reset = () => {
    setLines([]); setName(''); setPhone(''); setNote('')
    setShowNote(false); setShowCustomer(false); setTableId(null); setPayMode('CASH')
  }

  const placeOrder = async (paidNow) => {
    if (phone && !/^[6-9]\d{9}$/.test(phone)) return toast('Enter a valid 10-digit mobile number, or leave it empty.', 'error')
    setBusy(true)
    try {
      const order = await createCounterOrder({
        tableId, nickname: name.trim(), mobile: phone, cookingNote: note.trim() || null,
        payMode, paidNow, takenBy: by, idempotencyKey: idemKey.current,
        items: lines.map((l) => ({ menuItemId: l.menuItemId, quantity: l.qty, addonIds: l.addonIds })),
      })
      rememberName(by)
      toast(`✅ Order ${order.orderNumber} placed · ${place}`)

      // The first response is created before "paid" is saved, so set it here for the printout
      const printable = { ...order, paymentMethod: 'CASH', orderPaymentStatus: paidNow ? 'PAID' : 'PENDING', counterPaymentMode: payMode }
      if (print.kot) printKot(printable)
      if (print.bill && paidNow) setTimeout(() => printBill(printable), print.kot ? 1500 : 0) // never a "paid" bill without payment

      reset()
      idemKey.current = newKey()
      loadOrders()
    } catch (e) {
      toast(e.message, 'error')
    } finally {
      setBusy(false)
    }
  }

  if (error) return <ErrorNote>{error}</ErrorNote>
  if (!items) return <PageLoader label="Loading menu" />

  const catButton = (c, i, vertical) => {
    const on = cat === c.id
    const thumb = c.id === 'ALL'
      ? <span className="flex h-full w-full items-center justify-center bg-stone-100 text-lg">🍽️</span>
      : c.imageUrl
        ? <img src={c.imageUrl} alt="" className="h-full w-full object-cover" />
        : <span className="flex h-full w-full items-center justify-center bg-[#EEE3D6] font-semibold text-[#4A3B30]">{c.name.charAt(0).toUpperCase()}</span>
    return (
      <button key={c.id} onClick={() => setCat(c.id)}
        className={vertical
          ? `flex w-full flex-col items-center gap-1 rounded-2xl px-1 py-2 text-center text-[11px] font-medium transition ${on ? 'bg-espresso text-mint' : 'text-stone-600 hover:bg-stone-100'}`
          : `flex shrink-0 items-center gap-2 rounded-full py-1 pl-1 pr-3 text-sm font-medium ${on ? 'bg-espresso text-mint' : 'bg-white text-stone-600 ring-1 ring-stone-200'}`}>
        <span className={`block shrink-0 overflow-hidden rounded-full ${vertical ? 'h-10 w-10' : 'h-7 w-7'} ${on ? 'ring-2 ring-mint' : ''}`}>{thumb}</span>
        <span className={vertical ? 'line-clamp-2 leading-tight' : ''}>{c.name}</span>
      </button>
    )
  }

  const allCats = [{ id: 'ALL', name: 'All' }, ...usedCats]

  return (
    <div className="grid gap-3 md:h-[calc(100dvh-11rem)] md:grid-cols-[5.5rem_1fr_320px] lg:h-[calc(100dvh-4rem)] lg:grid-cols-[5.5rem_1fr_360px]">
      {/* Category rail (tablet / desktop) */}
      <nav className="hidden min-h-0 flex-col gap-1.5 overflow-y-auto rounded-3xl bg-white p-2 shadow-sm ring-1 ring-stone-200 md:flex">
        {allCats.map((c, i) => catButton(c, i, true))}
      </nav>

      {/* Menu */}
      <section className="min-h-0 md:overflow-y-auto md:pr-1">
        <div className="flex items-center gap-3">
          <h1 className="font-display text-2xl font-semibold">New order</h1>
          <div className="ml-auto w-full max-w-xs">
            <input className={input} placeholder="🔍 Search dishes" value={search} onChange={(e) => setSearch(e.target.value)} />
          </div>
        </div>

        {/* Category chips (phone) */}
        <div className="no-scrollbar mt-3 flex gap-2 overflow-x-auto pb-1 md:hidden">
          {allCats.map((c, i) => catButton(c, i, false))}
        </div>

        <div className="mt-3 grid grid-cols-2 gap-2.5 pb-4 lg:grid-cols-3 xl:grid-cols-4">
          {shown.map((item, i) => {
            const n = countFor(item.id)
            return (
              <button key={item.id} onClick={() => tapItem(item)} disabled={!item.available}
                className={`relative h-28 overflow-hidden rounded-2xl text-left transition active:scale-[0.97] ${n ? 'ring-[3px] ring-mint-deep' : ''} ${item.available ? '' : 'opacity-50'}`}
                style={{ background: TILE_BG[i % TILE_BG.length] }}>
                {item.imageUrl && <img src={item.imageUrl} alt="" className="absolute inset-0 h-full w-full object-cover" />}
                {!item.imageUrl && <span className="absolute left-3 top-2 text-3xl font-semibold text-white/60">{item.name.charAt(0).toUpperCase()}</span>}
                <span className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-espresso/90 via-espresso/50 to-transparent px-2.5 pb-2 pt-6 text-white">
                  <span className="flex items-center gap-1.5 text-sm font-semibold leading-tight">
                    <span className="rounded-sm bg-white p-px"><VegMark type={item.dietaryType} /></span>
                    <span className="truncate">{item.name}</span>
                  </span>
                  <span className="text-xs text-white/85">
                    {money(item.price)}{!item.available ? ' · Sold out' : addonMap[item.id] ? ' · custom' : ''}
                  </span>
                </span>
                {item.available && (
                  n > 0
                    ? <span className="absolute right-2 top-2 flex h-7 min-w-7 items-center justify-center rounded-full bg-mint-deep px-2 text-sm font-bold text-white shadow">{n}</span>
                    : <span className="absolute right-2 top-2 flex h-7 w-7 items-center justify-center rounded-full bg-white text-lg font-semibold text-espresso shadow">+</span>
                )}
              </button>
            )
          })}
          {shown.length === 0 && <p className="col-span-full py-10 text-center text-sm text-stone-500">No dishes match.</p>}
        </div>
      </section>

      {/* Bill */}
      <aside className="flex min-h-0 flex-col overflow-hidden rounded-3xl bg-white shadow-sm ring-1 ring-stone-200">
        {/* Where + who */}
        <div className="space-y-2.5 border-b border-stone-100 p-3">
          <div className="relative grid grid-cols-2 gap-1.5">
            <button onClick={() => { setTableId(null); setTableMenu(false) }}
              className={`rounded-xl py-2 text-sm font-semibold ${tableId == null ? 'bg-espresso text-mint' : 'bg-white text-stone-600 ring-1 ring-stone-200'}`}>
              🛍️ Counter
            </button>
            <button onClick={() => setTableMenu((v) => !v)}
              className={`rounded-xl py-2 text-sm font-semibold ${tableId != null ? 'bg-espresso text-mint' : 'bg-white text-stone-600 ring-1 ring-stone-200'}`}>
              🪑 {tableNumber || 'Table'} ▾
            </button>
            {tableMenu && (
              <div className="absolute inset-x-0 top-full z-20 mt-1.5 grid grid-cols-3 gap-1.5 rounded-2xl bg-white p-2 shadow-xl ring-1 ring-stone-200">
                {tables.map((t) => {
                  const v = visitOf(t)
                  return (
                    <button key={t.id} onClick={() => chooseTable(t)}
                      className={`rounded-xl py-2 text-sm font-semibold ${tableId === t.id ? 'bg-espresso text-mint' : v ? 'bg-red-50 text-red-700 ring-1 ring-red-200' : 'bg-stone-50 text-stone-700 ring-1 ring-stone-200'}`}>
                      {t.tableNumber}
                      {v && <span className="block text-[10px] font-medium">{Math.floor((now - v.start) / 60000)} min</span>}
                    </button>
                  )
                })}
              </div>
            )}
          </div>

          <div className="flex flex-wrap items-center gap-1.5">
            <span className="text-xs text-stone-500">🧑‍💼 Order by</span>
            {recentNames.map((n) => (
              <button key={n} onClick={() => { setTakenBy(n); rememberName(n); setEditingName(false) }}
                className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${takenBy === n && !editingName ? 'bg-espresso text-mint' : 'bg-stone-100 text-stone-600'}`}>
                {n}
              </button>
            ))}
            {editingName ? (
              <div className="w-28">
                <input autoFocus className={input} placeholder="Name" maxLength={100} value={takenBy}
                  onChange={(e) => setTakenBy(e.target.value)}
                  onBlur={() => { rememberName(takenBy); setEditingName(false) }}
                  onKeyDown={(e) => e.key === 'Enter' && e.currentTarget.blur()} />
              </div>
            ) : (
              <button onClick={() => { setTakenBy(''); setEditingName(true) }}
                className="rounded-full border border-dashed border-stone-300 px-2.5 py-0.5 text-xs text-stone-500" aria-label="Add a name">✎</button>
            )}
          </div>
        </div>

        {/* Items (only this part scrolls) */}
        <div className="flex min-h-[9rem] flex-1 flex-col overflow-y-auto p-3">
          <div className="mb-2 flex items-center justify-between text-xs text-stone-500">
            <span>Bill · {count} item{count === 1 ? '' : 's'}</span>
            {lines.length > 0 && <button onClick={reset} className="font-medium text-red-600">Clear</button>}
          </div>

          {lines.length === 0 ? (
            <div className="flex flex-1 flex-col items-center justify-center py-6 text-center text-stone-400">
              <span className="text-3xl">🧾</span>
              <p className="mt-2 text-sm">Tap dishes to add them</p>
            </div>
          ) : (
            <ul className="space-y-2.5">
              {lines.map((l) => (
                <li key={l.key} className="flex items-center gap-2">
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-semibold">{l.name}</p>
                    {l.addonsText && <p className="truncate text-xs text-amber-700">+ {l.addonsText}</p>}
                  </div>
                  <div className="flex items-center rounded-full bg-stone-100 text-sm">
                    <button className="h-7 w-7 rounded-full font-semibold hover:bg-stone-200" onClick={() => setQty(l.key, l.qty - 1)} aria-label="Decrease">−</button>
                    <span className="w-5 text-center font-semibold">{l.qty}</span>
                    <button className="h-7 w-7 rounded-full font-semibold hover:bg-stone-200" onClick={() => setQty(l.key, l.qty + 1)} aria-label="Increase">+</button>
                  </div>
                  <span className="w-16 text-right text-sm font-semibold tabular-nums">{money(l.unitPrice * l.qty)}</span>
                </li>
              ))}
            </ul>
          )}

          {lines.length > 0 && (
            <div className="mt-3 space-y-2">
              {showNote && <input className={input} placeholder="📝 Cooking note" maxLength={50} value={note} onChange={(e) => setNote(e.target.value)} />}
              {showCustomer && (
                <div className="grid grid-cols-2 gap-2">
                  <input className={input} placeholder="Customer name" maxLength={100} value={name} onChange={(e) => setName(e.target.value)} />
                  <input className={input} placeholder="Phone" inputMode="numeric" value={phone} onChange={(e) => setPhone(e.target.value.replace(/\D/g, '').slice(0, 10))} />
                </div>
              )}
              {(!showNote || !showCustomer) && (
                <p className="flex gap-3 text-xs font-semibold text-mint-deep">
                  {!showNote && <button onClick={() => setShowNote(true)}>+ Note</button>}
                  {!showCustomer && <button onClick={() => setShowCustomer(true)}>+ Customer details</button>}
                </p>
              )}
            </div>
          )}

          {lines.length > 0 && (
            <div className="mt-auto flex justify-between border-t border-dashed border-stone-200 pt-2 text-base font-semibold">
              <span>Total</span>
              <span className="tabular-nums">{money(total)}</span>
            </div>
          )}
        </div>

        {/* Payment + place (always visible) */}
        <div className="space-y-2 border-t border-stone-100 bg-stone-50/70 p-3">
          <div className="grid grid-cols-3 gap-1.5">
            {PAY_MODES.map(([key, label]) => (
              <button key={key} onClick={() => setPayMode(key)}
                className={`rounded-xl py-2 text-xs font-semibold ${payMode === key ? 'bg-espresso text-mint' : 'bg-white text-stone-600 ring-1 ring-stone-200'}`}>
                {label}
              </button>
            ))}
          </div>
          <div className="grid grid-cols-2 gap-1.5">
            {[['kot', '🖨️ KOT'], ['bill', '🧾 Bill']].map(([key, label]) => (
              <button key={key} onClick={() => togglePrint(key)}
                className="flex items-center gap-2 rounded-xl bg-white px-2.5 py-1.5 text-xs font-medium text-stone-600 ring-1 ring-stone-200">
                <span className={`relative h-4 w-7 rounded-full transition ${print[key] ? 'bg-mint-deep' : 'bg-stone-300'}`}>
                  <span className={`absolute top-0.5 h-3 w-3 rounded-full bg-white transition ${print[key] ? 'left-[14px]' : 'left-0.5'}`} />
                </span>
                {label}
              </button>
            ))}
          </div>
          <div className="grid grid-cols-[1fr_1.7fr] gap-1.5">
            <button onClick={() => placeOrder(false)} disabled={busy || lines.length === 0}
              className="rounded-2xl bg-white py-3 text-sm font-semibold text-amber-800 ring-1 ring-stone-200 disabled:opacity-40">
              ⏳ Pay later
            </button>
            <button onClick={() => placeOrder(true)} disabled={busy || lines.length === 0}
              className="rounded-2xl bg-mint-deep py-3 text-sm font-semibold text-white shadow-[0_10px_20px_-12px_rgba(15,110,86,0.9)] transition active:scale-[0.98] disabled:opacity-40">
              {busy ? 'Placing…' : `✓ Paid · Place ${money(total)}`}
            </button>
          </div>
          <p className="text-center text-[11px] text-stone-400">{place} · by {by}</p>
        </div>
      </aside>

      {customizing && (
        <AddonSheet item={customizing} groups={addonMap[customizing.id] || []} onClose={() => setCustomizing(null)} onAdd={addLine} />
      )}
    </div>
  )
}