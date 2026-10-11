/** "Hoy, 10:42" / "Ayer, 19:05" / "22/09/2026, 18:27": para mostrar cuándo pasó algo. */
export function formatearMomento(iso: string): string {
  const fecha = new Date(iso)
  const hoy = new Date()
  const ayer = new Date()
  ayer.setDate(hoy.getDate() - 1)
  const hora = fecha.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit', hour12: false })
  if (fecha.toDateString() === hoy.toDateString()) return `Hoy, ${hora}`
  if (fecha.toDateString() === ayer.toDateString()) return `Ayer, ${hora}`
  return `${fecha.toLocaleDateString('es-PE', { day: '2-digit', month: '2-digit', year: 'numeric' })}, ${hora}`
}
