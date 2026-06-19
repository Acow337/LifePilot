import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { FallbackImage } from '../../components/FallbackImage'
import { getAdminBlogs, reviewBlog } from '../../services/modules/admin'
import { brief, formatDateTime, pickFirstImage } from '../../utils/format'

const PAGE_SIZE = 10

export function AdminBlogsPage() {
  const [page, setPage] = useState(1)
  const [keyword, setKeyword] = useState('')
  const [status, setStatus] = useState<number | undefined>(undefined)
  const queryClient = useQueryClient()

  const queryKey = useMemo(() => ['admin-blogs', page, keyword, status], [keyword, page, status])

  const { data, isLoading, isFetching } = useQuery({
    queryKey,
    queryFn: () => getAdminBlogs({ page, size: PAGE_SIZE, keyword: keyword.trim() || undefined, status }),
  })

  const reviewMutation = useMutation({
    mutationFn: ({ id, action }: { id: number; action: 'APPROVE' | 'REJECT' | 'OFFLINE' }) => reviewBlog(id, action),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-blogs'] }),
  })

  const blogs = data?.data ?? []
  const total = data?.total ?? 0
  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE))

  return (
    <div className="section admin-panel">
      <div className="section-head">
        <h2>笔记审核</h2>
        <span className="muted">共 {total} 条</span>
      </div>

      <div className="toolbar admin-toolbar">
        <input placeholder="搜索标题/内容" value={keyword} onChange={(event) => setKeyword(event.target.value)} />
        <select
          value={status === undefined ? 'all' : String(status)}
          onChange={(event) => {
            const value = event.target.value
            setStatus(value === 'all' ? undefined : Number(value))
            setPage(1)
          }}
        >
          <option value="all">全部状态</option>
          <option value="0">正常</option>
          <option value="1">下架</option>
        </select>
        <button className="primary-btn" onClick={() => setPage(1)}>
          查询
        </button>
      </div>

      {isLoading ? <p className="muted">加载中...</p> : null}
      {isFetching && !isLoading ? <p className="muted">刷新中...</p> : null}

      <div className="table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>标题</th>
              <th>作者</th>
              <th>点赞/评论</th>
              <th>状态</th>
              <th>时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {blogs.map((blog) => (
              <tr key={blog.id}>
                <td>{blog.id}</td>
                <td>
                  <div className="admin-title-cell">
                    {pickFirstImage(blog.images) ? (
                      <FallbackImage src={pickFirstImage(blog.images)} alt={blog.title} className="admin-thumb" />
                    ) : null}
                    <div>
                      <strong>{blog.title}</strong>
                      <p className="muted small">{brief(blog.content, 36)}</p>
                    </div>
                  </div>
                </td>
                <td>{blog.name || '匿名'}</td>
                <td>
                  {blog.liked ?? 0} / {blog.comments ?? 0}
                </td>
                <td>{blog.status === 1 ? '下架' : '正常'}</td>
                <td>{formatDateTime(blog.createTime)}</td>
                <td>
                  <div className="op-row">
                    <button
                      className="ghost-btn"
                      disabled={reviewMutation.isPending}
                      onClick={() => reviewMutation.mutate({ id: blog.id, action: 'APPROVE' })}
                    >
                      通过
                    </button>
                    <button
                      className="ghost-btn"
                      disabled={reviewMutation.isPending}
                      onClick={() => reviewMutation.mutate({ id: blog.id, action: 'REJECT' })}
                    >
                      驳回
                    </button>
                    <button
                      className="ghost-btn"
                      disabled={reviewMutation.isPending}
                      onClick={() => reviewMutation.mutate({ id: blog.id, action: 'OFFLINE' })}
                    >
                      下架
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="pager">
        <button className="ghost-btn" disabled={page <= 1} onClick={() => setPage((value) => value - 1)}>
          上一页
        </button>
        <span>
          第 {page} / {totalPages} 页
        </span>
        <button className="ghost-btn" disabled={page >= totalPages} onClick={() => setPage((value) => value + 1)}>
          下一页
        </button>
      </div>
    </div>
  )
}
