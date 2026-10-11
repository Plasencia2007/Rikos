import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Building2, ChevronRight, Clock, KeyRound, Lock, MapPin, Plus, Search } from 'lucide-react'
import { useSesion } from '../../comun/sesion/SesionContexto'
import { formatearMomento } from '../../comun/formato/momento'
import {
  Boton,
  EstadoCargando,
  EstadoError,
  IconoBloquear,
  IconoBorrar,
  IconoLapiz,
  IconoMasOpciones,
  IconoReactivar,
  ModalDeConfirmacion,
} from '../../comun/componentes'
import {
  ErrorApi,
  actividadDeUsuario,
  cambiarEstadoUsuario,
  eliminarUsuario,
  listarUsuarios,
  type ActividadUsuario,
  type Usuario,
} from '../../api/cliente'

type Estado = 'todos' | 'activos' | 'inactivos'

const ROL_ADMINISTRADOR_CENTRAL = 'administrador central'

const VERBO_EVENTO: Record<string, string> = {
  creacion: 'Creó',
  actualizacion: 'Actualizó',
  eliminacion: 'Eliminó',
  cambio_estado: 'Cambió el estado de',
  cambio_precio: 'Cambió el precio de',
  cambio_receta: 'Cambió la receta de',
  reactivacion: 'Reactivó',
  marcado_agotado: 'Marcó como agotado',
  acceso_denegado: 'Intentó un acceso denegado a',
}
const NOMBRE_ENTIDAD: Record<string, string> = {
  local: 'un local', usuario: 'un usuario', rol: 'un rol', producto: 'un producto', categoria: 'una categoría',
  proveedor: 'un proveedor', receta: 'una receta', semana_menu: 'una semana del menú', motivo: 'un motivo',
  acceso: 'un recurso',
}

function textoDeActividad(a: ActividadUsuario): string {
  if (a.tipo === 'sesion') return 'Inició sesión en el sistema'
  const verbo = VERBO_EVENTO[a.evento] ?? a.evento
  const entidad = a.entidad ? (NOMBRE_ENTIDAD[a.entidad] ?? a.entidad) : ''
  return `${verbo} ${entidad}`.trim()
}

const capitalizar = (t: string) => t.charAt(0).toUpperCase() + t.slice(1)
const nombreCompleto = (u: Usuario) => `${u.nombres} ${u.apellidos}`.trim()
const rolesTexto = (u: Usuario) => u.roles.map((r) => capitalizar(r.nombre)).join(', ') || 'Sin rol'
const esCentral = (u: Usuario) => u.roles.some((r) => r.nombre.toLowerCase() === ROL_ADMINISTRADOR_CENTRAL)

/** US-005: lista de cuentas con su ficha al lado; alta, edición, desactivación y eliminación. */
export function PantallaUsuarios() {
  const { sesion } = useSesion()
  const token = sesion!.token
  const navigate = useNavigate()

  const [usuarios, setUsuarios] = useState<Usuario[]>([])
  const [cargando, setCargando] = useState(true)
  const [errorCarga, setErrorCarga] = useState('')
  const [busqueda, setBusqueda] = useState('')
  const [estado, setEstado] = useState<Estado>('todos')
  const [localFiltro, setLocalFiltro] = useState('')
  const [seleccionadoId, setSeleccionadoId] = useState<string | null>(null)

  const [actividad, setActividad] = useState<ActividadUsuario[] | null>(null)
  const [menuAbierto, setMenuAbierto] = useState(false)
  const [mensaje, setMensaje] = useState('')

  const [aEliminar, setAEliminar] = useState<Usuario | null>(null)
  const [aCambiarEstado, setACambiarEstado] = useState<Usuario | null>(null)
  const [confirmando, setConfirmando] = useState(false)
  const [errorAccion, setErrorAccion] = useState('')

  async function recargar() {
    setErrorCarga('')
    try {
      setUsuarios(await listarUsuarios(token))
    } catch {
      setErrorCarga('No se pudo cargar la lista de usuarios.')
    } finally {
      setCargando(false)
    }
  }

  useEffect(() => {
    void recargar()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    if (!mensaje) return
    const t = setTimeout(() => setMensaje(''), 2600)
    return () => clearTimeout(t)
  }, [mensaje])

  const localesDisponibles = useMemo(() => {
    const porId = new Map<string, string>()
    usuarios.forEach((u) => u.locales.forEach((l) => porId.set(l.id, l.nombre)))
    return [...porId.entries()].sort((a, b) => a[1].localeCompare(b[1]))
  }, [usuarios])

  const filtrados = useMemo(() => {
    const q = busqueda.trim().toLowerCase()
    return usuarios.filter((u) => {
      if (estado === 'activos' && !u.activo) return false
      if (estado === 'inactivos' && u.activo) return false
      if (localFiltro && !esCentral(u) && !u.locales.some((l) => l.id === localFiltro)) return false
      return !q || `${nombreCompleto(u)} ${u.nombreUsuario}`.toLowerCase().includes(q)
    })
  }, [usuarios, busqueda, estado, localFiltro])

  // La ficha muestra al elegido; si el filtro lo esconde (o no hay elegido), el primero de la lista.
  const seleccionado = filtrados.find((u) => u.id === seleccionadoId) ?? filtrados[0] ?? null

  useEffect(() => {
    setActividad(null)
    setMenuAbierto(false)
    if (!seleccionado) return
    let vigente = true
    actividadDeUsuario(token, seleccionado.id)
      .then((a) => vigente && setActividad(a))
      .catch(() => vigente && setActividad([]))
    return () => {
      vigente = false
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [seleccionado?.id, token])

  async function confirmarEliminar() {
    if (!aEliminar) return
    setConfirmando(true)
    setErrorAccion('')
    try {
      await eliminarUsuario(token, aEliminar.id)
      setAEliminar(null)
      setMensaje('Usuario eliminado')
      await recargar()
    } catch (e) {
      setErrorAccion(
        e instanceof ErrorApi && e.cuerpo?.error === 'TIENE_HISTORIAL'
          ? 'No se puede eliminar: este usuario ya tiene actividad registrada (sesiones, cambios hechos, etc.) que debe conservarse. Puedes desactivarlo en su lugar.'
          : 'No se pudo eliminar el usuario.',
      )
    } finally {
      setConfirmando(false)
    }
  }

  async function confirmarCambioEstado() {
    if (!aCambiarEstado) return
    setConfirmando(true)
    setErrorAccion('')
    try {
      await cambiarEstadoUsuario(token, aCambiarEstado.id, !aCambiarEstado.activo)
      setMensaje(`Cuenta ${aCambiarEstado.activo ? 'desactivada' : 'reactivada'}`)
      setACambiarEstado(null)
      await recargar()
    } catch {
      setErrorAccion('No se pudo cambiar el estado de la cuenta.')
    } finally {
      setConfirmando(false)
    }
  }

  return (
    <section className="panel-admin usr-pagina">
      <div className="usr-cabecera">
        <div>
          <nav className="loc-migas" aria-label="Ruta">
            <span>Usuarios y permisos</span>
            <span aria-hidden="true">/</span>
            <span>Administrar</span>
          </nav>
          <div className="usr-titulo">
            <h1>Usuarios</h1>
            {!cargando && !errorCarga && (
              <span>
                {usuarios.length} {usuarios.length === 1 ? 'cuenta' : 'cuentas'}
              </span>
            )}
          </div>
          <p className="panel-admin-subtitulo">Gestiona acceso, roles y locales de cada persona.</p>
        </div>
        <Boton onClick={() => navigate('/admin/usuarios/nuevo')}>
          <Plus size={17} strokeWidth={2.2} aria-hidden="true" /> Nuevo usuario
        </Boton>
      </div>

      <div className="usr-filtros">
        <label className="usr-select">
          <Building2 size={18} aria-hidden="true" />
          <select aria-label="Filtrar por local" value={localFiltro} onChange={(e) => setLocalFiltro(e.target.value)}>
            <option value="">Todos los locales</option>
            {localesDisponibles.map(([id, nombre]) => (
              <option key={id} value={id}>
                {nombre}
              </option>
            ))}
          </select>
        </label>
        <label className="loc-buscador usr-buscador">
          <Search size={17} aria-hidden="true" />
          <input
            type="search"
            aria-label="Buscar usuario"
            placeholder="Buscar usuario..."
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
          />
        </label>
        <div className="loc-pestanas usr-pestanas" role="tablist" aria-label="Estado">
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
      ) : usuarios.length === 0 ? (
        <div className="loc-vacio">
          <strong>Todavía no hay usuarios</strong>
          <span>Crea el primero para asignarle un rol y empezar a repartir accesos.</span>
          <Boton onClick={() => navigate('/admin/usuarios/nuevo')}>Nuevo usuario</Boton>
        </div>
      ) : (
        <div className="usr-cuerpo">
          <section className="usr-lista" aria-label="Lista de usuarios">
            {filtrados.length === 0 && <p className="usr-lista-vacia">Ningún usuario coincide con la búsqueda.</p>}
            {filtrados.map((u) => {
              const actual = seleccionado?.id === u.id
              return (
                <button
                  key={u.id}
                  type="button"
                  className={`usr-fila ${actual ? 'actual' : ''}`}
                  aria-current={actual || undefined}
                  onClick={() => setSeleccionadoId(u.id)}
                >
                  <span className="usr-avatar" aria-hidden="true">
                    {u.nombres.charAt(0).toUpperCase()}
                  </span>
                  <span className="usr-fila-texto">
                    <strong>{nombreCompleto(u)}</strong>
                    <span>@{u.nombreUsuario}</span>
                    <span>{rolesTexto(u)}</span>
                  </span>
                  <span className={`usr-estado ${u.activo ? 'activo' : 'inactivo'}`}>
                    <span aria-hidden="true" /> {u.activo ? 'Activa' : 'Inactiva'}
                  </span>
                  <ChevronRight size={16} aria-hidden="true" />
                </button>
              )
            })}
          </section>

          {seleccionado && (
            <FichaUsuario
              usuario={seleccionado}
              actividad={actividad}
              menuAbierto={menuAbierto}
              onAlternarMenu={() => setMenuAbierto((v) => !v)}
              onCerrarMenu={() => setMenuAbierto(false)}
              onEditar={() => navigate(`/admin/usuarios/${seleccionado.id}/editar`)}
              onEliminar={() => {
                setMenuAbierto(false)
                setErrorAccion('')
                setAEliminar(seleccionado)
              }}
              onCambiarEstado={() => {
                setErrorAccion('')
                setACambiarEstado(seleccionado)
              }}
            />
          )}
        </div>
      )}

      {mensaje && (
        <div className="loc-toast" role="status">
          <IconoReactivar width={16} height={16} aria-hidden="true" /> {mensaje}
        </div>
      )}

      <ModalDeConfirmacion
        abierto={aEliminar != null}
        titulo="Eliminar usuario"
        mensaje={`"${aEliminar?.nombreUsuario}" se eliminará permanentemente. Esta acción no se puede deshacer.`}
        textoConfirmar="Eliminar"
        peligro
        error={errorAccion}
        confirmando={confirmando}
        onConfirmar={confirmarEliminar}
        onCancelar={() => setAEliminar(null)}
      />
      <ModalDeConfirmacion
        abierto={aCambiarEstado != null}
        titulo={aCambiarEstado?.activo ? 'Desactivar cuenta' : 'Reactivar cuenta'}
        mensaje={
          aCambiarEstado?.activo
            ? `"${aCambiarEstado.nombreUsuario}" no podrá iniciar sesión hasta que reactives la cuenta. Su historial se conserva.`
            : `"${aCambiarEstado?.nombreUsuario}" volverá a poder iniciar sesión.`
        }
        textoConfirmar={aCambiarEstado?.activo ? 'Desactivar' : 'Reactivar'}
        peligro={aCambiarEstado?.activo}
        error={errorAccion}
        confirmando={confirmando}
        onConfirmar={confirmarCambioEstado}
        onCancelar={() => setACambiarEstado(null)}
      />
    </section>
  )
}

interface PropsFicha {
  usuario: Usuario
  actividad: ActividadUsuario[] | null
  menuAbierto: boolean
  onAlternarMenu: () => void
  onCerrarMenu: () => void
  onEditar: () => void
  onEliminar: () => void
  onCambiarEstado: () => void
}

function FichaUsuario({
  usuario: u, actividad, menuAbierto, onAlternarMenu, onCerrarMenu, onEditar, onEliminar, onCambiarEstado,
}: PropsFicha) {
  const raiz = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!menuAbierto) return
    function alClic(e: MouseEvent) {
      if (raiz.current && !raiz.current.contains(e.target as Node)) onCerrarMenu()
    }
    function alTeclear(e: KeyboardEvent) {
      if (e.key === 'Escape') onCerrarMenu()
    }
    document.addEventListener('mousedown', alClic)
    document.addEventListener('keydown', alTeclear)
    return () => {
      document.removeEventListener('mousedown', alClic)
      document.removeEventListener('keydown', alTeclear)
    }
  }, [menuAbierto, onCerrarMenu])

  return (
    <section className="usr-ficha" aria-label="Detalle del usuario">
      <div className="usr-ficha-cabecera">
        <span className="usr-avatar usr-avatar-grande" aria-hidden="true">
          {u.nombres.charAt(0).toUpperCase()}
        </span>
        <div className="usr-ficha-nombre">
          <div>
            <h2>{nombreCompleto(u)}</h2>
            <span className={`usr-estado ${u.activo ? 'activo' : 'inactivo'}`}>
              <span aria-hidden="true" /> {u.activo ? 'Activa' : 'Inactiva'}
            </span>
          </div>
          <span>@{u.nombreUsuario}</span>
          <span>{rolesTexto(u)}</span>
        </div>
        <button type="button" className="loc-boton-tenue" onClick={onEditar}>
          <IconoLapiz width={17} height={17} aria-hidden="true" /> Editar
        </button>
        <div className="loc-menu" ref={raiz}>
          <button
            type="button"
            className="usr-boton-mas"
            aria-label={`Más acciones de ${nombreCompleto(u)}`}
            aria-haspopup="menu"
            aria-expanded={menuAbierto}
            onClick={onAlternarMenu}
          >
            <IconoMasOpciones width={18} height={18} aria-hidden="true" />
          </button>
          {menuAbierto && (
            <div className="loc-menu-lista" role="menu">
              <button type="button" role="menuitem" className="loc-menu-peligro" onClick={onEliminar}>
                <IconoBorrar width={16} height={16} aria-hidden="true" /> Eliminar usuario
              </button>
            </div>
          )}
        </div>
      </div>

      <hr className="as-separador" />

      <h3 className="usr-seccion">
        <KeyRound size={20} aria-hidden="true" /> Acceso
      </h3>
      <dl className="usr-datos">
        <dt>{u.roles.length > 1 ? 'Roles' : 'Rol'}</dt>
        <dd>
          {u.roles.length === 0 && <span className="as-suave">Sin rol asignado</span>}
          {u.roles.map((r) => (
            <div key={r.id} className="usr-rol">
              <span>{capitalizar(r.nombre)}</span>
              {r.descripcion && <span className="as-ayuda">{r.descripcion}</span>}
            </div>
          ))}
        </dd>
        <dt>Local principal</dt>
        <dd>
          {u.localPrincipal ? (
            <span className="as-con-icono usr-principal">
              <MapPin size={16} aria-hidden="true" /> {u.localPrincipal.nombre}
            </span>
          ) : (
            <span className="as-suave">Sin local principal</span>
          )}
        </dd>
        <dt>Locales con acceso</dt>
        <dd className="usr-pastillas">
          <Building2 size={18} aria-hidden="true" />
          {esCentral(u) ? (
            <span className="usr-pastilla">Todos los locales</span>
          ) : u.locales.length === 0 ? (
            <span className="as-suave">Ninguno</span>
          ) : (
            u.locales.map((l) => (
              <span key={l.id} className="usr-pastilla">
                {l.nombre}
              </span>
            ))
          )}
        </dd>
      </dl>

      <h3 className="usr-seccion">
        <Lock size={20} aria-hidden="true" /> Seguridad
      </h3>
      <dl className="usr-datos">
        <dt>Último acceso</dt>
        <dd>
          {u.ultimoAcceso ? (
            <>
              {formatearMomento(u.ultimoAcceso.ocurridoEn)}
              {u.ultimoAcceso.direccionIp && <span className="as-ayuda"> · IP {u.ultimoAcceso.direccionIp}</span>}
            </>
          ) : (
            <span className="as-suave">Nunca</span>
          )}
        </dd>
      </dl>

      <h3 className="usr-seccion">
        <Clock size={20} aria-hidden="true" /> Actividad reciente
      </h3>
      {actividad === null ? (
        <p className="as-ayuda">Cargando actividad…</p>
      ) : actividad.length === 0 ? (
        <p className="as-ayuda">Todavía no hay actividad registrada.</p>
      ) : (
        <dl className="usr-datos">
          {actividad.map((a, i) => (
            <div key={i} className="usr-actividad">
              <dt>{formatearMomento(a.ocurridoEn)}</dt>
              <dd>{textoDeActividad(a)}</dd>
            </div>
          ))}
        </dl>
      )}

      <div className="usr-ficha-pie">
        <button type="button" className={u.activo ? 'usr-boton-peligro' : 'loc-boton-tenue'} onClick={onCambiarEstado}>
          {u.activo ? (
            <IconoBloquear width={16} height={16} aria-hidden="true" />
          ) : (
            <IconoReactivar width={16} height={16} aria-hidden="true" />
          )}
          {u.activo ? 'Desactivar cuenta' : 'Reactivar cuenta'}
        </button>
      </div>
    </section>
  )
}
