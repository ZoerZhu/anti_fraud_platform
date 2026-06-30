import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo } from '@/types/global'
import { get, post, put } from '@/utils/request'

interface LoginPayload {
  token: string
  userId: number
  username: string
  nickname?: string
  avatar?: string
  role: UserInfo['role']
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const userInfo = ref<UserInfo | null>(null)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 'admin')

  async function login(username: string, password: string) {
    const res = await post<LoginPayload>('/user/login', {
      username,
      password
    })
    token.value = res.token
    localStorage.setItem('token', res.token)
    userInfo.value = buildUserInfoFromLogin(res)
    return res
  }

  async function register(data: {
    username: string
    password: string
    nickname?: string
    phone?: string
    email?: string
    studentNo?: string
    grade?: string
    major?: string
  }) {
    const res = await post<UserInfo>('/user/register', data)
    return res
  }

  async function getUserInfo() {
    if (!token.value) {
      token.value = localStorage.getItem('token')
    }
    if (!token.value) return null
    try {
      const res = await get<UserInfo>('/user/info')
      userInfo.value = res
      return res
    } catch (error) {
      clearUser()
      return null
    }
  }

  async function updateUser(data: {
    nickname?: string
    phone?: string
    email?: string
    avatar?: string
    grade?: string
    major?: string
  }) {
    const res = await put<void>('/user/update', data)
    await getUserInfo()
    return res
  }

  async function changePassword(oldPassword: string, newPassword: string) {
    const res = await put<void>('/user/password', {
      oldPassword,
      newPassword
    })
    return res
  }

  async function logout() {
    try {
      await post('/user/logout')
    } finally {
      clearUser()
    }
  }

  function clearUser() {
    token.value = null
    userInfo.value = null
    localStorage.removeItem('token')
  }

  function setUserInfo(info: UserInfo) {
    userInfo.value = info
  }

  function buildUserInfoFromLogin(payload: LoginPayload): UserInfo {
    return {
      id: payload.userId,
      username: payload.username,
      nickname: payload.nickname,
      avatar: payload.avatar,
      role: payload.role,
      status: 1,
      createTime: ''
    }
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    isAdmin,
    login,
    register,
    getUserInfo,
    updateUser,
    changePassword,
    logout,
    clearUser,
    setUserInfo
  }
})
