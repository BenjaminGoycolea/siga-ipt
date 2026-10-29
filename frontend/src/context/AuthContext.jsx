import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import keycloak, { initializeKeycloak } from '../api/keycloak'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [isLoading, setIsLoading] = useState(true)
  const [roles, setRoles] = useState([])

  useEffect(() => {
    let active = true

    initializeKeycloak()
      .then((authenticated) => {
        if (!active) return
        setIsAuthenticated(authenticated)
        setRoles(keycloak.tokenParsed?.realm_access?.roles ?? [])
      })
      .catch((error) => {
        console.error('Unable to initialize Keycloak:', error)
        if (active) {
          setIsAuthenticated(false)
          setRoles([])
        }
      })
      .finally(() => {
        if (active) setIsLoading(false)
      })

    return () => {
      active = false
    }
  }, [])

  const login = useCallback(() => keycloak.login(), [])
  const logout = useCallback(() => keycloak.logout({ redirectUri: window.location.origin }), [])

  const value = useMemo(() => ({
    isAuthenticated,
    isLoading,
    roles,
    login,
    logout,
  }), [isAuthenticated, isLoading, roles, login, logout])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}
