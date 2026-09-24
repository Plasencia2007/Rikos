import { useEffect, useState, type CSSProperties } from 'react'
import { useNavigate } from 'react-router-dom'
import { useSesion } from '../../comun/sesion/SesionContexto'
import { Boton, EstadoCargando, EstadoError, EstadoVacio, IconoLapiz, IconoMas } from '../../comun/componentes'
import { listarRoles, type Rol } from '../../api/cliente'

/** US-004/US-006 (adelantadas). Los seis roles del negocio son datos semilla; se pueden crear otros. */
export function PantallaRoles() {
  const { sesion } = useSesion()
  const token = sesion!.token
  const navigate = useNavigate()

  const [roles, setRoles] = useState<Rol[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    setCargando(true)
    setError('')
    listarRoles(token)
      .then(setRoles)
      .catch(() => setError('No se pudo cargar la lista de roles.'))
      .finally(() => setCargando(false))
  }, [token])

  return (
    <section className="panel-admin">
      <div className="panel-admin-encabezado">
        <div>
          <h1>Roles</h1>
          <p className="panel-admin-subtitulo">Crea y administra los roles para tus diferentes tipos de personal.</p>
        </div>
        <Boton onClick={() => navigate('/admin/usuarios/roles/nuevo')}>
          <IconoMas width={16} height={16} aria-hidden="true" /> Nuevo rol
        </Boton>
      </div>

      {cargando && <EstadoCargando />}
      {!cargando && error && <EstadoError mensaje={error} />}
      {!cargando && !error && roles.length === 0 && (
        <EstadoVacio titulo="Todavía no hay roles" descripcion="Esto no debería pasar: los seis roles del negocio se cargan solos." />
      )}

      {!cargando && !error && roles.length > 0 && (
        <div className="roles-grilla">
          {roles.map((rol) => (
            <div className="rol-tarjeta" key={rol.id} style={{ '--rol-color': rol.color ?? '#8a3b12' } as CSSProperties}>
              <div className="rol-tarjeta-barra" />
              <div className="rol-tarjeta-cuerpo">
                <div className="rol-tarjeta-encabezado">
                  <span className="rol-tarjeta-icono" aria-hidden="true">
                    ⚙
                  </span>
                  {rol.esSistema && <span className="rol-tarjeta-insignia">Bloqueado (Sistema)</span>}
                </div>
                <h3 className="rol-tarjeta-nombre">{rol.nombre}</h3>
                <p className="rol-tarjeta-descripcion">{rol.descripcion || 'Sin descripción.'}</p>
                <div className="rol-tarjeta-pie">
                  <span className="rol-tarjeta-contador">{rol.cantidadPermisos} Permisos config.</span>
                  <button
                    type="button"
                    className="rol-tarjeta-editar"
                    aria-label={`Editar ${rol.nombre}`}
                    onClick={() => navigate(`/admin/usuarios/roles/${rol.id}/editar`)}
                  >
                    <IconoLapiz width={14} height={14} />
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  )
}
