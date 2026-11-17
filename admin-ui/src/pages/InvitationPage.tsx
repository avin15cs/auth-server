import { FormEvent, useEffect, useState } from 'react'
import { acceptInvitation, errorMessage, loadInvitation } from '../api'
import { LoadingBlock } from '../components/Ui'
import { ShieldIcon } from '../components/Icons'
import type { InvitationSetup } from '../types'

export function InvitationPage() {
  const [token] = useState(() => new URLSearchParams(window.location.hash.slice(1)).get('token') || '')
  const [setup, setSetup] = useState<InvitationSetup | null>(null)
  const [loadError, setLoadError] = useState(token ? '' : 'This invitation link is incomplete.')
  const [submitError, setSubmitError] = useState('')
  const [complete, setComplete] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (!token) return
    window.history.replaceState({}, '', '/invite')
    loadInvitation(token)
      .then(setSetup)
      .catch((reason: unknown) => setLoadError(errorMessage(reason)))
  }, [token])

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!setup) return
    const data = new FormData(event.currentTarget)
    const password = String(data.get('password') || '')
    if (password !== String(data.get('confirmPassword') || '')) {
      setSubmitError('Passwords do not match.')
      return
    }
    setSubmitting(true)
    setSubmitError('')
    try {
      await acceptInvitation(token, setup, {
        username: String(data.get('username') || ''),
        displayName: String(data.get('displayName') || ''),
        password,
      })
      setComplete(true)
    } catch (reason: unknown) {
      setSubmitError(errorMessage(reason))
    } finally {
      setSubmitting(false)
    }
  }

  if (!setup && !loadError) return <main className="center-state"><LoadingBlock label="Validating invitation" /></main>
  if (loadError) return <InvitationCard title="Invitation unavailable"><p>{loadError}</p><a className="button button--ghost button--wide" href="/">Return to sign in</a></InvitationCard>
  if (complete) return <InvitationCard title="Your account is ready."><p>Sign in with the username and password you just created.</p><a className="button button--primary button--wide" href="/">Continue to sign in</a></InvitationCard>

  return (
    <InvitationCard title="Create your account">
      <p>This invitation is for <strong>{setup!.email}</strong> and expires {new Date(setup!.expiresAt).toLocaleString()}.</p>
      {submitError && <div className="alert alert--error" role="alert">{submitError}</div>}
      <form className="local-login" onSubmit={submit}>
        <label><span>Display name</span><input name="displayName" maxLength={100} autoComplete="name" required /></label>
        <label><span>Username</span><input name="username" minLength={3} maxLength={64} pattern="[A-Za-z0-9._-]+" autoComplete="username" required /></label>
        <label><span>Password</span><input name="password" type="password" minLength={12} maxLength={128} autoComplete="new-password" required /></label>
        <label><span>Confirm password</span><input name="confirmPassword" type="password" minLength={12} maxLength={128} autoComplete="new-password" required /></label>
        <button className="button button--primary button--wide" type="submit" disabled={submitting}>{submitting ? 'Creating account…' : 'Create account'}</button>
      </form>
    </InvitationCard>
  )
}

function InvitationCard({ title, children }: { title: string; children: React.ReactNode }) {
  return <main className="center-state"><section className="auth-card"><span className="auth-card__icon"><ShieldIcon /></span><p className="eyebrow">Secure invitation</p><h1>{title}</h1>{children}</section></main>
}
