import { useEffect, useState } from 'react'
import { Wallet } from 'lucide-react'
import { Aviso, Boton, Campo, EstadoCargando, EstadoError, Modal, Select } from '../../comun/componentes'
import { abrirTurnoDeCaja, listarCajas, type CajaDelLocal, type TurnoDeCaja } from '../../api/cliente'
import { mensajeDeCaja, soles } from './mensajes'

/**
 * US-032: la caja cerrada y el cuadro "Abrir sesión de caja". El cajero elige entre las cajas REGISTRADAS en Configuración (la principal
 * viene elegida), escribe el fondo inicial (siempre se escribe, puede ser cero) y una nota opcional. Solo se ofrecen las cajas activas y
 * libres. Al abrir, la caja se rotula "Caja de <cajero>".
 */
export function AbrirTurnoDeCaja({ token, onAbierto }: { token: string; onAbierto: (turno: TurnoDeCaja) => void }) {
  const [cajas, setCajas] = useState<CajaDelLocal[] | null>(null)
  const [error, setError] = useState(false)
  const [abierto, setAbierto] = useState(false)

  useEffect(() => {
    listarCajas(token)
      .then(setCajas)
      .catch(() => setError(true))
  }, [token])

  if (error) return <EstadoError mensaje="No se pudieron cargar las cajas." />
  if (!cajas) return <EstadoCargando />

  const libres = cajas.filter((c) => c.activo && !c.turnoAbierto)

  return (
    <section className="caja-abrir">
      <div className="caja-abrir-tarjeta">
        <span className="caja-abrir-icono" aria-hidden="true">
          <Wallet size={30} />
        </span>
        <h2>Caja cerrada</h2>
        <p className="cta-nota">No tienes una sesión de caja activa. Abre una para empezar a cobrar.</p>
        {libres.length === 0 ? (
          <Aviso>
            Todas las cajas de este local están ocupadas o desactivadas. Un administrador puede registrar otra en Configuración → Cajas
            registradoras.
          </Aviso>
        ) : (
          <Boton tamano="tactil" onClick={() => setAbierto(true)}>
            Abrir sesión
          </Boton>
        )}
        <p className="cta-nota">La sesión controla el efectivo durante tu turno y se cierra con el conteo al final.</p>
      </div>
      {abierto && <DialogoAbrirSesion token={token} libres={libres} onCerrar={() => setAbierto(false)} onAbierto={onAbierto} />}
    </section>
  )
}

function DialogoAbrirSesion({
  token,
  libres,
  onCerrar,
  onAbierto,
}: {
  token: string
  libres: CajaDelLocal[]
  onCerrar: () => void
  onAbierto: (turno: TurnoDeCaja) => void
}) {
  // La principal viene elegida si esta libre; si no, la primera libre.
  const [cajaId, setCajaId] = useState((libres.find((c) => c.principal) ?? libres[0]).id)
  const [fondo, setFondo] = useState('')
  const [nota, setNota] = useState('')
  const [trabajando, setTrabajando] = useState(false)
  const [aviso, setAviso] = useState('')

  const centimos = soles(fondo)
  const fondoValido = centimos !== null && !Number.isNaN(centimos)

  async function abrir() {
    if (!fondoValido || centimos === null) return
    setTrabajando(true)
    setAviso('')
    try {
      onAbierto(await abrirTurnoDeCaja(token, cajaId, centimos, nota.trim() || null))
    } catch (e) {
      setAviso(mensajeDeCaja(e, 'No se pudo abrir la sesión. Inténtalo de nuevo.'))
    } finally {
      setTrabajando(false)
    }
  }

  return (
    <Modal
      abierto
      titulo="Abrir sesión de caja"
      onCerrar={onCerrar}
      pie={
        <>
          <Boton variante="secundario" onClick={onCerrar}>
            Cancelar
          </Boton>
          <Boton cargando={trabajando} disabled={!fondoValido} onClick={abrir}>
            Abrir sesión
          </Boton>
        </>
      }
    >
      <div className="ped-dialogo">
        {aviso && <Aviso tono="error">{aviso}</Aviso>}
        <Select etiqueta="Caja registradora" value={cajaId} onChange={(e) => setCajaId(e.target.value)}>
          {libres.map((c) => (
            <option key={c.id} value={c.id}>
              {c.nombre} - {c.codigo}
              {c.principal ? ' ★' : ''}
            </option>
          ))}
        </Select>
        <Campo
          etiqueta="Fondo inicial (S/)"
          inputMode="decimal"
          placeholder="0.00"
          value={fondo}
          error={fondo !== '' && !fondoValido ? 'Escribe un monto válido, por ejemplo 100 o 100.50.' : undefined}
          ayuda="El efectivo con el que empiezas. Puede ser cero, pero siempre se escribe."
          onChange={(e) => setFondo(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && abrir()}
        />
        <Campo
          etiqueta="Notas (opcional)"
          placeholder="Ej: turno mañana, caja con billetes de 10 y 20…"
          value={nota}
          maxLength={250}
          onChange={(e) => setNota(e.target.value)}
        />
      </div>
    </Modal>
  )
}
