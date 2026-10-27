import { useCallback, useEffect, useState } from 'react'
import {
  deleteCategory, deleteMenuItem, getAdminCategories, getAdminMenuItems, saveCategory, saveMenuItem,
} from '../api/staff'
import { ErrorNote, Field, Modal, PageLoader, VegMark, btn, input } from '../components/ui'
import { useToast } from '../components/Toast'
import { money } from '../lib/format'
import ImageUpload from '../components/ImageUpload'
import AddonEditorModal from './AddonEditorModal'
const EMPTY_ITEM = { name: '', description: '', price: '', imageUrl: '', dietaryType: 'VEG', categoryId: '', available: true }

// Categories and menu items: add, edit, availability, hide, delete.
export default function MenuAdminPage() {
  const toast = useToast()
  const [categories, setCategories] = useState(null)
  const [items, setItems] = useState([])
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')
    const [addonItem, setAddonItem] = useState(null)
  const [editingItem, setEditingItem] = useState(null)
  const [editingCategory, setEditingCategory] = useState(null)

  const load = useCallback(() => {
    Promise.all([getAdminCategories(), getAdminMenuItems()])
      .then(([cats, menuItems]) => { setCategories(cats); setItems(menuItems); setError('') })
      .catch((e) => setError(e.message))
  }, [])
  useEffect(() => { load() }, [load])

  const toggleAvailable = async (item) => {
    try {
      await saveMenuItem({ ...item, available: !item.available })
      toast(`${item.name} is now ${item.available ? 'sold out' : 'available'}`)
      load()
    } catch (e) {
      toast(e.message, 'error')
    }
  }

  const toggleCategory = async (category) => {
    try {
      await saveCategory({ ...category, active: !category.active })
      load()
    } catch (e) {
      toast(e.message, 'error')
    }
  }

  const removeItem = async (item) => {
    if (!window.confirm(`Delete "${item.name}"? This cannot be undone.`)) return
    try {
      await deleteMenuItem(item.id)
      toast('Item deleted')
      load()
    } catch (e) {
      toast(e.message, 'error')
    }
  }

  const removeCategory = async (category) => {
    if (!window.confirm(`Delete category "${category.name}"?`)) return
    try {
      await deleteCategory(category.id)
      toast('Category deleted')
      load()
    } catch (e) {
      toast(e.message, 'error')
    }
  }

  if (error) return <ErrorNote>{error}</ErrorNote>
  if (!categories) return <PageLoader label="Loading menu" />

  const q = search.trim().toLowerCase()
  const shown = items.filter((i) => !q || i.name.toLowerCase().includes(q))

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="font-display text-3xl font-semibold">Menu</h1>
          <p className="text-stone-500">{items.length} items in {categories.length} categories</p>
        </div>
        <div className="flex gap-2">
          <button className={btn.ghost} onClick={() => setEditingCategory({ name: '', description: '', imageUrl: '' })}>Add category</button>
          <button
            className={btn.primary}
            disabled={categories.length === 0}
            onClick={() => setEditingItem({ ...EMPTY_ITEM, categoryId: categories[0]?.id || '' })}
          >
            Add item
          </button>
        </div>
      </div>

      <section className="rounded-3xl bg-white p-5 shadow-sm">
        <h2 className="mb-3 font-semibold">Categories</h2>
        {categories.length === 0 && <p className="text-sm text-stone-500">Add your first category, for example “Coffee”.</p>}
        <div className="flex flex-wrap gap-2">
          {categories.map((c) => (
            <div key={c.id} className={`flex items-center gap-1 rounded-full border px-3 py-1.5 text-sm ${c.active ? 'border-stone-200' : 'border-dashed border-stone-300 text-stone-400'}`}>
              {c.imageUrl && <img src={c.imageUrl} alt="" className="h-5 w-5 rounded-full object-cover" />}
              <button onClick={() => setEditingCategory(c)} className="font-medium">{c.name}</button>
              <button onClick={() => toggleCategory(c)} className="ml-1 text-xs text-stone-500 underline-offset-2 hover:underline">
                {c.active ? 'Hide' : 'Show'}
              </button>
              <button onClick={() => removeCategory(c)} className="ml-1 text-xs text-red-600" aria-label={`Delete ${c.name}`}>✕</button>
            </div>
          ))}
        </div>
      </section>

      <section className="rounded-3xl bg-white shadow-sm">
        <div className="flex items-center justify-between gap-3 p-5 pb-3">
          <h2 className="font-semibold">Items</h2>
          <input className={`${input} max-w-xs`} placeholder="Search items" value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
        <div className="overflow-x-auto">
          <table className="w-full min-w-[680px] text-sm">
            <thead className="border-y border-stone-200 text-left text-stone-500">
              <tr>
                <th className="px-5 py-2.5 font-medium">Item</th>
                <th className="px-3 py-2.5 font-medium">Category</th>
                <th className="px-3 py-2.5 font-medium">Price</th>
                <th className="px-3 py-2.5 font-medium">Available</th>
                <th className="px-5 py-2.5" />
              </tr>
            </thead>
            <tbody>
              {shown.map((item) => (
                <tr key={item.id} className="border-b border-stone-100">
                  <td className="px-5 py-3">
                    <div className="flex items-center gap-3">
                      {item.imageUrl ? <img src={item.imageUrl} alt="" className="h-10 w-10 rounded-lg object-cover" /> : <div className="h-10 w-10 rounded-lg bg-stone-100" />}
                      <VegMark type={item.dietaryType} />
                      <span className="font-medium">{item.name}</span>
                    </div>
                  </td>
                  <td className="px-3 py-3">{item.categoryName}</td>
                  <td className="px-3 py-3">{money(item.price)}</td>
                  <td className="px-3 py-3">
                    <button
                      role="switch"
                      aria-checked={item.available}
                      onClick={() => toggleAvailable(item)}
                      className={`relative h-6 w-11 rounded-full transition ${item.available ? 'bg-mint-deep' : 'bg-stone-300'}`}
                    >
                      <span className={`absolute top-0.5 h-5 w-5 rounded-full bg-white transition ${item.available ? 'left-[22px]' : 'left-0.5'}`} />
                      <span className="sr-only">{item.available ? 'Available' : 'Sold out'}</span>
                    </button>
                  </td>
                  <td className="px-5 py-3 text-right">
                                      <button className="mr-3 font-medium text-mint-deep underline-offset-2 hover:underline" onClick={() => setAddonItem(item)}>Add-ons</button>
                    <button className="mr-3 font-medium text-espresso underline-offset-2 hover:underline" onClick={() => setEditingItem(item)}>Edit</button>
                    <button className="text-red-600 underline-offset-2 hover:underline" onClick={() => removeItem(item)}>Delete</button>
                  </td>
                </tr>
              ))}
              {shown.length === 0 && (
                <tr><td colSpan="5" className="px-5 py-10 text-center text-stone-500">No items yet.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </section>

      {editingItem && (
        <ItemForm item={editingItem} categories={categories} onClose={() => setEditingItem(null)} onSaved={() => { setEditingItem(null); toast('Item saved'); load() }} />
      )}
            {editingCategory && (
              <CategoryForm category={editingCategory} onClose={() => setEditingCategory(null)} onSaved={() => { setEditingCategory(null); toast('Category saved'); load() }} />
            )}
            {addonItem && <AddonEditorModal item={addonItem} onClose={() => setAddonItem(null)} />}
          </div>
        )
      }

function ItemForm({ item, categories, onClose, onSaved }) {
  const [form, setForm] = useState({ ...item, price: item.price === '' ? '' : String(item.price) })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.type === 'checkbox' ? e.target.checked : e.target.value }))

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      await saveMenuItem({
        id: form.id,
        name: form.name.trim(),
        description: form.description?.trim() || '',
        price: Number(form.price),
        imageUrl: form.imageUrl?.trim() || null,
        dietaryType: form.dietaryType,
        categoryId: Number(form.categoryId),
        available: form.available,
      })
      onSaved()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal open onClose={onClose} title={form.id ? 'Edit item' : 'Add item'}>
      <form onSubmit={submit} className="space-y-4">
        <Field label="Name"><input className={input} required maxLength={150} value={form.name} onChange={set('name')} /></Field>
        <Field label="Description"><textarea className={input} rows={3} maxLength={1000} value={form.description || ''} onChange={set('description')} /></Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Price (₹)"><input className={input} required type="number" min="1" step="0.01" value={form.price} onChange={set('price')} /></Field>
          <Field label="Category">
            <select className={input} required value={form.categoryId} onChange={set('categoryId')}>
              {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </Field>
        </div>
        <Field label="Food type">
          <div className="flex gap-2">
            {['VEG', 'NON_VEG'].map((type) => (
              <label key={type} className={`flex flex-1 cursor-pointer items-center justify-center gap-2 rounded-xl border px-3 py-2.5 ${form.dietaryType === type ? 'border-espresso bg-espresso/5' : 'border-stone-200'}`}>
                <input type="radio" className="sr-only" name="dietaryType" value={type} checked={form.dietaryType === type} onChange={set('dietaryType')} />
                <VegMark type={type} /> {type === 'VEG' ? 'Veg' : 'Non-veg'}
              </label>
            ))}
          </div>
        </Field>
        <ImageUpload
          label="Photo (optional)"
          folder="menu"
          value={form.imageUrl || ''}
          onChange={(url) => setForm((f) => ({ ...f, imageUrl: url }))}
        />
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" className="h-4 w-4 accent-espresso" checked={form.available} onChange={set('available')} /> Available to order
        </label>
        <ErrorNote>{error}</ErrorNote>
        <button className={`${btn.primary} w-full`} disabled={busy}>Save item</button>
      </form>
    </Modal>
  )
}

function CategoryForm({ category, onClose, onSaved }) {
  const [form, setForm] = useState(category)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      await saveCategory({ id: form.id, name: form.name.trim(), description: form.description || '', imageUrl: form.imageUrl || null, active: form.active })
      onSaved()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal open onClose={onClose} title={form.id ? 'Edit category' : 'Add category'}>
      <form onSubmit={submit} className="space-y-4">
        <Field label="Name"><input className={input} required maxLength={100} value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Field>
        <Field label="Description (optional)"><input className={input} maxLength={500} value={form.description || ''} onChange={(e) => setForm({ ...form, description: e.target.value })} /></Field>
        <ImageUpload
          label="Header photo (optional)"
          folder="category"
          aspect="aspect-[3/1]"
          value={form.imageUrl || ''}
          onChange={(url) => setForm((f) => ({ ...f, imageUrl: url }))}
        />
        <p className="-mt-2 text-xs text-espresso/50">Shown as a banner behind the category name on the customer menu. A wide photo looks best.</p>
        <ErrorNote>{error}</ErrorNote>
        <button className={`${btn.primary} w-full`} disabled={busy}>Save category</button>
      </form>
    </Modal>
  )
}
