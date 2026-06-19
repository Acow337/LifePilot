import { useMutation, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { FallbackImage } from '../components/FallbackImage'
import { getShopDetail } from '../services/modules/shop'
import { buyVoucher, getVoucherList, querySeckillOrderStatus, seckillVoucher } from '../services/modules/voucher'
import { formatCentPrice, formatPrice, formatScore, pickFirstImage } from '../utils/format'

export function ShopDetailPage() {
  const { id } = useParams()
  const shopId = Number(id)
  const [seckillTip, setSeckillTip] = useState('')
  const [processingVoucherId, setProcessingVoucherId] = useState<number | null>(null)

  const { data: shop, isLoading, error } = useQuery({
    queryKey: ['shop-detail', shopId],
    queryFn: () => getShopDetail(shopId),
    enabled: Number.isFinite(shopId) && shopId > 0,
  })

  const { data: vouchers = [] } = useQuery({
    queryKey: ['voucher-list', shopId],
    queryFn: () => getVoucherList(shopId),
    enabled: Number.isFinite(shopId) && shopId > 0,
  })

  const seckillMutation = useMutation({
    mutationFn: (voucherId: number) => seckillVoucher(voucherId),
  })

  const buyMutation = useMutation({
    mutationFn: (voucherId: number) => buyVoucher(voucherId),
  })

  const pollSeckillStatus = async (orderId: number) => {
    for (let i = 0; i < 15; i++) {
      await new Promise((resolve) => window.setTimeout(resolve, 1000))
      const status = await querySeckillOrderStatus(orderId)
      if (status.state === 'PENDING') {
        continue
      }
      if (status.state === 'SUCCESS') {
        setSeckillTip(`抢购成功，订单号：${orderId}`)
        return
      }
      setSeckillTip(status.message || '抢购失败，请稍后重试')
      return
    }
    setSeckillTip(`订单仍在排队中，订单号：${orderId}，请稍后刷新`)
  }

  const handleSeckill = async (voucherId: number) => {
    setProcessingVoucherId(voucherId)
    setSeckillTip('')
    try {
      const result = await seckillMutation.mutateAsync(voucherId)
      if (result.state === 'SUCCESS') {
        setSeckillTip(`抢购成功，订单号：${result.orderId}`)
        return
      }
      setSeckillTip(result.message || '抢购请求已受理，正在排队处理')
      await pollSeckillStatus(result.orderId)
    } catch (e) {
      setSeckillTip((e as Error).message || '抢购失败，请稍后重试')
    } finally {
      setProcessingVoucherId(null)
    }
  }

  const handleBuy = async (voucherId: number) => {
    setProcessingVoucherId(voucherId)
    setSeckillTip('')
    try {
      const result = await buyMutation.mutateAsync(voucherId)
      setSeckillTip(result.message || `购买成功，订单号：${result.orderId}`)
    } catch (e) {
      setSeckillTip((e as Error).message || '购买失败，请稍后重试')
    } finally {
      setProcessingVoucherId(null)
    }
  }

  if (!shopId || Number.isNaN(shopId)) {
    return (
      <div className="page">
        <p className="error-text">无效的店铺 ID</p>
      </div>
    )
  }

  return (
    <div className="page fade-in">
      <section className="section">
        <div className="section-head">
          <h2>店铺详情</h2>
          <Link to="/shops" className="text-link">
            返回列表
          </Link>
        </div>

        {isLoading ? <p className="muted">加载中...</p> : null}
        {error ? <p className="error-text">加载失败，请稍后再试</p> : null}

        {shop ? (
          <article className="detail-card">
            {pickFirstImage(shop.images) ? (
              <FallbackImage src={pickFirstImage(shop.images)} alt={shop.name} className="detail-cover" />
            ) : null}
            <div className="detail-body">
              <h1>{shop.name}</h1>
              <p className="muted">{shop.area || shop.address || '暂无地址信息'}</p>
              <div className="meta">
                <span>评分 {formatScore(shop.score)}</span>
                <span>人均 {formatPrice(shop.avgPrice)}</span>
                <span>销量 {shop.sold ?? 0}</span>
              </div>
              <p className="muted">营业时间：{shop.openHours || '暂无'}</p>
            </div>
          </article>
        ) : null}
      </section>

      <section className="section">
        <div className="section-head">
          <h2>优惠券</h2>
        </div>
        {seckillTip ? <p className="muted">{seckillTip}</p> : null}
        <div className="grid cards-2">
          {vouchers.map((voucher) => (
            <article key={voucher.id} className="card">
              <div className="card-body">
                <h3>{voucher.title}</h3>
                <p className="muted">{voucher.subTitle || voucher.rules || '限时优惠'}</p>
                <div className="meta">
                  <span>抵扣 {formatCentPrice(voucher.actualValue)}</span>
                  <span>支付 {formatCentPrice(voucher.payValue)}</span>
                </div>
                <div className="op-row">
                  <button
                    className="primary-btn"
                    onClick={() => (voucher.type === 1 ? handleSeckill(voucher.id) : handleBuy(voucher.id))}
                    disabled={processingVoucherId === voucher.id}
                  >
                    {processingVoucherId === voucher.id ? '处理中...' : voucher.type === 1 ? '立即抢购' : '立即购买'}
                  </button>
                  {voucher.type === 1 ? <span className="status-pill warn">秒杀券</span> : <span className="status-pill">普通券</span>}
                </div>
              </div>
            </article>
          ))}
        </div>
      </section>
    </div>
  )
}
