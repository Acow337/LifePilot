import { apiGet, apiGetWithMeta, apiPatch, apiPost, apiPut } from '../http'
import type {
  AdminDashboardData,
  AdminLog,
  SeckillDlqReplayPreviewResult,
  AdminUser,
  Blog,
  SeckillDlqReplayResult,
  SeckillDlqSnapshot,
  Shop,
  Voucher,
} from '../types'

interface PageParams {
  page?: number
  size?: number
}

export interface AdminUserQuery extends PageParams {
  keyword?: string
  status?: number
}

export const getAdminUsers = (params: AdminUserQuery) => apiGetWithMeta<AdminUser[]>('/admin/users', { params })

export const updateAdminUserStatus = (id: number, status: number) =>
  apiPatch<void>(`/admin/users/${id}/status`, { status })

export const updateAdminUserRole = (id: number, role: number) =>
  apiPatch<void>(`/admin/users/${id}/role`, { role })

export interface AdminBlogQuery extends PageParams {
  keyword?: string
  status?: number
}

export const getAdminBlogs = (params: AdminBlogQuery) => apiGetWithMeta<Blog[]>('/admin/blogs', { params })

export const reviewBlog = (id: number, action: 'APPROVE' | 'REJECT' | 'OFFLINE', reason?: string) =>
  apiPatch<void>(`/admin/blogs/${id}/review`, { action, reason })

export interface AdminShopQuery extends PageParams {
  keyword?: string
  typeId?: number
}

export const getAdminShops = (params: AdminShopQuery) => apiGetWithMeta<Shop[]>('/admin/shops', { params })

export type AdminShopPayload = Partial<Shop> & {
  name: string
  typeId: number
  images: string
  address: string
  x: number
  y: number
}

export const createAdminShop = (payload: AdminShopPayload) => apiPost<number>('/admin/shops', payload)

export const updateAdminShop = (id: number, payload: AdminShopPayload) => apiPut<void>(`/admin/shops/${id}`, payload)

export interface AdminVoucherQuery extends PageParams {
  type?: number
  shopId?: number
  status?: number
  title?: string
}

export const getAdminVouchers = (params: AdminVoucherQuery) =>
  apiGetWithMeta<Voucher[]>('/admin/vouchers', { params })

export type AdminVoucherPayload = Partial<Voucher> & {
  shopId: number
  title: string
  payValue: number
  actualValue: number
  type: number
  status: number
}

export const createAdminVoucher = (payload: AdminVoucherPayload) => apiPost<number>('/admin/vouchers', payload)

export const updateAdminVoucher = (id: number, payload: AdminVoucherPayload) =>
  apiPut<void>(`/admin/vouchers/${id}`, payload)

export const updateAdminVoucherStatus = (id: number, status: number) =>
  apiPatch<void>(`/admin/vouchers/${id}/status`, { status })

export interface AdminLogQuery extends PageParams {
  operatorId?: number
  module?: string
  action?: string
}

export const getAdminLogs = (params: AdminLogQuery) => apiGetWithMeta<AdminLog[]>('/admin/logs', { params })

export const getAdminDashboard = () => apiGet<AdminDashboardData>('/admin/dashboard')

export const getAdminSeckillDlq = (limit = 20, voucherId?: number) =>
  apiGet<SeckillDlqSnapshot>('/admin/seckill/dlq', {
    params: { limit, voucherId: voucherId || undefined },
  })

export const replayAdminSeckillDlq = (limit = 20, voucherId?: number) =>
  apiPost<SeckillDlqReplayResult>('/admin/seckill/dlq/replay', undefined, {
    params: { limit, voucherId: voucherId || undefined },
  })

export const previewAdminSeckillDlqReplay = (limit = 20, voucherId?: number) =>
  apiPost<SeckillDlqReplayPreviewResult>('/admin/seckill/dlq/replay/preview', undefined, {
    params: { limit, voucherId: voucherId || undefined },
  })
