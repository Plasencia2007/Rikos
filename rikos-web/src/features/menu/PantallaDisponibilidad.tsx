import { useEffect, useMemo, useState } from 'react'
import { useSesion } from '../../comun/sesion/SesionContexto'
import {
  Aviso,
  Boton,
  Campo,
  EstadoCargando,
  EstadoError,
  EstadoVacio,
  IconoBusqueda,
  Insignia,
  Modal,
  Select,
} from '../../comun/componentes'
import {
  ErrorApi,
  listarAgotados,
  listarMotivos,
  listarProductos,
  marcarAgotado,
  obtenerMenuDelDia,
  reactivarProducto,
  type Agotado,
  type Motivo,
  type Producto,
} from '../../api/cliente'

/**
 * US-015. Lo que se ofrece hoy en el local activo. Pensada para cocina en
 * pleno servicio: marcar agotado es un toque; volver a ofrecer pide motivo
 * (RN-066). El local no se elige: es el del encabezado (el de la sesion).
 */

const PARTE: Record<string, string> = {
  entrada: 'Entrada',
  segundo: 'Segundo',
  guarnicion: 'Guarnición',
  refresco: 'Refresco',
  postre: 'Postre',
}

function hoyTexto(): string {
  const f = new Date()
  return `${f.getFullYear()}-${String(f.getMonth() + 1).padStart(2, '0')}-${String(f.getDate()).padStart(2, '0')}`
}

function hora(iso: string): string {
  const f = new Date(iso)
  const hoy = new Date()
  const mismoDia = f.toDateString() === hoy.toDateString()
  return mismoDia
    ? `desde las ${f.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' })}`
    : `desde el ${f.toLocaleDateString('es-PE', { day: 'numeric', month: 'short' })}`
}

function mensajeError(e: unknown, porDefecto: string): string {
  if (!(e instanceof ErrorApi)) return porDefecto
  switch (e.cuerpo?.error) {
    case 'SIN_LOCAL_ACTIVO':
      return 'Tu sesión no tiene un local activo. Elige uno arriba.'
    case 'YA_AGOTADO':
      return 'Alguien ya lo marcó como agotado.'
    case 'NO_ESTA_AGOTADO':
      return 'Alguien ya lo volvió a ofrecer.'
    case 'MOTIVO_OBLIGATORIO':
    case 'MOTIVO_INVALIDO':
      return 'Elige un motivo de la lista.'
    case 'OBSERVACION_OBLIGATORIA':
      return 'Ese motivo pide una observación.'
    case 'PRODUCTO_NO_VENDIBLE':
      return 'Ese producto no se vende: no se marca como agotado.'
    default:
      return e.estado === 403 ? 'Tus permisos no incluyen marcar agotados. Si te los acaban de dar, vuelve a entrar.' : porDefecto
  }
}

interface Fila {
  id: string
  nombre: string
  detalle: string | null
}

export function PantallaDisponibilidad() {
  const { sesion, tienePermiso } = useSesion()
  const token = sesion!.token
  const puedeMarcar = tienePermiso('menu.marcar_agotado')

  const [productos, setProductos] = useState<Producto[]>([])
  const [agotados, setAgotados] = useState<Agotado[]>([])
  const [menuHoy, setMenuHoy] = useState<Fila[] | null>(null)
  const [motivos, setMotivos] = useState<Motivo[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [accionError, setAccionError] = useState('')
  const [trabajandoId, setTrabajandoId] = useState<string | null>(null)
  const [reactivando, setReactivando] = useState<Fila | null>(null)
  const [busqueda, setBusqueda] = useState('')

  function cargar() {
    setCargando(true)
    setError('')
    Promise.all([
      listarProductos(token),
      listarAgotados(token),
      obtenerMenuDelDia(token, hoyTexto()),
      puedeMarcar ? listarMotivos(token, 'disponibilidad') : Promise.resolve([]),
    ])
      .then(([p, a, m, mot]) => {
        setProductos(p.filter((x) => x.esVendible && x.activo))
        setAgotados(a)
        setMenuHoy(
          m
            ? m.opciones.map((o) => ({
                id: o.productoId,
                nombre: o.productoNombre,
                detalle: o.tipoMenu === 'extra' ? 'Segundo del extra' : (PARTE[o.componente] ?? o.componente),
              }))
            : null,
        )
        setMotivos(mot.filter((x) => x.activo))
      })
      .catch((e) =>
        setError(
          e instanceof ErrorApi && e.cuerpo?.error === 'SIN_LOCAL_ACTIVO'
            ? 'Tu sesión no tiene un local activo. Elige uno en el encabezado.'
            : 'No se pudo cargar la disponibilidad.',
        ),
      )
      .finally(() => setCargando(false))
  }

  // puedeMarcar va en las dependencias: justo despues del login los permisos
  // pueden llegar un render despues del token, y sin esto los motivos nunca se
  // cargaban (el modal decia "no hay motivos" -- encontrado al probar).
  useEffect(cargar, [token, sesion?.localActivoId, puedeMarcar])

  const agotadoPorId = useMemo(() => new Map(agotados.map((a) => [a.productoId, a])), [agotados])
  const enMenu = new Set((menuHoy ?? []).map((f) => f.id))
  const q = busqueda.trim().toLowerCase()
  const coincide = (f: Fila) => !q || f.nombre.toLowerCase().includes(q)
  const carta: Fila[] = productos
    .filter((p) => !enMenu.has(p.id) && !p.tipoMenu)
    .map((p) => ({ id: p.id, nombre: p.nombre, detalle: null }))
    .sort((a, b) => a.nombre.localeCompare(b.nombre))
  const menus: Fila[] = productos
    .filter((p) => p.tipoMenu)
    .map((p) => ({ id: p.id, nombre: p.nombre, detalle: 'Agotarlo deja de vender el menú completo' }))

  async function agotar(fila: Fila) {
    setTrabajandoId(fila.id)
    setAccionError('')
    try {
      await marcarAgotado(token, fila.id)
      setAgotados(await listarAgotados(token))
    } catch (e) {
      setAccionError(mensajeError(e, 'No se pudo marcar como agotado.'))
    } finally {
      setTrabajandoId(null)
    }
  }

  function Seccion({ titulo, filas, vacio }: { titulo: string; filas: Fila[]; vacio?: string }) {
    const visibles = filas.filter(coincide)
    if (visibles.length === 0 && !vacio) return null
    return (
      <section className="panel-admin-tarjeta disp-seccion">
        <h2>{titulo}</h2>
        {visibles.length === 0 ? (
          <p className="prod-nota">{q ? 'Nada coincide con la búsqueda.' : vacio}</p>
        ) : (
          <ul className="disp-lista">
            {visibles.map((f) => {
              const agotado = agotadoPorId.get(f.id)
              return (
                <li key={f.id} className={`disp-fila ${agotado ? 'disp-fila-agotada' : ''}`}>
                  <div className="disp-nombre">
                    <strong>{f.nombre}</strong>
                    {f.detalle && <span className="prod-nota">{f.detalle}</span>}
                  </div>
                  {agotado ? (
                    <span className="disp-estado">
                      <Insignia tono="inactivo">Agotado</Insignia>
                      <span className="prod-nota">{hora(agotado.marcadoEn)}</span>
                    </span>
                  ) : (
                    <Insignia tono="activo">Disponible</Insignia>
                  )}
                  {puedeMarcar &&
                    (agotado ? (
                      <Boton variante="secundario" disabled={trabajandoId !== null} onClick={() => setReactivando(f)}>
                        Volver a ofrecer
                      </Boton>
                    ) : (
                      <Boton
                        variante="peligro"
                        cargando={trabajandoId === f.id}
                        disabled={trabajandoId !== null}
                        onClick={() => agotar(f)}
                      >
                        Marcar agotado
                      </Boton>
                    ))}
                </li>
              )
            })}
          </ul>
        )}
      </section>
    )
  }

  return (
    <section className="panel-admin">
      <div className="panel-admin-encabezado">
        <div>
          <p className="categorias-migaja">Menú semanal</p>
          <h1>Disponibilidad</h1>
          <p className="panel-admin-subtitulo">
            Lo que se acaba en este local se marca aquí: el mozo deja de ofrecerlo en los pedidos nuevos. Lo que ya se
            pidió no cambia. Los otros locales no se enteran.
          </p>
        </div>
      </div>

      {cargando ? (
        <EstadoCargando />
      ) : error ? (
        <EstadoError mensaje={error} accion={<Boton onClick={cargar}>Reintentar</Boton>} />
      ) : productos.length === 0 ? (
        <div className="panel-admin-tarjeta">
          <EstadoVacio titulo="No hay productos que se vendan" descripcion="Cuando el catálogo tenga platos, aparecen aquí." />
        </div>
      ) : (
        <>
          {accionError && <Aviso tono="error">{accionError}</Aviso>}
          {!puedeMarcar && (
            <Aviso tono="informacion">Puedes ver qué está agotado, pero no marcarlo con tus permisos.</Aviso>
          )}
          <div className="disp-barra">
            <label className="prod-buscador">
              <IconoBusqueda width={15} height={15} aria-hidden="true" />
              <input
                type="text"
                aria-label="Buscar producto"
                placeholder="Buscar un plato"
                value={busqueda}
                onChange={(e) => setBusqueda(e.target.value)}
              />
            </label>
            <span className="prod-nota" aria-live="polite">
              {agotados.length === 0 ? 'Nada agotado' : agotados.length === 1 ? '1 agotado' : `${agotados.length} agotados`}
            </span>
          </div>
          <Seccion
            titulo="Almuerzo de hoy"
            filas={menuHoy ?? []}
            vacio="No hay un menú confirmado para hoy. Se programa en Menú semanal → Programación semanal."
          />
          <Seccion titulo="Menús" filas={menus} />
          <Seccion titulo="Carta y extras" filas={carta} />
        </>
      )}

      {reactivando && (
        <ModalReactivar
          producto={reactivando}
          motivos={motivos}
          onCancelar={() => setReactivando(null)}
          onConfirmar={async (motivoId, observacion) => {
            await reactivarProducto(token, reactivando.id, motivoId, observacion)
            setAgotados(await listarAgotados(token))
            setReactivando(null)
          }}
        />
      )}
    </section>
  )
}

function ModalReactivar({
  producto,
  motivos,
  onCancelar,
  onConfirmar,
}: {
  producto: Fila
  motivos: Motivo[]
  onCancelar: () => void
  onConfirmar: (motivoId: string, observacion: string | null) => Promise<void>
}) {
  const [motivoId, setMotivoId] = useState(motivos.length === 1 ? motivos[0].id : '')
  const [observacion, setObservacion] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  const motivo = motivos.find((m) => m.id === motivoId)

  async function confirmar() {
    if (!motivoId) {
      setError('Elige un motivo.')
      return
    }
    if (motivo?.requiereObservacion && !observacion.trim()) {
      setError('Ese motivo pide una observación.')
      return
    }
    setGuardando(true)
    setError('')
    try {
      await onConfirmar(motivoId, observacion.trim() || null)
    } catch (e) {
      setError(mensajeError(e, 'No se pudo volver a ofrecer.'))
      setGuardando(false)
    }
  }

  return (
    <Modal
      abierto
      titulo={`Volver a ofrecer ${producto.nombre}`}
      onCerrar={onCancelar}
      pie={
        <>
          <Boton variante="secundario" onClick={onCancelar} disabled={guardando}>
            Cancelar
          </Boton>
          <Boton cargando={guardando} onClick={confirmar} disabled={motivos.length === 0}>
            Volver a ofrecer
          </Boton>
        </>
      }
    >
      {motivos.length === 0 ? (
        <Aviso>
          No hay motivos de disponibilidad activos. El administrador central los carga en Configuración → Motivos.
        </Aviso>
      ) : (
        <>
          <Select etiqueta="Motivo" value={motivoId} onChange={(e) => setMotivoId(e.target.value)} error={error || undefined}>
            <option value="">Elige un motivo…</option>
            {motivos.map((m) => (
              <option key={m.id} value={m.id}>
                {m.descripcion}
              </option>
            ))}
          </Select>
          <Campo
            etiqueta={motivo?.requiereObservacion ? 'Observación' : 'Observación (opcional)'}
            value={observacion}
            maxLength={250}
            onChange={(e) => setObservacion(e.target.value)}
          />
          <p className="prod-nota">Queda en la bitácora con tu usuario y la hora.</p>
        </>
      )}
    </Modal>
  )
}
