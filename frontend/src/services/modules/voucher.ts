import { apiGet, apiPost } from '../http'
import type { SeckillOrderResult, Voucher, VoucherOrder } from '../types'

export const getVoucherList = (shopId: number) => apiGet<Voucher[]>(`/voucher/list/${shopId}`)

export const seckillVoucher = (id: number) => apiPost<SeckillOrderResult>(`/voucher-order/seckill/${id}`)

export const buyVoucher = (id: number) => apiPost<SeckillOrderResult>(`/voucher-order/${id}`)

export const querySeckillOrderStatus = (orderId: number) => apiGet<SeckillOrderResult>(`/voucher-order/status/${orderId}`)

export const getMyVoucherOrders = () => apiGet<VoucherOrder[]>('/voucher-order/my')

export const payVoucherOrder = (orderId: number) => apiPost<SeckillOrderResult>(`/voucher-order/${orderId}/pay`)

export const redeemVoucherOrder = (orderId: number) => apiPost<SeckillOrderResult>(`/voucher-order/${orderId}/redeem`)
