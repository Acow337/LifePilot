import { apiDelete, apiGet, apiGetWithMeta, apiPost } from '../http'
import type { Blog, BlogComment, BlogCommentCreatePayload } from '../types'

export const getHotBlogs = (current = 1) =>
  apiGet<Blog[]>('/blog/hot', {
    params: { current },
  })

export const getBlogComments = async (blogId: number, current = 1) => {
  const result = await apiGetWithMeta<BlogComment[]>(`/blog-comments/of/blog/${blogId}`, {
    params: { current },
  })
  return result.data
}

export const createBlogComment = (payload: BlogCommentCreatePayload) =>
  apiPost<number>('/blog-comments', payload)

export const deleteBlogComment = (commentId: number) => apiDelete<null>(`/blog-comments/${commentId}`)
