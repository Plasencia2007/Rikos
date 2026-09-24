import { useState } from 'react'
import { Boton } from '../../comun/componentes/Boton'
import { Campo } from '../../comun/componentes/Campo'
import { Select } from '../../comun/componentes/Select'
import { Checkbox } from '../../comun/componentes/Checkbox'
import { Radio } from '../../comun/componentes/Radio'
import { Tabla, type ColumnaTabla } from '../../comun/componentes/Tabla'
import { Modal } from '../../comun/componentes/Modal'
import { ModalDeConfirmacion } from '../../comun/componentes/ModalDeConfirmacion'
import { ModalDeMotivo } from '../../comun/componentes/ModalDeMotivo'
import { Insignia } from '../../comun/componentes/Insignia'
import { Aviso } from '../../comun/componentes/Aviso'
import { EstadoVacio } from '../../comun/componentes/EstadoVacio'
import { EstadoCargando } from '../../comun/componentes/EstadoCargando'
import { EstadoError } from '../../comun/componentes/EstadoError'

interface FilaEjemplo {
  id: string
  nombre: string
}

const FILAS_EJEMPLO: FilaEjemplo[] = [
  { id: '1', nombre: 'San Isidro' },
  { id: '2', nombre: 'Miraflores' },
]

const COLUMNAS_EJEMPLO: ColumnaTabla<FilaEjemplo>[] = [
  { encabezado: 'Nombre', render: (f) => f.nombre },
  { encabezado: 'Acciones', acciones: true, render: () => <Boton variante="texto">Editar</Boton> },
]

/**
 * Ruta de desarrollo, sin enlace en ningun menu: muestra todos los
 * componentes base con sus variantes y estados juntos, para poder
 * revisarlos de una vez sin tener que ir pantalla por pantalla.
 */
export default function Muestra() {
  const [modal, setModal] = useState(false)
  const [confirmacion, setConfirmacion] = useState(false)
  const [motivo, setMotivo] = useState(false)
  const [tablaEstado, setTablaEstado] = useState<'datos' | 'cargando' | 'vacia' | 'error'>('datos')

  return (
    <div className="muestra">
      <h1>Sistema de diseño — muestra de componentes</h1>

      <section className="muestra-seccion">
        <h2>Botón</h2>
        <div className="muestra-fila">
          <Boton variante="primario">Primario</Boton>
          <Boton variante="secundario">Secundario</Boton>
          <Boton variante="peligro">Peligro</Boton>
          <Boton variante="texto">Texto</Boton>
        </div>
        <div className="muestra-fila">
          <Boton tamano="tactil">Tamaño táctil (56px)</Boton>
          <Boton cargando>Cargando</Boton>
          <Boton disabled>Deshabilitado</Boton>
        </div>
      </section>

      <section className="muestra-seccion">
        <h2>Campo, Select, Checkbox, Radio</h2>
        <div className="muestra-fila">
          <Campo etiqueta="Nombre" placeholder="Texto normal" />
          <Campo etiqueta="Con ayuda" ayuda="Un dato que ayuda a llenarlo" />
          <Campo etiqueta="Con error" error="Este campo es obligatorio" />
        </div>
        <div className="muestra-fila">
          <Select etiqueta="Local">
            <option>San Isidro</option>
            <option>Miraflores</option>
          </Select>
          <Checkbox etiqueta="Acepto los términos" />
          <Radio etiqueta="Opción A" name="muestra-radio" />
          <Radio etiqueta="Opción B" name="muestra-radio" />
        </div>
      </section>

      <section className="muestra-seccion">
        <h2>Insignia</h2>
        <div className="muestra-fila">
          <Insignia tono="activo" />
          <Insignia tono="inactivo" />
          <Insignia tono="pendiente" />
        </div>
      </section>

      <section className="muestra-seccion">
        <h2>Aviso — cuatro estados semánticos, bien diferenciados</h2>
        <div className="muestra-fila" style={{ flexDirection: 'column', alignItems: 'stretch' }}>
          <Aviso tono="exito">Guardado correctamente.</Aviso>
          <Aviso tono="aviso">
            El pensionista supera su límite de crédito. Puedes continuar, pero quedará registrado.
          </Aviso>
          <Aviso tono="error">No se pudo guardar. Intenta de nuevo.</Aviso>
          <Aviso tono="informacion">Los cambios se aplican la próxima vez que el mozo inicie sesión.</Aviso>
        </div>
      </section>

      <section className="muestra-seccion">
        <h2>Tabla — cuatro estados</h2>
        <div className="muestra-fila">
          <Boton variante="secundario" onClick={() => setTablaEstado('datos')}>Con datos</Boton>
          <Boton variante="secundario" onClick={() => setTablaEstado('cargando')}>Cargando</Boton>
          <Boton variante="secundario" onClick={() => setTablaEstado('vacia')}>Vacía</Boton>
          <Boton variante="secundario" onClick={() => setTablaEstado('error')}>Error</Boton>
        </div>
        <div className="panel-admin-tarjeta">
          <Tabla
            columnas={COLUMNAS_EJEMPLO}
            filas={tablaEstado === 'vacia' ? [] : FILAS_EJEMPLO}
            obtenerClave={(f) => f.id}
            cargando={tablaEstado === 'cargando'}
            error={tablaEstado === 'error' ? 'No se pudo cargar la lista.' : ''}
            vacioTitulo="Todavía no hay nada aquí"
            vacioDescripcion="Así se ve una lista vacía: con un texto que explica qué hacer."
            vacioAccion={<Boton>Nuevo</Boton>}
          />
        </div>
      </section>

      <section className="muestra-seccion">
        <h2>EstadoVacio / EstadoCargando / EstadoError (sueltos)</h2>
        <div className="panel-admin-tarjeta">
          <EstadoVacio titulo="Sin resultados" descripcion="Ejemplo suelto del componente." accion={<Boton>Acción</Boton>} />
        </div>
        <div className="panel-admin-tarjeta">
          <EstadoCargando />
        </div>
        <div className="panel-admin-tarjeta">
          <EstadoError mensaje="Ejemplo de error suelto." />
        </div>
      </section>

      <section className="muestra-seccion">
        <h2>Modal, ModalDeConfirmacion, ModalDeMotivo</h2>
        <div className="muestra-fila">
          <Boton onClick={() => setModal(true)}>Abrir Modal</Boton>
          <Boton variante="peligro" onClick={() => setConfirmacion(true)}>
            Abrir ModalDeConfirmacion
          </Boton>
          <Boton variante="secundario" onClick={() => setMotivo(true)}>
            Abrir ModalDeMotivo
          </Boton>
        </div>

        <Modal abierto={modal} titulo="Modal de ejemplo" onCerrar={() => setModal(false)}>
          <p>Contenido libre dentro del modal.</p>
        </Modal>

        <ModalDeConfirmacion
          abierto={confirmacion}
          titulo="Desactivar local"
          mensaje='Vas a desactivar "San Isidro". Ningún local desactivado puede recibir operaciones nuevas.'
          peligro
          onConfirmar={() => setConfirmacion(false)}
          onCancelar={() => setConfirmacion(false)}
        />

        <ModalDeMotivo
          abierto={motivo}
          titulo="Ajuste de inventario"
          motivos={[
            { id: '1', descripcion: 'Merma' },
            { id: '2', descripcion: 'Conteo físico', requiereObservacion: true },
          ]}
          onConfirmar={() => setMotivo(false)}
          onCancelar={() => setMotivo(false)}
        />
      </section>
    </div>
  )
}
