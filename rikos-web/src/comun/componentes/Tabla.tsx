import type { ReactNode } from 'react'
import { EstadoCargando } from './EstadoCargando'
import { EstadoError } from './EstadoError'
import { EstadoVacio } from './EstadoVacio'

export interface ColumnaTabla<T> {
  encabezado: string
  render: (fila: T) => ReactNode
  /** Icono opcional junto al texto del encabezado (puramente visual). */
  iconoEncabezado?: ReactNode
  /** Para la columna de acciones: ancho fijo, alineada a la derecha. */
  acciones?: boolean
}

interface Props<T> {
  columnas: ColumnaTabla<T>[]
  filas: T[]
  obtenerClave: (fila: T) => string
  cargando?: boolean
  error?: string
  vacioTitulo?: string
  vacioDescripcion?: string
  vacioAccion?: ReactNode
}

/**
 * Los cuatro estados de cualquier pantalla de lista (cargando, con datos,
 * vacia, con error) resueltos en un solo lugar: ninguna pantalla nueva
 * tiene excusa para olvidarse de alguno.
 */
export function Tabla<T>({
  columnas,
  filas,
  obtenerClave,
  cargando,
  error,
  vacioTitulo = 'Todavia no hay nada aca',
  vacioDescripcion,
  vacioAccion,
}: Props<T>) {
  if (cargando) return <EstadoCargando />
  if (error) return <EstadoError mensaje={error} />
  if (filas.length === 0) {
    return <EstadoVacio titulo={vacioTitulo} descripcion={vacioDescripcion} accion={vacioAccion} />
  }

  return (
    <table className="tabla">
      <thead>
        <tr>
          {columnas.map((columna) => (
            <th key={columna.encabezado} className={columna.acciones ? 'tabla-columna-acciones' : ''}>
              {columna.acciones && !columna.iconoEncabezado ? (
                <span className="visualmente-oculto">{columna.encabezado}</span>
              ) : (
                <span className="tabla-encabezado-contenido">
                  {columna.iconoEncabezado}
                  {columna.encabezado}
                </span>
              )}
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {filas.map((fila) => (
          <tr key={obtenerClave(fila)}>
            {columnas.map((columna) => (
              <td key={columna.encabezado} className={columna.acciones ? 'tabla-columna-acciones' : ''}>
                {columna.render(fila)}
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  )
}
