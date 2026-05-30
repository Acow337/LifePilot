import { apiGet } from '../http'
import type { Blog } from '../types'

export const getHotBlogs = (current = 1) =>
  apiGet<Blog[]>('/blog/hot', {
    params: { current },
  })
