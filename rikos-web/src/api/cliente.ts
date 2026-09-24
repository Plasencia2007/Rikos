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
    public readonly cuerpo?: { error?: string; segundosRestantes?: number },
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
    deliveryHabilitado: boolean
    recojoHabilitado: boolean
    tiempoMinEntregaMin?: number | null
    tiempoMaxEntregaMin?: number | null
  },
) => pedirAutenticado<Local>(`/api/locales/${id}`, token, { method: 'PUT', body: JSON.stringify(datos) })

export const cambiarEstadoLocal = (token: string, id: string, activo: boolean) =>
  pedirAutenticado<Local>(`/api/locales/${id}/${activo ? 'activar' : 'desactivar'}`, token, { method: 'POST' })

/** Borrado real (distinto de desactivar): solo funciona si el local nunca tuvo uso. */
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
export type Usuario = Required<Omit<Esquemas['UsuarioRespuesta'], 'roles' | 'localPrincipal' | 'ultimoAcceso'>> & {
  roles: RolResumen[]
  localPrincipal: LocalPrincipalResumen | null
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

// ---- Catalogo (US-008, US-009, US-011) ---------------------------------

export type Categoria = Required<Esquemas['CategoriaRespuesta']>
export type Unidad = Required<Esquemas['UnidadRespuesta']>
export type Producto = Required<Esquemas['ProductoRespuesta']>
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

export const registrarCostoManual = (token: string, productoId: string, costo: number) =>
  pedirAutenticado<Producto>(`/api/productos/${productoId}/costo-manual`, token, {
    method: 'PUT',
    body: JSON.stringify({ costo }),
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
