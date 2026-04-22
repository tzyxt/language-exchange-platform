import { sessionStore, setSession } from '../stores/session'

export const API_BASE = 'http://localhost:8080/api'
export const API_ORIGIN = 'http://localhost:8080'

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

export async function resetPassword(form) {
  return request('/auth/reset-password', {
    method: 'POST',
    body: JSON.stringify(form)
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
