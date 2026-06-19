import axios, { AxiosHeaders, type AxiosError, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { useAuthStore } from '../store/auth'
import type { ApiResult } from './types'

class ApiBusinessError extends Error {
  code: string

  constructor(code: string, message: string) {
    super(message)
    this.code = code
    this.name = 'ApiBusinessError'
  }
}

const defaultErrorMessageByCode: Record<string, string> = {
  BAD_REQUEST: '请求参数有误',
  UNAUTHORIZED: '登录已过期，请重新登录',
  FORBIDDEN: '没有权限执行该操作',
  NOT_FOUND: '请求资源不存在',
  INTERNAL_ERROR: '服务器异常，请稍后重试',
  BIZ_ERROR: '操作失败，请稍后重试',
}

const toBusinessError = (payload: ApiResult<unknown>) => {
  const code = payload.errorCode || 'BIZ_ERROR'
  const message = payload.errorMsg || defaultErrorMessageByCode[code] || '请求失败'
  return new ApiBusinessError(code, message)
}

const redirectToLogin = () => {
  useAuthStore.getState().logout()
  if (window.location.pathname !== '/login') {
    const redirect = encodeURIComponent(window.location.pathname + window.location.search)
    window.location.href = `/login?redirect=${redirect}`
  }
}

const http = axios.create({
  baseURL: '/api',
  timeout: 10_000,
})

http.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token
  if (token) {
    if (!config.headers) {
      config.headers = new AxiosHeaders()
    }
    config.headers.set('authorization', token)
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      redirectToLogin()
    }
    return Promise.reject(error)
  },
)

const handleBusinessAuthError = (payload: ApiResult<unknown>) => {
  if (payload.errorCode === 'UNAUTHORIZED') {
    redirectToLogin()
  }
}

const unwrap = <T>(response: AxiosResponse<ApiResult<T>>) => {
  const payload = response.data
  if (!payload.success) {
    handleBusinessAuthError(payload)
    throw toBusinessError(payload)
  }
  return payload.data
}

const unwrapWithMeta = <T>(response: AxiosResponse<ApiResult<T>>) => {
  const payload = response.data
  if (!payload.success) {
    handleBusinessAuthError(payload)
    throw toBusinessError(payload)
  }
  return {
    data: payload.data,
    total: payload.total ?? 0,
  }
}

export const apiGet = async <T>(url: string, config?: AxiosRequestConfig) =>
  unwrap<T>(await http.get<ApiResult<T>>(url, config))

export const apiPost = async <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
  unwrap<T>(await http.post<ApiResult<T>>(url, data, config))

export const apiPut = async <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
  unwrap<T>(await http.put<ApiResult<T>>(url, data, config))

export const apiPatch = async <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
  unwrap<T>(await http.patch<ApiResult<T>>(url, data, config))

export const apiDelete = async <T>(url: string, config?: AxiosRequestConfig) =>
  unwrap<T>(await http.delete<ApiResult<T>>(url, config))

export const apiGetWithMeta = async <T>(url: string, config?: AxiosRequestConfig) =>
  unwrapWithMeta<T>(await http.get<ApiResult<T>>(url, config))
