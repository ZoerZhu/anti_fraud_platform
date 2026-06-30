import { del, get, post } from '@/utils/request'

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  time?: string
  fallback?: boolean
  /** 助手回复满意度：1 满意 / -1 不满意 */
  feedback?: 1 | -1
}

export interface ChatVO {
  sessionId: string
  question: string
  answer: string
  tokensUsed: number
  fallback?: boolean
  createTime: string
}

export interface SessionVO {
  sessionId: string
  firstQuestion: string
  lastAnswer: string
  messageCount: number
  totalTokens: number
  createTime: string
  updateTime: string
}

export interface TokenStatsVO {
  totalTokens: number
  totalQuestions: number
  satisfiedCount: number
  dissatisfiedCount: number
}

export interface ChatRequest {
  question: string
  sessionId?: string
}

export interface FeedbackRequest {
  sessionId: string
  feedback: 1 | -1
}

/**
 * 发送问题并获取AI回答
 */
export function askQuestion(data: ChatRequest): Promise<ChatVO> {
  return post<ChatVO>('/chat/ask', data)
}

/**
 * 获取会话历史
 */
export function getConversationHistory(sessionId: string): Promise<ChatVO[]> {
  return get<ChatVO[]>(`/chat/history/${sessionId}`)
}

/**
 * 获取会话列表
 */
export function getSessionList(): Promise<SessionVO[]> {
  return get<SessionVO[]>('/chat/sessions')
}

/**
 * 提交反馈
 */
export function submitFeedback(data: FeedbackRequest): Promise<void> {
  return post<void>('/chat/feedback', data)
}

/**
 * 获取Token统计
 */
export function getTokenStats(): Promise<TokenStatsVO> {
  return get<TokenStatsVO>('/chat/stats')
}

/**
 * 删除会话
 */
export function deleteSession(sessionId: string): Promise<void> {
  return del<void>(`/chat/session/${sessionId}`)
}

/**
 * 创建新会话
 */
export function createNewSession(): Promise<string> {
  return post<string>('/chat/new-session')
}
