import { useEffect, useMemo, useState } from 'react'
import { useSesion } from '../../comun/sesion/SesionContexto'
import {
  Aviso,
  Boton,
  Campo,
  EstadoCargando,
  EstadoError,
  EstadoVacio,
  IconoFlechaDerecha,
  IconoFlechaIzquierda,
  IconoMas,
  Insignia,
  Modal,
  ModalDeConfirmacion,
  Radio,
  Select,
} from '../../comun/componentes'
import {
  ErrorApi,
  actualizarOpcionMenu,
  agregarOpcionMenu,
  confirmarSemanaMenu,
  crearSemanaMenu,
  descartarSemanaMenu,
  listarCategorias,
  listarProductos,
  obtenerSemanaMenu,
  quitarOpcionMenu,
  type AdvertenciaMenu,
  type OpcionMenu,
  type Producto,
  type SemanaMenu,
} from '../../api/cliente'

/**
 * US-013. Una semana de lunes a domingo, un dia por tarjeta. Las reglas
 * (dias cerrados, repeticiones, estado en servicio) las calcula el backend
 * y llegan en la semana; esta pantalla solo las muestra.
 */

const COMPONENTES = [
  { valor: 'entrada', titulo: 'Entradas' },
  { valor: 'segundo', titulo: 'Segundos' },
  { valor: 'guarnicion', titulo: 'Guarniciones' },
  { valor: 'refresco', titulo: 'Refrescos' },
  { valor: 'postre', titulo: 'Postres' },
] as const

const TITULO_COMPONENTE_SINGULAR: Record<string, string> = {
  entrada: 'Entrada',
  segundo: 'Segundo',
  guarnicion: 'Guarnición',
  refresco: 'Refresco',
  postre: 'Postre',
}

const ESTADO: Record<SemanaMenu['estadoVisible'], { texto: string; tono: 'activo' | 'inactivo' | 'pendiente' }> = {
  borrador: { texto: 'Borrador', tono: 'pendiente' },
  confirmado: { texto: 'Confirmada', tono: 'activo' },
  en_servicio: { texto: 'En servicio', tono: 'activo' },
  pasada: { texto: 'Pasada', tono: 'inactivo' },
}

function mensajeError(e: unknown, porDefecto: string): string {
  if (!(e instanceof ErrorApi)) return porDefecto
  switch (e.cuerpo?.error) {
    case 'DIA_CERRADO':
      return 'Ese día ya pasó: su menú no se cambia.'
    case 'OPCION_REPETIDA':
      return 'Ese plato ya está programado ese día.'
    case 'EXTRA_SOLO_EN_SEGUNDO':
      return 'Solo el segundo puede ser del menú extra.'
    case 'PRODUCTO_NO_ES_DEL_MENU':
      return 'Ese producto no es una entrada, segundo, guarnición, refresco ni postre.'
    case 'PORCIONES_INVALIDAS':
      return 'Las porciones tienen que ser un número entero mayor que cero.'
    case 'SEMANA_PASADA':
      return 'Esa semana ya terminó.'
    case 'SEMANA_YA_EXISTE':
      return 'Esa semana ya existe. Recarga la página.'
    case 'SEMANA_VACIA':
      return 'Programa al menos un día antes de confirmar.'
    case 'YA_CONFIRMADA':
      return 'La semana ya estaba confirmada.'
    case 'SEMANA_CONFIRMADA':
      return 'Una semana confirmada no se descarta: el POS ya puede estar usándola.'
    default:
      return e.estado === 403 ? 'Tus permisos no alcanzan para esto.' : porDefecto
  }
}

// ---- Fechas (sin zona: 'AAAA-MM-DD' se trata como fecha local) -------------

function aTexto(fecha: Date): string {
  const m = String(fecha.getMonth() + 1).padStart(2, '0')
  const d = String(fecha.getDate()).padStart(2, '0')
  return `${fecha.getFullYear()}-${m}-${d}`
}

function desdeTexto(texto: string): Date {
  const [a, m, d] = texto.split('-').map(Number)
  return new Date(a, m - 1, d)
}

function lunesDe(fecha: Date): string {
  const copia = new Date(fecha.getFullYear(), fecha.getMonth(), fecha.getDate())
  copia.setDate(copia.getDate() - ((copia.getDay() + 6) % 7))
  return aTexto(copia)
}

function sumarDias(texto: string, dias: number): string {
  const f = desdeTexto(texto)
  f.setDate(f.getDate() + dias)
  return aTexto(f)
}

function diaCorto(texto: string): string {
  return desdeTexto(texto).toLocaleDateString('es-PE', { weekday: 'long', day: 'numeric', month: 'short' })
}

function rangoSemana(lunes: string): string {
  const inicio = desdeTexto(lunes)
  const fin = desdeTexto(sumarDias(lunes, 6))
  const dia = (f: Date) => f.toLocaleDateString('es-PE', { day: 'numeric', month: 'long' })
  return `${dia(inicio)} al ${dia(fin)} de ${fin.getFullYear()}`
}

function textoAdvertencia(a: AdvertenciaMenu): string {
  // Sin punto final: la fecha abreviada ya termina en punto ("29 set.").
  return `${a.productoNombre} se repite el ${diaCorto(a.fecha)}: ya se sirvió el ${diaCorto(a.repetidoEl)}`
}

// ---- Pantalla ---------------------------------------------------------------

export function PantallaProgramacionSemanal() {
  const { sesion, tienePermiso } = useSesion()
  const token = sesion!.token
  const puedeCrear = tienePermiso('menu.crear')
  const puedeModificar = tienePermiso('menu.modificar')
  const puedeQuitar = tienePermiso('menu.desactivar')

  const [lunes, setLunes] = useState(() => lunesDe(new Date()))
  const [semana, setSemana] = useState<SemanaMenu | null>(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [accionError, setAccionError] = useState('')
  const [trabajando, setTrabajando] = useState(false)
  const [platos, setPlatos] = useState<(Producto & { componente: string })[]>([])
  const [agregandoEn, setAgregandoEn] = useState<string | null>(null)
  const [editando, setEditando] = useState<OpcionMenu | null>(null)
  const [confirmando, setConfirmando] = useState(false)
  const [descartando, setDescartando] = useState(false)
  const [confirmadaAhora, setConfirmadaAhora] = useState(false)

  const lunesActual = lunesDe(new Date())
  const semanaTerminada = sumarDias(lunes, 6) < aTexto(new Date())

  function cargar() {
    setCargando(true)
    setError('')
    setAccionError('')
    setConfirmadaAhora(false)
    obtenerSemanaMenu(token, lunes)
      .then(setSemana)
      .catch(() => setError('No se pudo cargar la semana.'))
      .finally(() => setCargando(false))
  }

  useEffect(cargar, [token, lunes])

  // Los platos solo hacen falta para programar: se cargan una vez, si se puede.
  useEffect(() => {
    if (!puedeModificar) return
    Promise.all([listarProductos(token), listarCategorias(token)])
      .then(([productos, categorias]) => {
        const componentePorCategoria = new Map(
          categorias
            .filter((c) => c.naturaleza === 'componente_menu' && c.componenteMenu)
            .map((c) => [c.id, c.componenteMenu as string]),
        )
        setPlatos(
          productos
            .filter((p) => p.activo && componentePorCategoria.has(p.categoriaId))
            .map((p) => ({ ...p, componente: componentePorCategoria.get(p.categoriaId)! }))
            .sort((a, b) => a.nombre.localeCompare(b.nombre)),
        )
      })
      .catch(() => setPlatos([]))
  }, [token, puedeModificar])

  const repetidos = useMemo(
    () => new Set((semana?.advertencias ?? []).map((a) => `${a.fecha}|${a.productoId}`)),
    [semana],
  )

  async function ejecutar(accion: () => Promise<SemanaMenu | null | void>, porDefecto: string) {
    setTrabajando(true)
    setAccionError('')
    try {
      const resultado = await accion()
      if (resultado !== undefined) setSemana(resultado)
      return true
    } catch (e) {
      setAccionError(mensajeError(e, porDefecto))
      return false
    } finally {
      setTrabajando(false)
    }
  }

  const estado = semana ? ESTADO[semana.estadoVisible] : null
  const esBorrador = semana?.estado === 'borrador'

  return (
    <section className="panel-admin">
      <div className="panel-admin-encabezado">
        <div>
          <p className="categorias-migaja">Menú semanal</p>
          <h1>Programación semanal</h1>
          <p className="panel-admin-subtitulo">
            El almuerzo de cada día: sus opciones y cuántas porciones se esperan. Desayuno y cena no se programan: se
            venden de la carta.
          </p>
        </div>
      </div>

      <div className="prog-navegacion">
        <Boton variante="secundario" onClick={() => setLunes(sumarDias(lunes, -7))} aria-label="Semana anterior">
          <IconoFlechaIzquierda width={16} height={16} aria-hidden="true" />
        </Boton>
        <div className="prog-semana-titulo">
          <strong>Semana del {rangoSemana(lunes)}</strong>
          {estado && <Insignia tono={estado.tono}>{estado.texto}</Insignia>}
        </div>
        <Boton variante="secundario" onClick={() => setLunes(sumarDias(lunes, 7))} aria-label="Semana siguiente">
          <IconoFlechaDerecha width={16} height={16} aria-hidden="true" />
        </Boton>
        {lunes !== lunesActual && (
          <Boton variante="texto" onClick={() => setLunes(lunesActual)}>
            Ir a esta semana
          </Boton>
        )}
        <div className="prod-espaciador" />
        {semana && esBorrador && puedeQuitar && (
          <Boton variante="secundario" onClick={() => setDescartando(true)} disabled={trabajando}>
            Descartar borrador
          </Boton>
        )}
        {semana && esBorrador && puedeModificar && (
          <Boton onClick={() => setConfirmando(true)} disabled={trabajando}>
            Confirmar semana
          </Boton>
        )}
      </div>

      {accionError && <Aviso tono="error">{accionError}</Aviso>}
      {confirmadaAhora && (
        <Aviso tono="exito">Semana confirmada: desde ahora el POS ofrece este menú. Se puede seguir ajustando.</Aviso>
      )}

      {cargando ? (
        <EstadoCargando />
      ) : error ? (
        <EstadoError mensaje={error} accion={<Boton onClick={cargar}>Reintentar</Boton>} />
      ) : !semana ? (
        <div className="panel-admin-tarjeta">
          <EstadoVacio
            titulo={semanaTerminada ? 'Esta semana ya pasó sin programarse' : 'Esta semana todavía no tiene menú'}
            descripcion={
              semanaTerminada
                ? 'Las semanas que ya terminaron no se crean.'
                : 'Créala y programa los días que haya almuerzo. Nace en borrador: el POS no la ve hasta confirmarla.'
            }
            accion={
              !semanaTerminada && puedeCrear ? (
                <Boton
                  cargando={trabajando}
                  onClick={() => ejecutar(() => crearSemanaMenu(token, lunes), 'No se pudo crear la semana.')}
                >
                  <IconoMas width={16} height={16} aria-hidden="true" /> Crear semana
                </Boton>
              ) : undefined
            }
          />
        </div>
      ) : (
        <>
          {semana.advertencias.length > 0 && (
            <Aviso>
              <strong>Segundos que se repiten en menos de cuatro días:</strong>
              <ul className="prog-advertencias">
                {semana.advertencias.map((a) => (
                  <li key={`${a.fecha}-${a.productoId}`}>{textoAdvertencia(a)}</li>
                ))}
              </ul>
              Es solo un aviso: se puede confirmar igual.
            </Aviso>
          )}
          <div className="prog-dias">
            {semana.dias.map((dia) => {
              const esHoy = dia.fecha === semana.hoy
              const editable = !dia.cerrado && puedeModificar
              return (
                <article
                  key={dia.fecha}
                  className={`panel-admin-tarjeta prog-dia ${dia.cerrado ? 'prog-dia-cerrado' : ''} ${esHoy ? 'prog-dia-hoy' : ''}`}
                  aria-label={diaCorto(dia.fecha)}
                >
                  <header className="prog-dia-cabecera">
                    <h2>{diaCorto(dia.fecha)}</h2>
                    {esHoy && <span className="prod-pastilla prod-pastilla-exito">Hoy</span>}
                    {dia.cerrado && <span className="prod-nota">Cerrado</span>}
                  </header>
                  {dia.opciones.length === 0 ? (
                    <p className="prod-nota prog-sin-almuerzo">Sin almuerzo programado.</p>
                  ) : (
                    COMPONENTES.map((c) => {
                      const deEste = dia.opciones.filter((o) => o.componente === c.valor)
                      if (deEste.length === 0) return null
                      return (
                        <section key={c.valor} className="prog-componente">
                          <h3>{c.titulo}</h3>
                          <ul>
                            {deEste.map((o) => {
                              const repetido = repetidos.has(`${dia.fecha}|${o.productoId}`)
                              const contenido = (
                                <>
                                  <span className="prog-opcion-nombre">{o.productoNombre}</span>
                                  {o.tipoMenu === 'extra' && (
                                    <span className="prod-pastilla prod-pastilla-subreceta">Extra</span>
                                  )}
                                  {o.porcionesEstimadas != null && (
                                    <span className="prog-porciones">{o.porcionesEstimadas} porc.</span>
                                  )}
                                  {repetido && (
                                    <span className="prog-repetido" title="Se repite en los tres días anteriores">
                                      Se repite
                                    </span>
                                  )}
                                </>
                              )
                              return (
                                <li key={o.id}>
                                  {editable ? (
                                    <button
                                      type="button"
                                      className={`prog-opcion ${repetido ? 'prog-opcion-repetida' : ''}`}
                                      onClick={() => setEditando(o)}
                                    >
                                      {contenido}
                                    </button>
                                  ) : (
                                    <span className={`prog-opcion ${repetido ? 'prog-opcion-repetida' : ''}`}>
                                      {contenido}
                                    </span>
                                  )}
                                </li>
                              )
                            })}
                          </ul>
                        </section>
                      )
                    })
                  )}
                  {editable && (
                    <Boton variante="secundario" className="prog-agregar" onClick={() => setAgregandoEn(dia.fecha)}>
                      <IconoMas width={15} height={15} aria-hidden="true" /> Agregar opción
                    </Boton>
                  )}
                </article>
              )
            })}
          </div>
        </>
      )}

      {semana && agregandoEn && (
        <ModalAgregar
          fecha={agregandoEn}
          platos={platos}
          yaProgramados={new Set(semana.dias.find((d) => d.fecha === agregandoEn)?.opciones.map((o) => o.productoId))}
          onCancelar={() => setAgregandoEn(null)}
          onGuardar={async (datos) => {
            const ok = await ejecutar(
              () => agregarOpcionMenu(token, semana.id, { fecha: agregandoEn, ...datos }),
              'No se pudo agregar la opción.',
            )
            if (ok) setAgregandoEn(null)
            return ok
          }}
          error={accionError}
        />
      )}

      {editando && (
        <ModalEditar
          opcion={editando}
          puedeQuitar={puedeQuitar}
          error={accionError}
          onCancelar={() => setEditando(null)}
          onGuardar={async (datos) => {
            const ok = await ejecutar(() => actualizarOpcionMenu(token, editando.id, datos), 'No se pudo guardar.')
            if (ok) setEditando(null)
          }}
          onQuitar={async () => {
            const ok = await ejecutar(() => quitarOpcionMenu(token, editando.id), 'No se pudo quitar la opción.')
            if (ok) setEditando(null)
          }}
        />
      )}

      {semana && confirmando && (
        <Modal
          abierto
          titulo="Confirmar la semana"
          onCerrar={() => setConfirmando(false)}
          pie={
            <>
              <Boton variante="secundario" onClick={() => setConfirmando(false)} disabled={trabajando}>
                Cancelar
              </Boton>
              <Boton
                cargando={trabajando}
                onClick={async () => {
                  const ok = await ejecutar(
                    () => confirmarSemanaMenu(token, semana.id),
                    'No se pudo confirmar la semana.',
                  )
                  setConfirmando(false)
                  if (ok) setConfirmadaAhora(true)
                }}
              >
                {semana.advertencias.length > 0 ? 'Confirmar igual' : 'Confirmar'}
              </Boton>
            </>
          }
        >
          <p>Desde que la confirmes, el POS ofrece este menú. Después se puede seguir ajustando día por día.</p>
          {semana.advertencias.length > 0 && (
            <Aviso>
              <strong>Hay segundos repetidos:</strong>
              <ul className="prog-advertencias">
                {semana.advertencias.map((a) => (
                  <li key={`${a.fecha}-${a.productoId}`}>{textoAdvertencia(a)}</li>
                ))}
              </ul>
            </Aviso>
          )}
        </Modal>
      )}

      <ModalDeConfirmacion
        abierto={descartando}
        titulo="Descartar el borrador"
        mensaje="Se borra la semana con todo lo programado. Como está en borrador, el POS nunca la vio."
        textoConfirmar="Descartar"
        peligro
        confirmando={trabajando}
        error={accionError || undefined}
        onConfirmar={async () => {
          const ok = await ejecutar(async () => {
            await descartarSemanaMenu(token, semana!.id)
            return null
          }, 'No se pudo descartar la semana.')
          if (ok) setDescartando(false)
        }}
        onCancelar={() => setDescartando(false)}
      />
    </section>
  )
}

// ---- Modales ----------------------------------------------------------------

function ModalAgregar({
  fecha,
  platos,
  yaProgramados,
  error,
  onCancelar,
  onGuardar,
}: {
  fecha: string
  platos: (Producto & { componente: string })[]
  yaProgramados: Set<string>
  error: string
  onCancelar: () => void
  onGuardar: (datos: { productoId: string; tipoMenu: string; porcionesEstimadas: number | null }) => Promise<boolean>
}) {
  const [componente, setComponente] = useState('segundo')
  const [productoId, setProductoId] = useState('')
  const [tipoMenu, setTipoMenu] = useState<'normal' | 'extra'>('normal')
  const [porciones, setPorciones] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [errorLocal, setErrorLocal] = useState('')

  const candidatos = platos.filter((p) => p.componente === componente)

  async function guardar() {
    if (!productoId) {
      setErrorLocal('Elige el plato.')
      return
    }
    const n = porciones.trim() === '' ? null : Number(porciones)
    if (n != null && (!Number.isInteger(n) || n <= 0)) {
      setErrorLocal('Las porciones tienen que ser un número entero mayor que cero.')
      return
    }
    setErrorLocal('')
    setGuardando(true)
    await onGuardar({ productoId, tipoMenu: componente === 'segundo' ? tipoMenu : 'normal', porcionesEstimadas: n })
    setGuardando(false)
  }

  return (
    <Modal
      abierto
      titulo={`Agregar opción · ${diaCorto(fecha)}`}
      onCerrar={onCancelar}
      pie={
        <>
          <Boton variante="secundario" onClick={onCancelar} disabled={guardando}>
            Cancelar
          </Boton>
          <Boton cargando={guardando} onClick={guardar}>
            Agregar
          </Boton>
        </>
      }
    >
      <Select
        etiqueta="Parte del menú"
        value={componente}
        onChange={(e) => {
          setComponente(e.target.value)
          setProductoId('')
        }}
      >
        {COMPONENTES.map((c) => (
          <option key={c.valor} value={c.valor}>
            {TITULO_COMPONENTE_SINGULAR[c.valor]}
          </option>
        ))}
      </Select>
      <Select
        etiqueta="Plato"
        value={productoId}
        onChange={(e) => setProductoId(e.target.value)}
        ayuda={
          candidatos.length === 0
            ? 'No hay platos activos en las categorías de esta parte del menú. Créalos en Productos.'
            : undefined
        }
      >
        <option value="">Elige un plato…</option>
        {candidatos.map((p) => (
          <option key={p.id} value={p.id} disabled={yaProgramados.has(p.id)}>
            {p.nombre}
            {yaProgramados.has(p.id) ? ' (ya está este día)' : ''}
          </option>
        ))}
      </Select>
      {componente === 'segundo' && (
        <fieldset className="prog-tipo">
          <legend className="campo-etiqueta">¿De qué menú?</legend>
          <Radio etiqueta="Menú normal" name="tipo" checked={tipoMenu === 'normal'} onChange={() => setTipoMenu('normal')} />
          <Radio etiqueta="Menú extra" name="tipo" checked={tipoMenu === 'extra'} onChange={() => setTipoMenu('extra')} />
        </fieldset>
      )}
      <Campo
        etiqueta="Porciones estimadas (opcional)"
        type="number"
        min="1"
        step="1"
        inputMode="numeric"
        value={porciones}
        onChange={(e) => setPorciones(e.target.value)}
        error={errorLocal || error || undefined}
      />
    </Modal>
  )
}

function ModalEditar({
  opcion,
  puedeQuitar,
  error,
  onCancelar,
  onGuardar,
  onQuitar,
}: {
  opcion: OpcionMenu
  puedeQuitar: boolean
  error: string
  onCancelar: () => void
  onGuardar: (datos: { tipoMenu: string; porcionesEstimadas: number | null }) => Promise<void>
  onQuitar: () => Promise<void>
}) {
  const [tipoMenu, setTipoMenu] = useState(opcion.tipoMenu)
  const [porciones, setPorciones] = useState(opcion.porcionesEstimadas?.toString() ?? '')
  const [guardando, setGuardando] = useState(false)
  const [errorLocal, setErrorLocal] = useState('')

  async function guardar() {
    const n = porciones.trim() === '' ? null : Number(porciones)
    if (n != null && (!Number.isInteger(n) || n <= 0)) {
      setErrorLocal('Las porciones tienen que ser un número entero mayor que cero.')
      return
    }
    setErrorLocal('')
    setGuardando(true)
    await onGuardar({ tipoMenu, porcionesEstimadas: n })
    setGuardando(false)
  }

  return (
    <Modal
      abierto
      titulo={opcion.productoNombre}
      onCerrar={onCancelar}
      pie={
        <>
          {puedeQuitar && (
            <Boton variante="peligro" onClick={onQuitar} disabled={guardando} className="prog-quitar">
              Quitar del día
            </Boton>
          )}
          <Boton variante="secundario" onClick={onCancelar} disabled={guardando}>
            Cancelar
          </Boton>
          <Boton cargando={guardando} onClick={guardar}>
            Guardar
          </Boton>
        </>
      }
    >
      <p className="prod-nota">{TITULO_COMPONENTE_SINGULAR[opcion.componente]}</p>
      {opcion.componente === 'segundo' && (
        <fieldset className="prog-tipo">
          <legend className="campo-etiqueta">¿De qué menú?</legend>
          <Radio etiqueta="Menú normal" name="tipo-ed" checked={tipoMenu === 'normal'} onChange={() => setTipoMenu('normal')} />
          <Radio etiqueta="Menú extra" name="tipo-ed" checked={tipoMenu === 'extra'} onChange={() => setTipoMenu('extra')} />
        </fieldset>
      )}
      <Campo
        etiqueta="Porciones estimadas (opcional)"
        type="number"
        min="1"
        step="1"
        inputMode="numeric"
        value={porciones}
        autoFocus
        onChange={(e) => setPorciones(e.target.value)}
        error={errorLocal || error || undefined}
      />
    </Modal>
  )
}
