import { useMutation, useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { getShopDetail } from '../services/modules/shop'
import { getVoucherList, seckillVoucher } from '../services/modules/voucher'
import { formatPrice, formatScore, pickFirstImage } from '../utils/format'

export function ShopDetailPage() {
  const { id } = useParams()
  const shopId = Number(id)

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
              <img src={pickFirstImage(shop.images)} alt={shop.name} className="detail-cover" />
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
        <div className="grid cards-2">
          {vouchers.map((voucher) => (
            <article key={voucher.id} className="card">
              <div className="card-body">
                <h3>{voucher.title}</h3>
                <p className="muted">{voucher.subTitle || voucher.rules || '限时优惠'}</p>
                <div className="meta">
                  <span>到手 {formatPrice(voucher.actualValue)}</span>
                  <span>支付 {formatPrice(voucher.payValue)}</span>
                </div>
                <button
                  className="primary-btn"
                  onClick={() => seckillMutation.mutate(voucher.id)}
                  disabled={seckillMutation.isPending}
                >
                  {seckillMutation.isPending ? '处理中...' : '立即抢购'}
                </button>
              </div>
            </article>
          ))}
        </div>
      </section>
    </div>
  )
}
