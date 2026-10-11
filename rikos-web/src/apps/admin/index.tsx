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
  PantallaRecetas,
  PantallaRecetaFormulario,
} from '../../features/catalogo'
import { PantallaProveedores, PantallaProveedorFormulario } from '../../features/proveedores'
import { PantallaConfiguracion, PantallaMotivos } from '../../features/configuracion'
import { PantallaMenuNormalYExtra, PantallaProgramacionSemanal, PantallaDisponibilidad } from '../../features/menu'
import { PantallaAuditoria } from '../../features/auditoria'
import {
  PantallaPlanes,
  PantallaParametrosDeCobro,
  PantallaPensionistas,
  PantallaPensionistaFormulario,
  PantallaCuentaPensionista,
  PantallaCuentasCorrientes,
} from '../../features/pensionistas'
import { PantallaSalon } from '../../features/venta'
import {
  PantallaMesaFormulario,
  PantallaPlano,
  PantallaSalonDetalle,
  PantallaSalonFormulario,
  PantallaSalones,
} from '../../features/salones'
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
          <Route path="locales/:id/editar/:paso" element={<PantallaLocalFormulario />} />
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
          <Route path="catalogo/recetas" element={<PantallaRecetas />} />
          <Route path="catalogo/recetas/nueva" element={<PantallaRecetaFormulario />} />
          <Route path="catalogo/recetas/:id" element={<PantallaRecetaFormulario />} />
          <Route path="compras/proveedores" element={<PantallaProveedores />} />
          <Route path="compras/proveedores/nuevo" element={<PantallaProveedorFormulario />} />
          <Route path="compras/proveedores/:id/editar" element={<PantallaProveedorFormulario />} />
          <Route path="menu/normal-y-extra" element={<PantallaMenuNormalYExtra />} />
          <Route path="menu/programacion" element={<PantallaProgramacionSemanal />} />
          <Route path="menu/disponibilidad" element={<PantallaDisponibilidad />} />
          <Route path="configuracion" element={<PantallaConfiguracion />} />
          <Route path="configuracion/motivos" element={<PantallaMotivos />} />
          <Route path="configuracion/parametros-de-cobro" element={<PantallaParametrosDeCobro />} />
          <Route path="pensionistas/planes" element={<PantallaPlanes />} />
          <Route path="pensionistas" element={<PantallaPensionistas />} />
          <Route path="pensionistas/nuevo" element={<PantallaPensionistaFormulario />} />
          <Route path="pensionistas/cuentas" element={<PantallaCuentasCorrientes />} />
          {/* Salones y mesas (Configuración). La lista plana de mesas de antes quedó dentro de cada salón. */}
          <Route path="configuracion/mesas" element={<Navigate to="/admin/configuracion/salones" replace />} />
          <Route path="configuracion/salones" element={<PantallaSalones />} />
          <Route path="configuracion/salones/nuevo" element={<PantallaSalonFormulario />} />
          <Route path="configuracion/salones/:salonId" element={<PantallaSalonDetalle />} />
          <Route path="configuracion/salones/:salonId/editar" element={<PantallaSalonFormulario />} />
          <Route path="configuracion/salones/:salonId/mesas/nueva" element={<PantallaMesaFormulario />} />
          <Route path="configuracion/salones/:salonId/mesas/:mesaId/editar" element={<PantallaMesaFormulario />} />
          <Route path="configuracion/salones/:salonId/plano" element={<PantallaPlano />} />
          <Route path="salon/mesas" element={<PantallaSalon />} />
          <Route path="pensionistas/:id/cuenta" element={<PantallaCuentaPensionista />} />
          <Route path="pensionistas/:id" element={<PantallaPensionistaFormulario />} />
          <Route path="auditoria" element={<PantallaAuditoria />} />
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
