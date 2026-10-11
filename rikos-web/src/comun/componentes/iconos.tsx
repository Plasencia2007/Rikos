import type { SVGProps } from 'react'
import { ArrowLeftRight, Ban, Check, CircleCheck, EllipsisVertical, Eye, Info, Trash2, X } from 'lucide-react'

/**
 * Iconos de linea, minimos, sin color propio (heredan currentColor: el
 * tono sale siempre de donde se usan, nunca de aqui). No hay libreria de
 * iconos instalada todavia -- se agrega cuando haga falta variedad, no
 * antes -- asi que estos pocos se escriben a mano. Unica excepcion:
 * IconoBorrar, que es el tacho de basura de lucide-react (pedido a
 * proposito -- un tacho de verdad, no una version dibujada a mano).
 */

export const IconoBorrar = Trash2
export const IconoCerrar = X
export const IconoCheck = Check
export const IconoOjo = Eye
export const IconoCambiar = ArrowLeftRight
export const IconoMasOpciones = EllipsisVertical
export const IconoInfo = Info
export const IconoBloquear = Ban
export const IconoReactivar = CircleCheck

export function IconoCandado(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <rect x="5" y="11" width="14" height="9" rx="2" />
      <path d="M8 11V7a4 4 0 1 1 8 0v4" />
    </svg>
  )
}

export function IconoUbicacion(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <path d="M12 21s7-7.5 7-12a7 7 0 1 0-14 0c0 4.5 7 12 7 12z" />
      <circle cx="12" cy="9" r="2.4" />
    </svg>
  )
}

export function IconoGorroChef(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <path d="M7.5 20h9" />
      <path d="M8.3 20v-4.3M15.7 20v-4.3" />
      <path d="M8.3 15.7a4.3 4.3 0 0 1-2.1-3.7 4.3 4.3 0 0 1 4.3-4.3c.2 0 .4 0 .6.05a4.6 4.6 0 0 1 8.9 1.6c0 .5-.08 1-.24 1.45A4.3 4.3 0 0 1 18 15.7" />
      <path d="M8.3 15.7c.7.3 1.5.45 2.3.45h2.8c.8 0 1.6-.15 2.3-.45" />
    </svg>
  )
}

export function IconoCubiertos(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <path d="M6.5 2.5v6a1.8 1.8 0 0 0 1.8 1.8v11.2" />
      <path d="M6.5 2.5v4.3M9.6 2.5v4.3" />
      <path d="M16.8 2.5c-1.4 0-2.6 1.6-2.6 4.3 0 2.5 1.1 4.6 2.6 4.9v9.8" />
    </svg>
  )
}

/* ---- Navegacion del panel administrativo (Bloque A / dashboard) ---- */

export function IconoInicio(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <path d="M4 11.5 12 4l8 7.5" />
      <path d="M6 10v9a1 1 0 0 0 1 1h3v-6h4v6h3a1 1 0 0 0 1-1v-9" />
    </svg>
  )
}

export function IconoCajaRegistradora(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <rect x="3.5" y="10" width="17" height="9.5" rx="1.5" />
      <path d="M6.5 10V8a2 2 0 0 1 2-2h7a2 2 0 0 1 2 2v2" />
      <path d="M3.5 14h17M9 14v5.5M15 14v5.5" />
    </svg>
  )
}

export function IconoMoto(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <circle cx="6" cy="17.5" r="2.6" />
      <circle cx="18" cy="17.5" r="2.6" />
      <path d="M6 17.5h5l3-7h4M11 10.5H8.5" />
      <path d="M14 6.5h3l1.5 4" />
    </svg>
  )
}

export function IconoPersonas(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <circle cx="9" cy="8.5" r="3" />
      <path d="M3 20v-1.2A4.8 4.8 0 0 1 7.8 14h2.4A4.8 4.8 0 0 1 15 18.8V20" />
      <path d="M15.5 6.2a3 3 0 0 1 0 5.8" />
      <path d="M16 14.2a4.8 4.8 0 0 1 5 4.6V20" />
    </svg>
  )
}

export function IconoCalendarioSemana(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <rect x="3.5" y="5" width="17" height="15" rx="2" />
      <path d="M3.5 9.5h17M8 3v4M16 3v4" />
      <path d="M7.5 13.5h2M11 13.5h2M14.5 13.5h2M7.5 16.5h2M11 16.5h2" />
    </svg>
  )
}

export function IconoLibro(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <path d="M12 6.5c-1.6-1-4-1.5-6-1.5-1 0-1.5.15-1.5.15V17s.5-.15 1.5-.15c2 0 4.4.5 6 1.65" />
      <path d="M12 6.5c1.6-1 4-1.5 6-1.5 1 0 1.5.15 1.5.15V17s-.5-.15-1.5-.15c-2 0-4.4.5-6 1.65" />
      <path d="M12 6.5v12" />
    </svg>
  )
}

export function IconoCaja(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <path d="M3.5 8.5 12 4l8.5 4.5-8.5 4.5-8.5-4.5z" />
      <path d="M3.5 8.5V16L12 20.5 20.5 16V8.5" />
      <path d="M12 13v7.5" />
    </svg>
  )
}

export function IconoProduccion(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <path d="M5 20V10.5l3.2 2.8L11 8l2.5 4.5L16.2 9 19 12.5V20z" />
      <path d="M5 20h14" />
    </svg>
  )
}

export function IconoCarrito(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <path d="M3.5 4h2l2 12.5h10.5l1.7-8.2H7" />
      <circle cx="9.5" cy="20" r="1.4" />
      <circle cx="17" cy="20" r="1.4" />
    </svg>
  )
}

export function IconoRecibo(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <path d="M6 3.5h12v17l-2.2-1.5-2 1.5-2-1.5-2 1.5-2-1.5-1.8 1.5z" />
      <path d="M8.5 8h7M8.5 11.5h7M8.5 15h4" />
    </svg>
  )
}

export function IconoGrafico(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <path d="M4 20V9M10 20V4M16 20v-7M22 20H2" />
    </svg>
  )
}

export function IconoUsuario(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <circle cx="12" cy="8" r="3.6" />
      <path d="M4.5 20v-1.3A5.7 5.7 0 0 1 10.2 13h3.6a5.7 5.7 0 0 1 5.7 5.7V20" />
    </svg>
  )
}

export function IconoEngranaje(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <circle cx="12" cy="12" r="3" />
      <path d="M12 3.5v2.4M12 18.1v2.4M20.5 12h-2.4M5.9 12H3.5M18 6l-1.7 1.7M7.7 16.3 6 18M18 18l-1.7-1.7M7.7 7.7 6 6" />
    </svg>
  )
}

export function IconoBusqueda(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <circle cx="10.5" cy="10.5" r="6.5" />
      <path d="m20 20-4.3-4.3" />
    </svg>
  )
}

export function IconoCampana(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <path d="M6 10a6 6 0 1 1 12 0c0 4 1.3 5.5 1.3 5.5H4.7S6 14 6 10z" />
      <path d="M10.2 18.5a1.9 1.9 0 0 0 3.6 0" />
    </svg>
  )
}

export function IconoFlechaAbajo(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <path d="m6 9 6 6 6-6" />
    </svg>
  )
}

export function IconoTienda(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <path d="M4 9.5 5 4h14l1 5.5" />
      <path d="M4 9.5a2.2 2.2 0 0 0 4.3.6 2.2 2.2 0 0 0 4.2 0 2.2 2.2 0 0 0 4.2 0 2.2 2.2 0 0 0 4.3-.6" />
      <path d="M5.5 11v9h13v-9" />
      <path d="M10 20v-5h4v5" />
    </svg>
  )
}

export function IconoEscudo(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <path d="M12 3.5 19 6v6c0 5-3 7.7-7 8.5-4-.8-7-3.5-7-8.5V6z" />
      <path d="m9.2 12 1.9 1.9 3.7-3.7" />
    </svg>
  )
}

export function IconoLapiz(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <path d="M4 20h4l10.5-10.5a2.1 2.1 0 0 0-3-3L5 17v3z" />
    </svg>
  )
}

export function IconoProhibido(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <circle cx="12" cy="12" r="8.2" />
      <path d="m6.5 6.5 11 11" />
    </svg>
  )
}

export function IconoMas(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <path d="M12 5v14M5 12h14" />
    </svg>
  )
}

export function IconoMenuHamburguesa(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" {...props}>
      <path d="M4 6.5h16M4 12h16M4 17.5h16" />
    </svg>
  )
}

/** Manija de arrastre (reordenar filas): seis puntos, sin color propio. */
export function IconoArrastrar(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor" {...props}>
      <circle cx="9" cy="6" r="1.5" />
      <circle cx="9" cy="12" r="1.5" />
      <circle cx="9" cy="18" r="1.5" />
      <circle cx="15" cy="6" r="1.5" />
      <circle cx="15" cy="12" r="1.5" />
      <circle cx="15" cy="18" r="1.5" />
    </svg>
  )
}

export function IconoFlechaIzquierda(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <path d="m14.5 6-6 6 6 6" />
    </svg>
  )
}

export function IconoFlechaDerecha(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <path d="m9.5 6 6 6-6 6" />
    </svg>
  )
}

export function IconoDatos(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <rect x="4" y="4" width="16" height="16" rx="2" />
      <path d="M8 9.5h8M8 13h8M8 16.5h5" />
    </svg>
  )
}

export function IconoContacto(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <path d="M5 4h3.2l1.3 4.5-2 1.6a13 13 0 0 0 6.4 6.4l1.6-2 4.5 1.3V19a2 2 0 0 1-2.2 2C10.5 20.5 3.5 13.5 3 6.2A2 2 0 0 1 5 4z" />
    </svg>
  )
}

export function IconoHorario(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <circle cx="12" cy="12" r="8.5" />
      <path d="M12 7.5V12l3.2 2" />
    </svg>
  )
}

export function IconoSalir(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
      <polyline points="16 17 21 12 16 7" />
      <line x1="21" y1="12" x2="9" y2="12" />
    </svg>
  )
}

export function IconoSol(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <circle cx="12" cy="12" r="4.2" />
      <path d="M12 2v2.2M12 19.8V22M4.93 4.93l1.56 1.56M17.51 17.51l1.56 1.56M2 12h2.2M19.8 12H22M6.49 17.51l-1.56 1.56M19.07 4.93l-1.56 1.56" />
    </svg>
  )
}

