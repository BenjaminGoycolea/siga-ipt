import { useCallback, useEffect, useState } from 'react'
import api from '../api/client'
import { getAcademicPeriods, getErrorMessage } from '../api/academic'
import { Card, EmptyState, Field, inputClassName, LoadingState, Notice, PageHeading } from '../components/PortalUi'
import AdminCatalogs from '../components/AdminCatalogs'

const initialStudent = { nationalId: '', fullName: '', email: '', careerId: '' }
const initialTeacher = { nationalId: '', fullName: '', email: '', professionalTitle: '', academicDegree: '' }

export default function AdminWorkspace() {
  const [tab, setTab] = useState('students')
  const [students, setStudents] = useState([])
  const [teachers, setTeachers] = useState([])
  const [careers, setCareers] = useState([])
  const [periods, setPeriods] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [studentForm, setStudentForm] = useState(initialStudent)
  const [teacherForm, setTeacherForm] = useState(initialTeacher)
  const [periodForm, setPeriodForm] = useState({ year: String(new Date().getFullYear()), semester: 1 })
  const [identityValues, setIdentityValues] = useState({})
  const [selectedPeriodId, setSelectedPeriodId] = useState('')
  const [selectedCareerId, setSelectedCareerId] = useState('')
  const [studyPlans, setStudyPlans] = useState([])
  const [studyPlanId, setStudyPlanId] = useState('')
  const [courses, setCourses] = useState([])
  const [catalogRevision, setCatalogRevision] = useState(0)
  const [sectionForm, setSectionForm] = useState({ courseId: '', teacherId: '', capacity: '40', scheduleBlocks: '' })
  const [busy, setBusy] = useState(false)

  const reload = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [studentData, teacherData, careerData, periodData] = await Promise.all([
        api.get('/admin/students').then(({ data }) => data),
        api.get('/admin/teachers').then(({ data }) => data),
        api.get('/admin/careers').then(({ data }) => data),
        getAcademicPeriods(),
      ])
      setStudents(studentData)
      setTeachers(teacherData)
      setCareers(careerData)
      setPeriods(periodData)
      setSelectedPeriodId((current) => current || String(periodData.find((period) => period.status === 'OPEN')?.id ?? ''))
      setStudentForm((current) => current.careerId || !careerData.length
        ? current
        : { ...current, careerId: String(careerData[0].id) })
    } catch (requestError) {
      setError(getErrorMessage(requestError))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void reload()
  }, [reload])

  useEffect(() => {
    if (!selectedCareerId) {
      setStudyPlans([])
      setStudyPlanId('')
      setCourses([])
      return
    }
    api.get(`/admin/careers/${selectedCareerId}/study-plans`)
      .then(({ data }) => {
        setStudyPlans(data)
        const currentPlan = data.find((plan) => plan.currentPlan)
        setStudyPlanId(currentPlan ? String(currentPlan.id) : data[0] ? String(data[0].id) : '')
      })
      .catch((requestError) => setError(getErrorMessage(requestError)))
  }, [catalogRevision, selectedCareerId])

  useEffect(() => {
    if (!studyPlanId) {
      setCourses([])
      return
    }
    api.get(`/admin/study-plans/${studyPlanId}/courses`)
      .then(({ data }) => setCourses(data))
      .catch((requestError) => setError(getErrorMessage(requestError)))
  }, [catalogRevision, studyPlanId])

  const refreshCatalogs = useCallback(async () => {
    const { data } = await api.get('/admin/careers')
    setCareers(data)
    setCatalogRevision((revision) => revision + 1)
  }, [])

  async function performAction(action, successMessage) {
    setBusy(true)
    setError('')
    setMessage('')
    try {
      await action()
      await reload()
      setMessage(successMessage)
      return true
    } catch (requestError) {
      setError(getErrorMessage(requestError))
      return false
    } finally {
      setBusy(false)
    }
  }

  async function createStudent(event) {
    event.preventDefault()
    const succeeded = await performAction(
      () => api.post('/admin/students', { ...studentForm, careerId: Number(studentForm.careerId) }),
      'El estudiante quedó registrado.',
    )
    if (succeeded) setStudentForm((current) => ({ ...initialStudent, careerId: current.careerId }))
  }

  async function createTeacher(event) {
    event.preventDefault()
    const succeeded = await performAction(() => api.post('/admin/teachers', teacherForm), 'El docente quedó registrado.')
    if (succeeded) setTeacherForm(initialTeacher)
  }

  async function linkIdentity(kind, person) {
    const subject = identityValues[`${kind}-${person.id}`]?.trim()
    if (!subject) {
      setError('Ingresa el claim sub del token Keycloak de esta persona.')
      return
    }
    const endpoint = kind === 'student' ? 'students' : 'teachers'
    await performAction(
      () => api.put(`/admin/${endpoint}/${person.id}/keycloak-subject`, { keycloakSubject: subject }),
      `La cuenta Keycloak quedó vinculada a ${person.fullName}.`,
    )
  }

  async function updateMatriculation(student, matriculated) {
    if (!selectedPeriodId) {
      setError('Crea o selecciona un período académico primero.')
      return
    }
    await performAction(
      () => api.put(`/admin/students/${student.id}/periods/${selectedPeriodId}/matriculation`, { matriculated }),
      `Matrícula de ${student.fullName} actualizada.`,
    )
  }

  async function createPeriod(event) {
    event.preventDefault()
    await performAction(
      () => api.post('/admin/periods', { ...periodForm, year: Number(periodForm.year), semester: Number(periodForm.semester) }),
      'El período académico quedó creado.',
    )
  }

  async function closePeriod(period) {
    if (!window.confirm(`¿Cerrar el período ${period.code}? Esta acción es permanente y requiere que todas las notas estén registradas.`)) return
    await performAction(
      () => api.put(`/admin/periods/${period.id}/status`, { status: 'CLOSED' }),
      `El período ${period.code} quedó cerrado.`,
    )
  }

  async function createSection(event) {
    event.preventDefault()
    const scheduleBlocks = sectionForm.scheduleBlocks.split(',').map((block) => block.trim()).filter(Boolean)
    await performAction(
      () => api.post(`/admin/periods/${selectedPeriodId}/sections`, {
        courseId: Number(sectionForm.courseId),
        teacherId: Number(sectionForm.teacherId),
        capacity: Number(sectionForm.capacity),
        scheduleBlocks,
      }),
      'La sección quedó agregada a la oferta.',
    )
  }

  const tabs = [
    { id: 'students', label: 'Estudiantes' },
    { id: 'teachers', label: 'Docentes' },
    { id: 'periods', label: 'Períodos' },
    { id: 'sections', label: 'Secciones y oferta' },
    { id: 'catalogs', label: 'Carreras y asignaturas' },
  ]
  const selectedCourse = courses.find((course) => String(course.id) === sectionForm.courseId)
  const selectedCourseTelHours = selectedCourse
    ? selectedCourse.theoryHours + selectedCourse.exerciseHours + selectedCourse.laboratoryHours
    : null
  const isSelectedCourseScheduleValid = selectedCourseTelHours != null
    && selectedCourseTelHours > 0
    && selectedCourseTelHours % 2 === 0
  const requiredScheduleBlocks = isSelectedCourseScheduleValid ? selectedCourseTelHours / 2 : null

  return (
    <div className="mx-auto max-w-6xl px-6 py-10">
      <PageHeading eyebrow="Administración" title="Gestión académica" description="Registra estudiantes y docentes, vincula sus cuentas, prepara períodos y oferta, y cierra períodos una vez completadas las calificaciones." />
      {error && <div className="mb-4"><Notice tone="error">{error}</Notice></div>}
      {message && <div className="mb-4"><Notice tone="success">{message}</Notice></div>}
      <div className="mb-6 flex flex-wrap gap-2 border-b border-slate-200">
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
      {loading ? <LoadingState /> : (
        <div className="space-y-6">
          {tab === 'students' && (
            <>
              <Card>
                <h2 className="mb-4 text-lg font-bold text-navy">Registrar estudiante</h2>
                <form className="grid gap-4 md:grid-cols-2" onSubmit={createStudent}>
                  <Field label="RUN"><input className={inputClassName} onChange={(event) => setStudentForm({ ...studentForm, nationalId: event.target.value })} required value={studentForm.nationalId} /></Field>
                  <Field label="Nombre completo"><input className={inputClassName} onChange={(event) => setStudentForm({ ...studentForm, fullName: event.target.value })} required value={studentForm.fullName} /></Field>
                  <Field label="Correo"><input className={inputClassName} onChange={(event) => setStudentForm({ ...studentForm, email: event.target.value })} required type="email" value={studentForm.email} /></Field>
                  <Field label="Carrera">
                    <select className={inputClassName} onChange={(event) => setStudentForm({ ...studentForm, careerId: event.target.value })} required value={studentForm.careerId}>
                      {careers.filter((career) => career.status === 'ACTIVE').map((career) => <option key={career.id} value={career.id}>{career.code} · {career.name}</option>)}
                    </select>
                  </Field>
                  <div className="md:col-span-2"><button className="button-primary" disabled={busy || !careers.length} type="submit">Registrar estudiante</button></div>
                </form>
              </Card>
              <Card>
                <div className="mb-4 flex flex-wrap items-end justify-between gap-4">
                  <div><h2 className="text-lg font-bold text-navy">Estudiantes registrados</h2><p className="mt-1 text-sm text-slate-500">Para habilitar el acceso de estudiante, asigna el rol en Keycloak y vincula su claim sub.</p></div>
                  <Field label="Período para matrícula" className="w-full sm:max-w-xs">
                    <select className={inputClassName} onChange={(event) => setSelectedPeriodId(event.target.value)} value={selectedPeriodId}>
                      <option value="">Selecciona un período</option>
                      {periods.map((period) => <option key={period.id} value={period.id}>{period.code}</option>)}
                    </select>
                  </Field>
                </div>
                {students.length === 0 ? <EmptyState>No hay estudiantes registrados.</EmptyState> : (
                  <div className="space-y-4">
                    {students.map((student) => (
                      <div className="rounded-xl border border-slate-200 p-4" key={student.id}>
                        <div className="flex flex-wrap items-start justify-between gap-3">
                          <div>
                            <p className="font-semibold text-navy">{student.fullName} <span className="font-normal text-slate-500">· {student.nationalId}</span></p>
                            <p className="mt-1 text-sm text-slate-600">{student.email} · {student.careerName} · {student.status}</p>
                          </div>
                          <span className={`rounded-full px-3 py-1 text-xs font-semibold ${student.identityLinked ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'}`}>
                            {student.identityLinked ? 'Keycloak vinculado' : 'Sin vincular'}
                          </span>
                        </div>
                        <div className="mt-4 grid gap-3 md:grid-cols-[1fr_auto_auto]">
                          <input
                            aria-label={`Claim sub de ${student.fullName}`}
                            className={inputClassName}
                            onChange={(event) => setIdentityValues({ ...identityValues, [`student-${student.id}`]: event.target.value })}
                            placeholder="Claim sub del token Keycloak"
                            value={identityValues[`student-${student.id}`] ?? ''}
                          />
                          <button className="button-secondary" disabled={busy} onClick={() => linkIdentity('student', student)} type="button">Vincular cuenta</button>
                          <button className="button-secondary" disabled={busy || !selectedPeriodId || student.status !== 'REGULAR'} onClick={() => updateMatriculation(student, true)} type="button">Marcar matrícula</button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </Card>
            </>
          )}

          {tab === 'teachers' && (
            <>
              <Card>
                <h2 className="mb-4 text-lg font-bold text-navy">Registrar docente</h2>
                <form className="grid gap-4 md:grid-cols-2" onSubmit={createTeacher}>
                  <Field label="RUN"><input className={inputClassName} onChange={(event) => setTeacherForm({ ...teacherForm, nationalId: event.target.value })} required value={teacherForm.nationalId} /></Field>
                  <Field label="Nombre completo"><input className={inputClassName} onChange={(event) => setTeacherForm({ ...teacherForm, fullName: event.target.value })} required value={teacherForm.fullName} /></Field>
                  <Field label="Correo"><input className={inputClassName} onChange={(event) => setTeacherForm({ ...teacherForm, email: event.target.value })} required type="email" value={teacherForm.email} /></Field>
                  <Field label="Título profesional"><input className={inputClassName} onChange={(event) => setTeacherForm({ ...teacherForm, professionalTitle: event.target.value })} required value={teacherForm.professionalTitle} /></Field>
                  <Field label="Grado académico"><input className={inputClassName} onChange={(event) => setTeacherForm({ ...teacherForm, academicDegree: event.target.value })} required value={teacherForm.academicDegree} /></Field>
                  <div className="md:col-span-2"><button className="button-primary" disabled={busy} type="submit">Registrar docente</button></div>
                </form>
              </Card>
              <Card>
                <h2 className="mb-4 text-lg font-bold text-navy">Docentes y cuentas vinculadas</h2>
                {teachers.length === 0 ? <EmptyState>No hay docentes registrados.</EmptyState> : (
                  <div className="space-y-4">
                    {teachers.map((teacher) => (
                      <div className="rounded-xl border border-slate-200 p-4" key={teacher.id}>
                        <div className="flex flex-wrap justify-between gap-2">
                          <div><p className="font-semibold text-navy">{teacher.fullName} · {teacher.nationalId}</p><p className="mt-1 text-sm text-slate-600">{teacher.professionalTitle} · {teacher.email} · {teacher.status}</p></div>
                          <span className={`h-fit rounded-full px-3 py-1 text-xs font-semibold ${teacher.identityLinked ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'}`}>{teacher.identityLinked ? 'Keycloak vinculado' : 'Sin vincular'}</span>
                        </div>
                        <div className="mt-4 grid gap-3 sm:grid-cols-[1fr_auto]">
                          <input
                            aria-label={`Claim sub de ${teacher.fullName}`}
                            className={inputClassName}
                            onChange={(event) => setIdentityValues({ ...identityValues, [`teacher-${teacher.id}`]: event.target.value })}
                            placeholder="Claim sub del token Keycloak"
                            value={identityValues[`teacher-${teacher.id}`] ?? ''}
                          />
                          <button className="button-secondary" disabled={busy} onClick={() => linkIdentity('teacher', teacher)} type="button">Vincular cuenta</button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </Card>
            </>
          )}

          {tab === 'periods' && (
            <>
              <Card>
                <h2 className="mb-4 text-lg font-bold text-navy">Crear período académico</h2>
                <form className="flex flex-wrap items-end gap-4" onSubmit={createPeriod}>
                  <Field label="Año"><input className={inputClassName} max="9999" min="2000" onChange={(event) => setPeriodForm({ ...periodForm, year: event.target.value })} required type="number" value={periodForm.year} /></Field>
                  <Field label="Semestre">
                    <select className={inputClassName} onChange={(event) => setPeriodForm({ ...periodForm, semester: Number(event.target.value) })} value={periodForm.semester}>
                      <option value="1">1</option><option value="2">2</option>
                    </select>
                  </Field>
                  <button className="button-primary" disabled={busy} type="submit">Crear período</button>
                </form>
              </Card>
              <Card>
                <h2 className="mb-4 text-lg font-bold text-navy">Períodos existentes</h2>
                {periods.length === 0 ? <EmptyState>No hay períodos académicos.</EmptyState> : (
                  <div className="divide-y divide-slate-100">
                    {periods.map((period) => (
                      <div className="flex flex-wrap items-center justify-between gap-3 py-4" key={period.id}>
                        <div><p className="font-semibold text-navy">{period.code}</p><p className="mt-1 text-sm text-slate-500">{period.status === 'OPEN' ? 'Abierto para inscripción y calificaciones' : 'Cerrado permanentemente'}</p></div>
                        {period.status === 'OPEN'
                          ? <button className="button-secondary" disabled={busy} onClick={() => closePeriod(period)} type="button">Cerrar período</button>
                          : <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-600">Cerrado</span>}
                      </div>
                    ))}
                  </div>
                )}
              </Card>
            </>
          )}

          {tab === 'sections' && (
            <>
              <Card>
                <h2 className="mb-2 text-lg font-bold text-navy">Agregar sección a la oferta</h2>
                <p className="mb-5 text-sm text-slate-500">La carrera, plan y asignatura deben existir previamente. Los bloques usan formato TEL, por ejemplo L2 o W3; el backend comprueba que coincidan con las horas de la asignatura.</p>
                <form className="grid gap-4 md:grid-cols-2" onSubmit={createSection}>
                  <Field label="Período abierto">
                    <select className={inputClassName} onChange={(event) => setSelectedPeriodId(event.target.value)} required value={selectedPeriodId}>
                      <option value="">Selecciona un período</option>
                      {periods.filter((period) => period.status === 'OPEN').map((period) => <option key={period.id} value={period.id}>{period.code}</option>)}
                    </select>
                  </Field>
                  <Field label="Carrera">
                    <select className={inputClassName} onChange={(event) => {
                      setSelectedCareerId(event.target.value)
                      setSectionForm({ ...sectionForm, courseId: '', scheduleBlocks: '' })
                    }} required value={selectedCareerId}>
                      <option value="">Selecciona una carrera</option>
                      {careers.map((career) => <option key={career.id} value={career.id}>{career.code} · {career.name}</option>)}
                    </select>
                  </Field>
                  <Field label="Plan de estudio">
                    <select className={inputClassName} onChange={(event) => {
                      setStudyPlanId(event.target.value)
                      setSectionForm({ ...sectionForm, courseId: '', scheduleBlocks: '' })
                    }} required value={studyPlanId}>
                      <option value="">Selecciona un plan</option>
                      {studyPlans.map((plan) => <option key={plan.id} value={plan.id}>{plan.version}{plan.currentPlan ? ' · vigente' : ''}</option>)}
                    </select>
                  </Field>
                  <Field label="Asignatura">
                    <select className={inputClassName} onChange={(event) => setSectionForm({ ...sectionForm, courseId: event.target.value, scheduleBlocks: '' })} required value={sectionForm.courseId}>
                      <option value="">Selecciona una asignatura</option>
                      {courses.map((course) => {
                        const telHours = course.theoryHours + course.exerciseHours + course.laboratoryHours
                        return (
                          <option key={course.id} value={course.id}>
                            {course.code} · {course.name} — TEL {course.theoryHours}-{course.exerciseHours}-{course.laboratoryHours} ({telHours} h, {telHours / 2} bloques)
                          </option>
                        )
                      })}
                    </select>
                  </Field>
                  {selectedCourse && (
                    <div className={`rounded-lg border px-4 py-3 text-sm md:col-span-2 ${isSelectedCourseScheduleValid ? 'border-teal-200 bg-teal-50 text-teal-950' : 'border-amber-200 bg-amber-50 text-amber-950'}`}>
                      <strong>TEL de {selectedCourse.code}: </strong>
                      {selectedCourse.theoryHours} teoría + {selectedCourse.exerciseHours} ejercicios + {selectedCourse.laboratoryHours} laboratorio = {selectedCourseTelHours} horas semanales.
                      {isSelectedCourseScheduleValid
                        ? <> Esta asignatura requiere <strong>{requiredScheduleBlocks} bloques horarios</strong> (2 horas pedagógicas por bloque).</>
                        : ' No se puede crear una sección: el total TEL debe ser positivo y divisible en bloques de 2 horas.'}
                    </div>
                  )}

                  <Field label="Docente activo">
                    <select className={inputClassName} onChange={(event) => setSectionForm({ ...sectionForm, teacherId: event.target.value })} required value={sectionForm.teacherId}>
                      <option value="">Selecciona un docente</option>
                      {teachers.filter((teacher) => teacher.status === 'ACTIVE').map((teacher) => <option key={teacher.id} value={teacher.id}>{teacher.fullName}</option>)}
                    </select>
                  </Field>
                  <Field label="Capacidad (1–60)">
                    <input className={inputClassName} max="60" min="1" onChange={(event) => setSectionForm({ ...sectionForm, capacity: event.target.value })} required type="number" value={sectionForm.capacity} />
                  </Field>
                  <Field label={`Bloques horarios separados por coma${requiredScheduleBlocks == null ? '' : ` · requiere ${requiredScheduleBlocks}`}`}>
                    <input className={inputClassName} onChange={(event) => setSectionForm({ ...sectionForm, scheduleBlocks: event.target.value })} placeholder={requiredScheduleBlocks == null ? 'Selecciona una asignatura con TEL válido' : 'Ej.: L2, W3, J1'} required value={sectionForm.scheduleBlocks} />
                  </Field>
                  <div className="md:col-span-2"><button className="button-primary" disabled={busy || !selectedPeriodId || !isSelectedCourseScheduleValid} type="submit">Crear sección</button></div>
                </form>
              </Card>
              {selectedPeriodId && (
                <SectionList periodId={selectedPeriodId} />
              )}
            </>
          )}

          {tab === 'catalogs' && (
            <AdminCatalogs careers={careers} onCatalogChanged={refreshCatalogs} />
          )}
        </div>
      )}
    </div>
  )
}

function SectionList({ periodId }) {
  const [sections, setSections] = useState([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    api.get(`/admin/periods/${periodId}/sections`)
      .then(({ data }) => setSections(data))
      .catch((requestError) => setError(getErrorMessage(requestError)))
      .finally(() => setLoading(false))
  }, [periodId])

  return (
    <Card>
      <h2 className="mb-4 text-lg font-bold text-navy">Secciones del período seleccionado</h2>
      {error && <Notice tone="error">{error}</Notice>}
      {loading ? <LoadingState /> : sections.length === 0 ? <EmptyState>Aún no se han creado secciones para este período.</EmptyState> : (
        <div className="grid gap-3 md:grid-cols-2">
          {sections.map((section) => (
            <div className="rounded-xl border border-slate-200 p-4" key={section.id}>
              <p className="font-semibold text-navy">{section.courseCode} · {section.courseName}</p>
              <p className="mt-1 text-sm text-slate-600">Docente: {section.teacherName}</p>
              <p className="mt-1 text-xs text-slate-500">Sección {section.id} · {section.scheduleBlocks.join(', ')} · capacidad {section.capacity}</p>
            </div>
          ))}
        </div>
      )}
    </Card>
  )
}
