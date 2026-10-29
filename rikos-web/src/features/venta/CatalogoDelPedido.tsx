import { useEffect, useMemo, useRef, useState } from 'react'
import { ConciergeBell, LayoutGrid, Leaf, List, Plus, Search, Star, UtensilsCrossed, type LucideIcon } from 'lucide-react'
import { EstadoVacio } from '../../comun/componentes'
import { enSoles } from '../../comun/formato/dinero'
import type { OfertaDelDia, ProductoOfrecido } from '../../api/cliente'

type Pestana = 'todo' | 'entrada' | 'segundo' | 'extra'
type Modo = 'mosaico' | 'lista'

const PESTANAS: { id: Pestana; titulo: string; Icono: LucideIcon }[] = [
  { id: 'todo', titulo: 'Todo', Icono: LayoutGrid },
  { id: 'entrada', titulo: 'Entrada', Icono: Leaf },
  { id: 'segundo', titulo: 'Segundo', Icono: ConciergeBell },
  { id: 'extra', titulo: 'Extra', Icono: Star },
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
 * El catalogo del pedido (diseno 2026-10-28): buscador (Ctrl + K) con el cambio entre mosaico y lista, pestanas Todo /
 * Entrada / Segundo / Extra con icono y tarjetas con foto y un boton redondo para agregar. "Todo" trae primero las tarjetas del menu del dia y del menu extra (solo si hay menu confirmado
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
  const buscador = useRef<HTMLInputElement>(null)

  // Ctrl + K (o Cmd + K) lleva al buscador desde cualquier parte de la pantalla.
  useEffect(() => {
    const atajo = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault()
        buscador.current?.focus()
      }
    }
    window.addEventListener('keydown', atajo)
    return () => window.removeEventListener('keydown', atajo)
  }, [])

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
    <div className="ped3-catalogo ped5-catalogo">
      <div className="ped5-busqueda">
        <label className="ped3-buscador ped5-buscador">
          <Search size={20} aria-hidden="true" />
          <input
            ref={buscador}
            type="search"
            placeholder="Buscar producto por nombre…"
            aria-label="Buscar productos"
            aria-keyshortcuts="Control+K"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
          />
          <kbd>Ctrl + K</kbd>
        </label>
        <div className="ped3-modos ped5-modos" role="group" aria-label="Vista del catálogo">
          <button
            type="button"
            className={`ped3-modo ${modo === 'mosaico' ? 'ped3-modo-activo' : ''}`}
            aria-pressed={modo === 'mosaico'}
            aria-label="Mosaico"
            onClick={() => setModo('mosaico')}
          >
            <LayoutGrid size={20} aria-hidden="true" />
          </button>
          <button
            type="button"
            className={`ped3-modo ${modo === 'lista' ? 'ped3-modo-activo' : ''}`}
            aria-pressed={modo === 'lista'}
            aria-label="Lista"
            onClick={() => setModo('lista')}
          >
            <List size={20} aria-hidden="true" />
          </button>
        </div>
      </div>

      <div className="ped3-pestanas ped5-chips" role="tablist" aria-label="Catálogo">
        {PESTANAS.map(({ id, titulo, Icono }) => (
          <button
            key={id}
            type="button"
            role="tab"
            aria-selected={pestana === id}
            className={`ped3-pestana ${pestana === id ? 'ped3-pestana-activa' : ''}`}
            onClick={() => setPestana(id)}
          >
            <Icono size={19} strokeWidth={1.8} aria-hidden="true" />
            {titulo}
          </button>
        ))}
      </div>

      <h3 className="ped5-catalogo-titulo">Catálogo</h3>

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
        <ul className={modo === 'mosaico' ? 'ped2-rejilla ped3-rejilla ped5-rejilla' : 'ped3-lista ped5-lista'} aria-label="Productos">
          {visibles.map((i) => (
            <li key={i.clave} className={modo === 'mosaico' ? 'ped3-tarjeta ped5-tarjeta' : 'ped3-fila ped5-fila'}>
              <div className="ped3-imagen" aria-hidden="true">
                {i.imagen ? <img src={i.imagen} alt="" loading="lazy" /> : <UtensilsCrossed size={modo === 'mosaico' ? 34 : 20} strokeWidth={1.6} />}
              </div>
              <div className="ped5-tarjeta-cuerpo">
                <div className="ped3-texto">
                  <strong title={i.titulo}>{i.titulo}</strong>
                  {(i.subtitulo || i.nota) && (
                    <span className="ped3-subtitulo">{[i.subtitulo, i.nota].filter(Boolean).join(' · ')}</span>
                  )}
                  {i.precio && <strong className="ped3-precio">{i.precio}</strong>}
                </div>
                <button
                  type="button"
                  className="ped5-agregar"
                  aria-label={`Agregar ${i.titulo}`}
                  disabled={bloqueado || trabajando}
                  onClick={i.onAgregar}
                >
                  <Plus size={22} strokeWidth={2.4} aria-hidden="true" />
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
