/** Prefijos de pais para el telefono del local. Peru primero: es el mercado del negocio hoy. */
export interface PrefijoPais {
  codigo: string
  prefijo: string
  nombre: string
}

export const PREFIJOS_PAIS: PrefijoPais[] = [
  { codigo: 'PE', prefijo: '+51', nombre: 'Perú' },
  { codigo: 'AR', prefijo: '+54', nombre: 'Argentina' },
  { codigo: 'BO', prefijo: '+591', nombre: 'Bolivia' },
  { codigo: 'BR', prefijo: '+55', nombre: 'Brasil' },
  { codigo: 'CL', prefijo: '+56', nombre: 'Chile' },
  { codigo: 'CO', prefijo: '+57', nombre: 'Colombia' },
  { codigo: 'EC', prefijo: '+593', nombre: 'Ecuador' },
  { codigo: 'ES', prefijo: '+34', nombre: 'España' },
  { codigo: 'MX', prefijo: '+52', nombre: 'México' },
  { codigo: 'PY', prefijo: '+595', nombre: 'Paraguay' },
  { codigo: 'US', prefijo: '+1', nombre: 'Estados Unidos' },
  { codigo: 'UY', prefijo: '+598', nombre: 'Uruguay' },
  { codigo: 'VE', prefijo: '+58', nombre: 'Venezuela' },
]

export const PREFIJO_POR_DEFECTO = PREFIJOS_PAIS[0].prefijo

/** Separa "+51 987654321" en prefijo + numero. Si no reconoce el prefijo, asume Peru. */
export function separarTelefono(telefono: string | null | undefined): { prefijo: string; numero: string } {
  const valor = (telefono ?? '').trim()
  const conocido = PREFIJOS_PAIS.find((p) => valor.startsWith(p.prefijo + ' ') || valor === p.prefijo)
  if (conocido) {
    return { prefijo: conocido.prefijo, numero: valor.slice(conocido.prefijo.length).trim() }
  }
  return { prefijo: PREFIJO_POR_DEFECTO, numero: valor }
}

export function unirTelefono(prefijo: string, numero: string): string {
  const limpio = numero.trim()
  return limpio ? `${prefijo} ${limpio}` : ''
}
