import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ChevronDown, ChevronLeft, Check, Eye, Lock, RotateCcw, Search, TriangleAlert, X } from 'lucide-react'
import { useSesion } from '../../comun/sesion/SesionContexto'
import { Boton, EstadoCargando, EstadoError, IconoReactivar, Modal } from '../../comun/componentes'
import {
  ErrorApi,
  actualizarRol,
  asignarPermisosARol,
  crearRol,
  listarPermisos,
  listarRoles,
  permisosDeRol,
  permisosPredeterminadosDeRol,
  type Permiso,
  type Rol,
} from '../../api/cliente'
import { PALETA_ROL, capitalizar, datosDeModulo, esAccesoSensible } from './rolesVisual'

const detalleDe = (p: Permiso) => `Permite ${p.descripcion.charAt(0).toLowerCase()}${p.descripcion.slice(1)}.`

/**
 * Alta y edición de un rol con la misma pantalla: permisos agrupados por módulo, resumen a la
 * derecha y barra inferior. En un rol del sistema el nombre queda bloqueado y se puede volver
 * a los permisos de fábrica (se cargan en el formulario; recién se guardan con "Guardar cambios").
 */
export function PantallaRolFormulario() {
  const { id } = useParams<{ id: string }>()
  const esNuevo = id === undefined
  const navigate = useNavigate()
  const { sesion } = useSesion()
  const token = sesion!.token

  const [cargando, setCargando] = useState(true)
  const [errorCarga, setErrorCarga] = useState('')
  const [permisos, setPermisos] = useState<Permiso[]>([])
  const [seleccion, setSeleccion] = useState<Set<string>>(new Set())
  const [original, setOriginal] = useState<{ nombre: string; descripcion: string; color: string; permisos: Set<string> } | null>(null)
  const [predeterminados, setPredeterminados] = useState<Set<string>>(new Set())
  const [rolesConPermisos, setRolesConPermisos] = useState<{ nombre: string; ids: Set<string> }[]>([])

  const [rol, setRol] = useState<Rol | null>(null)
  const [nombre, setNombre] = useState('')
  const [descripcion, setDescripcion] = useState('')
  const [color, setColor] = useState(PALETA_ROL[0].hex)

  const [moduloActual, setModuloActual] = useState('')
  const [busqueda, setBusqueda] = useState('')
  const [abiertos, setAbiertos] = useState<Set<string>>(new Set())
  const [tecnicos, setTecnicos] = useState(false)
  const [enRevision, setEnRevision] = useState(false)
  const [confirmarRestaurar, setConfirmarRestaurar] = useState(false)

  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')
  const [errorNombre, setErrorNombre] = useState('')
  const [aviso, setAviso] = useState('')

  useEffect(() => {
    setCargando(true)
    setErrorCarga('')
    Promise.all([
      listarPermisos(token),
      listarRoles(token),
      esNuevo ? Promise.resolve([] as string[]) : permisosDeRol(token, id!),
      esNuevo ? Promise.resolve([] as string[]) : permisosPredeterminadosDeRol(token, id!),
    ])
      .then(async ([todos, roles, deEsteRol, deFabrica]) => {
        setPermisos(todos)
        setModuloActual((actual) => actual || [...new Set(todos.map((p) => p.modulo))].sort()[0] || '')
        setPredeterminados(new Set(deFabrica))
        if (!esNuevo) {
          const encontrado = roles.find((r) => r.id === id)
          if (!encontrado) throw new Error('rol no encontrado')
          setRol(encontrado)
          setNombre(encontrado.nombre)
          setDescripcion(encontrado.descripcion ?? '')
          setColor(encontrado.color ?? PALETA_ROL[0].hex)
          setSeleccion(new Set(deEsteRol))
          setOriginal({
            nombre: encontrado.nombre,
            descripcion: encontrado.descripcion ?? '',
            color: encontrado.color ?? PALETA_ROL[0].hex,
            permisos: new Set(deEsteRol),
          })
        }
        // Para "Lo tienen": qué roles ya cuentan con cada permiso (una llamada por rol, sin bloquear la pantalla).
        Promise.all(roles.map(async (r) => ({ nombre: r.nombre, ids: new Set(await permisosDeRol(token, r.id)) })))
          .then(setRolesConPermisos)
          .catch(() => undefined)
      })
      .catch(() => setErrorCarga('No se pudo cargar el rol.'))
      .finally(() => setCargando(false))
  }, [id, esNuevo, token])

  const esSistema = rol?.esSistema ?? false
  const tienePredeterminados = esSistema && predeterminados.size > 0

  const modulos = useMemo(() => {
    const grupos = new Map<string, Permiso[]>()
    for (const p of permisos) grupos.set(p.modulo, [...(grupos.get(p.modulo) ?? []), p])
    return [...grupos.entries()].sort(([a], [b]) => datosDeModulo(a).etiqueta.localeCompare(datosDeModulo(b).etiqueta, 'es'))
  }, [permisos])

  const q = busqueda.trim().toLowerCase()
  const coincide = (p: Permiso) => !q || `${p.descripcion} ${p.codigo}`.toLowerCase().includes(q)
  const modulosVisibles = modulos.filter(
    ([m, lista]) => !q || datosDeModulo(m).etiqueta.toLowerCase().includes(q) || lista.some(coincide),
  )
  const grupoActual = modulos.find(([m]) => m === moduloActual)
  const datosActual = datosDeModulo(moduloActual)
  const permisosDelModulo = (grupoActual?.[1] ?? []).filter(
    (p) => !q || datosActual.etiqueta.toLowerCase().includes(q) || coincide(p),
  )
  const todosMarcados = permisosDelModulo.length > 0 && permisosDelModulo.every((p) => seleccion.has(p.id))

  const cuentaDe = (m: string) => (modulos.find(([mm]) => mm === m)?.[1] ?? []).filter((p) => seleccion.has(p.id)).length

  const anadidos = tienePredeterminados ? permisos.filter((p) => seleccion.has(p.id) && !predeterminados.has(p.id)) : []
  const quitados = tienePredeterminados ? permisos.filter((p) => !seleccion.has(p.id) && predeterminados.has(p.id)) : []
  const modulosCambiados = new Set([...anadidos, ...quitados].map((p) => p.modulo))

  const permisosCambiados = original
    ? [...seleccion].filter((pid) => !original.permisos.has(pid)).length +
      [...original.permisos].filter((pid) => !seleccion.has(pid)).length
    : 0
  const cambiosSinGuardar = original
    ? permisosCambiados +
      (descripcion !== original.descripcion ? 1 : 0) +
      (color !== original.color ? 1 : 0) +
      (nombre !== original.nombre ? 1 : 0)
    : 0
  const puedeGuardar = esNuevo ? nombre.trim() !== '' && seleccion.size > 0 : cambiosSinGuardar > 0 && nombre.trim() !== ''

  const paleta = PALETA_ROL.some((c) => c.hex.toLowerCase() === color.toLowerCase())
    ? PALETA_ROL
    : [...PALETA_ROL, { hex: color, nombre: 'Color actual' }]

  function alternar(pid: string) {
    setAviso('')
    setSeleccion((actual) => {
      const nuevo = new Set(actual)
      if (nuevo.has(pid)) nuevo.delete(pid)
      else nuevo.add(pid)
      return nuevo
    })
  }

  function alternarModulo() {
    setSeleccion((actual) => {
      const nuevo = new Set(actual)
      permisosDelModulo.forEach((p) => (todosMarcados ? nuevo.delete(p.id) : nuevo.add(p.id)))
      return nuevo
    })
  }

  function restaurar() {
    setSeleccion(new Set(predeterminados))
    setConfirmarRestaurar(false)
    setAviso('Permisos predeterminados restaurados. Guarda los cambios para aplicarlos.')
  }

  function losTienen(pid: string) {
    return rolesConPermisos.filter((r) => r.ids.has(pid)).map((r) => r.nombre).join(', ')
  }

  async function guardar() {
    setError('')
    setErrorNombre('')
    if (!nombre.trim()) {
      setErrorNombre('El nombre es obligatorio.')
      return
    }
    setGuardando(true)
    try {
      const datos = { nombre: nombre.trim(), descripcion: descripcion.trim() || undefined, color }
      const idFinal = esNuevo ? (await crearRol(token, datos)).id : (await actualizarRol(token, id!, datos)).id
      await asignarPermisosARol(token, idFinal, [...seleccion])
      navigate('/admin/usuarios/roles')
    } catch (excepcion) {
      if (excepcion instanceof ErrorApi && excepcion.cuerpo?.error === 'NOMBRE_DUPLICADO') {
        setErrorNombre('Ya existe un rol con ese nombre. Usa uno distinto.')
      } else {
        setError('No se pudo guardar el rol.')
      }
    } finally {
      setGuardando(false)
    }
  }

  if (cargando) return <EstadoCargando />
  if (errorCarga) return <EstadoError mensaje={errorCarga} />

  const volver = () => navigate('/admin/usuarios/roles')
  const total = permisos.length
  const seleccionados = permisos.filter((p) => seleccion.has(p.id))

  return (
    <div className="as-pagina">
      <div className="as-cabecera">
        <button type="button" className="as-volver" onClick={volver} aria-label="Volver a Roles">
          <ChevronLeft size={18} aria-hidden="true" />
        </button>
        <div className="as-cabecera-texto">
          <nav className="as-migas" aria-label="Ruta">
            <span>Usuarios y permisos</span> / <span>Roles</span> /{' '}
            <span className="as-miga-actual">{esNuevo ? 'Nuevo rol' : rol?.nombre}</span>
          </nav>
          <h1>{esNuevo ? 'Crear rol' : 'Editar rol'}</h1>
          <p>
            {esNuevo
              ? 'Define el nombre y los accesos de este rol.'
              : 'Ajusta los accesos de este rol. Los cambios valen para todas las personas que lo tienen.'}
          </p>
        </div>
      </div>

      <div className="rf-cuerpo">
        <div className="rf-principal">
          <section className="as-tarjeta rf-info" aria-labelledby="rf-info">
            <h2 id="rf-info" className="as-h2">
              Información del rol
            </h2>
            <div className="rf-info-fila">
              <label className="rf-campo">
                <span>
                  Nombre del rol {!esSistema && <span className="as-req">*</span>}
                </span>
                {esSistema ? (
                  <div className="rf-bloqueado">
                    <Lock size={16} aria-hidden="true" />
                    <span>{nombre}</span>
                    <span className="rf-insignia">Rol del sistema</span>
                  </div>
                ) : (
                  <input
                    type="text"
                    placeholder="Ej. Cajero, Administrador de local, etc."
                    value={nombre}
                    aria-invalid={Boolean(errorNombre) || undefined}
                    className={errorNombre ? 'invalido' : ''}
                    onChange={(e) => {
                      setNombre(e.target.value)
                      setErrorNombre('')
                    }}
                  />
                )}
                {esSistema ? (
                  <span className="as-ayuda">
                    Los roles del sistema conservan su nombre. {rol?.cantidadUsuarios ?? 0}{' '}
                    {rol?.cantidadUsuarios === 1 ? 'persona tiene' : 'personas tienen'} este rol.
                  </span>
                ) : (
                  errorNombre && <span className="campo-error">{errorNombre}</span>
                )}
              </label>
              <label className="rf-campo rf-campo-ancho">
                <span>
                  Descripción <span className="as-suave">(opcional)</span>
                </span>
                <input
                  type="text"
                  placeholder="¿Qué hace la persona que tenga este rol?"
                  value={descripcion}
                  onChange={(e) => setDescripcion(e.target.value)}
                />
              </label>
              <div className="rf-campo rf-color">
                <span>
                  Color del rol <span className="as-suave">(opcional)</span>
                </span>
                <div role="radiogroup" aria-label="Color del rol">
                  {paleta.map((c) => (
                    <button
                      key={c.hex}
                      type="button"
                      role="radio"
                      aria-checked={c.hex.toLowerCase() === color.toLowerCase()}
                      aria-label={c.nombre}
                      className={`rf-punto ${c.hex.toLowerCase() === color.toLowerCase() ? 'elegido' : ''}`}
                      style={{ background: c.hex, ['--rf-color' as string]: c.hex }}
                      onClick={() => setColor(c.hex)}
                    />
                  ))}
                </div>
              </div>
            </div>
          </section>

          <section className="as-tarjeta rf-permisos" aria-label="Permisos">
            <nav className="rf-modulos" aria-label="Módulos">
              <label className="rf-busqueda">
                <Search size={16} aria-hidden="true" />
                <input
                  type="text"
                  aria-label="Buscar permiso o módulo"
                  placeholder="Buscar permiso o módulo..."
                  value={busqueda}
                  onChange={(e) => setBusqueda(e.target.value)}
                />
              </label>
              {modulosVisibles.map(([m]) => {
                const { etiqueta, Icono } = datosDeModulo(m)
                const n = cuentaDe(m)
                const actual = m === moduloActual && !enRevision
                return (
                  <button
                    key={m}
                    type="button"
                    className={`rf-modulo ${actual ? 'actual' : ''}`}
                    aria-current={actual || undefined}
                    onClick={() => {
                      setModuloActual(m)
                      setEnRevision(false)
                    }}
                  >
                    <Icono size={17} strokeWidth={1.7} aria-hidden="true" />
                    <span>{etiqueta}</span>
                    {modulosCambiados.has(m) && <i className="rf-cambio" title="Difiere del predeterminado" aria-label="Difiere del predeterminado" />}
                    <b className={n > 0 ? 'con' : ''}>{n}</b>
                  </button>
                )
              })}
            </nav>

            <div className="rf-detalle">
              {enRevision ? (
                <>
                  <div className="rf-detalle-cabecera">
                    <div>
                      <h3>Accesos seleccionados</h3>
                      <p>
                        {seleccionados.length} de {total} permisos en {new Set(seleccionados.map((p) => p.modulo)).size} módulos.
                      </p>
                    </div>
                    <button type="button" className="as-boton-borde" onClick={() => setEnRevision(false)}>
                      Volver a los permisos
                    </button>
                  </div>
                  {(anadidos.length > 0 || quitados.length > 0) && (
                    <div className="rf-cambios">
                      <strong>Cambios respecto al predeterminado</strong>
                      {anadidos.map((p) => (
                        <div key={p.id}>
                          <b className="mas">+</b> {capitalizar(p.descripcion)} <span>{p.codigo}</span>
                        </div>
                      ))}
                      {quitados.map((p) => (
                        <div key={p.id}>
                          <b className="menos">−</b> {capitalizar(p.descripcion)} <span>{p.codigo}</span>
                        </div>
                      ))}
                    </div>
                  )}
                  {seleccionados.length === 0 && <p className="rf-vacio">Todavía no elegiste ningún acceso.</p>}
                  {modulos
                    .filter(([, lista]) => lista.some((p) => seleccion.has(p.id)))
                    .map(([m, lista]) => (
                      <div key={m}>
                        <div className="rf-grupo-titulo">{datosDeModulo(m).etiqueta}</div>
                        <div className="rf-chips">
                          {lista
                            .filter((p) => seleccion.has(p.id))
                            .map((p) => (
                              <span key={p.id} className="rf-chip">
                                {capitalizar(p.descripcion)}
                                <button type="button" aria-label={`Quitar ${p.descripcion}`} onClick={() => alternar(p.id)}>
                                  <X size={12} strokeWidth={2.6} aria-hidden="true" />
                                </button>
                              </span>
                            ))}
                        </div>
                      </div>
                    ))}
                </>
              ) : (
                <>
                  <div className="rf-detalle-cabecera">
                    <span className="rf-icono-modulo" aria-hidden="true">
                      <datosActual.Icono size={26} strokeWidth={1.6} />
                    </span>
                    <div className="rf-detalle-titulo">
                      <h3>{datosActual.etiqueta}</h3>
                      <p>{datosActual.descripcion}</p>
                    </div>
                    <button
                      type="button"
                      role="checkbox"
                      aria-checked={todosMarcados}
                      className="as-boton-borde rf-todos"
                      onClick={alternarModulo}
                    >
                      <span className={`rf-caja ${todosMarcados ? 'on' : ''}`}>{todosMarcados && <Check size={12} strokeWidth={3} />}</span>
                      Seleccionar todos en {datosActual.etiqueta}
                    </button>
                  </div>

                  <div className="rf-lista">
                    {permisosDelModulo.length === 0 && <p className="rf-vacio">Ningún permiso coincide con la búsqueda.</p>}
                    {permisosDelModulo.map((p) => {
                      const on = seleccion.has(p.id)
                      const abierto = abiertos.has(p.id) || tecnicos
                      const esAnadido = on && tienePredeterminados && !predeterminados.has(p.id)
                      const esQuitado = !on && tienePredeterminados && predeterminados.has(p.id)
                      return (
                        <div key={p.id} className="rf-permiso">
                          <div className={`rf-permiso-fila ${on ? 'on' : ''}`}>
                            <button
                              type="button"
                              role="checkbox"
                              aria-checked={on}
                              aria-label={capitalizar(p.descripcion)}
                              className={`rf-caja rf-caja-grande ${on ? 'on' : ''}`}
                              onClick={() => alternar(p.id)}
                            >
                              {on && <Check size={14} strokeWidth={3} />}
                            </button>
                            <div className="rf-permiso-texto">
                              <strong>{capitalizar(p.descripcion)}</strong>
                              <span>{detalleDe(p)}</span>
                            </div>
                            {(esAnadido || esQuitado) && (
                              <span className={`rf-marca ${esAnadido ? 'anadido' : 'quitado'}`}>{esAnadido ? 'Añadido' : 'Quitado'}</span>
                            )}
                            {esAccesoSensible(p.accion) && (
                              <span className="rf-sensible">
                                <TriangleAlert size={14} aria-hidden="true" /> Acceso sensible
                              </span>
                            )}
                            <button
                              type="button"
                              className="rf-desplegar"
                              aria-expanded={abierto}
                              aria-label={`Más información de ${p.descripcion}`}
                              onClick={() =>
                                setAbiertos((actual) => {
                                  const nuevo = new Set(actual)
                                  if (nuevo.has(p.id)) nuevo.delete(p.id)
                                  else nuevo.add(p.id)
                                  return nuevo
                                })
                              }
                            >
                              <ChevronDown size={18} aria-hidden="true" className={abierto ? 'girado' : ''} />
                            </button>
                          </div>
                          {abierto && (
                            <div className="rf-permiso-extra">
                              <div>
                                <span>Código:</span> <code>{p.codigo}</code>
                              </div>
                              {esSistema && (
                                <div>
                                  <span>En {rol?.nombre}:</span>{' '}
                                  {tienePredeterminados && predeterminados.has(p.id) ? 'viene incluido de fábrica' : 'no viene incluido de fábrica'}
                                </div>
                              )}
                              {rolesConPermisos.length > 0 && (
                                <div>
                                  <span>Lo tienen:</span> {losTienen(p.id) || 'ningún rol todavía'}
                                </div>
                              )}
                            </div>
                          )}
                        </div>
                      )
                    })}
                  </div>
                  <button type="button" className="as-enlace" onClick={() => setTecnicos((v) => !v)}>
                    {tecnicos ? 'Ocultar códigos técnicos' : 'Ver códigos técnicos'} <ChevronDown size={14} aria-hidden="true" />
                  </button>
                </>
              )}
            </div>
          </section>
        </div>

        <aside className="rf-resumen" aria-label="Resumen">
          <div className="rf-contador">
            <div>
              {seleccion.size} <span>de {total}</span>
            </div>
            <p>permisos seleccionados</p>
            <div className="rf-barra">
              <div style={{ width: `${total ? Math.round((seleccion.size * 100) / total) : 0}%` }} />
            </div>
          </div>

          {tienePredeterminados && (
            <div className="rf-predeterminado">
              <div>
                <strong>Predeterminado del rol</strong>
                <span>{predeterminados.size} permisos</span>
              </div>
              <p className={anadidos.length + quitados.length === 0 ? 'ok' : ''}>
                {anadidos.length + quitados.length === 0
                  ? 'Coincide con el predeterminado.'
                  : `+${anadidos.length} añadidos · −${quitados.length} quitados`}
              </p>
              <button
                type="button"
                className="as-boton-borde"
                disabled={anadidos.length + quitados.length === 0}
                onClick={() => setConfirmarRestaurar(true)}
              >
                <RotateCcw size={16} aria-hidden="true" /> Restaurar predeterminados
              </button>
            </div>
          )}

          <h3 className="rf-resumen-titulo">Accesos por módulo</h3>
          <ul className="rf-resumen-lista">
            {modulos.map(([m]) => {
              const n = cuentaDe(m)
              return (
                <li key={m} className={`${m === moduloActual && !enRevision ? 'actual' : ''} ${n > 0 ? 'con' : ''}`}>
                  <span>
                    {datosDeModulo(m).etiqueta}
                    {modulosCambiados.has(m) && <i className="rf-cambio" />}
                  </span>
                  <span>{n}</span>
                </li>
              )
            })}
          </ul>

          <button type="button" className="rf-revisar" onClick={() => setEnRevision(true)}>
            <Eye size={18} aria-hidden="true" /> Revisar accesos seleccionados
          </button>
        </aside>
      </div>

      <footer className="as-pie">
        <strong>{seleccion.size} permisos seleccionados</strong>
        {!esNuevo && cambiosSinGuardar > 0 && (
          <span className="rf-sin-guardar">
            {cambiosSinGuardar} {cambiosSinGuardar === 1 ? 'cambio' : 'cambios'} sin guardar
          </span>
        )}
        <span className="rf-pie-ayuda">
          {error ||
            (esNuevo
              ? nombre.trim() === ''
                ? 'Falta el nombre del rol.'
                : seleccion.size === 0
                  ? 'Elige al menos un permiso.'
                  : 'Revisa los accesos antes de crear el rol. Podrás editarlo después.'
              : cambiosSinGuardar === 0
                ? 'Sin cambios por guardar.'
                : `Los cambios valen para todas las personas con el rol ${rol?.nombre}.`)}
        </span>
        <Boton variante="secundario" onClick={volver}>
          Cancelar
        </Boton>
        <Boton onClick={guardar} cargando={guardando} disabled={!puedeGuardar}>
          {esNuevo ? 'Crear rol' : 'Guardar cambios'}
        </Boton>
      </footer>

      {aviso && (
        <div className="loc-toast" role="status">
          <IconoReactivar width={16} height={16} aria-hidden="true" /> {aviso}
        </div>
      )}

      <Modal
        abierto={confirmarRestaurar}
        titulo="Restaurar permisos predeterminados"
        onCerrar={() => setConfirmarRestaurar(false)}
        pie={
          <>
            <Boton variante="secundario" onClick={() => setConfirmarRestaurar(false)}>
              Cancelar
            </Boton>
            <Boton onClick={restaurar}>Restaurar</Boton>
          </>
        }
      >
        <p>
          El rol <strong>{rol?.nombre}</strong> volverá a sus {predeterminados.size} permisos originales. Se van a deshacer estos cambios:
        </p>
        <ul className="rf-deshacer">
          {anadidos.map((p) => (
            <li key={p.id}>
              <b className="menos">−</b> {capitalizar(p.descripcion)}
            </li>
          ))}
          {quitados.map((p) => (
            <li key={p.id}>
              <b className="mas">+</b> {capitalizar(p.descripcion)}
            </li>
          ))}
        </ul>
        <p className="as-ayuda">Todavía no se guarda: podrás revisar antes de pulsar “Guardar cambios”.</p>
      </Modal>
    </div>
  )
}
