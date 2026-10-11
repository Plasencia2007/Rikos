import { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react'
import { Combine, Info, LayoutGrid, List, Lock, Maximize, Minus, Plus, User } from 'lucide-react'
import {
  Aviso,
  Boton,
  Campo,
  EstadoCargando,
  EstadoError,
  EstadoVacio,
  Modal,
  Select,
  useAvisos,
} from '../../comun/componentes'
import {
  ErrorApi,
  abrirPedido,
  bloquearMesa,
  liberarMesa,
  listarMesas,
  listarSalones,
  obtenerPedidoDeMesa,
  permisosPropios,
  separarMesa,
  terminarPedido,
  unirMesa,
  type EstadoDeMesa,
  type Mesa,
  type Salon,
  type ServicioDelPedido,
} from '../../api/cliente'
import { mensajeDePedido } from './mensajes'
import { tiempoTranscurrido } from '../../comun/formato/tiempo'
import { posicionesDeSillas } from '../salones'

/**
 * US-027: el mapa de mesas del POS. Muestra las mesas ACTIVAS del local del
 * token con su estado (libre, ocupada, unida, pendiente de cierre, bloqueada).
 * Se actualiza solo cada 15 s. El mozo puede bloquear una mesa libre (con
 * motivo) y liberar una bloqueada. Con onAbrirPedido (el POS) tambien abre o ve
 * el pedido de una mesa y abre pedidos para llevar. Operable con el dedo: cada mesa es un boton grande.
 */

const ETIQUETA: Record<EstadoDeMesa, string> = {
  libre: 'Libre',
  ocupada: 'Ocupada',
  unida: 'Unida',
  pendiente_cierre: 'Por cobrar',
  bloqueada: 'Bloqueada',
}

const ORDEN_DE_ESTADOS: EstadoDeMesa[] = ['libre', 'ocupada', 'unida', 'pendiente_cierre', 'bloqueada']

const SEGUNDOS_ACTUALIZACION = 15

function mensajeDe(error: unknown, porDefecto: string): string {
  if (error instanceof ErrorApi) {
    switch (error.cuerpo?.error) {
      case 'MESA_MODIFICADA':
        return 'Otra persona cambió esta mesa al mismo tiempo. Se actualizó el mapa: revisa y vuelve a intentarlo.'
      case 'MOTIVO_OBLIGATORIO':
        return 'Escribe el motivo del bloqueo.'
      case 'YA_BLOQUEADA':
        return 'Esa mesa ya estaba bloqueada.'
      case 'NO_BLOQUEADA':
        return 'Esa mesa ya estaba libre.'
      case 'MESA_NO_LIBRE':
        return 'Solo se puede bloquear una mesa libre.'
      case 'TIENE_PEDIDO_ABIERTO':
        return 'Esa mesa tiene un pedido abierto: primero hay que terminarlo.'
      case 'MESA_INACTIVA':
        return 'Esa mesa está desactivada.'
      case 'SIN_LOCAL_ACTIVO':
        return 'Elige primero el local en el que estás trabajando.'
    }
    if (error.estado === 403) return 'No tienes permiso para hacer esto.'
  }
  return porDefecto
}

const SERVICIOS: { valor: ServicioDelPedido; etiqueta: string }[] = [
  { valor: 'desayuno', etiqueta: 'Desayuno' },
  { valor: 'almuerzo', etiqueta: 'Almuerzo' },
  { valor: 'cena', etiqueta: 'Cena' },
]

/** Elige el servicio cuando el local esta fuera de horario (el servidor responde SERVICIO_REQUERIDO). */
function SelectorDeServicio({
  valor,
  onCambio,
}: {
  valor: ServicioDelPedido
  onCambio: (s: ServicioDelPedido) => void
}) {
  return (
    <Select etiqueta="Servicio" value={valor} onChange={(e) => onCambio(e.target.value as ServicioDelPedido)}>
      {SERVICIOS.map((s) => (
        <option key={s.valor} value={s.valor}>
          {s.etiqueta}
        </option>
      ))}
    </Select>
  )
}

/** Fuera de horario el servidor no sabe qué servicio abrir: se avisa con calma y se deja elegir (no es un error). */
function AvisoDeServicio({ valor, onCambio }: { valor: ServicioDelPedido; onCambio: (s: ServicioDelPedido) => void }) {
  return (
    <>
      <Aviso tono="aviso">Estamos fuera de horario: elige el servicio y vuelve a pulsar el botón.</Aviso>
      <SelectorDeServicio valor={valor} onCambio={onCambio} />
    </>
  )
}

/**
 * Al deshacer una unión, la mesa principal queda como estaba antes: si su pedido está VACÍO (se abrió al unir y nadie
 * tomó nada) se cierra y la mesa vuelve a quedar libre; si ya tiene productos, los conserva.
 */
async function cerrarPedidoVacio(token: string, mesaId: string) {
  try {
    const pedido = await obtenerPedidoDeMesa(token, mesaId)
    if (pedido.personas.every((p) => p.lineas.length === 0)) await terminarPedido(token, pedido.id)
  } catch {
    // Sin pedido, o no se pudo leer: la mesa queda como esté (no se bloquea el separar).
  }
}

const nombreDeMesa = (m: Mesa) => m.nombre || `Mesa ${m.numero}`

/** Las mesas que nunca se ubicaron en el plano se acomodan en una cuadrícula para que no queden apiladas. */
function ubicar(mesas: Mesa[]): { mesa: Mesa; x: number; y: number }[] {
  let sinUbicar = 0
  return mesas.map((mesa) => {
    if (mesa.posX != null && mesa.posY != null) return { mesa, x: mesa.posX, y: mesa.posY }
    const k = sinUbicar++
    return { mesa, x: 40 + (k % 8) * 140, y: 40 + Math.floor(k / 8) * 140 }
  })
}

type MesaUbicada = { mesa: Mesa; x: number; y: number; w: number; h: number; unidas: Mesa[] }

/**
 * Las mesas unidas se muestran como UNA sola mesa: la principal, con el nombre "Principal +N" y la capacidad sumada. Las
 * unidas no se dibujan. Al separarlas, cada una vuelve sola a su lugar guardado.
 */
function agrupar(sueltas: { mesa: Mesa; x: number; y: number }[]): MesaUbicada[] {
  const ids = new Set(sueltas.map((u) => u.mesa.id))
  const esUnida = (m: Mesa) => !!m.mesaPrincipalId && ids.has(m.mesaPrincipalId)
  const hijas = new Map<string, Mesa[]>()
  for (const u of sueltas) {
    if (esUnida(u.mesa)) hijas.set(u.mesa.mesaPrincipalId!, [...(hijas.get(u.mesa.mesaPrincipalId!) ?? []), u.mesa])
  }
  const visibles = sueltas.filter((u) => !esUnida(u.mesa))
  type Rect = { x: number; y: number; w: number; h: number }
  const choca = (a: Rect, b: Rect) =>
    a.x < b.x + b.w - 1 && b.x < a.x + a.w - 1 && a.y < b.y + b.h - 1 && b.y < a.y + a.h - 1
  return visibles.map((u) => {
    const unidas = hijas.get(u.mesa.id) ?? []
    const base = { x: u.x, y: u.y, w: u.mesa.ancho, h: u.mesa.alto }
    if (unidas.length === 0) return { ...u, ...base, unidas }
    // Una mesa fusionada es una mesa grande (hasta 3 mesas de ancho y las filas que haga falta). Se centra donde estaba
    // la principal; si así pisaría a otra mesa, se prueba creciendo hacia un lado u otro; si no cabe, queda del
    // tamaño de la principal.
    const n = unidas.length + 1
    const w = u.mesa.ancho * Math.min(n, 3)
    const h = u.mesa.alto * Math.ceil(n / 3)
    const dxs = [(w - base.w) / 2, 0, w - base.w]
    const dys = [(h - base.h) / 2, 0, h - base.h]
    const candidatos = dys.flatMap((dy) => dxs.map((dx) => ({ x: u.x - dx, y: u.y - dy, w, h })))
    const libre = candidatos.find((c) =>
      visibles.every((o) => o.mesa.id === u.mesa.id || !choca(c, { x: o.x, y: o.y, w: o.mesa.ancho, h: o.mesa.alto })),
    )
    return { ...u, ...(libre ?? base), unidas }
  })
}

const SIN_SALON = '__sin_salon__'
/** Por debajo de este ancho (px) el mapa se muestra como lista de mesas y no como plano. */
const ANCHO_MINIMO_PLANO = 640
/** Espacio (px del lienzo) alrededor de las mesas al encuadrar el plano. */
const MARGEN_DEL_PLANO = 70

/** El plano del salón a escala: cabe en el ancho disponible (sin encogerse de más; si no cabe, se desliza). */
type EstadoDeUnion = 'normal' | 'principal' | 'seleccionada' | 'atenuada'

function PlanoDelSalon({
  salon,
  mesas,
  onElegir,
  zoom = 1,
  estadoUnir,
  uniones,
}: {
  salon: Pick<Salon, 'anchoPlano' | 'altoPlano' | 'colorPiso'>
  mesas: Mesa[]
  onElegir: (m: Mesa) => void
  /** 1 = ajustado al ancho; mayor o menor acerca o aleja. */
  zoom?: number
  /** Solo en modo "Unir mesas": cómo se ve cada mesa. */
  estadoUnir?: (m: Mesa) => EstadoDeUnion
  /** Mesa principal y las que se le unen, para dibujar la línea entre ellas. */
  uniones?: { principal: string; secundarias: string[] }
}) {
  const ref = useRef<HTMLDivElement>(null)
  const [disponible, setDisponible] = useState(0)
  const [vista, setVista] = useState<'plano' | 'lista'>('plano')
  useLayoutEffect(() => {
    const el = ref.current
    if (!el) return
    const medir = () => setDisponible(el.clientWidth)
    medir()
    const obs = new ResizeObserver(medir)
    obs.observe(el)
    return () => obs.disconnect()
  }, [])
  const ubicadas = useMemo(() => agrupar(ubicar(mesas)), [mesas])
  // "Ajustar vista": se encuadran las mesas (no todo el lienzo del salón), con un margen, y se centran.
  const caja = useMemo(() => {
    if (ubicadas.length === 0) return { x: 0, y: 0, w: salon.anchoPlano, h: salon.altoPlano }
    const minX = Math.min(...ubicadas.map((u) => u.x))
    const minY = Math.min(...ubicadas.map((u) => u.y))
    const maxX = Math.max(...ubicadas.map((u) => u.x + u.w))
    const maxY = Math.max(...ubicadas.map((u) => u.y + u.h))
    return {
      x: minX - MARGEN_DEL_PLANO,
      y: minY - MARGEN_DEL_PLANO,
      w: maxX - minX + 2 * MARGEN_DEL_PLANO,
      h: maxY - minY + 2 * MARGEN_DEL_PLANO,
    }
  }, [ubicadas, salon.anchoPlano, salon.altoPlano])
  const colocadas = useMemo(() => ubicadas.map((u) => ({ ...u, x: u.x - caja.x, y: u.y - caja.y })), [ubicadas, caja])
  const escala = (disponible > 0 ? Math.min(1, (disponible - 4) / caja.w) : 1) * zoom
  // En pantallas angostas el plano a escala no se lee ni se toca bien: se pasa a una lista de mesas.
  const compacto = disponible > 0 && disponible < ANCHO_MINIMO_PLANO
  if (compacto) {
    const datosDe = (m: Mesa, unidas: Mesa[]) => {
      const enUso = m.estado === 'ocupada' || m.estado === 'pendiente_cierre'
      const tomando = m.estado === 'libre' && !!m.tomandoPedido
      const tiempo = enUso || tomando ? tiempoTranscurrido(m.abiertaEn) : ''
      const fusion = unidas.length > 0
      const partes = nombreDeMesa(m).match(/^(.*?)\s*(\d+)$/)
      return { tomando, tiempo, fusion, prefijo: partes?.[1] ?? '', numero: partes?.[2] ?? '', nombre: nombreDeMesa(m) }
    }
    const claseDeUnir = (m: Mesa) => (estadoUnir ? `unir-${estadoUnir(m)}` : '')
    // Plano compacto: las mesas se acomodan en una cuadricula de 3 columnas en el orden en que estan en el salon.
    const enOrden = [...ubicadas].sort((a, b) => Math.round(a.y / 100) - Math.round(b.y / 100) || a.x - b.x)
    // Lista: primero lo que esta en servicio, despues las libres.
    const prioridad = (m: Mesa) =>
      m.estado === 'ocupada' ? 0 : m.estado === 'pendiente_cierre' ? 1 : m.estado === 'libre' ? 2 : 3
    const enLista = [...ubicadas].sort((a, b) => prioridad(a.mesa) - prioridad(b.mesa))
    return (
      <div ref={ref} className="mapa-envoltura">
        <div className="mapa-vistas" role="group" aria-label="Vista de las mesas">
          <button type="button" aria-pressed={vista === 'plano'} onClick={() => setVista('plano')}>
            <LayoutGrid size={17} strokeWidth={1.9} aria-hidden="true" />
            Plano
          </button>
          <button type="button" aria-pressed={vista === 'lista'} onClick={() => setVista('lista')}>
            <List size={17} strokeWidth={1.9} aria-hidden="true" />
            Lista
          </button>
        </div>
        {vista === 'plano' ? (
          <div className="mapa-compacto-plano" style={{ backgroundColor: salon.colorPiso }}>
            {enOrden.map(({ mesa: m, unidas }) => {
              const d = datosDe(m, unidas)
              const sub = d.fusion
                ? 'unidas'
                : d.tomando
                  ? 'Tomando'
                  : m.estado === 'pendiente_cierre'
                    ? 'Cobrar'
                    : m.estado === 'bloqueada'
                      ? 'Bloqueada'
                      : d.tiempo
              return (
                <button
                  key={m.id}
                  type="button"
                  className={`mapa-circulo mapa-${d.fusion ? 'unida' : m.estado} ${d.fusion ? 'mapa-circulo-unida' : ''} ${m.forma === 'cuadrada' || m.forma === 'rectangular' ? 'mapa-circulo-cuadrada' : ''} ${claseDeUnir(m)}`}
                  onClick={() => onElegir(m)}
                  aria-label={`${d.nombre}, ${ETIQUETA[m.estado]}${d.tiempo ? `, hace ${d.tiempo}` : ''}`}
                >
                  {d.fusion ? (
                    <b>{`${d.nombre} +${unidas.length}`}</b>
                  ) : d.numero ? (
                    <>
                      <small>{d.prefijo}</small>
                      <b>{d.numero}</b>
                    </>
                  ) : (
                    <b className="mapa-circulo-nombre">{d.nombre}</b>
                  )}
                  <em>{sub}</em>
                  {m.vip && <i className="mapa-vip">VIP</i>}
                  {estadoUnir?.(m) === 'seleccionada' && <span className="mapa-etiqueta-unir">Se une</span>}
                </button>
              )
            })}
          </div>
        ) : (
          <ul className="mapa-filas">
            {enLista.map(({ mesa: m, unidas }) => {
              const d = datosDe(m, unidas)
              return (
                <li key={m.id}>
                  <button
                    type="button"
                    className={`mapa-fila ${claseDeUnir(m)}`}
                    onClick={() => onElegir(m)}
                    aria-label={`${d.nombre}, ${ETIQUETA[m.estado]}${d.tiempo ? `, hace ${d.tiempo}` : ''}`}
                  >
                    <span className={`mapa-fila-num mapa-${d.fusion ? 'unida' : m.estado}`}>
                      {d.numero || d.nombre.slice(0, 2)}
                    </span>
                    <span className="mapa-fila-texto">
                      <b>
                        {d.fusion ? `${d.nombre} +${unidas.length}` : d.nombre}
                        {m.vip && <i className="mapa-vip">VIP</i>}
                      </b>
                      <small>
                        {d.fusion
                          ? 'Mesas unidas'
                          : m.capacidad != null
                            ? `${m.capacidad} ${m.capacidad === 1 ? 'silla' : 'sillas'}`
                            : ''}
                      </small>
                    </span>
                    <span className="mapa-fila-estado">
                      <b className={`mapa-texto-${d.fusion ? 'unida' : m.estado}`}>
                        {d.tomando ? 'Tomando pedido' : ETIQUETA[m.estado]}
                      </b>
                      <small>{d.tiempo ? `hace ${d.tiempo}` : m.estado === 'libre' ? 'Toca para abrir' : ''}</small>
                    </span>
                  </button>
                </li>
              )
            })}
          </ul>
        )}
      </div>
    )
  }
  return (
    <div ref={ref} className="mapa-envoltura">
      <div
        className="mapa-plano"
        style={{
          ['--alto-contenido' as string]: `${caja.h * escala}px`,
          backgroundColor: salon.colorPiso,
        }}
      >
        <div className="mapa-escalado" style={{ width: caja.w * escala, height: caja.h * escala }}>
          <div
            className="mapa-lienzo"
            style={{
              width: caja.w,
              height: caja.h,
              transform: `scale(${escala})`,
            }}
          >
            {uniones && (
              <svg className="mapa-lineas" width={caja.w} height={caja.h} aria-hidden="true">
                {uniones.secundarias.map((id) => {
                  const a = colocadas.find((u) => u.mesa.id === uniones.principal)
                  const b = colocadas.find((u) => u.mesa.id === id)
                  if (!a || !b) return null
                  return <line key={id} x1={a.x + a.w / 2} y1={a.y + a.h / 2} x2={b.x + b.w / 2} y2={b.y + b.h / 2} />
                })}
              </svg>
            )}
            {colocadas.map(({ mesa: m, x, y, w: ancho, h: alto, unidas }) => {
              const enUso = m.estado === 'ocupada' || m.estado === 'pendiente_cierre'
              const tomando = m.estado === 'libre' && !!m.tomandoPedido
              const tiempo = enUso || tomando ? tiempoTranscurrido(m.abiertaEn) : ''
              const fusion = unidas.length > 0
              const capacidad = (m.capacidad ?? 4) + unidas.reduce((suma, u) => suma + (u.capacidad ?? 4), 0)
              const nombre = fusion ? `${nombreDeMesa(m)} +${unidas.length}` : nombreDeMesa(m)
              const sillas = posicionesDeSillas(fusion ? 'rectangular' : m.forma, ancho, alto, capacidad)
              const colorSilla = {
                libre: '#9bd0c3',
                ocupada: '#eba8a0',
                pendiente_cierre: '#e9cb8c',
                bloqueada: '#c9d2cf',
                unida: '#b5c6da',
              }[fusion ? 'unida' : m.estado]
              return (
                <button
                  key={m.id}
                  type="button"
                  className={`mapa-mesa mapa-${fusion ? 'unida' : m.estado} ${m.forma === 'redonda' ? 'redonda' : ''} ${fusion ? 'fusionada' : ''} ${estadoUnir ? `unir-${estadoUnir(m)}` : ''}`}
                  style={{ left: x, top: y, width: ancho, height: alto }}
                  onClick={() => onElegir(m)}
                  aria-label={`${nombre}, ${fusion ? `Unida (${unidas.length + 1} mesas)` : ETIQUETA[m.estado]}${tiempo ? `, hace ${tiempo}` : ''}`}
                >
                  {sillas.map((s, i) => (
                    <span
                      key={i}
                      className="mapa-silla"
                      style={{
                        left: s.x,
                        top: s.y,
                        width: s.horizontal ? 24 : 9,
                        height: s.horizontal ? 9 : 24,
                        background: colorSilla,
                      }}
                    />
                  ))}
                  <span className="mapa-cuerpo">
                    <b>{nombre}</b>
                    {m.estado === 'bloqueada' ? (
                      <span>
                        <Lock size={11} strokeWidth={2} aria-hidden="true" />
                        {m.motivoBloqueo ?? ''}
                      </span>
                    ) : tomando ? (
                      <span>Tomando · {tiempo}</span>
                    ) : tiempo ? (
                      <span>{tiempo}</span>
                    ) : fusion ? (
                      <span>{capacidad} sillas</span>
                    ) : m.estado === 'libre' && m.capacidad != null ? (
                      <span>{m.capacidad} sillas</span>
                    ) : m.estado !== 'libre' ? (
                      <span>{ETIQUETA[m.estado]}</span>
                    ) : null}
                  </span>
                  {m.vip && <span className="mapa-vip">VIP</span>}
                  {tomando && <span className="mapa-tomando">Tomando pedido</span>}
                  {estadoUnir?.(m) === 'principal' && <span className="mapa-etiqueta-unir principal">Principal</span>}
                  {estadoUnir?.(m) === 'seleccionada' && <span className="mapa-etiqueta-unir">Se une</span>}
                </button>
              )
            })}
          </div>
        </div>
      </div>
    </div>
  )
}

export function MapaDeMesas({
  token,
  onAbrirPedido,
}: {
  token: string
  onAbrirPedido?: (pedidoId: string, titulo: string) => void
}) {
  const [mesas, setMesas] = useState<Mesa[] | null>(null)
  const [salones, setSalones] = useState<Salon[]>([])
  const [salonElegido, setSalonElegido] = useState<string | null>(null)
  const [error, setError] = useState('')
  const [puedeBloquear, setPuedeBloquear] = useState(false)
  const [seleccionada, setSeleccionada] = useState<Mesa | null>(null)
  const [verDetalle, setVerDetalle] = useState(false)
  const [puedeReorganizar, setPuedeReorganizar] = useState(false)
  const [zoom, setZoom] = useState(1)
  const [modoUnir, setModoUnir] = useState<{ principal: string | null; secundarias: string[] } | null>(null)
  const [dialogoUnir, setDialogoUnir] = useState(false)
  const avisos = useAvisos()

  const cargar = useCallback(
    (silencioso = false) => {
      if (!silencioso) setMesas(null)
      setError('')
      Promise.all([listarMesas(token), listarSalones(token).catch(() => [] as Salon[])])
        .then(([m, s]) => {
          setMesas(m)
          setSalones(s.filter((x) => x.activo))
        })
        .catch((e) => setError(mensajeDe(e, 'No se pudo cargar el mapa de mesas.')))
    },
    [token],
  )

  useEffect(() => {
    cargar()
    const reloj = window.setInterval(() => cargar(true), SEGUNDOS_ACTUALIZACION * 1000)
    return () => window.clearInterval(reloj)
  }, [cargar])

  useEffect(() => {
    // Solo para decidir que botones mostrar; el servidor decide cada accion. Si falla, se ocultan.
    permisosPropios(token)
      .then((lista) => {
        setPuedeBloquear(lista.includes('venta.bloquear_mesa'))
        setPuedeReorganizar(lista.includes('venta.modificar'))
      })
      .catch(() => {
        setPuedeBloquear(false)
        setPuedeReorganizar(false)
      })
  }, [token])

  // Pestañas: los salones activos con mesas en este mapa, y "Sin salón" si alguna mesa quedó suelta.
  const pestanas = useMemo(() => {
    if (!mesas) return []
    const lista = salones.map((s) => ({
      id: s.id,
      nombre: s.nombre,
      salon: s as Pick<Salon, 'anchoPlano' | 'altoPlano' | 'colorPiso'> & {
        descripcion: string | null
      },
      mesas: mesas.filter((m) => m.salonId === s.id),
    }))
    const conocidos = new Set(salones.map((s) => s.id))
    const sueltas = mesas.filter((m) => !m.salonId || !conocidos.has(m.salonId))
    if (sueltas.length > 0) {
      lista.push({
        id: SIN_SALON,
        nombre: 'Sin salón',
        salon: {
          anchoPlano: 1200,
          altoPlano: 800,
          colorPiso: '#F3EEDF',
          descripcion: null,
        },
        mesas: sueltas,
      })
    }
    return lista.filter((p) => p.mesas.length > 0)
  }, [mesas, salones])

  // "Unir mesas": primero la principal (la que conserva el pedido), luego las libres que se le unen.
  const puedeSerPrincipal = (m: Mesa) => m.estado !== 'bloqueada' && m.estado !== 'unida'
  const puedeUnirse = (m: Mesa) => m.estado === 'libre' && !m.tomandoPedido

  const estadoUnir = (m: Mesa): EstadoDeUnion => {
    if (!modoUnir) return 'normal'
    if (m.id === modoUnir.principal) return 'principal'
    if (modoUnir.secundarias.includes(m.id)) return 'seleccionada'
    return (modoUnir.principal ? puedeUnirse(m) : puedeSerPrincipal(m)) ? 'normal' : 'atenuada'
  }

  function alElegirMesa(m: Mesa) {
    if (!modoUnir) {
      setVerDetalle(false)
      const unidas = (mesas ?? []).filter((x) => x.mesaPrincipalId === m.id)
      if (unidas.length > 0 && onAbrirPedido) {
        // Una mesa ya formada (unida) lleva directo a su pedido, titulado con todas las mesas: "Terraza 4 + Terraza 10".
        const titulo = [m, ...unidas].map(nombreDeMesa).join(' + ')
        obtenerPedidoDeMesa(token, m.id)
          .then((p) => onAbrirPedido(p.id, titulo))
          .catch(() => setSeleccionada(m))
        return
      }
      setSeleccionada(m)
      return
    }
    if (m.id === modoUnir.principal) {
      setModoUnir({ principal: null, secundarias: [] })
    } else if (!modoUnir.principal) {
      if (puedeSerPrincipal(m)) setModoUnir({ principal: m.id, secundarias: [] })
      else avisos.mostrar('Esa mesa no puede ser la principal.', 'error')
    } else if (modoUnir.secundarias.includes(m.id)) {
      setModoUnir({ ...modoUnir, secundarias: modoUnir.secundarias.filter((id) => id !== m.id) })
    } else if (puedeUnirse(m)) {
      setModoUnir({ ...modoUnir, secundarias: [...modoUnir.secundarias, m.id] })
    } else {
      avisos.mostrar('Solo se pueden unir mesas libres y sin pedido abierto.', 'error')
    }
  }

  if (error && !mesas) {
    return <EstadoError mensaje={error} accion={<Boton onClick={() => cargar()}>Reintentar</Boton>} />
  }
  if (!mesas) return <EstadoCargando />
  if (mesas.length === 0) {
    return (
      <EstadoVacio
        titulo="Este local todavía no tiene mesas"
        descripcion="Pídele al administrador que las cree en Configuración → Salones y mesas."
      />
    )
  }

  const actual = pestanas.find((p) => p.id === salonElegido) ?? pestanas[0]
  const ocupadas = actual.mesas.filter((m) => m.estado === 'ocupada' || m.estado === 'pendiente_cierre').length
  const libres = actual.mesas.filter((m) => m.estado === 'libre').length
  const porcentaje = Math.round((ocupadas / actual.mesas.length) * 100)
  const porCobrar = actual.mesas.filter((m) => m.estado === 'pendiente_cierre').length
  const bloqueadas = actual.mesas.filter((m) => m.estado === 'bloqueada').length
  const parte = (n: number) => `${(n / actual.mesas.length) * 100}%`

  return (
    <section className="mapa-mesas">
      {error && <Aviso tono="error">{error}</Aviso>}
      <div className="mapa-barra">
        {pestanas.length > 1 ? (
          <div className="mapa-pestanas" role="tablist" aria-label="Salones">
            {pestanas.map((p) => (
              <button
                key={p.id}
                type="button"
                role="tab"
                aria-selected={p.id === actual.id}
                className={`mapa-pestana ${p.id === actual.id ? 'elegida' : ''}`}
                onClick={() => {
                  setSalonElegido(p.id)
                  setModoUnir(null)
                }}
              >
                <LayoutGrid size={16} strokeWidth={1.9} aria-hidden="true" />
                {p.nombre} <span>({p.mesas.length})</span>
              </button>
            ))}
          </div>
        ) : (
          <span />
        )}
        <div className="mapa-barra-acciones">
          {puedeReorganizar && (
            <button
              type="button"
              className={`mapa-boton-unir ${modoUnir ? 'activo' : ''}`}
              aria-pressed={modoUnir !== null}
              onClick={() => setModoUnir(modoUnir ? null : { principal: null, secundarias: [] })}
            >
              <Combine size={17} strokeWidth={1.9} aria-hidden="true" />
              Unir mesas
            </button>
          )}
        </div>
      </div>
      <div className="mapa-tarjeta">
        <div className="mapa-cabecera">
          <div>
            <strong>{actual.nombre}</strong>
            {actual.salon.descripcion && <p>{actual.salon.descripcion}</p>}
          </div>
          <div className="mapa-cifras" aria-label="Resumen del salón">
            <span>
              <i className="mapa-punto mapa-ocupada" aria-hidden="true" />
              <b>{ocupadas}</b> de {actual.mesas.length} ocupadas <em className="mapa-porcentaje">{porcentaje}%</em>
            </span>
            <span className="mapa-cifras-separador" aria-hidden="true" />
            <span>
              <i className="mapa-punto mapa-libre" aria-hidden="true" />
              <b>{libres}</b> {libres === 1 ? 'libre' : 'libres'}
            </span>
          </div>
          <div className="mapa-barra-estado" aria-hidden="true">
            <span style={{ width: parte(ocupadas - porCobrar), background: '#c0392b' }} />
            <span style={{ width: parte(porCobrar), background: '#b7791f' }} />
            <span style={{ width: parte(bloqueadas), background: '#7b8985' }} />
          </div>
          <div className="mapa-zoom">
            <button type="button" className="mapa-ajustar" onClick={() => setZoom(1)}>
              <Maximize size={16} strokeWidth={1.8} aria-hidden="true" />
              Ajustar vista
            </button>
            <span className="mapa-zoom-control">
              <button
                type="button"
                aria-label="Alejar"
                onClick={() => setZoom((z) => Math.max(0.5, +(z - 0.1).toFixed(2)))}
              >
                <Minus size={16} aria-hidden="true" />
              </button>
              <span aria-live="polite">{Math.round(zoom * 100)}%</span>
              <button
                type="button"
                aria-label="Acercar"
                onClick={() => setZoom((z) => Math.min(1.6, +(z + 0.1).toFixed(2)))}
              >
                <Plus size={16} aria-hidden="true" />
              </button>
            </span>
          </div>
        </div>
        {modoUnir && (
          <div className="mapa-guia-unir" role="status">
            <Combine size={18} aria-hidden="true" />
            <div>
              <strong>Unir mesas</strong>
              <span>
                {modoUnir.principal
                  ? '2. Toca las mesas libres que se le unen (las ocupadas o con pedido abierto no se pueden unir).'
                  : '1. Toca la mesa principal (la que conserva el pedido).'}
              </span>
            </div>
          </div>
        )}
        <PlanoDelSalon
          salon={actual.salon}
          mesas={actual.mesas}
          onElegir={alElegirMesa}
          zoom={zoom}
          estadoUnir={modoUnir ? estadoUnir : undefined}
          uniones={
            modoUnir?.principal ? { principal: modoUnir.principal, secundarias: modoUnir.secundarias } : undefined
          }
        />
        {modoUnir ? (
          <div className="mapa-pie-unir">
            <p>
              <b>Principal:</b>{' '}
              {modoUnir.principal ? nombreDeMesa(mesas.find((m) => m.id === modoUnir.principal)!) : '—'}
              {' · '}
              <b>Se unen:</b>{' '}
              {modoUnir.secundarias.length === 0
                ? '—'
                : modoUnir.secundarias.map((id) => nombreDeMesa(mesas.find((m) => m.id === id)!)).join(', ')}
            </p>
            <Boton variante="secundario" onClick={() => setModoUnir(null)}>
              Cancelar
            </Boton>
            <Boton
              disabled={!modoUnir.principal || modoUnir.secundarias.length === 0}
              onClick={() => setDialogoUnir(true)}
            >
              Unir {modoUnir.secundarias.length + 1} mesas
            </Boton>
          </div>
        ) : (
          <ul className="mapa-leyenda" aria-label="Leyenda">
            {ORDEN_DE_ESTADOS.map((e) => (
              <li key={e}>
                <i className={`mapa-tinta mapa-${e}`} aria-hidden="true" />
                {ETIQUETA[e]}
              </li>
            ))}
            <li>
              <i className="mapa-tinta mapa-tomandomarca" aria-hidden="true" />
              Tomando pedido
            </li>
            <li>
              <i className="mapa-tinta mapa-vipmarca" aria-hidden="true" />
              Mesa VIP
            </li>
          </ul>
        )}
      </div>

      {dialogoUnir && modoUnir?.principal && (
        <DialogoUnirMesas
          token={token}
          principal={mesas.find((m) => m.id === modoUnir.principal)!}
          secundarias={modoUnir.secundarias.map((id) => mesas.find((m) => m.id === id)!)}
          onAbrirPedido={onAbrirPedido}
          onCerrar={() => setDialogoUnir(false)}
          onHecho={() => {
            setDialogoUnir(false)
            setModoUnir(null)
            cargar(true)
          }}
        />
      )}
      {seleccionada &&
        seleccionada.estado === 'libre' &&
        !seleccionada.tomandoPedido &&
        onAbrirPedido &&
        !verDetalle &&
        !mesas.some((m) => m.mesaPrincipalId === seleccionada.id) && (
          <DialogoAbrirMesa
            token={token}
            mesa={seleccionada}
            puedeBloquear={puedeBloquear}
            onAbrirPedido={onAbrirPedido}
            onCerrar={() => setSeleccionada(null)}
            onBloquear={() => setVerDetalle(true)}
            onCambio={() => {
              setSeleccionada(null)
              cargar(true)
            }}
          />
        )}
      {seleccionada &&
        (seleccionada.estado !== 'libre' ||
          !!seleccionada.tomandoPedido ||
          !onAbrirPedido ||
          verDetalle ||
          mesas.some((m) => m.mesaPrincipalId === seleccionada.id)) && (
          <DialogoDeMesa
            token={token}
            mesa={seleccionada}
            puedeBloquear={puedeBloquear}
            puedeReorganizar={puedeReorganizar}
            principal={mesas.find((m) => m.id === seleccionada.mesaPrincipalId) ?? null}
            unidas={mesas.filter((m) => m.mesaPrincipalId === seleccionada.id)}
            otrasUnidas={
              mesas.filter(
                (m) =>
                  m.mesaPrincipalId === seleccionada.mesaPrincipalId && m.id !== seleccionada.id && !!m.mesaPrincipalId,
              ).length
            }
            onAbrirPedido={onAbrirPedido}
            onCerrar={() => setSeleccionada(null)}
            onCambio={() => {
              setSeleccionada(null)
              cargar(true)
            }}
          />
        )}
    </section>
  )
}

/**
 * "Unir mesas": confirma la unión. La primera mesa elegida es la principal. Si la principal está libre y estamos en el
 * POS, se abre su pedido con el total de comensales y el cliente (como al abrir una mesa); si ya tiene pedido, solo se
 * unen las mesas a él.
 */
function DialogoUnirMesas({
  token,
  principal,
  secundarias,
  onAbrirPedido,
  onCerrar,
  onHecho,
}: {
  token: string
  principal: Mesa
  secundarias: Mesa[]
  onAbrirPedido?: (pedidoId: string, titulo: string) => void
  onCerrar: () => void
  onHecho: () => void
}) {
  const avisos = useAvisos()
  const todas = [principal, ...secundarias]
  const capacidad = todas.reduce((suma, m) => suma + (m.capacidad ?? 0), 0)
  const abreElPedido = principal.estado === 'libre' && !principal.tomandoPedido && !!onAbrirPedido
  const [comensales, setComensales] = useState(Math.min(30, Math.max(1, capacidad)))
  const [cliente, setCliente] = useState('')
  const [servicio, setServicio] = useState<ServicioDelPedido>('almuerzo')
  const [pideServicio, setPideServicio] = useState(false)
  const [trabajando, setTrabajando] = useState(false)
  const [error, setError] = useState('')

  const fijar = (n: number) => setComensales(Math.min(30, Math.max(1, Number.isFinite(n) ? n : 1)))

  async function unir() {
    setTrabajando(true)
    setError('')
    let pedidoId: string | null = null
    const hechas: Mesa[] = []
    try {
      // 1) Si hay que abrir el pedido de la principal, se hace PRIMERO: si falta el servicio (fuera de horario) o falla,
      //    no se une ninguna mesa.
      if (abreElPedido) {
        const pedido = await abrirPedido(token, {
          canal: 'mesa',
          mesaId: principal.id,
          servicio: pideServicio ? servicio : undefined,
          comensales,
          nombreCliente: cliente.trim() === '' ? undefined : cliente.trim(),
        })
        pedidoId = pedido.id
      }
      // 2) Se unen las mesas; si una falla se deshace todo lo hecho (todo o nada).
      for (const m of secundarias) {
        await unirMesa(token, m.id, principal.id)
        hechas.push(m)
      }
      avisos.mostrar(secundarias.length === 1 ? 'Mesa unida' : `${secundarias.length} mesas unidas`)
      onHecho()
      if (pedidoId && onAbrirPedido) onAbrirPedido(pedidoId, nombreDeMesa(principal))
    } catch (e) {
      for (const m of hechas) await separarMesa(token, m.id).catch(() => undefined)
      if (pedidoId) await terminarPedido(token, pedidoId).catch(() => undefined)
      if (e instanceof ErrorApi && e.cuerpo?.error === 'SERVICIO_REQUERIDO') setPideServicio(true)
      else setError(mensajeDePedido(e, 'No se pudieron unir las mesas. No se unió ninguna.'))
    } finally {
      setTrabajando(false)
    }
  }

  return (
    <Modal
      abierto
      titulo="Unir mesas"
      onCerrar={onCerrar}
      pie={
        <>
          <Boton variante="secundario" onClick={onCerrar}>
            Cancelar
          </Boton>
          <Boton cargando={trabajando} onClick={unir}>
            Unir
          </Boton>
        </>
      }
    >
      <div className="cta-dialogo">
        <p>
          {todas.length} mesas · Capacidad total: {capacidad} personas
        </p>
        {error && <Aviso tono="error">{error}</Aviso>}
        <span className="mapa-etiqueta-seccion">Mesas a unir</span>
        <div className="mapa-chips">
          {todas.map((m, i) => (
            <span key={m.id} className={`mapa-chip ${i === 0 ? 'principal' : ''}`}>
              {nombreDeMesa(m)}
              {i === 0 && <b>Principal</b>}
            </span>
          ))}
        </div>
        <small className="mapa-ayuda">La primera mesa elegida es la principal: conserva el pedido.</small>
        {abreElPedido ? (
          <>
            <span className="mapa-etiqueta">Total de comensales *</span>
            <div className="mapa-comensales">
              <button type="button" aria-label="Menos comensales" onClick={() => fijar(comensales - 1)}>
                <Minus size={18} aria-hidden="true" />
              </button>
              <input
                type="number"
                inputMode="numeric"
                min={1}
                max={30}
                aria-label="Total de comensales"
                value={comensales}
                onChange={(e) => fijar(Number(e.target.value))}
              />
              <button type="button" aria-label="Más comensales" onClick={() => fijar(comensales + 1)}>
                <Plus size={18} aria-hidden="true" />
              </button>
            </div>
            <Campo
              etiqueta="Nombre del cliente (opcional)"
              placeholder="Ej. Familia García"
              maxLength={120}
              value={cliente}
              onChange={(e) => setCliente(e.target.value)}
            />
            {pideServicio && <AvisoDeServicio valor={servicio} onCambio={setServicio} />}
          </>
        ) : (
          <div className="mapa-nota">
            <Info size={17} aria-hidden="true" />
            <span>
              {principal.estado === 'libre'
                ? 'Las mesas quedan unidas. El pedido se abre desde la mesa principal.'
                : `Las mesas se unen al pedido de ${nombreDeMesa(principal)}.`}
            </span>
          </div>
        )}
      </div>
    </Modal>
  )
}

/** "Abrir mesa": cuántos comensales y, si se quiere, a nombre de quién. Abre el pedido y entra a tomarlo. */
function DialogoAbrirMesa({
  token,
  mesa,
  puedeBloquear,
  onAbrirPedido,
  onCerrar,
  onBloquear,
  onCambio,
}: {
  token: string
  mesa: Mesa
  puedeBloquear: boolean
  onAbrirPedido: (pedidoId: string, titulo: string) => void
  onCerrar: () => void
  onBloquear: () => void
  onCambio: () => void
}) {
  const [comensales, setComensales] = useState(1)
  const [cliente, setCliente] = useState('')
  const [servicio, setServicio] = useState<ServicioDelPedido>('almuerzo')
  const [pideServicio, setPideServicio] = useState(false)
  const [trabajando, setTrabajando] = useState(false)
  const [error, setError] = useState('')

  const fijar = (n: number) => setComensales(Math.min(30, Math.max(1, Number.isFinite(n) ? n : 1)))

  function abrir() {
    setTrabajando(true)
    setError('')
    abrirPedido(token, {
      canal: 'mesa',
      mesaId: mesa.id,
      servicio: pideServicio ? servicio : undefined,
      comensales,
      nombreCliente: cliente.trim() === '' ? undefined : cliente.trim(),
    })
      .then((p) => onAbrirPedido(p.id, nombreDeMesa(mesa)))
      .catch((e) => {
        if (e instanceof ErrorApi && e.cuerpo?.error === 'SERVICIO_REQUERIDO') setPideServicio(true)
        else setError(mensajeDePedido(e, 'No se pudo abrir el pedido.'))
        if (e instanceof ErrorApi && e.cuerpo?.error === 'MESA_OCUPADA') onCambio()
      })
      .finally(() => setTrabajando(false))
  }

  return (
    <Modal
      abierto
      titulo={`Abrir ${nombreDeMesa(mesa)}`}
      onCerrar={onCerrar}
      pie={
        <>
          {puedeBloquear && (
            <Boton variante="secundario" onClick={onBloquear}>
              Bloquear mesa
            </Boton>
          )}
          <Boton variante="secundario" onClick={onCerrar}>
            Cancelar
          </Boton>
          <Boton cargando={trabajando} onClick={abrir}>
            Abrir mesa
          </Boton>
        </>
      }
    >
      <div className="cta-dialogo">
        <p>
          <User size={14} aria-hidden="true" /> Mesa {mesa.numero}
          {mesa.capacidad != null ? ` · hasta ${mesa.capacidad} comensales` : ''}
        </p>
        {error && <Aviso tono="error">{error}</Aviso>}
        <span className="mapa-etiqueta">Cantidad de comensales *</span>
        <div className="mapa-comensales">
          <button type="button" aria-label="Menos comensales" onClick={() => fijar(comensales - 1)}>
            <Minus size={18} aria-hidden="true" />
          </button>
          <input
            type="number"
            inputMode="numeric"
            min={1}
            max={30}
            aria-label="Comensales"
            value={comensales}
            onChange={(e) => fijar(Number(e.target.value))}
          />
          <button type="button" aria-label="Más comensales" onClick={() => fijar(comensales + 1)}>
            <Plus size={18} aria-hidden="true" />
          </button>
        </div>
        <Campo
          etiqueta="Nombre del cliente (opcional)"
          placeholder="Ej. Familia García"
          maxLength={120}
          value={cliente}
          onChange={(e) => setCliente(e.target.value)}
        />
        {pideServicio && <AvisoDeServicio valor={servicio} onCambio={setServicio} />}
        <div className="mapa-nota">
          <Info size={17} aria-hidden="true" />
          <span>La mesa quedará ocupada y podrás agregar productos a la orden.</span>
        </div>
      </div>
    </Modal>
  )
}

function DialogoDeMesa({
  token,
  mesa,
  puedeBloquear,
  puedeReorganizar = false,
  principal = null,
  unidas = [],
  otrasUnidas = 0,
  onAbrirPedido,
  onCerrar,
  onCambio,
}: {
  token: string
  mesa: Mesa
  puedeBloquear: boolean
  puedeReorganizar?: boolean
  /** Si la mesa está unida, la mesa principal a la que pertenece. */
  principal?: Mesa | null
  /** Si la mesa es principal, las mesas que tiene unidas. */
  unidas?: Mesa[]
  /** Si la mesa está unida: cuántas otras mesas siguen unidas a la misma principal. */
  otrasUnidas?: number
  onAbrirPedido?: (pedidoId: string, titulo: string) => void
  onCerrar: () => void
  onCambio: () => void
}) {
  const [motivo, setMotivo] = useState('')
  const [trabajando, setTrabajando] = useState(false)
  const [error, setError] = useState('')
  const [servicio, setServicio] = useState<ServicioDelPedido>('almuerzo')
  const [pideServicio, setPideServicio] = useState(false)
  const titulo = mesa.estado === 'unida' && principal ? nombreDeMesa(principal) : nombreDeMesa(mesa)

  function abrir() {
    if (!onAbrirPedido) return
    setTrabajando(true)
    setError('')
    abrirPedido(token, {
      canal: 'mesa',
      mesaId: mesa.id,
      servicio: pideServicio ? servicio : undefined,
    })
      .then((p) => onAbrirPedido(p.id, titulo))
      .catch((e) => {
        if (e instanceof ErrorApi && e.cuerpo?.error === 'SERVICIO_REQUERIDO') setPideServicio(true)
        setError(mensajeDePedido(e, 'No se pudo abrir el pedido.'))
        if (e instanceof ErrorApi && e.cuerpo?.error === 'MESA_OCUPADA') onCambio()
      })
      .finally(() => setTrabajando(false))
  }

  function ver() {
    if (!onAbrirPedido) return
    setTrabajando(true)
    setError('')
    obtenerPedidoDeMesa(token, mesa.id)
      .then((p) => onAbrirPedido(p.id, titulo))
      .catch((e) => setError(mensajeDePedido(e, 'No se pudo abrir el pedido de esta mesa.')))
      .finally(() => setTrabajando(false))
  }

  function correr(accion: Promise<unknown>) {
    setTrabajando(true)
    setError('')
    accion
      .then(onCambio)
      .catch((e) => setError(mensajeDe(e, 'No se pudo hacer el cambio.')))
      .finally(() => setTrabajando(false))
  }

  function bloquear() {
    if (motivo.trim() === '') {
      setError('Escribe el motivo del bloqueo.')
      return
    }
    correr(bloquearMesa(token, mesa.id, motivo.trim()))
  }

  return (
    <Modal
      abierto
      titulo={nombreDeMesa(mesa)}
      onCerrar={onCerrar}
      pie={
        <Boton variante="secundario" onClick={onCerrar}>
          Cerrar
        </Boton>
      }
    >
      <div className="cta-dialogo">
        <p>
          Estado: <strong>{ETIQUETA[mesa.estado]}</strong>
          {mesa.capacidad != null ? ` · ${mesa.capacidad} personas` : ''}
        </p>
        {error && <Aviso tono="error">{error}</Aviso>}

        {mesa.estado === 'bloqueada' && (
          <>
            <Aviso>Bloqueada: {mesa.motivoBloqueo}</Aviso>
            {puedeBloquear && (
              <Boton cargando={trabajando} onClick={() => correr(liberarMesa(token, mesa.id))}>
                Liberar mesa
              </Boton>
            )}
          </>
        )}

        {mesa.estado === 'libre' && mesa.tomandoPedido && (
          <>
            <Aviso tono="informacion">
              Esta mesa está libre, pero alguien está tomando su pedido (todavía no se comanda). Puedes continuar con
              ese mismo pedido.
            </Aviso>
            {onAbrirPedido && (
              <Boton cargando={trabajando} onClick={ver}>
                Continuar pedido
              </Boton>
            )}
          </>
        )}

        {mesa.estado === 'libre' && !mesa.tomandoPedido && onAbrirPedido && (
          <>
            {pideServicio && <AvisoDeServicio valor={servicio} onCambio={setServicio} />}
            <Boton cargando={trabajando} onClick={abrir}>
              Abrir pedido
            </Boton>
          </>
        )}

        {mesa.estado === 'libre' && !mesa.tomandoPedido && puedeBloquear && (
          <>
            <Campo
              etiqueta="Motivo del bloqueo"
              ayuda='Por ejemplo: "Mesa rota" o "Reservada".'
              maxLength={250}
              value={motivo}
              onChange={(e) => setMotivo(e.target.value)}
            />
            <Boton variante="secundario" cargando={trabajando} onClick={bloquear}>
              Bloquear mesa
            </Boton>
          </>
        )}

        {unidas.length > 0 && (
          <>
            <Aviso tono="informacion">
              Mesas unidas a esta: {unidas.map((m) => nombreDeMesa(m)).join(', ')}. Al separarlas, si el pedido esta
              vacio se cierra y esta mesa queda libre; si ya tiene productos, los conserva.
            </Aviso>
            {puedeReorganizar && (
              <Boton
                variante="secundario"
                cargando={trabajando}
                onClick={() =>
                  correr(
                    Promise.all(unidas.map((m) => separarMesa(token, m.id))).then(() =>
                      cerrarPedidoVacio(token, mesa.id),
                    ),
                  )
                }
              >
                {unidas.length === 1 ? 'Separar la mesa unida' : 'Separar todas las mesas'}
              </Boton>
            )}
          </>
        )}

        {mesa.estado === 'unida' && (
          <>
            <Aviso tono="informacion">
              Unida a {principal ? nombreDeMesa(principal) : 'otra mesa'}. Se atiende en el pedido de la mesa principal;
              al separarla queda libre.
            </Aviso>
            {onAbrirPedido && (
              <Boton variante="secundario" cargando={trabajando} onClick={ver}>
                Ver pedido de {principal ? nombreDeMesa(principal) : 'la principal'}
              </Boton>
            )}
            {puedeReorganizar && (
              <Boton
                cargando={trabajando}
                onClick={() =>
                  correr(
                    separarMesa(token, mesa.id).then(() =>
                      principal && otrasUnidas === 0 ? cerrarPedidoVacio(token, principal.id) : undefined,
                    ),
                  )
                }
              >
                Separar mesa
              </Boton>
            )}
          </>
        )}

        {(mesa.estado === 'ocupada' || mesa.estado === 'pendiente_cierre') && onAbrirPedido && (
          <Boton cargando={trabajando} onClick={ver}>
            Ver pedido
          </Boton>
        )}

        {mesa.estado !== 'libre' && mesa.estado !== 'bloqueada' && !onAbrirPedido && (
          <p>Las mesas ocupadas, unidas o pendientes de cierre se atienden desde su pedido.</p>
        )}
      </div>
    </Modal>
  )
}
