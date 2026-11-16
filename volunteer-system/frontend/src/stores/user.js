import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { post, TOKEN_KEY, USER_KEY } from '@/api/http'

function readUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch (e) {
    return null
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const user = ref(readUser())
  const role = computed(() => (user.value ? user.value.role : ''))
  const displayName = computed(() => (user.value ? user.value.realName || user.value.username : ''))

  async function login(username, password) {
    const data = await post('/auth/login', { username, password })
    token.value = data.token
    user.value = data.user
    localStorage.setItem(TOKEN_KEY, data.token)
    localStorage.setItem(USER_KEY, JSON.stringify(data.user))
    return data.user
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return { token, user, role, displayName, login, logout }
})
