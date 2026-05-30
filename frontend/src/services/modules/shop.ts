import { apiGet } from '../http'
import type { Shop, ShopType } from '../types'

export const getShopTypes = () => apiGet<ShopType[]>('/shop-type/list')

export const getShopByType = (typeId: number, current = 1) =>
  apiGet<Shop[]>('/shop/of/type', {
    params: { typeId, current },
  })

export const searchShopByName = (name: string, current = 1) =>
  apiGet<Shop[]>('/shop/of/name', {
    params: { name, current },
  })

export const getShopDetail = (id: number) => apiGet<Shop>(`/shop/${id}`)
