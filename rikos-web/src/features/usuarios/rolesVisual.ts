import {
  Banknote,
  ChartColumn,
  ChefHat,
  ConciergeBell,
  Bike,
  Contact,
  Package,
  Receipt,
  Settings,
  ShieldCheck,
  ShoppingBag,
  ShoppingCart,
  Store,
  Tag,
  Truck,
  UserRound,
  Users,
  UtensilsCrossed,
  type LucideIcon,
} from 'lucide-react'

export const capitalizar = (t: string) => t.charAt(0).toUpperCase() + t.slice(1)

/** Icono de cada rol del sistema (por nombre); cualquier otro rol usa el genérico. */
const ICONO_POR_ROL: Record<string, LucideIcon> = {
  'administrador central': Settings,
  cajero: Banknote,
  mozo: ConciergeBell,
  cocina: ChefHat,
  motorizado: Bike,
  'administrador de local': Store,
}

export const iconoDeRol = (nombre: string): LucideIcon => ICONO_POR_ROL[nombre] ?? UserRound

export interface DatosModulo {
  etiqueta: string
  descripcion: string
  Icono: LucideIcon
}

/** Presentación de cada módulo del catálogo de permisos; uno nuevo cae en el genérico (nombre capitalizado). */
const MODULOS: Record<string, DatosModulo> = {
  auditoria: { etiqueta: 'Auditoría', descripcion: 'Consulta la bitácora de cambios del sistema.', Icono: ShieldCheck },
  caja: { etiqueta: 'Caja', descripcion: 'Gestiona los movimientos de caja y turnos.', Icono: Banknote },
  catalogo: { etiqueta: 'Catálogo', descripcion: 'Administra productos y categorías.', Icono: Tag },
  compras: { etiqueta: 'Compras', descripcion: 'Registra compras, proveedores y órdenes.', Icono: ShoppingCart },
  delivery: { etiqueta: 'Delivery', descripcion: 'Atiende los pedidos a domicilio.', Icono: Truck },
  facturacion: { etiqueta: 'Facturación', descripcion: 'Emite y exporta comprobantes electrónicos.', Icono: Receipt },
  identidad: { etiqueta: 'Identidad', descripcion: 'Gestiona usuarios, roles y locales.', Icono: Users },
  inventario: { etiqueta: 'Inventario', descripcion: 'Controla existencias y kardex.', Icono: Package },
  menu: { etiqueta: 'Menú', descripcion: 'Programa el menú semanal y los agotados.', Icono: UtensilsCrossed },
  pensionistas: { etiqueta: 'Pensionistas', descripcion: 'Fichas, cuentas y abonos de pensionistas.', Icono: Contact },
  recetas: { etiqueta: 'Recetas', descripcion: 'Recetas y su costo.', Icono: ChefHat },
  reportes: { etiqueta: 'Reportes', descripcion: 'Reportes de ventas, márgenes y costos.', Icono: ChartColumn },
  venta: { etiqueta: 'Venta', descripcion: 'Pedidos y cuentas del salón y el POS.', Icono: ShoppingBag },
}

export function datosDeModulo(modulo: string): DatosModulo {
  return MODULOS[modulo] ?? { etiqueta: capitalizar(modulo), descripcion: `Permisos de ${modulo}.`, Icono: Package }
}

/** Acciones que conviene marcar como "acceso sensible" (dinero, anulaciones, costos, administración). */
const ACCIONES_SENSIBLES = new Set(['anular', 'reabrir', 'descontar', 'aprobar', 'ver_costos', 'administrar', 'administrar_permisos'])
export const esAccesoSensible = (accion: string) => ACCIONES_SENSIBLES.has(accion)

/** Paleta del color del rol: verde de la marca primero. */
export const PALETA_ROL = [
  { hex: '#2B7A68', nombre: 'Verde Riko’s' },
  { hex: '#2563EB', nombre: 'Azul' },
  { hex: '#7C3AED', nombre: 'Violeta' },
  { hex: '#D97706', nombre: 'Ámbar' },
  { hex: '#DC2626', nombre: 'Rojo' },
  { hex: '#0891B2', nombre: 'Turquesa' },
  { hex: '#64748B', nombre: 'Gris' },
]
