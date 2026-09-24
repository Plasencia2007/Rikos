# rikos-web

Frontend del Sistema de Gestion Integral RIKO'S.
React 19 + TypeScript, empaquetado con Vite.

## Arrancar

```bash
npm install
npm run dev
```

Queda en http://localhost:5173 y redirige a `/pos`.

El proxy manda `/api` al gateway en el 8080, asi que para probar contra
el backend hay que tenerlo levantado.

## Un proyecto, cinco superficies

| Ruta | Superficie | Se sirve desde |
|---|---|---|
| `/pos` | POS: mesas, pedido, caja | Equipo del local |
| `/kiosco` | Autoservicio del pensionista | Equipo del local |
| `/portal` | Pensionista y responsable | Nube |
| `/motorizado` | Entregas | Nube |
| `/admin` | Panel del administrador | Nube |

Se compila una vez y la misma carpeta `dist` se copia a los dos destinos.

## Donde va cada cosa

| Carpeta | Que contiene |
|---|---|
| `src/apps/` | El armazon de cada superficie: layout y rutas. Sin reglas de negocio. |
| `src/features/` | Pantallas y logica del negocio. Mismos nombres que los modulos del backend. |
| `src/comun/` | Lo que usan dos o mas features. |
| `src/api/` | Cliente HTTP y los tipos generados desde OpenAPI. |

El criterio: **si lo borras, que se rompe?** Una superficie, va en `apps`.
Una funcionalidad, va en `features`. Medio sistema, va en `comun`.

Cada feature expone un `index.ts` y ninguna importa desde adentro de otra:
la misma disciplina que Spring Modulith aplica en el backend.

## Reglas

1. Ninguna direccion escrita a mano. Todo pasa por `src/api` y es relativo.
2. `src/api/generado/` no se edita nunca: lo regenera una herramienta.
3. El buffer sin conexion vive solo en `comun/offline`, y es para microcortes
   de WiFi, no para replicar el sistema en el navegador.
4. Carga perezosa por superficie, siempre.

## Se instala cuando se necesite, no antes

- `@tanstack/react-query` al conectar la primera pantalla al backend
- `tailwindcss` al empezar a maquetar
- `vite-plugin-pwa` al armar el POS para tablets
- `@stomp/stompjs` al hacer mesas y comandas
- `dexie` para el buffer de microcortes
