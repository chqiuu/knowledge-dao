import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api/admin',
  timeout: 30000
})

request.interceptors.request.use(
  (config) => {
    return config
  },
  (error) => Promise.reject(error)
)

// AdminApiServer 返回 {success: true, data: ...}
// ApiServer 返回 {code: 200, data: ...}
request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 处理 AdminApiServer 格式 {success, data, message}
    if (res && res.success === true) {
      return res.data
    }
    // 处理 ApiServer 格式 {code, data, message}
    if (res && (res.code === 200 || res.code === 0)) {
      return res.data
    }
    ElMessage.error(res?.message || '请求失败')
    return Promise.reject(new Error(res?.message || '请求失败'))
  },
  (error) => {
    const msg = error.response?.data?.message || error.message || '网络错误'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default request
