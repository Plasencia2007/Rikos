import { useEffect, useState } from 'react'
import {
  ArrowLeftRight,
  ArrowRight,
  Ban,
  ChevronDown,
  ChevronUp,
  EllipsisVertical,
  Minus,
  Plus,
  SquarePen,
  StickyNote,
  Trash2,
  UserCheck,
  Utensils,
  UtensilsCrossed,
  Wallet,
  X,
} from 'lucide-react'
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

const productos = (n: number) => `${n} ${n === 1 ? 'producto' : 'productos'}`

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
 * El PEDIDO de toda la mesa (diseno 2026-10-29). "Por persona" agrupa las lineas en una tarjeta por comensal (P1, P2…)
 * con su total, su menu "⋮" (identificar pensionista) y se pliega; "Todos" es una sola lista con la sigla de cada
 * persona. Cada linea muestra foto, detalle, estado, precio y cantidad; el lapiz abre sus acciones (nota, pasar a otra
 * persona, anular, quitar). Un solo "Enviar a cocina" manda lo nuevo de TODA la mesa. En escritorio es el panel derecho;
 * en celular es un cajon que sale del costado (lo abre el carrito flotante). No calcula nada: pinta lo que llega del
 * servidor.
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
  onElegirPersona,
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
  /** La persona elegida arriba: su tarjeta se resalta (a ella van los productos del catálogo). */
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
  onElegirPersona: (personaId: string) => void
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
  const [vista, setVista] = useState<'persona' | 'todos'>('persona')
  const [plegadas, setPlegadas] = useState<string[]>([])
  // El menú abierto: "p:<persona>" (⋮ de la persona) o "l:<línea>" (acciones de la línea).
  const [menu, setMenu] = useState<string | null>(null)

  useEffect(() => {
    if (!menu) return
    const cerrar = (e: MouseEvent) => {
      if (!(e.target as HTMLElement).closest('.ped6-menu, .ped6-menu-boton')) setMenu(null)
    }
    const escape = (e: KeyboardEvent) => e.key === 'Escape' && setMenu(null)
    document.addEventListener('mousedown', cerrar)
    document.addEventListener('keydown', escape)
    return () => {
      document.removeEventListener('mousedown', cerrar)
      document.removeEventListener('keydown', escape)
    }
  }, [menu])

  const vigentesDe = (p: PersonaDelPedido) => p.lineas.filter((l) => l.estado !== 'anulado')
  const totalProductos = pedido.personas.reduce((n, p) => n + vigentesDe(p).reduce((m, l) => m + l.cantidad, 0), 0)
  const hayProductos = pedido.personas.some((p) => vigentesDe(p).length > 0)
  const hayLineas = pedido.personas.some((p) => p.lineas.length > 0)
  // Lo que se envía a cocina, contado en unidades (dos cafés en una línea son 2 productos).
  const porEnviar = pedido.personas.reduce((n, p) => n + p.lineas.filter((l) => l.estado === 'pendiente').reduce((m, l) => m + l.cantidad, 0), 0)
  const nombreDe = (p: PersonaDelPedido) =>
    // Al identificarla, el servidor pone el nombre del pensionista como etiqueta de la persona.
    p.pensionistaId ? (nombresDePensionista[p.pensionistaId] ?? p.etiqueta) : p.etiqueta

  const accion = (fn: () => void) => () => {
    setMenu(null)
    fn()
  }

  function linea(l: LineaDelPedido, p: PersonaDelPedido, conSigla: boolean) {
    const anulada = l.estado === 'anulado'
    const editable = !cerrado && l.estado === 'pendiente'
    const puedeAnularla = !cerrado && l.estado === 'comandado' && puedeAnular
    const puedePasarla = !cerrado && puedeReorganizar && !anulada
    const conAcciones = editable || puedeAnularla || puedePasarla
    // Del menú se muestra lo que se eligió; el refresco y el postre van incluidos y alargan la línea.
    const detalle = [
      ...l.componentes.filter((c) => c.componente !== 'refresco' && c.componente !== 'postre').map((c) => c.productoNombre),
      l.tarifaOrigen === 'plan' ? 'Precio de su plan' : null,
      l.tarifaOrigen === 'manual' ? 'Precio puesto a mano' : null,
      l.segundoDelDia ? 'Segundo del día' : null,
      l.taperUnitario > 0 ? `Táper ${enSoles(l.taperUnitario)}` : null,
    ].filter(Boolean)
    return (
      <li key={l.id} className={`ped6-linea ${anulada ? 'anulada' : ''}`}>
        <div className="ped6-foto" aria-hidden="true">
          {l.productoId && fotos[l.productoId] ? <img src={fotos[l.productoId]} alt="" /> : <UtensilsCrossed size={20} />}
        </div>
        <div className="ped6-linea-texto">
          <strong>
            {nombreDeLinea(l)}
            {conSigla && <span className="ped6-sigla">{siglaDePersona(p.etiqueta)}</span>}
          </strong>
          {detalle.length > 0 && <span className="ped6-detalle">{detalle.join(' · ')}</span>}
          {l.observacion && <span className="ped6-nota">Nota: {l.observacion}</span>}
          {anulada && (
            <span className="ped6-detalle">
              {l.anuladaPostComanda ? 'Anulado después de comandar' : 'Anulado antes de comandar'}
              {l.motivoAnulacion ? ` · ${l.motivoAnulacion}` : ''}
            </span>
          )}
        </div>
        <strong className="ped6-precio">{enSoles(l.precioUnitario * l.cantidad)}</strong>
        <div className="ped6-linea-pie">
          <span className={`ped6-estado ${l.estado}`}>
            {l.estado === 'pendiente' ? 'Por enviar' : l.estado === 'comandado' ? 'En cocina' : 'Anulado'}
          </span>
          <div className="ped6-controles">
            {conAcciones && (
              <div className="ped6-ancla">
                <button
                  type="button"
                  className="ped6-icono ped6-menu-boton"
                  aria-label={`Acciones de ${nombreDeLinea(l)}`}
                  aria-expanded={menu === `l:${l.id}`}
                  onClick={() => setMenu(menu === `l:${l.id}` ? null : `l:${l.id}`)}
                >
                  <SquarePen size={20} aria-hidden="true" />
                </button>
                {menu === `l:${l.id}` && (
                  <div className="ped6-menu" role="menu">
                    {editable && (
                      <button type="button" role="menuitem" onClick={accion(() => onNota(l))}>
                        <StickyNote size={17} aria-hidden="true" />
                        {l.observacion ? 'Editar nota para cocina' : 'Nota para cocina'}
                      </button>
                    )}
                    {puedePasarla && (
                      <button type="button" role="menuitem" onClick={accion(() => onPasarLinea(l))}>
                        <ArrowLeftRight size={17} aria-hidden="true" />
                        Pasar a otra persona
                      </button>
                    )}
                    {puedeAnularla && (
                      <button type="button" role="menuitem" className="peligro" disabled={trabajando} onClick={accion(() => onAnular(l))}>
                        <Ban size={17} aria-hidden="true" />
                        Anular
                      </button>
                    )}
                    {editable && (
                      <button type="button" role="menuitem" className="peligro" disabled={trabajando} onClick={accion(() => onQuitar(l))}>
                        <Trash2 size={17} aria-hidden="true" />
                        Quitar del pedido
                      </button>
                    )}
                  </div>
                )}
              </div>
            )}
            {editable && !cantidadFija(l, p) ? (
              <div className="ped6-cantidad">
                <button
                  type="button"
                  aria-label={l.cantidad <= 1 ? `Quitar ${nombreDeLinea(l)}` : 'Uno menos'}
                  disabled={trabajando}
                  onClick={() => (l.cantidad <= 1 ? onQuitar(l) : onCantidad(l, l.cantidad - 1))}
                >
                  {l.cantidad <= 1 ? <Trash2 size={16} aria-hidden="true" /> : <Minus size={16} aria-hidden="true" />}
                </button>
                <span aria-label="Cantidad">{l.cantidad}</span>
                <button type="button" aria-label="Uno más" disabled={trabajando} onClick={() => onCantidad(l, l.cantidad + 1)}>
                  <Plus size={16} aria-hidden="true" />
                </button>
              </div>
            ) : (
              <span className="ped6-cantidad-fija" aria-label="Cantidad">
                × {l.cantidad}
              </span>
            )}
          </div>
        </div>
      </li>
    )
  }

  return (
    <aside className={`ped2-orden ped3-orden ped4-orden ped6-orden ${abierta ? 'ped4-abierta' : ''}`} aria-label="Pedido de la mesa">
      <header className="ped3-orden-cabecera ped6-cabecera">
        <span className="ped3-circulo" aria-hidden="true">
          <Utensils size={22} />
        </span>
        <div className="ped3-orden-titulo">
          <h2>Pedido de la mesa</h2>
          <span className="cta-nota">
            {titulo} · {productos(totalProductos)}
          </span>
        </div>
        <div className="ped6-vistas" role="group" aria-label="Cómo ver el pedido">
          <button type="button" aria-pressed={vista === 'persona'} onClick={() => setVista('persona')}>
            Por persona
          </button>
          <button type="button" aria-pressed={vista === 'todos'} onClick={() => setVista('todos')}>
            Todos
          </button>
        </div>
        <button type="button" className="ped4-cerrar ped6-cerrar" aria-label="Cerrar el pedido" onClick={onCerrar}>
          <X size={20} aria-hidden="true" />
        </button>
      </header>

      <div className="ped6-cuerpo">
        {vista === 'persona' ? (
          pedido.personas.map((p) => {
            const vigentes = vigentesDe(p)
            const plegada = plegadas.includes(p.id)
            const pendientes = p.lineas.filter((l) => l.estado === 'pendiente')
            // Con productos tambien se puede: el servidor recalcula sus precios (menu al precio del plan).
            const puedeIdentificar = !cerrado && !p.lineas.some((l) => l.estado === 'cobrado')
            return (
              <section key={p.id} className={`ped6-persona ${p.id === filtroId ? 'elegida' : ''}`} aria-label={p.etiqueta}>
                <header className="ped6-persona-cabeza">
                  <button type="button" className="ped6-persona-elegir" onClick={() => onElegirPersona(p.id)}>
                    <span className={`ped6-avatar ${p.tipoCliente === 'pensionista' ? 'pensionista' : ''}`}>
                      {siglaDePersona(p.etiqueta)}
                    </span>
                    <span className="ped6-persona-texto">
                      <strong>{nombreDe(p)}</strong>
                      <small>
                        {p.tipoCliente === 'pensionista' ? 'Pensionista · ' : ''}
                        {productos(vigentes.reduce((m, l) => m + l.cantidad, 0))} · {enSoles(p.total)}
                      </small>
                    </span>
                  </button>
                  {!cerrado && (
                    <div className="ped6-ancla">
                      <button
                        type="button"
                        className="ped6-icono ped6-menu-boton"
                        aria-label={`Más opciones de ${p.etiqueta}`}
                        aria-expanded={menu === `p:${p.id}`}
                        onClick={() => setMenu(menu === `p:${p.id}` ? null : `p:${p.id}`)}
                      >
                        <EllipsisVertical size={20} aria-hidden="true" />
                      </button>
                      {menu === `p:${p.id}` && (
                        <div className="ped6-menu" role="menu">
                          <button
                            type="button"
                            role="menuitem"
                            disabled={!puedeIdentificar}
                            onClick={accion(() => onIdentificar(p.id))}
                          >
                            <UserCheck size={17} aria-hidden="true" />
                            {p.tipoCliente === 'pensionista' ? 'Cambiar pensionista' : 'Identificar pensionista'}
                          </button>
                          {!puedeIdentificar && <p className="ped6-menu-nota">Ya tiene algo cobrado.</p>}
                          {puedeIdentificar && vigentes.length > 0 && (
                            <p className="ped6-menu-nota">Sus productos se recalculan al precio de su plan.</p>
                          )}
                        </div>
                      )}
                    </div>
                  )}
                  <button
                    type="button"
                    className="ped6-icono"
                    aria-label={plegada ? `Mostrar los productos de ${p.etiqueta}` : `Ocultar los productos de ${p.etiqueta}`}
                    aria-expanded={!plegada}
                    onClick={() => setPlegadas(plegada ? plegadas.filter((x) => x !== p.id) : [...plegadas, p.id])}
                  >
                    {plegada ? <ChevronDown size={20} aria-hidden="true" /> : <ChevronUp size={20} aria-hidden="true" />}
                  </button>
                </header>
                {!plegada &&
                  (p.lineas.length === 0 ? (
                    <p className="ped6-persona-vacia">
                      {p.id === filtroId ? 'Agrega productos desde el catálogo.' : 'Sin productos. Tócala para agregarle.'}
                    </p>
                  ) : (
                    <>
                      <ul className="ped6-lineas">
                        {p.lineas.map((l) => linea(l, p, false))}
                      </ul>
                      {!cerrado && pendientes.length > 0 && (
                        <button type="button" className="ped6-agregar-nota" onClick={() => onNota(pendientes[pendientes.length - 1])}>
                          <Plus size={17} strokeWidth={2.4} aria-hidden="true" />
                          {pendientes[pendientes.length - 1].observacion ? 'Editar nota a cocina' : 'Agregar nota a cocina'}
                        </button>
                      )}
                    </>
                  ))}
              </section>
            )
          })
        ) : hayLineas ? (
          <ul className="ped6-lineas ped6-todos">
            {pedido.personas.flatMap((p) => p.lineas.map((l) => linea(l, p, true)))}
          </ul>
        ) : null}

        {!hayLineas && (
          <div className="ped5-vacio ped6-vacio">
            <PedidoVacio />
            <strong>Tu pedido está vacío</strong>
            <span>Selecciona un producto del catálogo para agregarlo a la persona elegida.</span>
          </div>
        )}
      </div>

      <footer className="ped2-orden-pie ped5-pie ped6-pie">
        <div className="ped6-resumen">
          <span>
            {productos(totalProductos)} · {pedido.personas.length} {pedido.personas.length === 1 ? 'persona' : 'personas'}
          </span>
          {pedido.taper > 0 && <span>Táper {enSoles(pedido.taper)}</span>}
        </div>
        <div className="ped6-total">
          <span>Total de la mesa</span>
          <strong aria-label="Total del pedido">{enSoles(pedido.total)}</strong>
        </div>
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
            {itemsNuevos > 0 ? `Enviar a cocina • ${productos(porEnviar)}` : 'Enviar a cocina'}
            <ArrowRight size={20} aria-hidden="true" />
          </button>
        )}
        {!cerrado && (
          <p className="ped5-ayuda">
            {itemsNuevos > 0 ? 'Revisa el pedido antes de enviarlo' : hayProductos ? 'Todo está en cocina' : 'Agrega productos para continuar'}
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
