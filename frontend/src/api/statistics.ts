import { get, post } from '@/utils/request'
import axios from 'axios'
import type { AxiosRequestConfig } from 'axios'

/**
 * 直接下载文件（绕过响应拦截器）
 * 用于Excel/PDF等二进制文件下载
 */
export function downloadFile(url: string, filename: string, config?: AxiosRequestConfig): Promise<void> {
  const token = localStorage.getItem('token')
  const apiUrl =
    url.startsWith('http://') || url.startsWith('https://')
      ? url
      : url.startsWith('/api')
        ? url
        : `/api${url.startsWith('/') ? '' : '/'}${url}`

  return axios({
    url: apiUrl,
    method: config?.method || 'get',
    params: config?.params,
    data: config?.data,
    headers: {
      Authorization: token ? `Bearer ${token}` : ''
    },
    responseType: 'blob'
  }).then(async (response) => {
    if (response.status !== 200) {
      throw new Error(`导出失败（HTTP ${response.status}）`)
    }
    const ct = (response.headers['content-type'] || '').toLowerCase()
    if (ct.includes('application/json')) {
      const text = await (response.data as Blob).text()
      let msg = '导出失败'
      try {
        const j = JSON.parse(text) as { message?: string; msg?: string }
        msg = j.message || j.msg || msg
      } catch {
        /* 非 JSON 时使用默认文案 */
      }
      throw new Error(msg)
    }
    const blob = new Blob([response.data], {
      type: response.headers['content-type'] || 'application/octet-stream'
    })
    const downloadUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(downloadUrl)
  })
}

/**
 * 访问量趋势数据
 */
export interface VisitTrendVO {
  dates: string[]
  pageViews: number[]
  activeUsers: number[]
  newUsers: number[]
}

/**
 * 诈骗类型分布
 */
export interface FraudTypeDistVO {
  types: string[]
  counts: number[]
}

/**
 * 院系得分数据
 */
export interface DepartmentScoreVO {
  departments: string[]
  avgScores: number[]
  userCounts: number[]
  completionRates: number[]
}

/**
 * TOP案例
 */
export interface TopCaseVO {
  id: number
  title: string
  caseType: string
  viewCount: number
  likeCount: number
  hotScore: number
}

/**
 * 用户活跃度数据
 */
export interface HourlyActivityVO {
  hour: number
  count: number
}

/**
 * 看板数据
 */
export interface DashboardVO {
  todayViews: number
  todayNewUsers: number
  todayActiveUsers: number
  totalCases: number
  totalUsers: number
  totalChallengeCompletions: number
  avgTestScore: number
  visitTrend: VisitTrendVO
  fraudTypeDist: FraudTypeDistVO
  departmentScores: DepartmentScoreVO
  topCases: TopCaseVO[]
  hourlyActivity: HourlyActivityVO[]
}

/**
 * 获取看板数据
 */
export function getDashboardData(): Promise<DashboardVO> {
  return get<DashboardVO>('/statistics/dashboard')
}

/**
 * 获取访问量趋势
 */
export function getVisitTrend(days: number = 7): Promise<VisitTrendVO> {
  return get<VisitTrendVO>('/statistics/visit/trend', { params: { days } })
}

/**
 * 获取诈骗类型分布
 */
export function getFraudTypeDistribution(): Promise<FraudTypeDistVO> {
  return get<FraudTypeDistVO>('/statistics/fraud/types')
}

/**
 * 获取高频诈骗类型TOP N
 */
export function getTopFraudTypes(limit: number = 5): Promise<any[]> {
  return get<any[]>('/statistics/fraud/top', { params: { limit } })
}

/**
 * 获取各院系测试得分统计
 */
export function getDepartmentScores(): Promise<DepartmentScoreVO> {
  return get<DepartmentScoreVO>('/statistics/department/scores')
}

/**
 * 获取学生学习完成率统计
 */
export function getCompletionRate(): Promise<any[]> {
  return get<any[]>('/statistics/completion/rate')
}

/**
 * 获取TOP案例排行榜
 */
export function getTopCases(limit: number = 10): Promise<TopCaseVO[]> {
  return get<TopCaseVO[]>('/statistics/cases/top', { params: { limit } })
}

/**
 * 获取用户活跃度热力图数据
 */
export function getHourlyActivity(statDate?: string): Promise<HourlyActivityVO[]> {
  return get<HourlyActivityVO[]>('/statistics/activity/hourly', { params: { statDate } })
}

/**
 * 手动刷新统计数据
 */
export function refreshStatistics(): Promise<void> {
  return post<void>('/statistics/refresh')
}

/**
 * 导出每日统计数据
 */
export function exportDailyStatistics(params?: { startDate?: string; endDate?: string }): Promise<void> {
  const filename = `每日统计数据_${new Date().toISOString().split('T')[0]}.xlsx`
  return downloadFile('/statistics/export/daily', filename, { params })
}

/**
 * 导出院系统计数据
 */
export function exportDepartmentStatistics(params?: { statDate?: string; startDate?: string; endDate?: string }): Promise<void> {
  const filename = `院系统计数据_${new Date().toISOString().split('T')[0]}.xlsx`
  return downloadFile('/statistics/export/department', filename, { params })
}
