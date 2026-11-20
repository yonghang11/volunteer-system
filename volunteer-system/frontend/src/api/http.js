import axios from 'axios'
import { ElMessage } from 'element-plus'

export const TOKEN_KEY = 'vs_token'
export const USER_KEY = 'vs_user'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000
})

function redirectLogin() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  import('@/router').then(({ default: router }) => {
    if (router.currentRoute.value.path !== '/login') {
      router.replace('/login')
    }
  })
}

service.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

service.interceptors.response.use(
  (response) => {
    if (response.config.responseType === 'blob') {
      return response
    }
    const body = response.data
    if (!body || typeof body !== 'object' || body.code === undefined) {
      return body
    }
    if (body.code === 0) {
      return body.data
    }
    if (body.code === 401) {
      ElMessage.error(body.msg || '登录已失效，请重新登录')
      redirectLogin()
    } else {
      ElMessage.error(body.msg || '请求失败')
    }
    return Promise.reject(new Error(body.msg || '请求失败'))
  },
  (error) => {
    const msg = error.response
      ? `请求失败（HTTP ${error.response.status}）`
      : '网络异常，请确认后端服务已启动'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export const get = (url, params) => service.get(url, { params })
export const post = (url, data) => service.post(url, data)
export const put = (url, data) => service.put(url, data)
export const del = (url, params) => service.delete(url, { params })
export const download = (url, params) =>
  service.get(url, { params, responseType: 'blob' })

/** multipart/form-data 上传文件 */
export const upload = (url, file, onProgress) => {
  const fd = new FormData()
  fd.append('file', file)
  return service.post(url, fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (e) => {
      if (onProgress && e.total) {
        onProgress(Math.round((e.loaded * 100) / e.total))
      }
    }
  })
}

export default service
