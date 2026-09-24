/**
 * Formato de montos.
 *
 * Nota para cuando se haga la logica de cobro: los importes NO se
 * manipulan como numeros decimales de JavaScript. Esto es solo
 * presentacion; el calculo vive en el backend.
 */
export function soles(monto: number): string {
  return new Intl.NumberFormat('es-PE', {
    style: 'currency',
    currency: 'PEN',
  }).format(monto)
}

/**
 * 2.6: los importes (dinero) se guardan como centimos en un entero, no
 * como soles con decimales -- nadie paga S/ 12.4567. Esta funcion solo
 * formatea, nunca redondea: no hay nada que redondear en un entero. El
 * redondeo mitad-arriba aplica a los costos (6 decimales), que se
 * formatean aparte y no en este sprint.
 */
export function formatearMoneda(centimos: number): string {
  const soles = centimos / 100
  return `S/ ${soles.toFixed(2)}`
}
