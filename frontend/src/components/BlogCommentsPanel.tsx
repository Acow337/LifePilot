import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { type FormEvent, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { createBlogComment, deleteBlogComment, getBlogComments } from '../services/modules/blog'
import type { BlogComment, BlogCommentCreatePayload } from '../services/types'
import { useAuthStore } from '../store/auth'
import { formatDateTime } from '../utils/format'

export function BlogCommentsPanel({ blogId }: { blogId: number }) {
  const queryClient = useQueryClient()
  const { token, user } = useAuthStore()
  const [draft, setDraft] = useState('')
  const [error, setError] = useState('')
  const [replyTarget, setReplyTarget] = useState<{
    parentId: number
    answerId: number
    nickName?: string
  } | null>(null)

  const { data: comments = [], isLoading } = useQuery({
    queryKey: ['blog-comments', blogId],
    queryFn: () => getBlogComments(blogId, 1),
  })

  const createMutation = useMutation({
    mutationFn: (payload: BlogCommentCreatePayload) => createBlogComment(payload),
    onSuccess: async () => {
      setDraft('')
      setReplyTarget(null)
      setError('')
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['blog-comments', blogId] }),
        queryClient.invalidateQueries({ queryKey: ['hot-blogs'] }),
        queryClient.invalidateQueries({ queryKey: ['blog-detail', blogId] }),
      ])
    },
    onError: (e: Error) => {
      setError(e.message || '评论发布失败')
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (commentId: number) => deleteBlogComment(commentId),
    onSuccess: async () => {
      setError('')
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['blog-comments', blogId] }),
        queryClient.invalidateQueries({ queryKey: ['hot-blogs'] }),
        queryClient.invalidateQueries({ queryKey: ['blog-detail', blogId] }),
      ])
    },
    onError: (e: Error) => {
      setError(e.message || '删除评论失败')
    },
  })

  const replyingText = useMemo(() => {
    if (!replyTarget) {
      return ''
    }
    return `回复 @${replyTarget.nickName || '用户'}`
  }, [replyTarget])

  const submitComment = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!token) {
      setError('请先登录后再评论')
      return
    }
    if (!draft.trim()) {
      setError('评论内容不能为空')
      return
    }

    createMutation.mutate({
      blogId,
      parentId: replyTarget?.parentId,
      answerId: replyTarget?.answerId,
      content: draft.trim(),
    })
  }

  const renderReplyItem = (reply: BlogComment, topComment: BlogComment) => {
    const canDelete = user?.id === reply.userId
    return (
      <div key={reply.id} className="comment-reply-item">
        <div className="comment-main">
          <strong>{reply.nickName || `用户${reply.userId}`}</strong>
          <span className="muted small">{formatDateTime(reply.createTime)}</span>
        </div>
        <p className="comment-content">{reply.content}</p>
        <div className="comment-actions">
          <button
            type="button"
            className="ghost-btn"
            onClick={() =>
              setReplyTarget({
                parentId: topComment.id,
                answerId: reply.userId,
                nickName: reply.nickName,
              })
            }
          >
            回复
          </button>
          {canDelete ? (
            <button
              type="button"
              className="ghost-btn"
              onClick={() => deleteMutation.mutate(reply.id)}
              disabled={deleteMutation.isPending}
            >
              删除
            </button>
          ) : null}
        </div>
      </div>
    )
  }

  return (
    <div className="comment-panel">
      <div className="section-head comment-head">
        <h4>评论 {comments.length}</h4>
      </div>

      {isLoading ? <p className="muted small">评论加载中...</p> : null}

      <form className="comment-form" onSubmit={submitComment}>
        <textarea
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
          placeholder={replyingText || '写下你的真实体验...'}
          rows={2}
        />
        <div className="comment-form-actions">
          {replyTarget ? (
            <button type="button" className="ghost-btn" onClick={() => setReplyTarget(null)}>
              取消回复
            </button>
          ) : null}
          <button type="submit" className="primary-btn" disabled={createMutation.isPending}>
            {createMutation.isPending ? '发布中...' : '发布评论'}
          </button>
        </div>
      </form>

      {!token ? (
        <p className="muted small">
          需要登录后评论，<Link to="/login">去登录</Link>
        </p>
      ) : null}

      {error ? <p className="error-text">{error}</p> : null}

      {comments.length === 0 ? <p className="muted small">暂无评论，抢个沙发吧～</p> : null}

      <div className="comment-list">
        {comments.map((comment) => {
          const canDelete = user?.id === comment.userId
          return (
            <article key={comment.id} className="comment-item">
              <div className="comment-main">
                <strong>{comment.nickName || `用户${comment.userId}`}</strong>
                <span className="muted small">{formatDateTime(comment.createTime)}</span>
              </div>
              <p className="comment-content">{comment.content}</p>
              <div className="comment-actions">
                <button
                  type="button"
                  className="ghost-btn"
                  onClick={() =>
                    setReplyTarget({
                      parentId: comment.id,
                      answerId: comment.userId,
                      nickName: comment.nickName,
                    })
                  }
                >
                  回复
                </button>
                {canDelete ? (
                  <button
                    type="button"
                    className="ghost-btn"
                    onClick={() => deleteMutation.mutate(comment.id)}
                    disabled={deleteMutation.isPending}
                  >
                    删除
                  </button>
                ) : null}
              </div>
              {comment.replies?.length ? (
                <div className="comment-replies">{comment.replies.map((reply) => renderReplyItem(reply, comment))}</div>
              ) : null}
            </article>
          )
        })}
      </div>
    </div>
  )
}
