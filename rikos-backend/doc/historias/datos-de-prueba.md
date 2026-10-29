# Datos de prueba al arrancar (perfil `demo`) y backend sin Docker

Hecho el 2026-10-28. Mejora de herramientas de desarrollo, sin historia del backlog.

## Qué se pidió

1. Poder levantar el backend sin Docker, porque Docker Desktop consume mucha RAM.
2. Que al levantar el backend siempre haya datos de prueba para operar, separados de
   los datos del sistema.

## Datos del sistema y datos de prueba

| | Datos del sistema | Datos de prueba |
|---|---|---|
| Qué son | Empresa, local San Isidro, usuario `admin`, roles, permisos, categorías, unidades, motivos, productos «Menú normal» y «Menú extra» | Personal, salones y mesas, insumos, platos, recetas, stock, proveedores, planes, pensionistas, horarios y el menú de la semana |
| Quién los crea | Las migraciones de Flyway (`db/migration`) | `SembradorDatosDePrueba` con `demo/datos-de-prueba.sql` |
| Dónde corren | En todos los entornos | Solo con el perfil `demo`, nunca con `nube` |

## Qué se construyó

- **`levantar-sin-docker.bat`** (en `rikos-backend/`): apunta los servicios al PostgreSQL
  nativo de Windows (puerto 5432) con `DB_URL`, apaga el chequeo de salud de RabbitMQ
  (el código todavía no lo usa) y activa el perfil `demo`. Sin argumentos abre
  `servicio-operacion` y el gateway en sus ventanas; con `operacion`, solo ese servicio.
  La preparación de la base (usuario `rikos` y las dos bases) está en `docs/EMPEZAR.md`.
- **Módulo `demo`** (`pe.rikos.operacion.demo`): `SembradorDatosDePrueba`, un
  `ApplicationRunner` con `@Profile("demo & !nube")`. En una sola transacción:
  1. Ejecuta `src/main/resources/demo/datos-de-prueba.sql`, reemplazando los `{{...}}`
     por hashes calculados al arrancar con el mismo `PasswordEncoder` y la misma
     `HuellaPinPensionista` del sistema.
  2. Si la semana actual no tiene programación, la crea confirmada, de lunes a domingo,
     con 3 entradas, 3 segundos del menú normal, 1 segundo del menú extra, refresco y postre.

### Qué carga

- **Personal** (contraseña `demo123`): `mozo1` (PIN 1111), `mozo2` (PIN 2222), `cajero1`,
  `cocina1`, `adminlocal`, todos en San Isidro.
- **Salones y mesas**: «Salón principal» con 8 mesas y «Terraza» con 3.
- **Horarios de servicio**: desayuno 7–10, almuerzo 12–16 y cena 18–21, los 7 días.
- **Catálogo**: 13 insumos con costo, 9 componentes del menú, 2 extras y 4 platos a la carta.
  Precio del menú normal S/ 12,00 y del extra S/ 16,00, y táper S/ 1,00, solo si nadie los
  había fijado.
- **Recetas costeadas**: ají de gallina, arroz con pollo, lomo saltado, tallarín verde,
  chicha morada y lomo saltado a la carta.
- **Stock inicial**: una entrada de ajuste por insumo (el Kardex cuadra con el stock); la
  espinaca queda bajo el mínimo a propósito, para ver la alerta.
- **Proveedores**: 2. **Planes**: «Plan estudiante (prueba)» y «Plan trabajador (prueba)».
- **Pensionistas**: 3, con responsable, cuenta abierta y PIN de kiosco 111111, 222222 y 333333.

Todo es inventado (nombres, DNI, RUC, precios, costos). Los IDs son fijos y empiezan con
`01DEM0`, para reconocerlos y borrarlos fácil si hiciera falta.

## Decisiones

- **No es una migración de Flyway.** Las migraciones son iguales en todos los entornos y no
  se editan; los datos de prueba cambian cuando el equipo quiera y no deben llegar a la nube.
  Por eso viven fuera de `db/migration` y no tocan el historial de Flyway.
- **Idempotente.** IDs fijos con `ON CONFLICT DO NOTHING`; el stock inicial solo se carga si
  el insumo no tiene existencia; los horarios, solo si ese día y servicio no tienen uno. Corre
  en cada arranque sin duplicar ni pisar lo que el equipo haya creado a mano.
- **SQL y no casos de uso.** Más simple de leer y ampliar. La contra es que no pasa por las
  validaciones ni deja auditoría; la prueba de abajo cubre que el resultado sea coherente.

## Cómo se verificó

- `SembradorDatosDePruebaTest` (5 casos, en verde): sembrar dos veces no duplica nada; las
  credenciales de prueba funcionan con el codificador y la huella reales; cada pensionista
  tiene responsable y cuenta abierta; las recetas quedan costeadas y el stock coincide con el
  Kardex; hoy hay menú programado. La prueba arma el sembrador a mano dentro de una
  transacción que se deshace.
- `ArquitecturaDeModulosTest`: el módulo `demo` no agrega violaciones (mismas violaciones con
  y sin él).
- El backend arrancó con el script contra PostgreSQL 18 nativo: Flyway aplicó V1 a V54 y el
  sembrador cargó los datos.

## Encontrado de paso

- `ArquitecturaDeModulosTest` ya no falla solo por el ciclo conocido `comun ↔ identidad`:
  también hay ciclos en `caja` e `inventario`, y dependencias a tipos no expuestos
  (`compras` → `inventario.infraestructura`, `inventario` → `produccion`,
  `pensionistas` → `caja.aplicacion.ErrorAbono`). Vienen de los sprints 8 y 9; queda
  pendiente.
- Los IDs de las categorías y productos sembrados por V15 y V21 tienen 13 ceros en el medio;
  es fácil copiarlos mal (pasó aquí y la prueba lo atrapó).
