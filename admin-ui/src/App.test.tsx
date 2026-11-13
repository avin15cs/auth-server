import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { App } from './App'
import { adminSession } from './test/fixtures'
import { jsonResponse } from './test/render'

describe('authentication states', () => {
  it('shows the Google login path for an unauthenticated browser', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({ title: 'Unauthorized' }, 401)))
    render(<MemoryRouter><App /></MemoryRouter>)

    const link = await screen.findByRole('link', { name: /continue with google/i })
    expect(link).toHaveAttribute('href', '/oauth2/authorization/google')
  })

  it('shows forbidden without requesting management data for a non-admin', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ ...adminSession, admin: false }))
    vi.stubGlobal('fetch', fetchMock)
    render(<MemoryRouter><App /></MemoryRouter>)

    expect(await screen.findByRole('heading', { name: /administrator access required/i })).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })

  it('loads the dashboard only after an admin session is established', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse(adminSession))
      .mockResolvedValueOnce(jsonResponse({ content: [], total: 2, limit: 1, offset: 0 }))
      .mockResolvedValueOnce(jsonResponse({ content: [], total: 3, limit: 1, offset: 0 }))
      .mockResolvedValueOnce(jsonResponse({ status: 'UP' }))
    vi.stubGlobal('fetch', fetchMock)
    render(<MemoryRouter initialEntries={['/dashboard']}><App /></MemoryRouter>)

    expect(await screen.findByRole('heading', { name: /welcome back, ada/i })).toBeInTheDocument()
    await waitFor(() => expect(screen.getByText('2')).toBeInTheDocument())
  })
})
