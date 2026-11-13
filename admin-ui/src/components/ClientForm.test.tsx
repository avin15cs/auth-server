import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ClientForm } from './ClientForm'

describe('ClientForm', () => {
  it('guides service clients and rejects human OpenID scopes', async () => {
    const user = userEvent.setup()
    const submit = vi.fn().mockResolvedValue(undefined)
    render(<ClientForm busy={false} submitLabel="Register client" onSubmit={submit} />)

    await user.click(screen.getByRole('radio', { name: /service workload/i }))
    await user.type(screen.getByLabelText(/client id/i), 'jobs-service')
    await user.type(screen.getByLabelText(/display name/i), 'Jobs Service')
    await user.type(screen.getByLabelText(/api audience/i), 'jobs-api')
    const scopes = screen.getByLabelText(/allowed scopes/i)
    await user.clear(scopes)
    await user.type(scopes, 'openid')
    await user.click(screen.getByRole('button', { name: /register client/i }))

    expect(await screen.findByText(/service clients cannot request openid/i)).toBeInTheDocument()
    expect(submit).not.toHaveBeenCalled()

    await user.clear(scopes)
    await user.type(scopes, 'jobs.read, jobs.write')
    await user.click(screen.getByRole('button', { name: /register client/i }))
    expect(submit).toHaveBeenCalledWith(expect.objectContaining({
      clientType: 'SERVICE',
      publicClient: false,
      grantTypes: ['client_credentials'],
      scopes: ['jobs.read', 'jobs.write'],
    }))
  })
})
