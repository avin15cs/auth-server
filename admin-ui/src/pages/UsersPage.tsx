import { FormEvent, useEffect, useState } from 'react'
import { errorMessage } from '../api'
import { SearchIcon } from '../components/Icons'
import { ConfirmDialog, EmptyState, ErrorBanner, LoadingBlock, PageHeader, Pagination } from '../components/Ui'
import { useSession } from '../session'
import type { AuthUser, InvitationCreation, UserPage } from '../types'

const PAGE_SIZE = 20
const DEFAULT_AUDIENCE = 'url-shortener-api'

interface PendingRoleChange {
  user: AuthUser
  audience: string
  enabled: boolean
}

export function UsersPage() {
  const { api, session } = useSession()
  const [page, setPage] = useState<UserPage | null>(null)
  const [query, setQuery] = useState('')
  const [search, setSearch] = useState('')
  const [offset, setOffset] = useState(0)
  const [audience, setAudience] = useState(DEFAULT_AUDIENCE)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [pending, setPending] = useState<PendingRoleChange | null>(null)
  const [revision, setRevision] = useState(0)
  const [inviteEmail, setInviteEmail] = useState('')
  const [inviteAdmin, setInviteAdmin] = useState(false)
  const [invitation, setInvitation] = useState<InvitationCreation | null>(null)
  const [inviteError, setInviteError] = useState('')
  const [inviting, setInviting] = useState(false)
  const [copied, setCopied] = useState(false)

  useEffect(() => {
    let active = true
    api.get<UserPage>(`/api/admin/users?search=${encodeURIComponent(search)}&limit=${PAGE_SIZE}&offset=${offset}`)
      .then((result) => {
        if (active) { setPage(result); setError('') }
      })
      .catch((reason: unknown) => active && setError(errorMessage(reason)))
    return () => { active = false }
  }, [api, search, offset, revision])

  function submit(event: FormEvent) {
    event.preventDefault()
    setOffset(0)
    setSearch(query.trim())
  }

  async function replaceRole() {
    if (!pending) return
    const current = new Set(pending.user.roles[pending.audience] || [])
    if (pending.enabled) current.add('URL_ADMIN')
    else current.delete('URL_ADMIN')
    setError('')
    try {
      await api.mutate<void>(`/api/admin/users/${pending.user.id}/roles/${encodeURIComponent(pending.audience)}`, 'PUT', { roles: [...current] })
      setNotice(`${pending.user.displayName || pending.user.email || 'User'} roles updated for ${pending.audience}.`)
      setPending(null)
      setRevision((value) => value + 1)
    } catch (reason: unknown) {
      setPending(null)
      setError(errorMessage(reason))
    }
  }

  async function createInvitation(event: FormEvent) {
    event.preventDefault()
    setInviting(true)
    setInviteError('')
    try {
      const created = await api.mutate<InvitationCreation>('/api/admin/invitations', 'POST', {
        email: inviteEmail.trim(),
        grantAuthAdmin: inviteAdmin,
      })
      setInvitation(created)
      setInviteEmail('')
      setInviteAdmin(false)
      setCopied(false)
    } catch (reason: unknown) {
      setInviteError(errorMessage(reason))
    } finally {
      setInviting(false)
    }
  }

  async function copyInvitation() {
    if (!invitation) return
    await navigator.clipboard.writeText(invitation.setupUrl)
    setCopied(true)
  }

  return (
    <div className="page">
      <PageHeader eyebrow="Human identities" title="Users & roles" description="Assign human roles only within the resource-server audience where they apply." />
      {session.invitationsEnabled && <section className="panel invitation-panel" aria-labelledby="invite-heading">
        <div>
          <p className="eyebrow">Password access</p>
          <h2 id="invite-heading">Invite a user</h2>
          <p>Create a one-time setup link. The recipient chooses their username and password; the plaintext password never passes through an administrator.</p>
        </div>
        {inviteError && <ErrorBanner message={inviteError} />}
        {!invitation && (
          <form className="invitation-form" onSubmit={createInvitation}>
            <label className="field"><span>Email address</span><input type="email" value={inviteEmail} onChange={(event) => setInviteEmail(event.target.value)} autoComplete="off" required /></label>
            <label className="role-toggle"><input type="checkbox" checked={inviteAdmin} onChange={(event) => setInviteAdmin(event.target.checked)} /><span>Grant AUTH_ADMIN when accepted</span></label>
            <button className="button button--primary" type="submit" disabled={inviting}>{inviting ? 'Creating…' : 'Create invitation'}</button>
          </form>
        )}
        {invitation && (
          <div className="invitation-result" role="status">
            <strong>Copy this setup link now</strong>
            <p>It is shown once and expires {new Date(invitation.invitation.expiresAt).toLocaleString()}.</p>
            <div><code data-testid="one-time-invitation">{invitation.setupUrl}</code><button className="button button--ghost button--small" type="button" onClick={copyInvitation}>{copied ? 'Copied' : 'Copy link'}</button></div>
            <button className="text-button" type="button" onClick={() => setInvitation(null)}>I saved the link</button>
          </div>
        )}
      </section>}
      <section className="panel panel--table">
        <div className="table-toolbar table-toolbar--users">
          <form role="search" onSubmit={submit} className="inline-search"><label className="search-field"><SearchIcon /><span className="sr-only">Search users</span><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search name or email" /></label><button className="button button--ghost" type="submit">Search</button></form>
          <label className="audience-select"><span>Role audience</span><input value={audience} onChange={(event) => setAudience(event.target.value)} aria-label="Role audience" /></label>
        </div>
        {error && <ErrorBanner message={error} />}
        {notice && <div className="alert alert--success" role="status">{notice}</div>}
        {!page && !error && <LoadingBlock />}
        {page && page.content.length === 0 && <EmptyState title="No users found">Users appear after Google login or accepting an administrator invitation.</EmptyState>}
        {page && page.content.length > 0 && (
          <>
            <div className="user-list">{page.content.map((user) => {
              const assigned = (user.roles[audience] || []).includes('URL_ADMIN')
              return (
                <article className="user-row" key={user.id}>
                  <span className="avatar avatar--large">{(user.displayName || user.email || 'U').slice(0, 1).toUpperCase()}</span>
                  <div className="user-row__identity"><strong>{user.displayName || 'Unnamed user'}</strong><small>{user.email || 'No email available'} {user.emailVerified && <span className="verified">Verified</span>}</small></div>
                  <div className="user-row__roles"><small>{audience}</small><label className="role-toggle"><input type="checkbox" checked={assigned} onChange={(event) => setPending({ user, audience, enabled: event.target.checked })} /><span>URL_ADMIN</span></label></div>
                </article>
              )
            })}</div>
            <Pagination total={page.total} limit={page.limit} offset={page.offset} onChange={setOffset} />
          </>
        )}
      </section>
      <ConfirmDialog open={Boolean(pending)} title={pending?.enabled ? 'Grant URL_ADMIN?' : 'Remove URL_ADMIN?'} description={`This changes access only for ${pending?.audience || audience}. Other audience assignments are untouched.`} confirmLabel={pending?.enabled ? 'Grant role' : 'Remove role'} dangerous={!pending?.enabled} onCancel={() => setPending(null)} onConfirm={replaceRole} />
    </div>
  )
}
