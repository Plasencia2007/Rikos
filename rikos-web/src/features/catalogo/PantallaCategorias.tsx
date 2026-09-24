import { useEffect, useState } from 'react'
import { useSesion } from '../../comun/sesion/SesionContexto'
import { Boton, Campo, Modal, Select, Tabla, type ColumnaTabla } from '../../comun/componentes'
import { listarCategorias, crearCategoria, actualizarCategoria, type Categoria } from '../../api/cliente'

const NATURALEZAS = [
  { valor: 'componente_menu', etiqueta: 'Componente de menú' },
  { valor: 'producto_fijo', etiqueta: 'Producto fijo' },
  { valor: 'extra', etiqueta: 'Extra' },
  { valor: 'insumo', etiqueta: 'Insumo' },
  { valor: 'subreceta', etiqueta: 'Subreceta' },
]

const COMPONENTES_MENU = [
  { valor: 'entrada', etiqueta: 'Entrada' },
  { valor: 'segundo', etiqueta: 'Segundo' },
  { valor: 'guarnicion', etiqueta: 'Guarnición' },
  { valor: 'refresco', etiqueta: 'Refresco' },
  { valor: 'postre', etiqueta: 'Postre' },
]

export function PantallaCategorias() {
  const { sesion } = useSesion()
  const token = sesion!.token

  const [categorias, setCategorias] = useState<Categoria[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [modalAbierto, setModalAbierto] = useState<'raiz' | 'hija' | null>(null)
  const [padreSeleccionado, setPadreSeleccionado] = useState<Categoria | null>(null)
  const [editando, setEditando] = useState<Categoria | null>(null)
  const [nombre, setNombre] = useState('')
  const [naturaleza, setNaturaleza] = useState(NATURALEZAS[0].valor)
  const [componenteMenu, setComponenteMenu] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [errorGuardado, setErrorGuardado] = useState('')

  function cargar() {
    setCargando(true)
    setError('')
    listarCategorias(token)
      .then(setCategorias)
      .catch(() => setError('No se pudieron cargar las categorías.'))
      .finally(() => setCargando(false))
  }

  useEffect(cargar, [token])

  const raices = categorias.filter((c) => c.categoriaPadreId === null)
  const hijasDe = (padreId: string) => categorias.filter((c) => c.categoriaPadreId === padreId)

  function abrirRaiz() {
    setModalAbierto('raiz')
    setEditando(null)
    setNombre('')
    setNaturaleza(NATURALEZAS[0].valor)
    setComponenteMenu('')
    setErrorGuardado('')
  }

  function abrirHija(padre: Categoria) {
    setModalAbierto('hija')
    setPadreSeleccionado(padre)
    setEditando(null)
    setNombre('')
    setErrorGuardado('')
  }

  function abrirEdicion(categoria: Categoria) {
    setModalAbierto(categoria.categoriaPadreId ? 'hija' : 'raiz')
    setEditando(categoria)
    setNombre(categoria.nombre)
    setNaturaleza(categoria.naturaleza)
    setComponenteMenu(categoria.componenteMenu ?? '')
    setErrorGuardado('')
  }

  async function guardar() {
    setGuardando(true)
    setErrorGuardado('')
    try {
      if (editando) {
        await actualizarCategoria(token, editando.id, { nombre, orden: editando.orden })
      } else if (modalAbierto === 'raiz') {
        await crearCategoria(token, {
          nombre,
          naturaleza,
          componenteMenu: naturaleza === 'componente_menu' ? componenteMenu : undefined,
          orden: raices.length + 1,
        })
      } else if (padreSeleccionado) {
        await crearCategoria(token, {
          nombre,
          categoriaPadreId: padreSeleccionado.id,
          orden: hijasDe(padreSeleccionado.id).length + 1,
        })
      }
      setModalAbierto(null)
      cargar()
    } catch {
      setErrorGuardado('No se pudo guardar la categoría. Revisa que el nombre no esté repetido.')
    } finally {
      setGuardando(false)
    }
  }

  const columnas: ColumnaTabla<Categoria>[] = [
    { encabezado: 'Nombre', render: (c) => (c.categoriaPadreId ? `— ${c.nombre}` : c.nombre) },
    { encabezado: 'Naturaleza', render: (c) => c.naturaleza },
    { encabezado: 'Componente de menú', render: (c) => c.componenteMenu ?? '—' },
    {
      encabezado: 'Acciones',
      acciones: true,
      render: (c) => (
        <div style={{ display: 'flex', gap: 8 }}>
          {!c.categoriaPadreId && (
            <Boton variante="secundario" onClick={() => abrirHija(c)}>
              Agregar subcategoría
            </Boton>
          )}
          <Boton variante="secundario" onClick={() => abrirEdicion(c)}>
            Editar
          </Boton>
        </div>
      ),
    },
  ]

  const filasOrdenadas = raices.flatMap((raiz) => [raiz, ...hijasDe(raiz.id)])

  return (
    <section className="panel-admin">
      <div className="panel-admin-encabezado">
        <h1>Categorías</h1>
        <Boton onClick={abrirRaiz}>Nueva categoría raíz</Boton>
      </div>

      <Tabla
        columnas={columnas}
        filas={filasOrdenadas}
        obtenerClave={(c) => c.id}
        cargando={cargando}
        error={error}
        vacioTitulo="Todavía no hay categorías"
        vacioDescripcion="Crea la primera categoría raíz para empezar a organizar el catálogo."
        vacioAccion={<Boton onClick={abrirRaiz}>Nueva categoría raíz</Boton>}
      />

      {modalAbierto && (
        <Modal
          abierto
          titulo={
            editando
              ? 'Editar categoría'
              : modalAbierto === 'raiz'
                ? 'Nueva categoría raíz'
                : `Nueva subcategoría de ${padreSeleccionado?.nombre}`
          }
          onCerrar={() => setModalAbierto(null)}
        >
          <Campo etiqueta="Nombre" value={nombre} onChange={(e) => setNombre(e.target.value)} required />
          {modalAbierto === 'raiz' && !editando && (
            <>
              <Select etiqueta="Naturaleza" value={naturaleza} onChange={(e) => setNaturaleza(e.target.value)}>
                {NATURALEZAS.map((n) => (
                  <option key={n.valor} value={n.valor}>
                    {n.etiqueta}
                  </option>
                ))}
              </Select>
              {naturaleza === 'componente_menu' && (
                <Select
                  etiqueta="Componente de menú"
                  value={componenteMenu}
                  onChange={(e) => setComponenteMenu(e.target.value)}
                >
                  <option value="">— Elegir —</option>
                  {COMPONENTES_MENU.map((c) => (
                    <option key={c.valor} value={c.valor}>
                      {c.etiqueta}
                    </option>
                  ))}
                </Select>
              )}
            </>
          )}
          {errorGuardado && <p className="campo-error">{errorGuardado}</p>}
          <Boton onClick={guardar} cargando={guardando}>
            Guardar
          </Boton>
        </Modal>
      )}
    </section>
  )
}
