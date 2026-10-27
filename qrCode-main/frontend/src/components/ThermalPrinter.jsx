import { useState } from 'react'
import { BRAND } from '../config/brand'
import ReceiptPaper from './ReceiptPaper'

/*
 * Thermal billing machine: the receipt feeds out of the printer slot in
 * small mechanical steps (see .receipt-feed in index.css). Fast (~2.4s),
 * and instant for people who turned on "reduce motion".
 */
export default function ThermalPrinter({ receipt, onDone }) {
  const [done, setDone] = useState(false)

  const finish = () => {
    if (done) return
    setDone(true)
    onDone?.()
  }

  return (
    <div className="flex flex-col items-center">
      <p className="mb-4 text-sm font-medium text-espresso/70" aria-live="polite">
        {done ? 'Your bill is ready' : 'Generating bill…'}
      </p>

      {/* Printer */}
      <div
        className={`printer-body relative z-10 w-[340px] max-w-[92vw] rounded-[22px] bg-gradient-to-b from-roast to-espresso px-5 pb-4 pt-4 shadow-[0_18px_30px_-12px_rgba(43,33,28,0.6)] ${
          done ? 'is-done' : ''
        }`}
      >
        <div className="flex items-center justify-between">
          <span className="font-display text-sm font-semibold tracking-wide text-paper/90">{BRAND.name}</span>
          <span className="flex items-center gap-2 text-[10px] text-paper/50">
            {done ? 'Done' : 'Printing'}
            <span className={`printer-led h-2 w-2 rounded-full bg-mint shadow-[0_0_8px_#6fe0b0] ${done ? 'is-done' : ''}`} />
          </span>
        </div>
        {/* Paper slot */}
        <div className="mt-4 h-2.5 rounded-full bg-black/60 shadow-[inset_0_2px_3px_rgba(0,0,0,0.8)]" />
      </div>

      {/* Paper coming out of the slot */}
      <div className="-mt-2 w-[320px] max-w-[88vw] overflow-hidden">
        <div className="receipt-feed flex justify-center" onAnimationEnd={finish}>
          <ReceiptPaper receipt={receipt} />
        </div>
      </div>
    </div>
  )
}
