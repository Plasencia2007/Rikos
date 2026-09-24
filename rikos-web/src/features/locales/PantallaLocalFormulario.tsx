import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useSesion } from '../../comun/sesion/SesionContexto'
import {
  Aviso,
  Boton,
  Campo,
  Checkbox,
  EstadoCargando,
  EstadoError,
  IconoContacto,
  IconoDatos,
  IconoFlechaIzquierda,
  IconoGorroChef,
  IconoUbicacion,
  ModalDeConfirmacion,
  Select,
  Tabs,
  type Pestana,
} from '../../comun/componentes'
import { ErrorApi, actualizarLocal, cambiarEstadoLocal, crearLocal, obtenerLocal, type Local } from '../../api/cliente'
import { esCodigoValido, sugerirCodigo } from './codigoLocal'
import { PREFIJOS_PAIS, separarTelefono, unirTelefono } from './prefijosPais'
import { PestanaOnlineYHorarios } from './PestanaOnlineYHorarios'
import { PestanaUbicacion } from './PestanaUbicacion'

type IdPestana = 'datos' | 'contacto' | 'online' | 'logo'

interface Formulario {
  codigo: string
  nombre: string
  direccion: string
  prefijo: string
  numero: string
  correo: string
  facebookUrl: string
  instagramUrl: string
  tiktokUrl: string
  deliveryHabilitado: boolean
  recojoHabilitado: boolean
  tiempoMinEntregaMin: string
  tiempoMaxEntregaMin: string
  logoUrl: string
}

const FORM_INICIAL: Formulario = {
  codigo: '', nombre: '', direccion: '', prefijo: PREFIJOS_PAIS[0].prefijo, numero: '', correo: '',
  facebookUrl: '', instagramUrl: '', tiktokUrl: '',
  deliveryHabilitado: true, recojoHabilitado: true, tiempoMinEntregaMin: '', tiempoMaxEntregaMin: '',
  logoUrl: '',
}

/**
 * Vista aparte (no modal): con pestañas, mapa, horario semanal y canales de
 * venta, el modal anterior se quedaba chico. Un local nuevo solo puede
 * completar "Datos básicos" -- el resto necesita que el local ya exista
 * (actualizar y horarios trabajan sobre un id).
 *
 * El PUT de actualizar reemplaza el local entero, no campo a campo -- por
 * eso hay un solo `formulario` con TODO lo editable, y las cuatro pestañas
 * (menos "Datos básicos", que crea) guardan sobre la misma copia completa
 * en vez de mandar solo lo que esa pestaña muestra.
 */
export function PantallaLocalFormulario() {
  const { id } = useParams<{ id: string }>()
  const esNuevo = id === undefined
  const navigate = useNavigate()
  const { sesion } = useSesion()
  const token = sesion!.token

  const [pestana, setPestana] = useState<IdPestana>('datos')
  const [cargando, setCargando] = useState(!esNuevo)
  const [errorCarga, setErrorCarga] = useState('')
  const [local, setLocal] = useState<Local | null>(null)

  const [formulario, setFormulario] = useState(FORM_INICIAL)
  const [codigoTocado, setCodigoTocado] = useState(false)
  const [errorCodigo, setErrorCodigo] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [errorGuardar, setErrorGuardar] = useState('')
  const [guardadoOk, setGuardadoOk] = useState(false)
  const [activo, setActivo] = useState(true)

  useEffect(() => {
    if (esNuevo) return
    setCargando(true)
    obtenerLocal(token, id!)
      .then((l) => {
        setLocal(l)
        const { prefijo, numero } = separarTelefono(l.telefono)
        setFormulario({
          codigo: l.codigo, nombre: l.nombre, direccion: l.direccion, prefijo, numero, correo: l.correo ?? '',
          facebookUrl: l.facebookUrl ?? '', instagramUrl: l.instagramUrl ?? '', tiktokUrl: l.tiktokUrl ?? '',
          deliveryHabilitado: l.deliveryHabilitado, recojoHabilitado: l.recojoHabilitado,
          tiempoMinEntregaMin: l.tiempoMinEntregaMin?.toString() ?? '', tiempoMaxEntregaMin: l.tiempoMaxEntregaMin?.toString() ?? '',
          logoUrl: l.logoUrl ?? '',
        })
        setActivo(l.activo)
      })
      .catch(() => setErrorCarga('No se pudo cargar este local.'))
      .finally(() => setCargando(false))
  }, [esNuevo, id, token])

  function cambiarNombre(nombre: string) {
    setFormulario((f) => ({
      ...f,
      nombre,
      codigo: esNuevo && !codigoTocado ? sugerirCodigo(nombre) : f.codigo,
    }))
  }

  function cambiarCodigo(codigo: string) {
    setCodigoTocado(true)
    setErrorCodigo('')
    setFormulario((f) => ({ ...f, codigo }))
  }

  function actualizarLocalDesdeRespuesta(l: Local) {
    setLocal(l)
    const { prefijo, numero } = separarTelefono(l.telefono)
    setFormulario((f) => ({ ...f, prefijo, numero }))
  }

  /** Guarda la copia completa de `formulario` -- lo usan Contactos, Online y Logo. */
  async function guardarTodo() {
    if (!local) return
    setErrorGuardar('')
    setGuardadoOk(false)
    setGuardando(true)
    try {
      const actualizado = await actualizarLocal(token, local.id, {
        nombre: formulario.nombre,
        direccion: formulario.direccion,
        telefono: unirTelefono(formulario.prefijo, formulario.numero) || null,
        correo: formulario.correo || null,
        latitud: local.latitud,
        longitud: local.longitud,
        facebookUrl: formulario.facebookUrl || null,
        instagramUrl: formulario.instagramUrl || null,
        tiktokUrl: formulario.tiktokUrl || null,
        logoUrl: formulario.logoUrl || null,
        deliveryHabilitado: formulario.deliveryHabilitado,
        recojoHabilitado: formulario.recojoHabilitado,
        tiempoMinEntregaMin: formulario.tiempoMinEntregaMin ? Number(formulario.tiempoMinEntregaMin) : null,
        tiempoMaxEntregaMin: formulario.tiempoMaxEntregaMin ? Number(formulario.tiempoMaxEntregaMin) : null,
      })
      actualizarLocalDesdeRespuesta(actualizado)
      setGuardadoOk(true)
    } catch {
      setErrorGuardar('No se pudo guardar el local.')
    } finally {
      setGuardando(false)
    }
  }

  async function guardarDatosBasicos() {
    setErrorGuardar('')
    setGuardadoOk(false)

    if (esNuevo && !esCodigoValido(formulario.codigo)) {
      setErrorCodigo('Solo minúsculas, números y guiones. Sin espacios, tildes ni ñ.')
      return
    }

    setGuardando(true)
    try {
      if (esNuevo) {
        const creado = await crearLocal(token, {
          codigo: formulario.codigo,
          nombre: formulario.nombre,
          direccion: formulario.direccion,
        })
        navigate(`/admin/locales/${creado.id}/editar`, { replace: true })
        return
      }

      const actualizado = await actualizarLocal(token, local!.id, {
        ...formulario,
        telefono: unirTelefono(formulario.prefijo, formulario.numero) || null,
        correo: formulario.correo || null,
        latitud: local!.latitud,
        longitud: local!.longitud,
        facebookUrl: formulario.facebookUrl || null,
        instagramUrl: formulario.instagramUrl || null,
        tiktokUrl: formulario.tiktokUrl || null,
        logoUrl: formulario.logoUrl || null,
        tiempoMinEntregaMin: formulario.tiempoMinEntregaMin ? Number(formulario.tiempoMinEntregaMin) : null,
        tiempoMaxEntregaMin: formulario.tiempoMaxEntregaMin ? Number(formulario.tiempoMaxEntregaMin) : null,
      })
      actualizarLocalDesdeRespuesta(actualizado)
      setGuardadoOk(true)
    } catch (excepcion) {
      if (excepcion instanceof ErrorApi && excepcion.cuerpo?.error === 'CODIGO_DUPLICADO') {
        setErrorCodigo('Ya existe un local con ese código.')
      } else if (excepcion instanceof ErrorApi && excepcion.cuerpo?.error === 'CODIGO_FORMATO_INVALIDO') {
        setErrorCodigo('Solo minúsculas, números y guiones. Sin espacios, tildes ni ñ.')
      } else {
        setErrorGuardar('No se pudo guardar el local.')
      }
    } finally {
      setGuardando(false)
    }
  }

  async function cambiarUbicacion(latitud: number, longitud: number) {
    if (!local) return
    setLocal({ ...local, latitud, longitud })
    try {
      const actualizado = await actualizarLocal(token, local.id, {
        nombre: formulario.nombre,
        direccion: formulario.direccion,
        telefono: unirTelefono(formulario.prefijo, formulario.numero) || null,
        correo: formulario.correo || null,
        latitud,
        longitud,
        facebookUrl: formulario.facebookUrl || null,
        instagramUrl: formulario.instagramUrl || null,
        tiktokUrl: formulario.tiktokUrl || null,
        logoUrl: formulario.logoUrl || null,
        deliveryHabilitado: formulario.deliveryHabilitado,
        recojoHabilitado: formulario.recojoHabilitado,
        tiempoMinEntregaMin: formulario.tiempoMinEntregaMin ? Number(formulario.tiempoMinEntregaMin) : null,
        tiempoMaxEntregaMin: formulario.tiempoMaxEntregaMin ? Number(formulario.tiempoMaxEntregaMin) : null,
      })
      actualizarLocalDesdeRespuesta(actualizado)
    } catch {
      setErrorGuardar('No se pudo guardar la ubicación.')
    }
  }

  const [confirmarCambioEstado, setConfirmarCambioEstado] = useState(false)
  const [cambiandoEstado, setCambiandoEstado] = useState(false)

  async function confirmarAlternarEstado() {
    if (!local) return
    setCambiandoEstado(true)
    try {
      const actualizado = await cambiarEstadoLocal(token, local.id, !activo)
      setActivo(actualizado.activo)
      setLocal(actualizado)
      setConfirmarCambioEstado(false)
    } finally {
      setCambiandoEstado(false)
    }
  }

  const pestanas: Pestana[] = [
    { id: 'datos', etiqueta: 'Datos Básicos', icono: <IconoDatos width={15} height={15} /> },
    { id: 'contacto', etiqueta: 'Contactos', icono: <IconoContacto width={15} height={15} /> },
    { id: 'online', etiqueta: 'Online y Horarios', icono: <IconoUbicacion width={15} height={15} /> },
    { id: 'logo', etiqueta: 'Logo y Aspecto', icono: <IconoGorroChef width={15} height={15} /> },
  ]

  if (cargando) return <EstadoCargando />
  if (errorCarga) return <EstadoError mensaje={errorCarga} />

  return (
    <section className="panel-admin">
      <div className="panel-admin-encabezado">
        <div className="panel-admin-titulo-grupo">
          <button type="button" className="boton-volver" onClick={() => navigate('/admin/locales')} aria-label="Volver a Locales">
            <IconoFlechaIzquierda width={18} height={18} />
          </button>
          <div>
            <h1>{esNuevo ? 'Nuevo local' : local?.nombre}</h1>
            <p className="panel-admin-subtitulo">{esNuevo ? 'Completa los datos básicos para crearlo.' : formulario.codigo}</p>
          </div>
        </div>
      </div>

      <div className="panel-admin-tarjeta formulario-local">
        <Tabs pestanas={pestanas} activa={pestana} onCambiar={(id) => setPestana(id as IdPestana)} />

        <div className="formulario-local-contenido">
          {pestana === 'datos' && (
            <div className="formulario-admin formulario-datos-basicos">
              <h3 className="formulario-local-subtitulo">Identificación del local</h3>
              <div className="datos-basicos-fila">
                <Campo
                  etiqueta="Código"
                  ayuda="Es la dirección web del POS de este local (ej. sanisidro.rikos.pe). Conviene no cambiarlo una vez desplegado."
                  value={formulario.codigo}
                  onChange={(e) => cambiarCodigo(e.target.value)}
                  error={errorCodigo}
                  disabled={!esNuevo}
                  required
                />
                <Campo etiqueta="Nombre" value={formulario.nombre} onChange={(e) => cambiarNombre(e.target.value)} required />
                {!esNuevo && (
                  <div className="campo">
                    <span className="campo-etiqueta">Estado</span>
                    <Checkbox
                      etiqueta="Local activo"
                      checked={activo}
                      onChange={() => setConfirmarCambioEstado(true)}
                      className="datos-basicos-estado"
                    />
                  </div>
                )}
              </div>

              <h3 className="formulario-local-subtitulo">Ubicación geográfica</h3>
              <Campo
                etiqueta="Dirección"
                value={formulario.direccion}
                onChange={(e) => setFormulario((f) => ({ ...f, direccion: e.target.value }))}
                required
              />

              {errorGuardar && <Aviso tono="error">{errorGuardar}</Aviso>}
              {guardadoOk && <Aviso tono="exito">Guardado.</Aviso>}
              <div className="formulario-local-guardar">
                <Boton onClick={guardarDatosBasicos} cargando={guardando}>
                  {esNuevo ? 'Crear local' : 'Guardar cambios'}
                </Boton>
              </div>

              {!esNuevo && local && (
                <>
                  <h3 className="formulario-local-subtitulo">Localización en el mapa</h3>
                  <PestanaUbicacion latitud={local.latitud} longitud={local.longitud} onCambiar={cambiarUbicacion} />
                </>
              )}
            </div>
          )}

          {pestana === 'contacto' &&
            (esNuevo ? (
              <PestanaPendienteDeCreacion />
            ) : (
              <div className="formulario-admin formulario-estrecho">
                <h3 className="formulario-local-subtitulo">Comunicación directa</h3>
                <div className="campo-telefono">
                  <Select
                    etiqueta="Prefijo"
                    value={formulario.prefijo}
                    onChange={(e) => setFormulario((f) => ({ ...f, prefijo: e.target.value }))}
                  >
                    {PREFIJOS_PAIS.map((p) => (
                      <option key={p.codigo} value={p.prefijo}>
                        {p.nombre} ({p.prefijo})
                      </option>
                    ))}
                  </Select>
                  <Campo
                    etiqueta="Teléfono"
                    value={formulario.numero}
                    onChange={(e) => setFormulario((f) => ({ ...f, numero: e.target.value }))}
                  />
                </div>
                <Campo
                  etiqueta="Correo del local"
                  type="email"
                  value={formulario.correo}
                  onChange={(e) => setFormulario((f) => ({ ...f, correo: e.target.value }))}
                />

                <h3 className="formulario-local-subtitulo">Redes sociales</h3>
                <p className="campo-ayuda-seccion">Enlaces directos para los botones del menú digital.</p>
                <Campo
                  etiqueta="Facebook"
                  placeholder="tu-restaurante"
                  value={formulario.facebookUrl}
                  onChange={(e) => setFormulario((f) => ({ ...f, facebookUrl: e.target.value }))}
                />
                <Campo
                  etiqueta="Instagram"
                  placeholder="@tu-restaurante"
                  value={formulario.instagramUrl}
                  onChange={(e) => setFormulario((f) => ({ ...f, instagramUrl: e.target.value }))}
                />
                <Campo
                  etiqueta="TikTok"
                  placeholder="@tu-restaurante"
                  value={formulario.tiktokUrl}
                  onChange={(e) => setFormulario((f) => ({ ...f, tiktokUrl: e.target.value }))}
                />

                {errorGuardar && <Aviso tono="error">{errorGuardar}</Aviso>}
                {guardadoOk && <Aviso tono="exito">Guardado.</Aviso>}
                <div className="formulario-local-guardar">
                  <Boton onClick={guardarTodo} cargando={guardando}>
                    Guardar cambios
                  </Boton>
                </div>
              </div>
            ))}

          {pestana === 'online' &&
            (esNuevo || !local ? (
              <PestanaPendienteDeCreacion />
            ) : (
              <PestanaOnlineYHorarios
                token={token}
                local={local}
                formulario={formulario}
                onCambiarFormulario={(cambios) => setFormulario((f) => ({ ...f, ...cambios }))}
                onGuardarCanales={guardarTodo}
                guardando={guardando}
                errorGuardar={errorGuardar}
                guardadoOk={guardadoOk}
              />
            ))}

          {pestana === 'logo' &&
            (esNuevo ? (
              <PestanaPendienteDeCreacion />
            ) : (
              <div className="formulario-admin formulario-estrecho">
                <Campo
                  etiqueta="URL del logo"
                  ayuda="Se aloja donde el propietario lo suba (ej. un CDN de imágenes); acá solo se guarda el enlace."
                  placeholder="https://…"
                  value={formulario.logoUrl}
                  onChange={(e) => setFormulario((f) => ({ ...f, logoUrl: e.target.value }))}
                />
                {formulario.logoUrl && (
                  <div className="logo-vista-previa">
                    <img src={formulario.logoUrl} alt="Logo del local" onError={(e) => (e.currentTarget.style.display = 'none')} />
                  </div>
                )}
                {errorGuardar && <Aviso tono="error">{errorGuardar}</Aviso>}
                {guardadoOk && <Aviso tono="exito">Guardado.</Aviso>}
                <div className="formulario-local-guardar">
                  <Boton onClick={guardarTodo} cargando={guardando}>
                    Guardar cambios
                  </Boton>
                </div>
              </div>
            ))}
        </div>
      </div>

      <ModalDeConfirmacion
        abierto={confirmarCambioEstado}
        titulo={activo ? 'Desactivar local' : 'Activar local'}
        mensaje={
          activo
            ? `Vas a desactivar "${local?.nombre}". Ningún local desactivado puede recibir operaciones nuevas (pedidos, caja, inventario) hasta que lo reactives.`
            : `Vas a reactivar "${local?.nombre}". Va a volver a aceptar operaciones normalmente.`
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

function PestanaPendienteDeCreacion() {
  return (
    <div className="estado-vacio">
      <p className="estado-vacio-titulo">Guarda los datos básicos primero</p>
      <p className="estado-vacio-descripcion">Esta pestaña se habilita apenas el local queda creado.</p>
    </div>
  )
}
