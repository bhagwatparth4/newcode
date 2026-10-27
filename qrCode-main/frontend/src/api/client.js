import { API_URL } from '../config/brand'
import { getAuth, clearAuth } from '../auth/authStore'

/*
 * One small wrapper around fetch used by every screen.
 * - Adds the staff JWT when `auth: true`
 * - Turns backend errors ({ error, message }) into readable messages
 * - Logs staff out automatically when the token expires (401)
 */
export class ApiError extends Error {
  constructor(message, status, code) {
    super(message)
    this.status = status
    this.code = code
  }
}

export async function request(path, { method = 'GET', body, auth = false, raw = false } = {}) {
  const headers = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  if (auth) {
    const session = getAuth()
    if (session?.token) headers.Authorization = `Bearer ${session.token}`
  }

  let response
  try {
    response = await fetch(API_URL + path, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    })
  } catch {
    throw new ApiError('Cannot reach the server. Check your internet connection and try again.', 0, 'NETWORK')
  }

  if (auth && response.status === 401) {
    clearAuth()
    if (!window.location.pathname.startsWith('/login')) window.location.assign('/login?expired=1')
    throw new ApiError('Your session has ended. Please log in again.', 401, 'UNAUTHORIZED')
  }

  if (response.status === 429) {
    throw new ApiError('Too many attempts. Please wait a minute and try again.', 429, 'RATE_LIMITED')
  }

  if (!response.ok) {
    let data = null
    try {
      data = await response.json()
    } catch {
      /* empty body */
    }
    const message =
      (data && data.message) ||
      (response.status === 403 ? 'You do not have permission to do this.' : 'Something went wrong. Please try again.')
    throw new ApiError(message, response.status, data?.error)
  }

  if (raw) return response
  if (response.status === 204) return null

  const text = await response.text()
  if (!text) return null
  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}

// Download a file (Excel, QR PNG) from a protected endpoint and save it
export async function downloadFile(path, fileName) {
  const response = await request(path, { auth: true, raw: true })
  const blob = await response.blob()
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

// Upload one photo (Admin only). Returns the public URL chosen by the backend.
export async function uploadImage(file, folder) {
  const session = getAuth()
  const form = new FormData()
  form.append('file', file)

  let response
  try {
    response = await fetch(`${API_URL}/api/admin/uploads/image?folder=${encodeURIComponent(folder)}`, {
      method: 'POST',
      headers: session?.token ? { Authorization: `Bearer ${session.token}` } : {},
      body: form,
    })
  } catch {
    throw new ApiError('Cannot reach the server. Check your internet connection and try again.', 0, 'NETWORK')
  }

  if (response.status === 401) {
    clearAuth()
    window.location.assign('/login?expired=1')
    throw new ApiError('Your session has ended. Please log in again.', 401, 'UNAUTHORIZED')
  }
  if (response.status === 429) {
    throw new ApiError('Too many uploads. Please wait a minute and try again.', 429, 'RATE_LIMITED')
  }

  let data = null
  try {
    data = await response.json()
  } catch {
    /* empty body */
  }
  if (!response.ok || !data?.url) {
    throw new ApiError(data?.message || 'Photo upload failed. Please try again.', response.status, data?.error)
  }
  return data.url
}
