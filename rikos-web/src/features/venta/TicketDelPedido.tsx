import { useEffect } from 'react'
import { Printer } from 'lucide-react'
import { Boton, Modal } from '../../comun/componentes'
import { enSoles } from '../../comun/formato/dinero'
import type { ComandaDetalle, Pedido } from '../../api/cliente'
import { nombreDeLinea } from './OrdenDeLaPersona'

/** Cómo se llama cada parte del menú en el ticket (el refresco, para cocina, es la bebida). */
const PARTE: Record<string, string> = {
  entrada: 'Entrada',
  segundo: 'Segundo',
  guarnicion: 'Guarnición',
  refresco: 'Bebida',
  postre: 'Postre',
}

const fechaYHora = (fecha: Date) =>
  `${fecha.toLocaleDateString('es-PE', { day: '2-digit', month: '2-digit', year: 'numeric' })} ${fecha.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' })}`

const sinSimbolo = (centimos: number) => enSoles(centimos).replace(/^S\/\s*/, '')

/**
 * Pre-cuenta (adenda 2026-10-29): la cuenta de la mesa en un ticket de 80 mm para que el cliente revise antes de pagar.
 * No es comprobante de pago y no cambia el pedido. Sale del pedido tal como llega del servidor; lo anulado no sale.
 */
export function TicketPrecuenta({ pedido, titulo, local }: { pedido: Pedido; titulo: string; local: string }) {
  const lineas = pedido.personas.flatMap((p) => p.lineas.filter((l) => l.estado !== 'anulado'))
  return (
    <div className="ticket" aria-label="Pre-cuenta">
      <div className="ticket-centro">
        <strong className="ticket-marca">RIKO'S</strong>
        {local && <span>{local}</span>}
      </div>
      <hr className="ticket-doble" />
      <div className="ticket-centro">
        <strong>PRE-CUENTA</strong>
        <span>** NO ES COMPROBANTE DE PAGO **</span>
      </div>
      <hr />
      <p>PEDIDO: #{pedido.numero}</p>
      <p>{pedido.canal === 'mesa' ? `MESA: ${titulo.replace(/^Mesa\s*/, '')}` : 'PARA LLEVAR'}</p>
      {pedido.mozoNombre && <p>MOZO: {pedido.mozoNombre}</p>}
      <p>FECHA: {fechaYHora(new Date())}</p>
      <hr />
      <p className="ticket-fila">
        <span>CANT. PRODUCTO</span>
        <span>IMPORTE</span>
      </p>
      <hr />
      {lineas.map((l) => (
        <div key={l.id} className="ticket-linea">
          <p className="ticket-fila">
            <strong>
              {l.cantidad}x {nombreDeLinea(l)}
            </strong>
            <strong>{sinSimbolo(l.precioUnitario * l.cantidad)}</strong>
          </p>
          {l.componentes.map((c) => (
            <p key={c.componente} className="ticket-sub">
              - {c.productoNombre}
            </p>
          ))}
          {l.observacion && <p className="ticket-sub">- Nota: {l.observacion}</p>}
        </div>
      ))}
      <hr />
      {pedido.taper > 0 && (
        <p className="ticket-fila">
          <span>TÁPER</span>
          <span>{sinSimbolo(pedido.taper)}</span>
        </p>
      )}
      <p className="ticket-fila ticket-total">
        <strong>TOTAL</strong>
        <strong>{enSoles(pedido.total)}</strong>
      </p>
      <hr />
      <div className="ticket-centro">
        <strong>GRACIAS POR SU PREFERENCIA</strong>
      </div>
    </div>
  )
}

/** La comanda en un ticket de 80 mm para cocina: por persona, con las opciones del menú etiquetadas y las notas. */
export function TicketComanda({ comanda, copia }: { comanda: ComandaDetalle; copia: boolean }) {
  return (
    <div className="ticket" aria-label={`Comanda ${comanda.numero}`}>
      {copia && <p className="ticket-copia">COPIA</p>}
      <div className="ticket-centro">
        <strong>COMANDA #{String(comanda.numero).padStart(3, '0')}</strong>
        <span>Pedido #{comanda.pedidoNumero}</span>
      </div>
      <p className="ticket-fila">
        <span>Mesa</span>
        <span>{comanda.mesa ?? 'Para llevar'}</span>
      </p>
      {comanda.mozo && (
        <p className="ticket-fila">
          <span>Mozo</span>
          <span>{comanda.mozo}</span>
        </p>
      )}
      <p className="ticket-fila">
        <span>Hora</span>
        <span>{fechaYHora(new Date(comanda.emitidaEn))}</span>
      </p>
      <hr />
      {comanda.personas.map((p) => (
        <div key={p.etiqueta} className="ticket-persona">
          <p className="ticket-etiqueta">{p.etiqueta}</p>
          {p.lineas.map((l, i) => (
            <div key={i} className="ticket-linea">
              <p>
                <strong>
                  {l.cantidad}x {l.nombre}
                </strong>
              </p>
              {(l.partes && l.partes.length > 0
                ? l.partes.map((c) => `${PARTE[c.componente] ?? c.componente}: ${c.productoNombre ?? ''}`)
                : l.componentes
              ).map((texto) => (
                <p key={texto} className="ticket-sub">
                  - {texto}
                </p>
              ))}
              {l.observacion && <p className="ticket-sub ticket-nota">&gt;&gt; {l.observacion}</p>}
            </div>
          ))}
        </div>
      ))}
      <hr />
    </div>
  )
}

/**
 * El diálogo que muestra un ticket. Con imprimirAlAbrir manda a imprimir apenas aparece (los botones sin ojo); sin él
 * es solo para mirar, con "Imprimir" por si se decide ahí. onImprimir puede preparar algo antes (registrar la
 * impresión de la comanda); el estilo de impresión deja solo el ticket (estilos/pos.css).
 */
export function DialogoTicket({
  titulo,
  imprimirAlAbrir,
  onImprimir,
  onCerrar,
  children,
}: {
  titulo: string
  imprimirAlAbrir: boolean
  onImprimir?: () => Promise<void> | void
  onCerrar: () => void
  children: React.ReactNode
}) {
  useEffect(() => {
    if (!imprimirAlAbrir) return
    const t = window.setTimeout(() => window.print(), 150)
    return () => window.clearTimeout(t)
  }, [imprimirAlAbrir])

  async function imprimir() {
    await onImprimir?.()
    window.setTimeout(() => window.print(), 100)
  }

  return (
    <Modal
      abierto
      titulo={titulo}
      icono={<Printer size={22} strokeWidth={1.9} />}
      subtitulo={imprimirAlAbrir ? 'Enviado a la impresora' : 'Vista previa'}
      onCerrar={onCerrar}
      pie={
        <>
          <Boton variante="secundario" onClick={onCerrar}>
            Cerrar
          </Boton>
          <Boton onClick={() => void imprimir()}>
            <Printer size={18} aria-hidden="true" />
            Imprimir
          </Boton>
        </>
      }
    >
      <div className="ticket-papel">{children}</div>
    </Modal>
  )
}
