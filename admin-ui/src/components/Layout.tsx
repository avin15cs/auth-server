import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { GridIcon, KeyIcon, ShieldIcon, UsersIcon } from './Icons'
import { useSession } from '../session'

export function Layout() {
  const { session, api } = useSession()
  const navigate = useNavigate()

  async function logout() {
    try {
      await api.mutate<void>('/logout', 'POST')
    } finally {
      navigate('/', { replace: true })
      window.location.reload()
    }
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <NavLink className="brand" to="/dashboard" aria-label="Auth Control dashboard">
          <span className="brand__mark"><ShieldIcon /></span>
          <span><strong>Auth Control</strong><small>Security platform</small></span>
        </NavLink>
        <nav className="main-nav" aria-label="Main navigation">
          <NavLink to="/dashboard"><GridIcon />Dashboard</NavLink>
          <NavLink to="/clients"><KeyIcon />OAuth clients</NavLink>
          <NavLink to="/users"><UsersIcon />Users & roles</NavLink>
        </nav>
        <div className="sidebar__footer">
          <span className="avatar">{(session.user.displayName || session.user.email || 'A').slice(0, 1).toUpperCase()}</span>
          <div><strong>{session.user.displayName || 'Administrator'}</strong><small>{session.user.email}</small></div>
          <button type="button" className="text-button" onClick={logout}>Sign out</button>
        </div>
      </aside>
      <main className="main-content"><Outlet /></main>
    </div>
  )
}
