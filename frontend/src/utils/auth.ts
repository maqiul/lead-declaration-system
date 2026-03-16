import Cookies from 'js-cookie'

const TokenKey = 'satoken'

export function getToken(): string | null {
  return Cookies.get(TokenKey) || null
}

export function setToken(token: string): string | undefined {
  return Cookies.set(TokenKey, token)
}

export function removeToken(): void {
  Cookies.remove(TokenKey)
}