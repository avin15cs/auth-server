import { FormEvent, useMemo, useState } from 'react'
import type { ClientPayload, ClientType, OAuthClient } from '../types'

interface ClientFormProps {
  client?: OAuthClient
  busy: boolean
  submitLabel: string
  onSubmit: (payload: ClientPayload) => Promise<void>
}

export function ClientForm({ client, busy, submitLabel, onSubmit }: ClientFormProps) {
  const editing = Boolean(client)
  const [type, setType] = useState<ClientType>(client?.clientType || 'INTERACTIVE')
  const [clientId, setClientId] = useState(client?.clientId || '')
  const [name, setName] = useState(client?.name || '')
  const [audience, setAudience] = useState(client?.audience || '')
  const [publicClient, setPublicClient] = useState(client?.publicClient ?? true)
  const [refreshToken, setRefreshToken] = useState(client?.grantTypes.includes('refresh_token') ?? true)
  const [scopes, setScopes] = useState(client?.scopes.join(', ') || '')
  const [redirectUris, setRedirectUris] = useState(client?.redirectUris.join('\n') || '')
  const [logoutUris, setLogoutUris] = useState(client?.postLogoutRedirectUris.join('\n') || '')
  const [errors, setErrors] = useState<Record<string, string>>({})

  const service = type === 'SERVICE'
  const helper = useMemo(() => service
    ? 'For trusted server workloads. A generated secret is shown once.'
    : 'For browser-facing applications using Authorization Code with PKCE.', [service])

  function changeType(next: ClientType) {
    if (editing) return
    setType(next)
    if (next === 'SERVICE') {
      setPublicClient(false)
      setRefreshToken(false)
      setRedirectUris('')
      setLogoutUris('')
      setScopes((current) => current || 'api.read')
    }
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    const parsedScopes = splitValues(scopes)
    const redirects = splitLines(redirectUris)
    const postLogout = splitLines(logoutUris)
    const nextErrors: Record<string, string> = {}
    if (!editing && !/^[A-Za-z0-9][A-Za-z0-9._-]{2,99}$/.test(clientId)) nextErrors.clientId = 'Use 3–100 letters, numbers, dots, underscores, or hyphens.'
    if (!name.trim()) nextErrors.name = 'Enter a client name.'
    if (!/^[A-Za-z0-9][A-Za-z0-9._:-]{0,199}$/.test(audience)) nextErrors.audience = 'Enter a valid resource-server audience.'
    if (parsedScopes.length === 0) nextErrors.scopes = 'Add at least one least-privilege scope.'
    if (service && parsedScopes.some((scope) => ['openid', 'profile', 'email'].includes(scope))) nextErrors.scopes = 'Service clients cannot request OpenID user scopes.'
    if (!service && redirects.length === 0) nextErrors.redirectUris = 'Interactive clients need at least one exact redirect URI.'
    if ([...redirects, ...postLogout].some((uri) => !validUri(uri))) nextErrors.redirectUris = 'Use exact absolute HTTPS URIs, or loopback HTTP URIs for local development.'
    if (!service && (parsedScopes.includes('profile') || parsedScopes.includes('email')) && !parsedScopes.includes('openid')) nextErrors.scopes = 'profile and email require the openid scope.'
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) return

    await onSubmit({
      ...(editing ? {} : { clientId: clientId.trim(), clientType: type, publicClient: service ? false : publicClient }),
      name: name.trim(),
      audience: audience.trim(),
      grantTypes: service ? ['client_credentials'] : ['authorization_code', ...(refreshToken ? ['refresh_token'] : [])],
      redirectUris: service ? [] : redirects,
      postLogoutRedirectUris: service ? [] : postLogout,
      scopes: parsedScopes,
    })
  }

  return (
    <form className="client-form" onSubmit={submit} noValidate>
      {!editing && (
        <fieldset className="form-section">
          <legend><span>1</span>Choose a client type</legend>
          <p className="section-help">{helper}</p>
          <div className="choice-grid">
            <label className={type === 'INTERACTIVE' ? 'choice-card choice-card--selected' : 'choice-card'}><input type="radio" name="clientType" value="INTERACTIVE" checked={type === 'INTERACTIVE'} onChange={() => changeType('INTERACTIVE')} /><strong>Interactive application</strong><small>Authorization Code + PKCE</small></label>
            <label className={type === 'SERVICE' ? 'choice-card choice-card--selected' : 'choice-card'}><input type="radio" name="clientType" value="SERVICE" checked={type === 'SERVICE'} onChange={() => changeType('SERVICE')} /><strong>Service workload</strong><small>Client Credentials only</small></label>
          </div>
        </fieldset>
      )}
      <fieldset className="form-section">
        <legend><span>{editing ? '1' : '2'}</span>Identity and audience</legend>
        <div className="form-grid">
          {!editing && <Field label="Client ID" error={errors.clientId} hint="A stable identifier shown to the application."><input value={clientId} onChange={(event) => setClientId(event.target.value)} autoComplete="off" aria-invalid={Boolean(errors.clientId)} /></Field>}
          <Field label="Display name" error={errors.name}><input value={name} onChange={(event) => setName(event.target.value)} aria-invalid={Boolean(errors.name)} /></Field>
          <Field label="API audience" error={errors.audience} hint="The resource server that will accept access tokens."><input value={audience} onChange={(event) => setAudience(event.target.value)} placeholder="url-shortener-api" aria-invalid={Boolean(errors.audience)} /></Field>
        </div>
      </fieldset>
      <fieldset className="form-section">
        <legend><span>{editing ? '2' : '3'}</span>Permissions and flow</legend>
        <Field label="Allowed scopes" error={errors.scopes} hint="Comma-separated. Grant only what this client needs."><input value={scopes} onChange={(event) => setScopes(event.target.value)} placeholder={service ? 'jobs.read, jobs.write' : 'openid, profile, api.read'} aria-invalid={Boolean(errors.scopes)} /></Field>
        {!service && (
          <div className="toggle-row">
            <label><input type="checkbox" checked={refreshToken} onChange={(event) => setRefreshToken(event.target.checked)} /><span><strong>Allow refresh tokens</strong><small>Always rotated by the authorization server.</small></span></label>
            {!editing && <label><input type="checkbox" checked={publicClient} onChange={(event) => setPublicClient(event.target.checked)} /><span><strong>Public client</strong><small>Receives no client secret.</small></span></label>}
          </div>
        )}
      </fieldset>
      {!service && (
        <fieldset className="form-section">
          <legend><span>{editing ? '3' : '4'}</span>Exact redirect destinations</legend>
          <div className="form-grid">
            <Field label="Redirect URIs" error={errors.redirectUris} hint="One exact URI per line. Wildcards are rejected."><textarea rows={4} value={redirectUris} onChange={(event) => setRedirectUris(event.target.value)} placeholder="https://app.example.com/login/oauth2/code/auth" aria-invalid={Boolean(errors.redirectUris)} /></Field>
            <Field label="Post-logout URIs" hint="Optional. One exact URI per line."><textarea rows={4} value={logoutUris} onChange={(event) => setLogoutUris(event.target.value)} placeholder="https://app.example.com/" /></Field>
          </div>
        </fieldset>
      )}
      <div className="form-actions"><button className="button button--primary" type="submit" disabled={busy}>{busy ? 'Saving…' : submitLabel}</button></div>
    </form>
  )
}

function Field({ label, hint, error, children }: { label: string; hint?: string; error?: string; children: React.ReactNode }) {
  return <label className="field"><span>{label}</span>{children}{error ? <small className="field__error">{error}</small> : hint ? <small>{hint}</small> : null}</label>
}

function splitValues(value: string) {
  return [...new Set(value.split(/[\s,]+/).map((item) => item.trim()).filter(Boolean))]
}

function splitLines(value: string) {
  return [...new Set(value.split(/\r?\n/).map((item) => item.trim()).filter(Boolean))]
}

function validUri(value: string) {
  if (value.includes('*')) return false
  try {
    const uri = new URL(value)
    return uri.protocol === 'https:' || (uri.protocol === 'http:' && ['localhost', '127.0.0.1', '[::1]'].includes(uri.hostname))
  } catch {
    return false
  }
}
