import { Coffee, Sun, Table2, TreePine, UtensilsCrossed, Wine, type LucideIcon } from 'lucide-react'
import type { ColorDeSalon, FormaDeMesa, IconoDeSalon } from '../../api/cliente'

export const ICONOS_DE_SALON: { valor: IconoDeSalon; etiqueta: string; Icono: LucideIcon }[] = [
  { valor: 'terraza', etiqueta: 'Terraza', Icono: Sun },
  { valor: 'cubiertos', etiqueta: 'Comedor', Icono: UtensilsCrossed },
  { valor: 'copa', etiqueta: 'Copa', Icono: Wine },
  { valor: 'arbol', etiqueta: 'Jardín', Icono: TreePine },
  { valor: 'taza', etiqueta: 'Barra', Icono: Coffee },
  { valor: 'mesa', etiqueta: 'Mesa', Icono: Table2 },
]

/** Fondo suave y color fuerte de cada color de salón. */
export const COLORES_DE_SALON: { valor: ColorDeSalon; etiqueta: string; fondo: string; fuerte: string }[] = [
  { valor: 'verde', etiqueta: 'Verde', fondo: '#E3F1EC', fuerte: '#2B7A68' },
  { valor: 'azul', etiqueta: 'Azul', fondo: '#E6EEFB', fuerte: '#2563EB' },
  { valor: 'violeta', etiqueta: 'Violeta', fondo: '#EFE9FB', fuerte: '#7C3AED' },
  { valor: 'ambar', etiqueta: 'Ámbar', fondo: '#FDF0DC', fuerte: '#D97706' },
  { valor: 'turquesa', etiqueta: 'Turquesa', fondo: '#DDF3F6', fuerte: '#0891B2' },
  { valor: 'gris', etiqueta: 'Gris', fondo: '#EEF1F0', fuerte: '#64748B' },
]

export const FORMAS_DE_MESA: { valor: FormaDeMesa; etiqueta: string }[] = [
  { valor: 'cuadrada', etiqueta: 'Cuadrada' },
  { valor: 'redonda', etiqueta: 'Redonda' },
  { valor: 'rectangular', etiqueta: 'Rectangular' },
]

export const iconoDeSalon = (valor: string): LucideIcon =>
  ICONOS_DE_SALON.find((i) => i.valor === valor)?.Icono ?? UtensilsCrossed

export const colorDeSalon = (valor: string) => COLORES_DE_SALON.find((c) => c.valor === valor) ?? COLORES_DE_SALON[0]

export const etiquetaDeForma = (forma: string) => FORMAS_DE_MESA.find((f) => f.valor === forma)?.etiqueta ?? forma

/** Medidas de fábrica de una forma (px), las mismas que aplica el servidor. */
export const medidasDeForma = (forma: FormaDeMesa): [number, number] => (forma === 'rectangular' ? [150, 78] : [84, 84])

/** Cuadrado de color con el icono del salón. */
export function EtiquetaDeSalon({ icono, color, tamano = 44 }: { icono: string; color: string; tamano?: number }) {
  const Icono = iconoDeSalon(icono)
  const c = colorDeSalon(color)
  return (
    <span
      className="sal-tile"
      style={{ width: tamano, height: tamano, background: c.fondo, color: c.fuerte }}
      aria-hidden="true"
    >
      <Icono size={Math.round(tamano * 0.5)} strokeWidth={1.7} />
    </span>
  )
}

/** Mensaje de error del servidor para las pantallas de salones y mesas. */
export function mensajeDeError(codigo: string | undefined, porDefecto: string): string {
  switch (codigo) {
    case 'NOMBRE_DUPLICADO':
      return 'Ya existe un salón con ese nombre en este local.'
    case 'NOMBRE_INVALIDO':
      return 'Escribe el nombre del salón (hasta 60 caracteres).'
    case 'VALOR_INVALIDO':
      return 'Revisa los datos: alguno no es válido.'
    case 'TIENE_MESAS':
      return 'Este salón tiene mesas: desactívalo en lugar de eliminarlo.'
    case 'NUMERO_DUPLICADO':
      return 'Ya existe una mesa con ese número en este local.'
    case 'NUMERO_INVALIDO':
      return 'El número de mesa es obligatorio (hasta 10 caracteres).'
    case 'CAPACIDAD_INVALIDA':
      return 'La capacidad debe ser un número mayor que cero.'
    case 'FORMA_INVALIDA':
      return 'La forma de la mesa no es válida.'
    case 'MEDIDAS_INVALIDAS':
      return 'El ancho y el alto deben ser mayores que cero.'
    case 'SALON_NO_ENCONTRADO':
    case 'NO_ENCONTRADO':
      return 'Ya no existe. Recarga la página.'
    case 'MESA_MODIFICADA':
    case 'SALON_MODIFICADO':
      return 'Otra persona lo modificó a la vez. Recarga y vuelve a intentarlo.'
    default:
      return porDefecto
  }
}
