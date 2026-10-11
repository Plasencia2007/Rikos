import { useEffect, useMemo, useState } from 'react'
import { ArrowLeft, Percent } from 'lucide-react'
import { Aviso, Boton, Campo, EstadoCargando, EstadoError, Insignia, Modal, Select } from '../../comun/componentes'
import { enSoles } from '../../comun/formato/dinero'
import {
  cobrarPedido,
  listarMotivos,
  obtenerPedido,
  type CobroRegistrado,
  type DescuentoACobrar,
  type LineaDelPedido,
  type MedioDePago,
  type Motivo,
  type Pedido,
  type PersonaDelPedido,
} from '../../api/cliente'
import { mensajeDeCaja, soles } from './mensajes'

const MEDIOS: { medio: MedioDePago; titulo: string }[] = [
  { medio: 'efectivo', titulo: 'Efectivo' },
  { medio: 'yape', titulo: 'Yape' },
  { medio: 'plin', titulo: 'Plin' },
  { medio: 'tarjeta', titulo: 'Tarjeta' },
]

function nombreDe(l: LineaDelPedido): string {
  return l.productoNombre ?? l.descripcion ?? 'Producto'
}

/** Lo que vale la linea antes del descuento: precio y taper por cantidad (igual que el servidor, que es quien decide). */
function brutoDe(l: LineaDelPedido): number {
  return (l.precioUnitario + l.taperUnitario) * l.cantidad
}

function descuentoDe(bruto: number, d: DescuentoACobrar | undefined): number {
  if (!d || d.valor <= 0) return 0
  return d.tipo === 'monto' ? d.valor : Math.floor((bruto * d.valor + 50) / 100)
}

/**
 * US-033 y US-034: cobrar LINEAS del pedido. Se marcan los productos que se pagan ahora (de una o varias personas), opcionalmente
 * con un descuento por linea, y se reparte lo que se recibe entre los medios (en efectivo se escribe lo RECIBIDO y el sistema da el
 * vuelto). Lo de un pensionista no pide medios: se carga a su cuenta. Los totales de aqui son solo de pantalla; el servidor decide.
 */
export function CobrarPedidoDeCaja({
  token,
  pedidoId,
  puedeDescontar,
  onVolver,
  onCobrado,
}: {
  token: string
  pedidoId: string
  puedeDescontar: boolean
  onVolver: () => void
  onCobrado: () => void
}) {
  const [pedido, setPedido] = useState<Pedido | null>(null)
  const [error, setError] = useState(false)
  const [motivos, setMotivos] = useState<Motivo[]>([])
  const [marcadas, setMarcadas] = useState<string[]>([])
  const [descuentos, setDescuentos] = useState<Record<string, DescuentoACobrar>>({})
  const [editando, setEditando] = useState<LineaDelPedido | null>(null)
  const [pagos, setPagos] = useState<Record<MedioDePago, string>>({ efectivo: '', yape: '', plin: '', tarjeta: '' })
  const [referencias, setReferencias] = useState<Record<string, string>>({})
  const [trabajando, setTrabajando] = useState(false)
  const [aviso, setAviso] = useState('')
  const [resultado, setResultado] = useState<CobroRegistrado | null>(null)

  function cargar() {
    setError(false)
    obtenerPedido(token, pedidoId)
      .then((p) => {
        setPedido(p)
        // Lo que ya no se puede cobrar (otro cajero lo cobro, se anulo) sale de la seleccion.
        const cobrables = new Set(p.personas.flatMap((x) => x.lineas).filter((l) => l.estado === 'comandado').map((l) => l.id))
        setMarcadas((m) => m.filter((id) => cobrables.has(id)))
      })
      .catch(() => setError(true))
  }

  useEffect(() => {
    cargar()
    if (puedeDescontar) {
      // "Descuentos habilitados": los motivos de tipo descuento que estan activos.
      listarMotivos(token, 'descuento')
        .then((lista) => setMotivos(lista.filter((m) => m.activo)))
        .catch(() => setMotivos([]))
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, pedidoId, puedeDescontar])

  const cuenta = useMemo(() => {
    if (!pedido) return { medios: 0, cuentas: 0, descuentoTotal: 0 }
    let medios = 0
    let cuentas = 0
    let descuentoTotal = 0
    for (const persona of pedido.personas) {
      for (const l of persona.lineas) {
        if (!marcadas.includes(l.id)) continue
        const bruto = brutoDe(l)
        const d = Math.min(bruto, descuentoDe(bruto, descuentos[l.id]))
        descuentoTotal += d
        if (persona.tipoCliente === 'pensionista') cuentas += bruto - d
        else medios += bruto - d
      }
    }
    return { medios, cuentas, descuentoTotal }
  }, [pedido, marcadas, descuentos])

  if (error) {
    return (
      <EstadoError
        mensaje="No se pudo cargar el pedido."
        accion={
          <>
            <Boton onClick={cargar}>Reintentar</Boton>
            <Boton variante="secundario" onClick={onVolver}>
              Volver a la caja
            </Boton>
          </>
        }
      />
    )
  }
  if (!pedido) return <EstadoCargando />

  if (resultado) {
    return (
      <div className="caja-resultado" role="status">
        <h2>Cobro registrado</h2>
        <p className="caja-resultado-total">{enSoles(resultado.total)}</p>
        {resultado.vuelto > 0 && (
          <p className="caja-vuelto">
            Vuelto: <strong>{enSoles(resultado.vuelto)}</strong>
          </p>
        )}
        <ul className="caja-resultado-lista">
          {resultado.pagos.map((p) => (
            <li key={p.medio}>
              {MEDIOS.find((m) => m.medio === p.medio)?.titulo}: {enSoles(p.importe)}
              {p.referencia ? ` · op. ${p.referencia}` : ''}
            </li>
          ))}
          {resultado.cargos.map((c) => (
            <li key={c.pensionistaId}>
              Cargado a la cuenta de {c.nombre}: {enSoles(c.importe)}
              {c.superaTope && <Insignia tono="pendiente">Supera su tope de crédito</Insignia>}
            </li>
          ))}
        </ul>
        {resultado.pedidoCerrado ? (
          <Aviso>El pedido quedó cerrado y la mesa está libre.</Aviso>
        ) : (
          <Aviso>Quedan productos por cobrar: la mesa sigue ocupada.</Aviso>
        )}
        <Boton tamano="tactil" onClick={onCobrado}>
          Listo
        </Boton>
      </div>
    )
  }

  const cerrado = pedido.estado === 'cerrado'
  const recibido = MEDIOS.reduce((suma, m) => {
    const c = soles(pagos[m.medio])
    return suma + (c && !Number.isNaN(c) ? c : 0)
  }, 0)
  const digital = MEDIOS.filter((m) => m.medio !== 'efectivo').reduce((suma, m) => {
    const c = soles(pagos[m.medio])
    return suma + (c && !Number.isNaN(c) ? c : 0)
  }, 0)
  const diferencia = recibido - cuenta.medios
  const huboPagoInvalido = MEDIOS.some((m) => Number.isNaN(soles(pagos[m.medio])))
  const alcanza = cuenta.medios === 0 ? recibido === 0 : recibido >= cuenta.medios && digital <= cuenta.medios
  const puedeCobrar = !cerrado && marcadas.length > 0 && !huboPagoInvalido && alcanza && cuenta.medios + cuenta.cuentas > 0

  function marcar(l: LineaDelPedido, marcada: boolean) {
    setMarcadas((m) => (marcada ? [...m, l.id] : m.filter((id) => id !== l.id)))
  }

  function marcarTodo() {
    if (!pedido) return
    setMarcadas(pedido.personas.flatMap((p) => p.lineas).filter((l) => l.estado === 'comandado').map((l) => l.id))
  }

  async function cobrar() {
    if (!puedeCobrar) return
    setTrabajando(true)
    setAviso('')
    const lista = MEDIOS.flatMap((m) => {
      const c = soles(pagos[m.medio])
      return c && !Number.isNaN(c)
        ? [{ medio: m.medio, monto: c, referencia: m.medio === 'efectivo' ? null : (referencias[m.medio] || null) }]
        : []
    })
    try {
      const hechos = Object.values(descuentos).filter((d) => marcadas.includes(d.lineaId))
      setResultado(await cobrarPedido(token, pedidoId, marcadas, lista, hechos))
    } catch (e) {
      setAviso(mensajeDeCaja(e, 'No se pudo cobrar. Inténtalo de nuevo.'))
      cargar()
    } finally {
      setTrabajando(false)
    }
  }

  const porCobrar = pedido.personas.flatMap((p) => p.lineas).filter((l) => l.estado === 'comandado').length

  return (
    <section className="caja-cobrar">
      <header className="caja-cobrar-cabecera">
        <Boton variante="secundario" onClick={onVolver}>
          <ArrowLeft size={16} aria-hidden="true" /> Caja
        </Boton>
        <div>
          <h2>Cobrar pedido {pedido.numero}</h2>
          <span className="cta-nota">
            {porCobrar} {porCobrar === 1 ? 'producto por cobrar' : 'productos por cobrar'}
          </span>
        </div>
        {porCobrar > 0 && (
          <Boton variante="texto" onClick={marcarTodo}>
            Marcar todo
          </Boton>
        )}
      </header>

      {aviso && <Aviso tono="error">{aviso}</Aviso>}
      {cerrado && <Aviso>Este pedido ya está cerrado.</Aviso>}

      <div className="caja-cobrar-cuerpo">
        <div className="caja-personas">
          {pedido.personas.map((persona) => (
            <PersonaACobrar
              key={persona.id}
              persona={persona}
              marcadas={marcadas}
              descuentos={descuentos}
              puedeDescontar={puedeDescontar && motivos.length > 0}
              onMarcar={marcar}
              onDescuento={setEditando}
            />
          ))}
        </div>

        <aside className="caja-pago" aria-label="Pago">
          <dl className="caja-totales">
            {cuenta.descuentoTotal > 0 && (
              <div>
                <dt>Descuentos</dt>
                <dd>− {enSoles(cuenta.descuentoTotal)}</dd>
              </div>
            )}
            {cuenta.cuentas > 0 && (
              <div>
                <dt>A cuenta de pensionistas</dt>
                <dd>{enSoles(cuenta.cuentas)}</dd>
              </div>
            )}
            <div className="caja-total-grande">
              <dt>A pagar en medios</dt>
              <dd aria-label="Total a pagar">{enSoles(cuenta.medios)}</dd>
            </div>
          </dl>

          {cuenta.medios > 0 && (
            <>
              <div className="caja-medios">
                {MEDIOS.map(({ medio, titulo }) => (
                  <div key={medio} className="caja-medio">
                    <Campo
                      etiqueta={medio === 'efectivo' ? 'Efectivo recibido (S/)' : `${titulo} (S/)`}
                      inputMode="decimal"
                      placeholder="0.00"
                      value={pagos[medio]}
                      error={Number.isNaN(soles(pagos[medio])) ? 'Monto no válido' : undefined}
                      onChange={(e) => setPagos((p) => ({ ...p, [medio]: e.target.value }))}
                    />
                    {medio !== 'efectivo' && pagos[medio] !== '' && (
                      <Campo
                        etiqueta="N.º de operación (opcional)"
                        value={referencias[medio] ?? ''}
                        maxLength={50}
                        onChange={(e) => setReferencias((r) => ({ ...r, [medio]: e.target.value }))}
                      />
                    )}
                  </div>
                ))}
              </div>
              <Boton
                variante="secundario"
                onClick={() => setPagos({ efectivo: (cuenta.medios / 100).toFixed(2), yape: '', plin: '', tarjeta: '' })}
              >
                Exacto en efectivo
              </Boton>
              <p className={`caja-diferencia ${diferencia < 0 ? 'caja-falta' : ''}`} role="status">
                {diferencia < 0
                  ? `Falta ${enSoles(-diferencia)}`
                  : digital > cuenta.medios
                    ? 'Lo digital no puede pasar del total'
                    : diferencia > 0
                      ? `Vuelto ${enSoles(diferencia)}`
                      : 'Pago exacto'}
              </p>
            </>
          )}
          {cuenta.medios === 0 && cuenta.cuentas > 0 && (
            <Aviso>Todo lo marcado se carga a la cuenta del pensionista: no hay que cobrar en medios.</Aviso>
          )}

          <Boton className="caja-cobrar-boton" tamano="tactil" cargando={trabajando} disabled={!puedeCobrar} onClick={cobrar}>
            Cobrar
          </Boton>
        </aside>
      </div>

      {editando && (
        <DialogoDescuento
          linea={editando}
          motivos={motivos}
          actual={descuentos[editando.id]}
          onCerrar={() => setEditando(null)}
          onGuardar={(d) => {
            setDescuentos((ds) => {
              const copia = { ...ds }
              if (d) copia[editando.id] = d
              else delete copia[editando.id]
              return copia
            })
            setEditando(null)
          }}
        />
      )}
    </section>
  )
}

function PersonaACobrar({
  persona,
  marcadas,
  descuentos,
  puedeDescontar,
  onMarcar,
  onDescuento,
}: {
  persona: PersonaDelPedido
  marcadas: string[]
  descuentos: Record<string, DescuentoACobrar>
  puedeDescontar: boolean
  onMarcar: (l: LineaDelPedido, marcada: boolean) => void
  onDescuento: (l: LineaDelPedido) => void
}) {
  const visibles = persona.lineas.filter((l) => l.estado !== 'pendiente')
  if (visibles.length === 0) return null
  return (
    <section className="caja-persona">
      <h3>
        {persona.etiqueta} {persona.tipoCliente === 'pensionista' && <Insignia tono="activo">Pensionista · va a su cuenta</Insignia>}
      </h3>
      <ul>
        {visibles.map((l) => {
          const cobrable = l.estado === 'comandado'
          const bruto = brutoDe(l)
          const d = Math.min(bruto, descuentoDe(bruto, descuentos[l.id]))
          return (
            <li key={l.id} className={cobrable ? '' : 'caja-linea-cerrada'}>
              <label className="caja-linea">
                <input
                  type="checkbox"
                  disabled={!cobrable}
                  checked={marcadas.includes(l.id)}
                  aria-label={`Cobrar ${nombreDe(l)}`}
                  onChange={(e) => onMarcar(l, e.target.checked)}
                />
                <span className="caja-linea-nombre">
                  <strong>
                    {l.cantidad} × {nombreDe(l)}
                  </strong>
                  {l.estado === 'cobrado' && <Insignia tono="activo">Cobrado</Insignia>}
                  {l.estado === 'anulado' && <Insignia tono="inactivo">Anulado</Insignia>}
                </span>
                <span className="caja-linea-precio">
                  {d > 0 ? (
                    <>
                      <s>{enSoles(bruto)}</s> {enSoles(bruto - d)}
                    </>
                  ) : (
                    enSoles(bruto)
                  )}
                </span>
              </label>
              {cobrable && puedeDescontar && marcadas.includes(l.id) && (
                <button type="button" className="caja-enlace" onClick={() => onDescuento(l)}>
                  <Percent size={14} aria-hidden="true" /> {descuentos[l.id] ? 'Cambiar descuento' : 'Descuento'}
                </button>
              )}
            </li>
          )
        })}
      </ul>
    </section>
  )
}

function DialogoDescuento({
  linea,
  motivos,
  actual,
  onGuardar,
  onCerrar,
}: {
  linea: LineaDelPedido
  motivos: Motivo[]
  actual: DescuentoACobrar | undefined
  /** null = quitar el descuento de esta linea */
  onGuardar: (descuento: DescuentoACobrar | null) => void
  onCerrar: () => void
}) {
  const [tipo, setTipo] = useState<'monto' | 'porcentaje'>(actual?.tipo ?? 'porcentaje')
  const [valor, setValor] = useState(actual ? (actual.tipo === 'monto' ? (actual.valor / 100).toFixed(2) : String(actual.valor)) : '')
  const [motivoId, setMotivoId] = useState(actual?.motivoId ?? motivos[0]?.id ?? '')
  const [observacion, setObservacion] = useState(actual?.observacion ?? '')

  const bruto = brutoDe(linea)
  const numero = Number(valor.replace(',', '.'))
  const centimos = tipo === 'monto' ? Math.round(numero * 100) : Math.round(numero)
  const descuento = descuentoDe(bruto, { lineaId: linea.id, tipo, valor: centimos, motivoId, observacion })
  const motivo = motivos.find((m) => m.id === motivoId)
  const valorValido = valor.trim() !== '' && Number.isFinite(numero) && centimos > 0 && (tipo === 'monto' ? centimos <= bruto : centimos <= 100)
  const faltaNota = Boolean(motivo?.requiereObservacion) && observacion.trim() === ''

  return (
    <Modal
      abierto
      titulo={`Descuento a ${nombreDe(linea)}`}
      onCerrar={onCerrar}
      pie={
        <>
          {actual && (
            <Boton variante="texto" onClick={() => onGuardar(null)}>
              Quitar descuento
            </Boton>
          )}
          <Boton variante="secundario" onClick={onCerrar}>
            Cancelar
          </Boton>
          <Boton
            disabled={!valorValido || !motivoId || faltaNota}
            onClick={() =>
              onGuardar({ lineaId: linea.id, tipo, valor: centimos, motivoId, observacion: observacion.trim() || null })
            }
          >
            Aplicar
          </Boton>
        </>
      }
    >
      <div className="ped-dialogo">
        <Select etiqueta="Tipo" value={tipo} onChange={(e) => setTipo(e.target.value as 'monto' | 'porcentaje')}>
          <option value="porcentaje">Porcentaje (%)</option>
          <option value="monto">Monto (S/)</option>
        </Select>
        <Campo
          etiqueta={tipo === 'monto' ? 'Monto a descontar (S/)' : 'Porcentaje (1 a 100)'}
          inputMode="decimal"
          value={valor}
          onChange={(e) => setValor(e.target.value)}
        />
        <Select etiqueta="Descuento habilitado (motivo)" value={motivoId} onChange={(e) => setMotivoId(e.target.value)}>
          {motivos.map((m) => (
            <option key={m.id} value={m.id}>
              {m.descripcion}
            </option>
          ))}
        </Select>
        <Campo
          etiqueta={motivo?.requiereObservacion ? 'Observación (obligatoria)' : 'Observación (opcional)'}
          value={observacion}
          maxLength={250}
          onChange={(e) => setObservacion(e.target.value)}
        />
        {valorValido && (
          <p className="cta-nota">
            Se descuentan {enSoles(descuento)}: la línea queda en {enSoles(bruto - descuento)}.
          </p>
        )}
      </div>
    </Modal>
  )
}
