import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { Ban, ChefHat, CircleCheck, CircleX, CreditCard, Info, Package, Plus, Receipt, RotateCcw, Store, Tag, type LucideIcon } from 'lucide-react'
import { useSesion } from '../../comun/sesion/SesionContexto'
import { Boton, EstadoCargando, EstadoError, IconoMasOpciones, IconoReactivar, Interruptor } from '../../comun/componentes'
import {
  ErrorApi,
  activarMotivo,
  actualizarMotivo,
  crearMotivo,
  desactivarMotivo,
  listarMotivos,
  type Motivo,
} from '../../api/cliente'

// Los ocho tipos son fijos -- los define el CHECK de la columna `tipo` en motivo (V13, ampliado en
// V23 con "disponibilidad"), no el usuario. Sin datos sembrados a proposito (RF-005 pendiente), salvo
// uno de disponibilidad (V23: sin el, nadie podria reactivar un agotado). La descripcion de cada tipo
// dice CUANDO se pide, que es lo que orienta a quien configura mientras el dueno del negocio no
// defina los motivos reales.
const TIPOS: { valor: string; titulo: string; descripcion: string; Icono: LucideIcon }[] = [
  { valor: 'ajuste_inventario', titulo: 'Ajuste de inventario', descripcion: 'Se piden al ajustar el inventario: mermas, conteos, etc.', Icono: Package },
  { valor: 'anulacion', titulo: 'Anulación', descripcion: 'Se piden al anular una venta o un pedido ya registrado.', Icono: CircleX },
  { valor: 'descuento', titulo: 'Descuento', descripcion: 'Se piden al aplicar un descuento manual sobre una venta.', Icono: Tag },
  { valor: 'rechazo_abono', titulo: 'Rechazo de abono', descripcion: 'Se piden al rechazar el abono de un pensionista (p. ej. comprobante inválido).', Icono: CreditCard },
  { valor: 'sobrante_produccion', titulo: 'Sobrante de producción', descripcion: 'Se piden al cerrar un lote de producción con sobrante.', Icono: ChefHat },
  { valor: 'reapertura', titulo: 'Reapertura', descripcion: 'Se piden al reabrir un turno, una comanda o un cierre de caja.', Icono: RotateCcw },
  { valor: 'disponibilidad', titulo: 'Disponibilidad', descripcion: 'Se piden al volver a ofrecer un producto que estaba agotado en un local.', Icono: Store },
  { valor: 'ajuste_cuenta', titulo: 'Ajuste de cuenta', descripcion: 'Se piden al corregir la cuenta de un pensionista (cobro de más, error de digitación).', Icono: Receipt },
]

interface Formulario {
  tipo: string
  /** null = motivo nuevo. */
  id: string | null
  nombre: string
  obs: boolean
  error: string
}

function mensajeError(error: unknown, porDefecto: string): string {
  if (error instanceof ErrorApi) {
    switch (error.cuerpo?.error) {
      case 'DESCRIPCION_DUPLICADA':
        return 'Ya existe un motivo activo con ese nombre en este tipo.'
      case 'TIPO_INVALIDO':
        return 'Ese tipo de motivo no existe.'
      case 'NO_ENCONTRADO':
        return 'Este motivo ya no existe. Recarga la página.'
      case 'ULTIMO_DE_DISPONIBILIDAD':
        return 'Debe quedar al menos un motivo activo de Disponibilidad: sin él nadie podría volver a habilitar un producto agotado.'
      default:
        return porDefecto
    }
  }
  return porDefecto
}

/** Motivos de operación (US-012): las razones que el personal indica al anular, ajustar, descontar, etc. */
export function PantallaMotivos() {
  const { sesion } = useSesion()
  const token = sesion!.token

  const [motivos, setMotivos] = useState<Motivo[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [verInactivos, setVerInactivos] = useState(false)
  const [form, setForm] = useState<Formulario | null>(null)
  const [guardando, setGuardando] = useState(false)
  const [menuId, setMenuId] = useState<string | null>(null)
  const [mensaje, setMensaje] = useState('')

  function cargar() {
    setError('')
    listarMotivos(token)
      .then(setMotivos)
      .catch(() => setError('No se pudieron cargar los motivos.'))
      .finally(() => setCargando(false))
  }

  useEffect(cargar, [token])

  useEffect(() => {
    if (!mensaje) return
    const t = setTimeout(() => setMensaje(''), 2800)
    return () => clearTimeout(t)
  }, [mensaje])

  // Un menú abierto se cierra con Escape o al hacer clic fuera.
  useEffect(() => {
    if (!menuId) return
    const alClic = (e: MouseEvent) => {
      if (!(e.target as HTMLElement).closest('[data-menu-motivo]')) setMenuId(null)
    }
    const alTeclear = (e: KeyboardEvent) => e.key === 'Escape' && setMenuId(null)
    document.addEventListener('mousedown', alClic)
    document.addEventListener('keydown', alTeclear)
    return () => {
      document.removeEventListener('mousedown', alClic)
      document.removeEventListener('keydown', alTeclear)
    }
  }, [menuId])

  const totalActivos = motivos.filter((m) => m.activo).length

  const tarjetas = useMemo(
    () =>
      TIPOS.map((t) => {
        const todos = motivos.filter((m) => m.tipo === t.valor)
        return {
          ...t,
          filas: todos.filter((m) => verInactivos || m.activo),
          activos: todos.filter((m) => m.activo).length,
        }
      }),
    [motivos, verInactivos],
  )
  // Los tipos con motivos (o con el formulario abierto) van como tarjeta; los vacíos, compactos abajo.
  const conMotivos = tarjetas.filter((t) => t.filas.length > 0 || form?.tipo === t.valor)
  const vacios = tarjetas.filter((t) => !(t.filas.length > 0 || form?.tipo === t.valor))

  async function guardar() {
    if (!form) return
    const nombre = form.nombre.trim()
    if (!nombre) {
      setForm({ ...form, error: 'Escribe el nombre del motivo.' })
      return
    }
    setGuardando(true)
    try {
      if (form.id) await actualizarMotivo(token, form.id, { descripcion: nombre, requiereObservacion: form.obs })
      else await crearMotivo(token, { tipo: form.tipo, descripcion: nombre, requiereObservacion: form.obs })
      setMensaje(form.id ? 'Motivo actualizado' : 'Motivo agregado')
      setForm(null)
      cargar()
    } catch (e) {
      setForm({ ...form, error: mensajeError(e, 'No se pudo guardar el motivo.') })
    } finally {
      setGuardando(false)
    }
  }

  async function alternar(m: Motivo) {
    setMenuId(null)
    try {
      if (m.activo) await desactivarMotivo(token, m.id)
      else await activarMotivo(token, m.id)
      setMensaje(`${m.descripcion} ${m.activo ? 'desactivado' : 'reactivado'}`)
      cargar()
    } catch (e) {
      setMensaje(mensajeError(e, 'No se pudo cambiar el estado.'))
    }
  }

  return (
    <section className="panel-admin mot-pagina">
      <div className="mot-cabecera">
        <div>
          <nav className="loc-migas" aria-label="Ruta">
            <Link to="/admin/configuracion" className="cfg-miga-enlace">
              Configuración
            </Link>
            <span aria-hidden="true">/</span>
            <span>Motivos</span>
          </nav>
          <h1 className="cfg-titulo pc-titulo">Motivos de operación</h1>
          <p className="cfg-subtitulo">Define las razones que el personal debe indicar en cada proceso.</p>
        </div>
        {!cargando && !error && (
          <div className="mot-resumen">
            <label className="mot-inactivos">
              <Interruptor etiqueta="Ver inactivos" checked={verInactivos} onChange={(e) => setVerInactivos(e.target.checked)} />
              Ver inactivos
            </label>
            <span>
              {TIPOS.length} tipos · {totalActivos} {totalActivos === 1 ? 'motivo activo' : 'motivos activos'}
            </span>
          </div>
        )}
      </div>

      {cargando ? (
        <EstadoCargando />
      ) : error ? (
        <EstadoError mensaje={error} accion={<Boton onClick={cargar}>Reintentar</Boton>} />
      ) : (
        <>
          <div className="mot-columnas">
            {conMotivos.map((t) => {
              const abierto = form?.tipo === t.valor
              return (
                <section key={t.valor} className="mot-tarjeta" aria-label={t.titulo}>
                  <div className="mot-tarjeta-cabecera">
                    <span className="mot-icono" aria-hidden="true">
                      <t.Icono size={32} strokeWidth={1.6} />
                    </span>
                    <div className="mot-titulo">
                      <div>
                        <h2>{t.titulo}</h2>
                        <span className={`mot-contador ${t.activos > 0 ? 'con' : ''}`}>
                          {t.activos} {t.activos === 1 ? 'activo' : 'activos'}
                        </span>
                      </div>
                      <p>{t.descripcion}</p>
                    </div>
                    <button
                      type="button"
                      className="as-boton-borde"
                      disabled={abierto}
                      onClick={() => setForm({ tipo: t.valor, id: null, nombre: '', obs: false, error: '' })}
                    >
                      <Plus size={16} strokeWidth={2.2} aria-hidden="true" /> Agregar motivo
                    </button>
                  </div>

                  {abierto && form && (
                    <div className="mot-form">
                      <label>
                        <span>Nombre del motivo</span>
                        <input
                          type="text"
                          autoFocus
                          placeholder="Ej. Pedido duplicado"
                          value={form.nombre}
                          aria-invalid={Boolean(form.error) || undefined}
                          className={form.error ? 'invalido' : ''}
                          onChange={(e) => setForm({ ...form, nombre: e.target.value, error: '' })}
                          onKeyDown={(e) => {
                            if (e.key === 'Enter') {
                              e.preventDefault()
                              void guardar()
                            } else if (e.key === 'Escape') setForm(null)
                          }}
                        />
                        {form.error && <span className="campo-error">{form.error}</span>}
                      </label>
                      <div className="mot-form-fila">
                        <div className="mot-form-obs">
                          <label className="mot-checkbox">
                            <input type="checkbox" checked={form.obs} onChange={(e) => setForm({ ...form, obs: e.target.checked })} />
                            <span>Exigir observación</span>
                          </label>
                          <span>El personal deberá escribir una observación al usar este motivo.</span>
                          <small>Enter guarda · Esc cancela</small>
                        </div>
                        <Boton variante="secundario" onClick={() => setForm(null)} disabled={guardando}>
                          Cancelar
                        </Boton>
                        <Boton cargando={guardando} onClick={() => void guardar()}>
                          {form.id ? 'Guardar cambios' : 'Guardar'}
                        </Boton>
                      </div>
                    </div>
                  )}

                  {t.valor === 'disponibilidad' && (
                    <div className="pc-aviso" role="note">
                      <Info size={17} aria-hidden="true" />
                      <span>Debe quedar al menos un motivo activo: sin él nadie podría volver a habilitar un producto agotado.</span>
                    </div>
                  )}

                  {t.filas.length > 0 && (
                    <div className="mot-tabla" role="table" aria-label={`Motivos de ${t.titulo}`}>
                      <div className="mot-fila mot-fila-cabecera" role="row">
                        <span role="columnheader">Motivo</span>
                        <span role="columnheader">Observación</span>
                        <span role="columnheader">Estado</span>
                        <span role="columnheader" className="mot-acc">
                          Acciones
                        </span>
                      </div>
                      {t.filas.map((m) => (
                        <div key={m.id} className={`mot-fila ${m.activo ? '' : 'inactivo'}`} role="row">
                          <strong role="cell">{m.descripcion}</strong>
                          <span role="cell" className="as-suave">
                            {m.requiereObservacion ? 'Requerida' : '—'}
                          </span>
                          <span role="cell">
                            <span className={`mot-estado ${m.activo ? 'activo' : ''}`}>
                              <i aria-hidden="true" /> {m.activo ? 'Activo' : 'Inactivo'}
                            </span>
                          </span>
                          <span role="cell" className="mot-acc" data-menu-motivo>
                            <button
                              type="button"
                              className="as-enlace mot-editar"
                              onClick={() => setForm({ tipo: m.tipo, id: m.id, nombre: m.descripcion, obs: m.requiereObservacion, error: '' })}
                            >
                              Editar
                            </button>
                            <button
                              type="button"
                              className="as-icono-boton"
                              aria-label={`Más acciones de ${m.descripcion}`}
                              aria-haspopup="menu"
                              aria-expanded={menuId === m.id}
                              onClick={() => setMenuId(menuId === m.id ? null : m.id)}
                            >
                              <IconoMasOpciones width={16} height={16} aria-hidden="true" />
                            </button>
                            {menuId === m.id && (
                              <div className="loc-menu-lista mot-menu" role="menu">
                                <button type="button" role="menuitem" onClick={() => void alternar(m)}>
                                  {m.activo ? <Ban size={16} aria-hidden="true" /> : <CircleCheck size={16} aria-hidden="true" />}
                                  {m.activo ? 'Desactivar' : 'Reactivar'}
                                </button>
                              </div>
                            )}
                          </span>
                        </div>
                      ))}
                    </div>
                  )}
                </section>
              )
            })}
          </div>

          {vacios.length > 0 && (
            <section className="mot-vacios" aria-labelledby="mot-vacios">
              <div>
                <h2 id="mot-vacios">Sin motivos todavía</h2>
                <p>Agrega los motivos que podrá seleccionar el personal en estos procesos.</p>
              </div>
              <div className="mot-vacios-grilla">
                {vacios.map((t) => (
                  <div key={t.valor} className="mot-vacio">
                    <span className="mot-icono" aria-hidden="true">
                      <t.Icono size={26} strokeWidth={1.6} />
                    </span>
                    <div>
                      <strong>{t.titulo}</strong>
                      <span>{t.descripcion}</span>
                    </div>
                    <button
                      type="button"
                      className="as-boton-borde"
                      onClick={() => setForm({ tipo: t.valor, id: null, nombre: '', obs: false, error: '' })}
                    >
                      <Plus size={15} strokeWidth={2.2} aria-hidden="true" /> Agregar motivo
                    </button>
                  </div>
                ))}
              </div>
            </section>
          )}
        </>
      )}

      {mensaje && (
        <div className="loc-toast" role="status">
          <IconoReactivar width={16} height={16} aria-hidden="true" /> {mensaje}
        </div>
      )}
    </section>
  )
}
