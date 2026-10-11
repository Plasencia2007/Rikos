import { useEffect, useState } from 'react'
import { Aviso, Boton, Campo, EstadoVacio, Modal } from '../../comun/componentes'
import { buscarPensionistas, type ResumenPensionista } from '../../api/cliente'

/**
 * El mozo busca al pensionista por nombre o DNI y lo asigna a la persona. Paso provisional hasta el ticket del
 * kiosco (sprint 8). Un pensionista desactivado se ve pero no se puede elegir.
 */
export function DialogoPensionista({
  token,
  trabajando,
  onElegir,
  onCerrar,
}: {
  token: string
  trabajando: boolean
  onElegir: (pensionista: ResumenPensionista) => void
  onCerrar: () => void
}) {
  const [q, setQ] = useState('')
  const [resultados, setResultados] = useState<ResumenPensionista[] | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    const texto = q.trim()
    if (texto.length < 2) {
      setResultados(null)
      return
    }
    const espera = window.setTimeout(() => {
      setError('')
      buscarPensionistas(token, texto)
        .then(setResultados)
        .catch(() => setError('No se pudo buscar. Revisa la conexión e inténtalo otra vez.'))
    }, 300)
    return () => window.clearTimeout(espera)
  }, [q, token])

  return (
    <Modal
      abierto
      titulo="Pensionista"
      onCerrar={onCerrar}
      pie={
        <Boton variante="secundario" onClick={onCerrar}>
          Cerrar
        </Boton>
      }
    >
      <div className="ped-dialogo">
        <div className="ped2-pestanas" role="tablist" aria-label="Cómo identificar al pensionista">
          <button type="button" role="tab" aria-selected="true" className="ped2-pestana ped2-pestana-activa">
            Por DNI o nombre
          </button>
          {/* Sprint 8 (US-019/US-020): aqui se tecleara el CODIGO DEL PRE-TICKET (un codigo se usa una vez; vencido o
              anulado se rechaza). Hasta entonces se busca por DNI o nombre. */}
          <button type="button" role="tab" aria-selected="false" className="ped2-pestana" disabled title="Llega en el sprint 8">
            Código del ticket <span className="cta-nota">· Sprint 8</span>
          </button>
        </div>
        <Campo
          etiqueta="Nombre o DNI"
          ayuda="Escribe al menos 2 letras o números."
          value={q}
          autoFocus
          onChange={(e) => setQ(e.target.value)}
        />
        {error && <Aviso tono="error">{error}</Aviso>}
        {resultados && resultados.length === 0 && (
          <EstadoVacio titulo="No se encontró a nadie" descripcion="Prueba con otro nombre o con el DNI." />
        )}
        <ul className="ped-lista-carta">
          {(resultados ?? []).map((p) => (
            <li key={p.id}>
              <button type="button" className="ped-producto" disabled={!p.activo || trabajando} onClick={() => onElegir(p)}>
                <span>
                  {p.apellidos}, {p.nombres} <span className="cta-nota">DNI {p.dni}</span>
                </span>
                <span className="cta-nota">{p.activo ? (p.planNombre ?? '') : 'Desactivado'}</span>
              </button>
            </li>
          ))}
        </ul>
      </div>
    </Modal>
  )
}
