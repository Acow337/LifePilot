import axios, { AxiosHeaders, type AxiosError, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { useAuthStore } from '../store/auth'
import type { ApiResult } from './types'

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
      useAuthStore.getState().logout()
      if (window.location.pathname !== '/login') {
        const redirect = encodeURIComponent(window.location.pathname + window.location.search)
        window.location.href = `/login?redirect=${redirect}`
      }
    }
    return Promise.reject(error)
  },
)

const unwrap = <T>(response: AxiosResponse<ApiResult<T>>) => {
  const payload = response.data
  if (!payload.success) {
    throw new Error(payload.errorMsg || '请求失败')
  }
  return payload.data
}

const unwrapWithMeta = <T>(response: AxiosResponse<ApiResult<T>>) => {
  const payload = response.data
  if (!payload.success) {
    throw new Error(payload.errorMsg || '请求失败')
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

export const apiGetWithMeta = async <T>(url: string, config?: AxiosRequestConfig) =>
  unwrapWithMeta<T>(await http.get<ApiResult<T>>(url, config))
