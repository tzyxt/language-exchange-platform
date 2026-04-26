import { sessionStore, setSession } from '../stores/session'

function trimTrailingSlash(value) {
  return value.replace(/\/+$/, '')
}

export const API_ORIGIN = trimTrailingSlash(typeof window !== 'undefined' ? window.location.origin : 'http://localhost:8080')
export const API_BASE = trimTrailingSlash(`${API_ORIGIN}/api`)
export const WS_ORIGIN = API_ORIGIN.replace(/^http/i, 'ws')

export async function request(path, options = {}) {
  const headers = { ...(options.headers || {}) }

  if (!(options.body instanceof FormData) && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json'
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers
  })

  const payload = await response.json()
  if (!payload.success) {
    throw new Error(payload.message || '请求失败')
  }
  return payload.data
}

export function currentUserId() {
  return sessionStore.currentUser?.id
}

export async function login(account, password) {
  const data = await request('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ account, password })
  })
  setSession(data)
  return data
}

export async function register(form) {
  const data = await request('/auth/register', {
    method: 'POST',
    body: JSON.stringify(form)
  })
  setSession(data)
  return data
}

export async function sendRegisterEmailCode(email) {
  return request('/auth/email-code', {
    method: 'POST',
    body: JSON.stringify({ email })
  })
}

export async function resetPassword(form) {
  return request('/auth/reset-password', {
    method: 'POST',
    body: JSON.stringify(form)
  })
}

export async function deleteAccount(userId) {
  return request(`/me/${userId}`, {
    method: 'DELETE'
  })
}

export async function checkAccountAvailability(account) {
  const query = new URLSearchParams({ account })
  return request(`/auth/check-account?${query.toString()}`)
}

export async function uploadChatMedia(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request('/uploads/chat', {
    method: 'POST',
    body: formData
  })
}
