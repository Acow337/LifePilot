export interface ApiResult<T> {
  success: boolean
  errorMsg: string | null
  data: T
  total: number | null
}

export interface LoginForm {
  phone: string
  password: string
}

export interface UserDTO {
  id: number
  nickName: string
  icon?: string
  role?: number
}

export interface AdminUser {
  id: number
  phone: string
  nickName: string
  role: number
  status: number
  icon?: string
  createTime?: string
  updateTime?: string
}

export interface ShopType {
  id: number
  name: string
  icon?: string
  sort?: number
}

export interface Shop {
  id: number
  name: string
  typeId: number
  images?: string
  area?: string
  address?: string
  avgPrice?: number
  sold?: number
  comments?: number
  score?: number
  openHours?: string
  distance?: number
  createTime?: string
  updateTime?: string
}

export interface Blog {
  id: number
  shopId?: number
  title: string
  content?: string
  images?: string
  liked?: number
  comments?: number
  icon?: string
  name?: string
  isLike?: boolean
  status?: number
  createTime?: string
}

export interface Voucher {
  id: number
  shopId: number
  title: string
  subTitle?: string
  rules?: string
  payValue?: number
  actualValue?: number
  type?: number
  status?: number
  stock?: number
  beginTime?: string
  endTime?: string
  createTime?: string
}

export interface AdminLog {
  id: number
  operatorId: number
  module: string
  action: string
  targetType: string
  targetId: number
  detail?: string
  createTime?: string
}
