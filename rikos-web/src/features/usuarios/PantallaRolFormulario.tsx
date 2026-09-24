import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useSesion } from '../../comun/sesion/SesionContexto'
import {
  Aviso,
  Boton,
  Campo,
  EstadoCargando,
  EstadoError,
  IconoFlechaIzquierda,
} from '../../comun/componentes'
import {
  ErrorApi,
  actualizarRol,
  asignarPermisosARol,
  crearRol,
  listarPermisos,
  listarRoles,
  permisosDeRol,
  type Permiso,
} from '../../api/cliente'

const COLORES = ['#2563eb', '#dc2626', '#059669', '#d97706', '#7c3aed', '#ea580c', '#334155']

export function PantallaRolFormulario() {
  const { id } = useParams<{ id: string }>()
  const esNuevo = id === undefined
  const navigate = useNavigate()
  const { sesion } = useSesion()
  const token = sesion!.token

  const [cargando, setCargando] = useState(true)
  const [errorCarga, setErrorCarga] = useState('')
  const [permisos, setPermisos] = useState<Permiso[]>([])
  const [seleccionados, setSeleccionados] = useState<Set<string>>(new Set())

  const [nombre, setNombre] = useState('')
  const [descripcion, setDescripcion] = useState('')
  const [color, setColor] = useState(COLORES[0])
  const [rolId, setRolId] = useState<string | null>(null)
  const [esSistema, setEsSistema] = useState(false)

  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    setCargando(true)
    setErrorCarga('')
    Promise.all([
      listarPermisos(token),
      esNuevo ? Promise.resolve([]) : permisosDeRol(token, id!),
      esNuevo ? Promise.resolve([]) : listarRoles(token),
    ])
      .then(([todos, deEsteRol, roles]) => {
        setPermisos(todos)
        setSeleccionados(new Set(deEsteRol))
        setRolId(esNuevo ? null : id!)
        if (!esNuevo) {
          const rol = roles.find((r) => r.id === id)
          if (!rol) throw new Error('rol no encontrado')
          setNombre(rol.nombre)
          setDescripcion(rol.descripcion ?? '')
          setColor(rol.color ?? COLORES[0])
          setEsSistema(rol.esSistema)
        }
      })
      .catch(() => setErrorCarga('No se pudo cargar el rol.'))
      .finally(() => setCargando(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, esNuevo, token])

  const porModulo = useMemo(() => {
    const grupos = new Map<string, Permiso[]>()
    for (const permiso of permisos) {
      const lista = grupos.get(permiso.modulo) ?? []
      lista.push(permiso)
      grupos.set(permiso.modulo, lista)
    }
    return [...grupos.entries()].sort(([a], [b]) => a.localeCompare(b))
  }, [permisos])

  function alternar(permisoId: string) {
    setSeleccionados((actual) => {
      const nuevo = new Set(actual)
      if (nuevo.has(permisoId)) nuevo.delete(permisoId)
      else nuevo.add(permisoId)
      return nuevo
    })
  }

  function alternarModulo(idsDelModulo: string[]) {
    const todosMarcados = idsDelModulo.every((pid) => seleccionados.has(pid))
    setSeleccionados((actual) => {
      const nuevo = new Set(actual)
      idsDelModulo.forEach((pid) => (todosMarcados ? nuevo.delete(pid) : nuevo.add(pid)))
      return nuevo
    })
  }

  async function guardar() {
    setError('')
    if (!nombre.trim()) {
      setError('El nombre es obligatorio.')
      return
    }
    setGuardando(true)
    try {
      let idFinal = rolId
      if (esNuevo) {
        const creado = await crearRol(token, { nombre, descripcion: descripcion || undefined, color })
        idFinal = creado.id
      } else {
        await actualizarRol(token, rolId!, { nombre, descripcion: descripcion || undefined, color })
      }
      await asignarPermisosARol(token, idFinal!, [...seleccionados])
      navigate('/admin/usuarios/roles')
    } catch (excepcion) {
      setError(
        excepcion instanceof ErrorApi && excepcion.cuerpo?.error === 'NOMBRE_DUPLICADO'
          ? 'Ya existe un rol con ese nombre.'
          : 'No se pudo guardar el rol.',
      )
    } finally {
      setGuardando(false)
    }
  }

  if (cargando) return <EstadoCargando />
  if (errorCarga) return <EstadoError mensaje={errorCarga} />

  return (
    <section className="panel-admin">
      <div className="panel-admin-encabezado">
        <div className="panel-admin-titulo-grupo">
          <button
            type="button"
            className="boton-volver"
            onClick={() => navigate('/admin/usuarios/roles')}
            aria-label="Volver a Roles"
          >
            <IconoFlechaIzquierda width={18} height={18} />
          </button>
          <h1>{esNuevo ? 'Crear Nuevo Rol' : 'Editar Rol'}</h1>
        </div>
        <Boton onClick={guardar} cargando={guardando}>
          {esNuevo ? 'Crear Rol' : 'Guardar cambios'}
        </Boton>
      </div>

      <div className="panel-admin-tarjeta rol-formulario-seccion">
        <h2 className="formulario-local-subtitulo">Información del rol</h2>
        <div className="rol-formulario-fila">
          <Campo
            etiqueta="Nombre del rol"
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
            required
            disabled={esSistema}
            ayuda={esSistema ? 'Los roles del sistema no se pueden renombrar.' : undefined}
          />
          <div className="campo">
            <span className="campo-etiqueta">Color distintivo</span>
            <div className="rol-selector-color">
              {COLORES.map((c) => (
                <button
                  key={c}
                  type="button"
                  className={`rol-color-punto ${color === c ? 'seleccionado' : ''}`}
                  style={{ background: c }}
                  aria-label={`Color ${c}`}
                  onClick={() => setColor(c)}
                />
              ))}
            </div>
          </div>
        </div>
        <Campo
          etiqueta="Descripción (opcional)"
          value={descripcion}
          onChange={(e) => setDescripcion(e.target.value)}
          placeholder="¿Qué hace la persona que tenga este rol?"
        />
        {error && <Aviso tono="error">{error}</Aviso>}
      </div>

      <div className="panel-admin-tarjeta rol-formulario-seccion">
        <div className="rol-permisos-encabezado">
          <h2 className="formulario-local-subtitulo">Asignar accesos</h2>
          <span className="rol-permisos-contador">{seleccionados.size} accesos seleccionados</span>
        </div>

        <div className="rol-permisos-grilla">
          {porModulo.map(([modulo, permisosDelModulo]) => {
            const ids = permisosDelModulo.map((p) => p.id)
            const todosMarcados = ids.every((pid) => seleccionados.has(pid))
            return (
              <div className="rol-permisos-columna" key={modulo}>
                <div className="rol-permisos-columna-titulo">
                  <span>{modulo}</span>
                  <button type="button" className="boton-texto-chico" onClick={() => alternarModulo(ids)}>
                    {todosMarcados ? 'Quitar todos' : 'Marcar todos'}
                  </button>
                </div>
                {permisosDelModulo.map((permiso) => (
                  <label key={permiso.id} className="rol-permiso-item">
                    <input
                      type="checkbox"
                      className="opcion-control"
                      checked={seleccionados.has(permiso.id)}
                      onChange={() => alternar(permiso.id)}
                    />
                    <span>
                      <span className="rol-permiso-descripcion">{permiso.descripcion}</span>
                      <span className="rol-permiso-codigo">{permiso.codigo}</span>
                    </span>
                  </label>
                ))}
              </div>
            )
          })}
        </div>
      </div>
    </section>
  )
}
