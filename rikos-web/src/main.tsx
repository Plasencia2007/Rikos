import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App'
import './estilos/global.css'
import './estilos/asistente-local.css'
import './estilos/usuarios.css'
import './estilos/roles.css'
import './estilos/configuracion.css'
import './estilos/parametros-cobro.css'
import './estilos/motivos.css'
import './estilos/salones.css'
import './estilos/mapa.css'
import './estilos/pos.css'
import './estilos/pin.css'
import '@fontsource-variable/plus-jakarta-sans'

createRoot(document.getElementById('raiz')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
