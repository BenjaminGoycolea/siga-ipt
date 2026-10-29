import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  cancelStudentEnrollment,
  getAcademicHistory,
  getAcademicPeriods,
  getErrorMessage,
  getStudentEnrollments,
  getStudentOfferings,
  saveStudentEnrollments,
} from '../api/academic'
import { Card, EmptyState, Field, inputClassName, LoadingState, Notice, PageHeading, formatGrade } from '../components/PortalUi'

function EnrollmentPage() {
  const [periods, setPeriods] = useState([])
  const [periodId, setPeriodId] = useState('')
  const [offerings, setOfferings] = useState([])
  const [enrollments, setEnrollments] = useState([])
  const [selectedIds, setSelectedIds] = useState([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  useEffect(() => {
    getAcademicPeriods()
      .then((data) => {
        setPeriods(data)
        const defaultPeriod = data.find((period) => period.status === 'OPEN')
        setPeriodId(defaultPeriod ? String(defaultPeriod.id) : data[0] ? String(data[0].id) : '')
      })
      .catch((requestError) => setError(getErrorMessage(requestError)))
      .finally(() => setLoading(false))
  }, [])

  const loadPeriod = useCallback(async () => {
    if (!periodId) return
    setLoading(true)
    setError('')
    setMessage('')
    try {
      const selectedPeriod = periods.find((period) => String(period.id) === periodId)
      const [available, current] = await Promise.all([
        selectedPeriod?.status === 'OPEN' ? getStudentOfferings(periodId) : Promise.resolve([]),
        getStudentEnrollments(periodId),
      ])
      setOfferings(available)
      setEnrollments(current)
      setSelectedIds(current.filter((item) => item.status === 'ENROLLED').map((item) => item.sectionId))
    } catch (requestError) {
      setOfferings([])
      setEnrollments([])
      setSelectedIds([])
      setError(getErrorMessage(requestError))
    } finally {
      setLoading(false)
    }
  }, [periodId, periods])

  useEffect(() => {
    void loadPeriod()
  }, [loadPeriod])

  const selectedCourseCount = useMemo(() => selectedIds.length, [selectedIds])

  function toggleSection(sectionId) {
    setSelectedIds((current) => current.includes(sectionId)
      ? current.filter((id) => id !== sectionId)
      : [...current, sectionId])
  }

  async function saveSelection(event) {
    event.preventDefault()
    setSaving(true)
    setError('')
    setMessage('')
    try {
      const updated = await saveStudentEnrollments(periodId, selectedIds)
      setEnrollments(updated)
      setSelectedIds(updated.filter((item) => item.status === 'ENROLLED').map((item) => item.sectionId))
      setMessage('Tu selección de asignaturas quedó guardada.')
    } catch (requestError) {
      setError(getErrorMessage(requestError))
    } finally {
      setSaving(false)
    }
  }

  async function cancelEnrollment(enrollmentId) {
    setError('')
    setMessage('')
    try {
      await cancelStudentEnrollment(periodId, enrollmentId)
      await loadPeriod()
      setMessage('La inscripción fue anulada.')
    } catch (requestError) {
      setError(getErrorMessage(requestError))
    }
  }

  const selectedPeriod = periods.find((period) => String(period.id) === periodId)
  const isOpen = selectedPeriod?.status === 'OPEN'

  if (loading && periods.length === 0) return <LoadingState />

  return (
    <div className="space-y-6">
      <Card>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <Field label="Período académico" className="w-full sm:max-w-xs">
            <select className={inputClassName} onChange={(event) => setPeriodId(event.target.value)} value={periodId}>
              {periods.map((period) => (
                <option key={period.id} value={period.id}>{period.code} · {period.status === 'OPEN' ? 'Abierto' : 'Cerrado'}</option>
              ))}
            </select>
          </Field>
          <span className={`rounded-full px-3 py-1 text-xs font-semibold ${isOpen ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-100 text-slate-600'}`}>
            {isOpen ? 'Inscripción habilitada' : 'Período cerrado'}
          </span>
        </div>
      </Card>

      {error && <Notice tone="error">{error}</Notice>}
      {message && <Notice tone="success">{message}</Notice>}

      {loading
        ? <LoadingState />
        : (
          <>
            <Card>
              <form onSubmit={saveSelection}>
                <div className="mb-5 flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <h2 className="text-lg font-bold text-navy">Oferta académica</h2>
                    <p className="mt-1 text-sm text-slate-500">Selecciona entre 3 y 6 asignaturas. Las reglas académicas se validan al guardar.</p>
                  </div>
                  <span className="rounded-full bg-slate-100 px-3 py-1 text-sm font-semibold text-slate-700">
                    {selectedCourseCount} seleccionadas
                  </span>
                </div>
                {offerings.length === 0
                  ? <EmptyState>No hay secciones disponibles para este período o no cumples los requisitos para inscribirlas.</EmptyState>
                  : (
                    <div className="grid gap-3 md:grid-cols-2">
                      {offerings.map((section) => (
                        <label
                          className={`flex cursor-pointer gap-3 rounded-xl border p-4 transition ${
                            selectedIds.includes(section.id) ? 'border-teal-600 bg-teal-50' : 'border-slate-200 hover:border-slate-300'
                          }`}
                          key={section.id}
                        >
                          <input
                            checked={selectedIds.includes(section.id)}
                            className="mt-1 h-4 w-4 accent-teal-700"
                            disabled={!isOpen}
                            onChange={() => toggleSection(section.id)}
                            type="checkbox"
                          />
                          <span>
                            <span className="block font-semibold text-navy">{section.courseCode} · {section.courseName}</span>
                            <span className="mt-1 block text-sm text-slate-600">{section.teacherName}</span>
                            <span className="mt-1 block text-xs text-slate-500">
                              Sección {section.id} · {section.scheduleBlocks.join(', ')} · cupo {section.capacity}
                            </span>
                          </span>
                        </label>
                      ))}
                    </div>
                  )}
                <div className="mt-5 flex justify-end">
                  <button className="button-primary" disabled={!isOpen || saving} type="submit">
                    {saving ? 'Guardando…' : 'Guardar selección'}
                  </button>
                </div>
              </form>
            </Card>

            <Card>
              <h2 className="mb-4 text-lg font-bold text-navy">Mis inscripciones</h2>
              {enrollments.length === 0
                ? <EmptyState>No hay inscripciones registradas en este período.</EmptyState>
                : (
                  <div className="overflow-x-auto">
                    <table className="w-full min-w-[600px] text-left text-sm">
                      <thead className="border-b border-slate-200 text-xs uppercase text-slate-500">
                        <tr><th className="py-3 pr-4">Asignatura</th><th className="py-3 pr-4">Sección</th><th className="py-3 pr-4">Estado</th><th className="py-3 pr-4">Nota</th><th className="py-3">Acción</th></tr>
                      </thead>
                      <tbody className="divide-y divide-slate-100">
                        {enrollments.map((item) => (
                          <tr key={item.id}>
                            <td className="py-3 pr-4 font-medium">{item.courseCode} · {item.courseName}</td>
                            <td className="py-3 pr-4">{item.sectionId}</td>
                            <td className="py-3 pr-4">{item.status === 'ENROLLED' ? 'Inscrita' : 'Anulada'}</td>
                            <td className="py-3 pr-4">{formatGrade(item.finalGrade)}</td>
                            <td className="py-3">
                              {isOpen && item.status === 'ENROLLED' && item.finalGrade == null
                                ? <button className="text-sm font-semibold text-rose-700 hover:underline" onClick={() => cancelEnrollment(item.id)} type="button">Anular</button>
                                : '—'}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
            </Card>
          </>
        )}
    </div>
  )
}

function HistoryPage() {
  const [history, setHistory] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    getAcademicHistory()
      .then(setHistory)
      .catch((requestError) => setError(getErrorMessage(requestError)))
      .finally(() => setLoading(false))
  }, [])

  return (
    <Card>
      <h2 className="mb-4 text-lg font-bold text-navy">Historial académico</h2>
      {error && <Notice tone="error">{error}</Notice>}
      {loading ? <LoadingState /> : history.length === 0
        ? <EmptyState>Aún no hay resultados de períodos cerrados.</EmptyState>
        : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[560px] text-left text-sm">
              <thead className="border-b border-slate-200 text-xs uppercase text-slate-500">
                <tr><th className="py-3 pr-4">Período</th><th className="py-3 pr-4">Asignatura</th><th className="py-3 pr-4">Nota final</th><th className="py-3">Resultado</th></tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {history.map((item) => (
                  <tr key={item.id}>
                    <td className="py-3 pr-4">{item.academicPeriodCode}</td>
                    <td className="py-3 pr-4">{item.courseCode} · {item.courseName}</td>
                    <td className="py-3 pr-4 font-semibold">{formatGrade(item.finalGrade)}</td>
                    <td className="py-3">{item.result === 'PASSED' ? 'Aprobada' : 'Reprobada'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
    </Card>
  )
}

export default function StudentWorkspace() {
  const [tab, setTab] = useState('enrollment')
  const tabs = [
    { id: 'enrollment', label: 'Inscripción' },
    { id: 'history', label: 'Historial académico' },
  ]

  return (
    <div className="mx-auto max-w-6xl px-6 py-10">
      <PageHeading eyebrow="Portal estudiante" title="Mi actividad académica" description="Consulta tu oferta, administra tus inscripciones y revisa los resultados de períodos cerrados." />
      <div className="mb-6 flex gap-2 border-b border-slate-200">
        {tabs.map((item) => (
          <button
            aria-current={tab === item.id ? 'page' : undefined}
            className={`border-b-2 px-4 py-3 text-sm font-semibold ${tab === item.id ? 'border-teal-700 text-teal-800' : 'border-transparent text-slate-500 hover:text-slate-800'}`}
            key={item.id}
            onClick={() => setTab(item.id)}
            type="button"
          >
            {item.label}
          </button>
        ))}
      </div>
      {tab === 'enrollment' ? <EnrollmentPage /> : <HistoryPage />}
    </div>
  )
}
