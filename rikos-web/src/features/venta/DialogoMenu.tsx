import { useState } from 'react'
import { Check, ConciergeBell, CupSoda, IceCreamCone, Leaf, Salad, UtensilsCrossed, type LucideIcon } from 'lucide-react'
import { Aviso, Boton, Modal } from '../../comun/componentes'
import { enSoles } from '../../comun/formato/dinero'
import type { OfertaDelDia } from '../../api/cliente'

const PARTE: Record<string, { titulo: string; articulo: string; Icono: LucideIcon }> = {
  entrada: { titulo: 'Entrada', articulo: 'la', Icono: Leaf },
  segundo: { titulo: 'Segundo', articulo: 'el', Icono: ConciergeBell },
  guarnicion: { titulo: 'Guarnición', articulo: 'la', Icono: Salad },
  refresco: { titulo: 'Refresco', articulo: 'el', Icono: CupSoda },
  postre: { titulo: 'Postre', articulo: 'el', Icono: IceCreamCone },
}
const parteDe = (componente: string) =>
  PARTE[componente] ?? { titulo: componente, articulo: 'el', Icono: UtensilsCrossed }

/**
 * El menu del dia (o el extra): una parte por componente. Con varias opciones el mozo elige una en tarjetas
 * numeradas ("1 Elige la entrada", "2 Elige el segundo"); con una sola opcion va en "Incluye". El precio no se calcula
 * aqui: se muestra el normal y, si es un pensionista, se avisa que se cobra el de su plan (lo decide el servidor).
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

  const porElegir = componentes.filter((c) => c.opciones.length > 1)
  const incluidos = componentes.filter((c) => c.opciones.length === 1)
  const listas = porElegir.filter((c) => elegidas[c.componente]).length
  const precioTexto = precio == null ? 'Sin precio' : enSoles(precio)

  function agregar() {
    const faltan = porElegir.filter((c) => !elegidas[c.componente])
    if (faltan.length > 0) {
      setError(`Elige una opción de: ${faltan.map((c) => parteDe(c.componente).titulo).join(', ')}.`)
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
      ancho="amplio"
      titulo={tipo === 'menu_extra' ? 'Menú extra' : 'Menú del día'}
      onCerrar={onCerrar}
      pie={
        <>
          <Boton variante="secundario" onClick={onCerrar}>
            Cancelar
          </Boton>
          <Boton cargando={trabajando} onClick={agregar}>
            Agregar al pedido · {esPensionista ? 'precio del plan' : precioTexto}
          </Boton>
        </>
      }
    >
      <div className="menu-dialogo">
        <div className="menu-cabecera">
          <strong className="menu-precio">{precioTexto}</strong>
          {porElegir.length > 0 && (
            <span className={`menu-progreso ${listas === porElegir.length ? 'listo' : ''}`}>
              {listas === porElegir.length ? <Check size={16} strokeWidth={2.6} aria-hidden="true" /> : null}
              {listas} de {porElegir.length} elegidas
            </span>
          )}
          {esPensionista && <span className="cta-nota">A este pensionista se le cobra el precio de su plan.</span>}
        </div>

        {error && <Aviso tono="error">{error}</Aviso>}

        {porElegir.length > 0 && (
          <div className="menu-pasos">
            {porElegir.map((c, indice) => {
              const { titulo, articulo, Icono } = parteDe(c.componente)
              const elegida = elegidas[c.componente]
              return (
                <section key={c.componente} className="menu-paso" aria-label={titulo}>
                  <header>
                    <span className={`menu-numero ${elegida ? 'listo' : ''}`}>
                      {elegida ? <Check size={15} strokeWidth={3} aria-hidden="true" /> : indice + 1}
                    </span>
                    <h3>
                      Elige {articulo} {titulo.toLowerCase()}
                    </h3>
                  </header>
                  <div className="menu-opciones" role="radiogroup" aria-label={titulo}>
                    {c.opciones.map((o) => {
                      const marcada = elegida === o.opcionMenuId
                      return (
                        <label key={o.opcionMenuId} className={`menu-opcion ${marcada ? 'elegida' : ''}`}>
                          <input
                            type="radio"
                            name={c.componente}
                            checked={marcada}
                            onChange={() => {
                              setError('')
                              setElegidas({ ...elegidas, [c.componente]: o.opcionMenuId })
                            }}
                          />
                          <span className="menu-opcion-icono" aria-hidden="true">
                            <Icono size={20} strokeWidth={1.8} />
                          </span>
                          <span className="menu-opcion-nombre">{o.productoNombre}</span>
                          <span className="menu-opcion-marca" aria-hidden="true">
                            <Check size={14} strokeWidth={3} />
                          </span>
                        </label>
                      )
                    })}
                  </div>
                </section>
              )
            })}
          </div>
        )}

        {incluidos.length > 0 && (
          <section className="menu-incluye" aria-label="Incluye">
            <h3>Incluye</h3>
            <div className="menu-incluidos">
              {incluidos.map((c) => {
                const { titulo, Icono } = parteDe(c.componente)
                return (
                  <div key={c.componente} className="menu-incluido">
                    <span className="menu-opcion-icono" aria-hidden="true">
                      <Icono size={20} strokeWidth={1.8} />
                    </span>
                    <span>
                      <small>{titulo}</small>
                      <strong>{c.opciones[0].productoNombre}</strong>
                    </span>
                  </div>
                )
              })}
            </div>
          </section>
        )}

        <label className="menu-nota">
          <span>
            Nota para cocina <em>(opcional)</em>
          </span>
          <textarea
            rows={2}
            maxLength={250}
            placeholder="Ej. sin cebolla, poco ají…"
            value={nota}
            onChange={(e) => setNota(e.target.value)}
          />
        </label>
      </div>
    </Modal>
  )
}
