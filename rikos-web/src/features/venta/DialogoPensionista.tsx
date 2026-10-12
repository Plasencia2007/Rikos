import { useEffect, useState } from 'react'
import { Aviso, Boton, Campo, EstadoVacio, Modal, TecladoNumerico } from '../../comun/componentes'
import { buscarPensionistas, type ResumenPensionista } from '../../api/cliente'

interface Props {
  token: string
  trabajando: boolean
  onVincularTicket: (codigo: string) => Promise<void> | void
  onElegir: (pensionista: ResumenPensionista) => void
  onCerrar: () => void
}

/**
 * US-020: El mozo vincula el pre-ticket impreso de 6 dígitos emitido en el kiosco.
 * Incluye pestaña de contingencia para buscar por DNI o nombre si la impresora del kiosco falló.
 */
export function DialogoPensionista({
  token,
  trabajando,
  onVincularTicket,
  onElegir,
  onCerrar,
}: Props) {
  const [pestana, setPestana] = useState<'ticket' | 'dni'>('ticket')
  const [codigoTicket, setCodigoTicket] = useState('')
  const [errorTicket, setErrorTicket] = useState('')
  const [enviandoTicket, setEnviandoTicket] = useState(false)

  // Búsqueda por DNI o nombre (Contingencia)
  const [q, setQ] = useState('')
  const [resultados, setResultados] = useState<ResumenPensionista[] | null>(null)
  const [errorDni, setErrorDni] = useState('')

  useEffect(() => {
    if (pestana !== 'dni') return
    const texto = q.trim()
    if (texto.length < 2) {
      setResultados(null)
      return
    }
    const espera = window.setTimeout(() => {
      setErrorDni('')
      buscarPensionistas(token, texto)
        .then(setResultados)
        .catch(() => setErrorDni('No se pudo buscar. Revisa la conexión e inténtalo otra vez.'))
    }, 300)
    return () => window.clearTimeout(espera)
  }, [q, token, pestana])

  const manejarDigito = (d: string) => {
    if (codigoTicket.length < 6) {
      setErrorTicket('')
      setCodigoTicket((prev) => prev + d)
    }
  }

  const manejarBorrar = () => {
    setErrorTicket('')
    setCodigoTicket((prev) => prev.slice(0, -1))
  }

  const manejarLimpiar = () => {
    setErrorTicket('')
    setCodigoTicket('')
  }

  const manejarVincular = async () => {
    if (codigoTicket.length !== 6 || trabajando || enviandoTicket) return
    setErrorTicket('')
    setEnviandoTicket(true)
    try {
      await onVincularTicket(codigoTicket)
    } catch (e: any) {
      const err = e?.response?.error || e?.message || ''
      if (err.includes('PRE_TICKET_NO_ENCONTRADO')) {
        setErrorTicket('No existe ningún pre-ticket con ese código.')
      } else if (err.includes('PRE_TICKET_DE_OTRO_LOCAL')) {
        setErrorTicket('El pre-ticket pertenece a otro local.')
      } else if (err.includes('PRE_TICKET_SERVICIO_DISTINTO')) {
        setErrorTicket('El servicio del pre-ticket no coincide con el turno de la mesa.')
      } else if (err.includes('PRE_TICKET_VENCIDO')) {
        setErrorTicket('El pre-ticket ha vencido al finalizar el horario de atención.')
      } else if (err.includes('PRE_TICKET_YA_VINCULADO')) {
        setErrorTicket('Este pre-ticket ya fue vinculado a otro pedido o mesa.')
      } else if (err.includes('PRE_TICKET_ANULADO')) {
        setErrorTicket('Este pre-ticket ha sido anulado.')
      } else if (err.includes('PRE_TICKET_CERRADO')) {
        setErrorTicket('Este pre-ticket ya fue consumido y cerrado en caja.')
      } else if (err.includes('PENSIONISTA_INACTIVO')) {
        setErrorTicket('La cuenta del pensionista está desactivada.')
      } else if (err.includes('PENSIONISTA_YA_EN_EL_PEDIDO')) {
        setErrorTicket('Este pensionista ya está asignado a otra persona en esta mesa.')
      } else if (err.includes('PERSONA_CON_LINEAS')) {
        setErrorTicket('No se puede asignar un pensionista a una persona con platos ya agregados.')
      } else {
        setErrorTicket('No se pudo vincular el pre-ticket. Verifica el código e intenta otra vez.')
      }
    } finally {
      setEnviandoTicket(false)
    }
  }

  return (
    <Modal
      abierto
      titulo="Asignar Pensionista"
      onCerrar={onCerrar}
      pie={
        <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end', width: '100%' }}>
          <Boton variante="secundario" onClick={onCerrar}>
            Cerrar
          </Boton>
          {pestana === 'ticket' && (
            <Boton
              variante="primario"
              disabled={codigoTicket.length !== 6 || trabajando || enviandoTicket}
              onClick={manejarVincular}
              id="btn-vincular-preticket"
            >
              {enviandoTicket || trabajando ? 'Vinculando...' : 'Vincular Ticket'}
            </Boton>
          )}
        </div>
      }
    >
      <div className="ped-dialogo">
        <div className="ped2-pestanas" role="tablist" aria-label="Cómo identificar al pensionista">
          <button
            type="button"
            role="tab"
            aria-selected={pestana === 'ticket'}
            className={`ped2-pestana ${pestana === 'ticket' ? 'ped2-pestana-activa' : ''}`}
            onClick={() => setPestana('ticket')}
            id="tab-codigo-ticket"
          >
            Código del ticket
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={pestana === 'dni'}
            className={`ped2-pestana ${pestana === 'dni' ? 'ped2-pestana-activa' : ''}`}
            onClick={() => setPestana('dni')}
            id="tab-por-dni"
          >
            Por DNI o nombre <span className="cta-nota">· Contingencia</span>
          </button>
        </div>

        {pestana === 'ticket' && (
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '16px', padding: '8px 0' }}>
            <p style={{ margin: 0, fontSize: '0.9rem', color: 'var(--texto-secundario, #666)', textAlign: 'center' }}>
              Digita los 6 dígitos del pre-ticket impreso emitido por el kiosco:
            </p>

            <div
              id="visor-codigo-preticket"
              style={{
                fontFamily: 'monospace',
                fontSize: '2.4rem',
                fontWeight: 700,
                letterSpacing: '0.3em',
                padding: '12px 24px',
                background: 'var(--superficie-resaltada, #f0f4f8)',
                border: '2px solid var(--borde, #cbd5e1)',
                borderRadius: '12px',
                minWidth: '220px',
                textAlign: 'center',
                color: codigoTicket.length > 0 ? 'var(--texto-principal, #1e293b)' : 'var(--texto-desactivado, #94a3b8)',
              }}
            >
              {codigoTicket.length > 0
                ? codigoTicket.padEnd(6, '·').replace(/(\d{3})(.)/, '$1 $2')
                : '··· ···'}
            </div>

            {errorTicket && <Aviso tono="error">{errorTicket}</Aviso>}

            <div style={{ width: '100%', maxWidth: '280px' }}>
              <TecladoNumerico
                onDigito={manejarDigito}
                onBorrar={manejarBorrar}
                onLimpiar={manejarLimpiar}
                deshabilitado={trabajando || enviandoTicket}
              />
            </div>
          </div>
        )}

        {pestana === 'dni' && (
          <>
            <Campo
              etiqueta="Nombre o DNI"
              ayuda="Escribe al menos 2 letras o números."
              value={q}
              autoFocus
              onChange={(e) => setQ(e.target.value)}
            />
            {errorDni && <Aviso tono="error">{errorDni}</Aviso>}
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
          </>
        )}
      </div>
    </Modal>
  )
}

