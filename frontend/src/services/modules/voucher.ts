import { apiGet, apiPost } from '../http'
import type { Voucher } from '../types'

export const getVoucherList = (shopId: number) => apiGet<Voucher[]>(`/voucher/list/${shopId}`)

export const seckillVoucher = (id: number) => apiPost<number>(`/voucher-order/seckill/${id}`)
