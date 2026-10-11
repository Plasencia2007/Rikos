import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useSesion } from '../../comun/sesion/SesionContexto'
import {
  Boton,
  EstadoCargando,
  EstadoError,
  IconoBloquear,
  IconoBorrar,
  IconoBusqueda,
  IconoCambiar,
  IconoCerrar,
  IconoInfo,
  IconoLapiz,
  IconoMasOpciones,
  IconoMas,
  IconoOjo,
  IconoPersonas,
  IconoReactivar,
  IconoTienda,
  IconoUbicacion,
  ModalDeConfirmacion,
} from '../../comun/componentes'
import {
  activarLocal,
  desactivarLocal,
  eliminarLocal,
  ErrorApi,
  listarLocales,
  type Local,
} from '../../api/cliente'

type Estado = 'todos' | 'activos' | 'inactivos'

const CLAVE_AVISO = 'rikos-locales-aviso-cerrado'

function avisoCerrado(): boolean {
  try {
    return localStorage.getItem(CLAVE_AVISO) === '1'
  } catch {
    return false
  }
}

/** US-001: alta, edición y eliminación de locales; y elegir cuál se administra. */
export function PantallaLocales() {
  const { sesion, cambiarLocalActivo, refrescarLocales } = useSesion()
  const token = sesion!.token
  const navigate = useNavigate()

  const [locales, setLocales] = useState<Local[]>([])
  const [cargando, setCargando] = useState(true)
  const [errorCarga, setErrorCarga] = useState('')
  const [busqueda, setBusqueda] = useState('')
  const [estado, setEstado] = useState<Estado>('todos')
  const [aviso, setAviso] = useState(() => !avisoCerrado())
  const [menuAbierto, setMenuAbierto] = useState<string | null>(null)
  const [mensaje, setMensaje] = useState('')
  const [cambiandoId, setCambiandoId] = useState<string | null>(null)

  const [localAEliminar, setLocalAEliminar] = useState<Local | null>(null)
  const [eliminando, setEliminando] = useState(false)
  const [errorEliminar, setErrorEliminar] = useState('')

  async function recargar() {
    setErrorCarga('')
    try {
      setLocales(await listarLocales(token))
    } catch {
      setErrorCarga('No se pudo cargar la lista de locales.')
    } finally {
      setCargando(false)
    }
  }

  useEffect(() => {
    void recargar()
    // Al entrar se trae la lista de locales de la sesion al dia: un local
    // recien creado tiene que aparecer en el selector del encabezado.
    void refrescarLocales()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // El mensaje de confirmacion se apaga solo.
  useEffect(() => {
    if (!mensaje) return
    const t = setTimeout(() => setMensaje(''), 2600)
    return () => clearTimeout(t)
  }, [mensaje])

  function cerrarAviso() {
    setAviso(false)
    try {
      localStorage.setItem(CLAVE_AVISO, '1')
    } catch {
      // no critico
    }
  }

  async function cambiarA(local: Local) {
    setCambiandoId(local.id)
    try {
      await cambiarLocalActivo(local.id)
      setMensaje(`Ahora administras ${local.nombre}`)
    } catch {
      setMensaje('No se pudo cambiar de local.')
    } finally {
      setCambiandoId(null)
    }
  }

  async function alternarActivo(local: Local) {
    setMenuAbierto(null)
    try {
      if (local.activo) await desactivarLocal(token, local.id)
      else await activarLocal(token, local.id)
      setMensaje(`${local.nombre} ${local.activo ? 'desactivado' : 'reactivado'}`)
      await recargar()
      await refrescarLocales()
    } catch {
      setMensaje('No se pudo cambiar el estado del local.')
    }
  }

  function abrirEliminar(local: Local) {
    setMenuAbierto(null)
    setLocalAEliminar(local)
    setErrorEliminar('')
  }

  async function confirmarEliminar() {
    if (!localAEliminar) return
    setEliminando(true)
    setErrorEliminar('')
    try {
      await eliminarLocal(token, localAEliminar.id)
      setLocalAEliminar(null)
      await recargar()
      await refrescarLocales()
    } catch (excepcion) {
      setErrorEliminar(
        excepcion instanceof ErrorApi && excepcion.cuerpo?.error === 'TIENE_HISTORIAL'
          ? 'No se puede eliminar: este local ya tiene usuarios o ventas registradas. Puedes desactivarlo en su lugar.'
          : 'No se pudo eliminar el local.',
      )
    } finally {
      setEliminando(false)
    }
  }

  const totales = useMemo(
    () => ({ total: locales.length, activos: locales.filter((l) => l.activo).length }),
    [locales],
  )

  const filtrados = useMemo(() => {
    const q = busqueda.trim().toLowerCase()
    return locales.filter((l) => {
      if (estado === 'activos' && !l.activo) return false
      if (estado === 'inactivos' && l.activo) return false
      return !q || l.nombre.toLowerCase().includes(q) || l.direccion.toLowerCase().includes(q) || l.codigo.includes(q)
    })
  }, [locales, busqueda, estado])

  const idsAccesibles = new Set(sesion!.locales.map((l) => l.id))

  return (
    <section className="panel-admin loc-pagina">
      <nav className="loc-migas" aria-label="Ruta">
        <span>Administración</span>
        <span aria-hidden="true">/</span>
        <span>Locales</span>
      </nav>

      <div className="panel-admin-encabezado">
        <div className="panel-admin-titulo-grupo">
          <span className="panel-admin-titulo-icono" aria-hidden="true">
            <IconoUbicacion width={24} height={24} />
          </span>
          <div>
            <h1>Locales</h1>
            <p className="panel-admin-subtitulo">Configura las sedes y define cuál estás administrando.</p>
          </div>
        </div>
        <Boton onClick={() => navigate('/admin/locales/nuevo')}>
          <IconoMas width={16} height={16} aria-hidden="true" /> Nuevo local
        </Boton>
      </div>

      {!cargando && !errorCarga && (
        <p className="loc-resumen">
          {totales.total} {totales.total === 1 ? 'local' : 'locales'} · {totales.activos}{' '}
          {totales.activos === 1 ? 'activo' : 'activos'}
        </p>
      )}

      {aviso && (
        <div className="loc-aviso" role="note">
          <IconoInfo width={18} height={18} aria-hidden="true" />
          <span>El local seleccionado define los datos que ves en los módulos operativos.</span>
          <button type="button" className="loc-aviso-cerrar" aria-label="Cerrar aviso" onClick={cerrarAviso}>
            <IconoCerrar width={16} height={16} aria-hidden="true" />
          </button>
        </div>
      )}

      <div className="loc-filtros">
        <label className="loc-buscador">
          <IconoBusqueda width={17} height={17} aria-hidden="true" />
          <input
            type="search"
            aria-label="Buscar locales"
            placeholder="Buscar locales..."
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
          />
        </label>
        <div className="loc-pestanas" role="tablist" aria-label="Estado">
          {(['todos', 'activos', 'inactivos'] as Estado[]).map((e) => (
            <button
              key={e}
              type="button"
              role="tab"
              aria-selected={estado === e}
              className={`loc-pestana ${estado === e ? 'loc-pestana-activa' : ''}`}
              onClick={() => setEstado(e)}
            >
              {e === 'todos' ? 'Todos' : e === 'activos' ? 'Activos' : 'Inactivos'}
            </button>
          ))}
        </div>
      </div>

      {cargando ? (
        <EstadoCargando />
      ) : errorCarga ? (
        <EstadoError mensaje={errorCarga} accion={<Boton onClick={() => void recargar()}>Reintentar</Boton>} />
      ) : locales.length === 0 ? (
        <div className="loc-vacio">
          <strong>Todavía no hay locales</strong>
          <span>Crea el primero para poder asignarle usuarios y empezar a operar.</span>
          <Boton onClick={() => navigate('/admin/locales/nuevo')}>Nuevo local</Boton>
        </div>
      ) : filtrados.length === 0 ? (
        <div className="loc-vacio">
          <strong>Ningún local coincide</strong>
          <span>Prueba con otro nombre o dirección.</span>
        </div>
      ) : (
        <div className="loc-lista">
          {filtrados.map((l) => {
            const esActual = l.id === sesion!.localActivoId
            // Se cambia solo a un local activo al que la sesion tenga acceso
            // (el administrador central los tiene todos).
            const puedeCambiar = !esActual && l.activo && idsAccesibles.has(l.id)
            return (
              <article key={l.id} className={`loc-tarjeta ${l.activo ? '' : 'loc-tarjeta-inactiva'}`}>
                <span className="loc-icono" aria-hidden="true">
                  <IconoTienda width={28} height={28} />
                </span>

                <div className="loc-principal">
                  <div className="loc-nombre-fila">
                    <h2>{l.nombre}</h2>
                    {esActual && <span className="loc-etiqueta">Local actual</span>}
                    <span className="loc-codigo">{l.codigo}</span>
                  </div>
                  <span className="loc-direccion">
                    <IconoUbicacion width={15} height={15} aria-hidden="true" />
                    {l.direccion}
                  </span>
                </div>

                <span className="loc-separador" aria-hidden="true" />

                <div className="loc-dato loc-dato-estado">
                  <strong className={l.activo ? 'loc-activo' : 'loc-inactivo'}>
                    <span aria-hidden="true" /> {l.activo ? 'Activo' : 'Inactivo'}
                  </strong>
                  <span>Estado</span>
                </div>

                <div className="loc-dato loc-dato-gerente">
                  <strong className={l.gerente ? '' : 'loc-tenue'}>
                    <IconoPersonas width={17} height={17} aria-hidden="true" /> {l.gerente ?? 'Sin asignar'}
                  </strong>
                  <span>Gerente</span>
                </div>

                <div className="loc-acciones">
                  {esActual && (
                    <button
                      type="button"
                      className="loc-boton-tenue"
                      onClick={() => navigate(`/admin/locales/${l.id}/editar`)}
                    >
                      <IconoOjo width={18} height={18} aria-hidden="true" /> Ver local
                    </button>
                  )}
                  {puedeCambiar && (
                    <button
                      type="button"
                      className="loc-boton-tenue"
                      disabled={cambiandoId === l.id}
                      onClick={() => void cambiarA(l)}
                    >
                      <IconoCambiar width={18} height={18} aria-hidden="true" />
                      {cambiandoId === l.id ? 'Cambiando…' : 'Cambiar a este local'}
                    </button>
                  )}
                  {!l.activo && (
                    <button type="button" className="loc-boton-borde" onClick={() => void alternarActivo(l)}>
                      Reactivar
                    </button>
                  )}
                  <MenuLocal
                    local={l}
                    abierto={menuAbierto === l.id}
                    onAlternar={() => setMenuAbierto(menuAbierto === l.id ? null : l.id)}
                    onCerrar={() => setMenuAbierto(null)}
                    onEditar={() => navigate(`/admin/locales/${l.id}/editar`)}
                    onEstado={() => void alternarActivo(l)}
                    onEliminar={() => abrirEliminar(l)}
                  />
                </div>
              </article>
            )
          })}
        </div>
      )}

      {mensaje && (
        <div className="loc-toast" role="status">
          <IconoReactivar width={16} height={16} aria-hidden="true" /> {mensaje}
        </div>
      )}

      <ModalDeConfirmacion
        abierto={localAEliminar != null}
        titulo="Eliminar local"
        mensaje={`Vas a eliminar "${localAEliminar?.nombre}" de forma permanente. Esta acción no se puede deshacer. Solo es posible si el local nunca tuvo usuarios asignados ni sesiones registradas.`}
        textoConfirmar="Eliminar"
        peligro
        confirmando={eliminando}
        error={errorEliminar}
        onConfirmar={confirmarEliminar}
        onCancelar={() => setLocalAEliminar(null)}
      />
    </section>
  )
}

function MenuLocal({
  local,
  abierto,
  onAlternar,
  onCerrar,
  onEditar,
  onEstado,
  onEliminar,
}: {
  local: Local
  abierto: boolean
  onAlternar: () => void
  onCerrar: () => void
  onEditar: () => void
  onEstado: () => void
  onEliminar: () => void
}) {
  const raiz = useRef<HTMLDivElement>(null)

  // Se cierra al hacer clic fuera o con Escape.
  useEffect(() => {
    if (!abierto) return
    function alClic(e: MouseEvent) {
      if (raiz.current && !raiz.current.contains(e.target as Node)) onCerrar()
    }
    function alTeclear(e: KeyboardEvent) {
      if (e.key === 'Escape') onCerrar()
    }
    document.addEventListener('mousedown', alClic)
    document.addEventListener('keydown', alTeclear)
    return () => {
      document.removeEventListener('mousedown', alClic)
      document.removeEventListener('keydown', alTeclear)
    }
  }, [abierto, onCerrar])

  return (
    <div className="loc-menu" ref={raiz}>
      <button
        type="button"
        className="loc-boton-mas"
        aria-label={`Más acciones de ${local.nombre}`}
        aria-haspopup="menu"
        aria-expanded={abierto}
        onClick={onAlternar}
      >
        <IconoMasOpciones width={18} height={18} aria-hidden="true" />
      </button>
      {abierto && (
        <div className="loc-menu-lista" role="menu">
          <button type="button" role="menuitem" onClick={onEditar}>
            <IconoLapiz width={16} height={16} aria-hidden="true" /> Editar local
          </button>
          <button type="button" role="menuitem" onClick={onEstado}>
            {local.activo ? (
              <IconoBloquear width={16} height={16} aria-hidden="true" />
            ) : (
              <IconoReactivar width={16} height={16} aria-hidden="true" />
            )}
            {local.activo ? 'Desactivar' : 'Reactivar'}
          </button>
          <div className="loc-menu-linea" />
          <button type="button" role="menuitem" className="loc-menu-peligro" onClick={onEliminar}>
            <IconoBorrar width={16} height={16} aria-hidden="true" /> Eliminar local
          </button>
        </div>
      )}
    </div>
  )
}
