import type { DatosMesa, Mesa } from '../../api/cliente'

/**
 * Los datos que pide el PUT de una mesa, tomados de la mesa tal como está, con los cambios encima. El PUT
 * reemplaza la mesa entera, así que hay que mandar también lo que no cambia.
 */
export function datosDeMesa(m: Mesa, cambios: Partial<DatosMesa> = {}): DatosMesa {
  return {
    numero: m.numero,
    nombre: m.nombre,
    capacidad: m.capacidad,
    salonId: m.salonId,
    forma: m.forma,
    vip: m.vip,
    zonaExterior: m.zonaExterior,
    zonaFumadores: m.zonaFumadores,
    ancho: m.ancho,
    alto: m.alto,
    ...cambios,
  }
}
