export interface ApiResult<T> {
  success: boolean
  errorCode?: string | null
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

export interface BlogComment {
  id: number
  userId: number
  blogId: number
  parentId: number
  answerId: number
  content: string
  liked?: number
  createTime?: string
  nickName?: string
  icon?: string
  replies: BlogComment[]
}

export interface BlogCommentCreatePayload {
  blogId: number
  parentId?: number
  answerId?: number
  content: string
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

export type SeckillOrderState = 'PENDING' | 'SUCCESS' | 'FAIL' | 'UNKNOWN'

export interface SeckillOrderResult {
  orderId: number
  state: SeckillOrderState
  status?: number
  message?: string
}

export interface VoucherOrder {
  id: number
  voucherId: number
  voucherTitle?: string
  voucherType?: number
  shopId?: number
  shopName?: string
  payValue?: number
  actualValue?: number
  payType?: number
  status?: number
  statusText?: string
  createTime?: string
  payTime?: string
  useTime?: string
  refundTime?: string
}

export interface SeckillDlqMessage {
  deliveryTag?: number
  routingKey?: string
  redeliver?: boolean
  messageId?: string
  timestamp?: string
  orderId?: number
  voucherId?: number
  payload?: string
}

export interface SeckillDlqSnapshot {
  queue: string
  messageCount: number
  limit: number
  messages: SeckillDlqMessage[]
}

export interface SeckillDlqReplayResult {
  requested: number
  replayed: number
  failed: number
  scanned: number
  remaining: number
  voucherId?: number
  replayedOrderIds?: number[]
  failures?: string[]
}

export interface SeckillDlqReplayPreviewResult {
  requested: number
  canReplay: number
  scanned: number
  remaining: number
  voucherId?: number
  sampleOrderIds?: number[]
  note?: string
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

export interface ChatBotRequest {
  session_id: string
  user_id: string
  message: string
  user_token?: string
}

export interface ChatBotResponse {
  answer: string
  used_tools?: string[]
  suggestions?: string[]
  cards?: ChatCard[]
}

export interface ChatCard {
  type: 'shop' | 'voucher' | 'reference'
  title: string
  subtitle?: string
  image?: string
  meta?: Record<string, string | number | null | undefined>
}
