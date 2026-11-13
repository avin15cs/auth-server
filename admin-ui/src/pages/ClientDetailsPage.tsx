import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { errorMessage } from '../api'
import { ClientForm } from '../components/ClientForm'
import { ConfirmDialog, ErrorBanner, LoadingBlock, PageHeader, StatusPill } from '../components/Ui'
import { useSession } from '../session'
import type { ClientPayload, OAuthClient } from '../types'

export function ClientDetailsPage() {
  const { id = '' } = useParams()
  const { api } = useSession()
  const [client, setClient] = useState<OAuthClient | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [confirmDisable, setConfirmDisable] = useState(false)

  useEffect(() => {
    let active = true
    api.get<OAuthClient>(`/api/admin/clients/${encodeURIComponent(id)}`)
      .then((result) => active && setClient(result))
      .catch((reason: unknown) => active && setError(errorMessage(reason)))
    return () => { active = false }
  }, [api, id])

  async function update(payload: ClientPayload) {
    setBusy(true)
    setError('')
    setNotice('')
    try {
      const updated = await api.mutate<OAuthClient>(`/api/admin/clients/${encodeURIComponent(id)}`, 'PUT', payload)
      setClient(updated)
      setNotice('Client configuration saved.')
    } catch (reason: unknown) {
      setError(errorMessage(reason))
    } finally {
      setBusy(false)
    }
  }

  async function setEnabled(enabled: boolean) {
    setBusy(true)
    setConfirmDisable(false)
    setError('')
    try {
      const updated = await api.mutate<OAuthClient>(`/api/admin/clients/${encodeURIComponent(id)}/enabled`, 'PUT', { enabled })
      setClient(updated)
      setNotice(enabled ? 'Client enabled.' : 'Client disabled. New authorization and token requests are blocked.')
    } catch (reason: unknown) {
      setError(errorMessage(reason))
    } finally {
      setBusy(false)
    }
  }

  if (!client && !error) return <div className="page"><LoadingBlock label="Loading client configuration" /></div>

  return (
    <div className="page page--form">
      <div className="breadcrumbs"><Link to="/clients">OAuth clients</Link><span>/</span><span>{client?.clientId || 'Details'}</span></div>
      {error && !client ? <ErrorBanner message={error} /> : client && (
        <>
          <PageHeader eyebrow={client.clientType === 'SERVICE' ? 'Service workload' : 'Interactive application'} title={client.name} description={`Client ID · ${client.clientId}`} action={<div className="header-actions"><StatusPill enabled={client.enabled} />{client.enabled ? <button className="button button--danger-quiet" type="button" disabled={busy} onClick={() => setConfirmDisable(true)}>Disable client</button> : <button className="button button--primary" type="button" disabled={busy} onClick={() => setEnabled(true)}>Enable client</button>}</div>} />
          <div className="detail-strip"><div><small>Audience</small><code>{client.audience}</code></div><div><small>Authentication</small><strong>{client.publicClient ? 'Public + PKCE' : 'Confidential'}</strong></div><div><small>Created</small><strong>{new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(new Date(client.createdAt))}</strong></div></div>
          {error && <ErrorBanner message={error} />}
          {notice && <div className="alert alert--success" role="status">{notice}</div>}
          <ClientForm key={client.updatedAt} client={client} busy={busy} submitLabel="Save changes" onSubmit={update} />
          <ConfirmDialog open={confirmDisable} title="Disable this client?" description="New authorization and token requests will fail immediately. Existing self-contained access tokens remain valid until expiry." confirmLabel="Disable client" dangerous onCancel={() => setConfirmDisable(false)} onConfirm={() => setEnabled(false)} />
        </>
      )}
    </div>
  )
}
