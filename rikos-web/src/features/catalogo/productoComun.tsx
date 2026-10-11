import { CircleDollarSign, FlaskConical, Package } from 'lucide-react'
import type { Categoria, Producto, Servicio } from '../../api/cliente'

/**
 * Lo que comparten la lista de productos y el formulario: como se ve cada
 * uso (vendible/insumo/subreceta), los servicios, el margen y el formato de
 * dinero. Un solo lugar para que la pastilla de "Insumo" sea igual en la
 * tabla, en la ficha y en el formulario.
 */

export type Uso = 'vendible' | 'insumo' | 'subreceta'

export const USOS: { clave: Uso; titulo: string; plural: string; descripcion: string }[] = [
  { clave: 'vendible', titulo: 'Vendible', plural: 'Vendibles', descripcion: 'Se cobra en caja. Pide precio de venta.' },
  { clave: 'insumo', titulo: 'Insumo', plural: 'Insumos', descripcion: 'Se compra y se guarda. Pide costo.' },
  { clave: 'subreceta', titulo: 'Subreceta', plural: 'Subrecetas', descripcion: 'Se prepara y se usa dentro de otras recetas.' },
]

export const ETIQUETA_SERVICIO: Record<Servicio, string> = {
  desayuno: 'Desayuno',
  almuerzo: 'Almuerzo',
  cena: 'Cena',
}

export function usosDe(p: Pick<Producto, 'esVendible' | 'esInsumo' | 'esSubreceta'>): Uso[] {
  const usos: Uso[] = []
  if (p.esVendible) usos.push('vendible')
  if (p.esInsumo) usos.push('insumo')
  if (p.esSubreceta) usos.push('subreceta')
  return usos
}

/** precio_venta viaja en centimos (BIGINT); el costo, en soles con decimales. */
export function soles(valor: number | null | undefined): string {
  if (valor == null || Number.isNaN(valor)) return '—'
  return `S/ ${valor.toFixed(2)}`
}

export function precioEnSoles(p: Producto): number | null {
  return p.esVendible && p.precioVenta != null ? p.precioVenta / 100 : null
}

export function costoEnSoles(p: Producto): number | null {
  return p.esInsumo && p.costo != null ? Number(p.costo) : null
}

/** Margen bruto sobre el precio. Solo existe si hay precio y costo a la vez. */
export function margen(precio: number | null, costo: number | null): number | null {
  if (precio == null || costo == null || precio <= 0) return null
  return Math.round(((precio - costo) / precio) * 100)
}

export function tonoMargen(m: number | null): 'bueno' | 'medio' | 'bajo' | 'nada' {
  if (m == null) return 'nada'
  if (m >= 55) return 'bueno'
  if (m < 35) return 'bajo'
  return 'medio'
}

export function iniciales(nombre: string): string {
  const partes = nombre.trim().split(/\s+/).filter(Boolean)
  if (partes.length === 0) return '?'
  if (partes.length === 1) return partes[0].slice(0, 2).toUpperCase()
  return (partes[0][0] + partes[1][0]).toUpperCase()
}

/** "Platos fijos › Sándwiches y panes"; una raiz sin hijas se muestra sola. */
export function rutaCategoria(categoriaId: string, categorias: Categoria[]): string {
  const cat = categorias.find((c) => c.id === categoriaId)
  if (!cat) return ''
  const padre = cat.categoriaPadreId ? categorias.find((c) => c.id === cat.categoriaPadreId) : null
  return padre ? `${padre.nombre} › ${cat.nombre}` : cat.nombre
}

export function PastillaUso({ uso }: { uso: Uso }) {
  const info = USOS.find((u) => u.clave === uso)!
  return <span className={`prod-pastilla prod-pastilla-${uso}`}>{info.titulo}</span>
}

export function Mosaico({ nombre, uso, activo = true, grande = false }: { nombre: string; uso?: Uso; activo?: boolean; grande?: boolean }) {
  const clase = !activo ? 'prod-mosaico-inactivo' : uso ? `prod-mosaico-${uso}` : 'prod-mosaico-vacio'
  return (
    <span className={`prod-mosaico ${clase} ${grande ? 'prod-mosaico-grande' : ''}`} aria-hidden="true">
      {iniciales(nombre)}
    </span>
  )
}

const ICONO_USO = { vendible: CircleDollarSign, insumo: Package, subreceta: FlaskConical }

/** Cuadrito con el icono de un uso (tarjetas del formulario). */
export function IconoUso({ uso }: { uso: Uso }) {
  const Icono = ICONO_USO[uso]
  return (
    <span className={`prod-icono-uso prod-mosaico-${uso}`} aria-hidden="true">
      <Icono width={16} height={16} />
    </span>
  )
}

export function Interruptor({ activo, etiqueta, onCambiar }: { activo: boolean; etiqueta: string; onCambiar: () => void }) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={activo}
      aria-label={etiqueta}
      className={`prod-switch ${activo ? 'prod-switch-on' : ''}`}
      onClick={(e) => {
        e.stopPropagation()
        onCambiar()
      }}
    >
      <span />
    </button>
  )
}

export function formatearFecha(iso: string | null | undefined): string {
  if (!iso) return '—'
  const fecha = new Date(iso.length === 10 ? `${iso}T00:00:00` : iso)
  return fecha.toLocaleDateString('es-PE', { day: 'numeric', month: 'short', year: 'numeric' })
}
