import { useEffect, useState } from 'react'
import { getItemAddons, saveItemAddons } from '../api/staff'
import { ErrorNote, Modal, btn, input } from '../components/ui'
import { useToast } from '../components/Toast'

const newOption = () => ({ name: '', price: '' })
const newGroup = () => ({ name: '', required: false, maxSelect: 1, options: [newOption()] })

// Admin editor for an item's add-on groups (e.g. "Size", "Extra toppings")
export default function AddonEditorModal({ item, onClose }) {
  const toast = useToast()
  const [groups, setGroups] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    getItemAddons(item.id)
      .then((list) => setGroups(list.map((g) => ({
        name: g.name, required: g.required, maxSelect: g.maxSelect,
        options: g.options.map((o) => ({ name: o.name, price: String(o.price) })),
      }))))
      .catch((e) => setError(e.message))
  }, [item.id])

  const editGroup = (gi, patch) => setGroups((gs) => gs.map((g, i) => (i === gi ? { ...g, ...patch } : g)))
  const editOption = (gi, oi, patch) =>
    editGroup(gi, { options: groups[gi].options.map((o, i) => (i === oi ? { ...o, ...patch } : o)) })

  const save = async () => {
    setBusy(true)
    setError('')
    try {
      await saveItemAddons(item.id, groups.map((g) => ({
        name: g.name.trim(),
        required: g.required,
        maxSelect: Number(g.maxSelect) || 1,
        options: g.options.filter((o) => o.name.trim()).map((o) => ({ name: o.name.trim(), price: Number(o.price) || 0 })),
      })))
      toast('Add-ons saved')
      onClose()
    } catch (e) {
      setError(e.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal open onClose={onClose} title={`Add-ons · ${item.name}`} wide>
      {!groups ? (
        error ? <ErrorNote>{error}</ErrorNote> : <p className="text-sm text-stone-500">Loading…</p>
      ) : (
        <div className="space-y-4 text-sm">
          <p className="text-stone-500">
            Group = one question for the customer, like <b>Size</b> (pick one) or <b>Extra toppings</b> (pick many). Prices are added to the item price.
          </p>

          {groups.length === 0 && <p className="rounded-xl bg-stone-50 p-4 text-stone-500">No add-ons. This item is added to the cart directly.</p>}

          {groups.map((g, gi) => (
            <section key={gi} className="rounded-2xl border border-stone-200 bg-white p-4">
              <div className="flex flex-wrap items-center gap-2">
                <input className={`${input} min-w-[10rem] flex-1`} placeholder='Group name, e.g. "Size"' maxLength={50}
                  value={g.name} onChange={(e) => editGroup(gi, { name: e.target.value })} />
                <select className={`${input} w-auto`} value={g.maxSelect > 1 ? 'many' : 'one'}
                  onChange={(e) => editGroup(gi, { maxSelect: e.target.value === 'one' ? 1 : Math.max(2, g.options.length) })}>
                  <option value="one">Pick one</option>
                  <option value="many">Pick many</option>
                </select>
                {g.maxSelect > 1 && (
                  <label className="flex items-center gap-1 text-stone-600">up to
                    <input type="number" min="2" max={g.options.length} className={`${input} w-16`}
                      value={g.maxSelect} onChange={(e) => editGroup(gi, { maxSelect: e.target.value })} />
                  </label>
                )}
                <label className="flex items-center gap-1.5 text-stone-600">
                  <input type="checkbox" className="h-4 w-4 accent-espresso" checked={g.required}
                    onChange={(e) => editGroup(gi, { required: e.target.checked })} />
                  Required
                </label>
              </div>

              <div className="mt-3 space-y-2">
                                {g.options.map((o, oi) => (
                                  <div key={oi} className="flex items-center gap-2">
                                    <div className="min-w-0 flex-1">
                                      <input className={input} placeholder='Option, e.g. "Extra cheese"' maxLength={50}
                                        value={o.name} onChange={(e) => editOption(gi, oi, { name: e.target.value })} />
                                    </div>
                                    <div className="w-28 shrink-0">
                                      <input className={input} type="number" min="0" placeholder="₹ price"
                                        value={o.price} onChange={(e) => editOption(gi, oi, { price: e.target.value })} />
                                    </div>
                                    <button className="px-2 text-stone-400 hover:text-red-600" aria-label="Remove option"
                                      onClick={() => editGroup(gi, { options: g.options.filter((_, i) => i !== oi) })}>✕</button>
                                  </div>
                                ))}
              </div>

              <div className="mt-3 flex justify-between">
                <button className="text-sm font-semibold text-mint-deep" onClick={() => editGroup(gi, { options: [...g.options, newOption()] })}>+ Add option</button>
                <button className="text-sm text-red-600" onClick={() => setGroups(groups.filter((_, i) => i !== gi))}>Remove group</button>
              </div>
            </section>
          ))}

          <button className="w-full rounded-2xl border-2 border-dashed border-stone-300 py-3 font-semibold text-stone-500 hover:bg-stone-50"
            onClick={() => setGroups([...groups, newGroup()])}>
            + Add group (e.g. "Size", "Extra toppings")
          </button>

          <ErrorNote>{error}</ErrorNote>

          <div className="flex gap-2">
            <button className={`${btn.ghost} flex-1`} onClick={onClose}>Cancel</button>
            <button className={`${btn.primary} flex-1`} onClick={save} disabled={busy}>{busy ? 'Saving…' : 'Save add-ons'}</button>
          </div>
        </div>
      )}
    </Modal>
  )
}