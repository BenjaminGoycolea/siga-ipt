import { useEffect, useState } from 'react'
import { Link, Navigate, Route, Routes } from 'react-router-dom'
import api from './api/client'
import { useAuth } from './context/AuthContext'
import AdminWorkspace from './pages/AdminWorkspace'
import HomePage from './pages/HomePage'
import StudentWorkspace from './pages/StudentWorkspace'
import TeacherWorkspace from './pages/TeacherWorkspace'

function RoleRoute({ authenticated, loading, login, roles, requiredRole, children }) {
  if (loading) {
    return <p className="mx-auto max-w-6xl px-6 py-16 text-sm text-slate-500">Verificando sesión…</p>
  }
  if (!authenticated) {
    return (
      <div className="mx-auto max-w-3xl px-6 py-16">
        <div className="rounded-2xl border border-slate-200 bg-white p-8 text-center shadow-sm">
          <h1 className="text-2xl font-bold text-navy">Inicia sesión para continuar</h1>
          <p className="mt-2 text-slate-600">La información académica está protegida y requiere una cuenta institucional.</p>
          <button className="button-primary mt-6" onClick={login} type="button">Iniciar sesión</button>
        </div>
      </div>
    )
  }
  if (!roles.includes(requiredRole)) {
    return (
      <div className="mx-auto max-w-3xl px-6 py-16">
        <div className="rounded-2xl border border-amber-200 bg-amber-50 p-8 text-center">
          <h1 className="text-2xl font-bold text-amber-950">Acceso no disponible</h1>
          <p className="mt-2 text-amber-900">Tu cuenta no tiene el rol {requiredRole}. Solicita la asignación del rol a un administrador.</p>
          <Link className="button-secondary mt-6 inline-block" to="/">Volver al inicio</Link>
        </div>
      </div>
    )
  }
  return children
}

function AppLayout() {
  const { isAuthenticated, isLoading, roles, login, logout } = useAuth()
  const [apiStatus, setApiStatus] = useState('checking')
  const portals = [
    { role: 'STUDENT', path: '/student', label: 'Estudiante' },
    { role: 'TEACHER', path: '/teacher', label: 'Docente' },
    { role: 'ADMIN', path: '/admin', label: 'Administración' },
  ].filter((portal) => roles.includes(portal.role))

  useEffect(() => {
    let active = true

    api.get('/health')
      .then(({ data }) => {
        if (active) setApiStatus(data.status === 'UP' ? 'online' : 'error')
      })
      .catch(() => {
        if (active) setApiStatus('offline')
      })

    return () => {
      active = false
    }
  }, [])

  return (
    <div className="min-h-screen bg-slate-50 text-slate-800">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-6 py-4">
          <Link className="flex items-center gap-3" to="/">
            <span className="grid h-11 w-11 place-items-center rounded-xl bg-navy font-bold text-white">
              IPT
            </span>
            <span>
              <span className="block font-bold text-navy">SIGA IPT</span>
              <span className="block text-xs text-slate-500">Gestión académica</span>
            </span>
          </Link>
          <div className="flex flex-wrap items-center justify-end gap-3">
            {isAuthenticated && portals.length > 0 && (
              <nav aria-label="Portales académicos" className="flex flex-wrap items-center gap-1">
                {portals.map((portal) => (
                  <Link className="rounded-lg px-3 py-2 text-sm font-semibold text-slate-600 hover:bg-slate-100 hover:text-navy" key={portal.path} to={portal.path}>
                    {portal.label}
                  </Link>
                ))}
              </nav>
            )}
            <span className="hidden text-sm text-slate-500 sm:inline">
              API: {apiStatus === 'online' ? 'conectada' : apiStatus === 'checking' ? 'verificando…' : 'sin conexión'}
            </span>
            {!isLoading && (
              isAuthenticated
                ? (
                  <button className="button-secondary" onClick={logout} type="button">
                    Cerrar sesión
                  </button>
                )
                : (
                  <button className="button-primary" onClick={login} type="button">
                    Iniciar sesión
                  </button>
                )
            )}
          </div>
        </div>
      </header>
      <main>
        <Routes>
          <Route path="/" element={<HomePage apiStatus={apiStatus} roles={roles} />} />
          <Route path="/student" element={<RoleRoute authenticated={isAuthenticated} loading={isLoading} login={login} requiredRole="STUDENT" roles={roles}><StudentWorkspace /></RoleRoute>} />
          <Route path="/teacher" element={<RoleRoute authenticated={isAuthenticated} loading={isLoading} login={login} requiredRole="TEACHER" roles={roles}><TeacherWorkspace /></RoleRoute>} />
          <Route path="/admin" element={<RoleRoute authenticated={isAuthenticated} loading={isLoading} login={login} requiredRole="ADMIN" roles={roles}><AdminWorkspace /></RoleRoute>} />
          <Route path="*" element={<Navigate replace to="/" />} />
        </Routes>
      </main>
      <footer className="border-t border-slate-200 bg-white">
        <div className="mx-auto max-w-6xl px-6 py-5 text-sm text-slate-500">
          Instituto Profesional de Tecnología · SIGA IPT
        </div>
      </footer>
    </div>
  )
}

export default function App() {
  return <AppLayout />
}
