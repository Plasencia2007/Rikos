import { createContext, useContext, useState, type ReactNode } from 'react'
import { iniciarSesionConContrasena, iniciarSesionConPin, type SesionIniciada } from '../../api/cliente'

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
  cambiarLocalActivo: (localId: string) => void
  tieneAlgunRol: (...roles: string[]) => boolean
}

const Contexto = createContext<ContextoSesion | undefined>(undefined)

/**
 * Concentra el usuario activo, sus roles y el local activo (comun/sesion,
 * per ESTRUCTURA.md 8). US-002 / US-003 son los primeros en escribir aca.
 */
export function SesionProveedor({ children }: { children: ReactNode }) {
  const [sesion, setSesion] = useState<SesionIniciada | null>(() => leerAlmacenada())
  const [cargando, setCargando] = useState(false)

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

  function cambiarLocalActivo(localId: string) {
    setSesion((actual) => {
      if (!actual) return actual
      const siguiente = { ...actual, localActivoId: localId }
      guardar(siguiente)
      return siguiente
    })
  }

  function tieneAlgunRol(...roles: string[]) {
    return sesion != null && roles.some((r) => sesion.roles.includes(r))
  }

  return (
    <Contexto.Provider
      value={{ sesion, cargando, entrarConContrasena, entrarConPin, salir, cambiarLocalActivo, tieneAlgunRol }}
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
