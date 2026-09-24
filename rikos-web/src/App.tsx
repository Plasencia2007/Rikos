import { lazy, Suspense } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { SesionProveedor } from './comun/sesion/SesionContexto'

// Carga perezosa por superficie: cada una se descarga solo cuando alguien
// entra a ella. Sin esto, la tablet del mozo descargaria tambien el panel
// del administrador y el portal.
const Pos = lazy(() => import('./apps/pos'))
const Kiosco = lazy(() => import('./apps/kiosco'))
const Portal = lazy(() => import('./apps/portal'))
const Motorizado = lazy(() => import('./apps/motorizado'))
const Admin = lazy(() => import('./apps/admin'))
// Andamiaje del Sprint 0, sin ruta visible desde ningun menu: solo para
// verificar el esqueleto (ver ESTRUCTURA.md, HT-01).
const Diagnostico = lazy(() => import('./apps/diagnostico'))
// Ruta de desarrollo, sin ruta visible desde ningun menu: muestra los
// componentes base del sistema de diseno juntos (ver "trabajo entre
// sprints", Bloque B).
const Muestra = lazy(() => import('./apps/muestra'))

export default function App() {
  return (
    <SesionProveedor>
      <BrowserRouter>
        <Suspense fallback={<div className="marcador">Cargando…</div>}>
          <Routes>
            {/* Se sirven desde el equipo del local */}
            <Route path="/pos/*" element={<Pos />} />
            <Route path="/kiosco/*" element={<Kiosco />} />

            {/* Se sirven desde la nube */}
            <Route path="/portal/*" element={<Portal />} />
            <Route path="/motorizado/*" element={<Motorizado />} />
            <Route path="/admin/*" element={<Admin />} />

            <Route path="/diagnostico" element={<Diagnostico />} />
            <Route path="/muestra" element={<Muestra />} />

            <Route path="/" element={<Navigate to="/pos" replace />} />
            <Route path="*" element={<Navigate to="/pos" replace />} />
          </Routes>
        </Suspense>
      </BrowserRouter>
    </SesionProveedor>
  )
}
