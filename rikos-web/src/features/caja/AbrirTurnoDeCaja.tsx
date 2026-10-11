import { useEffect, useState } from 'react'
import { Wallet } from 'lucide-react'
import { Aviso, Boton, Campo, EstadoCargando, EstadoError, Select } from '../../comun/componentes'
import { abrirTurnoDeCaja, listarCajas, type CajaDelLocal, type TurnoDeCaja } from '../../api/cliente'
import { mensajeDeCaja, soles } from './mensajes'

/**
 * US-032: abrir el turno. El fondo inicial siempre se escribe (puede ser cero): no hay un fondo por defecto. Solo se ofrecen las
 * cajas del local que no tienen un turno abierto; al abrir, la caja se rotula "Caja de <cajero>".
 */
export function AbrirTurnoDeCaja({ token, onAbierto }: { token: string; onAbierto: (turno: TurnoDeCaja) => void }) {
  const [cajas, setCajas] = useState<CajaDelLocal[] | null>(null)
  const [error, setError] = useState(false)
  const [cajaId, setCajaId] = useState('')
  const [fondo, setFondo] = useState('')
  const [trabajando, setTrabajando] = useState(false)
  const [aviso, setAviso] = useState('')

  useEffect(() => {
    listarCajas(token)
      .then((lista) => {
        const libres = lista.filter((c) => c.activo && !c.turnoAbierto)
        setCajas(lista)
        if (libres.length > 0) setCajaId(libres[0].id)
      })
      .catch(() => setError(true))
  }, [token])

  if (error) return <EstadoError mensaje="No se pudieron cargar las cajas." />
  if (!cajas) return <EstadoCargando />

  const libres = cajas.filter((c) => c.activo && !c.turnoAbierto)
  const centimos = soles(fondo)
  const fondoValido = centimos !== null && !Number.isNaN(centimos)

  async function abrir() {
    if (!fondoValido || centimos === null) return
    setTrabajando(true)
    setAviso('')
    try {
      onAbierto(await abrirTurnoDeCaja(token, cajaId, centimos))
    } catch (e) {
      setAviso(mensajeDeCaja(e, 'No se pudo abrir el turno. Inténtalo de nuevo.'))
    } finally {
      setTrabajando(false)
    }
  }

  return (
    <section className="caja-abrir">
      <div className="caja-abrir-tarjeta">
        <span className="caja-abrir-icono" aria-hidden="true">
          <Wallet size={30} />
        </span>
        <h2>Abrir turno de caja</h2>
        <p className="cta-nota">Cuenta el efectivo con el que empiezas e ingrésalo como fondo inicial.</p>
        {aviso && <Aviso tono="error">{aviso}</Aviso>}
        {libres.length === 0 ? (
          <Aviso>Todas las cajas de este local ya tienen un turno abierto. Pídele al administrador que cree otra caja.</Aviso>
        ) : (
          <>
            {libres.length > 1 && (
              <Select etiqueta="Caja" value={cajaId} onChange={(e) => setCajaId(e.target.value)}>
                {libres.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.nombre}
                  </option>
                ))}
              </Select>
            )}
            <Campo
              etiqueta="Fondo inicial (S/)"
              inputMode="decimal"
              placeholder="0.00"
              value={fondo}
              error={fondo !== '' && !fondoValido ? 'Escribe un monto válido, por ejemplo 100 o 100.50.' : undefined}
              onChange={(e) => setFondo(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && abrir()}
            />
            <Boton tamano="tactil" cargando={trabajando} disabled={!fondoValido} onClick={abrir}>
              Abrir turno
            </Boton>
          </>
        )}
      </div>
    </section>
  )
}
