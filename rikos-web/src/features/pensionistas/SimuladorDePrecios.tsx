import { useState } from 'react'
import { useSesion } from '../../comun/sesion/SesionContexto'
import { Aviso, Boton, Campo, Checkbox, Select } from '../../comun/componentes'
import {
  ErrorApi,
  calcularPrecio,
  type PlanTarifario,
  type PrecioCalculado,
  type SolicitudDePrecio,
} from '../../api/cliente'
import { aCentimos, enSoles } from './dinero'

/**
 * "Probar un precio": pregunta al motor (MotorDePrecios) lo mismo que le
 * preguntara la caja, y muestra el precio con su explicacion. No escribe
 * nada. Sirve para que el duenio compruebe las reglas con sus propios casos
 * antes de que exista la caja.
 */

const AVISOS: Record<PrecioCalculado['avisos'][number], string> = {
  SIN_PRECIO_NORMAL: 'El producto no tiene precio normal todavía.',
  PLAN_SIN_PRECIO: 'El plan no tiene precio para esa comida en esa fecha.',
  TAPER_SIN_MONTO: 'Es para llevar, pero falta el monto del táper en Parámetros de cobro: no se cobra táper.',
}

export function SimuladorDePrecios({ planes }: { planes: PlanTarifario[] }) {
  const { sesion } = useSesion()
  const token = sesion!.token

  const [tipo, setTipo] = useState<SolicitudDePrecio['tipo']>('menu_del_dia')
  const [comida, setComida] = useState<SolicitudDePrecio['comida']>('almuerzo')
  const [planId, setPlanId] = useState('')
  const [precioNormal, setPrecioNormal] = useState('12.00')
  const [segundo, setSegundo] = useState(false)
  const [paraLlevar, setParaLlevar] = useState(false)
  const [resultado, setResultado] = useState<PrecioCalculado | null>(null)
  const [error, setError] = useState('')
  const [calculando, setCalculando] = useState(false)

  async function calcular() {
    const normal = aCentimos(precioNormal)
    if (Number.isNaN(normal)) {
      setError('El precio normal no es un monto válido. Usa un número mayor que cero, o déjalo vacío.')
      return
    }
    setCalculando(true)
    setError('')
    try {
      setResultado(
        await calcularPrecio(token, {
          tipo,
          comida,
          precioNormal: normal,
          planId: planId || null,
          segundoDelDia: segundo,
          paraLlevar,
        }),
      )
    } catch (e) {
      setResultado(null)
      setError(
        e instanceof ErrorApi && e.estado === 404 ? 'Ese plan ya no existe. Recarga la página.' : 'No se pudo calcular el precio.',
      )
    } finally {
      setCalculando(false)
    }
  }

  return (
    <section className="panel-admin-tarjeta sim-tarjeta" aria-labelledby="sim-titulo">
      <h2 id="sim-titulo">Probar un precio</h2>
      <p className="prod-nota">
        Comprueba cuánto se cobraría en cada caso. Es lo mismo que usará la caja; aquí no se cobra ni se guarda nada.
      </p>

      <div className="sim-campos">
        <Select etiqueta="Qué se cobra" value={tipo} onChange={(e) => setTipo(e.target.value as SolicitudDePrecio['tipo'])}>
          <option value="menu_del_dia">Menú del día</option>
          <option value="menu_extra">Menú extra</option>
          <option value="agregado">Agregado o plato de carta</option>
        </Select>
        <Select etiqueta="Comida" value={comida} onChange={(e) => setComida(e.target.value as SolicitudDePrecio['comida'])}>
          <option value="desayuno">Desayuno</option>
          <option value="almuerzo">Almuerzo</option>
          <option value="cena">Cena</option>
        </Select>
        <Select etiqueta="Quién" value={planId} onChange={(e) => setPlanId(e.target.value)}>
          <option value="">Cliente normal (sin ticket)</option>
          {planes.map((p) => (
            <option key={p.id} value={p.id}>
              Pensionista con ticket · {p.nombre}
            </option>
          ))}
        </Select>
        <Campo
          etiqueta="Precio normal del producto"
          inputMode="decimal"
          value={precioNormal}
          icono={<span aria-hidden="true">S/</span>}
          onChange={(e) => setPrecioNormal(e.target.value)}
        />
      </div>
      <div className="sim-marcas">
        <Checkbox etiqueta="Es el segundo de esa comida en el día" checked={segundo} onChange={(e) => setSegundo(e.target.checked)} />
        <Checkbox etiqueta="Para llevar, recojo o delivery" checked={paraLlevar} onChange={(e) => setParaLlevar(e.target.checked)} />
      </div>
      <div>
        <Boton cargando={calculando} onClick={calcular}>
          Calcular
        </Boton>
      </div>

      {error && <Aviso tono="error">{error}</Aviso>}
      {resultado && (
        <div className="sim-resultado" aria-live="polite">
          <dl className="plan-precios">
            <div className="plan-precio">
              <dt>Precio</dt>
              <dd className={resultado.precio == null ? 'plan-precio-vacio' : ''}>{enSoles(resultado.precio)}</dd>
            </div>
            <div className="plan-precio">
              <dt>Táper</dt>
              <dd className={resultado.taper == null ? 'plan-precio-vacio' : ''}>
                {resultado.taper == null ? 'No aplica' : enSoles(resultado.taper)}
              </dd>
            </div>
            <div className="plan-precio">
              <dt>Total por unidad</dt>
              <dd className={resultado.total == null ? 'plan-precio-vacio' : ''}>{enSoles(resultado.total)}</dd>
            </div>
          </dl>
          {resultado.avisos.map((a) => (
            <Aviso key={a}>{AVISOS[a]}</Aviso>
          ))}
          <ul className="sim-explicacion">
            {resultado.explicacion.map((linea) => (
              <li key={linea}>{linea}</li>
            ))}
          </ul>
        </div>
      )}
    </section>
  )
}
