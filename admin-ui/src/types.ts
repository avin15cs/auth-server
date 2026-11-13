export interface Session {
  user: {
    id: string
    email: string | null
    displayName: string | null
    pictureUrl: string | null
  }
  admin: boolean
  csrf: {
    headerName: string
    token: string
  }
}

export type ClientType = 'INTERACTIVE' | 'SERVICE'

export interface OAuthClient {
  id: string
  clientId: string
  name: string
  clientType: ClientType
  publicClient: boolean
  audience: string
  enabled: boolean
  grantTypes: string[]
  redirectUris: string[]
  postLogoutRedirectUris: string[]
  scopes: string[]
  authenticationMethods: string[]
  clientIdIssuedAt: string
  createdAt: string
  updatedAt: string
}

export interface ClientPage {
  content: OAuthClient[]
  total: number
  limit: number
  offset: number
}

export interface ClientCreation {
  client: OAuthClient
  clientSecret?: string
}

export interface ClientPayload {
  clientId?: string
  name: string
  clientType?: ClientType
  publicClient?: boolean
  grantTypes: string[]
  redirectUris: string[]
  postLogoutRedirectUris: string[]
  scopes: string[]
  audience: string
}

export interface AuthUser {
  id: string
  email: string | null
  emailVerified: boolean
  displayName: string | null
  pictureUrl: string | null
  createdAt: string
  lastLoginAt: string
  roles: Record<string, string[]>
}

export interface UserPage {
  content: AuthUser[]
  total: number
  limit: number
  offset: number
}
