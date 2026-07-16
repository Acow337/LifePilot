import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { FallbackImage } from '../components/FallbackImage'
import { getHotBlogs } from '../services/modules/blog'
import { getShopTypes } from '../services/modules/shop'
import { brief, pickFirstImage } from '../utils/format'

export function HomePage() {
  const { data: types = [] } = useQuery({
    queryKey: ['shop-types'],
    queryFn: getShopTypes,
  })

  const { data: hotBlogs = [], isLoading } = useQuery({
    queryKey: ['hot-blogs'],
    queryFn: () => getHotBlogs(1),
  })

  return (
    <div className="page fade-in">
      <section className="hero-panel">
        <p className="kicker">LifePilot Local Ops Copilot</p>
        <h1>AI 驱动的本地生活营销履约平台</h1>
        <p className="muted">从活动配置、用户抢券、订单履约到 AI 客服和运营复盘，串起本地生活交易闭环。</p>
        <div className="hero-actions">
          <Link to="/shops" className="primary-btn">浏览本地服务</Link>
          <Link to="/admin/dashboard" className="ghost-btn">进入运营驾驶舱</Link>
        </div>
      </section>

      <section className="section">
        <div className="section-head">
          <h2>店铺分类</h2>
          <Link to="/shops" className="text-link">
            查看全部
          </Link>
        </div>
        <div className="chip-row">
          {types.map((type) => (
            <Link key={type.id} to={`/shops?typeId=${type.id}`} className="chip">
              {type.name}
            </Link>
          ))}
        </div>
      </section>

      <section className="section">
        <div className="section-head">
          <h2>热门笔记</h2>
        </div>
        {isLoading ? <p className="muted">加载中...</p> : null}
        <div className="grid cards-3">
          {hotBlogs.map((blog) => {
            const cover = pickFirstImage(blog.images)
            return (
              <Link key={blog.id} to={`/blog/${blog.id}`} className="card blog-card blog-card-link">
                {cover ? <FallbackImage src={cover} alt={blog.title} className="card-cover" /> : null}
                <div className="card-body">
                  <h3>{blog.title}</h3>
                  <p className="muted">{brief(blog.content, 72)}</p>
                  <div className="meta">
                    <span>{blog.name || '匿名用户'}</span>
                    <span>{blog.liked ?? 0} 赞</span>
                    <span>{blog.comments ?? 0} 评</span>
                  </div>
                  <span className="text-link">进入笔记详情 →</span>
                </div>
              </Link>
            )
          })}
        </div>
      </section>
    </div>
  )
}
