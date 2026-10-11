import { useEffect, useState, type CSSProperties } from 'react'
import { useNavigate } from 'react-router-dom'
import { ArrowRight, FileText, Plus, Users } from 'lucide-react'
import { useSesion } from '../../comun/sesion/SesionContexto'
import { Boton, EstadoCargando, EstadoError, IconoBorrar, IconoLapiz, ModalDeConfirmacion } from '../../comun/componentes'
import { listarRoles, eliminarRol, ErrorApi, type Rol } from '../../api/cliente'
import { iconoDeRol } from './rolesVisual'

/** US-004/US-006: los seis roles del sistema (se consultan y ajustan) y los roles propios del negocio. */
export function PantallaRoles() {
  const { sesion } = useSesion()
  const token = sesion!.token
  const navigate = useNavigate()

  const [roles, setRoles] = useState<Rol[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')

  const [rolAEliminar, setRolAEliminar] = useState<Rol | null>(null)
  const [confirmando, setConfirmando] = useState(false)
  const [errorEliminar, setErrorEliminar] = useState('')

  function recargar() {
    setError('')
    listarRoles(token)
      .then(setRoles)
      .catch(() => setError('No se pudo cargar la lista de roles.'))
      .finally(() => setCargando(false))
  }

  useEffect(recargar, [token])

  async function confirmarEliminar() {
    if (!rolAEliminar) return
    setConfirmando(true)
    setErrorEliminar('')
    try {
      await eliminarRol(token, rolAEliminar.id)
      setRolAEliminar(null)
      recargar()
    } catch (e) {
      setErrorEliminar(
        e instanceof ErrorApi && e.cuerpo?.error === 'TIENE_HISTORIAL'
          ? 'No se puede eliminar: hay usuarios con este rol asignado. Quítaselo a todos primero.'
          : 'No se pudo eliminar el rol.',
      )
    } finally {
      setConfirmando(false)
    }
  }

  const delSistema = roles.filter((r) => r.esSistema)
  const personalizados = roles.filter((r) => !r.esSistema)
  const irACrear = () => navigate('/admin/usuarios/roles/nuevo')
  const irAEditar = (r: Rol) => navigate(`/admin/usuarios/roles/${r.id}/editar`)

  function tarjeta(rol: Rol) {
    const Icono = iconoDeRol(rol.nombre)
    return (
      <article key={rol.id} className="rol-card" style={{ '--rol-color': rol.color ?? '#2B7A68' } as CSSProperties}>
        <div className="rol-card-barra" />
        <div className="rol-card-cuerpo">
          <div className="rol-card-cabecera">
            <span className="rol-card-icono" aria-hidden="true">
              <Icono size={26} strokeWidth={1.6} />
            </span>
            <div>
              <h3>{rol.nombre}</h3>
              <p>{rol.descripcion || 'Sin descripción.'}</p>
            </div>
          </div>
          <hr className="as-separador" />
          <div className="rol-card-pie">
            <span className="rol-card-cuenta">
              <span className="rol-card-cuenta-icono" aria-hidden="true">
                <FileText size={16} />
              </span>
              {rol.cantidadPermisos} {rol.cantidadPermisos === 1 ? 'permiso' : 'permisos'}
              <span className="rol-card-usuarios">
                <Users size={14} aria-hidden="true" /> {rol.cantidadUsuarios}
              </span>
            </span>
            <div className="rol-card-acciones">
              {!rol.esSistema && (
                <>
                  <button type="button" className="as-icono-boton" aria-label={`Editar ${rol.nombre}`} onClick={() => irAEditar(rol)}>
                    <IconoLapiz width={16} height={16} />
                  </button>
                  <button
                    type="button"
                    className="as-icono-boton peligro"
                    aria-label={`Eliminar ${rol.nombre}`}
                    onClick={() => {
                      setErrorEliminar('')
                      setRolAEliminar(rol)
                    }}
                  >
                    <IconoBorrar width={16} height={16} />
                  </button>
                </>
              )}
              <button type="button" className="as-enlace" onClick={() => irAEditar(rol)}>
                Ver permisos <ArrowRight size={16} strokeWidth={2} aria-hidden="true" />
              </button>
            </div>
          </div>
        </div>
      </article>
    )
  }

  return (
    <section className="panel-admin rol-pagina">
      <div className="usr-cabecera">
        <div>
          <h1 className="rol-titulo">Roles</h1>
          <p className="panel-admin-subtitulo">Define qué puede hacer cada tipo de personal.</p>
        </div>
        <Boton onClick={irACrear}>
          <Plus size={17} strokeWidth={2.2} aria-hidden="true" /> Nuevo rol
        </Boton>
      </div>

      {cargando ? (
        <EstadoCargando />
      ) : error ? (
        <EstadoError mensaje={error} accion={<Boton onClick={recargar}>Reintentar</Boton>} />
      ) : (
        <>
          <section className="rol-seccion" aria-labelledby="roles-sistema">
            <div>
              <h2 id="roles-sistema">Roles del sistema</h2>
              <p>Estos roles vienen con Riko&apos;s. Consulta sus permisos y ajústalos si tu negocio lo necesita.</p>
            </div>
            <div className="rol-grilla">
              {delSistema.map(tarjeta)}
            </div>
          </section>

          <section className="rol-seccion" aria-labelledby="roles-propios">
            <div className="rol-seccion-cabecera">
              <h2 id="roles-propios">Roles personalizados</h2>
              <button type="button" className="as-enlace" onClick={irACrear}>
                <Plus size={16} strokeWidth={2.2} aria-hidden="true" /> Crear rol personalizado
              </button>
            </div>
            {personalizados.length === 0 ? (
              <div className="rol-vacio">
                <span aria-hidden="true">
                  <Users size={24} strokeWidth={1.6} />
                </span>
                <div>
                  <strong>Aún no hay roles personalizados</strong>
                  <p>Crea roles personalizados para adaptar los permisos a las necesidades de tu negocio.</p>
                </div>
              </div>
            ) : (
              <div className="rol-grilla">
                {personalizados.map(tarjeta)}
              </div>
            )}
          </section>
        </>
      )}

      <ModalDeConfirmacion
        abierto={rolAEliminar != null}
        titulo="Eliminar rol"
        mensaje={`"${rolAEliminar?.nombre}" se eliminará permanentemente. Esta acción no se puede deshacer.`}
        textoConfirmar="Eliminar"
        peligro
        error={errorEliminar}
        confirmando={confirmando}
        onConfirmar={confirmarEliminar}
        onCancelar={() => setRolAEliminar(null)}
      />
    </section>
  )
}
