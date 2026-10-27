import { useState } from 'react'
import { Modal, VegMark, btn } from '../components/ui'
import { money } from '../lib/format'

// Item details: photo, description, quantity. (No customisation fields:
// the backend does not store customisations, so we do not pretend to.)
export default function ItemSheet({ item, inCart, onClose, onSave }) {
  const [quantity, setQuantity] = useState(inCart || 1)
  if (!item) return null

  return (
    <Modal open onClose={onClose} title={item.name}>
      {item.imageUrl && (
        <img src={item.imageUrl} alt="" className="mb-4 aspect-[4/3] w-full rounded-2xl object-cover" loading="lazy" />
      )}
      <div className="flex items-center gap-2 text-sm text-espresso/70">
        <VegMark type={item.dietaryType} />
        <span>{item.dietaryType === 'VEG' ? 'Vegetarian' : 'Non-vegetarian'}</span>
        <span aria-hidden="true">·</span>
        <span>{item.categoryName}</span>
      </div>
      {item.description && <p className="mt-3 leading-relaxed text-espresso/80">{item.description}</p>}
      <p className="mt-3 text-lg font-semibold">{money(item.price)}</p>

      {item.available ? (
        <div className="mt-6 flex items-center gap-3">
          <div className="flex items-center rounded-xl border border-espresso/15">
            <button
              className="px-4 py-3 text-lg"
              onClick={() => setQuantity((q) => Math.max(inCart ? 0 : 1, q - 1))}
              aria-label="Decrease quantity"
            >
              −
            </button>
            <span className="w-8 text-center font-semibold" aria-live="polite">{quantity}</span>
            <button
              className="px-4 py-3 text-lg"
              onClick={() => setQuantity((q) => Math.min(50, q + 1))}
              aria-label="Increase quantity"
            >
              +
            </button>
          </div>
          <button className={`${btn.primary} flex-1`} onClick={() => onSave(quantity)}>
            {quantity === 0 ? 'Remove from cart' : `${inCart ? 'Update' : 'Add'} · ${money(item.price * quantity)}`}
          </button>
        </div>
      ) : (
        <p className="mt-6 rounded-xl bg-stone-100 px-4 py-3 text-center font-medium text-stone-600">
          Sold out right now
        </p>
      )}
    </Modal>
  )
}
