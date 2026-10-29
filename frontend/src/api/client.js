import axios from 'axios'
import keycloak from './keycloak'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
})

api.interceptors.request.use(async (config) => {
  if (keycloak.authenticated) {
    await keycloak.updateToken(30)
    config.headers.Authorization = `Bearer ${keycloak.token}`
  }
  return config
})

export default api
