import { post } from '@/utils/request'

/**
 * 上传图片
 * @param file 图片文件
 */
export function uploadImage(file: File): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)
  return post<string>('/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
