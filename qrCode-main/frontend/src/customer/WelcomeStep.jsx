import { useState } from 'react'
import { BRAND } from '../config/brand'
import { Logo, btn, input } from '../components/ui'
import { placeLabel } from '../lib/format'

/*
 * First screen after scanning: ask for a nickname so staff can call out
 * the order ("Kittu, your coffee is ready"). No account, no login.
 */
export default function WelcomeStep({ qr, onDone }) {
  const [name, setName] = useState('')
  const [error, setError] = useState('')

  const submit = (e) => {
    e.preventDefault()
    const clean = name.trim().replace(/\s+/g, ' ')
    if (clean.length < 2) {
      setError('Please enter at least 2 letters.')
      return
    }
    if (clean.length > 30) {
      setError('Please keep it under 30 characters.')
      return
    }
    onDone(clean)
  }

  return (
    <main className="mx-auto flex min-h-dvh max-w-md flex-col justify-center px-6 py-10">
      <div className="flex flex-col items-center text-center">
        <div className="coin">
          <Logo size={148} className="shadow-[0_20px_40px_-18px_rgba(43,33,28,0.55)]" />
        </div>
        <span className="mt-6 rounded-full bg-mint/40 px-3 py-1 text-sm font-semibold text-mint-deep">
          {qr.orderType === 'TABLE' ? `You're at ${placeLabel(qr.orderType, qr.tableNumber)}` : 'Ordering at the counter'}
        </span>
        <h1 className="mt-4 font-display text-3xl font-semibold">Welcome to {BRAND.name}</h1>
        <p className="mt-2 text-espresso/70">What should we call you when your order is ready?</p>
      </div>

      <form onSubmit={submit} className="mt-8 space-y-3">
        <label className="sr-only" htmlFor="nickname">Your name</label>
        <input
          id="nickname"
          className={`${input} py-3.5 text-lg`}
          placeholder="Your name, e.g. Kittu"
          value={name}
          maxLength={30}
          autoComplete="given-name"
          autoFocus
          onChange={(e) => {
            setName(e.target.value)
            setError('')
          }}
        />
        {error && <p className="text-sm text-red-700">{error}</p>}
        <button type="submit" className={`${btn.primary} w-full py-3.5 text-base`}>
          See the menu
        </button>
      </form>
    </main>
  )
}
