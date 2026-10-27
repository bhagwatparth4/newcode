import { Navigate } from 'react-router-dom'
import { getAuth, homePathForRole } from './authStore'

// Screen guard only. The backend independently rejects wrong roles.
export default function RequireRole({ role, children }) {
  const session = getAuth()
  if (!session) return <Navigate to="/login" replace />
  if (session.role !== role) return <Navigate to={homePathForRole(session.role)} replace />
  return children
}
