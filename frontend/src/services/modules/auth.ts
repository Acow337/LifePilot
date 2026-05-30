import { apiGet, apiPost } from '../http'
import type { LoginForm, UserDTO } from '../types'

export const login = (form: LoginForm) => apiPost<string>('/user/login', form)

export const getMe = () => apiGet<UserDTO>('/user/me')
