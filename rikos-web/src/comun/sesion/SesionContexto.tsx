import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import {
  cambiarLocalActivo as cambiarLocalActivoApi,
  iniciarSesionConContrasena,
  iniciarSesionConPin,
  permisosDeUsuario,
  type SesionIniciada,
} from '../../api/cliente'

const CLAVE_ALMACENAMIENTO = 'rikos.sesion'

function leerAlmacenada(): SesionIniciada | null {
  try {
    const crudo = localStorage.getItem(CLAVE_ALMACENAMIENTO)
    return crudo ? (JSON.parse(crudo) as SesionIniciada) : null
  } catch {
    return null
  }
}

function guardar(sesion: SesionIniciada | null) {
  try {
    if (sesion) localStorage.setItem(CLAVE_ALMACENAMIENTO, JSON.stringify(sesion))
    else localStorage.removeItem(CLAVE_ALMACENAMIENTO)
  } catch {
    // Almacenamiento no disponible (privado, cuota, etc.): la sesion sigue viva en memoria.
  }
}

interface ContextoSesion {
  sesion: SesionIniciada | null
  cargando: boolean
  entrarConContrasena: (nombreUsuario: string, contrasena: string) => Promise<void>
  entrarConPin: (localCodigo: string, pin: string) => Promise<void>
  salir: () => void
  cambiarLocalActivo: (localId: string) => Promise<void>
  /**
   * Vuelve a pedir la sesion con el mismo local activo: trae la lista de
   * locales al dia (un local recien creado, activado o desactivado) y el
   * selector del encabezado deja de mostrar datos viejos.
   */
  refrescarLocales: () => Promise<void>
  tieneAlgunRol: (...roles: string[]) => boolean
  tienePermiso: (codigo: string) => boolean
}

const Contexto = createContext<ContextoSesion | undefined>(undefined)

/**
 * Concentra el usuario activo, sus roles y el local activo (comun/sesion,
 * per ESTRUCTURA.md 8). US-002 / US-003 son los primeros en escribir aca.
 * Los permisos efectivos (US-006, para ocultar/mostrar segun
 * "auditoria.ver" y similares) se cargan una vez por sesion, no en cada
 * pantalla que los necesita.
 */
export function SesionProveedor({ children }: { children: ReactNode }) {
  const [sesion, setSesion] = useState<SesionIniciada | null>(() => leerAlmacenada())
  const [cargando, setCargando] = useState(false)
  const [permisos, setPermisos] = useState<Set<string>>(new Set())

  useEffect(() => {
    if (!sesion) {
      setPermisos(new Set())
      return
    }
    permisosDeUsuario(sesion.token, sesion.usuarioId)
      .then((lista) => setPermisos(new Set(lista.filter((p) => p.efectivo).map((p) => p.codigo))))
      .catch(() => setPermisos(new Set()))
    // Solo depende de quien es el usuario, no del local activo (que cambia
    // con cambiarLocalActivo sin afectar los permisos).
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sesion?.token, sesion?.usuarioId])

  async function entrarConContrasena(nombreUsuario: string, contrasena: string) {
    setCargando(true)
    try {
      const resultado = await iniciarSesionConContrasena(nombreUsuario, contrasena)
      setSesion(resultado)
      guardar(resultado)
    } finally {
      setCargando(false)
    }
  }

  async function entrarConPin(localCodigo: string, pin: string) {
    setCargando(true)
    try {
      const resultado = await iniciarSesionConPin(localCodigo, pin)
      setSesion(resultado)
      guardar(resultado)
    } finally {
      setCargando(false)
    }
  }

  function salir() {
    setSesion(null)
    guardar(null)
  }

  async function cambiarLocalActivo(localId: string) {
    if (!sesion) return
    const siguiente = await cambiarLocalActivoApi(sesion.token, localId)
    setSesion(siguiente)
    guardar(siguiente)
  }

  async function refrescarLocales() {
    if (!sesion?.localActivoId) return
    try {
      const siguiente = await cambiarLocalActivoApi(sesion.token, sesion.localActivoId)
      setSesion(siguiente)
      guardar(siguiente)
    } catch {
      // Si el local activo ya no es valido (p. ej. lo desactivaron), se deja
      // la sesion como esta: refrescar es una comodidad, no puede sacar al
      // usuario de la pantalla.
    }
  }

  function tieneAlgunRol(...roles: string[]) {
    return sesion != null && roles.some((r) => sesion.roles.includes(r))
  }

  function tienePermiso(codigo: string) {
    return permisos.has(codigo)
  }

  return (
    <Contexto.Provider
      value={{
        sesion,
        cargando,
        entrarConContrasena,
        entrarConPin,
        salir,
        cambiarLocalActivo,
        refrescarLocales,
        tieneAlgunRol,
        tienePermiso,
      }}
    >
      {children}
    </Contexto.Provider>
  )
}

export function useSesion() {
  const contexto = useContext(Contexto)
  if (!contexto) throw new Error('useSesion() debe usarse dentro de <SesionProveedor>')
  return contexto
}
