import { useEffect, useState } from 'react'
import { ClipboardList, CookingPot, Printer } from 'lucide-react'
import { Aviso, Boton, EstadoCargando, EstadoError, Modal } from '../../comun/componentes'
import { obtenerComanda, registrarImpresionDeComanda, type ComandaDetalle } from '../../api/cliente'
import { mensajeDePedido } from './mensajes'
import { siglaDePersona } from './OrdenDeLaPersona'

const SERVICIO: Record<string, string> = { desayuno: 'Desayuno', almuerzo: 'Almuerzo', cena: 'Cena' }
/** Cómo se llama cada parte del menú en la comanda (el refresco, para cocina, es la bebida). */
const PARTE: Record<string, string> = {
  entrada: 'Entrada',
  segundo: 'Segundo',
  guarnicion: 'Guarnición',
  refresco: 'Bebida',
  postre: 'Postre',
}

const numeroCorto = (n: number | string) => String(n).padStart(3, '0')
const mesaCorta = (mesa: string) => (/^\d$/.test(mesa) ? `0${mesa}` : mesa)
const plural = (n: number, uno: string, varios: string) => `${n} ${n === 1 ? uno : varios}`

/**
 * US-030: la comanda para cocina, lista para imprimir desde el navegador (funciona con cualquier impresora y sin
 * internet). "Imprimir" primero REGISTRA la impresion: si el servidor dice que es la segunda o posterior, la hoja
 * sale con la leyenda COPIA (RN-063). El estilo de impresion deja solo la hoja (ver estilos/global.css).
 * Sin estado "listo": cocina avisa de palabra (RN-064). Diseño 2026-10-29: la mesa destacada, los datos del envío,
 * cada persona con sus productos y, en un menú, lo que es cada opción (entrada, segundo, bebida, postre).
 */
export function VistaComanda({ token, comandaId, onCerrar }: { token: string; comandaId: string; onCerrar: () => void }) {
  const [comanda, setComanda] = useState<ComandaDetalle | null>(null)
  const [error, setError] = useState('')
  const [copia, setCopia] = useState(false)
  const [imprimiendo, setImprimiendo] = useState(false)

  useEffect(() => {
    setComanda(null)
    setError('')
    setCopia(false)
    obtenerComanda(token, comandaId)
      .then((c) => {
        setComanda(c)
        // Si ya se imprimio antes, cualquier hoja que salga de aqui es una copia.
        setCopia(c.impresiones >= 1)
      })
      .catch((e) => setError(mensajeDePedido(e, 'No se pudo cargar la comanda.')))
  }, [token, comandaId])

  async function imprimir() {
    if (imprimiendo) return
    setImprimiendo(true)
    setError('')
    try {
      const registro = await registrarImpresionDeComanda(token, comandaId)
      // La leyenda tiene que estar en la hoja antes de abrir el dialogo de impresion.
      setCopia(registro.copia)
      setComanda((c) => (c ? { ...c, impresiones: registro.impresiones } : c))
      window.setTimeout(() => window.print(), 100)
    } catch (e) {
      setError(mensajeDePedido(e, 'No se pudo registrar la impresión. Inténtalo de nuevo.'))
    } finally {
      setImprimiendo(false)
    }
  }

  const emitida = comanda ? new Date(comanda.emitidaEn) : null
  const productos = comanda?.personas.reduce((n, p) => n + p.lineas.reduce((m, l) => m + l.cantidad, 0), 0) ?? 0

  return (
    <Modal
      abierto
      ancho="amplio"
      titulo="Detalle de comanda"
      subtitulo={comanda ? `Comanda #${numeroCorto(comanda.numero)} · Pedido #${comanda.pedidoNumero}` : undefined}
      icono={<ClipboardList size={24} strokeWidth={1.9} />}
      onCerrar={onCerrar}
      pie={
        <>
          {comanda && (
            <div className="com2-estado">
              <span className="com2-estado-icono" aria-hidden="true">
                <Printer size={20} />
              </span>
              <div>
                <strong className={comanda.impresiones === 0 ? 'pendiente' : 'impresa'}>
                  <i aria-hidden="true" />
                  {comanda.impresiones === 0
                    ? 'Pendiente de impresión'
                    : `Impresa ${plural(comanda.impresiones, 'vez', 'veces')}`}
                </strong>
                <span>La comanda ya fue enviada a cocina.</span>
              </div>
            </div>
          )}
          <Boton variante="secundario" onClick={onCerrar}>
            Cerrar
          </Boton>
          <Boton cargando={imprimiendo} disabled={!comanda} onClick={imprimir}>
            <Printer size={18} aria-hidden="true" />
            {comanda && comanda.impresiones > 0 ? 'Imprimir copia' : 'Imprimir comanda'}
          </Boton>
        </>
      }
    >
      {error && <Aviso tono="error">{error}</Aviso>}
      {!comanda && !error && <EstadoCargando />}
      {!comanda && error && <EstadoError mensaje={error} />}
      {comanda && emitida && (
        <article className="com-hoja com2" aria-label={`Comanda ${comanda.numero}`}>
          {copia && <p className="com-copia">COPIA</p>}
          <header className="com2-mesa">
            <strong>{comanda.mesa ? `Mesa ${mesaCorta(comanda.mesa)}` : 'Para llevar'}</strong>
            <span className="com2-en-cocina">
              <CookingPot size={18} aria-hidden="true" />
              En cocina
            </span>
          </header>

          <dl className="com2-datos">
            <div>
              <dt>Servicio</dt>
              <dd>{SERVICIO[comanda.servicio]}</dd>
            </div>
            <div>
              <dt>Atendido por</dt>
              <dd>{comanda.mozo ?? '—'}</dd>
            </div>
            <div>
              <dt>Fecha</dt>
              <dd>{emitida.toLocaleDateString('es-PE', { day: 'numeric', month: 'short', year: 'numeric' }).replace('.', '')}</dd>
            </div>
            <div>
              <dt>Hora de envío</dt>
              <dd>{emitida.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' })}</dd>
            </div>
          </dl>

          {comanda.personas.map((p) => {
            const suyos = p.lineas.reduce((m, l) => m + l.cantidad, 0)
            return (
              <section key={p.etiqueta} className="com2-persona" aria-label={p.etiqueta}>
                <header>
                  <span className="com2-sigla" aria-hidden="true">
                    {siglaDePersona(p.etiqueta)}
                  </span>
                  <h3>{p.etiqueta}</h3>
                  <span className="com2-cuenta">{plural(suyos, 'producto', 'productos')}</span>
                </header>
                {p.lineas.map((l, i) => {
                  const partes = l.partes && l.partes.length > 0 ? l.partes : null
                  return (
                    <div key={i} className="com2-linea">
                      <div className="com2-linea-cabeza">
                        <span className="com2-cantidad">{l.cantidad}×</span>
                        <div>
                          <strong>{l.nombre}</strong>
                          {partes && <span>Menú completo</span>}
                          {!partes && l.componentes.length > 0 && <span>{l.componentes.join(' · ')}</span>}
                        </div>
                      </div>
                      {partes && (
                        <dl className="com2-partes">
                          {partes.map((c) => (
                            <div key={c.componente}>
                              <dt>{PARTE[c.componente] ?? c.componente}</dt>
                              <dd>{c.productoNombre ?? '—'}</dd>
                            </div>
                          ))}
                        </dl>
                      )}
                      {l.observacion && <p className="com-nota com2-nota">Nota: {l.observacion}</p>}
                    </div>
                  )
                })}
              </section>
            )
          })}

          <p className="com2-resumen">
            {plural(productos, 'producto', 'productos')} · {plural(comanda.personas.length, 'comensal', 'comensales')}
          </p>
        </article>
      )}
    </Modal>
  )
}
