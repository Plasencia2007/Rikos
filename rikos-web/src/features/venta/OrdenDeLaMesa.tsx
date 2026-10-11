import { ArrowLeftRight, Ban, Send, StickyNote, Trash2, Utensils, UtensilsCrossed, X } from 'lucide-react'
import { Boton, Insignia } from '../../comun/componentes'
import { enSoles } from '../../comun/formato/dinero'
import type { LineaDelPedido, Pedido, PersonaDelPedido } from '../../api/cliente'
import { nombreDeLinea, siglaDePersona } from './OrdenDeLaPersona'

const SERVICIO: Record<string, string> = { desayuno: 'Desayuno', almuerzo: 'Almuerzo', cena: 'Cena' }

/** Una comida del plan o un menu de un pensionista se pide de a una por linea. */
function cantidadFija(l: LineaDelPedido, persona: PersonaDelPedido): boolean {
  return (
    persona.tipoCliente === 'pensionista' &&
    ['menu_normal', 'menu_extra', 'plan_desayuno', 'plan_cena'].includes(l.tipoLinea)
  )
}

/**
 * La ORDEN de toda la mesa (diseno aprobado 2026-10-01): todas las lineas de todas las personas en una sola lista,
 * cada una con su etiqueta (P1, P2…). Las personas solo separan: las pestañas "Mesa completa / Persona N" filtran la
 * lista, y con una persona elegida aparece su bloque de pensionista. Un solo "Comandar" manda lo nuevo de TODA la mesa.
 * En escritorio es el panel derecho; en celular es un cajon que sale del costado (lo abre el carrito flotante).
 * No calcula nada: pinta lo que llega del servidor.
 */
export function OrdenDeLaMesa({
  pedido,
  filtroId,
  nombresDePensionista,
  fotos,
  cerrado,
  puedeAnular,
  puedeReorganizar,
  trabajando,
  itemsNuevos,
  abierta,
  onCerrar,
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
  /** La persona elegida arriba: solo para su bloque de pensionista; la lista siempre es de toda la mesa. */
  filtroId: string | null
  nombresDePensionista: Record<string, string>
  /** Foto de cada producto de la carta, por id de producto (para la miniatura de cada linea). */
  fotos: Record<string, string>
  cerrado: boolean
  puedeAnular: boolean
  puedeReorganizar: boolean
  trabajando: boolean
  itemsNuevos: number
  /** Solo celular: el cajon esta abierto. */
  abierta: boolean
  onCerrar: () => void
  onCantidad: (linea: LineaDelPedido, cantidad: number) => void
  onQuitar: (linea: LineaDelPedido) => void
  onNota: (linea: LineaDelPedido) => void
  onAnular: (linea: LineaDelPedido) => void
  onPasarLinea: (linea: LineaDelPedido) => void
  onIdentificar: (personaId: string) => void
  onTerminar: () => void
  onComandar: () => void
}) {
  const filtrada = pedido.personas.find((p) => p.id === filtroId) ?? null
  const visibles = pedido.personas.flatMap((p) => p.lineas.map((l) => ({ l, p })))
  const vigentes = visibles.filter(({ l }) => l.estado !== 'anulado').length
  const totalVigentes = pedido.personas.reduce((n, p) => n + p.lineas.filter((l) => l.estado !== 'anulado').length, 0)
  const nombreDelPensionista = filtrada?.pensionistaId ? (nombresDePensionista[filtrada.pensionistaId] ?? null) : null

  return (
    <aside
      className={`ped2-orden ped3-orden ped4-orden ${abierta ? 'ped4-abierta' : ''}`}
      aria-label="Orden de la mesa"
    >
      <header className="ped3-orden-cabecera">
        <span className="ped3-circulo" aria-hidden="true">
          <Utensils size={20} />
        </span>
        <div className="ped3-orden-titulo">
          <h2>Orden de la mesa</h2>
          <span className="cta-nota">
            Pedido {pedido.numero} · {SERVICIO[pedido.servicio]} · {pedido.personas.length}{' '}
            {pedido.personas.length === 1 ? 'persona' : 'personas'}
          </span>
        </div>
        <div className="ped3-orden-cuenta">
          <strong>
            {vigentes} {vigentes === 1 ? 'ítem' : 'ítems'}
          </strong>
        </div>
        <button type="button" className="ped4-cerrar" onClick={onCerrar}>
          <X size={16} aria-hidden="true" />
          Cerrar
        </button>
      </header>

      {filtrada && (
        <div
          className={`ped3-pensionista ${filtrada.tipoCliente === 'pensionista' ? 'ped3-pensionista-verificado' : ''}`}
        >
          {filtrada.tipoCliente === 'pensionista' ? (
            <>
              <div>
                <strong>{nombreDelPensionista ?? 'Pensionista'}</strong>
                <span className="cta-nota">Pensionista verificado — DNI validado</span>
              </div>
              {!cerrado && filtrada.lineas.length === 0 && (
                <button type="button" className="ped3-enlace" onClick={() => onIdentificar(filtrada.id)}>
                  Cambiar
                </button>
              )}
            </>
          ) : (
            <>
              <div>
                <strong>{filtrada.etiqueta}</strong>
                <span className="cta-nota">Sin identificar</span>
              </div>
              {!cerrado && filtrada.lineas.length === 0 && (
                <Boton variante="secundario" onClick={() => onIdentificar(filtrada.id)}>
                  Identificar pensionista
                </Boton>
              )}
            </>
          )}
        </div>
      )}

      <ul
        className="ped2-lineas ped3-lineas"
        aria-label={filtrada ? `Productos de ${filtrada.etiqueta}` : 'Productos de la mesa'}
      >
        {visibles.length === 0 && (
          <li className="ped-vacio">
            {filtrada
              ? `Todavía no hay nada para ${filtrada.etiqueta}. Agrega un producto del catálogo.`
              : 'Todavía no hay nada en la mesa. Elige una persona y agrega un producto del catálogo.'}
          </li>
        )}
        {visibles.map(({ l, p }) => {
          const anulada = l.estado === 'anulado'
          const editable = !cerrado && l.estado === 'pendiente'
          const puedeAnularla = !cerrado && l.estado === 'comandado' && puedeAnular
          const puedePasarla = !cerrado && puedeReorganizar && !anulada
          return (
            <li key={l.id} className={`ped3-linea ${anulada ? 'ped-linea-anulada' : ''}`}>
              <div className="ped3-miniatura" aria-hidden="true">
                {l.productoId && fotos[l.productoId] ? (
                  <img src={fotos[l.productoId]} alt="" />
                ) : (
                  <UtensilsCrossed size={18} />
                )}
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
              </div>
              <div className="ped3-linea-lado">
                <div className="ped4-lado-arriba">
                  <span className="ped4-etiqueta" title={p.etiqueta}>
                    {siglaDePersona(p.etiqueta)}
                  </span>
                  {editable && (
                    <button
                      type="button"
                      className="ped4-accion"
                      title={l.observacion ? 'Editar la nota' : 'Agregar una nota'}
                      aria-label={`${l.observacion ? 'Editar la nota de' : 'Agregar una nota a'} ${nombreDeLinea(l)}`}
                      onClick={() => onNota(l)}
                    >
                      <StickyNote size={18} aria-hidden="true" />
                    </button>
                  )}
                  {puedePasarla && (
                    <button
                      type="button"
                      className="ped4-accion"
                      title="Pasar a otra persona"
                      aria-label={`Pasar ${nombreDeLinea(l)} a otra persona`}
                      onClick={() => onPasarLinea(l)}
                    >
                      <ArrowLeftRight size={18} aria-hidden="true" />
                    </button>
                  )}
                  {editable && (
                    <button
                      type="button"
                      className="ped4-accion ped4-accion-peligro"
                      title="Quitar"
                      aria-label={`Quitar ${nombreDeLinea(l)}`}
                      disabled={trabajando}
                      onClick={() => onQuitar(l)}
                    >
                      <Trash2 size={18} aria-hidden="true" />
                    </button>
                  )}
                  {puedeAnularla && (
                    <button
                      type="button"
                      className="ped4-accion ped4-accion-peligro"
                      title="Anular"
                      aria-label={`Anular ${nombreDeLinea(l)}`}
                      disabled={trabajando}
                      onClick={() => onAnular(l)}
                    >
                      <Ban size={18} aria-hidden="true" />
                    </button>
                  )}
                </div>
                <strong className="ped-linea-total">{enSoles(l.precioUnitario * l.cantidad)}</strong>
                <div className="ped-linea-cantidad">
                  {editable && !cantidadFija(l, p) ? (
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
            <dt>
              Subtotal · {totalVigentes} {totalVigentes === 1 ? 'ítem' : 'ítems'}
            </dt>
            <dd>{enSoles(pedido.subtotal)}</dd>
          </div>
          {pedido.taper > 0 && (
            <div>
              <dt>Táper</dt>
              <dd>{enSoles(pedido.taper)}</dd>
            </div>
          )}
          <div className="ped-total">
            <dt>Total de la mesa</dt>
            <dd aria-label="Total del pedido">{enSoles(pedido.total)}</dd>
          </div>
        </dl>
        {!cerrado && <p className="ped4-aviso-comanda">Se enviarán los productos nuevos de todas las personas</p>}
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
            <Send size={18} aria-hidden="true" />
            {itemsNuevos === 0
              ? 'Comandar'
              : `Comandar ${itemsNuevos} ${itemsNuevos === 1 ? 'ítem nuevo' : 'ítems nuevos'}`}
          </Boton>
        )}
      </footer>
    </aside>
  )
}
