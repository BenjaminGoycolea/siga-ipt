import { useCallback, useEffect, useState } from 'react'
import { getAcademicPeriods, getErrorMessage, getSectionEnrollments, getTeacherSections, saveFinalGrade } from '../api/academic'
import { Card, EmptyState, Field, inputClassName, LoadingState, Notice, PageHeading, formatGrade } from '../components/PortalUi'

export default function TeacherWorkspace() {
  const [periods, setPeriods] = useState([])
  const [periodId, setPeriodId] = useState('')
  const [sections, setSections] = useState([])
  const [sectionId, setSectionId] = useState('')
  const [students, setStudents] = useState([])
  const [grades, setGrades] = useState({})
  const [loading, setLoading] = useState(true)
  const [savingId, setSavingId] = useState(null)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  useEffect(() => {
    getAcademicPeriods()
      .then((data) => {
        setPeriods(data)
        const openPeriod = data.find((period) => period.status === 'OPEN')
        setPeriodId(openPeriod ? String(openPeriod.id) : data[0] ? String(data[0].id) : '')
      })
      .catch((requestError) => setError(getErrorMessage(requestError)))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    if (!periodId) return
    setLoading(true)
    setError('')
    setSectionId('')
    setStudents([])
    getTeacherSections(periodId)
      .then((data) => setSections(data))
      .catch((requestError) => {
        setSections([])
        setError(getErrorMessage(requestError))
      })
      .finally(() => setLoading(false))
  }, [periodId])

  const loadStudents = useCallback(async () => {
    if (!sectionId) return
    setLoading(true)
    setError('')
    setMessage('')
    try {
      const data = await getSectionEnrollments(sectionId)
      setStudents(data)
      setGrades(Object.fromEntries(data.map((item) => [item.id, item.finalGrade ?? ''])))
    } catch (requestError) {
      setStudents([])
      setError(getErrorMessage(requestError))
    } finally {
      setLoading(false)
    }
  }, [sectionId])

  useEffect(() => {
    void loadStudents()
  }, [loadStudents])

  async function submitGrade(event, enrollmentId) {
    event.preventDefault()
    const rawGrade = grades[enrollmentId]
    if (rawGrade === '' || rawGrade == null) {
      setError('Ingresa una nota entre 1,0 y 7,0.')
      return
    }
    setSavingId(enrollmentId)
    setError('')
    setMessage('')
    try {
      await saveFinalGrade(sectionId, enrollmentId, Number(rawGrade))
      await loadStudents()
      setMessage('La calificación quedó registrada.')
    } catch (requestError) {
      setError(getErrorMessage(requestError))
    } finally {
      setSavingId(null)
    }
  }

  const selectedSection = sections.find((section) => String(section.id) === sectionId)
  const selectedPeriod = periods.find((period) => String(period.id) === periodId)
  const canEditGrades = selectedPeriod?.status === 'OPEN'

  return (
    <div className="mx-auto max-w-6xl px-6 py-10">
      <PageHeading eyebrow="Portal docente" title="Calificaciones de mis secciones" description="Consulta las secciones que tienes asignadas y registra la nota final de cada estudiante. El backend verifica la asignación y el estado del período." />
      <Card className="mb-6">
        <div className="grid gap-4 md:grid-cols-2">
          <Field label="Período académico">
            <select className={inputClassName} onChange={(event) => setPeriodId(event.target.value)} value={periodId}>
              {periods.map((period) => <option key={period.id} value={period.id}>{period.code} · {period.status === 'OPEN' ? 'Abierto' : 'Cerrado'}</option>)}
            </select>
          </Field>
          <Field label="Sección asignada">
            <select className={inputClassName} onChange={(event) => setSectionId(event.target.value)} value={sectionId}>
              <option value="">Selecciona una sección</option>
              {sections.map((section) => (
                <option key={section.id} value={section.id}>{section.courseCode} · {section.courseName} · sección {section.id}</option>
              ))}
            </select>
          </Field>
        </div>
        {selectedSection && (
          <p className="mt-4 text-sm text-slate-500">
            {selectedSection.academicPeriodCode} · {selectedSection.scheduleBlocks.join(', ')} · capacidad {selectedSection.capacity}
          </p>
        )}
      </Card>

      {error && <div className="mb-4"><Notice tone="error">{error}</Notice></div>}
      {message && <div className="mb-4"><Notice tone="success">{message}</Notice></div>}
      {loading
        ? <LoadingState />
        : !sectionId
          ? <EmptyState>{sections.length ? 'Selecciona una sección para consultar a tus estudiantes.' : 'No tienes secciones asignadas en este período.'}</EmptyState>
          : (
            <Card>
              <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
                <div>
                  <h2 className="text-lg font-bold text-navy">Estudiantes inscritos</h2>
                  <p className="mt-1 text-sm text-slate-500">{students.length} inscripciones activas</p>
                </div>
                {!canEditGrades && <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-600">Período cerrado · solo lectura</span>}
              </div>
              {students.length === 0
                ? <EmptyState>La sección no tiene estudiantes inscritos.</EmptyState>
                : (
                  <div className="overflow-x-auto">
                    <table className="w-full min-w-[700px] text-left text-sm">
                      <thead className="border-b border-slate-200 text-xs uppercase text-slate-500">
                        <tr><th className="py-3 pr-4">Estudiante</th><th className="py-3 pr-4">RUN</th><th className="py-3 pr-4">Nota final</th><th className="py-3">Resultado</th></tr>
                      </thead>
                      <tbody className="divide-y divide-slate-100">
                        {students.map((student) => (
                          <tr key={student.id}>
                            <td className="py-3 pr-4 font-medium">{student.studentName}</td>
                            <td className="py-3 pr-4 text-slate-600">{student.studentNationalId}</td>
                            <td className="py-3 pr-4">
                              <form className="flex items-center gap-2" onSubmit={(event) => submitGrade(event, student.id)}>
                                <input
                                  aria-label={`Nota final de ${student.studentName}`}
                                  className={`${inputClassName} max-w-28`}
                                  max="7"
                                  min="1"
                                  onChange={(event) => setGrades((current) => ({ ...current, [student.id]: event.target.value }))}
                                  placeholder={formatGrade(student.finalGrade)}
                                  step="0.1"
                                  type="number"
                                  value={grades[student.id] ?? ''}
                                  disabled={!canEditGrades}
                                />
                                <span className="text-xs text-slate-500">actual: {formatGrade(student.finalGrade)}</span>
                                <button
                                  className="button-primary whitespace-nowrap"
                                  disabled={!canEditGrades || savingId === student.id}
                                  type="submit"
                                >
                                  {savingId === student.id ? 'Guardando…' : student.finalGrade == null ? 'Registrar' : 'Actualizar'}
                                </button>
                              </form>
                            </td>
                            <td className="py-3 text-slate-500">{student.result === 'PASSED' ? 'Aprobada' : student.result === 'FAILED' ? 'Reprobada' : 'Pendiente'}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
            </Card>
          )}
    </div>
  )
}
