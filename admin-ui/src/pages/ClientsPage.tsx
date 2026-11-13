import { FormEvent, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { errorMessage } from '../api'
import { ArrowIcon, PlusIcon, SearchIcon } from '../components/Icons'
import { EmptyState, ErrorBanner, LoadingBlock, PageHeader, Pagination, StatusPill } from '../components/Ui'
import { useSession } from '../session'
import type { ClientPage } from '../types'

const PAGE_SIZE = 20

export function ClientsPage() {
  const { api } = useSession()
  const [page, setPage] = useState<ClientPage | null>(null)
  const [query, setQuery] = useState('')
  const [search, setSearch] = useState('')
  const [offset, setOffset] = useState(0)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    api.get<ClientPage>(`/api/admin/clients?search=${encodeURIComponent(search)}&limit=${PAGE_SIZE}&offset=${offset}`)
      .then((result) => {
        if (active) { setPage(result); setError('') }
      })
      .catch((reason: unknown) => active && setError(errorMessage(reason)))
    return () => { active = false }
  }, [api, search, offset])

  function submit(event: FormEvent) {
    event.preventDefault()
    setOffset(0)
    setSearch(query.trim())
  }

  return (
    <div className="page">
      <PageHeader eyebrow="Applications" title="OAuth clients" description="Register and govern every application and workload that trusts this issuer." action={<Link className="button button--primary" to="/clients/new"><PlusIcon />Register client</Link>} />
      <section className="panel panel--table">
        <form className="table-toolbar" role="search" onSubmit={submit}>
          <label className="search-field"><SearchIcon /><span className="sr-only">Search clients</span><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search name, client ID, or audience" /></label>
          <button className="button button--ghost" type="submit">Search</button>
        </form>
        {error && <ErrorBanner message={error} />}
        {!page && !error && <LoadingBlock />}
        {page && page.content.length === 0 && <EmptyState title="No clients found">Try another search or register the first managed client.</EmptyState>}
        {page && page.content.length > 0 && (
          <>
            <div className="table-scroll">
              <table>
                <thead><tr><th>Client</th><th>Type</th><th>Audience</th><th>Grants</th><th>Status</th><th><span className="sr-only">Open</span></th></tr></thead>
                <tbody>{page.content.map((client) => (
                  <tr key={client.id}>
                    <td><strong>{client.name}</strong><small>{client.clientId}</small></td>
                    <td><span className="type-pill">{client.clientType === 'SERVICE' ? 'Service' : 'Interactive'}</span></td>
                    <td><code>{client.audience}</code></td>
                    <td><span className="compact-text">{client.grantTypes.map(formatGrant).join(', ')}</span></td>
                    <td><StatusPill enabled={client.enabled} /></td>
                    <td><Link className="icon-link" aria-label={`Open ${client.name}`} to={`/clients/${client.id}`}><ArrowIcon /></Link></td>
                  </tr>
                ))}</tbody>
              </table>
            </div>
            <Pagination total={page.total} limit={page.limit} offset={page.offset} onChange={setOffset} />
          </>
        )}
      </section>
    </div>
  )
}

function formatGrant(grant: string) {
  return grant.replaceAll('_', ' ')
}
