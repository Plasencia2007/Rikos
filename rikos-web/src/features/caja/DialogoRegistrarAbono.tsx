import { useEffect, useMemo, useState } from 'react'
import {
  Aviso,
  Boton,
  Campo,
  EstadoCargando,
  EstadoVacio,
  Insignia,
  Modal,
} from '../../comun/componentes'
import { enSoles } from '../../comun/formato/dinero'
import {
  listarCuentasCorrientes,
  registrarAbonoEnCaja,
  type AbonoRegistrado,
  type FilaDeCuentaCorriente,
  type SolicitudAbonoEnCaja,
} from '../../api/cliente'
import { mensajeDeCaja, soles } from './mensajes'
import { CheckCircle2, DollarSign, Smartphone, Landmark, AlertCircle, ArrowLeft } from 'lucide-react'

interface Props {
  token: string
  onCerrar: () => void
  onAbonoExitoso: (resultado: AbonoRegistrado) => void
}

type MedioPago = 'efectivo' | 'yape' | 'plin' | 'transferencia'

const MEDIOS: { id: MedioPago; etiqueta: string; icono: typeof DollarSign }[] = [
  { id: 'efectivo', etiqueta: 'Efectivo', icono: DollarSign },
  { id: 'yape', etiqueta: 'Yape', icono: Smartphone },
  { id: 'plin', etiqueta: 'Plin', icono: Smartphone },
  { id: 'transferencia', etiqueta: 'Transferencia', icono: Landmark },
]

const CUENTAS_SUGERIDAS: Record<MedioPago, string[]> = {
  efectivo: [],
  yape: ["Yape RIKO'S 987-654-321", "Yape Personal Administrador"],
  plin: ["Plin RIKO'S 987-654-321"],
  transferencia: ['BCP Cta Cte 191-00000000-0-00', 'BBVA Cta Cte 0011-0000-0000000000', 'Interbank Cta Cte'],
}

/**
 * US-021: Registrar abonos del pensionista en caja.
 * Permite buscar pensionistas, ver su saldo/deuda actual,
 * elegir medio (efectivo o digital), ingresar monto y número de operación.
 */
export function DialogoRegistrarAbono({ token, onCerrar, onAbonoExitoso }: Props) {
  const [cuentas, setCuentas] = useState<FilaDeCuentaCorriente[] | null>(null)
  const [cargando, setCargando] = useState(true)
  const [filtroTexto, setFiltroTexto] = useState('')
  const [seleccionado, setSeleccionado] = useState<FilaDeCuentaCorriente | null>(null)

  const [medio, setMedio] = useState<MedioPago>('efectivo')
  const [montoTexto, setMontoTexto] = useState('')
  const [numeroOperacion, setNumeroOperacion] = useState('')
  const [cuentaDestino, setCuentaDestino] = useState('')
  const [observacion, setObservacion] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [errorAviso, setErrorAviso] = useState('')

  useEffect(() => {
    setCargando(true)
    listarCuentasCorrientes(token)
      .then((res) => {
        setCuentas(res)
        setCargando(false)
      })
      .catch((err) => {
        setErrorAviso(mensajeDeCaja(err, 'No se pudo cargar la lista de pensionistas.'))
        setCargando(false)
      })
  }, [token])

  // Filtrado de pensionistas
  const pensionistasFiltrados = useMemo(() => {
    if (!cuentas) return []
    const q = filtroTexto.trim().toLowerCase()
    if (!q) return cuentas.slice(0, 20) // primeros 20 si no busca
    return cuentas.filter((c) =>
      c.dni.toLowerCase().includes(q) ||
      c.nombres.toLowerCase().includes(q) ||
      c.apellidos.toLowerCase().includes(q)
    )
  }, [cuentas, filtroTexto])

  const seleccionarPensionista = (p: FilaDeCuentaCorriente) => {
    setSeleccionado(p)
    setErrorAviso('')
    // Si tiene deuda, sugerir el monto total de la deuda por conveniencia
    if (p.deuda > 0) {
      setMontoTexto((p.deuda / 100).toFixed(2))
    } else {
      setMontoTexto('')
    }
  }

  const centimos = soles(montoTexto)
  const montoValido = centimos !== null && !Number.isNaN(centimos) && centimos > 0
  const esDigital = medio !== 'efectivo'
  const numOpValido = !esDigital || numeroOperacion.trim().length > 0

  const puedeConfirmar = seleccionado !== null && montoValido && numOpValido && !guardando

  const guardarAbono = async () => {
    if (!seleccionado || !montoValido || centimos === null) return
    if (esDigital && !numeroOperacion.trim()) {
      setErrorAviso('Los pagos digitales exigen número de operación.')
      return
    }

    setGuardando(true)
    setErrorAviso('')

    const solicitud: SolicitudAbonoEnCaja = {
      pensionistaId: seleccionado.pensionistaId,
      medio,
      monto: centimos,
      numeroOperacion: esDigital ? numeroOperacion.trim() : null,
      cuentaDestino: esDigital && cuentaDestino.trim() ? cuentaDestino.trim() : null,
      observacion: observacion.trim() || null,
    }

    try {
      const res = await registrarAbonoEnCaja(token, solicitud)
      onAbonoExitoso(res)
    } catch (err) {
      setErrorAviso(mensajeDeCaja(err, 'No se pudo registrar el abono.'))
      setGuardando(false)
    }
  }

  // Pie del modal
  const pie = (
    <div className="flex gap-2 justify-end w-full">
      <Boton variante="secundario" onClick={onCerrar} disabled={guardando}>
        Cancelar
      </Boton>
      {seleccionado && (
        <Boton tamano="tactil" onClick={guardarAbono} disabled={!puedeConfirmar}>
          {guardando ? 'Registrando...' : 'Confirmar abono'}
        </Boton>
      )}
    </div>
  )

  return (
    <Modal abierto titulo="Abono de pensionista en caja" onCerrar={onCerrar} pie={pie}>
      <div className="dialogo-abono-caja space-y-4">
        {errorAviso && <Aviso>{errorAviso}</Aviso>}

        {cargando ? (
          <EstadoCargando />
        ) : !seleccionado ? (
          /* PASO 1: Buscar y seleccionar pensionista */
          <div className="abono-paso-buscar space-y-3">
            <p className="text-sm text-neutral-400">
              Busca al pensionista por su DNI o nombres para registrar el abono a su cuenta corriente.
            </p>
            <Campo
              etiqueta="Buscar por DNI o Nombre"
              value={filtroTexto}
              onChange={(e) => setFiltroTexto(e.target.value)}
              placeholder="Escribe DNI o apellido..."
              autoFocus
            />

            <div className="abono-lista-pensionistas max-h-72 overflow-y-auto space-y-2 border border-neutral-800 rounded-lg p-2">
              {pensionistasFiltrados.length === 0 ? (
                <EstadoVacio
                  titulo="Sin resultados"
                  descripcion="No se encontraron pensionistas que coincidan con la búsqueda."
                />
              ) : (
                pensionistasFiltrados.map((p) => (
                  <button
                    key={p.pensionistaId}
                    type="button"
                    onClick={() => seleccionarPensionista(p)}
                    className="w-full text-left p-3 rounded-lg border border-neutral-800/60 hover:border-amber-500/50 hover:bg-neutral-800/40 transition-colors flex items-center justify-between gap-3 group"
                  >
                    <div>
                      <div className="flex items-center gap-2">
                        <strong className="text-white group-hover:text-amber-400">
                          {p.nombres} {p.apellidos}
                        </strong>
                        {!p.activo && <Insignia tono="inactivo">Inactivo</Insignia>}
                      </div>
                      <span className="text-xs text-neutral-400">
                        DNI: {p.dni} {p.planNombre ? `· Plan: ${p.planNombre}` : ''}
                      </span>
                    </div>
                    <div className="text-right">
                      {p.deuda > 0 ? (
                        <div>
                          <span className="text-xs text-rose-400 block">Deuda</span>
                          <strong className="text-rose-400 text-sm font-semibold">{enSoles(p.deuda)}</strong>
                        </div>
                      ) : p.saldoAFavor > 0 ? (
                        <div>
                          <span className="text-xs text-emerald-400 block">A favor</span>
                          <strong className="text-emerald-400 text-sm font-semibold">{enSoles(p.saldoAFavor)}</strong>
                        </div>
                      ) : (
                        <span className="text-xs text-neutral-400">Al día</span>
                      )}
                    </div>
                  </button>
                ))
              )}
            </div>
          </div>
        ) : (
          /* PASO 2: Datos del abono */
          <div className="abono-paso-datos space-y-4">
            {/* Tarjeta del pensionista seleccionado */}
            <div className="p-3 bg-neutral-900 border border-neutral-800 rounded-xl flex items-center justify-between">
              <div>
                <div className="flex items-center gap-2">
                  <strong className="text-white text-base">
                    {seleccionado.nombres} {seleccionado.apellidos}
                  </strong>
                  {!seleccionado.activo && (
                    <Insignia tono="inactivo">Inactivo (RN-034)</Insignia>
                  )}
                </div>
                <span className="text-xs text-neutral-400">
                  DNI: {seleccionado.dni} {seleccionado.planNombre ? `· ${seleccionado.planNombre}` : ''}
                </span>
                <div className="mt-1 flex gap-3 text-xs">
                  <span>
                    Deuda actual:{' '}
                    <strong className={seleccionado.deuda > 0 ? 'text-rose-400' : 'text-neutral-300'}>
                      {enSoles(seleccionado.deuda)}
                    </strong>
                  </span>
                  {seleccionado.saldoAFavor > 0 && (
                    <span>
                      Saldo a favor:{' '}
                      <strong className="text-emerald-400">{enSoles(seleccionado.saldoAFavor)}</strong>
                    </span>
                  )}
                </div>
              </div>
              <Boton
                variante="secundario"
                onClick={() => setSeleccionado(null)}
                aria-label="Cambiar pensionista"
              >
                <ArrowLeft size={14} className="mr-1" /> Cambiar
              </Boton>
            </div>

            {/* Selector de medio de pago */}
            <div>
              <label className="text-sm font-medium text-neutral-300 block mb-1.5">
                Método de pago
              </label>
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                {MEDIOS.map((m) => {
                  const Icono = m.icono
                  const activo = medio === m.id
                  return (
                    <button
                      key={m.id}
                      type="button"
                      onClick={() => {
                        setMedio(m.id)
                        if (CUENTAS_SUGERIDAS[m.id].length > 0 && !cuentaDestino) {
                          setCuentaDestino(CUENTAS_SUGERIDAS[m.id][0])
                        }
                      }}
                      className={`p-3 rounded-xl border flex flex-col items-center justify-center gap-1.5 transition-all ${
                        activo
                          ? 'border-amber-500 bg-amber-500/10 text-amber-400 font-bold'
                          : 'border-neutral-800 bg-neutral-900/60 text-neutral-300 hover:border-neutral-700'
                      }`}
                    >
                      <Icono size={20} className={activo ? 'text-amber-400' : 'text-neutral-400'} />
                      <span className="text-xs">{m.etiqueta}</span>
                    </button>
                  )
                })}
              </div>
            </div>

            {/* Monto del abono */}
            <div>
              <Campo
                etiqueta="Monto a abonar (S/)"
                value={montoTexto}
                onChange={(e) => setMontoTexto(e.target.value)}
                placeholder="0.00"
                type="number"
                autoFocus
              />
              {/* Botones de sugerencia de montos */}
              <div className="flex flex-wrap gap-1.5 mt-2">
                {seleccionado.deuda > 0 && (
                  <button
                    type="button"
                    onClick={() => setMontoTexto((seleccionado.deuda / 100).toFixed(2))}
                    className="px-2.5 py-1 text-xs rounded-lg border border-rose-500/30 bg-rose-500/10 text-rose-300 hover:bg-rose-500/20"
                  >
                    Toda la deuda ({enSoles(seleccionado.deuda)})
                  </button>
                )}
                {['10.00', '20.00', '50.00', '100.00'].map((val) => (
                  <button
                    key={val}
                    type="button"
                    onClick={() => setMontoTexto(val)}
                    className="px-2.5 py-1 text-xs rounded-lg border border-neutral-700 bg-neutral-800 text-neutral-300 hover:bg-neutral-700"
                  >
                    S/ {val}
                  </button>
                ))}
              </div>
            </div>

            {/* Campos condicionales para pagos digitales */}
            {esDigital && (
              <div className="space-y-3 p-3 bg-neutral-950/70 border border-neutral-800/80 rounded-xl">
                <Campo
                  etiqueta="Número de Operación / Referencia *"
                  value={numeroOperacion}
                  onChange={(e) => setNumeroOperacion(e.target.value)}
                  placeholder="Ej. 1829304"
                />

                <div>
                  <label className="text-xs font-medium text-neutral-300 block mb-1">
                    Cuenta destino / Billetera
                  </label>
                  <input
                    type="text"
                    value={cuentaDestino}
                    onChange={(e) => setCuentaDestino(e.target.value)}
                    placeholder="Ej. Yape RIKO'S o BCP Cta Cte..."
                    className="w-full bg-neutral-900 border border-neutral-700 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-amber-500"
                  />
                  {CUENTAS_SUGERIDAS[medio].length > 0 && (
                    <div className="flex flex-wrap gap-1 mt-1.5">
                      {CUENTAS_SUGERIDAS[medio].map((cta) => (
                        <button
                          key={cta}
                          type="button"
                          onClick={() => setCuentaDestino(cta)}
                          className="text-[11px] px-2 py-0.5 rounded bg-neutral-800 text-neutral-400 hover:text-white"
                        >
                          {cta}
                        </button>
                      ))}
                    </div>
                  )}
                </div>

                <div className="flex items-start gap-2 p-2.5 bg-amber-500/10 border border-amber-500/20 rounded-lg text-amber-300 text-xs">
                  <AlertCircle size={16} className="shrink-0 mt-0.5" />
                  <span>
                    <strong>RN-031:</strong> Los abonos digitales quedan en estado <em>Pendiente</em> para verificación administrativa (US-024). No afectan el efectivo en caja del turno.
                  </span>
                </div>
              </div>
            )}

            {!esDigital && (
              <div className="flex items-start gap-2 p-2.5 bg-emerald-500/10 border border-emerald-500/20 rounded-lg text-emerald-300 text-xs">
                <CheckCircle2 size={16} className="shrink-0 mt-0.5" />
                <span>
                  <strong>RN-030:</strong> El abono en efectivo se confirma al instante, entra en la conciliación del turno como <em>abono_pensionista</em> e impacta el saldo del pensionista.
                </span>
              </div>
            )}

            <Campo
              etiqueta="Observación (opcional)"
              value={observacion}
              onChange={(e) => setObservacion(e.target.value)}
              placeholder="Detalle o nota interna..."
            />
          </div>
        )}
      </div>
    </Modal>
  )
}
