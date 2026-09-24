import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useSesion } from '../../comun/sesion/SesionContexto'
import { Boton } from '../../comun/componentes/Boton'
import { ModalDeConfirmacion } from '../../comun/componentes/ModalDeConfirmacion'
import { Tabla, type ColumnaTabla } from '../../comun/componentes/Tabla'
import { IconoLapiz, IconoMas, IconoProhibido, IconoUbicacion, IconoUsuario } from '../../comun/componentes'
import { cambiarEstadoUsuario, listarUsuarios, type Usuario } from '../../api/cliente'

function formatearFecha(iso: string): string {
  const fecha = new Date(iso)
  return fecha.toLocaleDateString('es-PE', { day: '2-digit', month: '2-digit', year: 'numeric' }) +
    ' ' + fecha.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' })
}

/** US-005: alta, edición y desactivación de usuarios, con roles, locales y excepciones de permiso. */
export function PantallaUsuarios() {
  const { sesion } = useSesion()
  const token = sesion!.token
  const navigate = useNavigate()

  const [usuarios, setUsuarios] = useState<Usuario[]>([])
  const [cargando, setCargando] = useState(true)
  const [errorCarga, setErrorCarga] = useState('')

  const [usuarioAConfirmar, setUsuarioAConfirmar] = useState<Usuario | null>(null)
  const [confirmando, setConfirmando] = useState(false)

  async function recargar() {
    setCargando(true)
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

  async function confirmarCambioEstado() {
    if (!usuarioAConfirmar) return
    setConfirmando(true)
    try {
      await cambiarEstadoUsuario(token, usuarioAConfirmar.id, !usuarioAConfirmar.activo)
      setUsuarioAConfirmar(null)
      await recargar()
    } finally {
      setConfirmando(false)
    }
  }

  const columnas: ColumnaTabla<Usuario>[] = [
    {
      encabezado: 'Nombre / Login',
      iconoEncabezado: <IconoUsuario width={15} height={15} aria-hidden="true" />,
      render: (u) => (
        <div className="locales-fila-nombre">
          <span className="usuario-avatar" aria-hidden="true">
            {u.nombres.charAt(0).toUpperCase()}
          </span>
          <div>
            <div className="locales-nombre">{u.nombres} {u.apellidos}</div>
            <div className="locales-codigo">@{u.nombreUsuario}{u.correo ? ` · ${u.correo}` : ''}</div>
          </div>
        </div>
      ),
    },
    {
      encabezado: 'Local principal',
      iconoEncabezado: <IconoUbicacion width={15} height={15} aria-hidden="true" />,
      render: (u) => (
        <span className="locales-direccion">
          {u.localPrincipal ? <><IconoUbicacion width={14} height={14} aria-hidden="true" /> {u.localPrincipal.nombre}</> : '—'}
        </span>
      ),
    },
    {
      encabezado: 'Roles asignados',
      render: (u) => (
        <div className="usuario-roles-insignias">
          {u.roles.length === 0
            ? '—'
            : u.roles.map((r) => (
                <span key={r.id} className="usuario-rol-insignia" style={{ background: r.color ?? '#8a3b12' }}>
                  {r.nombre}
                </span>
              ))}
        </div>
      ),
    },
    {
      encabezado: 'Último acceso',
      render: (u) =>
        u.ultimoAcceso ? (
          <span className="usuario-ultimo-acceso">
            {formatearFecha(u.ultimoAcceso.ocurridoEn)}
            {u.ultimoAcceso.direccionIp && <span className="usuario-ultimo-ip">IP: {u.ultimoAcceso.direccionIp}</span>}
          </span>
        ) : (
          <span className="locales-codigo">Nunca</span>
        ),
    },
    {
      encabezado: 'Estado',
      render: (u) => <span className={`locales-estado ${u.activo ? 'activo' : 'inactivo'}`}>● {u.activo ? 'Activo' : 'Inactivo'}</span>,
    },
    {
      encabezado: 'Acciones',
      acciones: true,
      render: (u) => (
        <div className="locales-acciones">
          <Boton variante="secundario" onClick={() => navigate(`/admin/usuarios/${u.id}/editar`)}>
            <IconoLapiz width={14} height={14} aria-hidden="true" /> Editar
          </Boton>
          <Boton variante="peligro" onClick={() => setUsuarioAConfirmar(u)}>
            <IconoProhibido width={14} height={14} aria-hidden="true" /> {u.activo ? 'Desactivar' : 'Activar'}
          </Boton>
        </div>
      ),
    },
  ]

  return (
    <section className="panel-admin">
      <div className="panel-admin-encabezado">
        <div>
          <h1>Usuarios (Logins)</h1>
          <p className="panel-admin-subtitulo">Administra cuentas de usuario, locales y sus permisos.</p>
        </div>
        <Boton onClick={() => navigate('/admin/usuarios/nuevo')}>
          <IconoMas width={16} height={16} aria-hidden="true" /> Nuevo usuario
        </Boton>
      </div>

      <div className="panel-admin-tarjeta">
        <Tabla
          columnas={columnas}
          filas={usuarios}
          obtenerClave={(u) => u.id}
          cargando={cargando}
          error={errorCarga}
          vacioTitulo="Todavía no hay usuarios"
          vacioDescripcion="Crea el primero para asignarle un rol y empezar a repartir accesos."
          vacioAccion={<Boton onClick={() => navigate('/admin/usuarios/nuevo')}>Nuevo usuario</Boton>}
        />
      </div>

      <ModalDeConfirmacion
        abierto={usuarioAConfirmar != null}
        titulo={usuarioAConfirmar?.activo ? 'Desactivar usuario' : 'Activar usuario'}
        mensaje={
          usuarioAConfirmar?.activo
            ? `Vas a desactivar a "${usuarioAConfirmar?.nombreUsuario}". Pierde el acceso al sistema de inmediato, aunque conserva la atribución de lo que ya hizo.`
            : `Vas a reactivar a "${usuarioAConfirmar?.nombreUsuario}". Va a poder entrar de nuevo con su contraseña o PIN.`
        }
        textoConfirmar={usuarioAConfirmar?.activo ? 'Desactivar' : 'Activar'}
        peligro={usuarioAConfirmar?.activo}
        confirmando={confirmando}
        onConfirmar={confirmarCambioEstado}
        onCancelar={() => setUsuarioAConfirmar(null)}
      />
    </section>
  )
}
