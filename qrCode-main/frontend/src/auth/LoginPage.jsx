import { useState } from 'react'
import { Navigate, useNavigate, useSearchParams } from 'react-router-dom'
import { BRAND } from '../config/brand'
import { login } from '../api/staff'
import { ErrorNote, Field, Logo, Spinner, btn, input } from '../components/ui'
import { getAuth, homePathForRole, saveAuth } from './authStore'

/*
 * One login page for Admin and Kitchen. The user never picks a role:
 * the backend decides it and we open the matching dashboard.
 */
export default function LoginPage() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(params.get('expired') ? 'Your session ended. Please log in again.' : '')

  const existing = getAuth()
  if (existing) return <Navigate to={homePathForRole(existing.role)} replace />

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      const response = await login(email.trim(), password)
      const session = saveAuth(response)
      navigate(homePathForRole(session.role), { replace: true })
    } catch (err) {
      setError(err.status === 401 ? err.message || 'Invalid email or password' : err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="flex min-h-dvh items-center justify-center bg-espresso px-5">
      <form onSubmit={submit} className="w-full max-w-sm rounded-3xl bg-white p-7 shadow-2xl">
        <div className="flex items-center gap-3">
          <Logo size={48} />
          <div>
            <p className="font-display text-xl font-semibold">{BRAND.name}</p>
            <p className="text-sm text-espresso/60">Staff login</p>
          </div>
        </div>
        <div className="mt-6 space-y-4">
          <Field label="Email">
            <input className={input} type="email" autoComplete="username" required value={email} onChange={(e) => setEmail(e.target.value)} />
          </Field>
          <Field label="Password">
            <input className={input} type="password" autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} />
          </Field>
          <ErrorNote>{error}</ErrorNote>
          <button type="submit" disabled={busy} className={`${btn.primary} w-full`}>
            {busy ? <Spinner /> : 'Log in'}
          </button>
        </div>
      </form>
    </main>
  )
}
