/**
 * 凭证标识名，必须与后端 sa-token.token-name 一致：sa-token 拿它同时当
 * 请求头名、cookie 名、查询参数名与 Redis key 前缀（StpLogic#setTokenValueToCookie 里 setName(getTokenName())）。
 * 不用行业默认名 satoken，就是为了避开 localhost 上其他项目的同名 cookie 覆写。
 */
export const TOKEN_NAME = 'lead-decl-token'

/**
 * 登录凭证在本地读 localStorage，而不是从 Cookie 里读。
 * axios 请求头需要显式拿到 token，localStorage 按 origin（协议+host+端口）隔离，存取都不会被别的本地服务干扰。
 * 新窗口下载 / <img> 预览 / iframe 这类带不了请求头的请求，则由后端登录时下发的 cookie（已配 cookie.path=/）自动上送。
 */
export function getToken(): string | null {
  return localStorage.getItem(TOKEN_NAME) || null
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_NAME, token)
}

export function removeToken(): void {
  localStorage.removeItem(TOKEN_NAME)
}

/**
 * 给无法自定义请求头的地址（window.open、<a download>）补上凭证参数。
 * 后端 sa-token 允许从请求参数读 token（is-read-body），参数名同样等于 TOKEN_NAME。
 */
export function withAuthToken(url: string): string {
  const token = getToken()
  if (!token) return url
  return `${url}${url.includes('?') ? '&' : '?'}${TOKEN_NAME}=${encodeURIComponent(token)}`
}
