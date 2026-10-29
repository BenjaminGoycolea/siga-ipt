export function PageHeading({ eyebrow, title, description }) {
  return (
    <div className="mb-8">
      {eyebrow && <p className="text-sm font-semibold uppercase tracking-wider text-teal-700">{eyebrow}</p>}
      <h1 className="mt-2 text-3xl font-bold tracking-tight text-navy">{title}</h1>
      {description && <p className="mt-2 max-w-3xl text-slate-600">{description}</p>}
    </div>
  )
}

export function Notice({ children, tone = 'info' }) {
  const styles = {
    info: 'border-sky-200 bg-sky-50 text-sky-900',
    success: 'border-emerald-200 bg-emerald-50 text-emerald-900',
    error: 'border-rose-200 bg-rose-50 text-rose-900',
  }
  return <div className={`rounded-xl border px-4 py-3 text-sm ${styles[tone]}`}>{children}</div>
}

export function Card({ children, className = '' }) {
  return <section className={`rounded-2xl border border-slate-200 bg-white p-5 shadow-sm ${className}`}>{children}</section>
}

export function Field({ label, children, className = '' }) {
  return (
    <label className={`block text-sm font-medium text-slate-700 ${className}`}>
      <span className="mb-1.5 block">{label}</span>
      {children}
    </label>
  )
}

export const inputClassName = 'w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-800 outline-none transition focus:border-teal-600 focus:ring-2 focus:ring-teal-100'

export function LoadingState() {
  return <p className="py-8 text-sm text-slate-500">Cargando información académica…</p>
}

export function EmptyState({ children }) {
  return <p className="rounded-xl border border-dashed border-slate-300 px-4 py-8 text-center text-sm text-slate-500">{children}</p>
}

export function formatGrade(grade) {
  return grade == null ? 'Pendiente' : Number(grade).toFixed(1).replace('.', ',')
}
