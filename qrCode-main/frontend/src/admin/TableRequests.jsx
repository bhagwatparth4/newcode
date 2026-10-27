import { useNow } from '../lib/useNow'

// Soft two-tone "ding" (no sound file needed)
let audio
export function playDing() {
  try {
    audio = audio || new (window.AudioContext || window.webkitAudioContext)()
    const t = audio.currentTime
    ;[880, 1320, 880, 1320].forEach((freq, i) => {
      const osc = audio.createOscillator()
      const gain = audio.createGain()
      const start = t + i * 0.12
      osc.type = 'sine'
      osc.frequency.value = freq
      gain.gain.setValueAtTime(0.0001, start)
      gain.gain.exponentialRampToValueAtTime(0.5, start + 0.02)
      gain.gain.exponentialRampToValueAtTime(0.0001, start + 0.5)
      osc.connect(gain).connect(audio.destination)
      osc.start(start)
      osc.stop(start + 0.55)
    })
  } catch {
    // sound is optional
  }
}

// Small cards (top-right) for "Call waiter" / "Ask for bill" from tables
export default function TableRequests({ items, onDone }) {
  const now = useNow(30000)
  if (items.length === 0) return null

  return (
    <div className="fixed right-4 top-4 z-50 flex w-72 flex-col gap-2">
      {items.map((r) => {
        const bill = r.type === 'BILL'
        const mins = Math.floor((now - r.at) / 60000)
        return (
          <div key={r.key} className="flex items-center gap-3 rounded-2xl border border-stone-200 bg-white p-3 shadow-lg">
            <span className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-xl ${bill ? 'bg-amber-100' : 'bg-[#E1F5EE]'}`}>
              {bill ? '🧾' : '🙋'}
            </span>
            <div className="min-w-0 flex-1">
              <p className="text-sm font-semibold"> {r.tableNumber}</p>
              <p className="text-xs text-stone-500">
                {bill ? 'wants the bill' : 'is calling a waiter'} · {mins < 1 ? 'just now' : `${mins} min ago`}
              </p>
            </div>
            <button onClick={() => onDone(r.key)} className="rounded-lg bg-espresso px-2.5 py-1.5 text-xs font-semibold text-mint">
              ✓ Done
            </button>
          </div>
        )
      })}
    </div>
  )
}