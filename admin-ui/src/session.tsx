import { createContext, useContext } from 'react'
import type { AdminApi } from './api'
import type { Session } from './types'

export interface SessionContextValue {
  session: Session
  api: AdminApi
}

export const SessionContext = createContext<SessionContextValue | null>(null)

export function useSession(): SessionContextValue {
  const value = useContext(SessionContext)
  if (!value) throw new Error('Session context is unavailable')
  return value
}
