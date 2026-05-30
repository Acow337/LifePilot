import type { IncomingMessage, ServerResponse } from 'node:http'
import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv, type Plugin } from 'vite'

type ApiResult<T> = {
  success: boolean
  errorMsg: string | null
  data: T
  total: number | null
}

type ShopType = {
  id: number
  name: string
  icon?: string
  sort?: number
}

type Shop = {
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
}

type Blog = {
  id: number
  title: string
  content?: string
  images?: string
  liked?: number
  comments?: number
  icon?: string
  name?: string
}

type Voucher = {
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
}

type HttpResponse = ServerResponse<IncomingMessage>

const shopTypes: ShopType[] = [
  { id: 1, name: '美食', sort: 1 },
  { id: 2, name: '咖啡甜品', sort: 2 },
  { id: 3, name: '休闲娱乐', sort: 3 },
]

const shops: Shop[] = [
  {
    id: 1,
    name: '江边小馆',
    typeId: 1,
    images: 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4',
    area: '滨江区',
    address: '江南大道 188 号',
    avgPrice: 68,
    sold: 1560,
    comments: 620,
    score: 46,
    openHours: '10:00-22:00',
  },
  {
    id: 2,
    name: '深夜食堂',
    typeId: 1,
    images: 'https://images.unsplash.com/photo-1555396273-367ea4eb4db5',
    area: '西湖区',
    address: '文一路 66 号',
    avgPrice: 82,
    sold: 980,
    comments: 403,
    score: 45,
    openHours: '17:00-02:00',
  },
  {
    id: 3,
    name: '青柠咖啡',
    typeId: 2,
    images: 'https://images.unsplash.com/photo-1509042239860-f550ce710b93',
    area: '上城区',
    address: '解放路 102 号',
    avgPrice: 39,
    sold: 2050,
    comments: 830,
    score: 47,
    openHours: '08:00-21:30',
  },
  {
    id: 4,
    name: '奶油工坊',
    typeId: 2,
    images: 'https://images.unsplash.com/photo-1488477181946-6428a0291777',
    area: '拱墅区',
    address: '湖墅南路 76 号',
    avgPrice: 29,
    sold: 1120,
    comments: 290,
    score: 44,
    openHours: '09:30-20:30',
  },
  {
    id: 5,
    name: '星河影城',
    typeId: 3,
    images: 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba',
    area: '钱塘区',
    address: '学源街 201 号',
    avgPrice: 58,
    sold: 740,
    comments: 180,
    score: 43,
    openHours: '10:00-23:30',
  },
  {
    id: 6,
    name: '跃动健身',
    typeId: 3,
    images: 'https://images.unsplash.com/photo-1571902943202-507ec2618e8f',
    area: '余杭区',
    address: '未来科技城 9 号',
    avgPrice: 99,
    sold: 520,
    comments: 150,
    score: 45,
    openHours: '06:00-22:00',
  },
]

const hotBlogs: Blog[] = [
  {
    id: 101,
    title: '周末在江边小馆的轻松晚餐',
    content: '招牌牛肉面很稳，汤头偏清爽，适合和朋友随便聊聊。',
    images: 'https://images.unsplash.com/photo-1498837167922-ddd27525d352',
    liked: 268,
    comments: 43,
    name: '阿青',
  },
  {
    id: 102,
    title: '青柠咖啡新品测评',
    content: '新品气泡咖啡酸甜平衡，配柠檬蛋糕刚好。',
    images: 'https://images.unsplash.com/photo-1495474472287-4d71bcdd2085',
    liked: 193,
    comments: 28,
    name: 'Momo',
  },
  {
    id: 103,
    title: '下班后去星河影城看电影',
    content: '工作日人不多，座椅舒适，音响效果比预期好。',
    images: 'https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c',
    liked: 157,
    comments: 19,
    name: '南风',
  },
]

const vouchers: Voucher[] = [
  {
    id: 1001,
    shopId: 1,
    title: '双人套餐 8.5 折',
    subTitle: '仅限周一至周四使用',
    payValue: 168,
    actualValue: 198,
    stock: 120,
    status: 1,
  },
  {
    id: 1002,
    shopId: 1,
    title: '午市单人餐',
    subTitle: '11:00-14:00 可用',
    payValue: 39,
    actualValue: 58,
    stock: 200,
    status: 1,
  },
  {
    id: 2001,
    shopId: 3,
    title: '咖啡买一送一',
    subTitle: '每日限量 50 份',
    payValue: 26,
    actualValue: 52,
    stock: 50,
    status: 1,
  },
]

const toPositiveInt = (value: string | null, fallback: number) => {
  const parsed = Number(value)
  if (!Number.isFinite(parsed) || parsed <= 0) {
    return fallback
  }
  return Math.floor(parsed)
}

const paginate = <T>(items: T[], current: number, pageSize: number) => {
  const start = (current - 1) * pageSize
  return items.slice(start, start + pageSize)
}

const sendJson = <T>(res: HttpResponse, statusCode: number, payload: ApiResult<T>) => {
  res.statusCode = statusCode
  res.setHeader('Content-Type', 'application/json; charset=utf-8')
  res.end(JSON.stringify(payload))
}

const sendOk = <T>(res: HttpResponse, data: T, total: number | null = null) => {
  sendJson(res, 200, {
    success: true,
    errorMsg: null,
    data,
    total,
  })
}

const sendError = (res: HttpResponse, message: string, statusCode = 404) => {
  sendJson(res, statusCode, {
    success: false,
    errorMsg: message,
    data: null,
    total: null,
  })
}

const localMockPlugin = (enabled: boolean): Plugin => ({
  name: 'local-mock-api',
  configureServer(server) {
    if (!enabled) {
      return
    }

    server.middlewares.use((req, res, next) => {
      if (req.method !== 'GET') {
        next()
        return
      }

      const requestUrl = new URL(req.url ?? '/', 'http://localhost')
      const pathname = requestUrl.pathname
      const searchParams = requestUrl.searchParams

      if (pathname === '/api/shop-type/list') {
        sendOk(res, shopTypes, shopTypes.length)
        return
      }

      if (pathname === '/api/shop/of/type') {
        const typeId = toPositiveInt(searchParams.get('typeId'), shopTypes[0]?.id ?? 1)
        const current = toPositiveInt(searchParams.get('current'), 1)
        const filtered = shops.filter((item) => item.typeId === typeId)
        sendOk(res, paginate(filtered, current, 6), filtered.length)
        return
      }

      if (pathname === '/api/shop/of/name') {
        const current = toPositiveInt(searchParams.get('current'), 1)
        const keyword = (searchParams.get('name') ?? '').trim().toLowerCase()
        const filtered = keyword
          ? shops.filter((item) => item.name.toLowerCase().includes(keyword))
          : shops
        sendOk(res, paginate(filtered, current, 6), filtered.length)
        return
      }

      if (pathname === '/api/blog/hot') {
        const current = toPositiveInt(searchParams.get('current'), 1)
        sendOk(res, paginate(hotBlogs, current, 6), hotBlogs.length)
        return
      }

      const shopDetailMatch = pathname.match(/^\/api\/shop\/(\d+)$/)
      if (shopDetailMatch) {
        const id = Number(shopDetailMatch[1])
        const shop = shops.find((item) => item.id === id)
        if (!shop) {
          sendError(res, '店铺不存在')
          return
        }
        sendOk(res, shop)
        return
      }

      const voucherMatch = pathname.match(/^\/api\/voucher\/list\/(\d+)$/)
      if (voucherMatch) {
        const shopId = Number(voucherMatch[1])
        const matched = vouchers.filter((item) => item.shopId === shopId)
        sendOk(res, matched, matched.length)
        return
      }

      next()
    })
  },
})

export default defineConfig(({ mode, command }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const useMock = command === 'serve' && env.VITE_USE_MOCK === 'true'

  return {
    plugins: [react(), localMockPlugin(useMock)],
    server: {
      host: '0.0.0.0',
      port: 5173,
      proxy: {
        '/api': {
          target: 'http://127.0.0.1:8081',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, ''),
        },
      },
    },
  }
})
