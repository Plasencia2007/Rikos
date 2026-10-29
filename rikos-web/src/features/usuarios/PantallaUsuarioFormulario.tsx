import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useSesion } from '../../comun/sesion/SesionContexto'
import {
  Aviso,
  Boton,
  Campo,
  EntradaNumerica,
  EstadoCargando,
  EstadoError,
  IconoFlechaIzquierda,
  ModalDeConfirmacion,
} from '../../comun/componentes'
import {
  ErrorApi,
  actualizarUsuario,
  asignarExcepcionesPermiso,
  asignarPin,
  asignarRolesYLocales,
  cambiarEstadoUsuario,
  crearUsuario,
  listarLocales,
  listarRoles,
  obtenerUsuario,
  permisosDeUsuario,
  type Local,
  type PermisoUsuario,
  type Rol,
} from '../../api/cliente'

interface Formulario {
  nombreUsuario: string
  nombres: string
  apellidos: string
  correo: string
  telefono: string
  contrasena: string
  pin: string
  localesIds: string[]
  localPrincipalId: string
  rolesIds: string[]
}

const FORM_INICIAL: Formulario = {
  nombreUsuario: '', nombres: '', apellidos: '', correo: '', telefono: '', contrasena: '', pin: '',
  localesIds: [], localPrincipalId: '', rolesIds: [],
}

export function PantallaUsuarioFormulario() {
  const { id } = useParams<{ id: string }>()
  const esNuevo = id === undefined
  const navigate = useNavigate()
  const { sesion } = useSesion()
  const token = sesion!.token

  const [cargando, setCargando] = useState(true)
  const [errorCarga, setErrorCarga] = useState('')
  const [roles, setRoles] = useState<Rol[]>([])
  const [locales, setLocales] = useState<Local[]>([])
  const [usuarioId, setUsuarioId] = useState<string | null>(null)

  const [formulario, setFormulario] = useState(FORM_INICIAL)
  const [activo, setActivo] = useState(true)
  const [mostrarContrasena, setMostrarContrasena] = useState(false)

  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')
  const [guardadoOk, setGuardadoOk] = useState(false)

  const [confirmarCambioEstado, setConfirmarCambioEstado] = useState(false)
  const [cambiandoEstado, setCambiandoEstado] = useState(false)

  const [excepcionesAbierto, setExcepcionesAbierto] = useState(false)
  const [permisosUsuario, setPermisosUsuario] = useState<PermisoUsuario[] | null>(null)
  const [cargandoPermisos, setCargandoPermisos] = useState(false)
  const [seleccionPermisos, setSeleccionPermisos] = useState<Record<string, boolean>>({})
  const [motivos, setMotivos] = useState<Record<string, string>>({})
  const [guardandoPermisos, setGuardandoPermisos] = useState(false)
  const [errorPermisos, setErrorPermisos] = useState('')

  useEffect(() => {
    setCargando(true)
    setErrorCarga('')
    Promise.all([
      listarRoles(token),
      listarLocales(token),
      esNuevo ? Promise.resolve(null) : obtenerUsuario(token, id!),
    ])
      .then(([r, l, detalle]) => {
        setRoles(r)
        setLocales(l)
        if (detalle) {
          setUsuarioId(detalle.id)
          setActivo(detalle.activo)
          setFormulario({
            nombreUsuario: detalle.nombreUsuario,
            nombres: detalle.nombres,
            apellidos: detalle.apellidos,
            correo: detalle.correo ?? '',
            telefono: detalle.telefono ?? '',
            contrasena: '',
            pin: '',
            localesIds: detalle.localesIds,
            localPrincipalId: detalle.localPrincipalId ?? '',
            rolesIds: detalle.rolesIds,
          })
        }
      })
      .catch(() => setErrorCarga('No se pudo cargar el usuario.'))
      .finally(() => setCargando(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, esNuevo, token])

  function alternarSeleccion(campo: 'rolesIds' | 'localesIds', valor: string) {
    setFormulario((f) => {
      const yaEsta = f[campo].includes(valor)
      const siguiente = yaEsta ? f[campo].filter((x) => x !== valor) : [...f[campo], valor]
      const localPrincipalId =
        campo === 'localesIds' && !yaEsta && f.localPrincipalId === '' ? valor : f.localPrincipalId
      return { ...f, [campo]: siguiente, localPrincipalId }
    })
  }

  async function guardar() {
    setError('')
    setGuardadoOk(false)

    if (formulario.localesIds.length === 0) {
      setError('Elige al menos un local.')
      return
    }
    if (formulario.rolesIds.length === 0) {
      setError('Elige al menos un rol.')
      return
    }
    if (formulario.pin && !/^\d{4}$/.test(formulario.pin)) {
      setError('El PIN debe tener exactamente 4 dígitos.')
      return
    }
    if (esNuevo && formulario.contrasena.length < 6) {
      setError('La contraseña debe tener al menos 6 caracteres.')
      return
    }

    setGuardando(true)
    try {
      let idFinal = usuarioId
      if (esNuevo) {
        const creado = await crearUsuario(token, {
          nombreUsuario: formulario.nombreUsuario,
          nombres: formulario.nombres,
          apellidos: formulario.apellidos,
          correo: formulario.correo || undefined,
          telefono: formulario.telefono || undefined,
          contrasena: formulario.contrasena,
          rolesIds: formulario.rolesIds,
          localesIds: formulario.localesIds,
          localPrincipalId: formulario.localPrincipalId || formulario.localesIds[0],
        })
        idFinal = creado.id
      } else {
        await actualizarUsuario(token, idFinal!, {
          nombres: formulario.nombres,
          apellidos: formulario.apellidos,
          correo: formulario.correo || undefined,
          telefono: formulario.telefono || undefined,
        })
        await asignarRolesYLocales(token, idFinal!, {
          rolesIds: formulario.rolesIds,
          localesIds: formulario.localesIds,
          localPrincipalId: formulario.localPrincipalId || formulario.localesIds[0],
        })
      }
      if (formulario.pin) {
        await asignarPin(token, idFinal!, formulario.pin)
      }
      if (esNuevo) {
        navigate(`/admin/usuarios/${idFinal}/editar`, { replace: true })
      } else {
        setGuardadoOk(true)
      }
    } catch (excepcion) {
      setError(
        excepcion instanceof ErrorApi && excepcion.cuerpo?.error === 'NOMBRE_USUARIO_DUPLICADO'
          ? 'Ya existe un usuario con ese nombre de login.'
          : 'No se pudo guardar el usuario.',
      )
    } finally {
      setGuardando(false)
    }
  }

  async function confirmarAlternarEstado() {
    if (!usuarioId) return
    setCambiandoEstado(true)
    try {
      const actualizado = await cambiarEstadoUsuario(token, usuarioId, !activo)
      setActivo(actualizado.activo)
      setConfirmarCambioEstado(false)
    } finally {
      setCambiandoEstado(false)
    }
  }

  function abrirExcepciones() {
    setExcepcionesAbierto((abierto) => !abierto)
    if (!permisosUsuario && usuarioId) {
      setCargandoPermisos(true)
      permisosDeUsuario(token, usuarioId)
        .then((lista) => {
          setPermisosUsuario(lista)
          const seleccion: Record<string, boolean> = {}
          lista.forEach((p) => (seleccion[p.permisoId] = p.efectivo))
          setSeleccionPermisos(seleccion)
        })
        .catch(() => setErrorPermisos('No se pudieron cargar los permisos.'))
        .finally(() => setCargandoPermisos(false))
    }
  }

  async function guardarExcepciones() {
    if (!usuarioId || !permisosUsuario) return
    setGuardandoPermisos(true)
    setErrorPermisos('')
    try {
      const excepciones = permisosUsuario
        .filter((p) => Boolean(seleccionPermisos[p.permisoId]) !== p.vieneDeRol)
        .map((p) => ({
          permisoId: p.permisoId,
          otorgado: Boolean(seleccionPermisos[p.permisoId]),
          motivo: motivos[p.permisoId] || undefined,
        }))
      await asignarExcepcionesPermiso(token, usuarioId, excepciones)
      const actualizados = await permisosDeUsuario(token, usuarioId)
      setPermisosUsuario(actualizados)
    } catch {
      setErrorPermisos('No se pudieron guardar las excepciones.')
    } finally {
      setGuardandoPermisos(false)
    }
  }

  if (cargando) return <EstadoCargando />
  if (errorCarga) return <EstadoError mensaje={errorCarga} />

  return (
    <section className="panel-admin">
      <div className="panel-admin-encabezado">
        <div className="panel-admin-titulo-grupo">
          <button type="button" className="boton-volver" onClick={() => navigate('/admin/usuarios')} aria-label="Volver a Usuarios">
            <IconoFlechaIzquierda width={18} height={18} />
          </button>
          <h1>{esNuevo ? 'Crear Nuevo Usuario' : `Editar ${formulario.nombreUsuario}`}</h1>
        </div>
        <Boton onClick={guardar} cargando={guardando}>
          {esNuevo ? 'Guardar Usuario' : 'Guardar cambios'}
        </Boton>
      </div>

      <div className="panel-admin-tarjeta usuario-formulario-seccion">
        <div className="usuario-seccion-encabezado">
          <h2 className="formulario-local-subtitulo">Credenciales</h2>
          {!esNuevo && (
            <label className="usuario-estado-toggle">
              Estado de cuenta:
              <button
                type="button"
                className={`interruptor ${activo ? 'activo' : ''}`}
                role="switch"
                aria-checked={activo}
                onClick={() => setConfirmarCambioEstado(true)}
              >
                <span className="interruptor-bola" />
              </button>
              <span className={activo ? 'usuario-estado-texto activo' : 'usuario-estado-texto'}>
                {activo ? 'Activo' : 'Inactivo'}
              </span>
            </label>
          )}
        </div>

        <div className="usuario-formulario-grid">
          <Campo etiqueta="Nombres" value={formulario.nombres} onChange={(e) => setFormulario((f) => ({ ...f, nombres: e.target.value }))} required />
          <Campo etiqueta="Correo electrónico" type="email" value={formulario.correo} onChange={(e) => setFormulario((f) => ({ ...f, correo: e.target.value }))} />
          <Campo etiqueta="Apellidos" value={formulario.apellidos} onChange={(e) => setFormulario((f) => ({ ...f, apellidos: e.target.value }))} required />
          <Campo
            etiqueta="Nombre de usuario (login)"
            value={formulario.nombreUsuario}
            onChange={(e) => setFormulario((f) => ({ ...f, nombreUsuario: e.target.value }))}
            disabled={!esNuevo}
            required={esNuevo}
          />
          <Campo etiqueta="Teléfono móvil" value={formulario.telefono} onChange={(e) => setFormulario((f) => ({ ...f, telefono: e.target.value }))} />

          {esNuevo && (
            <Campo
              etiqueta="Contraseña"
              ayuda="Mínimo 6 caracteres."
              type={mostrarContrasena ? 'text' : 'password'}
              value={formulario.contrasena}
              onChange={(e) => setFormulario((f) => ({ ...f, contrasena: e.target.value }))}
              sufijo={
                <button type="button" className="boton-alternar-contrasena" onClick={() => setMostrarContrasena((p) => !p)}>
                  {mostrarContrasena ? 'Ocultar' : 'Ver'}
                </button>
              }
              required
            />
          )}

          <div className="campo">
            <span className="campo-etiqueta">PIN de acceso rápido (opcional)</span>
            <EntradaNumerica
              valor={formulario.pin}
              onCambiar={(v) => setFormulario((f) => ({ ...f, pin: v }))}
              largoFijo={4}
              oculto
            />
            <p className="campo-ayuda">
              Exactamente 4 dígitos para acceso rápido al POS. Solo tiene sentido para roles que operan el POS. Nunca
              se muestra el PIN ya guardado -- esto establece uno nuevo.
            </p>
          </div>
        </div>

        {error && <Aviso tono="error">{error}</Aviso>}
        {guardadoOk && <Aviso tono="exito">Guardado.</Aviso>}
      </div>

      <div className="usuario-formulario-columnas">
        <div className="panel-admin-tarjeta usuario-formulario-seccion">
          <h2 className="formulario-local-subtitulo">Acceso a locales</h2>
          <div className="usuario-lista-seleccion">
            {locales.map((local) => (
              <label key={local.id} className={`usuario-opcion-fila ${formulario.localesIds.includes(local.id) ? 'seleccionada' : ''}`}>
                <input
                  type="checkbox"
                  className="opcion-control"
                  checked={formulario.localesIds.includes(local.id)}
                  onChange={() => alternarSeleccion('localesIds', local.id)}
                />
                <span className="usuario-opcion-etiqueta">{local.nombre}</span>
                {formulario.localPrincipalId === local.id && <span className="usuario-insignia-principal">Principal</span>}
                {formulario.localesIds.includes(local.id) && formulario.localPrincipalId !== local.id && (
                  <button
                    type="button"
                    className="boton-texto-chico"
                    onClick={() => setFormulario((f) => ({ ...f, localPrincipalId: local.id }))}
                  >
                    Hacer principal
                  </button>
                )}
              </label>
            ))}
          </div>
          <p className="campo-ayuda">
            El usuario podrá cambiar entre todos los locales marcados sin volver a iniciar sesión. El local{' '}
            <strong>Principal</strong> es el que carga por defecto al ingresar.
          </p>
        </div>

        <div className="panel-admin-tarjeta usuario-formulario-seccion">
          <h2 className="formulario-local-subtitulo">Roles asignados</h2>
          <div className="usuario-lista-seleccion">
            {roles.map((rol) => (
              <label key={rol.id} className={`usuario-opcion-fila ${formulario.rolesIds.includes(rol.id) ? 'seleccionada' : ''}`}>
                <input
                  type="checkbox"
                  className="opcion-control"
                  checked={formulario.rolesIds.includes(rol.id)}
                  onChange={() => alternarSeleccion('rolesIds', rol.id)}
                />
                <span className="usuario-rol-punto" style={{ background: rol.color ?? '#2563eb' }} aria-hidden="true" />
                <span className="usuario-opcion-etiqueta">{rol.nombre}</span>
              </label>
            ))}
          </div>
        </div>
      </div>

      <div className="panel-admin-tarjeta usuario-formulario-seccion">
        <button type="button" className="usuario-excepciones-boton" onClick={abrirExcepciones} disabled={esNuevo}>
          <span>Permisos individuales</span>
          <span className="usuario-excepciones-chevron">{excepcionesAbierto ? '▲' : '▼'}</span>
        </button>
        {esNuevo && <p className="campo-ayuda">Disponible después de guardar el usuario por primera vez.</p>}

        {excepcionesAbierto && !esNuevo && (
          <div className="usuario-excepciones-contenido">
            <p className="campo-ayuda">
              El permiso efectivo es la unión de los permisos de todos sus roles, más las excepciones de aquí abajo
              -- la excepción siempre gana sobre el rol.
            </p>
            {cargandoPermisos && <EstadoCargando />}
            {errorPermisos && <Aviso tono="error">{errorPermisos}</Aviso>}
            {permisosUsuario && (
              <>
                <div className="usuario-excepciones-lista">
                  {permisosUsuario.map((p) => {
                    const marcado = Boolean(seleccionPermisos[p.permisoId])
                    const esExcepcion = marcado !== p.vieneDeRol
                    return (
                      <div key={p.permisoId} className="usuario-excepcion-fila">
                        <label className="usuario-opcion-fila">
                          <input
                            type="checkbox"
                            className="opcion-control"
                            checked={marcado}
                            onChange={() =>
                              setSeleccionPermisos((s) => ({ ...s, [p.permisoId]: !s[p.permisoId] }))
                            }
                          />
                          <span className="usuario-opcion-etiqueta">{p.descripcion}</span>
                          <span className="rol-permiso-codigo">{p.codigo}</span>
                          <span className={`usuario-permiso-origen ${esExcepcion ? 'excepcion' : p.vieneDeRol ? 'rol' : 'ninguno'}`}>
                            {esExcepcion ? (marcado ? 'concedido por excepción' : 'retirado por excepción') : p.vieneDeRol ? 'del rol' : '—'}
                          </span>
                        </label>
                        {esExcepcion && (
                          <input
                            type="text"
                            placeholder="Motivo de la excepción (opcional)"
                            className="campo-control usuario-excepcion-motivo"
                            value={motivos[p.permisoId] ?? ''}
                            onChange={(e) => setMotivos((m) => ({ ...m, [p.permisoId]: e.target.value }))}
                          />
                        )}
                      </div>
                    )
                  })}
                </div>
                <div className="formulario-local-guardar">
                  <Boton onClick={guardarExcepciones} cargando={guardandoPermisos}>
                    Guardar excepciones
                  </Boton>
                </div>
              </>
            )}
          </div>
        )}
      </div>

      <ModalDeConfirmacion
        abierto={confirmarCambioEstado}
        titulo={activo ? 'Desactivar usuario' : 'Activar usuario'}
        mensaje={
          activo
            ? `Vas a desactivar a "${formulario.nombreUsuario}". Pierde el acceso al sistema de inmediato, aunque conserva la atribución de lo que ya hizo.`
            : `Vas a reactivar a "${formulario.nombreUsuario}". Va a poder entrar de nuevo con su contraseña o PIN.`
        }
        textoConfirmar={activo ? 'Desactivar' : 'Activar'}
        peligro={activo}
        confirmando={cambiandoEstado}
        onConfirmar={confirmarAlternarEstado}
        onCancelar={() => setConfirmarCambioEstado(false)}
      />
    </section>
  )
}
