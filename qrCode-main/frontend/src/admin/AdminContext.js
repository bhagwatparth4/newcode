import { createContext, useContext } from 'react'

/*
 * Shared admin state:
 *  - businessType: SMALL_CAFE | LARGE_CAFE | RESTAURANT (read-only vendor setting)
 *  - hasKitchen:   true for LARGE_CAFE / RESTAURANT
 *  - liveVersion:  increases on every realtime order event, so pages refetch
 */
export const AdminContext = createContext({ businessType: 'SMALL_CAFE', hasKitchen: false, liveVersion: 0 })

export const useAdmin = () => useContext(AdminContext)

/*
 * Which next step the ADMIN may take (mirror of the backend rule in
 * OrderStatusTransitionPolicy.java; the backend still has the final say).
 */
export function adminNextAction(status, hasKitchen) {
  if (status === 'READY') return { to: 'COMPLETED', label: 'Mark completed' }
  if (hasKitchen) return null
  if (status === 'PAID') return { to: 'PREPARING', label: 'Start preparing' }
  if (status === 'PREPARING') return { to: 'READY', label: 'Mark ready' }
  return null
}
