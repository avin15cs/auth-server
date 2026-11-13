import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { errorMessage } from '../api'
import { ClientForm } from '../components/ClientForm'
import { SecretReveal } from '../components/SecretReveal'
import { ErrorBanner, PageHeader } from '../components/Ui'
import { useSession } from '../session'
import type { ClientCreation, ClientPayload } from '../types'

export function NewClientPage() {
  const { api } = useSession()
  const navigate = useNavigate()
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [creation, setCreation] = useState<ClientCreation | null>(null)

  async function create(payload: ClientPayload) {
    setBusy(true)
    setError('')
    try {
      const created = await api.mutate<ClientCreation>('/api/admin/clients', 'POST', payload)
      if (created.clientSecret) setCreation(created)
      else navigate(`/clients/${created.client.id}`, { replace: true })
    } catch (reason: unknown) {
      setError(errorMessage(reason))
    } finally {
      setBusy(false)
    }
  }

  if (creation?.clientSecret) {
    return <div className="page page--narrow"><SecretReveal clientId={creation.client.clientId} secret={creation.clientSecret} onAcknowledge={() => { setCreation(null); navigate(`/clients/${creation.client.id}`, { replace: true }) }} /></div>
  }
  return (
    <div className="page page--form">
      <div className="breadcrumbs"><Link to="/clients">OAuth clients</Link><span>/</span><span>Register</span></div>
      <PageHeader title="Register a client" description="Create the smallest safe trust relationship for an application or service." />
      {error && <ErrorBanner message={error} />}
      <ClientForm busy={busy} submitLabel="Register client" onSubmit={create} />
    </div>
  )
}
