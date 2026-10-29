import { useEffect, useMemo, useState } from 'react'
import api from '../api/client'
import { getErrorMessage } from '../api/academic'
import { Card, EmptyState, Field, inputClassName, LoadingState, Notice } from './PortalUi'

const emptyCareer = {
  code: '',
  name: '',
  description: '',
  startYear: String(new Date().getFullYear()),
  annualVacancies: '50',
}

const emptyCourse = {
  code: '',
  name: '',
  semester: '1',
  theoryHours: '2',
  exerciseHours: '0',
  laboratoryHours: '0',
  credits: '3',
  prerequisiteIds: [],
}

function toCourseRequest(course) {
  return {
    code: course.code,
    name: course.name,
    semester: Number(course.semester),
    theoryHours: Number(course.theoryHours),
    exerciseHours: Number(course.exerciseHours),
    laboratoryHours: Number(course.laboratoryHours),
    credits: Number(course.credits),
    prerequisiteIds: course.prerequisiteIds.map(Number),
  }
}

export default function AdminCatalogs({ careers, onCatalogChanged }) {
  const [careerId, setCareerId] = useState('')
  const [careerForm, setCareerForm] = useState(emptyCareer)
  const [careerMode, setCareerMode] = useState('create')
  const [plans, setPlans] = useState([])
  const [planId, setPlanId] = useState('')
  const [planForm, setPlanForm] = useState({ version: '', currentPlan: true })
  const [courses, setCourses] = useState([])
  const [courseId, setCourseId] = useState('')
  const [courseForm, setCourseForm] = useState(emptyCourse)
  const [loadingPlans, setLoadingPlans] = useState(false)
  const [loadingCourses, setLoadingCourses] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const selectedCareer = careers.find((career) => String(career.id) === careerId)
  const selectedCourse = courses.find((course) => String(course.id) === courseId)
  const eligiblePrerequisites = useMemo(
    () => courses.filter((course) =>
      course.id !== selectedCourse?.id && course.semester < Number(courseForm.semester)),
    [courses, courseForm.semester, selectedCourse?.id],
  )

  useEffect(() => {
    if (careerId && careers.some((career) => String(career.id) === careerId)) return
    setCareerId(careers[0] ? String(careers[0].id) : '')
  }, [careerId, careers])

  useEffect(() => {
    if (careerMode === 'create') {
      setCareerForm(emptyCareer)
      return
    }
    if (!selectedCareer) return
    setCareerForm({
      code: selectedCareer.code,
      name: selectedCareer.name,
      description: selectedCareer.description,
      startYear: String(selectedCareer.startYear),
      annualVacancies: String(selectedCareer.annualVacancies),
    })
  }, [careerMode, selectedCareer])

  useEffect(() => {
    if (!careerId) {
      setPlans([])
      setPlanId('')
      setCourses([])
      setCourseId('')
      return
    }
    setLoadingPlans(true)
    setError('')
    api.get(`/admin/careers/${careerId}/study-plans`)
      .then(({ data }) => {
        setPlans(data)
        setPlanId((current) => data.some((plan) => String(plan.id) === current)
          ? current
          : String(data.find((plan) => plan.currentPlan)?.id ?? data[0]?.id ?? ''))
      })
      .catch((requestError) => setError(getErrorMessage(requestError)))
      .finally(() => setLoadingPlans(false))
  }, [careerId])

  function selectCareer(id) {
    setCareerId(id)
    setPlans([])
    setPlanId('')
    setCourses([])
    setCourseId('')
  }

  useEffect(() => {
    if (!planId) {
      setCourses([])
      setCourseId('')
      return
    }
    setLoadingCourses(true)
    setError('')
    api.get(`/admin/study-plans/${planId}/courses`)
      .then(({ data }) => {
        setCourses(data)
        setCourseId((current) => data.some((course) => String(course.id) === current) ? current : '')
      })
      .catch((requestError) => setError(getErrorMessage(requestError)))
      .finally(() => setLoadingCourses(false))
  }, [planId])

  useEffect(() => {
    if (!selectedCourse) {
      setCourseForm(emptyCourse)
      return
    }
    setCourseForm({
      code: selectedCourse.code,
      name: selectedCourse.name,
      semester: String(selectedCourse.semester),
      theoryHours: String(selectedCourse.theoryHours),
      exerciseHours: String(selectedCourse.exerciseHours),
      laboratoryHours: String(selectedCourse.laboratoryHours),
      credits: String(selectedCourse.credits),
      prerequisiteIds: courses
        .filter((course) => selectedCourse.prerequisiteCodes.includes(course.code))
        .map((course) => String(course.id)),
    })
  }, [selectedCourse, courses])

  async function submit(action, successMessage, afterSuccess) {
    setBusy(true)
    setError('')
    setMessage('')
    try {
      const result = await action()
      await afterSuccess?.(result)
      setMessage(successMessage)
      return result
    } catch (requestError) {
      setError(getErrorMessage(requestError))
      return null
    } finally {
      setBusy(false)
    }
  }

  async function saveCareer(event) {
    event.preventDefault()
    const body = {
      ...careerForm,
      startYear: Number(careerForm.startYear),
      annualVacancies: Number(careerForm.annualVacancies),
    }
    if (careerMode === 'create') {
      const created = await submit(
        () => api.post('/admin/careers', body).then(({ data }) => data),
        'La carrera quedó creada.',
        async (data) => {
          setCareerMode('edit')
          setCareerId(String(data.id))
          await onCatalogChanged()
        },
      )
      if (created) setCareerForm({
        code: created.code,
        name: created.name,
        description: created.description,
        startYear: String(created.startYear),
        annualVacancies: String(created.annualVacancies),
      })
      return
    }
    await submit(
      () => api.put(`/admin/careers/${careerId}`, body),
      'Los datos de la carrera quedaron actualizados.',
      onCatalogChanged,
    )
  }

  async function changeCareerStatus(status) {
    await submit(
      () => api.put(`/admin/careers/${careerId}/status`, { status }),
      `La carrera quedó ${status === 'ACTIVE' ? 'activa' : 'inactiva'}.`,
      onCatalogChanged,
    )
  }

  async function createPlan(event) {
    event.preventDefault()
    const created = await submit(
      () => api.post(`/admin/careers/${careerId}/study-plans`, planForm).then(({ data }) => data),
      'El plan de estudio quedó creado.',
      async () => {
        const { data } = await api.get(`/admin/careers/${careerId}/study-plans`)
        setPlans(data)
        await onCatalogChanged()
      },
    )
    if (created) {
      setPlanId(String(created.id))
      setPlanForm({ version: '', currentPlan: true })
    }
  }

  async function setCurrentPlan(plan) {
    await submit(
      () => api.put(`/admin/study-plans/${plan.id}/current`),
      `El plan ${plan.version} quedó vigente.`,
      async () => {
        const { data } = await api.get(`/admin/careers/${careerId}/study-plans`)
        setPlans(data)
        await onCatalogChanged()
      },
    )
  }

  async function saveCourse(event) {
    event.preventDefault()
    const body = toCourseRequest(courseForm)
    const editing = Boolean(courseId)
    const saved = await submit(
      () => editing
        ? api.put(`/admin/courses/${courseId}`, body)
        : api.post(`/admin/study-plans/${planId}/courses`, body),
      editing ? 'La asignatura quedó actualizada.' : 'La asignatura quedó agregada al plan.',
      async () => {
        const { data } = await api.get(`/admin/study-plans/${planId}/courses`)
        setCourses(data)
        await onCatalogChanged()
      },
    )
    if (saved && !editing) setCourseForm(emptyCourse)
  }

  function togglePrerequisite(id) {
    const prerequisiteIds = courseForm.prerequisiteIds.includes(id)
      ? courseForm.prerequisiteIds.filter((prerequisiteId) => prerequisiteId !== id)
      : [...courseForm.prerequisiteIds, id]
    setCourseForm({ ...courseForm, prerequisiteIds })
  }

  const telHours = Number(courseForm.theoryHours) + Number(courseForm.exerciseHours) + Number(courseForm.laboratoryHours)

  return (
    <div className="space-y-6">
      {error && <Notice tone="error">{error}</Notice>}
      {message && <Notice tone="success">{message}</Notice>}

      <Card>
        <div className="mb-5 flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 className="text-lg font-bold text-navy">Carreras</h2>
            <p className="mt-1 text-sm text-slate-500">Cada carrera tiene duración fija de cuatro semestres. Las carreras con planes o estudiantes no se eliminan.</p>
          </div>
          {careerMode === 'edit' && selectedCareer && (
            <span className={`rounded-full px-3 py-1 text-xs font-semibold ${selectedCareer.status === 'ACTIVE' ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-100 text-slate-600'}`}>
              {selectedCareer.status === 'ACTIVE' ? 'Activa' : 'Inactiva'}
            </span>
          )}
        </div>
        <div className="mb-5 flex flex-wrap gap-2">
          <button className={careerMode === 'create' ? 'button-primary' : 'button-secondary'} onClick={() => setCareerMode('create')} type="button">Nueva carrera</button>
          <button className={careerMode === 'edit' ? 'button-primary' : 'button-secondary'} disabled={!careers.length} onClick={() => setCareerMode('edit')} type="button">Editar carrera</button>
        </div>
        {careerMode === 'edit' && (
          <Field label="Carrera">
            <select className={inputClassName} onChange={(event) => selectCareer(event.target.value)} value={careerId}>
              {careers.map((career) => <option key={career.id} value={career.id}>{career.code} · {career.name} ({career.status === 'ACTIVE' ? 'activa' : 'inactiva'})</option>)}
            </select>
          </Field>
        )}
        {careerMode === 'create' || selectedCareer ? (
          <form className="mt-4 grid gap-4 md:grid-cols-2" onSubmit={saveCareer}>
            <Field label="Código"><input className={inputClassName} onChange={(event) => setCareerForm({ ...careerForm, code: event.target.value })} required value={careerForm.code} /></Field>
            <Field label="Nombre"><input className={inputClassName} onChange={(event) => setCareerForm({ ...careerForm, name: event.target.value })} required value={careerForm.name} /></Field>
            <Field label="Descripción"><textarea className={inputClassName} onChange={(event) => setCareerForm({ ...careerForm, description: event.target.value })} required rows="2" value={careerForm.description} /></Field>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Año de inicio"><input className={inputClassName} min="1" onChange={(event) => setCareerForm({ ...careerForm, startYear: event.target.value })} required type="number" value={careerForm.startYear} /></Field>
              <Field label="Vacantes anuales"><input className={inputClassName} min="1" onChange={(event) => setCareerForm({ ...careerForm, annualVacancies: event.target.value })} required type="number" value={careerForm.annualVacancies} /></Field>
            </div>
            <div className="flex flex-wrap items-center gap-3 md:col-span-2">
              <button className="button-primary" disabled={busy} type="submit">{careerMode === 'create' ? 'Crear carrera' : 'Guardar cambios'}</button>
              {careerMode === 'edit' && selectedCareer && (
                <button
                  className="button-secondary"
                  disabled={busy}
                  onClick={() => changeCareerStatus(selectedCareer.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE')}
                  type="button"
                >
                  {selectedCareer.status === 'ACTIVE' ? 'Inactivar carrera' : 'Activar carrera'}
                </button>
              )}
            </div>
          </form>
        ) : <EmptyState>No hay carreras para editar.</EmptyState>}
      </Card>

      <Card>
        <h2 className="mb-2 text-lg font-bold text-navy">Planes de estudio</h2>
        <p className="mb-4 text-sm text-slate-500">Crear un plan vigente reemplaza el vigente anterior, sin cambiar el plan de estudiantes ya registrados.</p>
        <Field label="Carrera asociada">
          <select className={inputClassName} onChange={(event) => selectCareer(event.target.value)} value={careerId}>
            {careers.map((career) => <option key={career.id} value={career.id}>{career.code} · {career.name}</option>)}
          </select>
        </Field>
        {loadingPlans ? <LoadingState /> : (
          <div className="mt-4 space-y-3">
            {plans.length === 0
              ? <EmptyState>Esta carrera aún no tiene planes de estudio.</EmptyState>
              : plans.map((plan) => (
                <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-200 p-4" key={plan.id}>
                  <div><p className="font-semibold text-navy">Versión {plan.version}</p><p className="mt-1 text-sm text-slate-500">{plan.currentPlan ? 'Vigente' : 'No vigente'}</p></div>
                  {!plan.currentPlan && <button className="button-secondary" disabled={busy} onClick={() => setCurrentPlan(plan)} type="button">Establecer como vigente</button>}
                </div>
              ))}
          </div>
        )}
        {careerId && (
          <form className="mt-5 grid gap-4 border-t border-slate-200 pt-5 sm:grid-cols-[1fr_auto_auto] sm:items-end" onSubmit={createPlan}>
            <Field label="Versión del nuevo plan"><input className={inputClassName} maxLength="30" onChange={(event) => setPlanForm({ ...planForm, version: event.target.value })} placeholder="2027.1" required value={planForm.version} /></Field>
            <label className="flex items-center gap-2 pb-2 text-sm text-slate-700">
              <input checked={planForm.currentPlan} onChange={(event) => setPlanForm({ ...planForm, currentPlan: event.target.checked })} type="checkbox" />
              Dejar como vigente
            </label>
            <button className="button-primary" disabled={busy} type="submit">Crear plan</button>
          </form>
        )}
      </Card>

      <Card>
        <div className="mb-4">
          <h2 className="text-lg font-bold text-navy">Asignaturas del plan</h2>
          <p className="mt-1 text-sm text-slate-500">TEL es la suma de teoría, ejercicios y laboratorio. Se permiten hasta tres prerrequisitos de semestres anteriores del mismo plan.</p>
        </div>
        <div className="grid gap-4 md:grid-cols-2">
          <Field label="Plan de estudio">
            <select className={inputClassName} disabled={!plans.length} onChange={(event) => {
              setPlanId(event.target.value)
              setCourseId('')
              setCourses([])
            }} value={planId}>
              {plans.map((plan) => <option key={plan.id} value={plan.id}>{plan.version}{plan.currentPlan ? ' · vigente' : ''}</option>)}
            </select>
          </Field>
          <Field label="Asignatura para editar (opcional)">
            <select className={inputClassName} onChange={(event) => setCourseId(event.target.value)} value={courseId}>
              <option value="">Nueva asignatura</option>
              {courses.map((course) => <option key={course.id} value={course.id}>{course.code} · {course.name}</option>)}
            </select>
          </Field>
        </div>
        {loadingCourses ? <LoadingState /> : (
          <div className="my-4">
            {courses.length === 0 ? <EmptyState>No hay asignaturas registradas en este plan.</EmptyState> : (
              <div className="overflow-x-auto">
                <table className="w-full min-w-[620px] text-left text-sm">
                  <thead className="border-b border-slate-200 text-xs uppercase text-slate-500">
                    <tr><th className="py-2 pr-4">Semestre</th><th className="py-2 pr-4">Código y asignatura</th><th className="py-2 pr-4">TEL</th><th className="py-2">SCT</th></tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {courses.map((course) => (
                      <tr className={String(course.id) === courseId ? 'bg-teal-50' : ''} key={course.id}>
                        <td className="py-2 pr-4">{course.semester}</td>
                        <td className="py-2 pr-4">{course.code} · {course.name}</td>
                        <td className="py-2 pr-4">{course.theoryHours}-{course.exerciseHours}-{course.laboratoryHours}</td>
                        <td className="py-2">{course.credits}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}
        {planId && (
          <form className="grid gap-4 border-t border-slate-200 pt-5 md:grid-cols-2" onSubmit={saveCourse}>
            <Field label="Código"><input className={inputClassName} maxLength="20" onChange={(event) => setCourseForm({ ...courseForm, code: event.target.value })} required value={courseForm.code} /></Field>
            <Field label="Nombre"><input className={inputClassName} maxLength="150" onChange={(event) => setCourseForm({ ...courseForm, name: event.target.value })} required value={courseForm.name} /></Field>
            <Field label="Semestre">
              <select className={inputClassName} onChange={(event) => setCourseForm({ ...courseForm, semester: event.target.value, prerequisiteIds: [] })} value={courseForm.semester}>
                {[1, 2, 3, 4].map((semester) => <option key={semester} value={semester}>{semester}</option>)}
              </select>
            </Field>
            <Field label="Créditos SCT (1–7)"><input className={inputClassName} max="7" min="1" onChange={(event) => setCourseForm({ ...courseForm, credits: event.target.value })} required type="number" value={courseForm.credits} /></Field>
            <div className="grid gap-3 sm:grid-cols-3">
              <Field label="Teoría"><input className={inputClassName} min="0" onChange={(event) => setCourseForm({ ...courseForm, theoryHours: event.target.value })} required type="number" value={courseForm.theoryHours} /></Field>
              <Field label="Ejercicios"><input className={inputClassName} min="0" onChange={(event) => setCourseForm({ ...courseForm, exerciseHours: event.target.value })} required type="number" value={courseForm.exerciseHours} /></Field>
              <Field label="Laboratorio"><input className={inputClassName} min="0" onChange={(event) => setCourseForm({ ...courseForm, laboratoryHours: event.target.value })} required type="number" value={courseForm.laboratoryHours} /></Field>
            </div>
            <div className="self-end rounded-lg bg-slate-50 px-3 py-2 text-sm text-slate-700">
              TEL semanal: <strong>{telHours} horas</strong> · {telHours < 8 ? 'debe ser menor que 8' : 'el máximo permitido es menor que 8'}
            </div>
            <div className="md:col-span-2">
              <p className="mb-2 text-sm font-medium text-slate-700">Prerrequisitos</p>
              {eligiblePrerequisites.length === 0
                ? <p className="text-sm text-slate-500">No hay asignaturas elegibles de semestres anteriores.</p>
                : (
                  <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
                    {eligiblePrerequisites.map((course) => (
                      <label className="flex items-center gap-2 rounded-lg border border-slate-200 px-3 py-2 text-sm" key={course.id}>
                        <input checked={courseForm.prerequisiteIds.includes(String(course.id))} disabled={!courseForm.prerequisiteIds.includes(String(course.id)) && courseForm.prerequisiteIds.length >= 3} onChange={() => togglePrerequisite(String(course.id))} type="checkbox" />
                        {course.code} · {course.name}
                      </label>
                    ))}
                  </div>
                )}
              <p className="mt-2 text-xs text-slate-500">Seleccionados: {courseForm.prerequisiteIds.length} de 3. Al editar, esta selección reemplaza la lista actual.</p>
            </div>
            <div className="flex gap-3 md:col-span-2">
              <button className="button-primary" disabled={busy || telHours >= 8} type="submit">{courseId ? 'Guardar asignatura' : 'Agregar asignatura'}</button>
              {courseId && <button className="button-secondary" onClick={() => setCourseId('')} type="button">Cancelar edición</button>}
            </div>
          </form>
        )}
      </Card>
    </div>
  )
}
