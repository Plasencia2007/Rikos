import { useState } from 'react'
import { MoreVertical, UtensilsCrossed } from 'lucide-react'
import { Boton, Insignia } from '../../comun/componentes'
import { enSoles } from '../../comun/formato/dinero'
import type { LineaDelPedido, Pedido, PersonaDelPedido } from '../../api/cliente'

const SERVICIO: Record<string, string> = { desayuno: 'Desayuno', almuerzo: 'Almuerzo', cena: 'Cena' }

export function nombreDeLinea(l: LineaDelPedido): string {
  return l.productoNombre ?? l.descripcion ?? 'Producto'
}

/** Una comida del plan o un menu de un pensionista se pide de a una por linea. */
function cantidadFija(l: LineaDelPedido, persona: PersonaDelPedido): boolean {
  return (
    persona.tipoCliente === 'pensionista' && ['menu_normal', 'menu_extra', 'plan_desayuno', 'plan_cena'].includes(l.tipoLinea)
  )
}

/** "P1" de "Persona 1": las dos primeras letras utiles para el circulo de la cabecera. */
export function siglaDePersona(etiqueta: string): string {
  const numero = etiqueta.match(/\d+/)?.[0]
  return numero ? `P${numero}` : etiqueta.slice(0, 2).toUpperCase()
}

/**
 * El panel derecho de la pantalla del pedido: la ORDEN de la persona elegida (diseno aprobado). No calcula nada, pinta lo
 * que llega del servidor. Cada linea tiene su menu "⋮" (editar, quitar o anular, pasar a otra persona); para mover VARIAS
 * lineas a la vez esta "Seleccionar varias" en la cabecera, que muestra casillas y una barra "Pasar N lineas a…".
 */
export function OrdenDeLaPersona({
  pedido,
  persona,
  nombrePensionista,
  cerrado,
  puedeAnular,
  puedeReorganizar,
  trabajando,
  itemsNuevos,
  seleccionando,
  marcadas,
  onMarcar,
  onPasarMarcadas,
  onCantidad,
  onQuitar,
  onNota,
  onAnular,
  onPasarLinea,
  onIdentificar,
  onTerminar,
  onComandar,
}: {
  pedido: Pedido
  persona: PersonaDelPedido
  nombrePensionista: string | null
  cerrado: boolean
  puedeAnular: boolean
  puedeReorganizar: boolean
  trabajando: boolean
  itemsNuevos: number
  seleccionando: boolean
  marcadas: string[]
  onSeleccionar: (activo: boolean) => void
  onMarcar: (lineaId: string, marcada: boolean) => void
  onPasarMarcadas: () => void
  onCantidad: (linea: LineaDelPedido, cantidad: number) => void
  onQuitar: (linea: LineaDelPedido) => void
  onNota: (linea: LineaDelPedido) => void
  onAnular: (linea: LineaDelPedido) => void
  onPasarLinea: (linea: LineaDelPedido) => void
  onIdentificar: () => void
  onTerminar: () => void
  onComandar: () => void
}) {
  const [menuAbierto, setMenuAbierto] = useState<string | null>(null)
  const esPensionista = persona.tipoCliente === 'pensionista'
  const sinLineas = persona.lineas.length === 0
  const vigentes = persona.lineas.filter((l) => l.estado !== 'anulado')
  const nombreDeLaOrden = esPensionista && nombrePensionista ? `Orden de ${nombrePensionista}` : `Orden · ${persona.etiqueta}`

  return (
    <aside className="ped2-orden ped3-orden" aria-label={nombreDeLaOrden}>
      <header className="ped3-orden-cabecera">
        <span className="ped3-circulo" aria-hidden="true">
          {siglaDePersona(persona.etiqueta)}
        </span>
        <div className="ped3-orden-titulo">
          <h2>{nombreDeLaOrden}</h2>
          <span className="cta-nota">
            Pedido {pedido.numero} · {SERVICIO[pedido.servicio]}
          </span>
        </div>
        <div className="ped3-orden-cuenta">
          <strong>
            {vigentes.length} {vigentes.length === 1 ? 'ítem' : 'ítems'}
          </strong>
        </div>
      </header>

      <div className={`ped3-pensionista ${esPensionista ? 'ped3-pensionista-verificado' : ''}`}>
        {esPensionista ? (
          <>
            <div>
              <strong>{nombrePensionista ?? 'Pensionista'}</strong>
              <span className="cta-nota">Pensionista verificado — DNI validado</span>
            </div>
            {!cerrado && sinLineas && (
              <button type="button" className="ped3-enlace" onClick={onIdentificar}>
                Cambiar
              </button>
            )}
          </>
        ) : (
          <>
            <div>
              <strong>{persona.etiqueta}</strong>
              <span className="cta-nota">Sin identificar</span>
            </div>
            {!cerrado && sinLineas && (
              <Boton variante="secundario" onClick={onIdentificar}>
                Identificar pensionista
              </Boton>
            )}
          </>
        )}
      </div>

      {seleccionando && (
        <div className="ped-seleccion" role="status">
          <span>
            {marcadas.length} {marcadas.length === 1 ? 'línea' : 'líneas'}
          </span>
          <Boton disabled={marcadas.length === 0} onClick={onPasarMarcadas}>
            {marcadas.length > 0 ? `Pasar ${marcadas.length} ${marcadas.length === 1 ? 'línea' : 'líneas'} a…` : 'Pasar a…'}
          </Boton>
        </div>
      )}

      <ul className="ped2-lineas ped3-lineas" aria-label={`Productos de ${persona.etiqueta}`}>
        {sinLineas && <li className="ped-vacio">Todavía no hay nada para {persona.etiqueta}. Agrega un producto del catálogo.</li>}
        {persona.lineas.map((l) => {
          const anulada = l.estado === 'anulado'
          const editable = !cerrado && l.estado === 'pendiente'
          const puedeAnularla = !cerrado && l.estado === 'comandado' && puedeAnular
          const puedePasarla = !cerrado && puedeReorganizar && !anulada
          const hayMenu = editable || puedeAnularla || puedePasarla
          return (
            <li key={l.id} className={`ped3-linea ${anulada ? 'ped-linea-anulada' : ''}`}>
              {seleccionando && !anulada && (
                <label className="ped-marca">
                  <input
                    type="checkbox"
                    checked={marcadas.includes(l.id)}
                    aria-label={`Marcar ${nombreDeLinea(l)}`}
                    onChange={(e) => onMarcar(l.id, e.target.checked)}
                  />
                </label>
              )}
              <div className="ped3-miniatura" aria-hidden="true">
                <UtensilsCrossed size={18} />
              </div>
              <div className="ped3-linea-info">
                <strong>{nombreDeLinea(l)}</strong>
                <span className="ped3-subtitulo">
                  {l.estado === 'pendiente' && <Insignia tono="pendiente">Por enviar</Insignia>}
                  {l.estado === 'comandado' && <Insignia tono="activo">En cocina</Insignia>}
                  {anulada && <Insignia tono="inactivo">Anulado</Insignia>}
                </span>
                {anulada && (
                  <span className="cta-nota">
                    {l.anuladaPostComanda ? 'Anulado después de comandar' : 'Anulado antes de comandar'}
                    {l.motivoAnulacion ? ` · ${l.motivoAnulacion}` : ''}
                  </span>
                )}
                {l.componentes.length > 0 && (
                  <span className="cta-nota">{l.componentes.map((c) => c.productoNombre).join(' · ')}</span>
                )}
                {l.observacion && <span className="cta-nota">Nota: {l.observacion}</span>}
                {l.tarifaOrigen === 'plan' && <span className="cta-nota">Precio de su plan</span>}
                {l.segundoDelDia && <span className="cta-nota">Segundo del día</span>}
                {l.tarifaOrigen === 'manual' && <span className="cta-nota">Precio puesto a mano</span>}
                {l.taperUnitario > 0 && <span className="cta-nota">Táper {enSoles(l.taperUnitario)}</span>}
                {editable && (
                  <button type="button" className="ped3-enlace ped3-enlace-nota" onClick={() => onNota(l)}>
                    {l.observacion ? 'Editar nota' : 'Agregar nota'}
                  </button>
                )}
              </div>
              <div className="ped3-linea-lado">
                {hayMenu && (
                  <div className="ped3-menu">
                    <button
                      type="button"
                      className="ped3-icono"
                      aria-label={`Más acciones de ${nombreDeLinea(l)}`}
                      aria-expanded={menuAbierto === l.id}
                      onClick={() => setMenuAbierto(menuAbierto === l.id ? null : l.id)}
                    >
                      <MoreVertical size={18} aria-hidden="true" />
                    </button>
                    {menuAbierto === l.id && (
                      <ul className="ped3-menu-lista" role="menu">
                        {editable && (
                          <li>
                            <button
                              type="button"
                              role="menuitem"
                              onClick={() => {
                                setMenuAbierto(null)
                                onNota(l)
                              }}
                            >
                              Editar
                            </button>
                          </li>
                        )}
                        {puedePasarla && (
                          <li>
                            <button
                              type="button"
                              role="menuitem"
                              onClick={() => {
                                setMenuAbierto(null)
                                onPasarLinea(l)
                              }}
                            >
                              Pasar a otra persona…
                            </button>
                          </li>
                        )}
                        {editable && (
                          <li>
                            <button
                              type="button"
                              role="menuitem"
                              disabled={trabajando}
                              onClick={() => {
                                setMenuAbierto(null)
                                onQuitar(l)
                              }}
                            >
                              Quitar
                            </button>
                          </li>
                        )}
                        {puedeAnularla && (
                          <li>
                            <button
                              type="button"
                              role="menuitem"
                              disabled={trabajando}
                              onClick={() => {
                                setMenuAbierto(null)
                                onAnular(l)
                              }}
                            >
                              Anular
                            </button>
                          </li>
                        )}
                      </ul>
                    )}
                  </div>
                )}
                <strong className="ped-linea-total">{enSoles(l.precioUnitario * l.cantidad)}</strong>
                <div className="ped-linea-cantidad">
                  {editable && !cantidadFija(l, persona) ? (
                    <>
                      <button
                        type="button"
                        className="ped-paso"
                        aria-label="Uno menos"
                        disabled={trabajando || l.cantidad <= 1}
                        onClick={() => onCantidad(l, l.cantidad - 1)}
                      >
                        −
                      </button>
                      <span aria-label="Cantidad">{l.cantidad}</span>
                      <button
                        type="button"
                        className="ped-paso"
                        aria-label="Uno más"
                        disabled={trabajando}
                        onClick={() => onCantidad(l, l.cantidad + 1)}
                      >
                        +
                      </button>
                    </>
                  ) : (
                    <span aria-label="Cantidad">× {l.cantidad}</span>
                  )}
                </div>
              </div>
            </li>
          )
        })}
      </ul>

      <footer className="ped2-orden-pie">
        <dl className="ped2-totales">
          <div>
            <dt>Subtotal</dt>
            <dd>{enSoles(pedido.subtotal)}</dd>
          </div>
          {pedido.taper > 0 && (
            <div>
              <dt>Táper</dt>
              <dd>{enSoles(pedido.taper)}</dd>
            </div>
          )}
          <div className="ped-total">
            <dt>Total</dt>
            <dd aria-label="Total del pedido">{enSoles(pedido.total)}</dd>
          </div>
        </dl>
        {!cerrado && puedeReorganizar && (
          <button type="button" className="ped3-enlace ped3-terminar" onClick={onTerminar}>
            Terminar pedido
          </button>
        )}
        {!cerrado && (
          <Boton
            className="ped2-comandar"
            cargando={trabajando}
            disabled={itemsNuevos === 0}
            title={itemsNuevos === 0 ? 'No hay nada nuevo para mandar a cocina' : undefined}
            onClick={onComandar}
          >
            {itemsNuevos === 0 ? 'Comandar' : `Comandar (${itemsNuevos} ${itemsNuevos === 1 ? 'ítem nuevo' : 'ítems nuevos'})`}
          </Boton>
        )}
      </footer>
    </aside>
  )
}
