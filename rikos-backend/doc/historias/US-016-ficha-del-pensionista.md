# US-016 — La ficha del pensionista y su responsable

Sprint 5, entrega 3 de 4 (2026-09-29). Especificación en
`docs/producto/plan/sprint-05-cuenta-del-pensionista.md` (sección 3.2 y 3.3).

## Qué se pidió

Registrar la ficha completa del pensionista con su responsable obligatorio,
DNI único en la empresa, credenciales que nunca se muestran legibles, y una
cuenta corriente abierta al confirmar el alta. Desactivar sin perder la cuenta.

## Qué se construyó

**Base de datos — `V27__pensionistas.sql`.** `pensionista` (DNI de 8 dígitos
con restricción única **en la base**, plan, tres banderas de comidas
habituales, frecuencia de abono, límite de crédito nulo = calculado, PIN
cifrado, contraseña del portal nula, estado), `responsable` (1-1, los seis
datos obligatorios) y `cuenta_pensionista` (1-1, vacía). Evento de bitácora
nuevo `cambio_credencial`.

**Backend (`pensionistas`).**

- `CrearPensionista`: valida, guarda ficha + responsable + cuenta, genera el
  **PIN de 4 dígitos** (`SecureRandom`), lo guarda con BCrypt (el mismo
  codificador que los PIN de usuarios) y lo devuelve **en claro solo en esa
  respuesta**.
- `ActualizarPensionista` (no toca PIN ni estado; un cambio de plan queda en la
  bitácora con el anterior y el nuevo), `CambiarEstadoPensionista` (misma
  cuenta al reactivar, RN-034), `RegenerarPin` (bitácora sin el PIN),
  `ConsultaPensionistas` (lista, búsqueda por nombre o DNI, ficha).
- `PensionistaController`: `GET /pensionistas[?q=]`, `GET /{id}`, `POST`, `PUT
  /{id}`, `POST /{id}/desactivar|activar|pin`. Permisos: ver, crear, modificar
  (editar y PIN nuevo) y desactivar. Cajero y mozo solo ven; cocina y
  motorizado no entran.
- Bitácora en español: "admin desactivó el pensionista "X"", "admin generó un
  PIN nuevo para "X"", "admin cambió el plan de "X" de A a B".

**Frontend.** Pensionistas → **Pensionistas**: lista con buscador, pestañas
Activos / Inactivos / Todos y las comidas habituales (D A C); **ficha** con
datos personales, estudios, plan y comidas, y responsable; modal del PIN que
se muestra una sola vez ("no se puede volver a ver"); "Generar PIN nuevo",
"Desactivar" y "Reactivar" con confirmación; errores marcados en su campo (el
DNI repetido, por ejemplo). Con un solo plan, la ficha nueva ya lo trae elegido.
Solo lectura sin permiso.

## Decisiones de esta entrega

- **La contraseña del portal no se pide** (desviación de la fuente, con su razón
  en la especificación, sección 3.2): la columna existe y es nula hasta que se
  defina cómo se asigna y recupera (RF-027).
- **El PIN lo genera el sistema, no se elige.** Evita PIN previsibles
  (1234, fecha de nacimiento) y hace que "olvidé mi PIN" sea "genera otro".
- **Comidas habituales: al menos una**, y no limitan el consumo.
- **DNI editable** (un error de tipeo es realista), con la misma unicidad; el
  propio DNI no cuenta como duplicado al editar.
- **Un pensionista nunca se borra:** tiene una cuenta con historial. Solo se
  desactiva.
- **Solo con conexión** (sección 3.3): no hay nada especial en el código; es la
  regla de que las fichas se crean y editan solo en línea.
- **`limite_credito` ya existe en la tabla y en la respuesta,** pero no tiene
  pantalla: el tope y su edición son de la entrega 4.

## Qué se dejó fuera a propósito

- **La cuenta** (saldo, deuda, tope, movimientos) y la pestaña Cuenta: entrega 4.
- **Kiosco y pre-ticket** (sprint 8) y **portal** (sprint 13).
- **Cargar a los ~25 pensionistas:** se dan de alta a mano; con el saldo con el
  que llegan se cargan en la entrega 4 como "saldo inicial".
- **Validación de formato del correo o del teléfono:** solo que el del
  responsable no esté vacío. Sin regla en la especificación.

## Cómo se verificó

**Pruebas automatizadas: 278 en verde, sin contar `ArquitecturaDeModulosTest`**
(suma de reportes = conteo de `@Test`; 258 al cerrar la entrega 2).

- `PensionistaTest` (15): el alta guarda ficha, responsable y cuenta abierta; el
  PIN es de 4 dígitos, se guarda cifrado y coincide con su hash; la contraseña
  del portal es nula; regenerar cambia la credencial y queda en la bitácora sin
  el PIN; DNI único; DNI de 8 dígitos (siete, nueve, con letras, vacío, nulo);
  sin responsable no se guarda; los seis datos del responsable obligatorios y su
  DNI válido; nombres, plan existente, al menos una comida y frecuencia
  válida; las tres comidas; editar ficha y responsable; cambiar de plan queda
  en la bitácora y no toca el PIN; no se puede tomar el DNI de otro; desactivar
  y reactivar conserva **la misma cuenta** y deja dos filas en la bitácora;
  lista y búsqueda incluyen inactivos.
- `PensionistasAccesoPorRolTest` (4), con los roles reales: administrador
  central y de local hacen todo, y **ni la ficha ni la lista llevan `pin`,
  `hash`, `contrasena` ni `$2a$`**; cajero y mozo ven pero reciben 403 al crear,
  editar, generar PIN, activar y desactivar; cocina y motorizado 403; DNI
  repetido 409, sin responsable 422, comida inexistente 422, ficha inexistente
  404.
- `ListarAuditoriaTest` (+1): las frases de pensionista y que el PIN nuevo
  no aparece en ningún valor guardado.

**En vivo** (usuario `admin`, datos de **ejemplo**): la lista vacía con su
estado; intentar guardar incompleto avisa "Falta: nombres y apellidos, todos los
datos del responsable"; el alta de "Luis Alberto Quispe Rojas" mostró el modal
con el PIN (6887); ni `GET /pensionistas` ni la ficha lo devuelven, y en la
base `hash_pin` es un hash BCrypt distinto del PIN, `hash_contrasena_portal` es
nulo y la cuenta quedó "abierta" con su responsable. Desactivar mostró el aviso
"conserva su cuenta y puede seguir abonando", reactivar lo devolvió a activo,
"Generar PIN nuevo" dio otro PIN, editar el teléfono habilitó "Guardar
cambios" y guardó, y un segundo alta con el mismo DNI marcó el error en el campo
DNI sin mostrar PIN. La bitácora registró desactivó, activó y el PIN nuevo.

## Encontrado en el camino

- **Una prueba mía creaba un plan sin usuario en el contexto** (los planes se
  auditan) y hacía fallar el login siguiente con un 500 que parecía de otra
  cosa. La causa estaba en el log: `usuario_id` nulo en `auditoria`. Ya se
  autentica solo durante ese paso.
- **El shell rechaza bloques muy grandes con comillas mezcladas;** se escriben
  archivos con la herramienta de escritura. (Anotado: ya pasó dos veces.)
- Mis capturas del navegador se adelantaron dos veces a la carga de la pantalla;
  la app estaba bien, hay que esperar a que aparezcan los controles.

## Datos de desarrollo (no son reales)

En la base de desarrollo quedó **un pensionista de ejemplo**: "Luis Alberto
Quispe Rojas", DNI 70123456, con responsable "Rosa Pérez Díaz". Es inventado; se
puede desactivar o dejar para probar la cuenta en la entrega 4.
