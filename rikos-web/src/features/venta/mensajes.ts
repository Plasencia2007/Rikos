import { ErrorApi } from '../../api/cliente'

/** Los codigos de error de pedidos (US-028) en el idioma del mozo. */
const MENSAJES: Record<string, string> = {
  NO_ENCONTRADO: 'Ese pedido ya no existe en este local.',
  SIN_LOCAL_ACTIVO: 'Elige primero el local en el que estás trabajando.',
  CANAL_INVALIDO: 'Ese tipo de pedido no existe.',
  MESA_REQUERIDA: 'Elige una mesa.',
  MESA_NO_ENCONTRADA: 'Esa mesa ya no existe en este local.',
  MESA_NO_DISPONIBLE: 'Esa mesa está bloqueada, unida a otra o desactivada.',
  MESA_OCUPADA: 'Esa mesa ya tiene un pedido abierto.',
  SERVICIO_REQUERIDO: 'Estamos fuera de horario: elige el servicio (desayuno, almuerzo o cena).',
  SERVICIO_INVALIDO: 'Ese servicio no existe.',
  PERSONA_NO_ENCONTRADA: 'Esa persona ya no está en el pedido.',
  PENSIONISTA_NO_ENCONTRADO: 'No se encontró a ese pensionista.',
  PENSIONISTA_INACTIVO: 'Ese pensionista está desactivado: no puede consumir.',
  PENSIONISTA_YA_EN_EL_PEDIDO: 'Ese pensionista ya está en este pedido.',
  PERSONA_CON_LINEAS: 'Esa persona ya tiene productos: no se puede quitar.',
  LINEA_COBRADA: 'Esa persona ya tiene algo cobrado: no se puede cambiar.',
  PERSONA_NO_ES_PENSIONISTA: 'Solo un pensionista puede pedir la comida de su plan.',
  PRODUCTO_NO_ENCONTRADO: 'Ese producto ya no existe.',
  PRODUCTO_NO_VENDIBLE: 'Ese producto no se vende.',
  PRODUCTO_AGOTADO: 'Ese producto está agotado en este local.',
  SIN_PRECIO: 'Ese producto todavía no tiene precio: avísale al administrador.',
  PLAN_SIN_PRECIO: 'El plan de este pensionista no tiene precio para esa comida: avísale al administrador.',
  MENU_NO_PROGRAMADO: 'Hoy no hay menú programado.',
  MENU_FUERA_DE_SERVICIO: 'El menú del día solo se pide en el almuerzo.',
  COMPONENTE_OBLIGATORIO: 'Elige una opción para cada parte del menú.',
  OPCION_INVALIDA: 'Esa opción no es del menú de hoy.',
  PLAN_FUERA_DE_SERVICIO: 'Esa comida del plan no corresponde a este servicio.',
  CANTIDAD_INVALIDA: 'La cantidad no es válida: un pensionista pide un menú o una comida del plan por línea (déjalo en 1).',
  DESCRIPCION_OBLIGATORIA: 'Escribe qué es el extra.',
  PRECIO_INVALIDO: 'Escribe el precio del extra (mayor que cero).',
  OBSERVACION_LARGA: 'La nota es muy larga (máximo 250 caracteres).',
  LINEA_NO_ENCONTRADA: 'Ese producto ya no está en el pedido.',
  LINEA_YA_COMANDADA: 'Ese producto ya salió a cocina: no se puede cambiar aquí.',
  TIPO_INVALIDO: 'Ese tipo de producto no existe.',
  PEDIDO_MODIFICADO: 'Otra persona cambió este pedido al mismo tiempo. Se actualizó: revisa y vuelve a intentarlo.',
  NUMERO_DE_PEDIDO_REPETIDO: 'Hubo un choque al numerar el pedido: vuelve a intentarlo.',
  SIN_LINEAS_PENDIENTES: 'No hay nada nuevo para mandar a cocina.',
  PEDIDO_NO_COMANDABLE: 'Este pedido ya no está en curso: no se puede comandar.',
  COMANDA_NO_ENCONTRADA: 'Esa comanda ya no existe en este local.',
  LINEA_ANULADA: 'Ese producto ya estaba anulado.',
  MOTIVO_NO_ENCONTRADO: 'Elige el motivo de la anulación.',
  MOTIVO_INVALIDO: 'Ese motivo no sirve para anular (está desactivado o es de otro tipo).',
  OBSERVACION_OBLIGATORIA: 'Este motivo pide que escribas una nota.',
  PEDIDO_NO_REORGANIZABLE: 'Este pedido ya no está en curso: no se puede reorganizar.',
  SIN_LINEAS_PARA_MOVER: 'Marca al menos un producto para pasarlo.',
  MISMO_DESTINO: 'Ya está ahí: elige otro destino.',
  SOLO_PARA_PENSIONISTA: 'La comida del plan solo puede estar en la cuenta de un pensionista.',
  TIENE_MESAS_UNIDAS: 'Esta mesa tiene mesas unidas: sepáralas primero.',
  PRINCIPAL_INVALIDA: 'Esa mesa no sirve como principal (está bloqueada, desactivada o unida a otra).',
  YA_UNIDA: 'Esa mesa ya está unida a otra.',
  NO_ESTA_UNIDA: 'Esa mesa no está unida.',
  MESA_NO_LIBRE: 'Esa mesa no está libre (tiene pedido, está bloqueada o unida).',
  MESA_INACTIVA: 'Esa mesa está desactivada.',
  TIENE_PEDIDO_ABIERTO: 'Esa mesa tiene un pedido abierto: primero hay que terminarlo.',
  PEDIDO_NO_TERMINABLE: 'Este pedido ya estaba cerrado.',
  PEDIDO_CERRADO: 'Este pedido ya está cerrado: solo se puede consultar.',
  NUMERO_DE_COMANDA_REPETIDO: 'Hubo un choque al numerar la comanda: vuelve a intentarlo.',
}

export function mensajeDePedido(error: unknown, porDefecto: string): string {
  if (error instanceof ErrorApi) {
    const codigo = error.cuerpo?.error
    if (codigo && MENSAJES[codigo]) return MENSAJES[codigo]
    if (error.estado === 403) return 'No tienes permiso para hacer esto.'
  }
  return porDefecto
}
