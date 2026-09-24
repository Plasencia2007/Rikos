import { useState } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { RequiereSesion } from '../../comun/sesion/RequiereSesion'
import { EncabezadoLocal } from '../../comun/sesion/EncabezadoLocal'
import { PantallaLocales, PantallaLocalFormulario } from '../../features/locales'
import { PantallaUsuarios, PantallaUsuarioFormulario, PantallaRoles, PantallaRolFormulario } from '../../features/usuarios'
import {
  PantallaProductos,
  PantallaProductoFormulario,
  PantallaCategorias,
  PantallaUnidadesYPresentaciones,
} from '../../features/catalogo'
import { PantallaProveedores, PantallaProveedorFormulario } from '../../features/proveedores'
import { BarraLateral } from './BarraLateral'

/**
 * Armazon de dashboard: SOLO para /admin. El POS no navega menus, opera
 * sobre mesas a pantalla completa -- este layout no se aplica ahi.
 */
function Panel() {
  const [menuAbierto, setMenuAbierto] = useState(false)

  return (
    <div className="panel-shell">
      <BarraLateral abierta={menuAbierto} onCerrar={() => setMenuAbierto(false)} />

      <div className="panel-contenido">
        <EncabezadoLocal onAbrirMenu={() => setMenuAbierto(true)} />

        <Routes>
          <Route path="locales" element={<PantallaLocales />} />
          <Route path="locales/nuevo" element={<PantallaLocalFormulario />} />
          <Route path="locales/:id/editar" element={<PantallaLocalFormulario />} />
          <Route path="usuarios/roles" element={<PantallaRoles />} />
          <Route path="usuarios/roles/nuevo" element={<PantallaRolFormulario />} />
          <Route path="usuarios/roles/:id/editar" element={<PantallaRolFormulario />} />
          <Route path="usuarios/nuevo" element={<PantallaUsuarioFormulario />} />
          <Route path="usuarios/:id/editar" element={<PantallaUsuarioFormulario />} />
          <Route path="usuarios" element={<PantallaUsuarios />} />
          <Route path="catalogo/productos" element={<PantallaProductos />} />
          <Route path="catalogo/productos/nuevo" element={<PantallaProductoFormulario />} />
          <Route path="catalogo/productos/:id/editar" element={<PantallaProductoFormulario />} />
          <Route path="catalogo/categorias" element={<PantallaCategorias />} />
          <Route path="catalogo/unidades" element={<PantallaUnidadesYPresentaciones />} />
          <Route path="compras/proveedores" element={<PantallaProveedores />} />
          <Route path="compras/proveedores/nuevo" element={<PantallaProveedorFormulario />} />
          <Route path="compras/proveedores/:id/editar" element={<PantallaProveedorFormulario />} />
          {/* Ruta absoluta a proposito: una ruta relativa aca se resuelve
              respecto a la URL no coincidida (no a la base de este
              <Routes>), asi que con una URL invalida de mas de un segmento
              (ej. /admin/algo/que/no/existe) una ruta relativa arma un
              bucle infinito de redirecciones -- bug real encontrado al
              navegar directo a /admin/catalogo/categorias. */}
          <Route path="*" element={<Navigate to="/admin/usuarios" replace />} />
        </Routes>
      </div>
    </div>
  )
}

export default function Admin() {
  return (
    <RequiereSesion>
      <Panel />
    </RequiereSesion>
  )
}
