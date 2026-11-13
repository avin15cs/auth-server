import { Route, Routes } from 'react-router-dom'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ClientDetailsPage } from './ClientDetailsPage'
import { ClientsPage } from './ClientsPage'
import { UsersPage } from './UsersPage'
import { adminSession, serviceClient } from '../test/fixtures'
import { jsonResponse, renderAdmin } from '../test/render'

describe('management flows', () => {
  it('requires confirmation and sends CSRF before disabling a client', async () => {
    const user = userEvent.setup()
    const disabled = { ...serviceClient, enabled: false, updatedAt: '2026-01-02T00:00:00Z' }
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse(serviceClient))
      .mockResolvedValueOnce(jsonResponse(disabled))
    vi.stubGlobal('fetch', fetchMock)
    renderAdmin(<Routes><Route path="/clients/:id" element={<ClientDetailsPage />} /></Routes>, '/clients/client-1')

    await user.click(await screen.findByRole('button', { name: /disable client/i }))
    const dialog = screen.getByRole('dialog')
    expect(dialog).toHaveTextContent(/existing self-contained access tokens/i)
    await user.click(within(dialog).getByRole('button', { name: 'Disable client' }))

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2))
    expect(fetchMock.mock.calls[1][1]).toEqual(expect.objectContaining({
      method: 'PUT',
      headers: expect.objectContaining({ [adminSession.csrf.headerName]: adminSession.csrf.token }),
    }))
  })

  it('confirms an audience-specific role change and preserves CSRF', async () => {
    const user = userEvent.setup()
    const userPage = { content: [{ id: 'person-1', email: 'person@example.com', emailVerified: true, displayName: 'Pat Person', pictureUrl: null, createdAt: '2026-01-01T00:00:00Z', lastLoginAt: '2026-01-01T00:00:00Z', roles: {} }], total: 1, limit: 20, offset: 0 }
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse(userPage))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(jsonResponse({ ...userPage, content: [{ ...userPage.content[0], roles: { 'url-shortener-api': ['URL_ADMIN'] } }] }))
    vi.stubGlobal('fetch', fetchMock)
    renderAdmin(<UsersPage />)

    await user.click(await screen.findByRole('checkbox', { name: /url_admin/i }))
    expect(screen.getByRole('dialog')).toHaveTextContent('url-shortener-api')
    await user.click(screen.getByRole('button', { name: /grant role/i }))

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(3))
    expect(fetchMock.mock.calls[1][0]).toBe('/api/admin/users/person-1/roles/url-shortener-api')
    expect(fetchMock.mock.calls[1][1]).toEqual(expect.objectContaining({ method: 'PUT' }))
  })

  it('presents a safe generic message for server errors', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({ detail: 'stack trace with secret=bad' }, 500)))
    renderAdmin(<ClientsPage />)

    expect(await screen.findByText(/service could not complete the request/i)).toBeInTheDocument()
    expect(screen.queryByText(/secret=bad/i)).not.toBeInTheDocument()
  })
})
