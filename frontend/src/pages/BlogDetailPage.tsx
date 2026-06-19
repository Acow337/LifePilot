import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { BlogCommentsPanel } from '../components/BlogCommentsPanel'
import { FallbackImage } from '../components/FallbackImage'
import { getBlogById } from '../services/modules/blog'
import { brief, formatDateTime, pickFirstImage } from '../utils/format'

export function BlogDetailPage() {
  const { id } = useParams()
  const blogId = Number(id)

  const { data: blog, isLoading, error } = useQuery({
    queryKey: ['blog-detail', blogId],
    queryFn: () => getBlogById(blogId),
    enabled: Number.isFinite(blogId) && blogId > 0,
  })

  if (!blogId || Number.isNaN(blogId)) {
    return (
      <div className="page">
        <p className="error-text">无效的笔记 ID</p>
      </div>
    )
  }

  return (
    <div className="page fade-in">
      <section className="section">
        <div className="section-head">
          <h2>笔记详情</h2>
          <Link to="/" className="text-link">
            返回首页
          </Link>
        </div>

        {isLoading ? <p className="muted">加载中...</p> : null}
        {error ? <p className="error-text">加载失败：{(error as Error).message || '请稍后再试'}</p> : null}

        {blog ? (
          <article className="detail-card blog-detail-card">
            {pickFirstImage(blog.images) ? (
              <FallbackImage src={pickFirstImage(blog.images)} alt={blog.title} className="detail-cover" />
            ) : null}
            <div className="detail-body">
              <h1>{blog.title}</h1>
              <p className="muted">作者：{blog.name || '匿名用户'}</p>
              <p>{brief(blog.content, 500)}</p>
              <div className="meta">
                <span>{blog.liked ?? 0} 赞</span>
                <span>{blog.comments ?? 0} 评</span>
                <span>{formatDateTime(blog.createTime)}</span>
              </div>
            </div>
          </article>
        ) : null}
      </section>

      {blog ? (
        <section className="section">
          <BlogCommentsPanel blogId={blog.id} />
        </section>
      ) : null}
    </div>
  )
}
