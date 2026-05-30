import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
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
        <p className="kicker">Minimal lifestyle map</p>
        <h1>发现城市里的好店与好内容</h1>
        <p className="muted">后端接口直连，实时展示热门笔记和门店信息。</p>
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
              <article key={blog.id} className="card blog-card">
                {cover ? <img src={cover} alt={blog.title} className="card-cover" /> : null}
                <div className="card-body">
                  <h3>{blog.title}</h3>
                  <p className="muted">{brief(blog.content, 72)}</p>
                  <div className="meta">
                    <span>{blog.name || '匿名用户'}</span>
                    <span>{blog.liked ?? 0} 赞</span>
                  </div>
                </div>
              </article>
            )
          })}
        </div>
      </section>
    </div>
  )
}
