import type { ReactNode } from 'react'
import { render } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { AdminApi } from '../api'
import { SessionContext } from '../session'
import { adminSession } from './fixtures'

export function renderAdmin(node: ReactNode, route = '/') {
  return render(
    <MemoryRouter initialEntries={[route]}>
      <SessionContext.Provider value={{ session: adminSession, api: new AdminApi(adminSession) }}>
        {node}
      </SessionContext.Provider>
    </MemoryRouter>,
  )
}

export function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}
