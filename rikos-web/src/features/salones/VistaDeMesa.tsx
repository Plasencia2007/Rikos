import type { CSSProperties, HTMLAttributes } from 'react'
import type { FormaDeMesa } from '../../api/cliente'
import { medidasDeForma } from './salonesVisual'

interface Props extends Omit<HTMLAttributes<HTMLDivElement>, 'style' | 'children'> {
  forma: FormaDeMesa
  etiqueta: string
  sillas: number
  /** Medidas en px; sin ellas, las de fábrica de la forma. */
  ancho?: number
  alto?: number
  seleccionada?: boolean
  inactiva?: boolean
  style?: CSSProperties
}

/** Distribuye las sillas alrededor de la mesa: 4 por lado (cuadrada, redonda) o arriba y abajo (rectangular). */
export function posicionesDeSillas(forma: FormaDeMesa, w: number, h: number, n: number) {
  const sillas: { x: number; y: number; horizontal: boolean }[] = []
  const tope = Math.max(0, Math.min(n, 12))
  if (forma === 'rectangular') {
    const arriba = Math.ceil(tope / 2)
    const abajo = tope - arriba
    for (let k = 0; k < arriba; k++) sillas.push({ x: (w / (arriba + 1)) * (k + 1) - 12, y: -14, horizontal: true })
    for (let k = 0; k < abajo; k++) sillas.push({ x: (w / (abajo + 1)) * (k + 1) - 12, y: h + 5, horizontal: true })
    return sillas
  }
  const lados = [
    { x: w / 2 - 12, y: -14, horizontal: true },
    { x: w + 5, y: h / 2 - 12, horizontal: false },
    { x: w / 2 - 12, y: h + 5, horizontal: true },
    { x: -14, y: h / 2 - 12, horizontal: false },
  ]
  return lados.slice(0, Math.min(tope, 4)).concat(
    // Más de 4 sillas: se reparten las restantes sobre los lados largos para que se vea la cantidad.
    Array.from({ length: Math.max(0, tope - 4) }, (_, k) => ({ x: (w / (Math.ceil((tope - 4) / 2) + 1)) * ((k >> 1) + 1) - 12, y: k % 2 ? h + 5 : -14, horizontal: true })),
  )
}

/** Una mesa con sus sillas, tal como se ve en el plano. */
export function VistaDeMesa({ forma, etiqueta, sillas, ancho, alto, seleccionada, inactiva, style, ...resto }: Props) {
  const [wf, hf] = medidasDeForma(forma)
  const w = ancho ?? wf
  const h = alto ?? hf
  const colorSilla = seleccionada ? '#2B7A68' : '#B9C3C0'
  return (
    <div className={`sal-mesa ${seleccionada ? 'seleccionada' : ''} ${inactiva ? 'inactiva' : ''}`} style={{ width: w, height: h, ...style }} {...resto}>
      {posicionesDeSillas(forma, w, h, sillas).map((s, i) => (
        <span
          key={i}
          className="sal-silla"
          style={{ left: s.x, top: s.y, width: s.horizontal ? 24 : 9, height: s.horizontal ? 9 : 24, background: colorSilla }}
        />
      ))}
      <div className={`sal-mesa-cuerpo ${forma}`}>
        <b>{etiqueta}</b>
        <span>{sillas || ''}</span>
      </div>
    </div>
  )
}
