import { apiGet, apiPost } from '../http'
import type { SeckillOrderResult, Voucher } from '../types'

export const getVoucherList = (shopId: number) => apiGet<Voucher[]>(`/voucher/list/${shopId}`)

export const seckillVoucher = (id: number) => apiPost<SeckillOrderResult>(`/voucher-order/seckill/${id}`)

export const querySeckillOrderStatus = (orderId: number) => apiGet<SeckillOrderResult>(`/voucher-order/status/${orderId}`)
