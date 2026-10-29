const modules = [
  {
    number: '01',
    title: 'Administración académica',
    description: 'Estudiantes, carreras, planes de estudio, asignaturas y docentes.',
  },
  {
    number: '02',
    title: 'Oferta e inscripción',
    description: 'Períodos, secciones, horarios, cupos e inscripción de asignaturas.',
  },
  {
    number: '03',
    title: 'Seguimiento académico',
    description: 'Calificaciones finales, cierre de período e historial académico.',
  },
]

export default function HomePage({ apiStatus, roles }) {
  return (
    <div className="mx-auto max-w-6xl px-6 py-12 sm:py-16">
      <section className="overflow-hidden rounded-3xl bg-navy px-8 py-12 text-white sm:px-12 sm:py-16">
        <p className="mb-4 text-sm font-semibold uppercase tracking-[0.2em] text-teal-200">
          Instituto Profesional de Tecnología
        </p>
        <h1 className="max-w-3xl text-4xl font-bold tracking-tight sm:text-5xl">
          La gestión académica, en un solo lugar.
        </h1>
        <p className="mt-5 max-w-2xl text-lg leading-8 text-slate-200">
          SIGA IPT integra la información y los procesos de estudiantes, docentes y administración académica.
        </p>
        <div className="mt-8 flex flex-wrap gap-3">
          <span className="rounded-full bg-white/10 px-4 py-2 text-sm">
            API {apiStatus === 'online' ? 'operativa' : apiStatus === 'checking' ? 'verificando' : 'no disponible'}
          </span>
          {roles.length > 0 && roles.map((role) => (
            <span className="rounded-full bg-teal-700 px-4 py-2 text-sm" key={role}>{role}</span>
          ))}
        </div>
      </section>

      <section aria-labelledby="modules-title" className="py-12">
        <div className="mb-6">
          <p className="text-sm font-semibold uppercase tracking-wider text-teal-700">Plataforma académica</p>
          <h2 className="mt-2 text-2xl font-bold text-navy" id="modules-title">Procesos del sistema</h2>
        </div>
        <div className="grid gap-4 md:grid-cols-3">
          {modules.map((module) => (
            <article className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm" key={module.number}>
              <span className="text-sm font-bold text-teal">{module.number}</span>
              <h3 className="mt-4 text-lg font-bold text-navy">{module.title}</h3>
              <p className="mt-2 leading-6 text-slate-600">{module.description}</p>
            </article>
          ))}
        </div>
      </section>
    </div>
  )
}
