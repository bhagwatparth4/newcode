import { useState } from 'react'
import { VegMark } from '../components/ui'
import { money } from '../lib/format'

// Bottom sheet to choose add-ons (e.g. Size, Extra toppings) before adding an item
export default function AddonSheet({ item, groups, onClose, onAdd }) {
  // Required "pick one" groups start with their first option selected
  const [picked, setPicked] = useState(() => {
    const start = {}
    groups.forEach((g) => { start[g.id] = g.required && g.maxSelect === 1 && g.options[0] ? [g.options[0].id] : [] })
    return start
  })
  const [qty, setQty] = useState(1)

  const toggle = (g, id) => setPicked((cur) => {
    const list = cur[g.id] || []
    if (g.maxSelect === 1) return { ...cur, [g.id]: [id] }
    if (list.includes(id)) return { ...cur, [g.id]: list.filter((x) => x !== id) }
    if (list.length >= g.maxSelect) return cur
    return { ...cur, [g.id]: [...list, id] }
  })

  const chosen = groups.flatMap((g) => g.options.filter((o) => (picked[g.id] || []).includes(o.id)))
  const missing = groups.find((g) => g.required && (picked[g.id] || []).length === 0)
  const unitPrice = Number(item.price) + chosen.reduce((s, o) => s + Number(o.price), 0)

  const add = () => {
    const addonIds = chosen.map((o) => o.id).sort((a, b) => a - b)
    onAdd({
      key: `${item.id}:${addonIds.join('-')}`,
      menuItemId: item.id,
      name: item.name,
      addonIds,
      addonsText: chosen.map((o) => o.name).join(', '),
      unitPrice,
      qty,
    })
  }

  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-espresso/45" onClick={onClose}>
      <div className="max-h-[88dvh] w-full max-w-2xl overflow-y-auto rounded-t-3xl bg-white pb-[max(1rem,env(safe-area-inset-bottom))]" onClick={(e) => e.stopPropagation()}>
        {item.imageUrl && <img src={item.imageUrl} alt="" className="h-40 w-full object-cover" />}
        <div className="p-4">
          <div className="flex items-center gap-2">
            <VegMark type={item.dietaryType} />
            <h2 className="text-lg font-semibold">{item.name}</h2>
          </div>
          <p className="text-sm text-espresso/60">{money(item.price)}{item.description ? ` · ${item.description}` : ''}</p>

          {groups.map((g) => (
            <section key={g.id} className="mt-5">
              <div className="flex items-baseline justify-between">
                <p className="font-semibold">{g.name}</p>
                <span className={`rounded-full px-2 py-0.5 text-xs ${g.required ? 'bg-[#FAEEDA] text-[#854F0B]' : 'bg-stone-100 text-stone-600'}`}>
                  {g.required ? 'Required' : 'Optional'} · {g.maxSelect === 1 ? 'pick 1' : `up to ${g.maxSelect}`}
                </span>
              </div>
              <ul className="mt-1">
                {g.options.map((o) => {
                  const on = (picked[g.id] || []).includes(o.id)
                  return (
                    <li key={o.id}>
                      <button onClick={() => toggle(g, o.id)} className="flex w-full items-center justify-between border-b border-espresso/10 py-3 text-left text-sm">
                        <span className="flex items-center gap-3">
                          <span className={`flex h-5 w-5 items-center justify-center border-2 ${g.maxSelect === 1 ? 'rounded-full' : 'rounded-md'} ${on ? 'border-mint-deep bg-mint-deep text-white' : 'border-stone-300'}`}>
                            {on && <span className="text-[11px] leading-none">✓</span>}
                          </span>
                          {o.name}
                        </span>
                        <span className="text-espresso/60">{Number(o.price) > 0 ? `+${money(o.price)}` : 'Free'}</span>
                      </button>
                    </li>
                  )
                })}
              </ul>
            </section>
          ))}

          <div className="mt-5 flex items-center gap-3">
            <div className="flex items-center rounded-xl border border-espresso/15">
              <button className="px-3 py-2.5" onClick={() => setQty((q) => Math.max(1, q - 1))} aria-label="Decrease">−</button>
              <span className="w-6 text-center font-semibold">{qty}</span>
              <button className="px-3 py-2.5" onClick={() => setQty((q) => Math.min(50, q + 1))} aria-label="Increase">+</button>
            </div>
            <button onClick={add} disabled={!!missing}
              className="flex-1 rounded-2xl bg-espresso py-3.5 font-semibold text-mint disabled:opacity-50">
              {missing ? `Choose ${missing.name}` : `Add item · ${money(unitPrice * qty)}`}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}