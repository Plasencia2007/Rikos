import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useSesion } from '../../comun/sesion/SesionContexto'
import { Boton, EstadoCargando, EstadoError, EstadoVacio, IconoFlechaAbajo, IconoHorario } from '../../comun/componentes'
import {
  listarLocales,
  listarUsuarios,
  listarAuditoria,
  type Local,
  type Usuario,
  type RegistroAuditoria,
} from '../../api/cliente'

/**
 * ADM-31: los unicos cuatro tipos de evento que ya emite algun modulo este
 * sprint (Producto activar/desactivar, cambios de precio, permisos). Mas
 * tipos "llegan con sus modulos" -- no hay forma de descubrirlos desde el
 * backend sin un endpoint aparte, y no vale la pena crear uno solo para
 * poblar un <select>.
 */
const EVENTOS: { valor: string; etiqueta: string }[] = [
  { valor: 'cambio_estado', etiqueta: 'Cambio de estado' },
  { valor: 'eliminacion', etiqueta: 'Eliminación' },
  { valor: 'cambio_precio', etiqueta: 'Cambio de precio' },
  { valor: 'cambio_permiso', etiqueta: 'Cambio de permiso' },
  { valor: 'cambio_receta', etiqueta: 'Cambio de receta' },
  { valor: 'cambio_plan', etiqueta: 'Cambio de plan tarifario' },
  { valor: 'cambio_parametro', etiqueta: 'Cambio de parámetro de cobro' },
  { valor: 'cambio_credencial', etiqueta: 'Cambio de credencial' },
  { valor: 'marcado_agotado', etiqueta: 'Producto agotado' },
  { valor: 'reactivacion', etiqueta: 'Producto vuelto a ofrecer' },
  { valor: 'movimiento_cuenta', etiqueta: 'Movimiento de cuenta' },
  { valor: 'cambio_limite', etiqueta: 'Cambio de tope de crédito' },
  { valor: 'cambio_mesa', etiqueta: 'Cambio de mesa' },
  { valor: 'bloqueo_mesa', etiqueta: 'Mesa bloqueada o liberada' },
  { valor: 'precio_manual', etiqueta: 'Precio de extra puesto a mano' },
]

const TAMANO_PAGINA = 30

function formatearFechaHora(iso: string): string {
  const fecha = new Date(iso)
  if (Number.isNaN(fecha.getTime())) return iso
  return fecha.toLocaleString('es-PE', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

/** JSON crudo del backend, pretty-print para el detalle -- nunca se reinterpreta, solo se muestra. */
function formatearJson(crudo: string | null): string | null {
  if (!crudo) return null
  try {
    return JSON.stringify(JSON.parse(crudo), null, 2)
  } catch {
    return crudo
  }
}

export function PantallaAuditoria() {
  const { sesion } = useSesion()
  const token = sesion!.token

  const [locales, setLocales] = useState<Local[]>([])
  const [usuarios, setUsuarios] = useState<Usuario[]>([])
  const [cargandoListas, setCargandoListas] = useState(true)

  const [filtroLocal, setFiltroLocal] = useState('')
  const [filtroUsuario, setFiltroUsuario] = useState('')
  // Otras pantallas enlazan aquí con un evento ya elegido (p. ej. ?evento=cambio_parametro).
  const [parametrosUrl] = useSearchParams()
  const [filtroEvento, setFiltroEvento] = useState(() => parametrosUrl.get('evento') ?? '')
  const [desde, setDesde] = useState('')
  const [hasta, setHasta] = useState('')

  const [pagina, setPagina] = useState(0)
  const [registros, setRegistros] = useState<RegistroAuditoria[]>([])
  const [totalPaginas, setTotalPaginas] = useState(0)
  const [totalElementos, setTotalElementos] = useState(0)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')

  const [expandidoId, setExpandidoId] = useState<string | null>(null)

  useEffect(() => {
    listarLocales(token)
      .then(setLocales)
      .catch(() => setLocales([]))
    listarUsuarios(token)
      .then(setUsuarios)
      .catch(() => setUsuarios([]))
      .finally(() => setCargandoListas(false))
  }, [token])

  function cargar() {
    setCargando(true)
    setError('')
    listarAuditoria(token, {
      localId: filtroLocal || undefined,
      usuarioId: filtroUsuario || undefined,
      evento: filtroEvento || undefined,
      // <input type="date"> entrega "YYYY-MM-DD": desde arranca a las 00:00
      // locales y hasta cierra al final del dia, para que el rango incluya
      // todo lo ocurrido ese dia.
      desde: desde ? `${desde}T00:00:00` : undefined,
      hasta: hasta ? `${hasta}T23:59:59` : undefined,
      pagina,
      tamano: TAMANO_PAGINA,
    })
      .then((p) => {
        setRegistros(p.content)
        setTotalPaginas(p.totalPages)
        setTotalElementos(p.totalElements)
      })
      .catch(() => setError('No se pudo cargar la bitácora.'))
      .finally(() => setCargando(false))
  }

  useEffect(cargar, [token, filtroLocal, filtroUsuario, filtroEvento, desde, hasta, pagina])

  // Cualquier cambio de filtro vuelve a la primera pagina -- si no, se
  // puede quedar viendo una pagina 3 que ya no existe con el filtro nuevo.
  useEffect(() => {
    setPagina(0)
  }, [filtroLocal, filtroUsuario, filtroEvento, desde, hasta])

  const hayFiltros = Boolean(filtroLocal || filtroUsuario || filtroEvento || desde || hasta)

  function limpiarFiltros() {
    setFiltroLocal('')
    setFiltroUsuario('')
    setFiltroEvento('')
    setDesde('')
    setHasta('')
  }

  return (
    <section className="panel-admin">
      <div className="panel-admin-encabezado">
        <div>
          <p className="categorias-migaja">Administración</p>
          <h1>Bitácora de auditoría</h1>
          <p className="panel-admin-subtitulo">
            Quién hizo qué, cuándo y dónde. Solo lectura: nada de lo que pasó aquí se puede editar ni borrar.
          </p>
        </div>
      </div>

      <div className="prod-tabla-tarjeta">
        <div className="prod-barra-filtros">
          <select
            className="prod-select"
            aria-label="Local"
            value={filtroLocal}
            onChange={(e) => setFiltroLocal(e.target.value)}
            disabled={cargandoListas}
          >
            <option value="">Todos los locales</option>
            {locales.map((l) => (
              <option key={l.id} value={l.id}>
                {l.nombre}
              </option>
            ))}
          </select>
          <select
            className="prod-select"
            aria-label="Usuario"
            value={filtroUsuario}
            onChange={(e) => setFiltroUsuario(e.target.value)}
            disabled={cargandoListas}
          >
            <option value="">Todos los usuarios</option>
            {usuarios.map((u) => (
              <option key={u.id} value={u.id}>
                {u.nombres} {u.apellidos}
              </option>
            ))}
          </select>
          <select
            className="prod-select"
            aria-label="Tipo de evento"
            value={filtroEvento}
            onChange={(e) => setFiltroEvento(e.target.value)}
          >
            <option value="">Cualquier evento</option>
            {EVENTOS.map((ev) => (
              <option key={ev.valor} value={ev.valor}>
                {ev.etiqueta}
              </option>
            ))}
          </select>
          <label className="bitacora-fecha">
            Desde
            <input type="date" value={desde} max={hasta || undefined} onChange={(e) => setDesde(e.target.value)} />
          </label>
          <label className="bitacora-fecha">
            Hasta
            <input type="date" value={hasta} min={desde || undefined} onChange={(e) => setHasta(e.target.value)} />
          </label>
          {hayFiltros && (
            <button type="button" className="prod-limpiar" onClick={limpiarFiltros}>
              Limpiar filtros
            </button>
          )}
        </div>

        <div className="prod-tabla-scroll">
          {cargando ? (
            <EstadoCargando mensaje="Cargando bitácora…" />
          ) : error ? (
            <EstadoError mensaje={error} accion={<Boton onClick={cargar}>Reintentar</Boton>} />
          ) : registros.length === 0 ? (
            <EstadoVacio
              icono={<IconoHorario width={28} height={28} aria-hidden="true" />}
              titulo={hayFiltros ? 'Nada coincide con el filtro' : 'Todavía no hay nada registrado'}
              descripcion={
                hayFiltros
                  ? 'Prueba con otro local, usuario, evento o rango de fechas.'
                  : 'Las acciones de otros módulos (cambios de estado, precios, permisos…) van a ir apareciendo aquí.'
              }
              accion={hayFiltros ? <Boton variante="secundario" onClick={limpiarFiltros}>Limpiar filtros</Boton> : undefined}
            />
          ) : (
            <ul className="bitacora-lista">
              {registros.map((r) => (
                <FilaAuditoria
                  key={r.id}
                  registro={r}
                  expandido={expandidoId === r.id}
                  onAlternar={() => setExpandidoId((actual) => (actual === r.id ? null : r.id))}
                />
              ))}
            </ul>
          )}
        </div>

        {!cargando && !error && registros.length > 0 && (
          <div className="prod-pie">
            <span>
              Página {pagina + 1} de {totalPaginas} · {totalElementos} registros
            </span>
            <div className="bitacora-paginador">
              <button type="button" disabled={pagina === 0} onClick={() => setPagina((p) => p - 1)}>
                Anterior
              </button>
              <button
                type="button"
                disabled={pagina + 1 >= totalPaginas}
                onClick={() => setPagina((p) => p + 1)}
              >
                Siguiente
              </button>
            </div>
          </div>
        )}
      </div>
    </section>
  )
}

function FilaAuditoria({
  registro,
  expandido,
  onAlternar,
}: {
  registro: RegistroAuditoria
  expandido: boolean
  onAlternar: () => void
}) {
  const anterior = formatearJson(registro.valorAnterior)
  const nuevo = formatearJson(registro.valorNuevo)

  return (
    <li className={`bitacora-fila ${expandido ? 'bitacora-fila-abierta' : ''}`}>
      <button type="button" className="bitacora-fila-cabecera" aria-expanded={expandido} onClick={onAlternar}>
        <div className="bitacora-fila-texto">
          <p className="bitacora-descripcion">{registro.descripcion}</p>
          <p className="bitacora-meta">
            {registro.usuarioNombre ?? 'Sistema'}
            {registro.localNombre && <> · {registro.localNombre}</>} · {formatearFechaHora(registro.ocurridoEn)}
          </p>
        </div>
        <IconoFlechaAbajo className={`bitacora-chevron ${expandido ? 'girado' : ''}`} aria-hidden="true" width={14} height={14} />
      </button>

      {expandido && (
        <div className="bitacora-detalle">
          <div className="bitacora-detalle-dato">
            <span>Entidad</span>
            <strong>
              {registro.entidad}
              {registro.entidadId && ` · ${registro.entidadId}`}
            </strong>
          </div>
          {registro.observacion && (
            <div className="bitacora-detalle-dato">
              <span>Observación</span>
              <strong>{registro.observacion}</strong>
            </div>
          )}
          {anterior && (
            <div className="bitacora-detalle-dato">
              <span>Valor anterior</span>
              <pre className="bitacora-json">{anterior}</pre>
            </div>
          )}
          {nuevo && (
            <div className="bitacora-detalle-dato">
              <span>Valor nuevo</span>
              <pre className="bitacora-json">{nuevo}</pre>
            </div>
          )}
        </div>
      )}
    </li>
  )
}
