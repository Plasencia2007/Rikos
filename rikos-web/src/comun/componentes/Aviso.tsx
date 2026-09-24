type Tono = 'exito' | 'aviso' | 'error' | 'informacion'

interface Props {
  tono?: Tono
  children: React.ReactNode
}

/**
 * Mensaje NO bloqueante. Visualmente distinto de un error: por defecto es
 * "aviso" (ambar), no rojo -- el sistema avisa pero deja continuar (limite
 * de credito del pensionista, stock negativo, segundo repetido en el menu
 * al confirmar la semana). Usar tono="error" solo cuando de verdad bloquea.
 */
export function Aviso({ tono = 'aviso', children }: Props) {
  return (
    <div className={`aviso aviso-${tono}`} role={tono === 'error' ? 'alert' : 'status'}>
      {children}
    </div>
  )
}
