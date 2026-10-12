import { Printer, CheckCircle2, AlertTriangle, Clock } from 'lucide-react'
import { Boton, Modal } from '../../comun/componentes'
import { enSoles } from '../../comun/formato/dinero'
import type { AbonoRegistrado } from '../../api/cliente'

interface Props {
  resultado: AbonoRegistrado
  onCerrar: () => void
}

/**
 * US-021: Constancia interna de abono a cuenta corriente.
 * RN-035: No constituye comprobante de pago tributario.
 * Formato tipo ticket térmico imprimible (window.print()).
 */
export function ConstanciaAbonoModal({ resultado, onCerrar }: Props) {
  const { abono, pensionistaNombre, pensionistaDni, saldoAnterior, nuevoSaldo, saldoAFavor, alertaDuplicado, constancia } = resultado
  const esEfectivo = abono.medio.toLowerCase() === 'efectivo'
  const esAprobado = abono.estado === 'aprobado'

  const imprimir = () => {
    window.print()
  }

  const pie = (
    <div className="flex gap-2 justify-end w-full">
      <Boton variante="secundario" onClick={imprimir}>
        <Printer size={16} aria-hidden="true" /> Imprimir constancia
      </Boton>
      <Boton onClick={onCerrar}>Listo</Boton>
    </div>
  )

  return (
    <Modal abierto titulo="Constancia de Abono" onCerrar={onCerrar} pie={pie}>
      <div className="constancia-ticket-contenedor">
        {alertaDuplicado && (
          <div className="aviso-duplicado mb-3 flex items-center gap-2 p-3 bg-amber-500/10 border border-amber-500/30 rounded-lg text-amber-400 text-sm">
            <AlertTriangle size={18} className="shrink-0 text-amber-400" />
            <div>
              <strong>Alerta de verificación:</strong> El número de operación ({abono.numeroOperacion}) ya fue registrado en otro abono con este medio de pago.
            </div>
          </div>
        )}

        <div className="constancia-ticket" id="seccion-constancia-imprimible">
          <div className="constancia-encabezado">
            <h3 className="constancia-titulo">{constancia.localNombre}</h3>
            <p className="constancia-subtitulo">CONSTANCIA INTERNA DE ABONO</p>
            <p className="constancia-codigo">N° {abono.id.substring(0, 12).toUpperCase()}</p>
          </div>

          <div className="constancia-separador" />

          <dl className="constancia-datos">
            <div className="constancia-fila">
              <dt>Fecha / Hora:</dt>
              <dd>
                {(() => {
                  try {
                    const d = new Date(constancia.emitidoEn)
                    return Number.isNaN(d.getTime()) ? new Date().toLocaleString('es-PE') : d.toLocaleString('es-PE')
                  } catch {
                    return new Date().toLocaleString('es-PE')
                  }
                })()}
              </dd>
            </div>
            <div className="constancia-fila">
              <dt>Cajero:</dt>
              <dd>{constancia.cajeroNombre}</dd>
            </div>
            <div className="constancia-fila">
              <dt>Pensionista:</dt>
              <dd className="font-bold">{pensionistaNombre}</dd>
            </div>
            <div className="constancia-fila">
              <dt>DNI:</dt>
              <dd>{pensionistaDni}</dd>
            </div>
            <div className="constancia-fila">
              <dt>Método de pago:</dt>
              <dd className="capitalize font-semibold">{abono.medio}</dd>
            </div>
            {abono.numeroOperacion && (
              <div className="constancia-fila">
                <dt>N° Operación:</dt>
                <dd className="font-mono">{abono.numeroOperacion}</dd>
              </div>
            )}
            {abono.cuentaDestino && (
              <div className="constancia-fila">
                <dt>Cuenta destino:</dt>
                <dd>{abono.cuentaDestino}</dd>
              </div>
            )}
            <div className="constancia-fila">
              <dt>Estado:</dt>
              <dd>
                {esAprobado ? (
                  <span className="badge-aprobado inline-flex items-center gap-1 text-emerald-400 font-bold">
                    <CheckCircle2 size={14} /> Confirmado
                  </span>
                ) : (
                  <span className="badge-pendiente inline-flex items-center gap-1 text-amber-400 font-bold">
                    <Clock size={14} /> Pendiente revisión
                  </span>
                )}
              </dd>
            </div>
          </dl>

          <div className="constancia-separador" />

          <div className="constancia-monto-bloque">
            <span className="constancia-monto-label">MONTO ABONADO</span>
            <span className="constancia-monto-valor">{enSoles(abono.monto)}</span>
          </div>

          <div className="constancia-separador" />

          <div className="constancia-saldos">
            <div className="constancia-fila">
              <dt>Saldo anterior (deuda):</dt>
              <dd>{enSoles(saldoAnterior)}</dd>
            </div>
            <div className="constancia-fila">
              <dt>{esEfectivo ? 'Nuevo saldo (deuda):' : 'Saldo actual (sin verificar):'}</dt>
              <dd className="font-bold">{enSoles(nuevoSaldo)}</dd>
            </div>
            {saldoAFavor > 0 && (
              <div className="constancia-fila text-emerald-400">
                <dt>Saldo a favor:</dt>
                <dd className="font-bold">{enSoles(saldoAFavor)}</dd>
              </div>
            )}
          </div>

          {abono.observacion && (
            <>
              <div className="constancia-separador" />
              <p className="constancia-observacion">
                <strong>Nota:</strong> {abono.observacion}
              </p>
            </>
          )}

          <div className="constancia-separador" />

          <footer className="constancia-pie">
            <p className="constancia-leyenda">{constancia.leyenda}</p>
            <p className="constancia-gracias">¡Gracias por su preferencia!</p>
          </footer>
        </div>
      </div>
    </Modal>
  )
}
