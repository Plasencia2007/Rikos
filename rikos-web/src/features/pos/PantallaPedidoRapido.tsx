import { useState } from 'react'
import { ShoppingBag, Truck } from 'lucide-react'
import { Aviso, Boton, Campo, Select } from '../../comun/componentes'
import { ErrorApi, abrirPedido, type ServicioDelPedido } from '../../api/cliente'

const SERVICIOS: { valor: ServicioDelPedido; etiqueta: string }[] = [
  { valor: 'desayuno', etiqueta: 'Desayuno' },
  { valor: 'almuerzo', etiqueta: 'Almuerzo' },
  { valor: 'cena', etiqueta: 'Cena' },
]

/**
 * Pedido rápido (diseño PosRapido): un pedido PARA LLEVAR sin mesa. Se elige el canal, se pone una referencia opcional
 * (nombre o número) y se abre el pedido; después se toma en la misma pantalla del pedido (catálogo y orden).
 * Delivery aparece atenuado hasta el sprint 12; Mostrador no existe.
 */
export function PantallaPedidoRapido({
  token,
  onAbrirPedido,
}: {
  token: string
  onAbrirPedido: (pedidoId: string, titulo: string) => void
}) {
  const [referencia, setReferencia] = useState('')
  const [servicio, setServicio] = useState<ServicioDelPedido>('almuerzo')
  const [pideServicio, setPideServicio] = useState(false)
  const [trabajando, setTrabajando] = useState(false)
  const [error, setError] = useState('')

  function abrir() {
    setTrabajando(true)
    setError('')
    const cliente = referencia.trim()
    abrirPedido(token, {
      canal: 'para_llevar',
      servicio: pideServicio ? servicio : undefined,
      nombreCliente: cliente === '' ? undefined : cliente,
    })
      .then((p) => onAbrirPedido(p.id, cliente === '' ? 'Para llevar' : `Para llevar · ${cliente}`))
      .catch((e) => {
        if (e instanceof ErrorApi && e.cuerpo?.error === 'SERVICIO_REQUERIDO') setPideServicio(true)
        else setError('No se pudo abrir el pedido. Inténtalo de nuevo.')
      })
      .finally(() => setTrabajando(false))
  }

  return (
    <div className="pos-rapido">
      <div className="pos-rapido-canales" role="radiogroup" aria-label="Canal del pedido">
        <button type="button" role="radio" aria-checked="true" className="pos-canal elegido">
          <span className="pos-canal-icono">
            <ShoppingBag size={22} aria-hidden="true" />
          </span>
          <span>
            <strong>Para llevar</strong>
            <small>Cliente recoge en tienda</small>
          </span>
          <i aria-hidden="true" />
        </button>
        <button type="button" role="radio" aria-checked="false" className="pos-canal" disabled>
          <span className="pos-canal-icono">
            <Truck size={22} aria-hidden="true" />
          </span>
          <span>
            <strong>Delivery</strong>
            <small>Envío a domicilio</small>
          </span>
          <em className="pos-pronto pos-pronto-claro">Pronto</em>
        </button>
      </div>

      <section className="pos-rapido-tarjeta">
        <h2>Nuevo pedido para llevar</h2>
        <p>No ocupa ninguna mesa. Después de abrirlo eliges los productos y lo comandas.</p>
        {error && <Aviso tono="error">{error}</Aviso>}
        <Campo
          etiqueta="Nombre o referencia (opcional)"
          placeholder="Ej. Juan, #5, gorra roja"
          maxLength={120}
          value={referencia}
          onChange={(e) => setReferencia(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && !trabajando && abrir()}
        />
        {pideServicio && (
          <>
            <Aviso tono="aviso">Estamos fuera de horario: elige el servicio y vuelve a pulsar el botón.</Aviso>
            <Select
              etiqueta="Servicio"
              value={servicio}
              onChange={(e) => setServicio(e.target.value as ServicioDelPedido)}
            >
              {SERVICIOS.map((s) => (
                <option key={s.valor} value={s.valor}>
                  {s.etiqueta}
                </option>
              ))}
            </Select>
          </>
        )}
        <Boton cargando={trabajando} onClick={abrir}>
          Abrir pedido
        </Boton>
      </section>
    </div>
  )
}
