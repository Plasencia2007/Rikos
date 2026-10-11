import type { EstadoDelPedido, ResumenDePedido } from '../../api/cliente'

/** Lo que se ve como título de un pedido: el nombre de la mesa o "Para llevar". */
export function tituloDelPedido(p: ResumenDePedido): string {
  if (p.canal === 'para_llevar') return 'Para llevar'
  return p.mesaNombre || (p.mesaNumero ? `Mesa ${p.mesaNumero}` : 'Mesa')
}

export const ETIQUETA_DE_ESTADO: Record<EstadoDelPedido, string> = {
  abierto: 'Abierto',
  comandado: 'En cocina',
  pendiente_cierre: 'Por cobrar',
  cerrado: 'Cerrada',
}

export const ETIQUETA_DE_CANAL = { mesa: 'Para comer aquí', para_llevar: 'Para llevar' } as const

/** "14:15" desde una fecha ISO; vacío si no es válida. */
export function horaDe(iso: string): string {
  const d = new Date(iso)
  return Number.isNaN(d.getTime()) ? '' : d.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit', hour12: false })
}
