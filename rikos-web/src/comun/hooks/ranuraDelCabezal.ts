import { createContext, useContext } from 'react'

/**
 * El lugar del cabezal del POS donde una pantalla pone su propio título, en vez de la ruta "Punto de venta / …".
 * Vale null fuera del POS o cuando la pantalla no lo pidió: entonces la pantalla dibuja su título en el contenido.
 */
export const RanuraDelCabezal = createContext<HTMLElement | null>(null)

export const useRanuraDelCabezal = () => useContext(RanuraDelCabezal)
