import { apiGet, apiPost } from '../http'
import type { SeckillOrderResult, Voucher, VoucherOrder } from '../types'

export const getVoucherList = (shopId: number) => apiGet<Voucher[]>(`/voucher/list/${shopId}`)

export const seckillVoucher = (id: number) => apiPost<SeckillOrderResult>(`/voucher-order/seckill/${id}`)

export const buyVoucher = (id: number) => apiPost<SeckillOrderResult>(`/voucher-order/${id}`)

export const querySeckillOrderStatus = (orderId: number) => apiGet<SeckillOrderResult>(`/voucher-order/status/${orderId}`)

export const getMyVoucherOrders = () => apiGet<VoucherOrder[]>('/voucher-order/my')

export const payVoucherOrder = (orderId: number) => apiPost<SeckillOrderResult>(`/voucher-order/${orderId}/pay`)

export const redeemVoucherOrder = (orderId: number) => apiPost<SeckillOrderResult>(`/voucher-order/${orderId}/redeem`)

export const cancelVoucherOrder = (orderId: number) => apiPost<SeckillOrderResult>(`/voucher-order/${orderId}/cancel`)

export const requestVoucherRefund = (orderId: number) => apiPost<SeckillOrderResult>(`/voucher-order/${orderId}/refund`)

export const approveVoucherRefund = (orderId: number) => apiPost<SeckillOrderResult>(`/voucher-order/${orderId}/refund/approve`)

export const rejectVoucherRefund = (orderId: number) => apiPost<SeckillOrderResult>(`/voucher-order/${orderId}/refund/reject`)

export const cancelExpiredVoucherOrders = () => apiPost<{ canceled: number; timeoutMinutes: number }>('/voucher-order/expired/cancel')
