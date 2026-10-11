import { useState } from 'react'
import { Aviso, Boton, Modal } from '../../comun/componentes'
import type { PersonaDelPedido } from '../../api/cliente'

/**
 * US-029: a quien se pasan los productos marcados. "Persona nueva" es dividir la cuenta. Si el destino es de otro
 * tipo de cliente (normal <-> pensionista), el precio del menu se recalcula: lo decide el servidor, aqui solo se avisa.
 */
export function DialogoPasarA({
  cantidad,
  personas,
  trabajando,
  onPasar,
  onCerrar,
}: {
  cantidad: number
  /** Las personas a las que se puede pasar (sin la de origen). */
  personas: PersonaDelPedido[]
  trabajando: boolean
  /** null = persona nueva */
  onPasar: (personaId: string | null) => void
  onCerrar: () => void
}) {
  const [destino, setDestino] = useState<string | null | undefined>(undefined)

  return (
    <Modal
      abierto
      titulo={`Pasar ${cantidad} ${cantidad === 1 ? 'producto' : 'productos'} a…`}
      onCerrar={onCerrar}
      pie={
        <>
          <Boton variante="secundario" onClick={onCerrar}>
            Cancelar
          </Boton>
          <Boton cargando={trabajando} disabled={destino === undefined} onClick={() => onPasar(destino ?? null)}>
            Pasar
          </Boton>
        </>
      }
    >
      <div className="ped-dialogo">
        <ul className="ped-lista-carta">
          {personas.map((p) => (
            <li key={p.id}>
              <button
                type="button"
                className={`ped-producto ${destino === p.id ? 'ped-opcion-elegida' : ''}`}
                aria-pressed={destino === p.id}
                onClick={() => setDestino(p.id)}
              >
                <span>
                  {p.etiqueta}
                  {p.tipoCliente === 'pensionista' && <span className="cta-nota"> · pensionista</span>}
                </span>
              </button>
            </li>
          ))}
          <li>
            <button
              type="button"
              className={`ped-producto ${destino === null ? 'ped-opcion-elegida' : ''}`}
              aria-pressed={destino === null}
              onClick={() => setDestino(null)}
            >
              <span>
                <strong>Persona nueva</strong> <span className="cta-nota">· dividir la cuenta</span>
              </span>
            </button>
          </li>
        </ul>
        <Aviso tono="informacion">El precio se recalcula si cambia el tipo de cliente (normal o pensionista).</Aviso>
      </div>
    </Modal>
  )
}
