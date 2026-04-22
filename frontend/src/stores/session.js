import { reactive } from 'vue'

const savedUser = localStorage.getItem('lep_current_user')
const savedToken = localStorage.getItem('lep_token')

export const sessionStore = reactive({
  token: savedToken || '',
  currentUser: savedUser ? JSON.parse(savedUser) : null
})

export function setSession(payload) {
  sessionStore.token = payload?.token || ''
  sessionStore.currentUser = payload?.currentUser || null
  localStorage.setItem('lep_token', sessionStore.token)
  localStorage.setItem('lep_current_user', JSON.stringify(sessionStore.currentUser))
}

export function clearSession() {
  sessionStore.token = ''
  sessionStore.currentUser = null
  localStorage.removeItem('lep_token')
  localStorage.removeItem('lep_current_user')
}
