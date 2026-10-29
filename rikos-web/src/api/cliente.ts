/**
 * Cliente HTTP del sistema.
 *
 * Regla: ninguna pantalla escribe una direccion a mano. Todo pasa por aqui
 * y todo es relativo, para que el mismo codigo funcione apuntando al equipo
 * del local o al servidor de la nube.
 */
import type { components } from './generado/esquema'

/**
 * HT-05: tipos generados desde /api/v3/api-docs (nunca a mano, ver
 * src/api/generado/.gitkeep-equivalente en .gitignore). springdoc marca
 * TODOS los campos de un record de Java como opcionales -- no puede saber
 * cuales son realmente obligatorios sin anotaciones @Schema(required) que
 * el backend no usa. Por eso cada alias exportado envuelve el esquema
 * generado en Required<...>: conserva el contrato de tipos que el resto
 * del frontend ya asume (docenas de pantallas leen estos campos como no
 * opcionales), pero la FORMA (que campos existen, sus nombres) sigue
 * viniendo del backend real -- si alguien renombra o quita un campo alla,
 * tsc lo marca aca, que es el punto de HT-05.
 */
type Esquemas = components['schemas']

const BASE = import.meta.env.VITE_API_BASE ?? ''

export class ErrorApi extends Error {
  constructor(
    public readonly estado: number,
    mensaje: string,
    /** productoId: en errores de recetas, el ingrediente que fallo (US-010). */
    public readonly cuerpo?: { error?: string; segundosRestantes?: number; intentosRestantes?: number; productoId?: string | null },
  ) {
    super(mensaje)
  }
}

async function pedir<T>(ruta: string, opciones?: RequestInit): Promise<T> {
  const respuesta = await fetch(`${BASE}${ruta}`, {
    headers: { 'Content-Type': 'application/json' },
    ...opciones,
  })

  if (!respuesta.ok) {
    const cuerpo = await respuesta.json().catch(() => undefined)
    throw new ErrorApi(respuesta.status, `La peticion a ${ruta} fallo`, cuerpo)
  }

  if (respuesta.status === 204) {
    return undefined as T
  }

  return respuesta.json() as Promise<T>
}

/** Igual que pedir(), pero agregando el token de sesion. */
function pedirAutenticado<T>(ruta: string, token: string, opciones?: RequestInit): Promise<T> {
  return pedir<T>(ruta, {
    ...opciones,
    // El objeto headers reemplaza por completo al de pedir() cuando se
    // mezcla con ...opciones -- si no se repite Content-Type aca, se pierde
    // y el navegador manda text/plain, que el backend rechaza.
    headers: { 'Content-Type': 'application/json', ...(opciones?.headers ?? {}), Authorization: `Bearer ${token}` },
  })
}

export interface Salud {
  servicio: string
  perfilActivo: string
  baseDeDatos?: 'conectada' | 'desconectada'
  estado: string
  hora: string
}

/** Comprueba que un servicio del backend responde a traves del gateway. */
export const consultarSalud = (ruta: '/api/salud' | '/facturacion/salud') => pedir<Salud>(ruta)

// ---- Autenticacion (US-002, US-003) ----------------------------------

export interface LocalResumen {
  id: string
  codigo: string
  nombre: string
  esPrincipal: boolean
}

export interface SesionIniciada {
  token: string
  usuarioId: string
  nombreUsuario: string
  roles: string[]
  locales: LocalResumen[]
  localActivoId: string | null
}

export const iniciarSesionConContrasena = (nombreUsuario: string, contrasena: string) =>
  pedir<SesionIniciada>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ nombreUsuario, contrasena }),
  })

export const iniciarSesionConPin = (localCodigo: string, pin: string) =>
  pedir<SesionIniciada>('/api/auth/pin', {
    method: 'POST',
    body: JSON.stringify({ localCodigo, pin }),
  })

/**
 * US-006, hallazgo de revision: el token lleva el local activo (claim
 * "localActivo", lo lee RegistradorAuditoria para saber donde ocurrio
 * cada evento auditado). Cambiar de local sin pedir un token nuevo dejaba
 * ese claim desactualizado -- todo quedaba auditado contra el local del
 * login. Esto reemite el token con el local nuevo.
 */
export const cambiarLocalActivo = (token: string, localId: string) =>
  pedirAutenticado<SesionIniciada>('/api/auth/cambiar-local', token, {
    method: 'POST',
    body: JSON.stringify({ localId }),
  })

// ---- Locales (US-001) --------------------------------------------------

export type Local = Required<Esquemas['LocalRespuesta']>

export const listarLocales = (token: string) => pedirAutenticado<Local[]>('/api/locales', token)

/** No hay GET /locales/{id} individual todavia: se resuelve filtrando la lista completa. */
export const obtenerLocal = async (token: string, id: string) => {
  const encontrado = (await listarLocales(token)).find((l) => l.id === id)
  if (!encontrado) throw new ErrorApi(404, 'Local no encontrado', { error: 'NO_ENCONTRADO' })
  return encontrado
}

export interface LocalPublico {
  codigo: string
  nombre: string
}

/** Sin autenticar: la pantalla de PIN necesita el nombre real del local antes de que alguien entre. */
export const obtenerLocalPublico = (codigo: string) => pedir<LocalPublico>(`/api/locales/publico/${codigo}`)

export const crearLocal = (token: string, datos: { codigo: string; nombre: string; direccion: string }) =>
  pedirAutenticado<Local>('/api/locales', token, { method: 'POST', body: JSON.stringify(datos) })

export const actualizarLocal = (
  token: string,
  id: string,
  datos: {
    nombre: string
    direccion: string
    telefono?: string | null
    correo?: string | null
    latitud?: number | null
    longitud?: number | null
    facebookUrl?: string | null
    instagramUrl?: string | null
    tiktokUrl?: string | null
    logoUrl?: string | null
    colorPrincipal?: string | null
    portadaUrl?: string | null
    deliveryHabilitado: boolean
    recojoHabilitado: boolean
    tiempoMinEntregaMin?: number | null
    tiempoMaxEntregaMin?: number | null
  },
) => pedirAutenticado<Local>(`/api/locales/${id}`, token, { method: 'PUT', body: JSON.stringify(datos) })

export const cambiarEstadoLocal = (token: string, id: string, activo: boolean) =>
  pedirAutenticado<Local>(`/api/locales/${id}/${activo ? 'activar' : 'desactivar'}`, token, { method: 'POST' })

/** Borrado real (distinto de desactivar): solo funciona si el local nunca tuvo uso. */
export const desactivarLocal = (token: string, id: string) =>
  pedirAutenticado<Local>(`/api/locales/${id}/desactivar`, token, { method: 'POST' })

export const activarLocal = (token: string, id: string) =>
  pedirAutenticado<Local>(`/api/locales/${id}/activar`, token, { method: 'POST' })

export const eliminarLocal = (token: string, id: string) =>
  pedirAutenticado<void>(`/api/locales/${id}`, token, { method: 'DELETE' })

// ---- Horarios de servicio (US-007, adelantada) --------------------------

export const DIAS_SEMANA = ['lunes', 'martes', 'miercoles', 'jueves', 'viernes', 'sabado', 'domingo'] as const
export type DiaSemana = (typeof DIAS_SEMANA)[number]
export const SERVICIOS = ['desayuno', 'almuerzo', 'cena'] as const
export type Servicio = (typeof SERVICIOS)[number]

export interface TurnoServicio {
  id?: string
  diaSemana: DiaSemana
  servicio: Servicio
  horaInicio: string
  horaFin: string
}

export interface FranjaDiaEspecial {
  servicio: Servicio
  horaInicio: string
  horaFin: string
}

export interface DiaEspecial {
  id: string
  fecha: string
  cerrado: boolean
  descripcion: string | null
  franjas: FranjaDiaEspecial[]
}

export interface HorariosLocal {
  turnos: TurnoServicio[]
  diasEspeciales: DiaEspecial[]
}

export const obtenerHorarios = (token: string, localId: string) =>
  pedirAutenticado<HorariosLocal>(`/api/locales/${localId}/horarios`, token)

/** Reemplaza el horario semanal completo del local de una sola vez. */
export const guardarHorarios = (token: string, localId: string, turnos: TurnoServicio[]) =>
  pedirAutenticado<TurnoServicio[]>(`/api/locales/${localId}/horarios`, token, {
    method: 'PUT',
    body: JSON.stringify(turnos),
  })

export const crearDiaEspecial = (
  token: string,
  localId: string,
  datos: { fecha: string; cerrado: boolean; descripcion?: string; franjas: FranjaDiaEspecial[] },
) =>
  pedirAutenticado<DiaEspecial>(`/api/locales/${localId}/dias-especiales`, token, {
    method: 'POST',
    body: JSON.stringify(datos),
  })

export const eliminarDiaEspecial = (token: string, localId: string, id: string) =>
  pedirAutenticado<void>(`/api/locales/${localId}/dias-especiales/${id}`, token, { method: 'DELETE' })

// ---- Roles y permisos (US-004/US-006, adelantadas) ----------------------

export type Rol = Required<Esquemas['RolRespuesta']>

export const listarRoles = (token: string) => pedirAutenticado<Rol[]>('/api/roles', token)

export const permisosDeRol = (token: string, rolId: string) =>
  pedirAutenticado<string[]>(`/api/roles/${rolId}/permisos`, token)

/** Con qué permisos nació un rol del sistema; vacío para un rol personalizado. No cambia nada. */
export const permisosPredeterminadosDeRol = (token: string, rolId: string) =>
  pedirAutenticado<string[]>(`/api/roles/${rolId}/permisos-predeterminados`, token)

export const crearRol = (token: string, datos: { nombre: string; descripcion?: string; color?: string }) =>
  pedirAutenticado<Rol>('/api/roles', token, { method: 'POST', body: JSON.stringify(datos) })

export const actualizarRol = (
  token: string,
  id: string,
  datos: { nombre: string; descripcion?: string; color?: string },
) => pedirAutenticado<Rol>(`/api/roles/${id}`, token, { method: 'PUT', body: JSON.stringify(datos) })

/** Requiere identidad.administrar_permisos: reemplaza el conjunto completo de permisos del rol. */
export const asignarPermisosARol = (token: string, rolId: string, permisosIds: string[]) =>
  pedirAutenticado<void>(`/api/roles/${rolId}/permisos`, token, {
    method: 'PUT',
    body: JSON.stringify({ permisosIds }),
  })

export const eliminarRol = (token: string, id: string) =>
  pedirAutenticado<void>(`/api/roles/${id}`, token, { method: 'DELETE' })

export type Permiso = Required<Esquemas['PermisoRespuesta']>

export const listarPermisos = (token: string) => pedirAutenticado<Permiso[]>('/api/permisos', token)

export type PermisoUsuario = Required<Esquemas['PermisoUsuarioVista']>

export const permisosDeUsuario = (token: string, usuarioId: string) =>
  pedirAutenticado<PermisoUsuario[]>(`/api/usuarios/${usuarioId}/permisos`, token)

export interface ExcepcionPermiso {
  permisoId: string
  otorgado: boolean
  motivo?: string
}

/** Requiere identidad.administrar_permisos: reemplaza el conjunto completo de excepciones del usuario. */
export const asignarExcepcionesPermiso = (token: string, usuarioId: string, excepciones: ExcepcionPermiso[]) =>
  pedirAutenticado<void>(`/api/usuarios/${usuarioId}/permisos`, token, {
    method: 'PUT',
    body: JSON.stringify({ excepciones }),
  })

// ---- Usuarios (US-005) --------------------------------------------------

export type RolResumen = Required<Esquemas['RolResumen']>
export type LocalPrincipalResumen = Required<Esquemas['LocalPrincipalResumen']>
export type UltimoAcceso = Required<Esquemas['UltimoAcceso']>

/**
 * UsuarioRespuesta anida roles/localPrincipal/ultimoAcceso como schemas
 * aparte -- Required<> es superficial (no baja a los objetos anidados), asi
 * que se reconstruyen a mano con Omit + interseccion para que localPrincipal/
 * ultimoAcceso sigan siendo "| null" (pueden faltar de verdad: un usuario
 * recien creado no tiene ultimoAcceso todavia).
 */
export type Usuario = Required<
  Omit<Esquemas['UsuarioRespuesta'], 'roles' | 'localPrincipal' | 'locales' | 'ultimoAcceso'>
> & {
  roles: RolResumen[]
  localPrincipal: LocalPrincipalResumen | null
  /** Todos los locales asignados (no solo el principal). */
  locales: LocalPrincipalResumen[]
  ultimoAcceso: UltimoAcceso | null
}

/**
 * HT-05, limitacion real encontrada al migrar: UsuarioDetalleRespuesta no
 * aparece en NINGUN lado del esquema generado. UsuarioController.obtener()
 * devuelve ResponseEntity<?> (comodin) y este DTO no se usa como cuerpo de
 * ninguna otra ruta ni como @RequestBody en ningun lado -- springdoc solo
 * puede describir un tipo si lo ve en una firma concreta, y este nunca
 * aparece en una. El mismo patron ResponseEntity<?> existe en 47 metodos
 * de controlador a lo largo de todo el backend (identidad + catalogo);
 * arreglarlo en todos es un refactor aparte, no de este sprint. Se deja
 * esta interfaz escrita a mano a proposito, documentando por que.
 */
export interface UsuarioDetalle {
  id: string
  nombreUsuario: string
  nombres: string
  apellidos: string
  dni: string | null
  correo: string | null
  telefono: string | null
  activo: boolean
  rolesIds: string[]
  localesIds: string[]
  localPrincipalId: string | null
}

export const listarUsuarios = (token: string) => pedirAutenticado<Usuario[]>('/api/usuarios', token)

/** Un acceso al sistema ("sesion") o una accion que la persona hizo ("auditoria"). */
export interface ActividadUsuario {
  ocurridoEn: string
  tipo: 'sesion' | 'auditoria'
  evento: string
  entidad: string | null
}

export const actividadDeUsuario = (token: string, id: string) =>
  pedirAutenticado<ActividadUsuario[]>(`/api/usuarios/${id}/actividad`, token)

export const obtenerUsuario = (token: string, id: string) =>
  pedirAutenticado<UsuarioDetalle>(`/api/usuarios/${id}`, token)

export interface CrearUsuarioDatos {
  nombreUsuario: string
  nombres: string
  apellidos: string
  dni?: string
  correo?: string
  telefono?: string
  contrasena: string
  rolesIds: string[]
  localesIds: string[]
  localPrincipalId: string
}

export const crearUsuario = (token: string, datos: CrearUsuarioDatos) =>
  pedirAutenticado<Usuario>('/api/usuarios', token, { method: 'POST', body: JSON.stringify(datos) })

export const actualizarUsuario = (
  token: string,
  id: string,
  datos: { nombres: string; apellidos: string; dni?: string; correo?: string; telefono?: string },
) => pedirAutenticado<Usuario>(`/api/usuarios/${id}`, token, { method: 'PUT', body: JSON.stringify(datos) })

export const asignarRolesYLocales = (
  token: string,
  id: string,
  datos: { rolesIds: string[]; localesIds: string[]; localPrincipalId: string },
) =>
  pedirAutenticado<void>(`/api/usuarios/${id}/roles-locales`, token, {
    method: 'PUT',
    body: JSON.stringify(datos),
  })

export const asignarPin = (token: string, id: string, pin: string) =>
  pedirAutenticado<void>(`/api/usuarios/${id}/pin`, token, { method: 'PUT', body: JSON.stringify({ pin }) })

export const cambiarEstadoUsuario = (token: string, id: string, activo: boolean) =>
  pedirAutenticado<Usuario>(`/api/usuarios/${id}/${activo ? 'activar' : 'desactivar'}`, token, { method: 'POST' })

export const eliminarUsuario = (token: string, id: string) =>
  pedirAutenticado<void>(`/api/usuarios/${id}`, token, { method: 'DELETE' })

// ---- Catalogo (US-008, US-009, US-011) ---------------------------------

export type Categoria = Required<Esquemas['CategoriaRespuesta']>
export type Unidad = Required<Esquemas['UnidadRespuesta']>
/** tipoMenu: 'normal' | 'extra' en los dos productos del menu (US-014), null en el resto. */
export type Producto = Omit<Required<Esquemas['ProductoRespuesta']>, 'tipoMenu' | 'imagen'> & {
  tipoMenu: string | null
  /** Foto como data URL (JPEG reducido en el navegador); null si no tiene. */
  imagen: string | null
}
export type Presentacion = Required<Esquemas['PresentacionRespuesta']>
export type ConversionUnidad = Required<Esquemas['ConversionRespuesta']>
export type Proveedor = Required<Esquemas['ProveedorRespuesta']>

export const listarCategorias = (token: string) => pedirAutenticado<Categoria[]>('/api/categorias', token)

export const crearCategoria = (
  token: string,
  datos: { nombre: string; naturaleza?: string; componenteMenu?: string; categoriaPadreId?: string; orden: number },
) => pedirAutenticado<Categoria>('/api/categorias', token, { method: 'POST', body: JSON.stringify(datos) })

export const actualizarCategoria = (token: string, id: string, datos: { nombre: string; orden: number }) =>
  pedirAutenticado<Categoria>(`/api/categorias/${id}`, token, { method: 'PUT', body: JSON.stringify(datos) })

/** Guarda de un solo golpe el nuevo orden de un grupo de hermanas (arrastrar y soltar). */
export const reordenarCategorias = (
  token: string,
  datos: { categoriaPadreId: string | null; ids: string[] },
) => pedirAutenticado<void>('/api/categorias/orden', token, { method: 'PUT', body: JSON.stringify(datos) })

/** Borrado real: solo si la categoria esta vacia (sin subcategorias ni productos). */
export const eliminarCategoria = (token: string, id: string) =>
  pedirAutenticado<void>(`/api/categorias/${id}`, token, { method: 'DELETE' })

export const listarUnidades = (token: string) => pedirAutenticado<Unidad[]>('/api/unidades', token)

export const crearUnidad = (token: string, datos: { codigo: string; nombre: string; tipo: string }) =>
  pedirAutenticado<Unidad>('/api/unidades', token, { method: 'POST', body: JSON.stringify(datos) })

export const listarProductos = (token: string) => pedirAutenticado<Producto[]>('/api/productos', token)

export interface DatosProducto {
  codigo: string
  nombre: string
  categoriaId: string
  unidadBaseId: string
  esVendible: boolean
  esInsumo: boolean
  esSubreceta: boolean
  precioVenta: number | null
  controlaStock: boolean
}

export const crearProducto = (token: string, datos: DatosProducto) =>
  pedirAutenticado<Producto>('/api/productos', token, { method: 'POST', body: JSON.stringify(datos) })

export const actualizarProducto = (token: string, id: string, datos: DatosProducto) =>
  pedirAutenticado<Producto>(`/api/productos/${id}`, token, { method: 'PUT', body: JSON.stringify(datos) })

export const desactivarProducto = (token: string, id: string) =>
  pedirAutenticado<void>(`/api/productos/${id}/desactivar`, token, { method: 'POST' })

export const activarProducto = (token: string, id: string) =>
  pedirAutenticado<void>(`/api/productos/${id}/activar`, token, { method: 'POST' })

export const eliminarProducto = (token: string, id: string) =>
  pedirAutenticado<void>(`/api/productos/${id}`, token, { method: 'DELETE' })

export const registrarCostoManual = (token: string, productoId: string, costo: number) =>
  pedirAutenticado<Producto>(`/api/productos/${productoId}/costo-manual`, token, {
    method: 'PUT',
    body: JSON.stringify({ costo }),
  })

/** Pone la foto del producto (data URL de imagen) o la quita con null. */
export const ponerImagenDeProducto = (token: string, productoId: string, imagen: string | null) =>
  pedirAutenticado<Producto>(`/api/productos/${productoId}/imagen`, token, {
    method: 'PUT',
    body: JSON.stringify({ imagen }),
  })

export const asignarServiciosProducto = (token: string, productoId: string, servicios: string[]) =>
  pedirAutenticado<void>(`/api/productos/${productoId}/servicios`, token, {
    method: 'PUT',
    body: JSON.stringify({ servicios }),
  })

export const listarPresentaciones = (token: string, productoId: string) =>
  pedirAutenticado<Presentacion[]>(`/api/productos/${productoId}/presentaciones`, token)

export const crearPresentacion = (
  token: string,
  datos: { productoId: string; nombre: string; unidadId: string; factor: number; esPredeterminada: boolean },
) => pedirAutenticado<Presentacion>('/api/presentaciones', token, { method: 'POST', body: JSON.stringify(datos) })

export const listarConversiones = (token: string, productoId: string) =>
  pedirAutenticado<ConversionUnidad[]>(`/api/productos/${productoId}/conversiones`, token)

export const crearConversion = (
  token: string,
  datos: { productoId: string; unidadOrigenId: string; unidadDestinoId: string; factor: number },
) => pedirAutenticado<ConversionUnidad>('/api/conversiones', token, { method: 'POST', body: JSON.stringify(datos) })

export const listarProveedores = (token: string) => pedirAutenticado<Proveedor[]>('/api/proveedores', token)

export interface DatosProveedor {
  ruc: string | null
  razonSocial: string
  nombreComercial: string | null
  telefono: string | null
  correo: string | null
  direccion: string | null
  contacto: string | null
}

export const crearProveedor = (token: string, datos: DatosProveedor) =>
  pedirAutenticado<Proveedor>('/api/proveedores', token, { method: 'POST', body: JSON.stringify(datos) })

export const actualizarProveedor = (token: string, id: string, datos: DatosProveedor) =>
  pedirAutenticado<Proveedor>(`/api/proveedores/${id}`, token, { method: 'PUT', body: JSON.stringify(datos) })

export const desactivarProveedor = (token: string, id: string) =>
  pedirAutenticado<void>(`/api/proveedores/${id}/desactivar`, token, { method: 'POST' })

export const activarProveedor = (token: string, id: string) =>
  pedirAutenticado<void>(`/api/proveedores/${id}/activar`, token, { method: 'POST' })

export const eliminarProveedor = (token: string, id: string) =>
  pedirAutenticado<void>(`/api/proveedores/${id}`, token, { method: 'DELETE' })

// ---- Recetas (US-010) ----------------------------------------------------

/**
 * Los costos son null cuando el usuario no tiene recetas.ver_costos (el
 * backend los quita, costosVisibles=false) o cuando la receta esta
 * incompleta -- algun ingrediente sin costo todavia. Required<...> los
 * volveria obligatorios, asi que se reabren a mano.
 */
type ConCostos<T, K extends keyof T> = Omit<Required<T>, K> & { [P in K]: number | null }

export type RecetaResumen = ConCostos<
  Esquemas['RecetaResumen'],
  'costoTotal' | 'costoPorPorcion'
> & { costoIncompleto: boolean | null }
export type IngredienteReceta = ConCostos<Esquemas['IngredienteRespuesta'], 'costoUnitario' | 'costoLinea'>
export type RecetaDetalle = Omit<
  ConCostos<Esquemas['RecetaDetalle'], 'costoTotal' | 'costoPorPorcion'>,
  'ingredientes' | 'costoIncompleto' | 'actualizadoEn'
> & { ingredientes: IngredienteReceta[]; costoIncompleto: boolean | null; actualizadoEn: string | null }

export interface DatosReceta {
  rendimiento: number
  porciones: number
  ingredientes: { productoId: string; cantidad: number; unidadId: string }[]
}

export const listarRecetas = (token: string) => pedirAutenticado<RecetaResumen[]>('/api/recetas', token)

export const obtenerReceta = (token: string, id: string) =>
  pedirAutenticado<RecetaDetalle>(`/api/recetas/${id}`, token)

export const crearReceta = (token: string, datos: DatosReceta & { productoId: string }) =>
  pedirAutenticado<RecetaDetalle>('/api/recetas', token, { method: 'POST', body: JSON.stringify(datos) })

export const actualizarReceta = (token: string, id: string, datos: DatosReceta) =>
  pedirAutenticado<RecetaDetalle>(`/api/recetas/${id}`, token, { method: 'PUT', body: JSON.stringify(datos) })

// ---- Menu normal y extra (US-014) ----------------------------------------

/** precioVenta en centimos; null mientras el dueno no lo defina (RF-002). */
export type TipoMenu = Omit<Required<Esquemas['TipoMenuRespuesta']>, 'precioVenta'> & { precioVenta: number | null }

export const listarTiposMenu = (token: string) => pedirAutenticado<TipoMenu[]>('/api/menu/tipos', token)

export const cambiarPrecioMenu = (token: string, tipo: string, precioVenta: number) =>
  pedirAutenticado<TipoMenu>(`/api/menu/${tipo}/precio`, token, {
    method: 'PUT',
    body: JSON.stringify({ precioVenta }),
  })

// ---- Programacion semanal (US-013) --------------------------------------

export interface OpcionMenu {
  id: string
  productoId: string
  productoNombre: string
  /** entrada | segundo | guarnicion | refresco | postre (derivado de la categoria del producto). */
  componente: string
  tipoMenu: 'normal' | 'extra'
  porcionesEstimadas: number | null
}

export interface AdvertenciaMenu {
  fecha: string
  productoId: string
  productoNombre: string
  /** El dia anterior (el mas cercano) en que ya se sirvio. */
  repetidoEl: string
}

/**
 * Espejo de SemanaRespuesta (ProgramacionMenuController). estadoVisible
 * agrega 'en_servicio' y 'pasada', que se calculan por fecha. Los siete
 * dias siempre vienen. Escrito a mano (no Required<...>) porque varios
 * campos son nulos de verdad; la forma se contrasta con el esquema abajo.
 */
export interface SemanaMenu {
  id: string
  fechaInicio: string
  fechaFin: string
  estado: 'borrador' | 'confirmado'
  estadoVisible: 'borrador' | 'confirmado' | 'en_servicio' | 'pasada'
  confirmadaEn: string | null
  hoy: string
  dias: { fecha: string; cerrado: boolean; opciones: OpcionMenu[] }[]
  advertencias: AdvertenciaMenu[]
}

// HT-05: si el backend renombra o quita un campo, tsc lo marca aca.
type ClavesSemanaDelBackend = keyof Required<Esquemas['SemanaRespuesta']>
const _semanaCoincide: Record<ClavesSemanaDelBackend, true> = {
  id: true, fechaInicio: true, fechaFin: true, estado: true, estadoVisible: true, confirmadaEn: true, hoy: true,
  dias: true, advertencias: true,
} satisfies Record<keyof SemanaMenu, true>
void _semanaCoincide

/** null si esa semana todavia no se creo (404). */
export async function obtenerSemanaMenu(token: string, lunes: string): Promise<SemanaMenu | null> {
  try {
    return await pedirAutenticado<SemanaMenu>(`/api/menu/semanas?lunes=${lunes}`, token)
  } catch (e) {
    if (e instanceof ErrorApi && e.estado === 404) return null
    throw e
  }
}

export const crearSemanaMenu = (token: string, fechaInicio: string) =>
  pedirAutenticado<SemanaMenu>('/api/menu/semanas', token, { method: 'POST', body: JSON.stringify({ fechaInicio }) })

export const agregarOpcionMenu = (
  token: string,
  semanaId: string,
  datos: { fecha: string; productoId: string; tipoMenu: string; porcionesEstimadas: number | null },
) =>
  pedirAutenticado<SemanaMenu>(`/api/menu/semanas/${semanaId}/opciones`, token, {
    method: 'POST',
    body: JSON.stringify(datos),
  })

export const actualizarOpcionMenu = (
  token: string,
  opcionId: string,
  datos: { tipoMenu: string; porcionesEstimadas: number | null },
) => pedirAutenticado<SemanaMenu>(`/api/menu/opciones/${opcionId}`, token, { method: 'PUT', body: JSON.stringify(datos) })

export const quitarOpcionMenu = (token: string, opcionId: string) =>
  pedirAutenticado<SemanaMenu>(`/api/menu/opciones/${opcionId}`, token, { method: 'DELETE' })

export const confirmarSemanaMenu = (token: string, semanaId: string) =>
  pedirAutenticado<SemanaMenu>(`/api/menu/semanas/${semanaId}/confirmar`, token, { method: 'POST' })

export const descartarSemanaMenu = (token: string, semanaId: string) =>
  pedirAutenticado<void>(`/api/menu/semanas/${semanaId}`, token, { method: 'DELETE' })

// ---- Planes tarifarios y parametros de cobro (sprint 5) -------------------

/** Precios en centimos; null = todavia sin precio. */
export interface VersionPlan {
  vigenteDesde: string
  vigenteHasta: string | null
  precioDesayuno: number | null
  precioAlmuerzo: number | null
  precioCena: number | null
  /** vigente (rige hoy), programada (empieza despues) o cerrada (ya termino). */
  estado: 'vigente' | 'programada' | 'cerrada'
}

/** versiones: el historial completo, la mas nueva primero. hoy: la fecha del servidor (zona Lima). */
export interface PlanTarifario {
  id: string
  nombre: string
  descripcion: string | null
  hoy: string
  versiones: VersionPlan[]
}

export interface PreciosDePlan {
  precioDesayuno: number | null
  precioAlmuerzo: number | null
  precioCena: number | null
}

export const listarPlanes = (token: string) => pedirAutenticado<PlanTarifario[]>('/api/planes', token)

export const crearPlan = (
  token: string,
  datos: PreciosDePlan & { nombre: string; descripcion: string | null; vigenteDesde: string | null },
) => pedirAutenticado<PlanTarifario>('/api/planes', token, { method: 'POST', body: JSON.stringify(datos) })

export const cambiarPreciosPlan = (
  token: string,
  planId: string,
  datos: PreciosDePlan & { vigenteDesde: string | null },
) =>
  pedirAutenticado<PlanTarifario>(`/api/planes/${planId}/precios`, token, {
    method: 'PUT',
    body: JSON.stringify(datos),
  })

/** taperPrecio en centimos; null = sin monto (no se cobra taper). */
export interface ParametrosDeCobro {
  taperPrecio: number | null
  reglaSegundoConsumo: 'precio_del_plan' | 'precio_normal'
}

export const obtenerParametros = (token: string) => pedirAutenticado<ParametrosDeCobro>('/api/parametros', token)

export const cambiarParametro = (token: string, clave: 'taper_precio' | 'segundo_consumo_regla', valor: string | null) =>
  pedirAutenticado<ParametrosDeCobro>(`/api/parametros/${clave}`, token, {
    method: 'PUT',
    body: JSON.stringify({ valor }),
  })

/** Motor de precios (sprint 5, 3.5). Todo en centimos; precio null = no calculable, mira los avisos. */
export interface SolicitudDePrecio {
  tipo: 'menu_del_dia' | 'menu_extra' | 'agregado'
  comida: 'desayuno' | 'almuerzo' | 'cena'
  precioNormal: number | null
  /** El plan del pensionista con ticket; null = cliente normal. */
  planId: string | null
  segundoDelDia: boolean
  paraLlevar: boolean
}

export interface PrecioCalculado {
  precio: number | null
  taper: number | null
  total: number | null
  avisos: ('SIN_PRECIO_NORMAL' | 'PLAN_SIN_PRECIO' | 'TAPER_SIN_MONTO')[]
  explicacion: string[]
}

export const calcularPrecio = (token: string, datos: SolicitudDePrecio) =>
  pedirAutenticado<PrecioCalculado>('/api/precios/calculo', token, { method: 'POST', body: JSON.stringify(datos) })

// ---- Pensionistas (US-016) ------------------------------------------------

export type ComidaHabitual = 'desayuno' | 'almuerzo' | 'cena'

export interface ResumenPensionista {
  id: string
  dni: string
  nombres: string
  apellidos: string
  institucion: string | null
  planId: string
  planNombre: string | null
  comidas: ComidaHabitual[]
  activo: boolean
}

export interface ResponsablePensionista {
  nombreCompleto: string
  dni: string
  telefono: string
  correo: string
  parentesco: string
  direccion: string
}

/** Sin PIN ni contrasena, nunca: el backend no los devuelve. limiteCredito null = el tope se calcula. */
export interface DetallePensionista {
  id: string
  dni: string
  nombres: string
  apellidos: string
  telefono: string | null
  correo: string | null
  institucion: string | null
  codigoEstudiante: string | null
  carrera: string | null
  ciclo: string | null
  planId: string
  planNombre: string | null
  comidas: ComidaHabitual[]
  frecuenciaAbono: string | null
  limiteCredito: number | null
  activo: boolean
  cuentaId: string | null
  responsable: ResponsablePensionista | null
  creadoEn: string
}

export interface DatosPensionista {
  dni: string
  nombres: string
  apellidos: string
  telefono: string | null
  correo: string | null
  institucion: string | null
  codigoEstudiante: string | null
  carrera: string | null
  ciclo: string | null
  planId: string
  comidas: ComidaHabitual[]
  frecuenciaAbono: string | null
  responsable: ResponsablePensionista
}

export const listarPensionistas = (token: string) => pedirAutenticado<ResumenPensionista[]>('/api/pensionistas', token)

export const obtenerPensionista = (token: string, id: string) =>
  pedirAutenticado<DetallePensionista>(`/api/pensionistas/${id}`, token)

/** pin: en claro, SOLO en esta respuesta. */
export const crearPensionista = (token: string, datos: DatosPensionista) =>
  pedirAutenticado<{ pensionista: DetallePensionista; pin: string }>('/api/pensionistas', token, {
    method: 'POST',
    body: JSON.stringify(datos),
  })

export const actualizarPensionista = (token: string, id: string, datos: DatosPensionista) =>
  pedirAutenticado<DetallePensionista>(`/api/pensionistas/${id}`, token, { method: 'PUT', body: JSON.stringify(datos) })

export const desactivarPensionista = (token: string, id: string) =>
  pedirAutenticado<void>(`/api/pensionistas/${id}/desactivar`, token, { method: 'POST' })

export const activarPensionista = (token: string, id: string) =>
  pedirAutenticado<void>(`/api/pensionistas/${id}/activar`, token, { method: 'POST' })

/** El PIN nuevo, en claro, una sola vez. */
export const regenerarPinPensionista = (token: string, id: string) =>
  pedirAutenticado<{ pin: string }>(`/api/pensionistas/${id}/pin`, token, { method: 'POST' })

// ---- Cuenta corriente del pensionista (US-018) ----------------------------

export type SignoMovimiento = 'cargo' | 'credito'
export type TipoMovimientoCuenta = 'consumo' | 'abono' | 'ajuste' | 'arrastre' | 'saldo_inicial'

/** monto null = no se pudo calcular (ver aviso); el tope es un recordatorio, nunca un limite. */
export interface TopeDeCredito {
  monto: number | null
  origen: 'calculado' | 'fijado'
  aviso: 'PLAN_SIN_PRECIO' | 'SIN_DIAS_DE_ATENCION' | null
  comidasContadas: number
}

/** Todo en centimos. saldo negativo = deuda. */
export interface ResumenDeCuenta {
  saldo: number
  deuda: number
  saldoAFavor: number
  tope: TopeDeCredito
  creditoDisponible: number | null
  superaTope: boolean
}

export interface MovimientoDeCuenta {
  id: string
  tipo: TipoMovimientoCuenta
  signo: SignoMovimiento
  importe: number
  localId: string
  localNombre: string | null
  pedidoId: string | null
  motivo: string | null
  descripcion: string | null
  ocurridoEn: string
  registradoEn: string
  registradoPorNombre: string | null
}

export interface CuentaDePensionista {
  resumen: ResumenDeCuenta
  movimientos: MovimientoDeCuenta[]
}

/** Una fila de la lista de cuentas corrientes; todo en centimos, la mayor deuda primero. */
export interface FilaDeCuentaCorriente {
  pensionistaId: string
  dni: string
  nombres: string
  apellidos: string
  planNombre: string | null
  activo: boolean
  saldo: number
  deuda: number
  saldoAFavor: number
  tope: TopeDeCredito
  creditoDisponible: number | null
  superaTope: boolean
}

export const listarCuentasCorrientes = (token: string) =>
  pedirAutenticado<FilaDeCuentaCorriente[]>('/api/pensionistas/cuentas', token)

export const obtenerCuenta = (token: string, pensionistaId: string) =>
  pedirAutenticado<CuentaDePensionista>(`/api/pensionistas/${pensionistaId}/cuenta`, token)

/** Una sola vez por cuenta y solo oficina central. */
export const cargarSaldoInicial = (
  token: string,
  pensionistaId: string,
  datos: { importe: number; signo: SignoMovimiento; fechaCorte: string | null; descripcion: string | null },
) =>
  pedirAutenticado<CuentaDePensionista>(`/api/pensionistas/${pensionistaId}/cuenta/saldo-inicial`, token, {
    method: 'POST',
    body: JSON.stringify(datos),
  })

export const registrarAjusteDeCuenta = (
  token: string,
  pensionistaId: string,
  datos: { importe: number; signo: SignoMovimiento; motivoId: string; observacion: string | null },
) =>
  pedirAutenticado<CuentaDePensionista>(`/api/pensionistas/${pensionistaId}/cuenta/ajustes`, token, {
    method: 'POST',
    body: JSON.stringify(datos),
  })

/** limite null = que el tope se calcule. */
export const fijarTopeDeCredito = (token: string, pensionistaId: string, limite: number | null) =>
  pedirAutenticado<CuentaDePensionista>(`/api/pensionistas/${pensionistaId}/tope`, token, {
    method: 'PUT',
    body: JSON.stringify({ limite }),
  })

// ---- Mesas (US-027) --------------------------------------------------------

export type EstadoDeMesa = 'libre' | 'ocupada' | 'unida' | 'pendiente_cierre' | 'bloqueada'

/** Siempre del local activo del token: el local nunca viaja en la peticion. El estado se deriva en el servidor. */
export interface Mesa {
  id: string
  numero: string
  nombre: string | null
  capacidad: number | null
  estado: EstadoDeMesa
  motivoBloqueo: string | null
  mesaPrincipalId: string | null
  activo: boolean
  /** El salon donde esta. Null solo en una fila insertada por fuera del alta. */
  salonId: string | null
  forma: FormaDeMesa
  vip: boolean
  zonaExterior: boolean
  zonaFumadores: boolean
  /** Lugar en el plano de su salon (esquina superior izquierda, px); null si nunca se ubico. */
  posX: number | null
  posY: number | null
  ancho: number
  alto: number
  /** Desde cuando esta abierto el pedido en curso (ISO); null o ausente si no hay pedido. */
  abiertaEn?: string | null
  /** Libre pero con un pedido abierto sin comandar (alguien esta tomando el pedido): no se la ofrezcas a otro cliente. */
  tomandoPedido?: boolean
}

export type FormaDeMesa = 'cuadrada' | 'redonda' | 'rectangular'

export interface DatosMesa {
  numero: string
  nombre: string | null
  capacidad: number | null
  /** Sin salon va al "Principal" del local; sin forma es cuadrada; sin medidas, las de la forma. */
  salonId?: string | null
  forma?: FormaDeMesa | null
  vip?: boolean
  zonaExterior?: boolean
  zonaFumadores?: boolean
  ancho?: number | null
  alto?: number | null
}

/** todas=true incluye las inactivas (administracion); el mapa del POS pide solo las activas. */
export const listarMesas = (token: string, todas = false) =>
  pedirAutenticado<Mesa[]>(`/api/mesas${todas ? '?todas=true' : ''}`, token)

export const crearMesa = (token: string, datos: DatosMesa) =>
  pedirAutenticado<Mesa>('/api/mesas', token, { method: 'POST', body: JSON.stringify(datos) })

export const actualizarMesa = (token: string, id: string, datos: DatosMesa) =>
  pedirAutenticado<Mesa>(`/api/mesas/${id}`, token, { method: 'PUT', body: JSON.stringify(datos) })

export const desactivarMesa = (token: string, id: string) =>
  pedirAutenticado<Mesa>(`/api/mesas/${id}/desactivar`, token, { method: 'POST' })

export const activarMesa = (token: string, id: string) =>
  pedirAutenticado<Mesa>(`/api/mesas/${id}/activar`, token, { method: 'POST' })

export const bloquearMesa = (token: string, id: string, motivo: string) =>
  pedirAutenticado<Mesa>(`/api/mesas/${id}/bloquear`, token, { method: 'POST', body: JSON.stringify({ motivo }) })

export const liberarMesa = (token: string, id: string) =>
  pedirAutenticado<Mesa>(`/api/mesas/${id}/liberar`, token, { method: 'POST' })

// ---- Salones y plano de mesas ------------------------------------------------

export type IconoDeSalon = 'terraza' | 'cubiertos' | 'copa' | 'arbol' | 'taza' | 'mesa'
export type ColorDeSalon = 'verde' | 'azul' | 'violeta' | 'ambar' | 'turquesa' | 'gris'

/** mesas: todas las del salon; mesasActivas y capacidad: solo las activas. Siempre del local activo del token. */
export interface Salon {
  id: string
  nombre: string
  descripcion: string | null
  icono: IconoDeSalon
  color: ColorDeSalon
  activo: boolean
  anchoPlano: number
  altoPlano: number
  colorPiso: string
  mesas: number
  mesasActivas: number
  capacidad: number
}

export interface DatosSalon {
  nombre: string
  descripcion: string | null
  icono: IconoDeSalon
  color: ColorDeSalon
  anchoPlano: number | null
  altoPlano: number | null
  colorPiso: string | null
}

export const listarSalones = (token: string) => pedirAutenticado<Salon[]>('/api/salones', token)

export const obtenerSalon = (token: string, id: string) => pedirAutenticado<Salon>(`/api/salones/${id}`, token)

export const crearSalon = (token: string, datos: DatosSalon) =>
  pedirAutenticado<Salon>('/api/salones', token, { method: 'POST', body: JSON.stringify(datos) })

export const actualizarSalon = (token: string, id: string, datos: DatosSalon) =>
  pedirAutenticado<Salon>(`/api/salones/${id}`, token, { method: 'PUT', body: JSON.stringify(datos) })

export const cambiarEstadoSalon = (token: string, id: string, activo: boolean) =>
  pedirAutenticado<Salon>(`/api/salones/${id}/${activo ? 'activar' : 'desactivar'}`, token, { method: 'POST' })

/** Un salon con mesas no se elimina (409 TIENE_MESAS): se desactiva. */
export const eliminarSalon = (token: string, id: string) =>
  pedirAutenticado<void>(`/api/salones/${id}`, token, { method: 'DELETE' })

export interface MesaEnPlano {
  mesaId: string
  x: number
  y: number
  ancho: number
  alto: number
}

/** Guarda de una vez donde queda cada mesa del salon y cuanto mide (todo o nada). */
export const guardarPlano = (token: string, salonId: string, mesas: MesaEnPlano[]) =>
  pedirAutenticado<void>(`/api/salones/${salonId}/plano`, token, { method: 'PUT', body: JSON.stringify({ mesas }) })

/** Los permisos efectivos del usuario autenticado (a diferencia de permisosDeUsuario, no exige ser administrador). */
export const permisosPropios = (token: string) => pedirAutenticado<string[]>('/api/auth/mis-permisos', token)

// ---- Pedidos (US-028) ----------------------------------------------------------

export type TipoDeLinea = 'menu_normal' | 'menu_extra' | 'plan_desayuno' | 'plan_cena' | 'producto' | 'extra_libre'
export type ServicioDelPedido = 'desayuno' | 'almuerzo' | 'cena'

export interface ComponenteDelPedido {
  componente: string
  productoNombre: string | null
}

/** Todo en centimos. Una linea anulada no suma. */
export interface LineaDelPedido {
  id: string
  tipoLinea: TipoDeLinea
  productoId: string | null
  productoNombre: string | null
  descripcion: string | null
  cantidad: number
  observacion: string | null
  estado: 'pendiente' | 'comandado' | 'anulado' | 'cobrado'
  precioUnitario: number
  taperUnitario: number
  tarifaOrigen: 'publico' | 'plan' | 'extra' | 'manual'
  segundoDelDia: boolean
  componentes: ComponenteDelPedido[]
  /** Solo con estado "anulado" (US-031): la receta ya se gasto porque cocina la habia recibido. */
  anuladaPostComanda: boolean
  motivoAnulacion: string | null
}

export interface PersonaDelPedido {
  id: string
  etiqueta: string
  tipoCliente: 'regular' | 'pensionista'
  pensionistaId: string | null
  preTicketId?: string | null
  lineas: LineaDelPedido[]
  subtotal: number
  taper: number
  total: number
}

/** avisos: por ahora TAPER_SIN_MONTO (es para llevar y el duenio no ha puesto el monto del taper). */
export interface Pedido {
  id: string
  numero: string
  canal: 'mesa' | 'para_llevar'
  servicio: ServicioDelPedido
  mesaId: string | null
  estado: 'abierto' | 'comandado' | 'pendiente_cierre' | 'cerrado'
  personas: PersonaDelPedido[]
  subtotal: number
  taper: number
  total: number
  avisos: string[]
  /** A nombre de quien es el pedido (opcional, solo para reconocer la mesa). */
  nombreCliente: string | null
  /** Nombre de usuario del mozo que abrio el pedido. */
  mozoNombre?: string | null
  abiertoEn?: string | null
}

export interface OpcionOfrecida {
  opcionMenuId: string
  productoNombre: string
}

export interface ComponenteOfrecido {
  componente: string
  opciones: OpcionOfrecida[]
}

export interface ProductoOfrecido {
  id: string
  nombre: string
  precio: number
  /** Segun la categoria raiz del producto; null para bebidas, postres, etc. (solo salen en "Todo"). */
  grupo: "entrada" | "segundo" | "extra" | null
  /** Nombre de su subcategoria (para el subtitulo de la tarjeta). */
  categoria: string
  /** Su foto (data URL) o null. */
  imagen: string | null
}

export interface OfertaDelDia {
  fecha: string
  servicio: ServicioDelPedido
  hayMenu: boolean
  precioMenuNormal: number | null
  precioMenuExtra: number | null
  normal: ComponenteOfrecido[]
  extra: ComponenteOfrecido[]
  carta: ProductoOfrecido[]
}

export interface DatosDeLinea {
  personaId: string
  tipo: TipoDeLinea
  cantidad: number
  productoId?: string
  observacion?: string | null
  componentes?: { componente: string; opcionMenuId: string }[]
  /** Solo en extra_libre. */
  descripcion?: string
  /** Solo en extra_libre, en centimos: el unico precio que escribe el mozo. */
  precio?: number
}

export const abrirPedido = (
  token: string,
  datos: {
    canal: 'mesa' | 'para_llevar'
    mesaId?: string
    servicio?: ServicioDelPedido
    /** 1 a 30; con N se crean las personas "Persona 1" a "Persona N". */
    comensales?: number
    nombreCliente?: string
  },
) => pedirAutenticado<Pedido>('/api/pedidos', token, { method: 'POST', body: JSON.stringify(datos) })

export type EstadoDelPedido = Pedido['estado']

export interface LineaDeResumen {
  cantidad: number
  nombre: string
  componentes: string[]
  estado: string
}

/** Una fila de la lista de pedidos del local. lineas solo viene en los pedidos en curso (sin las anuladas). */
export interface ResumenDePedido {
  id: string
  numero: string
  canal: 'mesa' | 'para_llevar'
  servicio: ServicioDelPedido
  estado: EstadoDelPedido
  mesaId: string | null
  mesaNumero: string | null
  mesaNombre: string | null
  salonNombre: string | null
  nombreCliente: string | null
  comensales: number
  mozoNombre: string | null
  abiertoEn: string
  total: number
  lineas: LineaDeResumen[] | null
}

export interface PaginaDePedidos {
  contenido: ResumenDePedido[]
  pagina: number
  tamano: number
  total: number
  totalPaginas: number
}

export interface FiltrosDePedidos {
  situacion?: 'en_curso' | 'cerrados'
  canal?: 'mesa' | 'para_llevar'
  estado?: EstadoDelPedido
  q?: string
  /** AAAA-MM-DD */
  desde?: string
  hasta?: string
  pagina?: number
  tamano?: number
}

export const listarPedidos = (token: string, filtros: FiltrosDePedidos = {}) => {
  const parametros = new URLSearchParams()
  Object.entries(filtros).forEach(([clave, valor]) => {
    if (valor !== undefined && valor !== '') parametros.set(clave, String(valor))
  })
  const consulta = parametros.toString()
  return pedirAutenticado<PaginaDePedidos>(`/api/pedidos${consulta ? `?${consulta}` : ''}`, token)
}

/** Conteos baratos para el menu del POS y el Inicio. porCobrar = pedidos pendientes de cierre. */
export const resumenDePedidos = (token: string) =>
  pedirAutenticado<{ enCurso: number; porCobrar: number }>('/api/pedidos/resumen', token)

export const obtenerPedido = (token: string, id: string) => pedirAutenticado<Pedido>(`/api/pedidos/${id}`, token)

/** 404 si la mesa no tiene un pedido en curso. */
export const obtenerPedidoDeMesa = (token: string, mesaId: string) =>
  pedirAutenticado<Pedido>(`/api/mesas/${mesaId}/pedido`, token)

export const ofertaDelPedido = (token: string, id: string) =>
  pedirAutenticado<OfertaDelDia>(`/api/pedidos/${id}/oferta`, token)

export const agregarPersonaAlPedido = (token: string, pedidoId: string, etiqueta?: string) =>
  pedirAutenticado<Pedido>(`/api/pedidos/${pedidoId}/personas`, token, {
    method: 'POST',
    body: JSON.stringify({ etiqueta: etiqueta ?? null }),
  })

export const asignarPensionistaAPersona = (token: string, pedidoId: string, personaId: string, pensionistaId: string) =>
  pedirAutenticado<Pedido>(`/api/pedidos/${pedidoId}/personas/${personaId}/pensionista`, token, {
    method: 'PUT',
    body: JSON.stringify({ pensionistaId }),
  })

/** US-020: Vincula el pre-ticket emitido en el kiosco (código de 6 dígitos) a una persona de la mesa */
export const vincularPreTicketAPersona = (token: string, pedidoId: string, personaId: string, codigo: string) =>
  pedirAutenticado<Pedido>(`/api/pedidos/${pedidoId}/personas/${personaId}/pre-ticket`, token, {
    method: 'PUT',
    body: JSON.stringify({ codigo }),
  })

export const quitarPersonaDelPedido = (token: string, pedidoId: string, personaId: string) =>
  pedirAutenticado<Pedido>(`/api/pedidos/${pedidoId}/personas/${personaId}`, token, { method: 'DELETE' })

export const agregarLineaAlPedido = (token: string, pedidoId: string, datos: DatosDeLinea) =>
  pedirAutenticado<Pedido>(`/api/pedidos/${pedidoId}/lineas`, token, { method: 'POST', body: JSON.stringify(datos) })

export const modificarLineaDelPedido = (
  token: string,
  pedidoId: string,
  lineaId: string,
  datos: { cantidad: number; observacion: string | null },
) =>
  pedirAutenticado<Pedido>(`/api/pedidos/${pedidoId}/lineas/${lineaId}`, token, {
    method: 'PUT',
    body: JSON.stringify(datos),
  })

export const quitarLineaDelPedido = (token: string, pedidoId: string, lineaId: string) =>
  pedirAutenticado<Pedido>(`/api/pedidos/${pedidoId}/lineas/${lineaId}`, token, { method: 'DELETE' })

// ---- Comanda y anulaciones (US-030, US-031) -------------------------------------

export interface ComandaResumen {
  id: string
  numero: number
  emitidaEn: string
  impresiones: number
  ultimaImpresionEn: string | null
  estadoImpresion: 'pendiente' | 'impresa' | 'fallida'
}

export interface LineaDeComanda {
  cantidad: number
  nombre: string
  componentes: string[]
  observacion: string | null
  /** Los componentes con lo que es cada uno (entrada, segundo...), en el orden del menú. */
  partes?: ComponenteDelPedido[]
}

export interface PersonaDeComanda {
  etiqueta: string
  lineas: LineaDeComanda[]
}

/** Lo que se mando a cocina, listo para imprimir. mesa es null si es para llevar. */
export interface ComandaDetalle {
  id: string
  numero: number
  pedidoNumero: string
  canal: 'mesa' | 'para_llevar'
  mesa: string | null
  servicio: ServicioDelPedido
  mozo: string | null
  emitidaEn: string
  impresiones: number
  personas: PersonaDeComanda[]
}

/** copia: es la segunda impresion o posterior, y sale con la leyenda COPIA. */
export interface ImpresionRegistrada {
  id: string
  numero: number
  impresiones: number
  copia: boolean
}

export const comandarPedido = (token: string, pedidoId: string) =>
  pedirAutenticado<ComandaResumen>(`/api/pedidos/${pedidoId}/comandas`, token, { method: 'POST', body: '{}' })

export const listarComandasDelPedido = (token: string, pedidoId: string) =>
  pedirAutenticado<ComandaResumen[]>(`/api/pedidos/${pedidoId}/comandas`, token)

export const obtenerComanda = (token: string, id: string) =>
  pedirAutenticado<ComandaDetalle>(`/api/comandas/${id}`, token)

export const registrarImpresionDeComanda = (token: string, id: string) =>
  pedirAutenticado<ImpresionRegistrada>(`/api/comandas/${id}/impresiones`, token, { method: 'POST' })

/** Requiere venta.anular. Devuelve el pedido completo. */
export const anularLineaDelPedido = (
  token: string,
  pedidoId: string,
  lineaId: string,
  datos: { motivoId: string; observacion: string | null },
) =>
  pedirAutenticado<Pedido>(`/api/pedidos/${pedidoId}/lineas/${lineaId}/anular`, token, {
    method: 'POST',
    body: JSON.stringify(datos),
  })

// ---- Reorganizar (US-029) ---------------------------------------------------------

/** personaDestinoId null = una persona nueva (dividir la cuenta). Devuelve el pedido completo, con el precio ya recalculado. */
export const trasladarLineasDelPedido = (
  token: string,
  pedidoId: string,
  lineaIds: string[],
  personaDestinoId: string | null,
) =>
  pedirAutenticado<Pedido>(`/api/pedidos/${pedidoId}/lineas/trasladar`, token, {
    method: 'POST',
    body: JSON.stringify({ lineaIds, personaDestinoId }),
  })

/** Cierre SIN cobro (provisional hasta el sprint 7): el pedido queda cerrado y la mesa libre. Devuelve el pedido. */
export const terminarPedido = (token: string, pedidoId: string) =>
  pedirAutenticado<Pedido>(`/api/pedidos/${pedidoId}/terminar`, token, { method: 'POST' })

export const moverPedidoDeMesa = (token: string, pedidoId: string, mesaId: string) =>
  pedirAutenticado<Pedido>(`/api/pedidos/${pedidoId}/mesa`, token, { method: 'PUT', body: JSON.stringify({ mesaId }) })

export const unirMesa = (token: string, mesaId: string, principalId: string) =>
  pedirAutenticado<Mesa>(`/api/mesas/${mesaId}/unir`, token, { method: 'POST', body: JSON.stringify({ principalId }) })

export const separarMesa = (token: string, mesaId: string) =>
  pedirAutenticado<Mesa>(`/api/mesas/${mesaId}/separar`, token, { method: 'POST' })

/** Por nombre o DNI (el mozo lo necesita para asignar un pensionista). */
export const buscarPensionistas = (token: string, q: string) =>
  pedirAutenticado<ResumenPensionista[]>(`/api/pensionistas?q=${encodeURIComponent(q)}`, token)

// ---- Disponibilidad por local (US-015) -----------------------------------

/** Siempre del local activo del token: el local nunca viaja en la peticion. */
export interface Agotado {
  productoId: string
  productoNombre: string
  marcadoEn: string
}

export const listarAgotados = (token: string) => pedirAutenticado<Agotado[]>('/api/disponibilidad/agotados', token)

export const marcarAgotado = (token: string, productoId: string) =>
  pedirAutenticado<void>(`/api/disponibilidad/${productoId}/agotar`, token, { method: 'POST' })

export const reactivarProducto = (token: string, productoId: string, motivoId: string, observacion: string | null) =>
  pedirAutenticado<void>(`/api/disponibilidad/${productoId}/reactivar`, token, {
    method: 'POST',
    body: JSON.stringify({ motivoId, observacion }),
  })

/** Menu confirmado de un dia, con "agotado" segun el local activo. null si no hay menu confirmado (404). */
export interface MenuDelDia {
  fecha: string
  opciones: (OpcionMenu & { agotado: boolean })[]
}

export async function obtenerMenuDelDia(token: string, fecha: string): Promise<MenuDelDia | null> {
  try {
    return await pedirAutenticado<MenuDelDia>(`/api/menu/dias/${fecha}`, token)
  } catch (e) {
    if (e instanceof ErrorApi && e.estado === 404) return null
    throw e
  }
}

// ---- Motivos configurables (US-012) --------------------------------------

export type Motivo = Required<Esquemas['MotivoRespuesta']>

export const listarMotivos = (token: string, tipo?: string) =>
  pedirAutenticado<Motivo[]>(`/api/motivos${tipo ? `?tipo=${encodeURIComponent(tipo)}` : ''}`, token)

export const crearMotivo = (
  token: string,
  datos: { tipo: string; descripcion: string; requiereObservacion: boolean },
) => pedirAutenticado<Motivo>('/api/motivos', token, { method: 'POST', body: JSON.stringify(datos) })

export const actualizarMotivo = (
  token: string,
  id: string,
  datos: { descripcion: string; requiereObservacion: boolean },
) => pedirAutenticado<Motivo>(`/api/motivos/${id}`, token, { method: 'PUT', body: JSON.stringify(datos) })

export const desactivarMotivo = (token: string, id: string) =>
  pedirAutenticado<void>(`/api/motivos/${id}/desactivar`, token, { method: 'POST' })

export const activarMotivo = (token: string, id: string) =>
  pedirAutenticado<void>(`/api/motivos/${id}/activar`, token, { method: 'POST' })

// ---- Bitácora de auditoría (US-006, ADM-31) ------------------------------

/**
 * Espejo de AuditoriaRespuesta.java. "descripcion" ya viene armada en
 * español desde el backend (CA3) -- es el texto principal de cada fila.
 * valorAnterior/valorNuevo son JSON crudo en texto, solo para el detalle
 * al expandir, nunca para volver a parsearlos en el frontend.
 */
export interface RegistroAuditoria {
  id: string
  evento: string
  entidad: string
  entidadId: string | null
  usuarioId: string | null
  usuarioNombre: string | null
  localId: string | null
  localNombre: string | null
  descripcion: string
  valorAnterior: string | null
  valorNuevo: string | null
  observacion: string | null
  ocurridoEn: string
}

/** Forma minima de un Page<T> de Spring que esta pantalla necesita. */
export interface PaginaAuditoria {
  content: RegistroAuditoria[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface FiltroAuditoria {
  localId?: string
  usuarioId?: string
  evento?: string
  desde?: string
  hasta?: string
  pagina?: number
  tamano?: number
}

export const listarAuditoria = (token: string, filtro: FiltroAuditoria = {}) => {
  const parametros = new URLSearchParams()
  if (filtro.localId) parametros.set('localId', filtro.localId)
  if (filtro.usuarioId) parametros.set('usuarioId', filtro.usuarioId)
  if (filtro.evento) parametros.set('evento', filtro.evento)
  if (filtro.desde) parametros.set('desde', filtro.desde)
  if (filtro.hasta) parametros.set('hasta', filtro.hasta)
  parametros.set('pagina', String(filtro.pagina ?? 0))
  parametros.set('tamano', String(filtro.tamano ?? 30))
  return pedirAutenticado<PaginaAuditoria>(`/api/auditoria?${parametros.toString()}`, token)
}

// ---- Caja (sprint 7) ----

export interface TurnoDeCaja {
  id: string
  cajaId: string
  cajeroId: string
  cajeroNombre: string | null
  /** En centimos. */
  fondoInicial: number
  abiertoEn: string
  estado: 'abierto' | 'cerrado'
  /** "Caja de <cajero>": como se rotula la caja mientras el turno esta abierto. */
  etiqueta: string | null
  /** Nota opcional que el cajero escribio al abrir. */
  notaApertura: string | null
}

export interface CajaDelLocal {
  id: string
  nombre: string
  /** Codigo corto unico en el local (CAJ-01...). */
  codigo: string
  /** La que viene elegida al abrir turno; una sola por local. */
  principal: boolean
  etiqueta: string
  activo: boolean
  turnoAbierto: TurnoDeCaja | null
}

export const listarCajas = (token: string) => pedirAutenticado<CajaDelLocal[]>('/api/cajas', token)

export const abrirTurnoDeCaja = (token: string, cajaId: string, fondoInicial: number, nota?: string | null) =>
  pedirAutenticado<TurnoDeCaja>(`/api/cajas/${cajaId}/turnos`, token, {
    method: 'POST',
    body: JSON.stringify({ fondoInicial, nota: nota ?? null }),
  })

// Cajas registradoras (Configuracion): se administran desde el panel; el POS solo elige entre las registradas.
export const crearCaja = (token: string, datos: { nombre: string; codigo?: string | null }) =>
  pedirAutenticado<CajaDelLocal>('/api/cajas', token, { method: 'POST', body: JSON.stringify(datos) })

export const editarCaja = (token: string, id: string, datos: { nombre: string; codigo: string }) =>
  pedirAutenticado<CajaDelLocal>(`/api/cajas/${id}`, token, { method: 'PUT', body: JSON.stringify(datos) })

export const definirCajaPrincipal = (token: string, id: string) =>
  pedirAutenticado<CajaDelLocal>(`/api/cajas/${id}/principal`, token, { method: 'POST' })

export const activarCaja = (token: string, id: string) =>
  pedirAutenticado<CajaDelLocal>(`/api/cajas/${id}/activar`, token, { method: 'POST' })

export const desactivarCaja = (token: string, id: string) =>
  pedirAutenticado<CajaDelLocal>(`/api/cajas/${id}/desactivar`, token, { method: 'POST' })

/** Solo una caja que nunca se uso; con historial se desactiva (409 TIENE_HISTORIAL). */
export const eliminarCaja = (token: string, id: string) =>
  pedirAutenticado<void>(`/api/cajas/${id}`, token, { method: 'DELETE' })

/** El turno abierto de quien pregunta; null si no tiene (el servidor responde 204). */
export const turnoActualDeCaja = (token: string) => pedirAutenticado<TurnoDeCaja | null>('/api/turnos/actual', token)

export type MedioDePago = 'efectivo' | 'yape' | 'plin' | 'tarjeta'

/** En efectivo, `monto` es lo RECIBIDO: el servidor calcula el vuelto. Montos en centimos. */
export interface PagoACobrar {
  medio: MedioDePago
  monto: number
  referencia?: string | null
}

export interface CargoACuenta {
  pensionistaId: string
  nombre: string
  importe: number
  /** Despues del cargo su deuda paso del tope: solo avisa, nunca bloquea. */
  superaTope: boolean
}

export interface CobroRegistrado {
  id: string
  pedidoId: string
  turnoId: string
  cobradoEn: string
  total: number
  totalMedios: number
  totalCuentas: number
  vuelto: number
  pagos: { medio: MedioDePago; importe: number; montoRecibido: number | null; referencia: string | null }[]
  cargos: CargoACuenta[]
  /** Ya no queda nada por cobrar: el pedido se cerro y la mesa quedo libre. */
  pedidoCerrado: boolean
}

/** Un descuento para UNA linea: 'monto' en centimos o 'porcentaje' de 1 a 100. Exige el permiso caja.descontar y un motivo de tipo descuento. */
export interface DescuentoACobrar {
  lineaId: string
  tipo: 'monto' | 'porcentaje'
  valor: number
  motivoId: string
  observacion?: string | null
}

export const cobrarPedido = (
  token: string,
  pedidoId: string,
  lineaIds: string[],
  pagos: PagoACobrar[],
  descuentos: DescuentoACobrar[] = [],
) =>
  pedirAutenticado<CobroRegistrado>(`/api/pedidos/${pedidoId}/cobros`, token, {
    method: 'POST',
    body: JSON.stringify({ lineaIds, pagos, descuentos }),
  })

// ---- Cierre de turno y reapertura (sprint 7, entregas 4 y 5) ----

/** Lo que ve el cajero ANTES de contar. Centimos. efectivoEsperado = fondo + efectivo cobrado (sin vuelto ni cobros reabiertos). */
export interface ResumenDeTurno {
  turno: TurnoDeCaja
  fondoInicial: number
  efectivoCobrado: number
  efectivoEsperado: number
  totalYape: number
  totalPlin: number
  totalTarjeta: number
  cobros: number
}

export interface CierreDeCaja {
  id: string
  turnoId: string
  efectivoEsperado: number
  efectivoContado: number
  /** contado - esperado: negativa si falta dinero. */
  diferencia: number
  totalYape: number
  totalPlin: number
  totalTarjeta: number
  observacion: string | null
  cerradoEn: string
  /** Hay cualquier diferencia (desde S/ 0,01, a favor o en contra): avisa, nunca bloquea el cierre. */
  alerta: boolean
}

export const resumenDelTurno = (token: string) => pedirAutenticado<ResumenDeTurno>('/api/turnos/actual/resumen', token)

export const cerrarTurnoDeCaja = (token: string, efectivoContado: number, observacion?: string | null) =>
  pedirAutenticado<CierreDeCaja>('/api/turnos/actual/cierre', token, {
    method: 'POST',
    body: JSON.stringify({ efectivoContado, observacion: observacion ?? null }),
  })

export const cierreDelTurno = (token: string, turnoId: string) =>
  pedirAutenticado<CierreDeCaja>(`/api/turnos/${turnoId}/cierre`, token)

export interface CobroDelPedido {
  id: string
  cobradoEn: string
  total: number
  totalMedios: number
  totalCuentas: number
  /** El cobro se reabrio: sus lineas volvieron a "comandado". */
  revertido: boolean
  lineaIds: string[]
}

export const cobrosDelPedido = (token: string, pedidoId: string) =>
  pedirAutenticado<CobroDelPedido[]>(`/api/pedidos/${pedidoId}/cobros`, token)

/** Solo mientras el turno de origen siga abierto; exige un motivo de tipo reapertura y el permiso caja.reabrir. */
export const reabrirCobro = (token: string, cobroId: string, motivoId: string, observacion?: string | null) =>
  pedirAutenticado<CobroDelPedido>(`/api/cobros/${cobroId}/reapertura`, token, {
    method: 'POST',
    body: JSON.stringify({ motivoId, observacion: observacion ?? null }),
  })

export interface CobroDelTurno {
  id: string
  pedidoId: string
  pedidoNumero: string
  cobradoEn: string
  total: number
  totalMedios: number
  totalCuentas: number
  vuelto: number
  revertido: boolean
}

/** Los cobros del turno abierto de quien pregunta, el mas reciente primero (para verlos y reabrir uno). */
export const cobrosDelTurno = (token: string) => pedirAutenticado<CobroDelTurno[]>('/api/turnos/actual/cobros', token)

/* =========================================================================
 * US-021: Abonos de pensionistas en caja
 * ========================================================================= */

export interface SolicitudAbonoEnCaja {
  pensionistaId: string
  medio: string // 'efectivo' | 'yape' | 'plin' | 'transferencia'
  monto: number // centimos
  numeroOperacion?: string | null
  cuentaDestino?: string | null
  observacion?: string | null
}

export interface ConstanciaAbono {
  localNombre: string
  cajeroNombre: string
  emitidoEn: string
  leyenda: string
}

export interface AbonoRegistrado {
  abono: {
    id: string
    pensionistaId: string
    localId: string
    medio: string
    monto: number
    fechaAbono: string
    numeroOperacion?: string | null
    cuentaDestino?: string | null
    comprobanteUrl?: string | null
    origen: string
    estado: 'pendiente' | 'aprobado' | 'rechazado'
    registradoPor: string
    aprobadoPor?: string | null
    aprobadoEn?: string | null
    motivoRechazo?: string | null
    turnoCajaId?: string | null
    observacion?: string | null
    creadoEn: string
  }
  pensionistaNombre: string
  pensionistaDni: string
  saldoAnterior: number
  nuevoSaldo: number
  saldoAFavor: number
  alertaDuplicado: boolean
  constancia: ConstanciaAbono
}

export interface AbonoDelTurno {
  id: string
  pensionistaId: string
  pensionistaNombre: string
  pensionistaDni: string
  monto: number
  medio: string
  estado: string
  numeroOperacion?: string | null
  cuentaDestino?: string | null
  creadoEn: string
  observacion?: string | null
  turnoCajaId?: string | null
}

export const registrarAbonoEnCaja = (token: string, solicitud: SolicitudAbonoEnCaja) =>
  pedirAutenticado<AbonoRegistrado>('/api/turnos/actual/abonos', token, {
    method: 'POST',
    body: JSON.stringify(solicitud),
  })

export const abonosDelTurno = (token: string) =>
  pedirAutenticado<AbonoDelTurno[]>('/api/turnos/actual/abonos', token)

/* =========================================================================
 * US-019: Kiosco de autoservicio de pensionistas
 * ========================================================================= */

export interface SolicitudPreTicket {
  localCodigo: string
  dni?: string | null
  pin: string
}

export interface ContextoKiosco {
  locales: { codigo: string; nombre: string }[]
  localCodigo: string | null
  localNombre: string | null
  estado: 'sin_local' | 'en_atencion' | 'fuera_de_horario'
  servicioActual: { servicio: string; inicio: string; fin: string } | null
  siguienteServicio: { servicio: string; inicio: string; fin: string } | null
  horaServidor: string
  longitudPin: number
}

export const listarLocalesPublicosKiosco = () =>
  pedir<{ codigo: string; nombre: string }[]>('/api/locales/publico')

export const consultarContextoKiosco = (localCodigo: string) =>
  pedir<ContextoKiosco>(`/api/kiosco/contexto?localCodigo=${encodeURIComponent(localCodigo)}`)

export interface PreTicketEmitido {
  id: string
  codigo: string
  pensionistaId: string
  pensionistaNombre: string
  dni: string
  localId: string
  localNombre: string
  servicio: string
  fecha: string
  emitidoEn: string
  venceEn: string
  reutilizado: boolean
  mensajeLegal: string
}

/** US-019: Emite el pre-ticket de autoservicio para el pensionista (sin token de sesion). */
export const emitirPreTicketKiosco = (solicitud: SolicitudPreTicket) =>
  pedir<PreTicketEmitido>('/api/kiosco/pre-ticket', {
    method: 'POST',
    body: JSON.stringify(solicitud),
  })

/* =========================================================================
 * US-024: Aprobación y rechazo de abonos digitales de pensionistas
 * ========================================================================= */

export interface FilaAbono {
  id: string
  pensionistaId: string
  pensionistaNombre: string
  pensionistaDni: string
  localId: string
  localNombre: string
  cajaSesionId: string | null
  monto: number
  medio: string
  numeroOperacion: string | null
  entidadBancaria: string | null
  comprobanteUrl: string | null
  estado: 'pendiente' | 'aprobado' | 'rechazado'
  creadoEn: string
  revisadoEn: string | null
  revisadoPor: string | null
  motivoRechazoId: string | null
  motivoRechazoDescripcion: string | null
  observacionRechazo: string | null
}

export interface AbonoAprobadoRespuesta {
  abono: {
    id: string
    pensionistaId: string
    monto: number
    medio: string
    estado: string
    numeroOperacion: string | null
    entidadBancaria: string | null
    creadoEn: string
    revisadoEn: string | null
    revisadoPor: string | null
  }
  pensionistaNombre: string
  pensionistaDni: string
  nuevoSaldo: number
  saldoAFavor: number
}

export interface SolicitudRechazoAbono {
  motivoId: string
  observacion?: string | null
}

export const listarAbonos = (
  token: string,
  filtros?: { estado?: string; busqueda?: string },
) => {
  const params = new URLSearchParams()
  if (filtros?.estado && filtros.estado !== 'todos') params.set('estado', filtros.estado)
  if (filtros?.busqueda) params.set('busqueda', filtros.busqueda)
  const q = params.toString() ? `?${params.toString()}` : ''
  return pedirAutenticado<FilaAbono[]>(`/api/pensionistas/abonos${q}`, token)
}

export const listarAbonosPendientes = (token: string) =>
  pedirAutenticado<FilaAbono[]>('/api/pensionistas/abonos/pendientes', token)

export const obtenerConteoAbonosPendientes = (token: string) =>
  pedirAutenticado<{ pendientes: number }>('/api/pensionistas/abonos/conteo-pendientes', token)

export const aprobarAbonoDigital = (token: string, abonoId: string) =>
  pedirAutenticado<AbonoAprobadoRespuesta>(`/api/pensionistas/abonos/${abonoId}/aprobar`, token, {
    method: 'POST',
  })

export const rechazarAbonoDigital = (
  token: string,
  abonoId: string,
  datos: SolicitudRechazoAbono,
) =>
  pedirAutenticado<{ id: string; estado: string }>(`/api/pensionistas/abonos/${abonoId}/rechazar`, token, {
    method: 'POST',
    body: JSON.stringify(datos),
  })

// ---- Inventario y Existencias (US-043, EP-08) -----------------------

export interface FilaExistencia {
  localId: string
  productoId: string
  codigo: string
  nombre: string
  categoriaId: string
  categoriaNombre: string
  unidadId: string
  unidadCodigo: string
  unidadNombre: string
  controlaStock: boolean
  stockActual: number
  stockMinimo: number | null
  estadoAlerta: 'normal' | 'bajo_minimo' | 'negativo' | 'sin_stock' | string
  recalculadoEn: string | null
}

export interface ResumenAlertasInventario {
  totalProductos: number
  totalBajoMinimo: number
  totalNegativos: number
  totalSinStock: number
}

export interface SolicitudStockMinimo {
  stockMinimo: number | null
}

export const listarExistencias = (token: string, localId: string, filtro?: string) => {
  const q = filtro && filtro !== 'todos' ? `?filtro=${encodeURIComponent(filtro)}` : ''
  return pedirAutenticado<FilaExistencia[]>(`/api/locales/${localId}/existencias${q}`, token)
}

export const obtenerResumenAlertasInventario = (token: string, localId: string) =>
  pedirAutenticado<ResumenAlertasInventario>(`/api/locales/${localId}/existencias/resumen-alertas`, token)

export const configurarStockMinimo = (
  token: string,
  localId: string,
  productoId: string,
  stockMinimo: number | null,
) =>
  pedirAutenticado<FilaExistencia>(`/api/locales/${localId}/existencias/${productoId}/stock-minimo`, token, {
    method: 'PUT',
    body: JSON.stringify({ stockMinimo }),
  })

// ---- Kardex de Inventario (US-044, EP-08) -----------------------

export interface FilaKardex {
  id: string
  localId: string
  productoId: string
  productoCodigo: string
  productoNombre: string
  categoriaNombre: string | null
  origen: 'compra' | 'produccion' | 'venta' | 'ajuste' | 'transferencia' | 'conteo' | 'consumo_interno' | 'anulacion_post_comanda' | string
  signo: 'entrada' | 'salida' | string
  cantidad: number
  unidadMedidaId: string
  unidadMedidaNombre: string
  unidadMedidaSimbolo: string
  cantidadBase: number
  costoUnitario: number | null
  costoTotal: number | null
  stockAnterior: number
  stockNuevo: number
  diferencia: number
  referenciaTipo: string | null
  referenciaId: string | null
  motivoDescripcion: string | null
  observacion: string | null
  ocurridoEn: string
  registradoEn: string
  registradoPorId: string | null
  registradoPorNombre: string
}

export interface PaginaKardex {
  elementos: FilaKardex[]
  totalElementos: number
  totalPaginas: number
  pagina: number
  tamano: number
}

export interface FiltroKardexConsulta {
  productoId?: string
  origen?: string
  desde?: string
  hasta?: string
  orden?: 'asc' | 'desc'
  pagina?: number
  tamano?: number
}

export const consultarKardex = (
  token: string,
  localId: string,
  filtros?: FiltroKardexConsulta,
) => {
  const params = new URLSearchParams()
  if (filtros?.productoId) params.append('productoId', filtros.productoId)
  if (filtros?.origen && filtros.origen !== 'todos') params.append('origen', filtros.origen)
  if (filtros?.desde) params.append('desde', filtros.desde)
  if (filtros?.hasta) params.append('hasta', filtros.hasta)
  if (filtros?.orden) params.append('orden', filtros.orden)
  if (filtros?.pagina !== undefined) params.append('pagina', String(filtros.pagina))
  if (filtros?.tamano !== undefined) params.append('tamano', String(filtros.tamano))

  const queryString = params.toString() ? `?${params.toString()}` : ''
  return pedirAutenticado<PaginaKardex>(`/api/locales/${localId}/inventario/kardex${queryString}`, token)
}

// --- US-050: COMPRAS E INGRESO DE INVENTARIO ---

export interface LineaDetalleCompra {
  id: string
  productoId: string
  productoCodigo: string
  productoNombre: string
  unidadBaseSimbolo: string
  presentacionId?: string | null
  presentacionNombre?: string | null
  cantidad: number
  cantidadBase: number
  precioUnitario: number // centimos
  subtotal: number // centimos
}

export interface Compra {
  id: string
  localId: string
  proveedorId: string
  proveedorRazonSocial: string
  proveedorRuc?: string | null
  tipoDocumento: 'factura' | 'boleta' | 'ticket' | 'sin_documento'
  serie?: string | null
  numero?: string | null
  fechaDocumento: string
  total: number // centimos
  rutaAdjunto?: string | null
  estado: 'borrador' | 'confirmada' | 'anulada'
  confirmadaEn?: string | null
  observacion?: string | null
  creadoEn: string
  detalles: LineaDetalleCompra[]
}

export interface ItemRegistroCompra {
  productoId: string
  presentacionId?: string | null
  cantidad: number
  precioUnitario: number // centimos
}

export interface SolicitudRegistroCompra {
  proveedorId: string
  tipoDocumento: 'factura' | 'boleta' | 'ticket' | 'sin_documento'
  serie?: string | null
  numero?: string | null
  fechaDocumento: string
  observacion?: string | null
  confirmarInmediatamente: boolean
  detalles: ItemRegistroCompra[]
}

export interface ProveedorCatalogo {
  id: string
  ruc?: string | null
  razonSocial: string
  nombreComercial?: string | null
  telefono?: string | null
  correo?: string | null
  direccion?: string | null
  contacto?: string | null
  activo: boolean
}

export interface PresentacionCatalogo {
  id: string
  nombre: string
  unidadId: string
  factor: number
  esPredeterminada: boolean
  activo: boolean
}

export const listarCompras = (token: string, localId: string, estado?: string) => {
  const q = estado ? `?estado=${encodeURIComponent(estado)}` : ''
  return pedirAutenticado<Compra[]>(`/api/locales/${localId}/compras${q}`, token)
}

export const obtenerCompra = (token: string, localId: string, compraId: string) =>
  pedirAutenticado<Compra>(`/api/locales/${localId}/compras/${compraId}`, token)

export const registrarCompra = (token: string, localId: string, datos: SolicitudRegistroCompra) =>
  pedirAutenticado<Compra>(`/api/locales/${localId}/compras`, token, {
    method: 'POST',
    body: JSON.stringify(datos),
  })

export const confirmarCompra = (token: string, localId: string, compraId: string) =>
  pedirAutenticado<Compra>(`/api/locales/${localId}/compras/${compraId}/confirmar`, token, {
    method: 'POST',
  })

export const subirComprobanteCompra = async (
  token: string,
  localId: string,
  compraId: string,
  archivo: File,
): Promise<{ rutaAdjunto: string }> => {
  const form = new FormData()
  form.append('archivo', archivo)
  const res = await fetch(`${BASE}/api/locales/${localId}/compras/${compraId}/adjunto`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
    },
    body: form,
  })
  if (!res.ok) {
    const err = await res.json().catch(() => undefined)
    throw new ErrorApi(res.status, 'Error al subir comprobante', err)
  }
  return res.json()
}

export const listarPresentacionesProducto = (token: string, productoId: string) =>
  pedirAutenticado<PresentacionCatalogo[]>(`/api/productos/${productoId}/presentaciones`, token)

// ==========================================
// Producción de Subrecetas y Platos (US-048)
// ==========================================

export interface DetalleProduccion {
  id: string
  localId: string
  productoId: string
  productoCodigo: string
  productoNombre: string
  unidadMedidaSimbolo: string
  esSubreceta: boolean
  recetaId: string
  fecha: string
  servicio?: string | null
  porcionesPlanificadas?: number | null
  porcionesPreparadas: number
  porcionesVendidas: number
  rendimientoObtenido: number
  costoUnitarioEstimado?: number | null
  costoTotalLote?: number | null
  estado: string
  registradoPorNombre?: string | null
  sobrante?: number | null
  destinoSobrante?: 'conservar' | 'personal' | 'perdida' | 'reutilizacion' | 'otro' | null
  motivoId?: string | null
  motivoDescripcion?: string | null
  creadoEn: string
}

export interface RegistroProduccionComando {
  productoId: string
  recetaId?: string | null
  fecha: string
  servicio?: string | null
  porcionesPlanificadas?: number | null
  porcionesPreparadas: number
  rendimientoObtenido: number
  usuarioId?: string
}

export interface LiquidarSobranteComando {
  sobrante: number
  destino: 'conservar' | 'personal' | 'perdida' | 'reutilizacion' | 'otro'
  motivoId?: string | null
  observacion?: string | null
}

export interface IngredientePrecarga {
  productoId: string
  productoNombre: string
  cantidadBase: number
  unidadMedidaSimbolo: string
  stockActual: number
  costoUnitario: number
}

export interface ProductoConReceta {
  id: string
  codigo: string
  nombre: string
  unidadMedidaSimbolo: string
  esSubreceta: boolean
  recetaId: string
  rendimientoBase: number
  porcionesBase: number
  ingredientes: IngredientePrecarga[]
}

export interface PrecargaProduccion {
  productos: ProductoConReceta[]
  porcionesPlanificadasPorProductoId: Record<string, number>
}

export const listarProduccion = (token: string, localId: string, fecha?: string) => {
  const q = fecha ? `?fecha=${encodeURIComponent(fecha)}` : ''
  return pedirAutenticado<DetalleProduccion[]>(`/api/locales/${localId}/produccion${q}`, token)
}

export const obtenerPrecargaProduccion = (token: string, localId: string, fecha?: string, servicio?: string) => {
  const params = new URLSearchParams()
  if (fecha) params.append('fecha', fecha)
  if (servicio) params.append('servicio', servicio)
  const q = params.toString() ? `?${params.toString()}` : ''
  return pedirAutenticado<PrecargaProduccion>(`/api/locales/${localId}/produccion/precarga${q}`, token)
}

export const registrarProduccion = (token: string, localId: string, datos: RegistroProduccionComando) =>
  pedirAutenticado<DetalleProduccion>(`/api/locales/${localId}/produccion`, token, {
    method: 'POST',
    body: JSON.stringify(datos),
  })

export const registrarSobranteProduccion = (
  token: string,
  localId: string,
  produccionId: string,
  datos: LiquidarSobranteComando,
) =>
  pedirAutenticado<DetalleProduccion>(`/api/locales/${localId}/produccion/${produccionId}/sobrante`, token, {
    method: 'POST',
    body: JSON.stringify(datos),
  })

// ==========================================
// Consumo Interno de Personal (US-051)
// ==========================================

export interface FilaConsumoInterno {
  id: string
  localId: string
  usuarioId: string
  usuarioNombre: string
  fecha: string
  servicio: string
  turno?: string | null
  productoId: string
  productoCodigo: string
  productoNombre: string
  unidadMedidaSimbolo: string
  cantidad: number
  costoUnitario: number
  costoTotal: number
  requirioAutorizacion: boolean
  autorizadoPorId?: string | null
  autorizadoPorNombre?: string | null
  observacion?: string | null
  movimientoId?: string | null
  creadoEn: string
}

export interface ResumenConsumoInterno {
  totalComidas: number
  costoTotal: number
  totalConAutorizacion: number
}

export interface RegistroConsumoInternoComando {
  usuarioId: string
  fecha?: string
  servicio: string
  turno?: string
  productoId: string
  cantidad: number
  observacion?: string
  supervisorPin?: string
}

export const listarConsumosInternos = (
  token: string,
  localId: string,
  fecha?: string,
) => {
  const q = fecha ? `?fecha=${encodeURIComponent(fecha)}` : ''
  return pedirAutenticado<FilaConsumoInterno[]>(`/api/locales/${localId}/consumo-interno${q}`, token)
}

export const obtenerResumenConsumoInterno = (
  token: string,
  localId: string,
  fecha?: string,
) => {
  const q = fecha ? `?fecha=${encodeURIComponent(fecha)}` : ''
  return pedirAutenticado<ResumenConsumoInterno>(`/api/locales/${localId}/consumo-interno/resumen${q}`, token)
}

export const registrarConsumoInterno = (
  token: string,
  localId: string,
  datos: RegistroConsumoInternoComando,
) =>
  pedirAutenticado<FilaConsumoInterno>(`/api/locales/${localId}/consumo-interno`, token, {
    method: 'POST',
    body: JSON.stringify(datos),
  })


// ---- Ajustes de Inventario (US-045, EP-08) -----------------------

export interface DetalleAjuste {
  id: string
  localId: string
  productoId: string
  productoCodigo: string
  productoNombre: string
  unidadMedida: string
  signo: 'entrada' | 'salida' | string
  cantidad: number
  stockAnterior: number
  stockNuevo: number
  costoUnitario: number | null
  costoTotal: number | null
  motivoId: string
  motivoDescripcion: string
  observacion: string | null
  ocurridoEn: string
  registradoPorId: string | null
  registradoPorNombre: string
}

export interface SolicitudRegistroAjuste {
  productoId: string
  motivoId: string
  tipoAjuste: 'INCREMENTO' | 'DISMINUCION' | 'REEMPLAZO_STOCK' | 'variacion' | 'stock_final' | string
  cantidad?: number | null
  stockFinal?: number | null
  observacion?: string | null
  fecha?: string | null
}

export const listarAjustesInventario = (
  token: string,
  localId: string,
  filtros?: { productoId?: string; desde?: string; hasta?: string },
) => {
  const params = new URLSearchParams()
  if (filtros?.productoId) params.append('productoId', filtros.productoId)
  if (filtros?.desde) params.append('desde', filtros.desde)
  if (filtros?.hasta) params.append('hasta', filtros.hasta)
  const q = params.toString() ? `?${params.toString()}` : ''
  return pedirAutenticado<DetalleAjuste[]>(`/api/locales/${localId}/inventario/ajustes${q}`, token)
}

export const registrarAjusteInventario = (
  token: string,
  localId: string,
  datos: SolicitudRegistroAjuste,
) =>
  pedirAutenticado<DetalleAjuste>(`/api/locales/${localId}/inventario/ajustes`, token, {
    method: 'POST',
    body: JSON.stringify(datos),
  })
