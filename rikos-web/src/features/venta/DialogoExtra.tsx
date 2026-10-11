import { useState } from 'react'
import { Aviso, Boton, Campo, Modal } from '../../comun/componentes'
import { aCentimos } from '../../comun/formato/dinero'

/**
 * El "extra": algo que no esta en la carta (un huevo de mas). Es el UNICO caso en que el mozo escribe un precio;
 * queda marcado como manual y en la bitacora.
 */
export function DialogoExtra({
  trabajando,
  onAgregar,
  onCerrar,
}: {
  trabajando: boolean
  onAgregar: (descripcion: string, precio: number, cantidad: number) => void
  onCerrar: () => void
}) {
  const [descripcion, setDescripcion] = useState('')
  const [precio, setPrecio] = useState('')
  const [cantidad, setCantidad] = useState('1')
  const [error, setError] = useState('')

  function agregar() {
    const centimos = aCentimos(precio)
    const unidades = Number(cantidad)
    if (descripcion.trim() === '') return setError('Escribe qué es el extra.')
    if (centimos == null || Number.isNaN(centimos)) return setError('Escribe el precio, por ejemplo 3 o 3.50.')
    if (!Number.isInteger(unidades) || unidades < 1) return setError('La cantidad tiene que ser un número entero desde 1.')
    onAgregar(descripcion.trim(), centimos, unidades)
  }

  return (
    <Modal
      abierto
      titulo="Extra"
      onCerrar={onCerrar}
      pie={
        <>
          <Boton variante="secundario" onClick={onCerrar}>
            Cancelar
          </Boton>
          <Boton cargando={trabajando} onClick={agregar}>
            Agregar al pedido
          </Boton>
        </>
      }
    >
      <div className="ped-dialogo">
        <p className="cta-nota">
          Para algo que no está en la carta. El precio que escribas queda registrado como puesto a mano.
        </p>
        {error && <Aviso tono="error">{error}</Aviso>}
        <Campo etiqueta="Qué es" maxLength={120} value={descripcion} onChange={(e) => setDescripcion(e.target.value)} />
        <Campo etiqueta="Precio (S/)" inputMode="decimal" value={precio} onChange={(e) => setPrecio(e.target.value)} />
        <Campo etiqueta="Cantidad" inputMode="numeric" value={cantidad} onChange={(e) => setCantidad(e.target.value)} />
      </div>
    </Modal>
  )
}
