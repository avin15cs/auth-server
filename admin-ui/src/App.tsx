import { useEffect, useMemo, useState } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { AdminApi, ApiError, loadSession } from './api'
import { SessionContext } from './session'
import type { Session } from './types'
import { Layout } from './components/Layout'
import { LoadingBlock } from './components/Ui'
import { DashboardPage } from './pages/DashboardPage'
import { ClientsPage } from './pages/ClientsPage'
import { NewClientPage } from './pages/NewClientPage'
import { ClientDetailsPage } from './pages/ClientDetailsPage'
import { UsersPage } from './pages/UsersPage'
import { ShieldIcon } from './components/Icons'

type AuthState = { kind: 'loading' } | { kind: 'unauthenticated' } | { kind: 'ready'; session: Session } | { kind: 'error' }

export function App() {
  const [auth, setAuth] = useState<AuthState>({ kind: 'loading' })

  useEffect(() => {
    let active = true
    loadSession()
      .then((session) => active && setAuth({ kind: 'ready', session }))
      .catch((error: unknown) => {
        if (!active) return
        setAuth(error instanceof ApiError && error.status === 401 ? { kind: 'unauthenticated' } : { kind: 'error' })
      })
    return () => { active = false }
  }, [])

  if (auth.kind === 'loading') return <CenterState><LoadingBlock label="Verifying your secure session" /></CenterState>
  if (auth.kind === 'unauthenticated') {
    return (
      <CenterState>
        <div className="auth-card">
          <span className="auth-card__icon"><ShieldIcon /></span>
          <p className="eyebrow">Auth Control</p>
          <h1>Security starts with identity.</h1>
          <p>Sign in with your verified Google account to continue to the authorization platform.</p>
          <a className="button button--primary button--wide" href="/oauth2/authorization/google">Continue with Google</a>
        </div>
      </CenterState>
    )
  }
  if (auth.kind === 'error') {
    return <CenterState><div className="auth-card"><h1>Unable to load Auth Control</h1><p>The service is temporarily unavailable. Refresh to try again.</p><button className="button button--primary" onClick={() => window.location.reload()}>Refresh</button></div></CenterState>
  }
  if (!auth.session.admin) {
    return <ForbiddenState session={auth.session} />
  }
  return <AuthenticatedApp session={auth.session} />
}

function AuthenticatedApp({ session }: { session: Session }) {
  const api = useMemo(() => new AdminApi(session), [session])
  return (
    <SessionContext.Provider value={{ session, api }}>
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="clients" element={<ClientsPage />} />
          <Route path="clients/new" element={<NewClientPage />} />
          <Route path="clients/:id" element={<ClientDetailsPage />} />
          <Route path="users" element={<UsersPage />} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Route>
      </Routes>
    </SessionContext.Provider>
  )
}

function CenterState({ children }: { children: React.ReactNode }) {
  return <main className="center-state">{children}</main>
}

function ForbiddenState({ session }: { session: Session }) {
  async function signOut() {
    await fetch('/logout', {
      method: 'POST',
      credentials: 'same-origin',
      headers: { [session.csrf.headerName]: session.csrf.token },
    })
    window.location.assign('/')
  }

  return <CenterState><div className="auth-card"><span className="auth-card__icon auth-card__icon--warning">!</span><p className="eyebrow">Access restricted</p><h1>Administrator access required.</h1><p>You are signed in, but your account does not have the persisted <code>AUTH_ADMIN</code> role.</p><button className="button button--ghost button--wide" type="button" onClick={signOut}>Sign out</button></div></CenterState>
}
