import type { OAuthClient, Session } from '../types'

export const adminSession: Session = {
  user: { id: 'user-1', email: 'admin@example.com', displayName: 'Ada Admin', pictureUrl: null },
  admin: true,
  csrf: { headerName: 'X-CSRF-TOKEN', token: 'csrf-test-token' },
}

export const serviceClient: OAuthClient = {
  id: 'client-1',
  clientId: 'reporting-service',
  name: 'Reporting Service',
  clientType: 'SERVICE',
  publicClient: false,
  audience: 'reporting-api',
  enabled: true,
  grantTypes: ['client_credentials'],
  redirectUris: [],
  postLogoutRedirectUris: [],
  scopes: ['reports.read'],
  authenticationMethods: ['client_secret_basic'],
  clientIdIssuedAt: '2026-01-01T00:00:00Z',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}
