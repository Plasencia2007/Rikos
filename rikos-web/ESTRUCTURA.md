# Estructura del frontend

Como se organiza el codigo y por que. Si vas a escribir aqui —persona o
asistente— lee esto primero.

---

## 1. Un proyecto, cinco superficies

```
src/apps/
├── pos/           mesas, pedido, caja        → equipo del local
├── kiosco/        autoservicio del pensionista → equipo del local
├── portal/        pensionista y responsable  → nube
├── motorizado/    entregas                   → nube
└── admin/         panel del administrador    → nube
```

Son cinco aplicaciones distintas dentro de un mismo proyecto. Se compila
**una vez** y la misma carpeta `dist` se copia a los dos destinos: el equipo
del restaurante sirve `/pos` y `/kiosco`, el servidor de la nube sirve el
resto.

El frontend nunca corre un proceso de Node en produccion: son archivos
estaticos servidos por nginx. Eso importa sobre todo en el equipo del local,
donde cada proceso extra es algo que alguien tendria que diagnosticar por
telefono un viernes de noche.

---

## 2. Donde va cada cosa

| Carpeta | Que contiene |
|---|---|
| `apps/` | El armazon de cada superficie: layout, barra lateral, arbol de rutas. **Sin reglas de negocio.** |
| `features/` | Las pantallas y la logica del negocio. |
| `comun/` | Lo que usan dos o mas features. |
| `api/` | Cliente HTTP y tipos generados desde OpenAPI. |
| `estilos/` | Estilos globales y variables. |

El criterio para decidir: **si lo borras, que se rompe?**

- Se rompe una superficie → va en `apps/`
- Se rompe una funcionalidad del negocio → va en `features/`
- Se rompe medio sistema → va en `comun/`

### Las features corresponden con los modulos del backend

```
features/          pe.rikos.operacion/
├── autenticacion  ←→  identidad
├── mesas          ←→  venta
├── pedido         ←→  venta
├── comanda        ←→  venta
├── caja           ←→  caja
├── pensionistas   ←→  pensionistas
├── catalogo       ←→  catalogo
├── menu           ←→  menu
├── inventario     ←→  inventario
└── delivery       ←→  delivery
```

Una historia del backlog se trabaja siempre en los mismos dos lugares.

---

## 3. La frontera entre features

Cada feature expone un `index.ts` y **ninguna importa desde los archivos
internos de otra**:

```ts
// bien
import { PantallaPin } from '../../features/autenticacion'

// mal
import { PantallaPin } from '../../features/autenticacion/PantallaPin'
```

Es la misma disciplina que Spring Modulith aplica en el backend. Sin ella, a
los tres meses todo depende de todo y mover una pantalla rompe cuatro.

Una feature por dentro:

```
features/pedido/
├── paginas/          las pantallas completas
├── componentes/      piezas propias de esta feature
├── hooks/            logica de estado reutilizable
├── modelo/           tipos y logica de presentacion
└── index.ts          lo unico que el resto puede importar
```

---

## 4. Ninguna direccion escrita a mano

Todas las llamadas pasan por `src/api` y son **relativas**: siempre empiezan
con `/api`.

Esa es la razon por la que el mismo codigo funciona apuntando al equipo del
local (`sanisidro.rikos.pe`) o a la nube (`app.rikos.pe`) sin cambiar nada:
en produccion el front y la API se sirven desde el mismo origen, y en
desarrollo el proxy de Vite hace lo mismo.

Si en algun componente aparece `http://localhost:8080` o un dominio escrito
a mano, esta mal.

### `src/api/generado/` no se edita nunca

Lo genera una herramienta a partir de la especificacion OpenAPI del backend.
Si alguien lo corrige a mano, la proxima regeneracion lo borra y mientras
tanto el frontend y el backend dejan de coincidir sin que nadie se entere.

---

## 5. Carga perezosa por superficie: obligatoria

En `App.tsx`, cada superficie se importa con `lazy()`.

No es una optimizacion opcional. Sin ella, la tablet del mozo descarga
tambien el panel del administrador y el portal completo, en un dispositivo
que suele ser modesto y con el WiFi del salon.

Cuando agregues una superficie o una pantalla pesada, mantene el patron.

---

## 6. El buffer sin conexion: que es y que no es

Vive **solo** en `comun/offline`. Ninguna feature guarda cosas en el
navegador por su cuenta.

**Que es:** una red de seguridad para microcortes. Si el WiFi parpadea o el
equipo del local se reinicia, el mozo sigue tecleando y lo pendiente se
reenvia solo.

**Que no es:** una replica del sistema. El modo sin conexion del *local* lo
resuelve el equipo que esta dentro del restaurante, no el navegador. Si
empiezan a replicar datos y logica en el cliente, van a construir dos veces
el mismo sistema y ninguna de las dos versiones va a estar bien.

---

## 7. Diseño tactil

El mozo opera **de pie, con una mano, con prisa y con el cliente esperando**.
Eso condiciona todo el POS:

- Objetivo tactil minimo 56px (`--toque` en los estilos). Por debajo se falla
  el toque.
- En pantallas de celular, el teclado y las acciones van en la mitad inferior:
  el pulgar no llega arriba.
- Buen contraste: hay reflejo y luz de sala.
- Menos toques siempre gana. El PIN entra solo al cuarto digito, sin boton de
  confirmar.

### La pantalla de PIN es una pantalla de bloqueo

No es un inicio de sesion. Aparece cada vez que un mozo suelta la tablet y
otro la toma. Por lo tanto:

- Boton de "cambiar de mozo" siempre visible en el POS.
- Retorno automatico al PIN por inactividad.
- **Sin perder nada**: el pedido pertenece a la mesa, no al dispositivo ni al
  mozo. Si entra otro mozo, la mesa sigue igual; solo cambia a quien se
  atribuyen las acciones siguientes.

Si esto se diseña mal, los mozos van a compartir un mismo PIN para no perder
el pedido, y se cae toda la trazabilidad que el sistema promete.

### La pantalla que decide todo

La toma de pedido con asignacion por persona en una mesa mixta —donde
conviven clientes regulares y varios pensionistas— es la pantalla mas dificil
del sistema y la que decide si se usa o si los mozos vuelven al papel.
Merece bocetos y una prueba con un mozo real antes de escribir codigo.

---

## 8. Permisos en un solo lugar

`comun/sesion` concentra el usuario activo, sus permisos efectivos y el
cambio rapido por PIN.

De ahi sale un componente que oculta acciones segun el permiso. Un usuario
sin permiso no puede anular, descontar ni ver costos, y eso se resuelve en un
solo lugar, no repartido por veinte pantallas.

Ocultar en el frontend es comodidad, **no seguridad**: el backend valida
igual. Pero si la comprobacion esta dispersa, tarde o temprano una pantalla
se olvida y el usuario ve un boton que no deberia.

---

## 9. Errores tipicos que conviene no cometer

- Escribir una direccion o un puerto a mano en un componente.
- Importar desde adentro de otra feature en vez de su `index.ts`.
- Editar `src/api/generado/`.
- Guardar cosas en el navegador desde una feature en vez de `comun/offline`.
- Agregar una superficie sin `lazy()`.
- Poner logica de negocio en `apps/`: ahi solo va el armazon.
- Botones pequeños "porque en el monitor se ven bien". El POS se usa en tablet.
- Crear una carpeta en `comun/` para algo que usa una sola feature.

---

## 10. Se instala cuando se necesite, no antes

| Libreria | Cuando |
|---|---|
| `@tanstack/react-query` | Al conectar la primera pantalla al backend |
| `tailwindcss` | Al empezar a maquetar en serio |
| `vite-plugin-pwa` | Al preparar el POS para las tablets |
| `@stomp/stompjs` | Al hacer mesas y comandas en tiempo real |
| `dexie` | Al implementar el buffer de microcortes |

Cada dependencia que se agrega antes de necesitarla es una decision tomada
sin informacion.
