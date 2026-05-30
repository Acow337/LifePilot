import { create } from 'zustand'
import type { UserDTO } from '../services/types'

interface AuthState {
  token: string | null
  user: UserDTO | null
  setToken: (token: string | null) => void
  setUser: (user: UserDTO | null) => void
  logout: () => void
}

const TOKEN_KEY = 'dianping-token'

const readToken = () => {
  if (typeof window === 'undefined') {
    return null
  }
  return window.localStorage.getItem(TOKEN_KEY)
}

export const useAuthStore = create<AuthState>((set) => ({
  token: readToken(),
  user: null,
  setToken: (token) => {
    if (typeof window !== 'undefined') {
      if (token) {
        window.localStorage.setItem(TOKEN_KEY, token)
      } else {
        window.localStorage.removeItem(TOKEN_KEY)
      }
    }
    set(() => ({ token }))
  },
  setUser: (user) => set(() => ({ user })),
  logout: () => {
    if (typeof window !== 'undefined') {
      window.localStorage.removeItem(TOKEN_KEY)
    }
    set(() => ({ token: null, user: null }))
  },
}))
