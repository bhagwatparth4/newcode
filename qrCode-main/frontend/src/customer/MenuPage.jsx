import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { BRAND } from '../config/brand'
import { useNow } from '../lib/useNow'
// import { getCafeStatus, getCategories, getMenuItems, getOrderDetails, resolveQr } from '../api/customer'
import { Logo, PageLoader, VegMark } from '../components/ui'
import { money, placeLabel } from '../lib/format'
import { feedbackReadyAt, isFeedbackSkipped, markNudged, wasNudged } from '../lib/feedback'
import {
  getCart, getDiet, getNickname, getQr,
  saveCart, saveDiet, saveNickname, saveQr, getMyOrders,getCustomCart, saveCustomCart
} from '../lib/customerSession'
import ScanPrompt from './ScanPrompt'
import WelcomeStep from './WelcomeStep'
import ItemSheet from './ItemSheet'
import { useRealtime } from '../lib/useRealtime'
import StatusPopup from '../components/StatusPopup'
import ClosedScreen from './ClosedScreen'
import {getAddons, getBestsellers, getCafeStatus, getCategories, getMenuItems, getOffer, getOrderDetails, resolveQr ,getPaymentOptions, sendTableRequest} from '../api/customer'
import AddonSheet from './AddonSheet'
const CASH_PAID_MESSAGE = 'Cash payment received. Thank you!'
const DIETS = [
  { value: 'ALL', label: 'All' },
  { value: 'VEG', label: 'Veg' },
  { value: 'NON_VEG', label: 'Non-veg' },
]
const TILE_BG = ['bg-[#FAEEDA]', 'bg-[#FBEAF0]', 'bg-[#EEE3D6]', 'bg-[#E1F5EE]', 'bg-[#FAECE7]']
const ACTIVE = ['PAID', 'PREPARING', 'READY']

/*
 * Customer flow step 1-3:  QR  ->  nickname  ->  menu
 * The QR token in the URL (/menu?qr=...) is checked by the backend, which is
 * the only source of truth for which table/counter this is.
 */
export default function MenuPage() {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const qrParam = params.get('qr')

  const [qr, setQr] = useState(getQr)
  const [qrError, setQrError] = useState('')
  const [nickname, setNickname] = useState(getNickname)

  useEffect(() => {
    if (!qrParam) return
    let cancelled = false
    resolveQr(qrParam)
      .then((ctx) => {
        if (cancelled) return
        if (!ctx?.valid) {
          setQrError('This QR code is not active right now. Please ask the staff for help.')
          return
        }
        const next = { qrToken: qrParam, orderType: ctx.orderType, tableNumber: ctx.tableNumber }
        saveQr(next)
        setQr(next)
        navigate('/menu', { replace: true })
      })
      .catch((e) => !cancelled && setQrError(e.status === 404 ? 'This QR code is not valid. Please scan the code on your table or at the counter.' : e.message))
    return () => {
      cancelled = true
    }
  }, [qrParam, navigate])

  if (qrError) return <ScanPrompt message={qrError} />
  if (qrParam && qr?.qrToken !== qrParam) return <PageLoader label="Finding your table" />
  if (!qr) return <ScanPrompt />

  if (!nickname) {
    return (
      <WelcomeStep
        qr={qr}
        onDone={(name) => {
          saveNickname(name)
          setNickname(name)
        }}
      />
    )
  }

  return <Menu qr={qr} nickname={nickname} />
}

function Menu({ qr, nickname }) {
  const navigate = useNavigate()
  const [items, setItems] = useState(null)
  const [categories, setCategories] = useState([])
  const [error, setError] = useState('')
  const [diet, setDiet] = useState(getDiet)
  const [search, setSearch] = useState('')
  const [category, setCategory] = useState('ALL')
  const [cart, setCart] = useState(getCart)
    const [custom, setCustom] = useState(getCustomCart)
    const [addonMap, setAddonMap] = useState({})   // menuItemId -> groups
    const [customizing, setCustomizing] = useState(null)
  const [openItem, setOpenItem] = useState(null)
  const [popup, setPopup] = useState(null)
  const [closed, setClosed] = useState(false)
  const [refreshKey, setRefreshKey] = useState(0)
  const [sheet, setSheet] = useState(null) // 'orders' | 'menu' | null
  const [offer, setOffer] = useState(null)
    const [businessType, setBusinessType] = useState('SMALL_CAFE')
  const myOrders = getMyOrders()


    const feedbackDue = useFeedbackDue(myOrders, refreshKey)
    const [nudge, setNudge] = useState(false)

    // Show the bubble once per order, then hide it after 2 minutes
    useEffect(() => {
      if (!feedbackDue || wasNudged(feedbackDue.o.token)) return undefined
      markNudged(feedbackDue.o.token)
      setNudge(true)
      const id = setTimeout(() => setNudge(false), 120000)
      return () => clearTimeout(id)
    }, [feedbackDue?.o.token])

  useEffect(() => {
    getCafeStatus().then((s) => setClosed(s?.acceptingOrders === false)).catch(() => {})
    getOffer().then((o) => setOffer(o?.code ? o : null)).catch(() => {})
        getPaymentOptions().then((o) => o?.businessType && setBusinessType(o.businessType)).catch(() => {})
  }, [])

  useRealtime({
    topics: myOrders.map((o) => `/topic/customer/orders/${o.token}`),
    onMessage: (_topic, event) => {
      const isPaymentUpdate = event?.message === CASH_PAID_MESSAGE
      if (event?.orderStatus && event.orderStatus !== 'COMPLETED' && !isPaymentUpdate) {
        setPopup({ ...event, key: Date.now() })
      }
      setRefreshKey((k) => k + 1)
    },
  })

    useEffect(() => {
      Promise.all([getMenuItems(), getCategories(), getBestsellers().catch(() => [])])
        .then(([menuItems, cats, best]) => {
          const rank = new Map((best || []).map((id, i) => [id, i]))
          setItems(menuItems.map((i) => ({ ...i, bestseller: rank.has(i.id), bestRank: rank.get(i.id) })))
          setCategories(cats)
        })
        .catch((e) => setError(e.message))
    }, [])

      const updateCart = (itemId, quantity) => {
        setCart((current) => {
          const next = { ...current }
          if (quantity <= 0) delete next[itemId]
          else next[itemId] = Math.min(50, quantity)
          saveCart(next)
          return next
        })
      }

      useEffect(() => {
        getAddons()
          .then((list) => {
            const map = {}
            list.forEach((g) => { (map[g.menuItemId] = map[g.menuItemId] || []).push(g) })
            setAddonMap(map)
          })
          .catch(() => {})
      }, [])

      const addCustom = (line) => {
        setCustom((cur) => {
          const i = cur.findIndex((l) => l.key === line.key)
          const next = i >= 0
            ? cur.map((l, j) => (j === i ? { ...l, qty: Math.min(50, l.qty + line.qty) } : l))
            : [...cur, line]
          saveCustomCart(next)
          return next
        })
        setCustomizing(null)
      }

      // "−" on a customised item removes one from its most recently added line
      const removeOneCustom = (itemId) => {
        setCustom((cur) => {
          const i = cur.map((l) => l.menuItemId).lastIndexOf(itemId)
          if (i < 0) return cur
          const next = cur.map((l, j) => (j === i ? { ...l, qty: l.qty - 1 } : l)).filter((l) => l.qty > 0)
          saveCustomCart(next)
          return next
        })
      }

      const qtyFor = (item) => (addonMap[item.id]
        ? custom.filter((l) => l.menuItemId === item.id).reduce((s, l) => s + l.qty, 0)
        : cart[item.id] || 0)

      const changeQty = (item, q) => {
        if (!addonMap[item.id]) return updateCart(item.id, q)
        if (q > qtyFor(item)) setCustomizing(item)
        else removeOneCustom(item.id)
      }

      const openItemFor = (item) => (addonMap[item.id] ? setCustomizing(item) : setOpenItem(item))



  const chooseDiet = (value) => {
    setDiet(value)
    saveDiet(value)
  }

  const visible = useMemo(() => {
    if (!items) return []
    const q = search.trim().toLowerCase()
    return items.filter(
      (item) =>
        (diet === 'ALL' || item.dietaryType === diet) &&
        (category === 'ALL' || item.categoryId === category) &&
        (!q || item.name.toLowerCase().includes(q) || (item.description || '').toLowerCase().includes(q)),
    )
  }, [items, diet, category, search])

  const groups = useMemo(() => {
    const order = categories.map((c) => c.id)
    const map = new Map()
    for (const item of visible) {
      if (!map.has(item.categoryId)) map.set(item.categoryId, { name: item.categoryName, items: [] })
      map.get(item.categoryId).items.push(item)
    }
    return [...map.entries()]
      .sort((a, b) => order.indexOf(a[0]) - order.indexOf(b[0]))
      .map(([id, group]) => ({ id, ...group, items: group.items.sort((a, b) => Number(b.available) - Number(a.available)) }))
  }, [visible, categories])

  const usedCategoryIds = new Set((items || []).map((i) => i.categoryId))
  const chipCategories = categories.filter((c) => usedCategoryIds.has(c.id))
  // Shown only once the backend sends a "bestseller" flag (Step 4)
  const loved = (items || []).filter((i) => i.bestseller && i.available && (diet === 'ALL' || i.dietaryType === diet)).sort((a, b) => a.bestRank - b.bestRank)
  const showExtras = category === 'ALL' && !search.trim()

   const customCount = custom.reduce((s, l) => s + l.qty, 0)
   const cartCount = Object.values(cart).reduce((a, b) => a + b, 0) + customCount
   const cartTotal = (items || []).reduce((sum, item) => sum + (cart[item.id] || 0) * Number(item.price), 0)
     + custom.reduce((s, l) => s + l.unitPrice * l.qty, 0)

  const jumpTo = (id) => {
    setSheet(null)
    setCategory('ALL')
    setTimeout(() => document.getElementById(`cat-${id}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 60)
  }

  if (closed) return <ClosedScreen />

  return (
    <div className="mx-auto min-h-dvh max-w-2xl bg-[#F7F3EE] pb-48">
      <StatusPopup popup={popup} onClose={() => setPopup(null)} />

      {/* Header */}
      <header className="rounded-b-3xl bg-espresso px-4 pb-5 pt-4 text-paper">
        <div className="flex items-center gap-3">
          <Logo size={44} />
          <div className="min-w-0 flex-1">
            <p className="font-display text-lg font-semibold leading-tight">{BRAND.name}</p>
            <p className="truncate text-xs text-paper/70">{placeLabel(qr.orderType, qr.tableNumber)} · Hi {nickname}</p>
          </div>
                    {myOrders.length > 0 && (
                      <button onClick={() => setSheet('orders')} className="relative shrink-0 rounded-xl bg-mint px-3 py-2 text-sm font-semibold text-espresso">
                        🧾 Orders ({myOrders.length})
                        {feedbackDue && <span className="absolute -right-1 -top-1 h-3 w-3 rounded-full border-2 border-espresso bg-amber-400" />}
                      </button>
                    )}
        </div>

                 {nudge && feedbackDue && (
                   <div className="mt-3 flex justify-end">
                     <div className="relative w-56 rounded-2xl bg-white p-3 text-espresso shadow-xl">
                       <span className="absolute -top-1.5 right-8 h-3 w-3 rotate-45 bg-white" />
                       <p className="font-semibold">⭐ How was your food?</p>
                       <p className="mt-0.5 text-xs text-espresso/60">Rate order {feedbackDue.o.orderNumber} · takes 5 sec</p>
                       <div className="mt-2 grid grid-cols-2 gap-2 text-xs font-semibold">
                         <button onClick={() => navigate(`/order/${feedbackDue.o.token}`)} className="rounded-lg bg-espresso py-1.5 text-mint">Rate now</button>
                         <button onClick={() => setNudge(false)} className="rounded-lg border border-espresso/15 py-1.5 text-espresso/60">Later</button>
                       </div>
                     </div>
                   </div>
                 )}


        <label className="sr-only" htmlFor="menu-search">Search the menu</label>
        <input
          id="menu-search"
          type="search"
          placeholder='Search "dosa", "coffee"…'
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="mt-4 w-full rounded-xl bg-paper px-4 py-2.5 text-espresso placeholder:text-espresso/45 focus:outline-none"
        />

        <div className="mt-3 inline-flex rounded-xl bg-paper/10 p-1" role="radiogroup" aria-label="Food preference">
          {DIETS.map((d) => (
            <button
              key={d.value}
              role="radio"
              aria-checked={diet === d.value}
              onClick={() => chooseDiet(d.value)}
              className={`flex items-center gap-1.5 rounded-lg px-3.5 py-1.5 text-sm font-medium transition ${
                diet === d.value ? 'bg-paper text-espresso' : 'text-paper/80'
              }`}
            >
              {d.value !== 'ALL' && <VegMark type={d.value} className="h-3.5 w-3.5" />}
              {d.label}
            </button>
          ))}
        </div>
      </header>

      {/* Offer banner (set BRAND.offer in config/brand.js to show it) */}
     {showExtras && offer && (
        <div className="mx-4 mt-4 flex items-center gap-3 rounded-2xl border border-dashed border-mint-deep bg-[#E1F5EE] px-4 py-3">
          <span className="text-2xl">🎉</span>
          <div>
            <p className="font-semibold text-[#085041]">Use code {offer.code}</p>
            <p className="text-sm text-mint-deep">{offer.text}</p>
          </div>
        </div>
      )}

      {/* Category tiles */}
      {chipCategories.length > 0 && (
        <section className="mt-5">
          <p className="px-4 font-semibold">What are you craving?</p>
              <div className="no-scrollbar mt-1 flex gap-3 overflow-x-auto px-4 py-3">
            {[{ id: 'ALL', name: 'All' }, ...chipCategories].map((c, i) => {
              const active = category === c.id
              return (
                <button
                  key={c.id}
                  onClick={() => setCategory(active && c.id !== 'ALL' ? 'ALL' : c.id)}
                  className="flex w-[4.5rem] shrink-0 flex-col items-center gap-1.5"
                >
                  <span className={`flex h-16 w-16 items-center justify-center overflow-hidden rounded-full text-xl font-semibold ${TILE_BG[i % TILE_BG.length]} ${active ? 'ring-2 ring-mint-deep ring-offset-2' : ''}`}>
                    {c.imageUrl
                      ? <img src={c.imageUrl} alt="" className="h-full w-full object-cover" />
                      : c.id === 'ALL' ? '🍽️' : c.name.charAt(0).toUpperCase()}
                  </span>
                  <span className={`line-clamp-1 text-xs ${active ? 'font-semibold' : 'text-espresso/70'}`}>{c.name}</span>
                </button>
              )
            })}
          </div>
        </section>
      )}

      {/* Most loved */}
      {showExtras && loved.length > 0 && (
        <section className="mt-6">
          <p className="px-4 font-semibold">⭐ Most loved here</p>
          <div className="no-scrollbar mt-3 flex gap-3 overflow-x-auto px-4 pb-1">
            {loved.map((item) => (
                          <LovedCard key={item.id} item={item} quantity={qtyFor(item)} onOpen={() => openItemFor(item)} onChange={(q) => changeQty(item, q)} />
            ))}
          </div>
        </section>
      )}

      {/* Dish list */}
      <main className="mt-6 rounded-t-3xl bg-white pb-4">
        {error && <p className="mx-4 pt-6"><span className="block rounded-xl bg-red-50 p-4 text-red-800">{error}</span></p>}
        {!items && !error && <PageLoader label="Loading the menu" />}
        {items && groups.length === 0 && (
          <p className="px-6 pt-10 text-center text-espresso/60">Nothing matches. Try another search, or switch the filter to “All”.</p>
        )}

        {groups.map((group) => (
          <section key={group.id} id={`cat-${group.id}`} className="scroll-mt-4 pt-5">
            <CategoryHeader group={group} category={categories.find((c) => c.id === group.id)} />
            <ul>
              {group.items.map((item) => (
                                <MenuRow
                                  key={item.id}
                                  item={item}
                                  customisable={!!addonMap[item.id]}
                                  quantity={qtyFor(item)}
                                  onOpen={() => openItemFor(item)}
                                  onChange={(q) => changeQty(item, q)}
                                />
              ))}
            </ul>
          </section>
        ))}
      </main>

      {/* Bottom: menu button, live order strip, cart bar */}
      <div className="fixed inset-x-0 bottom-0 z-30 mx-auto max-w-2xl px-3 pb-[max(0.75rem,env(safe-area-inset-bottom))]">
               <div className="mb-2 flex items-end justify-between gap-2">
                 {qr.orderType === 'TABLE'
                   ? <WaiterButton qr={qr} orders={myOrders} refreshKey={refreshKey} allowBill={businessType !== 'SMALL_CAFE'} />
                   : <span />}
                 {chipCategories.length > 1 && (
                   <button onClick={() => setSheet('menu')} className="rounded-full bg-espresso px-4 py-2.5 text-sm font-semibold text-paper shadow-lg active:scale-95">
                     ☰ Menu
                   </button>
                 )}
               </div>
        <LiveOrderStrip orders={myOrders} refreshKey={refreshKey} onViewAll={() => setSheet('orders')} />
        {cartCount > 0 && (
          <button
            onClick={() => navigate('/cart')}
            className="flex w-full items-center justify-between rounded-2xl bg-espresso px-5 py-3.5 text-paper shadow-[0_12px_30px_-10px_rgba(43,33,28,0.7)]"
          >
            <span className="text-left">
              <span className="block text-xs text-paper/70">{cartCount} item{cartCount > 1 ? 's' : ''} added</span>
              <span className="block font-semibold">{money(cartTotal)}</span>
            </span>
            <span className="font-semibold text-mint">View cart →</span>
          </button>
        )}
      </div>

      {/* Orders sheet */}
      {sheet === 'orders' && (
        <Sheet title="Your orders today" onClose={() => setSheet(null)}>
          {myOrders.map((o) => <MyOrderLink key={o.token} o={o} refreshKey={refreshKey} />)}
          <p className="mt-3 text-center text-xs text-espresso/50">Tap an order to track it or see the receipt</p>
        </Sheet>
      )}

      {/* Menu (categories) sheet */}
      {sheet === 'menu' && (
        <Sheet title="Menu" onClose={() => setSheet(null)}>
          {chipCategories.map((c) => (
            <button key={c.id} onClick={() => jumpTo(c.id)} className="flex w-full justify-between border-b border-espresso/10 py-3 text-left last:border-b-0">
              <span className="font-medium">{c.name}</span>
              <span className="text-sm text-espresso/50">{(items || []).filter((i) => i.categoryId === c.id).length}</span>
            </button>
          ))}
        </Sheet>
      )}

      {openItem && (
        <ItemSheet
          item={openItem}
          inCart={cart[openItem.id] || 0}
          onClose={() => setOpenItem(null)}
          onSave={(q) => {
            updateCart(openItem.id, q)
            setOpenItem(null)
          }}
        />
      )}
            {customizing && (
              <AddonSheet item={customizing} groups={addonMap[customizing.id] || []} onClose={() => setCustomizing(null)} onAdd={addCustom} />
            )}
    </div>
  )
}

function AddControl({ item, quantity, onChange, small = false }) {
  const size = small ? 'px-2.5 py-0.5 text-xs' : 'py-1.5 text-sm'
  if (!item.available) {
    return <span className="block rounded-lg bg-white py-1.5 text-center text-xs font-semibold text-stone-500 shadow">Sold out</span>
  }
  if (quantity === 0) {
    return (
      <button onClick={() => onChange(1)} className={`w-full rounded-lg border border-mint-deep bg-white font-bold text-mint-deep shadow-sm ${size}`}>
        ADD
      </button>
    )
  }
  return (
    <div className={`flex items-center justify-between rounded-lg bg-mint-deep font-bold text-white shadow-sm ${small ? 'text-xs' : 'text-sm'}`}>
      <button className={small ? 'px-2 py-0.5' : 'px-3 py-1.5'} onClick={() => onChange(quantity - 1)} aria-label={`Remove one ${item.name}`}>−</button>
      <span aria-live="polite">{quantity}</span>
      <button className={small ? 'px-2 py-0.5' : 'px-3 py-1.5'} onClick={() => onChange(quantity + 1)} aria-label={`Add one more ${item.name}`}>+</button>
    </div>
  )
}

function MenuRow({ item, quantity, onOpen, onChange, customisable }) {
  return (
    <li className={`flex gap-4 border-b border-dashed border-espresso/15 px-4 pb-8 pt-4 last:border-b-0 ${item.available ? '' : 'opacity-60'}`}>
      <button onClick={onOpen} className="min-w-0 flex-1 text-left">
        {item.bestseller && <span className="mb-1.5 inline-block rounded bg-[#FAEEDA] px-1.5 py-0.5 text-xs font-medium text-[#854F0B]">Bestseller</span>}
        <div className="flex items-center gap-1.5">
          <VegMark type={item.dietaryType} />
          <span className="text-[15px] font-semibold">{item.name}</span>
        </div>
        <p className="mt-1 font-semibold">{money(item.price)}</p>
        {item.description && <p className="mt-1.5 line-clamp-2 text-sm text-espresso/60">{item.description}</p>}
      </button>

      <div className="relative w-28 shrink-0">
        {item.imageUrl ? (
          <>
            <button onClick={onOpen} className="block" aria-label={`View ${item.name}`}>
              <img src={item.imageUrl} alt="" className="h-28 w-28 rounded-2xl object-cover" loading="lazy" />
            </button>
                       <div className="absolute inset-x-2 -bottom-4">
                         <AddControl item={item} quantity={quantity} onChange={onChange} />
                         {customisable && <p className="mt-0.5 text-center text-[10px] text-espresso/50">Customisable</p>}
                       </div>
          </>
        ) : (
                    <div className="pt-6">
                      <AddControl item={item} quantity={quantity} onChange={onChange} />
                      {customisable && <p className="mt-0.5 text-center text-[10px] text-espresso/50">Customisable</p>}
                    </div>
        )}
      </div>
    </li>
  )
}

function LovedCard({ item, quantity, onOpen, onChange }) {
  return (
    <div className="w-36 shrink-0 overflow-hidden rounded-2xl border border-espresso/10 bg-white">
      <button onClick={onOpen} className="block w-full">
        {item.imageUrl
          ? <img src={item.imageUrl} alt="" className="h-20 w-full object-cover" loading="lazy" />
          : <div className="h-20 bg-[#E8C89A]" />}
      </button>
      <div className="p-2.5">
        <p className="flex items-center gap-1.5 text-sm font-semibold">
          <VegMark type={item.dietaryType} />
          <span className="truncate">{item.name}</span>
        </p>
        <div className="mt-2 flex items-center justify-between gap-2">
          <span className="font-semibold">{money(item.price)}</span>
          <div className="w-16"><AddControl item={item} quantity={quantity} onChange={onChange} small /></div>
        </div>
      </div>
    </div>
  )
}

/*
 * Category title. When the admin uploaded a header photo, it is shown as a
 * banner with the name on top; otherwise just the name and item count.
 */
function CategoryHeader({ group, category }) {
  const count = `${group.items.length} item${group.items.length > 1 ? 's' : ''}`
  if (!category?.imageUrl) {
    return (
      <div className="flex items-baseline justify-between px-4 pb-1">
        <h2 className="font-display text-xl font-semibold">{group.name}</h2>
        <span className="text-sm text-espresso/50">{count}</span>
      </div>
    )
  }
  return (
    <div className="relative mx-4 mb-1 h-28 overflow-hidden rounded-2xl sm:h-36">
      <img src={category.imageUrl} alt="" className="absolute inset-0 h-full w-full object-cover" loading="lazy" />
      <div className="absolute inset-0 bg-gradient-to-t from-espresso/85 via-espresso/35 to-transparent" />
      <div className="absolute inset-x-4 bottom-3 flex items-end justify-between gap-2">
        <div>
          <h2 className="font-display text-2xl font-semibold text-paper drop-shadow">{group.name}</h2>
          {category.description && <p className="line-clamp-1 text-sm text-paper/80">{category.description}</p>}
        </div>
        <span className="text-sm text-paper/80">{count}</span>
      </div>
    </div>
  )
}

function Sheet({ title, onClose, children }) {
  return (
    <div className="fixed inset-0 z-40 flex items-end justify-center bg-espresso/45" onClick={onClose}>
      <div className="max-h-[75dvh] w-full max-w-2xl overflow-y-auto rounded-t-3xl bg-white p-4 pb-[max(1rem,env(safe-area-inset-bottom))]" onClick={(e) => e.stopPropagation()}>
        <div className="mx-auto mb-3 h-1 w-10 rounded-full bg-stone-300" />
        <p className="mb-3 text-lg font-semibold">{title}</p>
        {children}
      </div>
    </div>
  )
}

// Shows the newest order that is still in progress (not done / cancelled)
// Shows orders still in progress; 2+ running orders collapse into one row
function LiveOrderStrip({ orders, refreshKey, onViewAll }) {
  const [live, setLive] = useState([])

  useEffect(() => {
    let cancelled = false
    Promise.all(orders.slice(0, 5).map((o) => getOrderDetails(o.token).then((d) => ({ o, d })).catch(() => null)))
      .then((list) => {
        if (!cancelled) setLive(list.filter((x) => x && ACTIVE.includes(x.d.status)))
      })
    return () => { cancelled = true }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [refreshKey, orders.length])

  if (live.length === 0) return null

  const anyCashDue = live.some(({ d }) => d.paymentMethod === 'CASH' && d.orderPaymentStatus !== 'PAID')
  const box = `mb-2 flex w-full items-center justify-between rounded-xl px-4 py-2.5 text-left text-sm ${anyCashDue ? 'border border-amber-400 bg-amber-50' : 'bg-[#E1F5EE]'}`
  const dot = <span className="h-2 w-2 shrink-0 animate-pulse rounded-full bg-mint-deep" />

  if (live.length > 1) {
    return (
      <button onClick={onViewAll} className={box}>
        <span className="flex items-center gap-2">
          {dot}
          <span><b className="font-semibold">{live.length} orders</b> in progress{anyCashDue ? ' · payment due at counter' : ''}</span>
        </span>
        <span className="font-semibold text-mint-deep">View</span>
      </button>
    )
  }

  const { o, d } = live[0]
  const cashDue = d.paymentMethod === 'CASH' && d.orderPaymentStatus !== 'PAID'
  const text = d.status === 'READY' ? 'is ready!' : d.status === 'PREPARING' ? 'is being prepared' : 'is received'

  return (
    <Link to={`/order/${o.token}`} className={box}>
      <span className="flex items-center gap-2">
        {dot}
        <span><b className="font-semibold">Your order</b> {text}{cashDue ? ` · Pay ${money(d.totalAmount)} at counter` : ''}</span>
      </span>
      <span className="font-semibold text-mint-deep">Track</span>
    </Link>
  )
}
// One order row inside the "Your orders" sheet, with live payment status
function MyOrderLink({ o, refreshKey }) {
  const [d, setD] = useState(null)

  useEffect(() => {
    getOrderDetails(o.token).then(setD).catch(() => {})
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [o.token, refreshKey])

  const cancelled = d?.status === 'CANCELLED'
  const cashDue = d?.paymentMethod === 'CASH' && d?.orderPaymentStatus !== 'PAID' && !cancelled
  const cashPaid = d?.paymentMethod === 'CASH' && d?.orderPaymentStatus === 'PAID'
  const onlinePaid = d?.paymentMethod === 'ONLINE' && d?.orderPaymentStatus === 'PAID' && !cancelled
  const statusText = { PAID: 'Received', PREPARING: 'Preparing', READY: 'Ready', COMPLETED: 'Done', CANCELLED: 'Cancelled' }[d?.status] || ''

  return (
    <Link
      to={`/order/${o.token}`}
      className={`mb-2 block rounded-2xl px-4 py-3 text-sm ${
        cashDue ? 'border-2 border-amber-400 bg-amber-50' : cancelled ? 'border border-red-200 bg-red-50' : 'border border-espresso/10 bg-white'
      }`}
    >
      <span className="flex justify-between">
        <b className="font-semibold">{o.orderNumber}</b>
        <span className={cancelled ? 'text-red-700' : 'text-mint-deep'}>{statusText}</span>
      </span>
      {d && (
        <span className="mt-1 block text-xs text-espresso/60">
          {d.items?.map((i) => `${i.quantity} × ${i.itemName}`).join(', ')} · {money(d.totalAmount)}
        </span>
      )}
      {cashDue && <span className="mt-1 block font-semibold text-amber-900">💵 Pay {money(d.totalAmount)} at the counter</span>}
      {cashPaid && <span className="mt-1 block text-mint-deep">Paid · Cash</span>}
      {onlinePaid && <span className="mt-1 block text-mint-deep">Paid · Online</span>}
    </Link>
  )
}

// Floating "Call waiter" pill; "Ask for bill" only when the table owes cash (large cafe / restaurant)
function WaiterButton({ qr, orders, refreshKey, allowBill }) {
  const now = useNow(1000)
  const [open, setOpen] = useState(false)
  const [msg, setMsg] = useState('')
  const [until, setUntil] = useState(0)
  const [cashDue, setCashDue] = useState(false)

  useEffect(() => {
    if (!allowBill) return
    Promise.all(orders.slice(0, 5).map((o) => getOrderDetails(o.token).catch(() => null)))
      .then((list) => setCashDue(list.some((d) =>
        d && d.tableNumber === qr.tableNumber && ACTIVE.includes(d.status)
        && d.paymentMethod === 'CASH' && d.orderPaymentStatus !== 'PAID')))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [allowBill, refreshKey, orders.length])

  const showBill = allowBill && cashDue
  const waiting = until > now

  const send = async (type) => {
    setOpen(false)
    try {
      const r = await sendTableRequest(qr.qrToken, type)
      setMsg(type === 'BILL'
        ? '🧾 Bill request sent. Staff will bring it shortly.'
        : r?.status === 'ALREADY_CALLED' ? '✅ Already called. Staff is on the way.' : '🙋 Waiter is on the way!')
      setUntil(Date.now() + 60000)
    } catch (e) {
      setMsg(e.message)
    }
    setTimeout(() => setMsg(''), 4000)
  }

  return (
    <div className="relative">
      {msg && (
        <div className="absolute bottom-full left-0 mb-2 w-64 rounded-2xl bg-espresso px-4 py-3 text-sm text-paper shadow-lg">{msg}</div>
      )}
      {open && (
        <div className="absolute bottom-full left-0 mb-2 w-52 overflow-hidden rounded-2xl bg-white shadow-xl ring-1 ring-espresso/10">
          <button onClick={() => send('WAITER')} className="w-full px-4 py-3 text-left text-sm font-medium active:bg-mist">🙋 Call waiter</button>
          <button onClick={() => send('BILL')} className="w-full border-t border-espresso/10 px-4 py-3 text-left text-sm font-medium active:bg-mist">🧾 Ask for bill</button>
        </div>
      )}
      <button
        onClick={() => (showBill ? setOpen((v) => !v) : send('WAITER'))}
        disabled={waiting && !showBill}
        className={`flex items-center gap-2 rounded-full px-4 py-2.5 text-sm font-semibold shadow-lg transition active:scale-95 ${
          waiting ? 'bg-white text-mint-deep ring-2 ring-mint-deep' : 'bg-mint text-espresso'
        }`}
      >
        <span>🛎️</span>
        {waiting ? `Called ✓ ${Math.ceil((until - now) / 1000)}s` : showBill ? 'Need help?' : 'Call waiter'}
      </button>
    </div>
  )
}



// The newest completed order that is ready for feedback (not rated / skipped yet)
function useFeedbackDue(orders, refreshKey) {
  const now = useNow(30000)
  const [list, setList] = useState([])

  useEffect(() => {
    Promise.all(orders.slice(0, 5).map((o) => getOrderDetails(o.token).then((d) => ({ o, d })).catch(() => null)))
      .then((r) => setList(r.filter(Boolean)))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [refreshKey, orders.length])

  return list.find(({ o, d }) =>
    d.status === 'COMPLETED' && d.rating == null && !isFeedbackSkipped(o.token) && now >= feedbackReadyAt(d)) || null
}


