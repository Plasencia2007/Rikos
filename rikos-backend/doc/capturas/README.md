# Capturas del estado actual (2026-09-22, última pasada)

Generadas con Playwright contra `rikos-web` corriendo en `localhost:5173`,
autenticado como `admin`. Cada una se revisó contra la consola del navegador
antes de guardarse -- 0 errores reales (algunas herramientas de captura
arrastran historial de consola de navegaciones previas; se verificó que
esos mensajes no correspondían a la carga actual).

| Archivo | Vista | Notas |
|---|---|---|
| `01-locales-lista.png` | `/admin/locales` | Tarjetas de resumen, columna "Gerente", header sin buscador/campana falsos, selector de local siempre visible |
| `02-usuarios-lista.png` | `/admin/usuarios` | Lista de usuarios |
| `03-usuarios-modal-nuevo.png` | `/admin/usuarios` | Modal "Nuevo" abierto |
| `04-local-datos-basicos.png` | `/admin/locales/{id}/editar` | Datos Básicos: código+ayuda+validación, nombre, estado (checkbox con confirmación), dirección, y **el mapa de ubicación movido aquí** (antes vivía en una pestaña aparte) |
| `05-local-contactos.png` | ídem | Contactos: teléfono con prefijo de país, correo, **redes sociales** (Facebook/Instagram/TikTok) |
| `06-local-online-canales.png` | ídem | Online y Horarios: **canales de venta** (delivery/recojo + tiempo mín/máx de entrega) |
| `07-local-horarios.png` | ídem (scroll) | Misma pestaña, más abajo: horario semanal con insignias circulares por día ("Abierto"/"Cerrado") |
| `08-local-nuevo.png` | `/admin/locales/nuevo` | Vista aparte (no modal) para crear un local |
| `09-login.png` | `/admin` sin sesión | Login del panel |
| `10-pin-escritorio.png` / `11-pin-movil.png` | `/pos` | Pantalla de bloqueo del POS, escritorio y móvil |
| `12-muestra-componentes.png` | `/muestra` | Catálogo de componentes base (dev only) |
| `13-error-codigo-duplicado.png` | `/admin/locales/nuevo` | "Ya existe un local con ese código." |
| `14-error-codigo-formato.png` | ídem | Validación de formato de código (cliente, antes de tocar el backend) |
| `15-error-eliminar-con-historial.png` | `/admin/locales` | "No se puede eliminar: este local ya tiene ventas registradas. Puedes desactivarlo en su lugar." |
| `16-dia-especial-modal.png` | Online y Horarios | Modal "Nuevo día especial" |
| `17-dias-especiales-lista.png` | ídem | Lista de días especiales/feriados cargados |
| `18-pruebas-horarios.txt` | -- | Salida de las pruebas de cruce de medianoche y de día-especial-gana-sobre-horario-semanal |
| `19-local-logo.png` | Logo y Aspecto | Pestaña nueva: URL del logo con vista previa |

**Nota:** `06-local-ubicacion.png` de la pasada anterior se eliminó -- esa
pestaña ya no existe como tal, el mapa se fusionó dentro de Datos Básicos.

## Cambios de esta ultima pasada (sobre lo que ya estaba documentado antes)

Revierte a proposito dos exclusiones explicitas de la ampliacion anterior
de US-007 ("sin redes sociales/logo, no hay superficie publica que las
use" y "el tiempo de entrega es una decision de negocio que el propietario
no habia tomado") -- el propietario ya tomo esas decisiones. Sigue
excluido, porque no se pidio revertirlo: RUC/razon social por local (el
modelo sigue siendo una empresa con locales como establecimientos anexos)
y zonas de reparto (el delivery sigue gratuito y sin zonas, RN-081).

- **Backend**: migracion `V12__local_canales_venta.sql` -- `local` gana
  `facebook_url`, `instagram_url`, `tiktok_url`, `logo_url`,
  `delivery_habilitado`, `recojo_habilitado`, `tiempo_min_entrega_min`,
  `tiempo_max_entrega_min`. `Local.actualizar(...)` se refactorizo a
  recibir un record `DatosActualizacion` (la firma individual ya iba en
  10+ parametros).
- **Frontend**: pestañas renombradas y reestructuradas para calzar con la
  referencia de diseño que compartio el usuario: "Datos Básicos" /
  "Contactos" / "Online y Horarios" / "Logo y Aspecto" (antes eran Datos
  básicos / Contacto / Ubicación / Horarios, cuatro pestañas separadas).
  El mapa se movio de una pestaña propia a vivir dentro de Datos Básicos
  (identificacion + ubicacion geografica son la misma pregunta). El
  selector de local en el encabezado volvio a estar siempre visible
  (antes se ocultaba con 0-1 locales); se quitaron el buscador global y
  la campana de notificaciones del encabezado porque no hacian nada real.
  Los dias de la semana en el horario ahora muestran una insignia
  circular con la inicial + "Abierto"/"Cerrado", como en la referencia.
- **Simplificaciones conscientes** frente a la referencia visual: el logo
  se guarda como URL (no hay subida de archivos ni almacenamiento de
  imagenes todavia), y hay un solo horario semanal ("Horarios de
  atencion"), no tres calendarios separados por canal (atencion/delivery/
  recojo) -- la referencia insinua eso pero construirlo triplicaria el
  modelo de horario_servicio sin que se haya pedido explicitamente.

## Verificación (no solo visual)

- Backend: `mvn test` en `servicio-operacion` → **20/20 pruebas verdes**
  (6 clases), incluyendo `RangoHorarioTest` y `ResolverServicioVigenteTest`.
- `tsc --noEmit` en `rikos-web` limpio en cada iteración.
- Flujo completo probado en vivo: crear local → completar Contactos con
  redes sociales → verificado por API (`GET /api/locales`) que
  `facebookUrl` persistio correctamente.
- Migraciones `V11` (horarios) y `V12` (canales de venta) aplicadas
  limpias sobre la base de datos de desarrollo (Postgres en Docker).

## Modelo de datos

`RIKOS-modelo-de-datos.md` en esta misma carpeta (`rikos-backend/doc/`)
**todavia no refleja** los campos de V12 (redes sociales, logo, canales de
venta) -- esa migracion se hizo directo en codigo a pedido del usuario,
sin pasar primero por una actualizacion del documento. Vale la pena
actualizarlo antes de que alguien lo lea como fuente de verdad.

---

# US-004/US-006: permisos granulares y roles (2026-09-23)

Movido a [`../historias/US-004-US-006-permisos-roles.md`](../historias/US-004-US-006-permisos-roles.md).
A partir de ahora las historias de usuario se documentan ahi, una por
archivo -- ver `doc/historias/README.md` para la convencion.
