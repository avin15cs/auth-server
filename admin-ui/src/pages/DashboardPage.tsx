import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useSession } from '../session'
import type { ClientPage, UserPage } from '../types'
import { ArrowIcon, KeyIcon, PlusIcon, ShieldIcon, UsersIcon } from '../components/Icons'
import { ErrorBanner, LoadingBlock, PageHeader } from '../components/Ui'
import { errorMessage } from '../api'

interface Summary {
  clients: number
  users: number
  healthy: boolean
}

export function DashboardPage() {
  const { api, session } = useSession()
  const [summary, setSummary] = useState<Summary | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    Promise.all([
      api.get<ClientPage>('/api/admin/clients?limit=1&offset=0'),
      api.get<UserPage>('/api/admin/users?limit=1&offset=0'),
      fetch('/actuator/health', { credentials: 'same-origin' }).then((response) => response.ok),
    ])
      .then(([clients, users, healthy]) => active && setSummary({ clients: clients.total, users: users.total, healthy }))
      .catch((reason: unknown) => active && setError(errorMessage(reason)))
    return () => { active = false }
  }, [api])

  return (
    <div className="page">
      <PageHeader eyebrow="Control plane" title={`Welcome back, ${session.user.displayName?.split(' ')[0] || 'administrator'}.`} description="A clear view of the identities and applications that trust this authorization server." action={<Link className="button button--primary" to="/clients/new"><PlusIcon />Register client</Link>} />
      {error && <ErrorBanner message={error} />}
      {!summary && !error ? <LoadingBlock /> : summary && (
        <section className="metric-grid" aria-label="Platform summary">
          <article className="metric-card"><span className="metric-card__icon metric-card__icon--blue"><KeyIcon /></span><div><small>OAuth clients</small><strong>{summary.clients}</strong><p>Interactive and service registrations</p></div></article>
          <article className="metric-card"><span className="metric-card__icon metric-card__icon--violet"><UsersIcon /></span><div><small>Known users</small><strong>{summary.users}</strong><p>Google-backed internal identities</p></div></article>
          <article className="metric-card"><span className="metric-card__icon metric-card__icon--green"><ShieldIcon /></span><div><small>Platform status</small><strong className="metric-card__status"><i className={summary.healthy ? 'dot dot--on' : 'dot dot--off'} />{summary.healthy ? 'Operational' : 'Unavailable'}</strong><p>Readiness endpoint signal</p></div></article>
        </section>
      )}
      <section className="dashboard-grid">
        <article className="panel panel--feature">
          <p className="eyebrow">Secure by design</p>
          <h2>Provider-neutral identity, one control plane.</h2>
          <p>Google authenticates people. This platform owns application credentials, stable user identifiers, audience-scoped roles, and every privileged change.</p>
          <div className="trust-flow" aria-label="Trust flow"><span>Google</span><b>→</b><span>Auth server</span><b>→</b><span>Your APIs</span></div>
        </article>
        <article className="panel">
          <div className="panel__heading"><div><p className="eyebrow">Quick actions</p><h2>Manage the platform</h2></div></div>
          <div className="action-list">
            <Link to="/clients/new"><span><PlusIcon /></span><div><strong>Register a client</strong><small>Interactive app or machine workload</small></div><ArrowIcon /></Link>
            <Link to="/clients"><span><KeyIcon /></span><div><strong>Review OAuth clients</strong><small>Configuration, scopes, and status</small></div><ArrowIcon /></Link>
            <Link to="/users"><span><UsersIcon /></span><div><strong>Manage user roles</strong><small>Audience-specific access control</small></div><ArrowIcon /></Link>
          </div>
        </article>
      </section>
    </div>
  )
}
