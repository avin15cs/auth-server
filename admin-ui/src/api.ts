import type { Session } from './types'

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

async function safeMessage(response: Response): Promise<string> {
  if (response.status >= 500) return 'The service could not complete the request. Try again.'
  try {
    const payload = (await response.json()) as { detail?: unknown; title?: unknown }
    if (typeof payload.detail === 'string' && payload.detail.length <= 240) return payload.detail
    if (typeof payload.title === 'string' && payload.title.length <= 120) return payload.title
  } catch {
    // Non-JSON protocol errors are intentionally not reflected into the page.
  }
  return response.status === 403
    ? 'You no longer have permission to perform this action.'
    : 'The request could not be completed.'
}

export async function loadSession(): Promise<Session> {
  const response = await fetch('/api/session', {
    credentials: 'same-origin',
    headers: { Accept: 'application/json' },
  })
  if (!response.ok) throw new ApiError(await safeMessage(response), response.status)
  return response.json() as Promise<Session>
}

export class AdminApi {
  constructor(private readonly session: Session) {}

  async get<T>(path: string): Promise<T> {
    return this.request<T>(path, { method: 'GET' })
  }

  async mutate<T>(path: string, method: 'POST' | 'PUT' | 'DELETE', body?: unknown): Promise<T> {
    return this.request<T>(path, {
      method,
      headers: {
        'Content-Type': 'application/json',
        [this.session.csrf.headerName]: this.session.csrf.token,
      },
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  }

  private async request<T>(path: string, init: RequestInit): Promise<T> {
    const response = await fetch(path, {
      ...init,
      credentials: 'same-origin',
      headers: { Accept: 'application/json', ...init.headers },
    })
    if (!response.ok) throw new ApiError(await safeMessage(response), response.status)
    if (response.status === 204) return undefined as T
    return response.json() as Promise<T>
  }
}

export function errorMessage(error: unknown): string {
  return error instanceof ApiError ? error.message : 'Something went wrong. Try again.'
}
