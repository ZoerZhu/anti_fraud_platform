import { del, get, post, put } from '@/utils/request'

export interface CaseVO {
  id: number
  title: string
  caseType: string
  content: string
  scripts?: string
  targetGrades?: string[]
  targetMajors?: string[]
  difficultyLevel: number
  difficultyName: string
  riskScore: number
  riskLevel: string
  viewCount: number
  likeCount: number
  likeRate: number
  wilsonScore: number
  tags: TagVO[]
  isFeatured: number
  status: number
  publishTime: string
  createTime: string
  isLiked?: boolean
}

export interface TagVO {
  id: number
  name: string
  category: string
  description?: string
  color: string
  caseCount?: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface CaseBrowseVO {
  caseId: number
  caseTitle: string
  caseType: string
  difficultyLevel: number
  browseTime: string
  stayDuration: number
  stayDurationDesc: string
  tagNames: string[]
}

export interface CreateCaseRequest {
  title: string
  caseType: string
  content: string
  scripts?: string
  targetGrades?: string[]
  targetMajors?: string[]
  difficultyLevel?: number
  riskScore?: number
  tagIds?: number[]
}

export interface UpdateCaseRequest extends Partial<CreateCaseRequest> {
  status?: number
  isFeatured?: number
}

/**
 * 获取案例分页列表
 */
export function getCasePage(params: {
  pageNum: number
  pageSize: number
  tagId?: number
  keyword?: string
}): Promise<PageResult<CaseVO>> {
  return get<PageResult<CaseVO>>('/case/page', { params })
}

/**
 * 管理端获取案例分页列表
 */
export function getAdminCasePage(params: {
  pageNum: number
  pageSize: number
  tagId?: number
  keyword?: string
  status?: number
}): Promise<PageResult<CaseVO>> {
  return get<PageResult<CaseVO>>('/case/admin/page', { params })
}

/**
 * 获取案例详情
 */
export function getCaseDetail(id: number): Promise<CaseVO> {
  return get<CaseVO>(`/case/${id}`)
}

/**
 * 创建案例(管理员)
 */
export function createCase(data: CreateCaseRequest): Promise<CaseVO> {
  return post<CaseVO>('/case', data)
}

/**
 * 更新案例(管理员)
 */
export function updateCase(id: number, data: UpdateCaseRequest): Promise<CaseVO> {
  return put<CaseVO>(`/case/${id}`, data)
}

/**
 * 删除案例(管理员)
 */
export function deleteCase(id: number): Promise<void> {
  return del<void>(`/case/${id}`)
}

/**
 * 发布案例(管理员)
 */
export function publishCase(id: number): Promise<void> {
  return post<void>(`/case/${id}/publish`)
}

/**
 * 设置精选(管理员)
 */
export function setCaseFeatured(id: number, isFeatured: number): Promise<void> {
  return put<void>(`/case/${id}/featured`, null, { params: { isFeatured } })
}

/**
 * 点赞案例
 */
export function likeCase(id: number): Promise<void> {
  return post<void>(`/case/${id}/like`)
}

/**
 * 取消点赞
 */
export function unlikeCase(id: number): Promise<void> {
  return del<void>(`/case/${id}/like`)
}

/**
 * 记录浏览
 */
export function browseCase(id: number, stayDuration: number = 0): Promise<void> {
  return post<void>(`/case/${id}/browse`, null, { params: { stayDuration } })
}

/**
 * 获取浏览记录
 */
export function getBrowseHistory(params: {
  pageNum: number
  pageSize: number
}): Promise<PageResult<CaseBrowseVO>> {
  return get<PageResult<CaseBrowseVO>>('/case/browse/history', { params })
}

/**
 * 获取热度排行榜
 */
export function getHotCases(limit: number = 10): Promise<CaseVO[]> {
  return get<CaseVO[]>('/case/hot', { params: { limit } })
}

/**
 * 获取威尔逊置信度得分
 */
export function getWilsonScore(positive: number, total: number): Promise<number> {
  return get<number>('/case/wilson', { params: { positive, total } })
}

/**
 * 获取所有标签
 */
export function getAllTags(): Promise<TagVO[]> {
  return get<TagVO[]>('/case/tag/list')
}

/**
 * 根据分类获取标签
 */
export function getTagsByCategory(category: string): Promise<TagVO[]> {
  return get<TagVO[]>(`/case/tag/category/${category}`)
}

/**
 * 创建标签(管理员)
 */
export function createTag(data: {
  name: string
  category: string
  description?: string
  color?: string
}): Promise<TagVO> {
  return post<TagVO>('/case/tag', data)
}

/**
 * 更新标签(管理员)
 */
export function updateTag(id: number, data: {
  name?: string
  category?: string
  description?: string
  color?: string
}): Promise<TagVO> {
  return put<TagVO>(`/case/tag/${id}`, data)
}

/**
 * 删除标签(管理员)
 */
export function deleteTag(id: number): Promise<void> {
  return del<void>(`/case/tag/${id}`)
}
