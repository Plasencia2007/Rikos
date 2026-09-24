type Tono = 'activo' | 'inactivo' | 'pendiente'

interface Props {
  tono: Tono
  children?: React.ReactNode
}

const TEXTO_POR_DEFECTO: Record<Tono, string> = {
  activo: 'Activo',
  inactivo: 'Inactivo',
  pendiente: 'Pendiente',
}

/** Estado corto (activo/inactivo/pendiente) para tablas y listas. */
export function Insignia({ tono, children }: Props) {
  return <span className={`insignia insignia-${tono}`}>{children ?? TEXTO_POR_DEFECTO[tono]}</span>
}
