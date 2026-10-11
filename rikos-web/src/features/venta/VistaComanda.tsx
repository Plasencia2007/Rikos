import { useEffect, useState } from 'react'
import { Aviso, Boton, EstadoCargando, EstadoError, Modal } from '../../comun/componentes'
import { obtenerComanda, registrarImpresionDeComanda, type ComandaDetalle } from '../../api/cliente'
import { mensajeDePedido } from './mensajes'

const SERVICIO: Record<string, string> = { desayuno: 'Desayuno', almuerzo: 'Almuerzo', cena: 'Cena' }

function hora(iso: string): string {
  return new Date(iso).toLocaleString('es-PE', { dateStyle: 'short', timeStyle: 'short' })
}

/**
 * US-030: la comanda para cocina, lista para imprimir desde el navegador (funciona con cualquier impresora y sin
 * internet). "Imprimir" primero REGISTRA la impresion: si el servidor dice que es la segunda o posterior, la hoja
 * sale con la leyenda COPIA (RN-063). El estilo de impresion deja solo la hoja (ver estilos/global.css).
 * Sin estado "listo": cocina avisa de palabra (RN-064).
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

  return (
    <Modal
      abierto
      titulo={comanda ? `Comanda ${comanda.numero}` : 'Comanda'}
      onCerrar={onCerrar}
      pie={
        <>
          <Boton variante="secundario" onClick={onCerrar}>
            Cerrar
          </Boton>
          <Boton cargando={imprimiendo} disabled={!comanda} onClick={imprimir}>
            Imprimir
          </Boton>
        </>
      }
    >
      {error && <Aviso tono="error">{error}</Aviso>}
      {!comanda && !error && <EstadoCargando />}
      {!comanda && error && <EstadoError mensaje={error} />}
      {comanda && (
        <article className="com-hoja" aria-label={`Comanda ${comanda.numero}`}>
          {copia && <p className="com-copia">COPIA</p>}
          <header className="com-cabecera">
            <strong className="com-mesa">{comanda.mesa ? `Mesa ${comanda.mesa}` : 'Para llevar'}</strong>
            <span>
              Pedido {comanda.pedidoNumero} · Comanda {comanda.numero}
            </span>
            <span>
              {SERVICIO[comanda.servicio]} · {hora(comanda.emitidaEn)}
              {comanda.mozo ? ` · ${comanda.mozo}` : ''}
            </span>
          </header>
          {comanda.personas.map((p) => (
            <section key={p.etiqueta} className="com-persona">
              <h3>{p.etiqueta}</h3>
              <ul>
                {p.lineas.map((l, i) => (
                  <li key={i}>
                    <strong>
                      {l.cantidad} × {l.nombre}
                    </strong>
                    {l.componentes.length > 0 && <span>{l.componentes.join(' · ')}</span>}
                    {l.observacion && <span className="com-nota">Nota: {l.observacion}</span>}
                  </li>
                ))}
              </ul>
            </section>
          ))}
        </article>
      )}
    </Modal>
  )
}
