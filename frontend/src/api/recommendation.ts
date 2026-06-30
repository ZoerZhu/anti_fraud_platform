import { get, post } from '@/utils/request'

export interface RecommendationVO {
  itemId: number
  itemType: 'case' | 'news' | 'challenge' | string
  title: string
  coverImage?: string
  summary?: string
  score?: number
  reasons?: string[]
  tags?: string[]
  createTime?: string
}

export interface UserInterestVO {
  lifecycleStage: string
  lifecycleStageName: string
  knowledgeLevel: number
  weakPoints: string[]
  interestTags: { tagId: number; tagName: string; score: number }[]
}

export function getRecommendationList(params: { limit?: number; itemType?: string }): Promise<RecommendationVO[]> {
  return get<RecommendationVO[]>('/recommendation/list', { params })
}

export function getUserInterest(): Promise<UserInterestVO> {
  return get<UserInterestVO>('/recommendation/interest')
}

export function recordRecommendationClick(itemId: number, itemType: string): Promise<void> {
  return post<void>('/recommendation/click', null, {
    params: { itemId, itemType }
  })
}
