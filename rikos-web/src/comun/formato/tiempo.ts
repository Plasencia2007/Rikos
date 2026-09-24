/** "125" -> "2:05". Para mostrar cuanto falta de un bloqueo. */
export function minutosSegundos(totalSegundos: number): string {
  const segundos = Math.max(0, Math.round(totalSegundos))
  const minutos = Math.floor(segundos / 60)
  const resto = segundos % 60
  return `${minutos}:${String(resto).padStart(2, '0')}`
}
