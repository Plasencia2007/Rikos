import { ErrorApi } from '../../api/cliente'

/** Los codigos de error de caja (turnos, cobro, cierre y reapertura) en el idioma del cajero. */
const MENSAJES: Record<string, string> = {
  SIN_LOCAL_ACTIVO: 'Elige primero el local en el que estás trabajando.',
  CAJA_NO_ENCONTRADA: 'Esa caja ya no existe en este local.',
  CAJA_INACTIVA: 'Esa caja está desactivada.',
  CAJA_CON_TURNO_ABIERTO: 'Esa caja ya tiene un turno abierto.',
  CAJERO_CON_TURNO_ABIERTO: 'Ya tienes un turno abierto en otra caja.',
  FONDO_INVALIDO: 'El fondo inicial no es válido (puede ser cero, no negativo).',
  SIN_TURNO_ABIERTO: 'No tienes un turno de caja abierto.',
  CONTADO_INVALIDO: 'Escribe el efectivo que contaste (puede ser cero).',
  OBSERVACION_LARGA: 'La observación es muy larga.',
  PEDIDO_NO_ENCONTRADO: 'Ese pedido ya no existe en este local.',
  PEDIDO_CERRADO: 'Ese pedido ya está cerrado.',
  PEDIDO_MODIFICADO: 'Otra persona cambió este pedido al mismo tiempo. Se recargó: revisa y vuelve a intentar.',
  SIN_LINEAS: 'Marca al menos un producto para cobrar.',
  LINEA_NO_ENCONTRADA: 'Uno de los productos ya no está en el pedido.',
  LINEA_YA_COBRADA: 'Uno de los productos ya fue cobrado.',
  LINEA_ANULADA: 'Uno de los productos está anulado.',
  LINEA_SIN_COMANDAR: 'Solo se cobra lo que ya salió a cocina: comanda el pedido primero.',
  MEDIO_INVALIDO: 'Ese medio de pago no existe.',
  MEDIO_REPETIDO: 'Cada medio de pago va una sola vez.',
  MONTO_INVALIDO: 'Algún monto no es válido.',
  REFERENCIA_INVALIDA: 'El número de operación es muy largo.',
  PAGO_INSUFICIENTE: 'Lo que recibes no cubre el total.',
  PAGO_EXCEDE_EL_TOTAL: 'Solo el efectivo puede dar vuelto: lo digital no puede pasar del total.',
  PAGO_SIN_NADA_QUE_PAGAR: 'Todo lo marcado va a la cuenta de un pensionista: no hay nada que pagar en medios.',
  PENSIONISTA_INACTIVO: 'Un pensionista está desactivado: no puede consumir.',
  DESCUENTO_INVALIDO: 'El descuento no es válido para esa línea.',
  MOTIVO_NO_ENCONTRADO: 'Elige un motivo.',
  MOTIVO_INVALIDO: 'Ese motivo no se puede usar aquí.',
  OBSERVACION_OBLIGATORIA: 'Ese motivo exige escribir una observación.',
  TOTAL_EN_CERO: 'Con los descuentos el total queda en cero: eso no es un cobro.',
  SIN_PERMISO_PARA_DESCONTAR: 'No tienes permiso para aplicar descuentos.',
  COBRO_NO_ENCONTRADO: 'Ese cobro ya no existe.',
  COBRO_YA_REVERTIDO: 'Ese cobro ya se reabrió.',
  MESA_OCUPADA: 'La mesa ya la ocupa otro pedido: no se puede reabrir.',
  TURNO_CERRADO: 'El turno de ese cobro ya se cerró: ya no se puede reabrir.',
  NUMERO_OPERACION_REQUERIDO: 'Los pagos digitales requieren el número de operación.',
  PENSIONISTA_NO_ENCONTRADO: 'El pensionista seleccionado no existe.',
}

export function mensajeDeCaja(error: unknown, porDefecto: string): string {
  if (error instanceof ErrorApi) {
    const codigo = error.cuerpo?.error
    if (codigo && MENSAJES[codigo]) return MENSAJES[codigo]
    if (error.estado === 403) return 'No tienes permiso para hacer esto.'
  }
  return porDefecto
}

/** "12", "12.5", "12,50" -> centimos; vacio -> null; NaN si no es un monto valido. El cero SI vale (un fondo puede ser cero). */
export function soles(texto: string): number | null {
  const limpio = texto.trim().replace(',', '.')
  if (limpio === '') return null
  const valor = Number(limpio)
  if (!Number.isFinite(valor) || valor < 0) return Number.NaN
  return Math.round(valor * 100)
}
