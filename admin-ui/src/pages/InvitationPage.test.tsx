import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { InvitationPage } from './InvitationPage'
import { jsonResponse } from '../test/render'

describe('invitation setup', () => {
  beforeEach(() => {
    window.history.replaceState({}, '', '/invite#token=abcdefghijklmnopqrstuvwxyzABCDEFGH012345678')
  })

  it('lets the recipient choose credentials and sends CSRF', async () => {
    const user = userEvent.setup()
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({
        email: 'new@example.com',
        expiresAt: '2026-12-01T00:00:00Z',
        csrf: { headerName: 'X-CSRF-TOKEN', token: 'invite-csrf' },
      }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
    vi.stubGlobal('fetch', fetchMock)
    render(<InvitationPage />)

    expect(await screen.findByText('new@example.com')).toBeInTheDocument()
    await user.type(screen.getByLabelText(/display name/i), 'New User')
    await user.type(screen.getByLabelText(/^username$/i), 'newuser')
    await user.type(screen.getByLabelText(/^password$/i), 'a-secure-password')
    await user.type(screen.getByLabelText(/confirm password/i), 'a-secure-password')
    await user.click(screen.getByRole('button', { name: /create account/i }))

    expect(await screen.findByRole('heading', { name: /account is ready/i })).toBeInTheDocument()
    expect(fetchMock.mock.calls[1][1]).toEqual(expect.objectContaining({
      method: 'POST',
      headers: expect.objectContaining({ 'X-CSRF-TOKEN': 'invite-csrf' }),
    }))
  })
})
