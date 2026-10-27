/*
 * Staff login session (Admin / Kitchen) kept in localStorage.
 * NOTE: this only decides which screen to show. Every protected API call is
 * checked again by the backend (Spring Security), which is the real security.
 */
const KEY = 'rh_staff_session'

export function saveAuth(loginResponse) {
  const session = {
    token: loginResponse.accessToken,
    role: loginResponse.role,
    name: loginResponse.name,
    email: loginResponse.email,
    businessType: loginResponse.businessType,
  }
  localStorage.setItem(KEY, JSON.stringify(session))
  return session
}

export function getAuth() {
  try {
    const session = JSON.parse(localStorage.getItem(KEY))
    if (!session?.token) return null
    if (isExpired(session.token)) {
      clearAuth()
      return null
    }
    return session
  } catch {
    return null
  }
}

export function clearAuth() {
  localStorage.removeItem(KEY)
}

// Reads the "exp" time inside the JWT (no verification; the backend does that)
function isExpired(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
    return typeof payload.exp === 'number' && payload.exp * 1000 < Date.now()
  } catch {
    return true
  }
}

export function homePathForRole(role) {
  return role === 'KITCHEN' ? '/kitchen' : '/admin'
}
