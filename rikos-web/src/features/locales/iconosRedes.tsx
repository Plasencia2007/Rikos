import type { SVGProps } from 'react'

/** lucide-react ya no trae logos de marcas: estos tres se dibujan a mano, heredan currentColor. */
const base = { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 1.8, strokeLinecap: 'round', strokeLinejoin: 'round' } as const

export function IconoFacebook(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base} {...props}>
      <path d="M14 8h3V4h-3a4 4 0 0 0-4 4v3H7v4h3v6h4v-6h3l1-4h-4V8z" />
    </svg>
  )
}

export function IconoInstagram(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base} {...props}>
      <rect x="4" y="4" width="16" height="16" rx="5" />
      <circle cx="12" cy="12" r="3.6" />
      <circle cx="17" cy="7" r=".9" fill="currentColor" />
    </svg>
  )
}

export function IconoTiktok(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base} {...props}>
      <path d="M14 4v10.5a3.5 3.5 0 1 1-3.5-3.5M14 4c.4 2.4 2 4 4.5 4.2" />
    </svg>
  )
}
