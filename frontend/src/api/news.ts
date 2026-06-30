import { del, get, post } from '@/utils/request'

export interface News {
  id: number
  title: string
  content: string
  summary: string
  coverImage: string
  categoryId: number
  categoryName: string
  authorId: number
  authorName: string
  newsType: 'news' | 'warning' | 'policy'
  isTop: number
  isMandatory: number
  viewCount: number
  likeCount: number
  isLiked: boolean
  status: number
  publishTime: string
  createTime: string
}

export interface NewsCategory {
  id: number
  name: string
  parentId: number
  sortOrder: number
  createTime: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
}

export const getNewsPage = (params: {
  pageNum: number
  pageSize: number
  categoryId?: number
  newsType?: string
  keyword?: string
}) => {
  return get<PageResult<News>>('/news/page', { params })
}

export const getRequiredNews = (limit: number = 3) => {
  return get<News[]>('/news/required', { params: { limit } })
}

export const getNewsDetail = (id: number) => {
  return get<News>(`/news/${id}`)
}

export const likeNews = (id: number) => {
  return post<boolean>(`/news/${id}/like`)
}

export const unlikeNews = (id: number) => {
  return del<boolean>(`/news/${id}/like`)
}

export const viewNews = (id: number, stayDuration?: number) => {
  return post<void>(`/news/${id}/view`, { stayDuration })
}

export const getBrowseHistory = (params: { pageNum: number; pageSize: number }) => {
  return get<PageResult<News>>('/news/browse/history', { params })
}

export const getCategories = () => {
  return get<NewsCategory[]>('/news/category/list')
}
