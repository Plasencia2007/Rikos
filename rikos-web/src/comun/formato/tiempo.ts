/** "125" -> "2:05". Para mostrar cuanto falta de un bloqueo. */
export function minutosSegundos(totalSegundos: number): string {
  const segundos = Math.max(0, Math.round(totalSegundos))
  const minutos = Math.floor(segundos / 60)
  const resto = segundos % 60
  return `${minutos}:${String(resto).padStart(2, '0')}`
}

/** "42 min" o "1 h 12 min" desde que se abrio algo (fecha ISO); vacio si no hay fecha valida. */
export function tiempoTranscurrido(desdeIso: string | null | undefined, ahora: number = Date.now()): string {
  if (!desdeIso) return ''
  const inicio = new Date(desdeIso).getTime()
  if (Number.isNaN(inicio)) return ''
  const minutos = Math.max(0, Math.floor((ahora - inicio) / 60000))
  if (minutos < 60) return `${minutos} min`
  const resto = minutos % 60
  return resto === 0 ? `${Math.floor(minutos / 60)} h` : `${Math.floor(minutos / 60)} h ${resto} min`
}
