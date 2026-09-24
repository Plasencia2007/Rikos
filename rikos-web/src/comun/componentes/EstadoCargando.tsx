export function EstadoCargando({ mensaje = 'Cargando…' }: { mensaje?: string }) {
  return (
    <div className="estado-cargando" role="status" aria-live="polite">
      <span className="estado-cargando-espiral" aria-hidden="true" />
      <span>{mensaje}</span>
    </div>
  )
}
