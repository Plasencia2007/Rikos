import { useState } from 'react'
import { Aviso, Boton, Campo, Modal } from '../../comun/componentes'
import { enSoles } from '../../comun/formato/dinero'
import type { OfertaDelDia } from '../../api/cliente'

const TITULO: Record<string, string> = {
  entrada: 'Entrada',
  segundo: 'Segundo',
  guarnicion: 'Guarnición',
  refresco: 'Refresco',
  postre: 'Postre',
}

/**
 * El menu del dia (o el extra): una parte por componente. Con una sola opcion se muestra "incluida"; con varias, el
 * mozo tiene que elegir una antes de agregar. El precio no se calcula aqui: se muestra el normal y, si es un
 * pensionista, se avisa que se cobra el de su plan (lo decide el servidor).
 */
export function DialogoMenu({
  tipo,
  oferta,
  esPensionista,
  trabajando,
  onAgregar,
  onCerrar,
}: {
  tipo: 'menu_normal' | 'menu_extra'
  oferta: OfertaDelDia
  esPensionista: boolean
  trabajando: boolean
  onAgregar: (componentes: { componente: string; opcionMenuId: string }[], observacion: string | null) => void
  onCerrar: () => void
}) {
  const componentes = tipo === 'menu_extra' ? oferta.extra : oferta.normal
  const precio = tipo === 'menu_extra' ? oferta.precioMenuExtra : oferta.precioMenuNormal
  const [elegidas, setElegidas] = useState<Record<string, string>>({})
  const [nota, setNota] = useState('')
  const [error, setError] = useState('')

  function agregar() {
    const faltan = componentes.filter((c) => c.opciones.length > 1 && !elegidas[c.componente])
    if (faltan.length > 0) {
      setError(`Elige una opción de: ${faltan.map((c) => TITULO[c.componente] ?? c.componente).join(', ')}.`)
      return
    }
    // Solo se mandan las elecciones que el mozo hizo; las de una sola opcion las asigna el servidor.
    onAgregar(
      Object.entries(elegidas).map(([componente, opcionMenuId]) => ({ componente, opcionMenuId })),
      nota.trim() || null,
    )
  }

  return (
    <Modal
      abierto
      titulo={tipo === 'menu_extra' ? 'Menú extra' : 'Menú del día'}
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
        <p className="ped-precio-menu">
          {precio == null ? 'Sin precio' : enSoles(precio)}
          {esPensionista && <span className="cta-nota"> · a este pensionista se le cobra el precio de su plan</span>}
        </p>
        {error && <Aviso tono="error">{error}</Aviso>}
        {componentes.map((c) => (
          <fieldset key={c.componente} className="ped-parte">
            <legend>{TITULO[c.componente] ?? c.componente}</legend>
            {c.opciones.length === 1 ? (
              <p className="ped-incluida">
                {c.opciones[0].productoNombre} <span className="cta-nota">(incluida)</span>
              </p>
            ) : (
              c.opciones.map((o) => (
                <label
                  key={o.opcionMenuId}
                  className={`ped-opcion ${elegidas[c.componente] === o.opcionMenuId ? 'ped-opcion-elegida' : ''}`}
                >
                  <input
                    type="radio"
                    name={c.componente}
                    checked={elegidas[c.componente] === o.opcionMenuId}
                    onChange={() => {
                      setError('')
                      setElegidas({ ...elegidas, [c.componente]: o.opcionMenuId })
                    }}
                  />
                  {o.productoNombre}
                </label>
              ))
            )}
          </fieldset>
        ))}
        <Campo etiqueta="Nota para cocina" maxLength={250} value={nota} onChange={(e) => setNota(e.target.value)} />
      </div>
    </Modal>
  )
}
