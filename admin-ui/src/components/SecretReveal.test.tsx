import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { SecretReveal } from './SecretReveal'

describe('SecretReveal', () => {
  it('requires copy-safe acknowledgement before clearing the one-time secret', async () => {
    const user = userEvent.setup()
    const writeText = vi.spyOn(navigator.clipboard, 'writeText')
    const acknowledge = vi.fn()
    render(<SecretReveal clientId="jobs-service" secret="one-time-plaintext" onAcknowledge={acknowledge} />)

    const finish = screen.getByRole('button', { name: /finish registration/i })
    expect(screen.getByTestId('one-time-secret')).toHaveTextContent('one-time-plaintext')
    expect(finish).toBeDisabled()
    expect(localStorage).toHaveLength(0)
    expect(sessionStorage).toHaveLength(0)

    await user.click(screen.getByRole('button', { name: /^copy$/i }))
    expect(writeText).toHaveBeenCalledWith('one-time-plaintext')
    await user.click(screen.getByRole('checkbox'))
    await user.click(finish)
    expect(acknowledge).toHaveBeenCalledOnce()
  })
})
