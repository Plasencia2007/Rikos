import { ArrowLeftRight, ArrowRight, Ban, StickyNote, Trash2, User, Utensils, UtensilsCrossed, Wallet, X } from 'lucide-react'
import { Insignia } from '../../comun/componentes'
import { enSoles } from '../../comun/formato/dinero'
import type { LineaDelPedido, Pedido, PersonaDelPedido } from '../../api/cliente'
import { nombreDeLinea, siglaDePersona } from './OrdenDeLaPersona'

/** Una comida del plan o un menu de un pensionista se pide de a una por linea. */
function cantidadFija(l: LineaDelPedido, persona: PersonaDelPedido): boolean {
  return (
    persona.tipoCliente === 'pensionista' &&
    ['menu_normal', 'menu_extra', 'plan_desayuno', 'plan_cena'].includes(l.tipoLinea)
  )
}

/** Dibujo del pedido vacío: una boleta con cubiertos. */
function PedidoVacio() {
  return (
    <svg className="ped5-vacio-dibujo" viewBox="0 0 120 110" aria-hidden="true">
      <g fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round">
        <path d="M38 14 h40 a4 4 0 0 1 4 4 v62 l-6 -4 -6 4 -6 -4 -6 4 -6 -4 -6 4 -6 -4 -6 4 v-62 a4 4 0 0 1 4 -4 z" fill="#fff" />
        <path d="M46 30 h28 M46 41 h28 M46 52 h18" />
        <circle cx="80" cy="78" r="15" fill="#fff" />
        <path d="M76 71 v6 a2 2 0 0 0 4 0 v-6 M78 77 v9 M85 71 v15 M85 71 c3 2 3 7 0 8" />
        <path d="M24 34 l-7 -4 M22 48 h-8 M24 62 l-7 4 M96 34 l7 -4 M98 48 h8" />
      </g>
    </svg>
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
  titulo,
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
  onCobrar,
}: {
  pedido: Pedido
  /** El nombre de la mesa ("Mesa 04") o del pedido para llevar, para la cabecera. */
  titulo: string
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
  /** Lleva a la Caja con este pedido para cobrarlo; solo si el usuario puede cobrar (caja.crear). */
  onCobrar?: () => void
}) {
  const filtrada = pedido.personas.find((p) => p.id === filtroId) ?? null
  const visibles = pedido.personas.flatMap((p) => p.lineas.map((l) => ({ l, p })))
  const vigentes = visibles.filter(({ l }) => l.estado !== 'anulado').length
  const nombreDelPensionista = filtrada?.pensionistaId ? (nombresDePensionista[filtrada.pensionistaId] ?? null) : null

  // Hay productos pedidos si alguna linea no esta anulada.
  const hayProductos = pedido.personas.some((p) => p.lineas.some((l) => l.estado !== 'anulado'))

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
            {titulo} · Pedido #{pedido.numero}
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
          <span className="ped5-avatar" aria-hidden="true">
            <User size={22} />
          </span>
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
                <button type="button" className="ped5-identificar" onClick={() => onIdentificar(filtrada.id)}>
                  Identificar pensionista
                </button>
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
          <li className="ped5-vacio">
            <PedidoVacio />
            <strong>Tu pedido está vacío</strong>
            <span>
              {filtrada
                ? `Selecciona un producto para agregarlo a ${filtrada.etiqueta}.`
                : 'Elige una persona y selecciona un producto del catálogo.'}
            </span>
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

      <footer className="ped2-orden-pie ped5-pie">
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
            <dt>Total de la mesa</dt>
            <dd aria-label="Total del pedido">{enSoles(pedido.total)}</dd>
          </div>
        </dl>
        {!cerrado && onCobrar && pedido.personas.some((p) => p.lineas.some((l) => l.estado === 'comandado')) && (
          <button type="button" className={itemsNuevos > 0 ? 'ped5-secundario' : 'ped5-principal'} onClick={onCobrar}>
            <Wallet size={20} aria-hidden="true" />
            Cobrar
          </button>
        )}
        {/* La accion principal manda a cocina lo nuevo de TODA la mesa; sin nada nuevo queda apagada. */}
        {!cerrado && (itemsNuevos > 0 || !hayProductos) && (
          <button
            type="button"
            className="ped5-principal"
            disabled={itemsNuevos === 0 || trabajando}
            aria-busy={trabajando || undefined}
            onClick={onComandar}
          >
            {itemsNuevos > 0 ? `Comandar ${itemsNuevos} ${itemsNuevos === 1 ? 'ítem nuevo' : 'ítems nuevos'}` : 'Comandar pedido'}
            <ArrowRight size={20} aria-hidden="true" />
          </button>
        )}
        {!cerrado && (
          <p className="ped5-ayuda">
            {itemsNuevos > 0
              ? 'Se enviarán a cocina los productos nuevos de todas las personas'
              : hayProductos
                ? 'Todo está en cocina'
                : 'Agrega productos para continuar'}
          </p>
        )}
        {/* "Terminar pedido" (cerrar sin cobrar) solo mientras no se pidio nada (o todo se anulo). */}
        {!cerrado && puedeReorganizar && !hayProductos && (
          <button type="button" className="ped3-enlace ped3-terminar" onClick={onTerminar}>
            Terminar pedido sin productos
          </button>
        )}
      </footer>
    </aside>
  )
}
