import { useMemo, useState } from 'react'
import { LayoutGrid, List, Plus, Search, UtensilsCrossed } from 'lucide-react'
import { Boton, EstadoVacio } from '../../comun/componentes'
import { enSoles } from '../../comun/formato/dinero'
import type { OfertaDelDia, ProductoOfrecido } from '../../api/cliente'

type Pestana = 'todo' | 'entrada' | 'segundo' | 'extra'
type Modo = 'mosaico' | 'lista'

const PESTANAS: { id: Pestana; titulo: string }[] = [
  { id: 'todo', titulo: 'Todo' },
  { id: 'entrada', titulo: 'Entrada' },
  { id: 'segundo', titulo: 'Segundo' },
  { id: 'extra', titulo: 'Extra' },
]

const COMPONENTE: Record<string, string> = {
  entrada: 'Entrada',
  segundo: 'Segundo',
  guarnicion: 'Guarnición',
  refresco: 'Refresco',
  postre: 'Postre',
}

interface Item {
  clave: string
  titulo: string
  subtitulo?: string
  precio?: string
  nota?: string
  /** Foto del producto (data URL); si no hay, se ve el icono. */
  imagen?: string | null
  onAgregar: () => void
}

/**
 * El catalogo del pedido (diseno aprobado): buscador arriba, pestanas Todo / Entrada / Segundo / Extra y cambio entre
 * mosaico y lista. "Todo" trae primero las tarjetas del menu del dia y del menu extra (solo si hay menu confirmado
 * hoy, abren su dialogo con opciones), la comida del plan del pensionista y luego todos los productos; Entrada, Segundo
 * y Extra filtran por el `grupo` que calcula el servidor desde la categoria raiz; el "Extra" libre (precio manual, queda
 * marcado y auditado) es una tarjeta dentro de Extra. Un producto de la carta se agrega directo.
 */
export function CatalogoDelPedido({
  oferta,
  servicio,
  esPensionista,
  planDelServicio,
  bloqueado,
  trabajando,
  onMenu,
  onPlan,
  onProducto,
  onExtraLibre,
}: {
  oferta: OfertaDelDia
  servicio: string
  esPensionista: boolean
  planDelServicio: 'plan_desayuno' | 'plan_cena' | null
  bloqueado: boolean
  trabajando: boolean
  onMenu: (cual: 'menu_normal' | 'menu_extra') => void
  onPlan: (tipo: 'plan_desayuno' | 'plan_cena') => void
  onProducto: (producto: ProductoOfrecido) => void
  onExtraLibre: () => void
}) {
  const [pestana, setPestana] = useState<Pestana>('todo')
  const [modo, setModo] = useState<Modo>('mosaico')
  const [busqueda, setBusqueda] = useState('')

  const resumenDelMenu = (cual: 'normal' | 'extra') =>
    (cual === 'extra' ? oferta.extra : oferta.normal).map((c) => COMPONENTE[c.componente] ?? c.componente).join(' · ')

  const items = useMemo(() => {
    const tarjetasDeMenu: Item[] = []
    if (oferta.hayMenu) {
      tarjetasDeMenu.push({
        clave: 'menu-normal',
        titulo: 'Menú del día',
        subtitulo: resumenDelMenu('normal'),
        precio: esPensionista ? 'Precio de su plan' : enSoles(oferta.precioMenuNormal),
        onAgregar: () => onMenu('menu_normal'),
      })
      if (oferta.extra.length > 0) {
        tarjetasDeMenu.push({
          clave: 'menu-extra',
          titulo: 'Menú extra',
          subtitulo: resumenDelMenu('extra'),
          precio: enSoles(oferta.precioMenuExtra),
          onAgregar: () => onMenu('menu_extra'),
        })
      }
    }
    const delPlan: Item[] = planDelServicio
      ? [
          {
            clave: planDelServicio,
            titulo: planDelServicio === 'plan_desayuno' ? 'Desayuno del plan' : 'Cena del plan',
            subtitulo: 'La comida fija de su plan',
            precio: 'Precio de su plan',
            onAgregar: () => onPlan(planDelServicio),
          },
        ]
      : []
    const producto = (p: ProductoOfrecido): Item => ({
      clave: p.id,
      titulo: p.nombre,
      subtitulo: p.categoria ?? undefined,
      imagen: p.imagen,
      precio: enSoles(p.precio),
      onAgregar: () => onProducto(p),
    })
    const libre: Item = {
      clave: 'extra-libre',
      titulo: 'Extra',
      subtitulo: 'Algo que no está en la carta',
      nota: 'Tú pones el precio',
      onAgregar: onExtraLibre,
    }
    const delGrupo = (grupo: string) => oferta.carta.filter((p) => p.grupo === grupo).map(producto)
    switch (pestana) {
      case 'entrada':
        return delGrupo('entrada')
      case 'segundo':
        return delGrupo('segundo')
      case 'extra':
        return [libre, ...delGrupo('extra')]
      default:
        return [...tarjetasDeMenu, ...delPlan, ...oferta.carta.map(producto)]
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [oferta, pestana, esPensionista, planDelServicio, onMenu, onPlan, onProducto, onExtraLibre])

  const visibles = useMemo(() => {
    const q = busqueda.trim().toLowerCase()
    return q ? items.filter((i) => `${i.titulo} ${i.subtitulo ?? ''}`.toLowerCase().includes(q)) : items
  }, [items, busqueda])

  return (
    <div className="ped3-catalogo">
      <label className="ped3-buscador">
        <Search size={18} aria-hidden="true" />
        <input
          type="search"
          placeholder="Buscar productos…"
          aria-label="Buscar productos"
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)}
        />
      </label>

      <div className="ped3-barra-catalogo">
        <div className="ped3-pestanas" role="tablist" aria-label="Catálogo">
          {PESTANAS.map((p) => (
            <button
              key={p.id}
              type="button"
              role="tab"
              aria-selected={pestana === p.id}
              className={`ped3-pestana ${pestana === p.id ? 'ped3-pestana-activa' : ''}`}
              onClick={() => setPestana(p.id)}
            >
              {p.titulo}
            </button>
          ))}
        </div>
        <h3 className="ped4-productos">Productos</h3>
        <div className="ped3-modos" role="group" aria-label="Vista del catálogo">
          <button
            type="button"
            className={`ped3-modo ${modo === 'mosaico' ? 'ped3-modo-activo' : ''}`}
            aria-pressed={modo === 'mosaico'}
            aria-label="Mosaico"
            onClick={() => setModo('mosaico')}
          >
            <LayoutGrid size={18} aria-hidden="true" />
          </button>
          <button
            type="button"
            className={`ped3-modo ${modo === 'lista' ? 'ped3-modo-activo' : ''}`}
            aria-pressed={modo === 'lista'}
            aria-label="Lista"
            onClick={() => setModo('lista')}
          >
            <List size={18} aria-hidden="true" />
          </button>
        </div>
      </div>

      {pestana === 'todo' && !oferta.hayMenu && servicio === 'almuerzo' && !busqueda && (
        <p className="cta-nota ped4-aviso-menu">Hoy no hay menú programado: se pide de la carta.</p>
      )}

      {visibles.length === 0 ? (
        <EstadoVacio
          titulo={busqueda ? 'Nada coincide' : 'No hay productos en esta categoría'}
          descripcion={
            busqueda
              ? `No hay nada con "${busqueda}" aquí.`
              : 'Los productos con precio y disponibles de este local salen aquí.'
          }
        />
      ) : (
        <ul className={modo === 'mosaico' ? 'ped2-rejilla ped3-rejilla' : 'ped3-lista'} aria-label="Productos">
          {visibles.map((i) => (
            <li key={i.clave} className={modo === 'mosaico' ? 'ped3-tarjeta' : 'ped3-fila'}>
              <div className="ped3-imagen" aria-hidden="true">
                {i.imagen ? <img src={i.imagen} alt="" loading="lazy" /> : <UtensilsCrossed size={modo === 'mosaico' ? 30 : 20} />}
              </div>
              <div className="ped3-texto">
                <strong>{i.titulo}</strong>
                {i.subtitulo && <span className="ped3-subtitulo">{i.subtitulo}</span>}
                {i.nota && <span className="ped3-subtitulo">{i.nota}</span>}
              </div>
              <div className="ped3-pie">
                {i.precio && <strong className="ped3-precio">{i.precio}</strong>}
                <Boton
                  tamano="tactil"
                  aria-label={`Agregar ${i.titulo}`}
                  disabled={bloqueado || trabajando}
                  onClick={i.onAgregar}
                >
                  <Plus size={16} aria-hidden="true" /> Agregar
                </Boton>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
