# 1. El problema

> **El deficiente control integrado y sin trazabilidad de los pedidos, los cobros a
> pensionistas y el manejo de almacén en el Restaurante RIKO'S.**

Es el problema central declarado en el Project Charter, sección 7 — Origen del proyecto.
Las tres palabras añadidas —«y sin trazabilidad»— son la única variación respecto del texto
original, y existen para que las épicas de acceso y de reportes bajen del problema y no solo
de las bases legales.

El charter descompone ese problema en **tres causas directas**:

| | Causa | Consecuencia que ya se observa |
|----------|----------------------------------------------------------------|-----------------------------------------------------|
| 1 | Los pedidos se registran manualmente en papel y se trasladan entre el salón y la cocina | Demoras, pérdida de información y errores en la preparación |
| 2 | Los consumos de los estudiantes se registran en cuadernos, sin validar identidad, saldo ni local | Saldos negativos, consumos cruzados y deudas no detectadas a tiempo |
| 3 | Las compras y salidas de almacén no se relacionan con las recetas ni con las ventas | No se conoce el consumo real de los insumos; hay mermas y compras de emergencia |

A esas tres se suma un **factor del entorno**: la inestabilidad de la conexión a Internet, que
puede detener el registro de las ventas. No es una causa del descontrol, pero condiciona
cualquier solución: un sistema que dependa de la conexión no resuelve el problema, lo traslada.

# 2. Las cinco épicas

El problema se reparte en cinco épicas. Las tres primeras son las tres causas, en el mismo
orden en que el charter las enuncia. La cuarta responde al factor del entorno y la quinta
sostiene el control: ninguna de las dos es una causa, y por eso van al final.

| Prioridad | Épica | Historias |
|--------------------|-------------------------------------------------------------------------------|-------------------:|
| 1 — Alta | **E1 — Registrar los pedidos de venta** | 14 |
| 2 — Alta | **E2 — Controlar la cobranza** | 18 |
| 3 — Alta | **E3 — Controlar el consumo de insumos** | 14 |
| 4 — Media | **E4 — Garantizar la continuidad del servicio** | 5 |
| 5 — Media | **E5 — Supervisar la operación** | 13 |
| | **Total** | **64** |

A esto se suman **ocho tareas técnicas** (apartado 4), que no son historias de usuario
porque nadie del restaurante las pide y no le entregan nada directamente.

**Cómo se escribe cada historia.** Perfil más deseo más finalidad, siempre desde quien usa el
sistema en el restaurante y nunca desde el equipo de desarrollo:

> *Como cajero, quiero cerrar la cuenta dividiendo libremente el pago entre medios, para cobrar
> como el cliente lo necesite sin liberar la mesa antes de tiempo.*

**Cómo se ordenan.** Dentro de cada épica van primero las historias Must, luego las Should y al
final las Could; a igual prioridad, en el orden en que conviene construirlas. La numeración
US-01 a US-64 sigue ese mismo orden.


## 2.1 E1 — Registrar los pedidos de venta

**Causa 1 del charter.** Los pedidos se registran manualmente en papel y se trasladan entre el salón y la cocina, lo que genera demoras, pérdida de información y errores en la preparación.


## 2.2 E2 — Controlar la cobranza

**Causa 2 del charter.** Los consumos de los estudiantes se registran en cuadernos sin validación automática de identidad, saldo disponible ni local de consumo, lo que facilita saldos negativos, consumos cruzados y deudas no detectadas a tiempo.


## 2.3 E3 — Controlar el consumo de insumos

**Causa 3 del charter.** Las compras y salidas de almacén no se relacionan sistemáticamente con las recetas y las ventas, lo que dificulta conocer el consumo real de los insumos y ocasiona mermas y compras de emergencia.


## 2.4 E4 — Garantizar la continuidad del servicio

**Contexto del problema.** A las tres causas se suma la inestabilidad de la conexión a Internet, que puede afectar la continuidad de las ventas y el registro de las operaciones. Lo sostiene además la norma ISO/IEC 25010 § 4.2.3, tolerancia a fallos, citada en el charter.


## 2.5 E5 — Supervisar la operación

**Soporte del control.** No es una causa, sino la condición para que exista «control integrado y sin trazabilidad»: quién puede hacer qué, qué pasó y cómo va el negocio. Lo exige además la Ley N.° 30096 citada en el charter, que obliga a control de acceso por roles y a una bitácora inmutable.


# 3. Historias de usuario


## 3.1 E1 — Registrar los pedidos de venta

Prioridad 1 de 5 · 14 historias, de US-01 a US-14.

:::: {.historia title="US-01 — Operar el POS con una sola mano"}

**E1 — Registrar los pedidos de venta** · Prioridad **Must** · Release **MVP** · Estimación **M**

**Actor:** Mozo

> *Como mozo,*  
> *quiero que los botones y el teclado del POS sean grandes y se manejen con una sola mano,*  
> *para tomar los pedidos de pie y con prisa sin equivocarme.*

- **Descripción funcional:** conjunto de componentes compartidos del POS —teclado numérico, botón, modal y formato de moneda— con las medidas de diseño táctil del sistema: objetivo mínimo de 56 px, contraste suficiente y teclado en la mitad inferior en pantallas de celular, alcanzable con el pulgar.
- **Reglas de negocio asociadas:** RN-058.
- **Criterios de aceptación:**
  - **CA1 — Tamaño mínimo del objetivo táctil**
    - Given cualquier pantalla del POS
    - When se mide un control que el mozo debe tocar
    - Then su área activa no debe ser menor a 56 px por lado.
  - **CA2 — Alcance con el pulgar**
    - Given el POS abierto en un celular
    - When aparece el teclado numérico
    - Then debe mostrarse en la mitad inferior de la pantalla.
  - **CA3 — Un solo teclado para cuatro pantallas**
    - Given el PIN del mozo, el DNI y el PIN del kiosco, las cantidades del pedido y el conteo de efectivo
    - When se revisa su implementación
    - Then las cuatro deben usar el mismo componente de teclado numérico.
- **Dependencias:** ninguna funcional.
- **Datos o entidades involucradas:** ninguna; es una capacidad de la interfaz.
- **Riesgos, supuestos o pendientes:** la medida de 56 px es una decisión del equipo (ISO/IEC 25010 § 4.2.4, operabilidad); la especificación funcional solo exige que el POS sea responsive.
- **Trazabilidad:** `03-pos-caja-cocina-delivery.md` § POS y mesas; decisión técnica DT-028.
- **Estimación inicial:** M

::::

:::: {.historia title="US-02 — Programar el menú semanal del almuerzo"}

**E1 — Registrar los pedidos de venta** · Prioridad **Must** · Release **MVP** · Estimación **L**

**Actor:** Administrador central

> *Como administrador central,*  
> *quiero programar una semana completa de almuerzos por día y servicio con sus opciones y porciones estimadas,*  
> *para que el POS sepa exactamente qué se ofrece cada día sin alterar el catálogo maestro.*

- **Descripción funcional:** Creación de la semana de menú con sus días, y por cada día y servicio las opciones clasificadas por componente (entrada, segundo, guarnición, refresco, postre) con porciones estimadas. Estados: borrador, confirmado y en servicio. El menú confirmado sigue siendo editable sin modificar el catálogo maestro. El sistema advierte, sin bloquear, si un segundo o guiso se repite respecto de los tres días anteriores. El menú solo se publica dentro del POS.
- **Reglas de negocio asociadas:** RN-039, RN-040, RN-041, RN-042, RN-001.
- **Criterios de aceptación:**
  - **CA1 — Registro de opciones del día**
    - Given que la semana de menú se encuentra en estado borrador
    - When el usuario autorizado registra las opciones de entrada, segundo, guarnición, refresco y postre para un día y servicio
    - Then el sistema debe guardar la configuración asociada a ese día y servicio.
  - **CA2 — Porciones estimadas**
    - Given una opción de menú registrada
    - When el usuario indica las porciones estimadas
    - Then el sistema debe guardarlas junto con la opción.
  - **CA3 — Advertencia por repetición**
    - Given que se está programando un segundo o guiso para un día
    - When el producto seleccionado aparece en cualquiera de los tres días anteriores
    - Then el sistema debe mostrar una advertencia
    - And debe permitir confirmar la programación de todos modos.
  - **CA4 — Transición de estados**
    - Given una semana en borrador
    - When el usuario la confirma
    - Then el estado debe pasar a confirmado
    - And el menú debe quedar disponible para el POS.
  - **CA5 — Edición del menú confirmado**
    - Given una semana confirmada
    - When el usuario modifica una opción
    - Then el sistema debe aplicar el cambio al menú programado
    - And no debe modificar el catálogo maestro de productos.
  - **CA6 — Publicación solo interna**
    - Given un menú confirmado o en servicio
    - When se revisan los canales de publicación
    - Then el sistema debe exponerlo únicamente dentro del POS y no publicarlo de forma pública.
  - **CA7 — Desayuno y cena sin programación semanal**
    - Given los servicios de desayuno y cena
    - When se accede a la programación semanal
    - Then el sistema no debe exigir programarlos
    - And debe permitir elegir sus productos fijos individualmente en el pedido.
- **Dependencias:** US-33, US-03.
- **Datos o entidades involucradas:** SemanaMenu, DiaMenu, OpcionMenu (componente, servicio, porciones estimadas, estado, disponibilidad por local), Producto.
- **Riesgos, supuestos o pendientes:** la transición al estado "en servicio" no está documentada (manual o automática por horario) — ver RF-025.
- **Trazabilidad:** `04-menu-recetas-inventario-compras.md` §§ Programación semanal, Almuerzo; `06-modelo-de-datos.md` § Menú y producción; `README.md` § Alcance confirmado.
- **Estimación inicial:** L

::::

:::: {.historia title="US-03 — Configurar el menú normal y el menú extra"}

**E1 — Registrar los pedidos de venta** · Prioridad **Must** · Release **MVP** · Estimación **M**

**Actor:** Administrador central

> *Como administrador central,*  
> *quiero definir la composición y el precio del menú normal y del menú extra,*  
> *para que el cobro sea consistente sin importar qué opciones elija el cliente.*

- **Descripción funcional:** Definición del producto vendible "menú normal" (entrada + segundo con guarnición + refresco + postre) con precio único independiente de las opciones elegidas, y del "menú extra", que es un menú completo cuyo segundo es especial y tiene precio propio.
- **Reglas de negocio asociadas:** RN-037, RN-023, RN-001.
- **Criterios de aceptación:**
  - **CA1 — Composición del menú normal**
    - Given la configuración del menú normal
    - When se revisa su composición
    - Then el sistema debe considerar entrada, segundo con guarnición, refresco y postre.
  - **CA2 — Precio único**
    - Given un menú normal vendido
    - When el cliente elige cualquier combinación de opciones programadas
    - Then el sistema debe cobrar el mismo precio único.
  - **CA3 — Menú extra con precio propio**
    - Given un menú extra
    - When se vende
    - Then el sistema debe cobrar su precio propio completo
    - And no debe aplicar la tarifa reducida del plan del pensionista.
- **Dependencias:** US-33, US-37.
- **Datos o entidades involucradas:** Producto vendible (menú normal, menú extra), componentes, precio.
- **Riesgos, supuestos o pendientes:** **Pendiente de definición** — los precios definitivos no han sido proporcionados y los importes de los documentos son ejemplos (`README.md` § Convenciones, `07` § Pendientes funcionales). Ver RF-002.
- **Trazabilidad:** `04-menu-recetas-inventario-compras.md` § Almuerzo; `02-pensionistas.md` § Planes tarifarios; `README.md` § Convenciones.
- **Estimación inicial:** M

::::

:::: {.historia title="US-04 — Marcar productos agotados y reactivarlos"}

**E1 — Registrar los pedidos de venta** · Prioridad **Must** · Release **MVP** · Estimación **S**

**Actor:** Cocina / Administrador de local

> *Como personal de cocina o administrador de local,*  
> *quiero marcar manualmente un producto como agotado y luego reactivarlo,*  
> *para que los mozos dejen de ofrecerlo sin afectar las comandas ya emitidas.*

- **Descripción funcional:** Marcado manual de disponibilidad por local. Un producto agotado se oculta en pedidos nuevos. Las comandas ya emitidas no se modifican. La reactivación registra usuario, hora y motivo.
- **Reglas de negocio asociadas:** RN-066, RN-002, RN-004.
- **Criterios de aceptación:**
  - **CA1 — Marcado de agotado**
    - Given un producto disponible en un local
    - When cocina o administración lo marca como agotado
    - Then el sistema debe ocultarlo en los pedidos nuevos de ese local.
  - **CA2 — Comandas existentes intactas**
    - Given comandas ya emitidas que incluyen ese producto
    - When el producto se marca como agotado
    - Then el sistema no debe modificar ni cancelar esas comandas.
  - **CA3 — Reactivación auditada**
    - Given un producto marcado como agotado
    - When se reactiva
    - Then el sistema debe registrar usuario, hora y motivo
    - And debe volver a mostrarlo en los pedidos nuevos.
  - **CA4 — Alcance por local**
    - Given un producto agotado en un local
    - When se consulta la disponibilidad en otro local
    - Then el sistema debe mostrarlo disponible si allí no fue marcado.
- **Dependencias:** US-33, US-57, US-06 (POS que consume la disponibilidad).
- **Datos o entidades involucradas:** Producto, OpcionMenu (disponibilidad por local), Local, registro de auditoría.
- **Riesgos, supuestos o pendientes:** ninguno documentado.
- **Trazabilidad:** `03-pos-caja-cocina-delivery.md` § Disponibilidad; `06-modelo-de-datos.md` § Menú y producción; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (8).
- **Estimación inicial:** S

::::

:::: {.historia title="US-05 — Visualizar y operar el mapa de mesas"}

**E1 — Registrar los pedidos de venta** · Prioridad **Must** · Release **MVP** · Estimación **M**

**Actor:** Mozo / Cajero / Administrador de local

> *Como mozo,*  
> *quiero ver el mapa de mesas de mi local con su estado actual,*  
> *para saber de un vistazo dónde atender y qué mesas están pendientes de cierre.*

- **Descripción funcional:** Mapa de mesas por local, accesible desde una PWA responsive para celular, tablet y terminal fija, con los estados libre, ocupada, unida, pendiente de cierre y bloqueada.
- **Reglas de negocio asociadas:** RN-058, RN-059, RN-002.
- **Criterios de aceptación:**
  - **CA1 — Estados visibles**
    - Given un local con mesas configuradas
    - When el mozo abre el mapa de mesas
    - Then el sistema debe mostrar cada mesa en uno de los estados libre, ocupada, unida, pendiente de cierre o bloqueada.
  - **CA2 — Alcance por local**
    - Given un usuario asignado a un local
    - When abre el mapa de mesas
    - Then el sistema debe mostrar únicamente las mesas de ese local.
  - **CA3 — Responsive**
    - Given el POS abierto en celular, tablet o terminal fija
    - When se usa el mapa de mesas
    - Then la interfaz debe ser operable en los tres formatos.
- **Dependencias:** US-52, US-56, US-53/US-54.
- **Datos o entidades involucradas:** Mesa, Local, Pedido.
- **Riesgos, supuestos o pendientes:** la regla de negocio para el estado "bloqueada" (quién y cuándo bloquea una mesa) no está documentada — ver RF-028. La cantidad de mesas por local está pendiente (`07`) — ver RF-010.
- **Trazabilidad:** `03-pos-caja-cocina-delivery.md` § POS y mesas; `06-modelo-de-datos.md` § Organización; `README.md` § Alcance confirmado.
- **Estimación inicial:** M

::::

:::: {.historia title="US-06 — Tomar el pedido asignando productos por persona"}

**E1 — Registrar los pedidos de venta** · Prioridad **Must** · Release **MVP** · Estimación **XL**

**Actor:** Mozo

> *Como mozo,*  
> *quiero registrar el pedido asignando cada producto a una persona de la mesa,*  
> *para poder cobrar correctamente mesas mixtas con clientes regulares y varios pensionistas.*

- **Descripción funcional:** Toma de pedido sobre una mesa o canal, creando cuentas por persona y su detalle. Permite mesas mixtas con clientes regulares y pensionistas. Muestra solo productos disponibles y las opciones programadas del día. Para llevar, recojo y delivery añade el cargo de táper por cada menú. Registra observaciones y tipo de entrega.
- **Reglas de negocio asociadas:** RN-060, RN-038, RN-009, RN-066, RN-042, RN-058, RN-008.
- **Criterios de aceptación:**
  - **CA1 — Asignación por persona**
    - Given una mesa con varios comensales
    - When el mozo registra los productos
    - Then el sistema debe permitir asignar cada producto a una persona identificada de la mesa.
  - **CA2 — Mesa mixta**
    - Given una mesa con clientes regulares y uno o más pensionistas
    - When se toma el pedido
    - Then el sistema debe separar correctamente los consumos de cada uno desde la toma del pedido.
  - **CA3 — Registro de la opción elegida**
    - Given un componente del menú con varias opciones programadas
    - When el mozo informa las opciones y registra la elegida
    - Then el sistema debe guardarla en el detalle
    - And cuando solo exista una opción debe asignarla directamente.
  - **CA4 — Producto agotado oculto**
    - Given un producto marcado como agotado en el local
    - When el mozo abre la lista de productos para un pedido nuevo
    - Then el sistema no debe ofrecerlo.
  - **CA5 — Cargo de táper**
    - Given un pedido en canal para llevar, recojo programado o delivery
    - When se registra un menú
    - Then el sistema debe añadir un cargo de táper por cada menú.
  - **CA6 — Observaciones y tipo de entrega**
    - Given un producto en el pedido
    - When el mozo agrega una observación y define el tipo de entrega
    - Then el sistema debe conservarlas para la comanda.
- **Dependencias:** US-05, US-02, US-03, US-04, US-15, US-33.
- **Datos o entidades involucradas:** Pedido (local, canal, turno, usuario), CuentaPersona, DetallePedido (producto, componentes seleccionados, precio, snapshot de costo/receta), Mesa, Pensionista, Cliente regular.
- **Riesgos, supuestos o pendientes:** **Pendiente de definición** — el precio del táper por menú (`07`). No existe ficha documentada del cliente regular (qué datos se registran) — ver RF-017. Las reglas de cortesía y de productos compartidos están pendientes (`07`) — ver RF-008.
- **Trazabilidad:** `03-pos-caja-cocina-delivery.md` § POS y mesas; `04-menu-recetas-inventario-compras.md` § Almuerzo; `01-alcance-y-reglas.md` § Canales; `06-modelo-de-datos.md` § Venta; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (7).
- **Estimación inicial:** XL

::::

:::: {.historia title="US-07 — Unir, mover, dividir y trasladar cuentas y productos"}

**E1 — Registrar los pedidos de venta** · Prioridad **Must** · Release **MVP** · Estimación **L**

**Actor:** Mozo / Cajero

> *Como mozo,*  
> *quiero mover pedidos, unir mesas, dividir cuentas por productos y trasladar productos entre cuentas,*  
> *para adaptarme a cómo se reacomodan los comensales sin perder el detalle del consumo.*

- **Descripción funcional:** Operaciones de reorganización sobre pedidos y cuentas: mover un pedido a otra mesa, unir mesas, dividir la cuenta por productos y trasladar productos entre cuentas de personas, manteniendo la trazabilidad del detalle y del snapshot económico.
- **Reglas de negocio asociadas:** RN-059, RN-060, RN-005.
- **Criterios de aceptación:**
  - **CA1 — Mover pedido**
    - Given un pedido asociado a una mesa
    - When el mozo lo mueve a otra mesa libre
    - Then el sistema debe reasignar el pedido y actualizar los estados de ambas mesas.
  - **CA2 — Unir mesas**
    - Given dos mesas ocupadas
    - When el mozo las une
    - Then el sistema debe marcarlas como unidas y permitir operar sus consumos en conjunto.
  - **CA3 — Dividir por productos**
    - Given una cuenta con varios productos
    - When el mozo la divide por productos
    - Then el sistema debe generar las cuentas resultantes conservando el detalle y el precio de cada producto.
  - **CA4 — Trasladar producto entre cuentas**
    - Given un producto asignado a una persona
    - When se traslada a la cuenta de otra persona
    - Then el sistema debe reflejar el cambio en ambas cuentas antes del cierre.
  - **CA5 — Consumo de pensionista**
    - Given un producto vinculado a la cuenta de un pensionista
    - When se traslada a otra persona
    - Then el sistema debe recalcular a qué cuenta corresponde el cargo al momento del cierre.
- **Dependencias:** US-06, US-05.
- **Datos o entidades involucradas:** Pedido, CuentaPersona, DetallePedido, Mesa.
- **Riesgos, supuestos o pendientes:** las reglas de productos compartidos no están definidas (`07`) — ver RF-008. **Supuesto de análisis** en CA5: la especificación no detalla el recálculo del cargo al trasladar productos entre un pensionista y un cliente regular; debe confirmarse.
- **Trazabilidad:** `03-pos-caja-cocina-delivery.md` § POS y mesas; `06-modelo-de-datos.md` § Venta; `01-alcance-y-reglas.md` § Reglas de historial.
- **Estimación inicial:** L

::::

:::: {.historia title="US-08 — Emitir la comanda a la impresora central de cocina"}

**E1 — Registrar los pedidos de venta** · Prioridad **Must** · Release **MVP** · Estimación **L**

**Actor:** Mozo / Cocina

> *Como mozo,*  
> *quiero enviar la comanda a la impresora central de cocina con todo el detalle,*  
> *para que la cocina prepare exactamente lo pedido por cada persona.*

- **Descripción funcional:** Impresión de la comanda en la única impresora central de cocina con pedido/mesa/canal, mozo, hora, productos por persona, componentes, observaciones y tipo de entrega. Toda reimpresión se marca como copia. La cocina avisa el pedido listo de forma verbal o por campana, sin cambio digital de estado. La emisión debe funcionar sin Internet.
- **Reglas de negocio asociadas:** RN-062, RN-063, RN-064, RN-086.
- **Criterios de aceptación:**
  - **CA1 — Contenido de la comanda**
    - Given un pedido registrado
    - When el mozo lo comanda
    - Then la impresión debe incluir pedido/mesa/canal, mozo, hora, productos por persona, componentes, observaciones y tipo de entrega.
  - **CA2 — Impresora única**
    - Given el local en operación
    - When se emiten comandas de cualquier canal
    - Then todas deben dirigirse a la única impresora central de cocina.
  - **CA3 — Reimpresión marcada**
    - Given una comanda ya impresa
    - When se reimprime
    - Then el sistema debe marcar la impresión como copia
    - And debe registrar la reimpresión.
  - **CA4 — Sin estado digital de listo**
    - Given una comanda impresa
    - When cocina termina la preparación
    - Then el sistema no debe exigir un cambio digital de estado.
  - **CA5 — Impresión sin Internet**
    - Given un local sin conexión a Internet
    - When el mozo comanda un pedido
    - Then el sistema debe imprimir la comanda normalmente.
- **Dependencias:** US-06, US-48.
- **Datos o entidades involucradas:** Comanda, DetalleComanda, registro de impresiones, Pedido, CuentaPersona.
- **Riesgos, supuestos o pendientes:** **Pendiente de definición** — el modelo de la impresora central (`07`); la impresión debe probarse con el hardware real (`07` § Validaciones externas). Ver RF-012 y RF-016.
- **Trazabilidad:** `03-pos-caja-cocina-delivery.md` § Comandas; `06-modelo-de-datos.md` § Venta; `05-usuarios-reportes-multilocal-offline.md` § Offline-first.
- **Estimación inicial:** L

::::

:::: {.historia title="US-09 — Anular productos antes y después de comandar"}

**E1 — Registrar los pedidos de venta** · Prioridad **Must** · Release **MVP** · Estimación **M**

**Actor:** Mozo / Cajero con permiso de anular

> *Como usuario autorizado,*  
> *quiero anular un producto del pedido indicando el motivo,*  
> *para no cobrarlo al cliente y que el inventario refleje si el insumo ya se consumió.*

- **Descripción funcional:** Anulación de productos del pedido con efecto diferenciado en inventario: antes de comandar no descuenta; después de comandar no se cobra pero descuenta la receta como consumo o pérdida. Requiere permiso específico y motivo, y queda auditada y reportada.
- **Reglas de negocio asociadas:** RN-065, RN-072, RN-004, RN-057.
- **Criterios de aceptación:**
  - **CA1 — Anulación antes de comandar**
    - Given un producto aún no comandado
    - When se anula
    - Then el sistema no debe cobrarlo
    - And no debe descontar inventario.
  - **CA2 — Anulación después de comandar**
    - Given un producto ya comandado
    - When se anula
    - Then el sistema no debe cobrarlo al cliente
    - And debe descontar la receta del inventario como consumo o pérdida.
  - **CA3 — Permiso y motivo**
    - Given un usuario sin permiso de anular
    - When intenta anular un producto
    - Then el sistema debe impedirlo
    - And cuando el usuario sí tiene permiso, debe exigir el motivo antes de confirmar.
  - **CA4 — Auditoría y reporte**
    - Given una anulación confirmada
    - When se consulta la auditoría o el reporte de productos anulados después de comanda
    - Then el sistema debe mostrar usuario, fecha, hora, local, motivo y el producto anulado.
- **Dependencias:** US-08, US-38/US-39 (inventario), US-36 (motivos), US-56, US-57.
- **Datos o entidades involucradas:** DetallePedido, Comanda, MovimientoInventario (origen venta/ajuste), motivo de anulación, registro de auditoría.
- **Riesgos, supuestos o pendientes:** los motivos iniciales de anulación están pendientes — ver RF-005.
- **Trazabilidad:** `03-pos-caja-cocina-delivery.md` §§ Comandas, Caja; `05-usuarios-reportes-multilocal-offline.md` § Reportes mínimos; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (9).
- **Estimación inicial:** M

::::

:::: {.historia title="US-10 — Registrar pedidos de recojo programado"}

**E1 — Registrar los pedidos de venta** · Prioridad **Should** · Release **Post-MVP** · Estimación **L**

**Actor:** Cajero / Mozo

> *Como cajero,*  
> *quiero registrar manualmente en el POS un pedido de recojo con fecha y hora futura,*  
> *para preparar el pedido a tiempo y cobrarlo según el tipo de cliente.*

- **Descripción funcional:** Registro manual en POS de pedidos recibidos por WhatsApp, guardando fecha y hora futura, nombre y celular de quien recoge. El cliente regular paga por anticipado antes de confirmar; al pensionista se le carga al cerrar el pedido. Se añade táper por cada menú. La cancelación es sin cobro si se hace antes de enviar la comanda.
- **Reglas de negocio asociadas:** RN-076, RN-077, RN-078, RN-009, RN-015.
- **Criterios de aceptación:**
  - **CA1 — Datos del recojo**
    - Given un pedido de recojo
    - When se registra en el POS
    - Then el sistema debe guardar fecha y hora futura, nombre y celular de quien recoge.
  - **CA2 — Pago anticipado del cliente regular**
    - Given un pedido de recojo de un cliente regular
    - When se intenta confirmar sin pago
    - Then el sistema debe impedir la confirmación hasta registrar el pago anticipado.
  - **CA3 — Cargo del pensionista al cierre**
    - Given un pedido de recojo de un pensionista
    - When se cierra el pedido
    - Then el sistema debe generar el cargo en su cuenta corriente en ese momento y no antes.
  - **CA4 — Táper por menú**
    - Given un pedido de recojo con uno o más menús
    - When se calcula el total
    - Then el sistema debe añadir un cargo de táper por cada menú.
  - **CA5 — Cancelación sin cobro**
    - Given un pedido de recojo aún no comandado
    - When se cancela
    - Then el sistema no debe generar cobro.
- **Dependencias:** US-06, US-19, US-17.
- **Datos o entidades involucradas:** PedidoRecojo (fecha/hora, persona que recoge, celular, estado de preparación/retiro), Pedido, Pago, CuentaPensionista.
- **Riesgos, supuestos o pendientes:** **Pendiente de definición** — el precio del táper (`07`). La integración automática con WhatsApp está excluida: se confirma que el registro es manual — ver RF-026.
- **Trazabilidad:** `03-pos-caja-cocina-delivery.md` § Recojo programado; `01-alcance-y-reglas.md` § Canales; `06-modelo-de-datos.md` § Delivery y recojo; `README.md` § Exclusiones confirmadas.
- **Estimación inicial:** L

::::

:::: {.historia title="US-11 — Gestionar el recojo preparado y no retirado"}

**E1 — Registrar los pedidos de venta** · Prioridad **Should** · Release **Post-MVP** · Estimación **M**

**Actor:** Cajero / Administrador de local

> *Como cajero,*  
> *quiero registrar cuando un pedido preparado no fue recogido,*  
> *para que el inventario refleje la merma y el cobro se aplique según lo definido para cada tipo de cliente.*

- **Descripción funcional:** Marcado de un pedido de recojo ya preparado como no recogido, con descuento de inventario. Al cliente regular no se le devuelve el pago; al pensionista se le carga el pedido completo y se emite el comprobante.
- **Reglas de negocio asociadas:** RN-079, RN-035, RN-057.
- **Criterios de aceptación:**
  - **CA1 — Registro de no recogido**
    - Given un pedido de recojo ya preparado
    - When pasa el momento del retiro y no se recoge
    - Then el sistema debe permitir registrarlo como no recogido
    - And debe descontar el inventario correspondiente.
  - **CA2 — Cliente regular**
    - Given un pedido no recogido de un cliente regular con pago anticipado
    - When se registra como no recogido
    - Then el sistema no debe devolver el pago.
  - **CA3 — Pensionista**
    - Given un pedido no recogido de un pensionista
    - When se registra como no recogido
    - Then el sistema debe cargar el pedido completo a su cuenta
    - And debe emitir el comprobante correspondiente.
- **Dependencias:** US-10, US-38, US-27, US-17.
- **Datos o entidades involucradas:** PedidoRecojo (estado de retiro), MovimientoInventario, MovimientoCuenta, ComprobanteElectronico.
- **Riesgos, supuestos o pendientes:** ninguno documentado.
- **Trazabilidad:** `03-pos-caja-cocina-delivery.md` § Recojo programado; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (13, 14).
- **Estimación inicial:** M

::::

:::: {.historia title="US-12 — Registrar el pedido de delivery y asignar motorizado"}

**E1 — Registrar los pedidos de venta** · Prioridad **Should** · Release **Post-MVP** · Estimación **L**

**Actor:** Cajero / Administrador de local

> *Como cajero,*  
> *quiero registrar manualmente el pedido de delivery, aceptar la dirección y asignar un motorizado,*  
> *para despachar el pedido y poder seguir su estado.*

- **Descripción funcional:** Registro manual del pedido recibido por WhatsApp, con dirección y teléfono. El delivery es propio, inicialmente gratuito y sin zonas: caja decide si acepta la dirección. Administración o caja asigna el motorizado y controla los estados del pedido.
- **Reglas de negocio asociadas:** RN-080, RN-081, RN-009, RN-002.
- **Criterios de aceptación:**
  - **CA1 — Registro manual con dirección**
    - Given un pedido de delivery recibido por WhatsApp
    - When el cajero lo registra en el POS
    - Then el sistema debe guardar cliente, dirección y teléfono.
  - **CA2 — Aceptación de la dirección**
    - Given una dirección ingresada
    - When el cajero decide si la acepta
    - Then el sistema debe permitir aceptarla o rechazarla sin aplicar zonas ni cobro de envío.
  - **CA3 — Asignación de motorizado**
    - Given un pedido de delivery en estado pendiente
    - When administración o caja asigna un motorizado
    - Then el estado debe pasar a asignado y el pedido debe quedar visible para ese motorizado.
  - **CA4 — Táper por menú**
    - Given un pedido de delivery con uno o más menús
    - When se calcula el total
    - Then el sistema debe añadir un cargo de táper por cada menú.
  - **CA5 — Sin envío tarifado**
    - Given un pedido de delivery
    - When se calcula el total
    - Then el sistema no debe añadir un costo de envío.
- **Dependencias:** US-06, US-55 (usuarios motorizados), US-52.
- **Datos o entidades involucradas:** PedidoDelivery (cliente, dirección, motorizado, estados con timestamps), HistorialDelivery, Pedido.
- **Riesgos, supuestos o pendientes:** no se documenta si el cliente regular de delivery paga por anticipado o contra entrega (a diferencia del recojo, donde sí se exige anticipado) — ver RF-020. La ficha del cliente regular no está documentada — ver RF-017.
- **Trazabilidad:** `03-pos-caja-cocina-delivery.md` § Delivery propio; `01-alcance-y-reglas.md` § Canales; `06-modelo-de-datos.md` § Delivery y recojo.
- **Estimación inicial:** L

::::

:::: {.historia title="US-13 — Operar la vista móvil del motorizado"}

**E1 — Registrar los pedidos de venta** · Prioridad **Should** · Release **Post-MVP** · Estimación **L**

**Actor:** Motorizado

> *Como motorizado,*  
> *quiero ver mis pedidos asignados con su dirección y teléfono y cambiar su estado,*  
> *para entregar correctamente y que administración sepa dónde está cada pedido.*

- **Descripción funcional:** Vista web móvil para el motorizado con sus pedidos asignados, dirección y teléfono del cliente, y cambio de estados: aceptado, recogido, en ruta, entregado y fallido. Comparte la última ubicación mientras está en ruta. La entrega se confirma solo marcando "entregado".
- **Reglas de negocio asociadas:** RN-081, RN-082, RN-083, RN-088.
- **Criterios de aceptación:**
  - **CA1 — Listado de pedidos asignados**
    - Given un motorizado con pedidos asignados
    - When abre su vista móvil
    - Then el sistema debe mostrar sus pedidos con dirección y teléfono.
  - **CA2 — Transición de estados**
    - Given un pedido asignado
    - When el motorizado cambia su estado
    - Then el sistema debe registrar la transición entre pendiente, asignado, aceptado, recogido, en ruta, entregado y fallido con su marca de tiempo.
  - **CA3 — Confirmación de entrega**
    - Given un pedido en ruta
    - When el motorizado lo marca como entregado
    - Then el sistema debe considerar la entrega confirmada
    - And no debe considerarla confirmada por ningún otro estado.
  - **CA4 — Última ubicación**
    - Given un pedido en estado en ruta
    - When el motorizado se desplaza
    - Then el sistema debe compartir su última ubicación mientras dure ese estado.
  - **CA5 — Sin comisiones**
    - Given la liquidación del motorizado
    - When se calcula su pago
    - Then el sistema no debe calcular comisiones por entrega.
  - **CA6 — Ubicación indisponible**
    - Given una caída de conexión
    - When la ubicación remota no está disponible
    - Then la operación del local debe continuar sin verse afectada.
- **Dependencias:** US-12, US-54/US-53 (acceso del motorizado).
- **Datos o entidades involucradas:** PedidoDelivery, HistorialDelivery (cambios de estado, ubicación, incidencias), Usuario motorizado.
- **Riesgos, supuestos o pendientes:** la optimización automática de rutas está excluida (`README.md`).
- **Trazabilidad:** `03-pos-caja-cocina-delivery.md` § Delivery propio; `05-usuarios-reportes-multilocal-offline.md` § Offline-first; `06-modelo-de-datos.md` § Delivery y recojo; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (15).
- **Estimación inicial:** L

::::

:::: {.historia title="US-14 — Liquidar al motorizado y cerrar la venta de delivery"}

**E1 — Registrar los pedidos de venta** · Prioridad **Should** · Release **Post-MVP** · Estimación **M**

**Actor:** Cajero

> *Como cajero,*  
> *quiero revisar y liquidar lo cobrado por cada motorizado antes de cerrar sus ventas,*  
> *para controlar el dinero entregado y registrar las incidencias.*

- **Descripción funcional:** Revisión y liquidación de los pedidos entregados por motorizado: control del dinero cobrado, registro de incidencias y cierre de la venta en caja después de la liquidación.
- **Reglas de negocio asociadas:** RN-084, RN-081, RN-083.
- **Criterios de aceptación:**
  - **CA1 — Venta no cerrada antes de liquidar**
    - Given un pedido marcado como entregado
    - When aún no se ha liquidado con el motorizado
    - Then el sistema no debe cerrar la venta.
  - **CA2 — Liquidación**
    - Given los pedidos entregados de un motorizado
    - When el cajero revisa y liquida el dinero cobrado
    - Then el sistema debe registrar el importe liquidado por motorizado
    - And debe permitir cerrar las ventas correspondientes.
  - **CA3 — Incidencias**
    - Given un pedido con incidencia o estado fallido
    - When el cajero la registra
    - Then el sistema debe asociarla al motorizado y al pedido.
- **Dependencias:** US-13, US-19, US-22.
- **Datos o entidades involucradas:** PedidoDelivery, HistorialDelivery (incidencias), Pago, TurnoCaja, MovimientoCaja.
- **Riesgos, supuestos o pendientes:** ver RF-020 sobre el momento de pago del cliente regular en delivery.
- **Trazabilidad:** `03-pos-caja-cocina-delivery.md` § Delivery propio; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (15).
- **Estimación inicial:** M

::::


## 3.2 E2 — Controlar la cobranza

Prioridad 2 de 5 · 18 historias, de US-15 a US-32.

:::: {.historia title="US-15 — Registrar la ficha del pensionista y su responsable"}

**E2 — Controlar la cobranza** · Prioridad **Must** · Release **MVP** · Estimación **M**

**Actor:** Administrador central / Administrador de local

> *Como administrador,*  
> *quiero registrar la ficha completa del pensionista junto con su responsable obligatorio,*  
> *para poder abrirle una cuenta corriente identificada y contactable.*

- **Descripción funcional:** Alta y mantenimiento del pensionista con sus datos personales, académicos, plan tarifario, límite de crédito, frecuencia habitual de abono, PIN presencial, contraseña del portal y estado. El responsable es obligatorio. El DNI del pensionista es único en la empresa.
- **Reglas de negocio asociadas:** RN-036, RN-090, RN-007, RN-021.
- **Criterios de aceptación:**
  - **CA1 — Campos del pensionista**
    - Given el alta de un pensionista
    - When se guarda la ficha
    - Then el sistema debe registrar DNI, nombres y apellidos, teléfono, correo, universidad o instituto, código de estudiante, carrera, ciclo o semestre opcional, plan tarifario, límite de crédito, frecuencia habitual de abono, PIN presencial, contraseña del portal y estado activo/inactivo.
  - **CA2 — Responsable obligatorio**
    - Given el alta de un pensionista
    - When se intenta guardar sin responsable
    - Then el sistema debe impedir el registro
    - And debe exigir nombre completo, DNI, teléfono, correo, parentesco o relación y dirección del responsable.
  - **CA3 — DNI único**
    - Given un DNI ya registrado para otro pensionista de la empresa
    - When se intenta registrar de nuevo
    - Then el sistema debe rechazar el alta.
  - **CA4 — Credenciales separadas y no legibles**
    - Given la ficha guardada
    - When se consulta
    - Then el sistema no debe mostrar el PIN ni la contraseña del portal en texto legible
    - And debe mantenerlos como credenciales distintas entre sí.
  - **CA5 — Apertura de cuenta**
    - Given un pensionista dado de alta
    - When se confirma el registro
    - Then el sistema debe crear su cuenta corriente asociada.
- **Dependencias:** US-16 (plan tarifario), US-56.
- **Datos o entidades involucradas:** Pensionista, Responsable, CuentaPensionista, PlanTarifario.
- **Riesgos, supuestos o pendientes:** los campos opcionales adicionales de clientes están pendientes (`07`) — ver RF-009. El proceso de asignación y recuperación de la contraseña del portal no está documentado — ver RF-027.
- **Trazabilidad:** `02-pensionistas.md` § Ficha; `06-modelo-de-datos.md` §§ Pensionistas, Reglas de integridad esenciales; `05-usuarios-reportes-multilocal-offline.md` § Respaldo y seguridad.
- **Estimación inicial:** M

::::

:::: {.historia title="US-16 — Administrar planes tarifarios y precios especiales"}

**E2 — Controlar la cobranza** · Prioridad **Must** · Release **MVP** · Estimación **L**

**Actor:** Administrador central

> *Como administrador central,*  
> *quiero configurar planes tarifarios con vigencia e historial y asignarlos a los pensionistas,*  
> *para cobrar a cada estudiante el precio que le corresponde sin alterar lo ya consumido.*

- **Descripción funcional:** Mantenimiento de planes tarifarios configurables y asignables. Cada plan define el precio del almuerzo normal y precios especiales por producto fijo de desayuno y cena. Tienen vigencia, estado e historial de modificaciones. Los consumos anteriores conservan la tarifa aplicada. El menú extra nunca usa tarifa reducida.
- **Reglas de negocio asociadas:** RN-021, RN-022, RN-023, RN-001, RN-004.
- **Criterios de aceptación:**
  - **CA1 — Definición del plan**
    - Given la creación de un plan tarifario
    - When se configura
    - Then el sistema debe permitir definir el precio del almuerzo normal y precios especiales por producto fijo de desayuno y cena.
  - **CA2 — Vigencia e historial**
    - Given un plan con precios definidos
    - When se modifica
    - Then el sistema debe conservar el historial de modificaciones y la vigencia de cada versión.
  - **CA3 — Consumos anteriores intactos**
    - Given consumos registrados con una tarifa previa
    - When el plan cambia de precio
    - Then el sistema debe conservar la tarifa aplicada en esos consumos.
  - **CA4 — Menú extra a precio completo**
    - Given un pensionista con plan tarifario asignado
    - When consume un menú extra
    - Then el sistema debe cobrar el precio completo del extra sin aplicar tarifa reducida.
  - **CA5 — Alcance central**
    - Given un plan configurado
    - When se usa en cualquier local
    - Then el sistema debe aplicar el mismo plan y precios.
- **Dependencias:** US-33, US-57.
- **Datos o entidades involucradas:** PlanTarifario, PrecioPlan (por servicio/producto), Pensionista, vigencia, estado.
- **Riesgos, supuestos o pendientes:** **Pendiente de definición** — precios definitivos, planes y vigencias (`07`). No se documenta qué precio aplica al pensionista para productos que el plan no contempla (por ejemplo extras o postres sueltos) — ver RF-019.
- **Trazabilidad:** `02-pensionistas.md` § Planes tarifarios; `06-modelo-de-datos.md` § Pensionistas; `07-criterios-de-aceptacion-y-pendientes.md` § Pendientes funcionales.
- **Estimación inicial:** L

::::

:::: {.historia title="US-17 — Mantener la cuenta corriente del pensionista"}

**E2 — Controlar la cobranza** · Prioridad **Must** · Release **MVP** · Estimación **L**

**Actor:** Administrador / Cajero (consulta y registro)

> *Como administrador,*  
> *quiero que cada pensionista tenga una cuenta corriente única con cargos, abonos, ajustes y saldos,*  
> *para conocer en todo momento su deuda, su saldo a favor y su crédito disponible en cualquier local.*

- **Descripción funcional:** Cuenta corriente única por pensionista, utilizable en todos los locales, que acumula cargos, abonos, ajustes, saldo a favor, deuda y crédito disponible. El cargo se genera únicamente al cerrar el consumo en caja. El saldo a favor se aplica primero y el remanente aumenta la deuda. El límite de crédito es preventivo. Cada movimiento identifica el local donde ocurrió.
- **Reglas de negocio asociadas:** RN-003, RN-015, RN-016, RN-017, RN-018, RN-019, RN-008.
- **Criterios de aceptación:**
  - **CA1 — Cargo solo al cerrar en caja**
    - Given un pedido de pensionista tomado y comandado
    - When aún no se cierra en caja
    - Then el sistema no debe registrar ningún cargo en su cuenta.
  - **CA2 — Cálculo del crédito disponible**
    - Given una cuenta con límite individual y deuda pendiente
    - When se consulta el crédito disponible
    - Then el sistema debe mostrar el resultado de límite individual menos deuda pendiente.
  - **CA3 — Aplicación del saldo a favor**
    - Given una cuenta con saldo a favor menor que el consumo
    - When se cierra el consumo
    - Then el sistema debe aplicar primero el saldo a favor
    - And debe aumentar la deuda solo por el remanente.
  - **CA4 — Límite preventivo**
    - Given un consumo que hace superar el límite individual
    - When el cajero cierra la cuenta
    - Then el sistema debe mostrar una alerta
    - And debe permitir completar el cierre.
  - **CA5 — Varios consumos del mismo servicio**
    - Given un pensionista que ya consumió el almuerzo del día
    - When registra un segundo consumo del mismo servicio ese mismo día
    - Then el sistema debe permitirlo y generar un cargo adicional.
  - **CA6 — Cuenta única multilocal**
    - Given un pensionista que consume en un local distinto al habitual
    - When se consulta su cuenta
    - Then el sistema debe mostrar una sola cuenta con los movimientos de todos los locales
    - And debe indicar el local de cada movimiento.
- **Dependencias:** US-15, US-16, US-19 (cierre en caja que genera el cargo).
- **Datos o entidades involucradas:** CuentaPensionista, MovimientoCuenta (consumo, abono, ajuste, arrastre), Local, límite de crédito, saldo a favor, deuda.
- **Riesgos, supuestos o pendientes:** el tratamiento tributario de las ventas a crédito debe validarse con el contador (`07` § Validaciones externas) — ver RF-014.
- **Trazabilidad:** `02-pensionistas.md` § Cuenta corriente; `01-alcance-y-reglas.md` §§ Canales, Multilocal; `06-modelo-de-datos.md` § Pensionistas; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (3, 4, 20).
- **Estimación inicial:** L

::::

:::: {.historia title="US-18 — Abrir el turno de caja con fondo inicial"}

**E2 — Controlar la cobranza** · Prioridad **Must** · Release **MVP** · Estimación **S**

**Actor:** Cajero

> *Como cajero,*  
> *quiero abrir mi turno registrando el fondo inicial,*  
> *para que todos los cobros y abonos del turno queden asociados a mí y a esa caja.*

- **Descripción funcional:** Apertura del turno de caja por caja, local y cajero, con registro del fondo inicial. Todas las operaciones de cobro y abono en efectivo del periodo se asocian al turno abierto.
- **Reglas de negocio asociadas:** RN-067, RN-002, RN-069.
- **Criterios de aceptación:**
  - **CA1 — Apertura**
    - Given un cajero con permiso
    - When abre el turno indicando el fondo inicial
    - Then el sistema debe crear el turno asociado a la caja, el local y el cajero.
  - **CA2 — Asociación de movimientos**
    - Given un turno abierto
    - When se registra un cobro o un abono en efectivo
    - Then el sistema debe asociar el movimiento a ese turno.
  - **CA3 — Solo ventas y abonos**
    - Given un turno abierto
    - When se revisan los tipos de movimiento permitidos
    - Then el sistema debe aceptar únicamente ventas y abonos en efectivo de pensionista.
- **Dependencias:** US-52, US-56, US-53.
- **Datos o entidades involucradas:** TurnoCaja (caja, local, cajero), MovimientoCaja, fondo inicial.
- **Riesgos, supuestos o pendientes:** la relación entre turnos de caja y horarios de servicio, y la cantidad de cajas por local, no están documentadas — ver RF-021.
- **Trazabilidad:** `03-pos-caja-cocina-delivery.md` § Caja; `06-modelo-de-datos.md` § Caja.
- **Estimación inicial:** S

::::

:::: {.historia title="US-19 — Cobrar la cuenta con medios divididos y cierres parciales"}

**E2 — Controlar la cobranza** · Prioridad **Must** · Release **MVP** · Estimación **XL**

**Actor:** Cajero

> *Como cajero,*  
> *quiero cerrar la cuenta dividiendo libremente el pago entre medios y permitiendo cierres parciales,*  
> *para cobrar como el cliente lo necesite sin liberar la mesa antes de tiempo.*

- **Descripción funcional:** Cierre de la cuenta en caja: cálculo del total, división libre del pago entre efectivo, Yape, Plin y tarjeta, uso del saldo del pensionista para los consumos vinculados a su cuenta, y cierres parciales sin liberar la mesa mientras haya consumos pendientes. El cierre es el evento que genera el cargo en la cuenta del pensionista, el snapshot económico de la venta, el descuento de inventario por receta y la emisión del comprobante.
- **Reglas de negocio asociadas:** RN-071, RN-061, RN-015, RN-005, RN-046, RN-016, RN-017, RN-018, RN-035, RN-047.
- **Criterios de aceptación:**
  - **CA1 — División de pago**
    - Given una cuenta lista para cobrar
    - When el cajero reparte el importe entre efectivo, Yape, Plin y tarjeta
    - Then el sistema debe aceptar la división libre siempre que la suma cubra el total.
  - **CA2 — Cierre parcial**
    - Given una mesa con varios consumos
    - When se cierra la cuenta de una persona y quedan consumos pendientes
    - Then el sistema debe cerrar solo esa cuenta
    - And no debe liberar la mesa.
  - **CA3 — Cargo al pensionista**
    - Given consumos vinculados a la cuenta de un pensionista
    - When el cajero cierra la cuenta
    - Then el sistema debe generar el cargo en su cuenta corriente aplicando primero el saldo a favor
    - And debe alertar si se supera el límite sin bloquear el cierre.
  - **CA4 — Snapshot económico**
    - Given una venta cerrada
    - When se consulta posteriormente
    - Then el sistema debe conservar precio, costo, composición, receta aplicada, utilidad y margen históricos e inmutables.
  - **CA5 — Descuento de inventario**
    - Given una venta cerrada de un plato con receta
    - When se confirma el cierre
    - Then el sistema debe descontar los insumos de su receta, incluidas las cantidades de subrecetas, sin duplicar consumos.
  - **CA6 — Sin movimientos extraordinarios**
    - Given la pantalla de caja
    - When el cajero busca registrar un gasto, retiro o ingreso extraordinario
    - Then el sistema no debe ofrecer esa opción, salvo el abono en efectivo de pensionista.
- **Dependencias:** US-18, US-06, US-17, US-37, US-38, US-27.
- **Datos o entidades involucradas:** Pedido, CuentaPersona, DetallePedido, Pago, MedioPago, TurnoCaja, MovimientoCaja, MovimientoCuenta, MovimientoInventario, ComprobanteElectronico.
- **Riesgos, supuestos o pendientes:** el precio del táper y los precios definitivos están pendientes — ver RF-002 y RF-003.
- **Trazabilidad:** `03-pos-caja-cocina-delivery.md` §§ Caja, POS y mesas; `02-pensionistas.md` §§ Cuenta corriente, Facturación; `04-menu-recetas-inventario-compras.md` §§ Recetas y costos, Producción; `06-modelo-de-datos.md` §§ Venta, Caja; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (1, 4, 11).
- **Estimación inicial:** XL

::::

:::: {.historia title="US-20 — Aplicar descuentos y anulaciones con autorización y motivo"}

**E2 — Controlar la cobranza** · Prioridad **Must** · Release **MVP** · Estimación **M**

**Actor:** Cajero / Administrador autorizado

> *Como cajero autorizado,*  
> *quiero aplicar un descuento o anular una venta indicando el motivo,*  
> *para resolver situaciones excepcionales dejando constancia de quién lo autorizó.*

- **Descripción funcional:** Aplicación de descuentos y anulaciones en caja sujeta a permisos específicos y registro obligatorio del motivo, con auditoría completa.
- **Reglas de negocio asociadas:** RN-072, RN-004, RN-012.
- **Criterios de aceptación:**
  - **CA1 — Permiso requerido**
    - Given un usuario sin el permiso correspondiente
    - When intenta aplicar un descuento o anular una venta
    - Then el sistema debe impedirlo.
  - **CA2 — Motivo obligatorio**
    - Given un usuario con permiso
    - When aplica un descuento o una anulación sin indicar motivo
    - Then el sistema debe impedir la confirmación.
  - **CA3 — Auditoría**
    - Given un descuento o anulación confirmada
    - When se consulta la auditoría
    - Then el sistema debe mostrar usuario, fecha, hora, local, valores anterior y nuevo y el motivo.
- **Dependencias:** US-19, US-56, US-36, US-57.
- **Datos o entidades involucradas:** Pedido, Pago, descuento, motivo, registro de auditoría.
- **Riesgos, supuestos o pendientes:** motivos iniciales pendientes — ver RF-005.
- **Trazabilidad:** `03-pos-caja-cocina-delivery.md` § Caja; `01-alcance-y-reglas.md` § Auditoría transversal; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (19).
- **Estimación inicial:** M

::::

:::: {.historia title="US-21 — Reabrir una venta dentro del turno o compensarla después"}

**E2 — Controlar la cobranza** · Prioridad **Must** · Release **MVP** · Estimación **M**

**Actor:** Administrador autorizado

> *Como administrador autorizado,*  
> *quiero reabrir una venta solo mientras su turno siga abierto y, si ya cerró, corregirla con una operación compensatoria,*  
> *para arreglar errores sin alterar cierres ya realizados.*

- **Descripción funcional:** Reapertura controlada de ventas cerradas con permiso específico y solo mientras el turno de origen esté abierto. Después del cierre del turno, la corrección se hace con una operación compensatoria o documento relacionado que no altera el cierre original. Todo queda auditado.
- **Reglas de negocio asociadas:** RN-073, RN-074, RN-006, RN-004.
- **Criterios de aceptación:**
  - **CA1 — Reapertura permitida**
    - Given una venta cerrada cuyo turno de origen sigue abierto
    - When un administrador autorizado la reabre
    - Then el sistema debe permitirlo y registrar el evento en la auditoría.
  - **CA2 — Reapertura impedida**
    - Given una venta cuyo turno de origen ya fue cerrado
    - When se intenta reabrirla
    - Then el sistema debe impedirlo.
  - **CA3 — Operación compensatoria**
    - Given una venta de un turno cerrado que requiere corrección
    - When el administrador registra la corrección
    - Then el sistema debe generar una operación compensatoria o documento relacionado
    - And no debe alterar el cierre original.
  - **CA4 — Permiso específico**
    - Given un usuario sin el permiso de reabrir
    - When intenta reabrir una venta
    - Then el sistema debe impedirlo.
- **Dependencias:** US-19, US-22, US-56, US-57.
- **Datos o entidades involucradas:** Pedido, TurnoCaja, CierreCaja, documento relacionado, registro de auditoría.
- **Riesgos, supuestos o pendientes:** el tratamiento de notas y documentos relacionados debe validarse con el contador — ver RF-014.
- **Trazabilidad:** `03-pos-caja-cocina-delivery.md` § Caja; `01-alcance-y-reglas.md` § Reglas de historial; `06-modelo-de-datos.md` § Reglas de integridad esenciales; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (17).
- **Estimación inicial:** M

::::

:::: {.historia title="US-22 — Cerrar el turno de caja con conciliación y alertas"}

**E2 — Controlar la cobranza** · Prioridad **Must** · Release **MVP** · Estimación **L**

**Actor:** Cajero

> *Como cajero,*  
> *quiero cerrar mi turno conciliando lo esperado con lo contado,*  
> *para dejar registrada cualquier diferencia y cerrar de forma definitiva.*

- **Descripción funcional:** Cierre del turno con conciliación de fondo inicial, cobros, abonos en efectivo y medios digitales/tarjeta. El cajero puede ver el efectivo esperado antes de contar. Cualquier diferencia desde S/0.01 genera alerta sin bloquear el cierre. El cierre conserva esperado, contado, diferencia y observación, y es inalterable.
- **Reglas de negocio asociadas:** RN-067, RN-068, RN-070, RN-006, RN-004.
- **Criterios de aceptación:**
  - **CA1 — Efectivo esperado visible**
    - Given un turno abierto
    - When el cajero inicia el cierre
    - Then el sistema debe mostrarle el efectivo esperado antes de registrar el conteo.
  - **CA2 — Conciliación completa**
    - Given el cierre en curso
    - When se calcula el resultado
    - Then el sistema debe conciliar fondo inicial, cobros, abonos en efectivo y medios digitales/tarjeta.
  - **CA3 — Alerta desde un céntimo**
    - Given una diferencia de S/0.01 entre esperado y contado
    - When el cajero confirma el conteo
    - Then el sistema debe mostrar una alerta
    - And debe permitir completar el cierre.
  - **CA4 — Contenido y cierre inalterable**
    - Given un cierre confirmado
    - When se consulta después
    - Then el sistema debe conservar esperado, contado, diferencia y observación
    - And no debe permitir su modificación.
  - **CA5 — Auditoría del cierre**
    - Given un cierre confirmado
    - When se consulta la auditoría
    - Then el sistema debe registrar usuario, fecha, hora y local.
- **Dependencias:** US-18, US-19, US-25.
- **Datos o entidades involucradas:** TurnoCaja, MovimientoCaja, CierreCaja (esperado, contado, diferencia, observación).
- **Riesgos, supuestos o pendientes:** ninguno documentado.
- **Trazabilidad:** `03-pos-caja-cocina-delivery.md` § Caja; `01-alcance-y-reglas.md` §§ Auditoría transversal, Reglas de historial; `06-modelo-de-datos.md` § Caja; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (18).
- **Estimación inicial:** L

::::

:::: {.historia title="US-23 — Emitir el pre-ticket de autoservicio del pensionista"}

**E2 — Controlar la cobranza** · Prioridad **Must** · Release **MVP** · Estimación **L**

**Actor:** Pensionista

> *Como pensionista,*  
> *quiero identificarme con mi DNI y PIN y obtener un pre-ticket impreso,*  
> *para que el mozo me atienda sin que yo tenga que elegir platos en el kiosco.*

- **Descripción funcional:** Terminal de autoservicio donde el pensionista ingresa DNI y PIN corto; el sistema determina el servicio por horario configurable e imprime un pre-ticket sin selección de platos, con código único de un solo uso. Estados: pendiente, vinculado, cerrado, vencido y anulado.
- **Reglas de negocio asociadas:** RN-024, RN-025, RN-026, RN-027, RN-094, RN-034, RN-091.
- **Criterios de aceptación:**
  - **CA1 — Emisión del pre-ticket**
    - Given un pensionista activo que ingresa DNI y PIN correctos dentro del horario de un servicio
    - When confirma
    - Then el sistema debe determinar automáticamente el servicio e imprimir un pre-ticket en estado pendiente, sin selección de platos.
  - **CA2 — Contenido obligatorio**
    - Given un pre-ticket impreso
    - When se revisa
    - Then debe mostrar código único, pensionista, servicio, fecha y hora y la leyenda de que no es un consumo cargado.
  - **CA3 — Sin efecto en la cuenta**
    - Given un pre-ticket emitido
    - When se consulta la cuenta corriente del pensionista
    - Then el sistema no debe mostrar ningún cargo asociado a ese pre-ticket.
  - **CA4 — Vencimiento**
    - Given un pre-ticket en estado pendiente
    - When termina el horario del servicio
    - Then el sistema debe marcarlo como vencido
    - And no debe generar ningún cargo.
  - **CA5 — Pensionista desactivado**
    - Given un pensionista desactivado
    - When intenta emitir un pre-ticket
    - Then el sistema debe impedirlo.
  - **CA6 — Bloqueo por intentos fallidos**
    - Given intentos fallidos consecutivos de PIN
    - When se alcanza el umbral configurado
    - Then el sistema debe bloquear temporalmente el intento.
- **Dependencias:** US-15, US-58, US-48 (debe funcionar sin Internet).
- **Datos o entidades involucradas:** PreTicket (código, pensionista, servicio, horario, estado, pedido vinculado), Pensionista, configuración de horarios.
- **Riesgos, supuestos o pendientes:** **Pendiente de definición** — dispositivo e impresora del kiosco (`07`), horarios exactos (`07`) y plazo de bloqueo por intentos fallidos (`07`). Ambigüedad: la especificación permite varios consumos del mismo servicio en un día pero no aclara si se pueden emitir varios pre-tickets pendientes del mismo servicio simultáneamente — ver RF-018.
- **Trazabilidad:** `02-pensionistas.md` § Autoservicio presencial; `06-modelo-de-datos.md` §§ Pensionistas, Reglas de integridad esenciales; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (1, 2).
- **Estimación inicial:** L

::::

:::: {.historia title="US-24 — Vincular el pre-ticket al pedido del pensionista"}

**E2 — Controlar la cobranza** · Prioridad **Must** · Release **MVP** · Estimación **M**

**Actor:** Mozo

> *Como mozo,*  
> *quiero ingresar manualmente el código del pre-ticket que me entrega el pensionista y vincularlo a su pedido,*  
> *para asociar correctamente lo que consume con su cuenta y su mesa.*

- **Descripción funcional:** El mozo recibe el papel, ingresa manualmente el código único del pre-ticket y vincula pensionista, mesa o persona y productos. El sistema impide la doble vinculación y rechaza pre-tickets vencidos o anulados.
- **Reglas de negocio asociadas:** RN-025, RN-026, RN-027, RN-094, RN-060.
- **Criterios de aceptación:**
  - **CA1 — Vinculación exitosa**
    - Given un pre-ticket en estado pendiente
    - When el mozo ingresa su código y lo asocia a la mesa o persona y a los productos elegidos
    - Then el sistema debe cambiar el estado del pre-ticket a vinculado
    - And debe asociar el consumo al pensionista correspondiente.
  - **CA2 — Doble vinculación impedida**
    - Given un pre-ticket ya vinculado
    - When se intenta usar el mismo código en otro pedido
    - Then el sistema debe rechazar la operación.
  - **CA3 — Pre-ticket vencido o anulado**
    - Given un pre-ticket vencido o anulado
    - When el mozo intenta vincularlo
    - Then el sistema debe rechazar la operación e informar el estado.
  - **CA4 — Registro de opciones**
    - Given un menú con varias opciones programadas
    - When el mozo registra la elegida por el pensionista
    - Then el sistema debe guardar esa opción en el detalle del pedido.
  - **CA5 — Estado tras el cierre**
    - Given un pre-ticket vinculado cuyo pedido se cierra en caja
    - When se confirma el cierre
    - Then el sistema debe cambiar su estado a cerrado.
- **Dependencias:** US-23, US-06, US-02.
- **Datos o entidades involucradas:** PreTicket, Pedido, CuentaPersona, DetallePedido, Pensionista, Mesa.
- **Riesgos, supuestos o pendientes:** ninguno adicional a RF-018.
- **Trazabilidad:** `02-pensionistas.md` § Autoservicio presencial; `03-pos-caja-cocina-delivery.md` § POS y mesas; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (1, 7).
- **Estimación inicial:** M

::::

:::: {.historia title="US-25 — Registrar abonos del pensionista en caja"}

**E2 — Controlar la cobranza** · Prioridad **Must** · Release **MVP** · Estimación **M**

**Actor:** Cajero

> *Como cajero,*  
> *quiero registrar los abonos que hace el pensionista o su responsable,*  
> *para actualizar su saldo y que el dinero entre correctamente en la conciliación del turno.*

- **Descripción funcional:** Registro de abonos en efectivo, Yape, Plin y transferencia bancaria. El efectivo queda confirmado de inmediato y entra en la conciliación del turno; los digitales quedan pendientes de aprobación. Se permiten abonos parciales. El abono no genera una venta.
- **Reglas de negocio asociadas:** RN-030, RN-031, RN-032, RN-033, RN-069, RN-035.
- **Criterios de aceptación:**
  - **CA1 — Abono en efectivo**
    - Given un pensionista con cuenta activa
    - When el cajero registra un abono en efectivo
    - Then el sistema debe confirmarlo inmediatamente, actualizar el saldo e incluirlo en la conciliación del turno.
  - **CA2 — Abono digital desde caja**
    - Given un abono por Yape, Plin o transferencia registrado por caja
    - When se guarda
    - Then el sistema debe dejarlo en estado pendiente
    - And no debe modificar el saldo hasta su aprobación.
  - **CA3 — Abono parcial**
    - Given una deuda pendiente
    - When se registra un abono por un monto menor a la deuda
    - Then el sistema debe aceptarlo y actualizar la deuda restante según su estado.
  - **CA4 — No genera venta**
    - Given un abono registrado
    - When se revisan las ventas del turno
    - Then el sistema no debe generar una venta por el abono
    - And puede generar una constancia interna.
  - **CA5 — Excepción de caja**
    - Given la restricción de no permitir ingresos extraordinarios en caja
    - When se registra un abono en efectivo de pensionista
    - Then el sistema debe permitirlo como única excepción documentada.
- **Dependencias:** US-17, US-18.
- **Datos o entidades involucradas:** Abono (medio, monto, estado, adjunto, aprobador), MovimientoCuenta, MovimientoCaja, TurnoCaja, MedioPago.
- **Riesgos, supuestos o pendientes:** el tratamiento tributario de abonos debe validarse con el contador — ver RF-014.
- **Trazabilidad:** `02-pensionistas.md` §§ Abonos, Facturación; `03-pos-caja-cocina-delivery.md` § Caja; `06-modelo-de-datos.md` §§ Pensionistas, Caja.
- **Estimación inicial:** M

::::

:::: {.historia title="US-26 — Aprobar o rechazar abonos digitales"}

**E2 — Controlar la cobranza** · Prioridad **Must** · Release **MVP** · Estimación **M**

**Actor:** Administrador central

> *Como administrador,*  
> *quiero revisar los abonos digitales pendientes y aprobarlos o rechazarlos,*  
> *para que solo el dinero verificado modifique el saldo del pensionista.*

- **Descripción funcional:** Bandeja de abonos pendientes con su comprobante adjunto y datos. Solo el administrador aprueba o rechaza. La aprobación actualiza el saldo; el rechazo no. Ambas acciones se auditan y notifican internamente al portal.
- **Reglas de negocio asociadas:** RN-031, RN-029, RN-004, RN-092.
- **Criterios de aceptación:**
  - **CA1 — Aprobación exclusiva del administrador**
    - Given un abono pendiente
    - When un usuario sin permiso de aprobar intenta procesarlo
    - Then el sistema debe impedirlo.
  - **CA2 — Aprobación**
    - Given un abono pendiente
    - When el administrador lo aprueba
    - Then el sistema debe actualizar el saldo del pensionista
    - And debe registrar usuario, fecha, hora y valores en la auditoría
    - And debe notificar internamente al portal.
  - **CA3 — Rechazo**
    - Given un abono pendiente
    - When el administrador lo rechaza
    - Then el sistema no debe modificar el saldo
    - And debe registrar el rechazo y notificarlo internamente.
  - **CA4 — Sin efecto mientras esté pendiente**
    - Given un abono en estado pendiente
    - When se consulta el saldo del pensionista
    - Then el saldo no debe incluir ese abono.
- **Dependencias:** US-25, US-31, US-56, US-57.
- **Datos o entidades involucradas:** Abono (estado, aprobador), CuentaPensionista, MovimientoCuenta, registro de auditoría.
- **Riesgos, supuestos o pendientes:** los motivos de rechazo de pagos están pendientes de definición — ver RF-005.
- **Trazabilidad:** `02-pensionistas.md` § Abonos; `01-alcance-y-reglas.md` § Auditoría transversal; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (5).
- **Estimación inicial:** M

::::

:::: {.historia title="US-27 — Emitir comprobantes electrónicos por proveedor API"}

**E2 — Controlar la cobranza** · Prioridad **Must** · Release **MVP** · Estimación **XL**

**Actor:** Cajero (emisión) / Administrador (consulta)

> *Como cajero,*  
> *quiero que el sistema emita el comprobante electrónico al cerrar la venta y lo reintente si no hay Internet,*  
> *para cumplir con la facturación sin detener la atención ni duplicar documentos.*

- **Descripción funcional:** Integración con un proveedor externo de facturación por API para enviar boletas, facturas y notas, guardando respuesta, estado y errores. Incluye cola offline, reintentos idempotentes, prevención de duplicados y consulta/reimpresión de la representación del comprobante. Para pensionistas se emite un comprobante por cada consumo, a su nombre y DNI.
- **Reglas de negocio asociadas:** RN-075, RN-035, RN-088, RN-087.
- **Criterios de aceptación:**
  - **CA1 — Emisión al cerrar**
    - Given una venta cerrada en caja
    - When se confirma el cierre
    - Then el sistema debe generar y enviar el comprobante electrónico correspondiente.
  - **CA2 — Comprobante del pensionista**
    - Given un consumo de pensionista cerrado en caja
    - When se emite el comprobante
    - Then debe emitirse una boleta a nombre del pensionista y con su DNI, una por cada consumo.
  - **CA3 — Cola offline y reintentos**
    - Given el proveedor de facturación no disponible o sin Internet
    - When se cierra una venta
    - Then el sistema debe encolar el comprobante y reintentar el envío de forma idempotente al restablecerse la conexión.
  - **CA4 — Prevención de duplicados**
    - Given un comprobante ya aceptado por el proveedor
    - When el reintento se ejecuta de nuevo
    - Then el sistema no debe generar un comprobante duplicado.
  - **CA5 — Registro de respuesta**
    - Given una respuesta del proveedor
    - When se recibe
    - Then el sistema debe guardar respuesta, estado y errores asociados al comprobante.
  - **CA6 — Consulta y reimpresión**
    - Given un comprobante emitido
    - When un usuario autorizado lo consulta
    - Then el sistema debe permitir ver y reimprimir su representación.
  - **CA7 — El abono no genera venta**
    - Given un abono registrado
    - When se procesa
    - Then el sistema no debe emitir un comprobante de venta por el abono
    - And puede generar una constancia interna.
- **Dependencias:** US-19, US-17, US-49 (idempotencia de sincronización).
- **Datos o entidades involucradas:** ComprobanteElectronico (venta, respuestas del proveedor, estado, errores), Pedido, Pensionista, cola de envío.
- **Riesgos, supuestos o pendientes:** **Pendiente de definición** — datos fiscales y proveedor de facturación API (`07` § Pendientes funcionales); endpoints, estados, contingencia e idempotencia deben validarse con el proveedor (`07` § Validaciones externas). Ver RF-004 y RF-015. Esta historia no puede completarse sin esas definiciones.
- **Trazabilidad:** `03-pos-caja-cocina-delivery.md` § Facturación electrónica; `02-pensionistas.md` § Facturación; `05-usuarios-reportes-multilocal-offline.md` § Offline-first; `06-modelo-de-datos.md` § Venta; `07-criterios-de-aceptacion-y-pendientes.md` §§ Casos críticos (16), Validaciones externas.
- **Estimación inicial:** XL

::::

:::: {.historia title="US-28 — Generar el cierre mensual y el estado de cuenta"}

**E2 — Controlar la cobranza** · Prioridad **Must** · Release **MVP** · Estimación **L**

**Actor:** Administrador central

> *Como administrador,*  
> *quiero cerrar el mes de cada pensionista y generar su estado de cuenta,*  
> *para dejar un corte formal e inalterable y arrastrar el saldo al mes siguiente.*

- **Descripción funcional:** Cierre mensual de la cuenta con saldo inicial, consumos, abonos, ajustes y saldo final. Arrastra deuda o saldo a favor al mes siguiente sin marcar atraso automáticamente. El estado mensual cerrado es inalterable, conserva un PDF fijo y es consultable desde el portal.
- **Reglas de negocio asociadas:** RN-006, RN-020, RN-085, RN-029.
- **Criterios de aceptación:**
  - **CA1 — Contenido del cierre**
    - Given un periodo mensual con movimientos
    - When se genera el cierre
    - Then el estado debe mostrar saldo inicial, consumos, abonos, ajustes y saldo final.
  - **CA2 — Arrastre**
    - Given un cierre con deuda o saldo a favor
    - When inicia el mes siguiente
    - Then el sistema debe arrastrar ese saldo como saldo inicial
    - And no debe marcar atraso automáticamente.
  - **CA3 — Inalterabilidad**
    - Given un estado mensual cerrado
    - When se intenta registrar un movimiento retroactivo en ese periodo
    - Then el sistema debe impedirlo.
  - **CA4 — PDF fijo conservado**
    - Given un estado mensual cerrado
    - When se consulta más adelante desde el portal o desde reportes
    - Then el sistema debe entregar el mismo PDF conservado en el cierre.
  - **CA5 — Notificación**
    - Given la generación del estado mensual
    - When se completa
    - Then el sistema debe notificarlo internamente en el portal.
- **Dependencias:** US-17, US-25, US-26.
- **Datos o entidades involucradas:** PeriodoCuenta, EstadoCuentaMensual, MovimientoCuenta, CuentaPensionista.
- **Riesgos, supuestos o pendientes:** el tratamiento tributario de los cierres mensuales debe validarse con el contador — ver RF-014.
- **Trazabilidad:** `02-pensionistas.md` § Cuenta corriente; `05-usuarios-reportes-multilocal-offline.md` § Reportes mínimos; `06-modelo-de-datos.md` §§ Pensionistas, Reglas de integridad esenciales; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (6).
- **Estimación inicial:** L

::::

:::: {.historia title="US-29 — Entregar el comprobante al cliente por correo"}

**E2 — Controlar la cobranza** · Prioridad **Should** · Release **Post-MVP · **Sprint:** 11** · Estimación **M**

**Actor:** Sistema / Cajero (reenvío manual)

> *Como cliente que pidió su comprobante,*  
> *quiero recibirlo en mi correo apenas esté aceptado,*  
> *para no depender de un papel ni de volver al local.*

- **Descripción funcional:** al pasar el comprobante a aceptado, el sistema descarga su PDF y XML del proveedor, los guarda en el almacenamiento propio y los envía por correo al cliente desde el dominio del restaurante. El envío es opcional y a pedido para el cliente regular: solo se pide el correo cuando el cliente dice que lo quiere.
- **Reglas de negocio asociadas:** RN-035, RN-029, RN-085, RN-092.
- **Criterios de aceptación:**
  - **CA1 — Disparo al aceptar**
    - Given un comprobante que pasa al estado aceptado
    - When se confirma ese cambio de estado
    - Then el sistema debe enviar el correo con el PDF y el XML adjuntos
    - And no debe enviarlo mientras el comprobante esté solo registrado.
  - **CA2 — Exactamente una vez**
    - Given un comprobante ya notificado
    - When el proceso de reconsulta vuelve a encontrarlo aceptado
    - Then el sistema no debe enviar un segundo correo.
  - **CA3 — Comprobante rechazado**
    - Given un comprobante rechazado por SUNAT
    - When se procesa su estado
    - Then el sistema no debe enviar ningún correo al cliente.
  - **CA4 — Rebote**
    - Given un correo que rebota
    - When se detecta
    - Then el comprobante debe quedar marcado en la bandeja para revisión
    - And debe permitir corregir la dirección y reenviar.
  - **CA5 — Reenvío manual**
    - Given un cliente que reclama no haber recibido su comprobante
    - When el cajero lo busca y solicita el reenvío
    - Then el sistema debe volver a enviarlo sin alterar el estado fiscal.
  - **CA6 — Pensionistas excluidos**
    - Given un consumo de pensionista
    - When se emite su comprobante
    - Then el sistema no debe enviar correo por ese consumo
    - And el comprobante debe quedar disponible en el portal.
- **Dependencias:** US-27, US-30.
- **Datos o entidades involucradas:** ComprobanteElectronico (estado de entrega, marca de correo enviado), Pensionista, Cliente regular, almacenamiento de archivos.
- **Riesgos, supuestos o pendientes:** CA6 se apoya en RN-029, que prohíbe notificar por cada consumo. Si el propietario quisiera lo contrario, debe ser una preferencia en la ficha del pensionista, apagada por defecto, y hay que modificar esa regla. **Supuesto de análisis:** el envío por correo no figura en ninguno de los ocho documentos de especificación; es una decisión funcional posterior del equipo.
- **Trazabilidad:** decisión técnica del equipo (DT-006 y DT-007); se apoya en `02-pensionistas.md` §§ Portal compartido, Facturación y en `05-usuarios-reportes-multilocal-offline.md` § Respaldo y seguridad.
- **Estimación inicial:** M

::::

:::: {.historia title="US-30 — Consultar el portal compartido del pensionista"}

**E2 — Controlar la cobranza** · Prioridad **Should** · Release **Post-MVP** · Estimación **L**

**Actor:** Pensionista y responsable (acceso compartido)

> *Como pensionista o responsable,*  
> *quiero ingresar a un portal con DNI y contraseña para ver movimientos, saldos y comprobantes,*  
> *para hacer seguimiento de los consumos y de la deuda sin tener que preguntar en el local.*

- **Descripción funcional:** Portal web de consulta con un único acceso compartido por pensionista y responsable. Permite ver movimientos, saldo, límite, crédito disponible, comprobantes y estados mensuales, y recibir notificaciones internas acotadas. No permite modificar consumos ni aprobar abonos.
- **Reglas de negocio asociadas:** RN-028, RN-029, RN-088, RN-090, RN-092.
- **Criterios de aceptación:**
  - **CA1 — Acceso compartido**
    - Given un pensionista con contraseña de portal
    - When el pensionista o su responsable ingresa con DNI y esa contraseña
    - Then el sistema debe dar acceso a la misma información de la cuenta.
  - **CA2 — Contenido de consulta**
    - Given una sesión activa en el portal
    - When se consulta la cuenta
    - Then el sistema debe mostrar movimientos, saldo, límite, crédito disponible, comprobantes y estados mensuales.
  - **CA3 — Alcance restringido**
    - Given una sesión activa en el portal
    - When se revisan las acciones disponibles
    - Then el sistema no debe permitir modificar consumos ni aprobar abonos.
  - **CA4 — Notificaciones acotadas**
    - Given eventos de la cuenta
    - When se aprueba o rechaza un abono, o se genera un estado mensual
    - Then el sistema debe notificar internamente
    - And no debe notificar por cada consumo ni por superar el límite.
  - **CA5 — Indisponibilidad tolerada**
    - Given una caída de Internet en el local
    - When el portal no está disponible
    - Then la operación del local debe continuar sin verse afectada.
- **Dependencias:** US-17, US-28, US-15.
- **Datos o entidades involucradas:** Pensionista, Responsable, CuentaPensionista, MovimientoCuenta, EstadoCuentaMensual, ComprobanteElectronico.
- **Riesgos, supuestos o pendientes:** el alta y la recuperación de la contraseña del portal no están documentadas — ver RF-027.
- **Trazabilidad:** `02-pensionistas.md` § Portal compartido; `05-usuarios-reportes-multilocal-offline.md` §§ Offline-first, Respaldo y seguridad.
- **Estimación inicial:** L

::::

:::: {.historia title="US-31 — Reportar un abono digital desde el portal"}

**E2 — Controlar la cobranza** · Prioridad **Should** · Release **Post-MVP** · Estimación **M**

**Actor:** Pensionista y responsable

> *Como pensionista o responsable,*  
> *quiero reportar desde el portal un abono digital adjuntando el comprobante,*  
> *para que el administrador lo apruebe y se refleje en mi saldo.*

- **Descripción funcional:** Registro de abonos digitales desde el portal con datos y adjunto obligatorios. Queda en estado pendiente hasta la aprobación del administrador y no modifica el saldo. El sistema valida la posible duplicidad del número de operación.
- **Reglas de negocio asociadas:** RN-031, RN-032, RN-028, RN-092.
- **Criterios de aceptación:**
  - **CA1 — Datos obligatorios**
    - Given el formulario de reporte de abono en el portal
    - When el usuario intenta enviarlo sin imagen de comprobante, número de operación, monto, fecha, medio o cuenta destino
    - Then el sistema debe impedir el envío.
  - **CA2 — Estado pendiente sin efecto en el saldo**
    - Given un abono digital reportado con todos los datos
    - When se envía
    - Then el sistema debe registrarlo en estado pendiente
    - And no debe modificar el saldo del pensionista.
  - **CA3 — Validación de duplicidad**
    - Given un número de operación ya registrado
    - When se reporta otro abono con el mismo número
    - Then el sistema debe advertir la posible duplicidad.
  - **CA4 — Protección del adjunto**
    - Given un comprobante adjunto cargado
    - When un usuario sin permiso intenta consultarlo
    - Then el sistema debe impedir el acceso.
- **Dependencias:** US-30, US-26.
- **Datos o entidades involucradas:** Abono (estado, adjunto, número de operación, medio, cuenta destino, aprobador), CuentaPensionista.
- **Riesgos, supuestos o pendientes:** ninguno documentado más allá de la política de retención de adjuntos (RF-011).
- **Trazabilidad:** `02-pensionistas.md` §§ Abonos, Portal compartido; `05-usuarios-reportes-multilocal-offline.md` § Respaldo y seguridad; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (5).
- **Estimación inicial:** M

::::

:::: {.historia title="US-32 — Desactivar y reactivar a un pensionista"}

**E2 — Controlar la cobranza** · Prioridad **Should** · Release **MVP** · Estimación **S**

**Actor:** Administrador

> *Como administrador,*  
> *quiero desactivar a un pensionista y poder reactivarlo después,*  
> *para cortar el consumo a crédito sin perder su historial ni su deuda.*

- **Descripción funcional:** La desactivación bloquea nuevos tickets y consumos, conserva la cuenta, el historial y el acceso de consulta al portal, y permite seguir registrando abonos. La reactivación reutiliza la misma cuenta.
- **Reglas de negocio asociadas:** RN-034, RN-007.
- **Criterios de aceptación:**
  - **CA1 — Bloqueo de consumo**
    - Given un pensionista desactivado
    - When intenta emitir un pre-ticket o se intenta registrar un consumo a su cuenta
    - Then el sistema debe impedirlo.
  - **CA2 — Consulta y abonos posteriores**
    - Given un pensionista desactivado con deuda
    - When ingresa al portal o el cajero registra un abono
    - Then el sistema debe permitir la consulta y el registro del abono.
  - **CA3 — Reactivación sobre la misma cuenta**
    - Given un pensionista desactivado
    - When se reactiva
    - Then el sistema debe reutilizar la misma cuenta con su saldo e historial.
- **Dependencias:** US-15, US-17, US-25.
- **Datos o entidades involucradas:** Pensionista (estado), CuentaPensionista, MovimientoCuenta, PreTicket.
- **Riesgos, supuestos o pendientes:** ninguno documentado.
- **Trazabilidad:** `02-pensionistas.md` § Desactivación; `01-alcance-y-reglas.md` § Reglas de historial.
- **Estimación inicial:** S

::::


## 3.3 E3 — Controlar el consumo de insumos

Prioridad 3 de 5 · 14 historias, de US-33 a US-46.

:::: {.historia title="US-33 — Administrar el catálogo central de productos"}

**E3 — Controlar el consumo de insumos** · Prioridad **Must** · Release **MVP** · Estimación **M**

**Actor:** Administrador central

> *Como administrador central,*  
> *quiero mantener un catálogo único de productos clasificados por tipo,*  
> *para que todos los locales vendan, produzcan y compren sobre la misma base de datos maestra.*

- **Descripción funcional:** Alta, edición y desactivación de productos con su clasificación: entradas, segundos/guisos, guarniciones/menestras, refrescos, postres, extras, productos fijos de desayuno/cena, insumos y subrecetas. Un producto puede ser vendible, insumo, preparación o ambos. El catálogo es central y común a todos los locales.
- **Reglas de negocio asociadas:** RN-001, RN-007.
- **Criterios de aceptación:**
  - **CA1 — Clasificación obligatoria**
    - Given el alta de un producto
    - When el administrador lo guarda
    - Then el sistema debe exigir su categoría entre entradas, segundos/guisos, guarniciones/menestras, refrescos, postres, extras, productos fijos de desayuno/cena, insumos y subrecetas.
  - **CA2 — Naturaleza del producto**
    - Given un producto registrado
    - When se define su naturaleza
    - Then el sistema debe permitir marcarlo como vendible, insumo, preparación o combinación de estas.
  - **CA3 — Alcance central**
    - Given un producto creado o modificado
    - When se consulta desde cualquier local
    - Then el sistema debe mostrar la misma definición para todos los locales.
  - **CA4 — Desactivación**
    - Given un producto con movimientos históricos
    - When se desactiva
    - Then el sistema debe impedir su uso en nuevos registros y conservar su historial.
- **Dependencias:** US-52, US-56.
- **Datos o entidades involucradas:** Producto, Categoría.
- **Riesgos, supuestos o pendientes:** ninguno documentado.
- **Trazabilidad:** `04-menu-recetas-inventario-compras.md` § Catálogo; `01-alcance-y-reglas.md` § Multilocal; `06-modelo-de-datos.md` § Catálogo y costos.
- **Estimación inicial:** M

::::

:::: {.historia title="US-34 — Definir unidades base, presentaciones y conversiones"}

**E3 — Controlar el consumo de insumos** · Prioridad **Must** · Release **MVP** · Estimación **M**

**Actor:** Administrador central

> *Como administrador central,*  
> *quiero definir la unidad base de cada producto y sus presentaciones de compra con factor de conversión,*  
> *para que compras, recetas e inventario trabajen sobre una misma unidad comparable.*

- **Descripción funcional:** Configuración de la unidad base por producto y de las presentaciones de compra con su equivalencia (cajas/unidades, sacos/kg, litros/ml y similares), usadas al registrar compras, recetas y movimientos de inventario.
- **Reglas de negocio asociadas:** RN-001.
- **Criterios de aceptación:**
  - **CA1 — Unidad base obligatoria**
    - Given el alta de un producto insumo
    - When se guarda
    - Then el sistema debe exigir una unidad base.
  - **CA2 — Presentación con conversión**
    - Given un producto con unidad base definida
    - When se registra una presentación de compra
    - Then el sistema debe exigir el factor de conversión a la unidad base.
  - **CA3 — Uso en compra**
    - Given una compra registrada en una presentación
    - When se confirma
    - Then el sistema debe convertir la cantidad a la unidad base para actualizar el stock.
- **Dependencias:** US-33.
- **Datos o entidades involucradas:** Producto, PresentaciónCompra, ConversiónUnidad.
- **Riesgos, supuestos o pendientes:** ninguno documentado.
- **Trazabilidad:** `04-menu-recetas-inventario-compras.md` § Inventario y Kardex; `06-modelo-de-datos.md` § Catálogo y costos.
- **Estimación inicial:** M

::::

:::: {.historia title="US-35 — Administrar el catálogo de proveedores"}

**E3 — Controlar el consumo de insumos** · Prioridad **Must** · Release **MVP** · Estimación **S**

**Actor:** Administrador de local / Administrador central

> *Como administrador,*  
> *quiero mantener el catálogo de proveedores,*  
> *para poder asociar cada compra a su proveedor y su comprobante.*

- **Descripción funcional:** Alta, edición y desactivación de proveedores usados en el registro de compras. No incluye órdenes de compra ni cuentas por pagar.
- **Reglas de negocio asociadas:** RN-007, RN-055.
- **Criterios de aceptación:**
  - **CA1 — Alta de proveedor**
    - Given un usuario con permiso de creación
    - When registra un proveedor
    - Then el sistema debe guardarlo y ponerlo disponible para el registro de compras.
  - **CA2 — Sin órdenes de compra**
    - Given el módulo de proveedores
    - When se revisan sus funciones
    - Then el sistema no debe ofrecer órdenes de compra ni cuentas por pagar.
  - **CA3 — Desactivación**
    - Given un proveedor con compras registradas
    - When se desactiva
    - Then el sistema debe impedir su uso en nuevas compras y conservar las anteriores.
- **Dependencias:** US-56.
- **Datos o entidades involucradas:** Proveedor, Compra.
- **Riesgos, supuestos o pendientes:** los campos opcionales adicionales de proveedores están pendientes de definición (`07`) — ver RF-009.
- **Trazabilidad:** `04-menu-recetas-inventario-compras.md` § Compras; `06-modelo-de-datos.md` § Catálogo y costos; `README.md` § Exclusiones confirmadas.
- **Estimación inicial:** S

::::

:::: {.historia title="US-36 — Configurar listas de motivos del sistema"}

**E3 — Controlar el consumo de insumos** · Prioridad **Must** · Release **MVP** · Estimación **XS**

**Actor:** Administrador central

> *Como administrador central,*  
> *quiero configurar las listas de motivos de ajuste, anulación y rechazo,*  
> *para que las acciones sensibles se justifiquen con opciones homogéneas y reportables.*

- **Descripción funcional:** Mantenimiento de catálogos de motivos configurables utilizados en ajustes de inventario y, según la especificación, también en anulaciones y rechazos de pagos.
- **Reglas de negocio asociadas:** RN-052, RN-072, RN-004.
- **Criterios de aceptación:**
  - **CA1 — Motivos configurables de ajuste**
    - Given el catálogo de motivos de ajuste
    - When el administrador agrega, edita o desactiva un motivo
    - Then el sistema debe reflejarlo en la lista disponible al registrar ajustes.
  - **CA2 — Uso obligatorio**
    - Given un ajuste de inventario
    - When el usuario intenta confirmarlo sin seleccionar un motivo de la lista
    - Then el sistema debe impedir la confirmación.
  - **CA3 — Observación complementaria**
    - Given un ajuste con motivo seleccionado
    - When el usuario añade una observación
    - Then el sistema debe guardarla junto con el movimiento.
- **Dependencias:** US-56.
- **Datos o entidades involucradas:** MotivoAjuste, motivos de anulación, motivos de rechazo de pago.
- **Riesgos, supuestos o pendientes:** **Pendiente de definición** — los motivos iniciales de ajuste, anulación y rechazo de pagos no están definidos (`07` § Pendientes funcionales). La funcionalidad es implementable como catálogo vacío configurable. Ver RF-005.
- **Trazabilidad:** `04-menu-recetas-inventario-compras.md` § Inventario y Kardex; `03-pos-caja-cocina-delivery.md` § Caja; `07-criterios-de-aceptacion-y-pendientes.md` § Pendientes funcionales; `06-modelo-de-datos.md` § Inventario.
- **Estimación inicial:** XS

::::

:::: {.historia title="US-37 — Crear recetas y subrecetas valorizadas por porción"}

**E3 — Controlar el consumo de insumos** · Prioridad **Must** · Release **MVP** · Estimación **XL**

**Actor:** Administrador central

> *Como administrador central,*  
> *quiero registrar la receta valorizada de cada componente, incluyendo subrecetas,*  
> *para conocer el costo por porción y sostener el control de costos y márgenes.*

- **Descripción funcional:** Definición de recetas de productos vendibles y de subrecetas, con insumos y/o subrecetas, cantidades, unidades, rendimiento y porciones. La valorización usa el costo de última compra y se recalcula cuando ingresa una compra nueva. La modificación actualiza la receta maestra y aplica solo a nuevas ventas. El acceso a costos y el mantenimiento de recetas están gobernados por permisos.
- **Reglas de negocio asociadas:** RN-043, RN-044, RN-045, RN-012, RN-004.
- **Criterios de aceptación:**
  - **CA1 — Composición de la receta**
    - Given un producto vendible o una subreceta
    - When el administrador registra su receta
    - Then el sistema debe permitir indicar insumos y/o subrecetas, cantidades, unidades, rendimiento y porciones.
  - **CA2 — Costo por porción**
    - Given una receta con insumos valorizados
    - When se guarda
    - Then el sistema debe calcular y mostrar el costo por porción usando el costo de última compra de cada insumo.
  - **CA3 — Anidamiento de subrecetas**
    - Given una subreceta ya definida
    - When se incluye como ingrediente de otra receta
    - Then el sistema debe incorporar su costo por porción al costo de la receta contenedora.
  - **CA4 — Recálculo por nueva compra**
    - Given una compra confirmada que actualiza el costo de un insumo
    - When se consulta la receta que lo utiliza
    - Then el sistema debe mostrar el costo actualizado y los márgenes futuros recalculados
    - And no debe modificar los costos de ventas ya cerradas.
  - **CA5 — Vigencia de la modificación**
    - Given una receta modificada
    - When se registran nuevas ventas
    - Then el sistema debe aplicar la receta actualizada solo a esas ventas nuevas.
  - **CA6 — Permisos y auditoría**
    - Given un usuario sin permiso de ver costos
    - When abre una receta
    - Then el sistema no debe mostrar los costos
    - And todo cambio de receta debe registrarse en la auditoría.
- **Dependencias:** US-33, US-34, US-56, US-57. Para la valorización real depende de US-42 (compras) que alimenta el costo de última compra.
- **Datos o entidades involucradas:** Receta, IngredienteReceta, Producto, subreceta, rendimiento, porciones, costo por porción.
- **Riesgos, supuestos o pendientes:** la valorización inicial antes de la primera compra no está documentada — **Supuesto de análisis:** requiere un costo de referencia inicial cargado manualmente; debe confirmarse con negocio. Relacionado con RF-002.
- **Trazabilidad:** `04-menu-recetas-inventario-compras.md` § Recetas y costos; `06-modelo-de-datos.md` § Catálogo y costos; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (10, 11).
- **Estimación inicial:** XL

::::

:::: {.historia title="US-38 — Mantener existencias por local con stock mínimo y alertas"}

**E3 — Controlar el consumo de insumos** · Prioridad **Must** · Release **MVP** · Estimación **M**

**Actor:** Administrador de local

> *Como administrador de local,*  
> *quiero consultar las existencias de mi local con su stock mínimo configurado,*  
> *para saber qué falta sin que el sistema me bloquee la operación.*

- **Descripción funcional:** Existencia identificada por local y producto, con stock mínimo configurable por producto y local. El stock negativo se permite con alerta y no bloquea movimientos.
- **Reglas de negocio asociadas:** RN-002, RN-050, RN-051, RN-057.
- **Criterios de aceptación:**
  - **CA1 — Existencia por local y producto**
    - Given un producto y un local
    - When se consulta la existencia
    - Then el sistema debe mostrar una única existencia identificada por esa combinación.
  - **CA2 — Stock mínimo configurable**
    - Given un producto en un local
    - When el administrador define su stock mínimo
    - Then el sistema debe guardarlo para ese producto y local.
  - **CA3 — Alerta de stock mínimo**
    - Given una existencia que baja del stock mínimo
    - When se registra el movimiento
    - Then el sistema debe alertar sin bloquear la operación.
  - **CA4 — Stock negativo permitido con alerta**
    - Given una existencia insuficiente
    - When se registra una venta o consumo que la deja en negativo
    - Then el sistema debe permitir el movimiento y mostrar una alerta.
- **Dependencias:** US-33, US-34, US-52.
- **Datos o entidades involucradas:** Existencia (Local + Producto), stock mínimo, MovimientoInventario.
- **Riesgos, supuestos o pendientes:** ninguno documentado.
- **Trazabilidad:** `04-menu-recetas-inventario-compras.md` § Inventario y Kardex; `01-alcance-y-reglas.md` § Multilocal; `06-modelo-de-datos.md` § Inventario.
- **Estimación inicial:** M

::::

:::: {.historia title="US-39 — Consultar el Kardex con origen rastreable"}

**E3 — Controlar el consumo de insumos** · Prioridad **Must** · Release **MVP** · Estimación **L**

**Actor:** Administrador de local / Administrador central

> *Como administrador,*  
> *quiero ver el Kardex de cada producto con el origen de cada movimiento,*  
> *para poder explicar cualquier variación de stock.*

- **Descripción funcional:** Registro y consulta de todos los movimientos de inventario con su origen: compra, producción, venta, ajuste, transferencia, conteo o consumo interno, incluyendo stock anterior y nuevo, diferencia, costo, usuario, fecha y local.
- **Reglas de negocio asociadas:** RN-057, RN-052, RN-002.
- **Criterios de aceptación:**
  - **CA1 — Origen obligatorio**
    - Given cualquier movimiento de inventario
    - When se registra
    - Then el sistema debe asociarlo a un origen entre compra, producción, venta, ajuste, transferencia, conteo o consumo interno.
  - **CA2 — Contenido del movimiento**
    - Given un movimiento registrado
    - When se consulta el Kardex
    - Then debe mostrar stock anterior, stock nuevo, diferencia, costo, usuario, fecha y local.
  - **CA3 — Filtros**
    - Given el Kardex de un local
    - When se filtra por producto o rango de fechas
    - Then el sistema debe mostrar los movimientos correspondientes en orden cronológico.
- **Dependencias:** US-38.
- **Datos o entidades involucradas:** MovimientoInventario, Existencia, Producto, Local, Usuario.
- **Riesgos, supuestos o pendientes:** ninguno documentado.
- **Trazabilidad:** `04-menu-recetas-inventario-compras.md` § Inventario y Kardex; `06-modelo-de-datos.md` §§ Inventario, Reglas de integridad esenciales; `05-usuarios-reportes-multilocal-offline.md` § Reportes mínimos.
- **Estimación inicial:** L

::::

:::: {.historia title="US-40 — Registrar ajustes de inventario con motivo"}

**E3 — Controlar el consumo de insumos** · Prioridad **Must** · Release **MVP** · Estimación **M**

**Actor:** Administrador de local con permiso

> *Como administrador de local,*  
> *quiero registrar ajustes positivos o negativos indicando motivo y observación,*  
> *para corregir el stock dejando constancia de por qué cambió.*

- **Descripción funcional:** Registro de ajustes de inventario con motivo tomado de una lista configurable y observación, guardando stock anterior y nuevo, diferencia, costo, usuario, fecha y local. Los ajustes se auditan.
- **Reglas de negocio asociadas:** RN-052, RN-004, RN-012, RN-057.
- **Criterios de aceptación:**
  - **CA1 — Ajuste positivo y negativo**
    - Given una existencia
    - When el usuario registra un ajuste positivo o negativo
    - Then el sistema debe aplicar la variación al stock del local.
  - **CA2 — Motivo obligatorio**
    - Given un ajuste en curso
    - When se intenta confirmar sin motivo de la lista configurable
    - Then el sistema debe impedir la confirmación.
  - **CA3 — Datos guardados**
    - Given un ajuste confirmado
    - When se consulta
    - Then debe mostrar stock anterior, stock nuevo, diferencia, costo, usuario, fecha, local, motivo y observación.
  - **CA4 — Permiso**
    - Given un usuario sin permiso de ajustar inventario
    - When intenta registrar un ajuste
    - Then el sistema debe impedirlo.
- **Dependencias:** US-38, US-36, US-56, US-57.
- **Datos o entidades involucradas:** Ajuste, MotivoAjuste, MovimientoInventario, Existencia.
- **Riesgos, supuestos o pendientes:** motivos iniciales pendientes — ver RF-005.
- **Trazabilidad:** `04-menu-recetas-inventario-compras.md` § Inventario y Kardex; `01-alcance-y-reglas.md` § Auditoría transversal; `06-modelo-de-datos.md` § Inventario; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (19).
- **Estimación inicial:** M

::::

:::: {.historia title="US-41 — Producir subrecetas y descontar receta al vender"}

**E3 — Controlar el consumo de insumos** · Prioridad **Must** · Release **MVP** · Estimación **XL**

**Actor:** Cocina / Administrador de local

> *Como encargado de cocina,*  
> *quiero registrar la producción de subrecetas y su rendimiento,*  
> *para que el inventario descuente los insumos una sola vez y el costo del plato sea correcto.*

- **Descripción funcional:** Registro de producción: al producir una subreceta se descuentan sus insumos y se ingresa el rendimiento producido; al vender un plato final se descuenta su receta, incluidas las cantidades de subrecetas, sin duplicar consumos. Se registran porciones planificadas, preparadas y vendidas por plato. El sistema no calcula automáticamente la lista de insumos requeridos desde la planificación.
- **Reglas de negocio asociadas:** RN-047, RN-048, RN-057, RN-043.
- **Criterios de aceptación:**
  - **CA1 — Producción de subreceta**
    - Given una subreceta con receta definida
    - When se registra su producción
    - Then el sistema debe descontar sus insumos e ingresar el rendimiento producido al inventario.
  - **CA2 — Venta del plato final**
    - Given un plato cuya receta incluye una subreceta ya producida
    - When se vende
    - Then el sistema debe descontar su receta incluidas las cantidades de subreceta
    - And no debe volver a descontar los insumos ya consumidos en la producción de la subreceta.
  - **CA3 — Porciones**
    - Given un plato del día
    - When se consulta su seguimiento
    - Then el sistema debe mostrar porciones planificadas, preparadas y vendidas.
  - **CA4 — Sin cálculo automático de requerimientos**
    - Given la planificación semanal del menú
    - When se revisa el módulo de producción
    - Then el sistema no debe generar automáticamente una lista de insumos requeridos.
- **Dependencias:** US-37, US-38, US-02, US-19.
- **Datos o entidades involucradas:** Produccion (subreceta o plato, local, rendimiento, porciones), MovimientoInventario, Receta, IngredienteReceta.
- **Riesgos, supuestos o pendientes:** ninguno documentado.
- **Trazabilidad:** `04-menu-recetas-inventario-compras.md` § Producción; `06-modelo-de-datos.md` § Menú y producción; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (11).
- **Estimación inicial:** XL

::::

:::: {.historia title="US-42 — Registrar compras y actualizar el costo de última compra"}

**E3 — Controlar el consumo de insumos** · Prioridad **Must** · Release **MVP** · Estimación **L**

**Actor:** Administrador de local

> *Como administrador de local,*  
> *quiero registrar la compra con su comprobante y confirmarla,*  
> *para aumentar el stock y actualizar el costo con el que se valorizan las recetas.*

- **Descripción funcional:** Registro básico del ingreso de compra asociado a un proveedor y su comprobante (tipo, serie, número, fecha, productos, presentaciones, cantidades, precios, total y archivo adjunto). Al confirmar, aumenta el stock del local y actualiza el costo de última compra. No hay orden de compra ni cuenta por pagar.
- **Reglas de negocio asociadas:** RN-054, RN-055, RN-044, RN-057, RN-092.
- **Criterios de aceptación:**
  - **CA1 — Datos del comprobante**
    - Given el registro de una compra
    - When se guarda
    - Then el sistema debe exigir tipo, serie, número, proveedor, fecha, productos, presentaciones, cantidades, precios y total, y permitir adjuntar el archivo del comprobante.
  - **CA2 — Confirmación de la compra**
    - Given una compra registrada
    - When se confirma
    - Then el sistema debe aumentar el stock del local según las presentaciones y conversiones
    - And debe actualizar el costo de última compra de cada producto.
  - **CA3 — Recálculo de recetas**
    - Given un costo de última compra actualizado
    - When se consultan las recetas que usan ese producto
    - Then el sistema debe mostrar los costos y márgenes futuros recalculados
    - And no debe modificar las ventas anteriores.
  - **CA4 — Sin OC ni CxP**
    - Given el módulo de compras
    - When se revisan sus funciones
    - Then el sistema no debe ofrecer órdenes de compra ni cuentas por pagar.
  - **CA5 — Protección del adjunto**
    - Given un comprobante adjunto
    - When lo consulta un usuario sin permiso
    - Then el sistema debe impedir el acceso.
- **Dependencias:** US-35, US-34, US-38, US-37.
- **Datos o entidades involucradas:** Compra, DetalleCompra, Proveedor, PresentaciónCompra, MovimientoInventario, costo de última compra, adjunto.
- **Riesgos, supuestos o pendientes:** la política de retención de adjuntos está pendiente — ver RF-011.
- **Trazabilidad:** `04-menu-recetas-inventario-compras.md` §§ Compras, Recetas y costos; `06-modelo-de-datos.md` § Catálogo y costos; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (10); `README.md` § Exclusiones confirmadas.
- **Estimación inicial:** L

::::

:::: {.historia title="US-43 — Registrar el destino de los sobrantes de producción"}

**E3 — Controlar el consumo de insumos** · Prioridad **Should** · Release **MVP** · Estimación **S**

**Actor:** Encargado de cocina / Administrador de local

> *Como encargado,*  
> *quiero decidir y registrar qué se hace con el sobrante,*  
> *para que el inventario refleje si se conservó, se consumió, se perdió o se reutilizó.*

- **Descripción funcional:** Registro del destino del sobrante de producción entre conservar, personal, pérdida, reutilización u otro. El destino elegido genera el ajuste de inventario correspondiente.
- **Reglas de negocio asociadas:** RN-049, RN-052, RN-057.
- **Criterios de aceptación:**
  - **CA1 — Selección del destino**
    - Given una producción con sobrante
    - When el encargado registra su destino
    - Then el sistema debe permitir elegir entre conservar, personal, pérdida, reutilización u otro.
  - **CA2 — Ajuste generado**
    - Given un destino registrado
    - When se confirma
    - Then el sistema debe generar el ajuste de inventario correspondiente a ese destino.
  - **CA3 — Trazabilidad**
    - Given un ajuste por sobrante
    - When se consulta el Kardex
    - Then debe identificar el destino registrado.
- **Dependencias:** US-41, US-40.
- **Datos o entidades involucradas:** Produccion, MovimientoInventario, Ajuste.
- **Riesgos, supuestos o pendientes:** ninguno documentado.
- **Trazabilidad:** `04-menu-recetas-inventario-compras.md` § Producción.
- **Estimación inicial:** S

::::

:::: {.historia title="US-44 — Registrar el consumo interno del personal"}

**E3 — Controlar el consumo de insumos** · Prioridad **Should** · Release **MVP** · Estimación **M**

**Actor:** Administrador de local / Cajero

> *Como administrador de local,*  
> *quiero registrar la comida del personal y su costo,*  
> *para descontar el inventario y reportarla separada de las ventas.*

- **Descripción funcional:** Registro del consumo interno: una comida sin cobro por trabajador y turno; una segunda requiere autorización según permiso. Registra trabajador, platos, turno, costo y descuento de inventario, y se reporta separado de las ventas.
- **Reglas de negocio asociadas:** RN-056, RN-012, RN-057.
- **Criterios de aceptación:**
  - **CA1 — Primera comida sin cobro**
    - Given un trabajador que aún no registró consumo interno en su turno
    - When se registra su comida
    - Then el sistema debe permitirla sin cobro y descontar el inventario.
  - **CA2 — Segunda comida con autorización**
    - Given un trabajador que ya registró una comida en el turno
    - When se intenta registrar una segunda
    - Then el sistema debe exigir la autorización de un usuario con el permiso correspondiente.
  - **CA3 — Datos registrados**
    - Given un consumo interno confirmado
    - When se consulta
    - Then debe mostrar trabajador, platos, turno y costo.
  - **CA4 — Reporte separado**
    - Given consumos internos registrados
    - When se consultan los reportes de ventas
    - Then los consumos internos deben reportarse por separado y no como ventas.
- **Dependencias:** US-38, US-37, US-56, US-18.
- **Datos o entidades involucradas:** consumo interno (trabajador, platos, turno, costo), MovimientoInventario, Usuario.
- **Riesgos, supuestos o pendientes:** no se documenta qué platos puede incluir el consumo interno ni si aplica a motorizados o personal externo — ver RF-023.
- **Trazabilidad:** `04-menu-recetas-inventario-compras.md` § Consumo interno; `05-usuarios-reportes-multilocal-offline.md` § Reportes mínimos.
- **Estimación inicial:** M

::::

:::: {.historia title="US-45 — Transferir productos entre locales"}

**E3 — Controlar el consumo de insumos** · Prioridad **Should** · Release **Post-MVP** · Estimación **M**

**Actor:** Administrador de local / Administrador central

> *Como administrador,*  
> *quiero transferir productos de un local a otro,*  
> *para cubrir faltantes manteniendo el stock de ambos locales correcto.*

- **Descripción funcional:** Transferencia simple entre locales que, al confirmarse, genera dos ajustes enlazados aplicados simultáneamente: salida en el local origen y entrada en el local destino. Queda auditada.
- **Reglas de negocio asociadas:** RN-010, RN-004, RN-057.
- **Criterios de aceptación:**
  - **CA1 — Ajustes enlazados**
    - Given una transferencia entre dos locales
    - When se confirma
    - Then el sistema debe generar simultáneamente un movimiento de salida en el origen y uno de entrada en el destino, enlazados entre sí.
  - **CA2 — Trazabilidad**
    - Given una transferencia confirmada
    - When se consulta el Kardex de cualquiera de los dos locales
    - Then el movimiento debe identificarse con origen transferencia y referenciar su contraparte.
  - **CA3 — Auditoría**
    - Given una transferencia confirmada
    - When se consulta la auditoría
    - Then debe registrar usuario, fecha, hora, locales y cantidades.
- **Dependencias:** US-40, US-38, US-57.
- **Datos o entidades involucradas:** Transferencia, MovimientoInventario (salida y entrada), Existencia, Local.
- **Riesgos, supuestos o pendientes:** no se documenta quién confirma la transferencia ni si el local receptor debe aceptarla — ver RF-024.
- **Trazabilidad:** `01-alcance-y-reglas.md` § Multilocal; `04-menu-recetas-inventario-compras.md` § Inventario y Kardex; `06-modelo-de-datos.md` § Inventario.
- **Estimación inicial:** M

::::

:::: {.historia title="US-46 — Ejecutar conteos físicos sin detener la operación"}

**E3 — Controlar el consumo de insumos** · Prioridad **Should** · Release **Post-MVP** · Estimación **XL**

**Actor:** Administrador de local

> *Como administrador de local,*  
> *quiero hacer un conteo físico total mientras el local sigue vendiendo,*  
> *para cuadrar el inventario sin cerrar la operación.*

- **Descripción funcional:** Conteo físico total y periódico que toma un snapshot inicial, permite registrar el detalle contado y concilia las compras, ventas, producción y ajustes ocurridos durante el conteo.
- **Reglas de negocio asociadas:** RN-053, RN-057, RN-052.
- **Criterios de aceptación:**
  - **CA1 — Snapshot inicial**
    - Given el inicio de un conteo físico
    - When se confirma su apertura
    - Then el sistema debe guardar el snapshot de existencias en ese momento.
  - **CA2 — Operación simultánea**
    - Given un conteo físico en curso
    - When se registran ventas, compras, producción o ajustes
    - Then el sistema debe permitir esas operaciones sin interrumpir el conteo.
  - **CA3 — Conciliación**
    - Given un conteo físico cerrado
    - When se calculan las diferencias
    - Then el sistema debe conciliar el conteo con los movimientos ocurridos durante el periodo y mostrar la diferencia resultante.
  - **CA4 — Generación del ajuste**
    - Given diferencias conciliadas
    - When se confirma el resultado del conteo
    - Then el sistema debe generar los movimientos de inventario con origen conteo.
- **Dependencias:** US-39, US-40, US-38.
- **Datos o entidades involucradas:** ConteoFisico, DetalleConteo, snapshot, MovimientoInventario.
- **Riesgos, supuestos o pendientes:** **Pendiente de definición** — la frecuencia concreta de los conteos físicos (`07`). Ver RF-007.
- **Trazabilidad:** `04-menu-recetas-inventario-compras.md` § Inventario y Kardex; `06-modelo-de-datos.md` § Inventario; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (12).
- **Estimación inicial:** XL

::::


## 3.4 E4 — Garantizar la continuidad del servicio

Prioridad 4 de 5 · 5 historias, de US-47 a US-51.

:::: {.historia title="US-47 — Reinicio autónomo del equipo del local"}

**E4 — Garantizar la continuidad del servicio** · Prioridad **Must** · Release **MVP** · Estimación **M**

**Actor:** Administrador de local

> *Como administrador de local,*  
> *quiero que el sistema del local vuelva a funcionar solo después de un corte de luz o de Internet,*  
> *para reabrir la atención sin depender de soporte técnico.*

- **Descripción funcional:** el equipo del local arranca su propio sistema sin pedir nada a la nube. El perfil de ejecución del local no depende de un servidor central de configuración ni de un servicio de descubrimiento: si lo hiciera, el restaurante sin conexión no podría ni encender el POS.
- **Reglas de negocio asociadas:** RN-086, RN-093.
- **Criterios de aceptación:**
  - **CA1 — Arranque sin Internet**
    - Given el equipo del local apagado y sin conexión a Internet
    - When se enciende
    - Then el POS, el kiosco y los servicios del local deben quedar operativos sin intervención de nadie.
  - **CA2 — Sin dependencias remotas en el arranque**
    - Given el perfil de ejecución del local
    - When se revisa su configuración
    - Then no debe requerir un servidor central de configuración ni un servicio de descubrimiento.
  - **CA3 — Recuperación tras corte de luz**
    - Given un corte de energía durante el servicio
    - When vuelve la corriente
    - Then el local debe poder seguir vendiendo sin llamar a soporte.
- **Dependencias:** ninguna funcional.
- **Datos o entidades involucradas:** ninguna; es una condición de despliegue.
- **Riesgos, supuestos o pendientes:** debe probarse con el equipo real del local, junto con la validación externa de hardware pendiente (RF-016).
- **Trazabilidad:** `05-usuarios-reportes-multilocal-offline.md` § Offline-first; decisiones técnicas DT-031 y siguientes.
- **Estimación inicial:** M

::::

:::: {.historia title="US-48 — Operar el local completo sin conexión a Internet"}

**E4 — Garantizar la continuidad del servicio** · Prioridad **Must** · Release **MVP** · Estimación **XL**

**Actor:** Todos los roles operativos del local

> *Como personal del local,*  
> *quiero seguir operando cuando se cae Internet,*  
> *para no detener la atención, el cobro ni la carga de consumos de los pensionistas.*

- **Descripción funcional:** Operación local completa sin Internet: autenticación disponible localmente, POS, pensionistas, impresora, caja, inventario, producción y delivery local. Se conserva una copia local del saldo necesario para operar pensionistas. El portal externo, la API fiscal y la ubicación remota pueden quedar temporalmente indisponibles.
- **Reglas de negocio asociadas:** RN-086, RN-088, RN-093.
- **Criterios de aceptación:**
  - **CA1 — Módulos operables sin Internet**
    - Given un local sin conexión a Internet
    - When el personal opera
    - Then deben funcionar la autenticación local, el POS, la gestión de pensionistas, la impresora, la caja, el inventario, la producción y el delivery local.
  - **CA2 — Saldo local del pensionista**
    - Given un local sin conexión
    - When un pensionista consume
    - Then el sistema debe usar la copia local del saldo necesaria para operar y registrar el cargo al cerrar en caja.
  - **CA3 — Servicios externos degradados**
    - Given una caída de Internet
    - When el portal, la API fiscal o la ubicación remota no responden
    - Then el sistema debe continuar la operación local y encolar lo que corresponda.
  - **CA4 — Continuidad tras la caída**
    - Given una caída de Internet durante el servicio
    - When se restablece la conexión
    - Then el local debe haber operado sin interrupciones y quedar listo para sincronizar.
- **Dependencias:** US-53, US-17, US-06, US-08, US-19, US-27, US-38.
- **Datos o entidades involucradas:** copia local de saldo, cola de comprobantes, Pedido, Comanda, TurnoCaja, MovimientoInventario.
- **Riesgos, supuestos o pendientes:** la operación offline debe probarse con el hardware real (`07` § Validaciones externas) — ver RF-016. La habilitación de la autenticación local para credenciales nuevas durante una caída no está documentada — ver RF-022.
- **Trazabilidad:** `05-usuarios-reportes-multilocal-offline.md` §§ Offline-first, Restricción tecnológica; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (16); `README.md` § Alcance confirmado.
- **Estimación inicial:** XL

::::

:::: {.historia title="US-49 — Sincronizar sin duplicados y resolver conflictos"}

**E4 — Garantizar la continuidad del servicio** · Prioridad **Must** · Release **MVP** · Estimación **XL**

**Actor:** Sistema / Administrador central (supervisión)

> *Como administrador central,*  
> *quiero que al volver Internet todo se sincronice sin duplicar ni perder transacciones,*  
> *para que la información central sea confiable después de cualquier caída.*

- **Descripción funcional:** Sincronización al restablecerse la conexión, con identificadores únicos e idempotencia para prevenir duplicados y una resolución de conflictos que no pierde transacciones y deja registro de la resolución aplicada.
- **Reglas de negocio asociadas:** RN-087, RN-075, RN-086.
- **Criterios de aceptación:**
  - **CA1 — Sin duplicados**
    - Given pedidos, cargos y comprobantes generados sin conexión
    - When se restablece Internet y se sincroniza
    - Then el sistema no debe duplicar pedidos, cargos ni comprobantes.
  - **CA2 — Idempotencia**
    - Given una sincronización interrumpida y reintentada
    - When se ejecuta de nuevo
    - Then el resultado debe ser el mismo que con un único envío exitoso.
  - **CA3 — Resolución de conflictos**
    - Given operaciones en conflicto entre el local y el central
    - When se sincronizan
    - Then el sistema debe resolverlas sin perder ninguna transacción
    - And debe dejar registro de la resolución aplicada.
  - **CA4 — Cuenta de pensionista consistente**
    - Given consumos offline en más de un local sobre la misma cuenta
    - When se sincronizan
    - Then el saldo consolidado debe reflejar todos los movimientos con su local de origen.
- **Dependencias:** US-48, US-27, US-17.
- **Datos o entidades involucradas:** identificadores únicos de transacción, cola de sincronización, registro de resolución de conflictos, MovimientoCuenta, Pedido, ComprobanteElectronico.
- **Riesgos, supuestos o pendientes:** **Pendiente de definición** — las reglas exactas de resolución de conflictos entre operaciones offline de varios locales (`07` § Pendientes funcionales). Ver RF-013; sin esa definición, CA3 no puede especificarse por completo.
- **Trazabilidad:** `05-usuarios-reportes-multilocal-offline.md` § Offline-first; `03-pos-caja-cocina-delivery.md` § Facturación electrónica; `07-criterios-de-aceptacion-y-pendientes.md` §§ Casos críticos (16), Pendientes funcionales.
- **Estimación inicial:** XL

::::

:::: {.historia title="US-50 — Respaldar la información en tiempo real"}

**E4 — Garantizar la continuidad del servicio** · Prioridad **Must** · Release **MVP** · Estimación **L**

**Actor:** Administrador central

> *Como administrador central,*  
> *quiero un respaldo central en tiempo real con copias recuperables,*  
> *para no perder información ante una falla y poder restaurarla.*

- **Descripción funcional:** Respaldo central en tiempo real más copias recuperables de la información del sistema.
- **Reglas de negocio asociadas:** RN-089, RN-093.
- **Criterios de aceptación:**
  - **CA1 — Respaldo en tiempo real**
    - Given operaciones registradas en cualquier local con conexión
    - When se confirman
    - Then deben quedar respaldadas centralmente en tiempo real.
  - **CA2 — Copias recuperables**
    - Given un respaldo existente
    - When se necesita restaurar información
    - Then el sistema debe permitir recuperarla desde las copias.
- **Dependencias:** US-49.
- **Datos o entidades involucradas:** todas las entidades transaccionales y sus adjuntos.
- **Riesgos, supuestos o pendientes:** **Pendiente de definición** — la política de retención de respaldos y adjuntos (`07`). Ver RF-011.
- **Trazabilidad:** `05-usuarios-reportes-multilocal-offline.md` § Respaldo y seguridad; `README.md` § Alcance confirmado.
- **Estimación inicial:** L

::::

:::: {.historia title="US-51 — Proteger credenciales, sesiones y adjuntos"}

**E4 — Garantizar la continuidad del servicio** · Prioridad **Must** · Release **MVP** · Estimación **M**

**Actor:** Administrador central

> *Como administrador central,*  
> *quiero que las credenciales nunca sean legibles y que los adjuntos estén protegidos por permisos,*  
> *para resguardar los datos sensibles de usuarios, pensionistas y proveedores.*

- **Descripción funcional:** Medidas transversales de seguridad: contraseñas y PIN nunca almacenados ni mostrados en texto legible, separación del PIN presencial y la contraseña del portal, bloqueo temporal tras intentos fallidos de PIN, protección de adjuntos de pagos y compras según permisos, y registro de sesiones y acciones sensibles.
- **Reglas de negocio asociadas:** RN-090, RN-091, RN-092.
- **Criterios de aceptación:**
  - **CA1 — Credenciales no legibles**
    - Given cualquier pantalla, exportación o reporte
    - When se muestran datos de usuarios o pensionistas
    - Then las contraseñas y los PIN nunca deben aparecer en texto legible.
  - **CA2 — Credenciales separadas**
    - Given un pensionista
    - When se gestionan sus accesos
    - Then el PIN presencial y la contraseña del portal deben mantenerse como credenciales distintas.
  - **CA3 — Bloqueo temporal**
    - Given intentos fallidos consecutivos de PIN
    - When se alcanza el umbral configurado
    - Then el sistema debe bloquear temporalmente el acceso.
  - **CA4 — Adjuntos protegidos**
    - Given adjuntos de pagos o compras
    - When accede un usuario sin el permiso correspondiente
    - Then el sistema debe impedir el acceso.
  - **CA5 — Registro de sesiones y acciones sensibles**
    - Given una sesión o una acción sensible
    - When ocurre
    - Then el sistema debe registrarla.
- **Dependencias:** US-53, US-54, US-15, US-30, US-31, US-42, US-57.
- **Datos o entidades involucradas:** Usuario, Pensionista, credenciales, adjuntos de Abono y Compra, registro de sesiones y auditoría.
- **Riesgos, supuestos o pendientes:** **Pendiente de definición** — plazo de bloqueo por intentos fallidos y método de recuperación de acceso (`07`). Ver RF-006.
- **Trazabilidad:** `05-usuarios-reportes-multilocal-offline.md` § Respaldo y seguridad; `02-pensionistas.md` § Ficha.
- **Estimación inicial:** M

::::


## 3.5 E5 — Supervisar la operación

Prioridad 5 de 5 · 13 historias, de US-52 a US-64.

:::: {.historia title="US-52 — Administrar empresa y locales"}

**E5 — Supervisar la operación** · Prioridad **Must** · Release **MVP** · Estimación **S**

**Actor:** Administrador central

> *Como administrador central,*  
> *quiero registrar la empresa y sus locales con su configuración propia,*  
> *para que toda la operación, el inventario y la caja queden separados por local desde la primera versión.*

- **Descripción funcional:** Alta, edición y desactivación de locales pertenecientes a la empresa. Cada local queda habilitado como contenedor de caja, mesas, inventario, compras, producción, pedidos y delivery propios. Los elementos centrales (catálogo, recetas, precios, planes y programación del menú) no se configuran por local.
- **Reglas de negocio asociadas:** RN-001, RN-002, RN-007.
- **Criterios de aceptación:**
  - **CA1 — Alta de local**
    - Given que el administrador central tiene permiso de creación
    - When registra un nuevo local de la empresa
    - Then el sistema debe crear el local con su propio inventario general, caja, mesas, compras, producción y delivery
    - And no debe crear copias separadas del catálogo, recetas, precios, planes ni programación de menú.
  - **CA2 — Un único inventario general por local**
    - Given un local registrado
    - When se consulta su configuración de existencias
    - Then el sistema debe mostrar un único inventario general asociado a ese local.
  - **CA3 — Desactivación conservando historial**
    - Given un local con operaciones registradas
    - When el administrador lo desactiva
    - Then el sistema debe impedir nuevas operaciones en ese local
    - And debe conservar íntegramente su historial.
- **Dependencias:** ninguna. Es la historia raíz del backlog.
- **Datos o entidades involucradas:** Empresa, Local; relaciones Local 1—N Caja, Mesa, Inventario, Compra, Pedido y Producción.
- **Riesgos, supuestos o pendientes:** la cantidad de locales iniciales no está definida (`07` § Pendientes funcionales) — ver RF-010.
- **Trazabilidad:** `01-alcance-y-reglas.md` § Multilocal; `06-modelo-de-datos.md` § Organización; `README.md` § Alcance confirmado.
- **Estimación inicial:** S

::::

:::: {.historia title="US-53 — Autenticar usuarios con usuario y contraseña"}

**E5 — Supervisar la operación** · Prioridad **Must** · Release **MVP** · Estimación **M**

**Actor:** Todos los roles internos (administrador central, administrador de local, cajero, mozo, cocina, motorizado)

> *Como usuario del sistema,*  
> *quiero iniciar sesión con mi usuario y contraseña,*  
> *para acceder solo a los módulos y locales que me corresponden.*

- **Descripción funcional:** Acceso general al sistema mediante usuario y contraseña, con resolución de los permisos efectivos del usuario (rol más excepciones) y del o los locales asignados. La autenticación debe estar disponible localmente cuando no hay Internet.
- **Reglas de negocio asociadas:** RN-011, RN-012, RN-013, RN-086, RN-090, RN-092.
- **Criterios de aceptación:**
  - **CA1 — Ingreso válido**
    - Given un usuario activo con credenciales correctas y al menos un local asignado
    - When inicia sesión
    - Then el sistema debe otorgar acceso limitado a sus permisos efectivos y a sus locales asignados.
  - **CA2 — Usuario desactivado**
    - Given un usuario desactivado
    - When intenta iniciar sesión
    - Then el sistema debe negar el acceso
    - And debe conservar su historial de acciones previas.
  - **CA3 — Credenciales nunca legibles**
    - Given cualquier operación de consulta o mantenimiento de usuarios
    - When se visualiza la ficha del usuario
    - Then el sistema no debe mostrar la contraseña en texto legible en ninguna pantalla ni reporte.
  - **CA4 — Autenticación sin Internet**
    - Given un local sin conexión a Internet
    - When un usuario habilitado inicia sesión en ese local
    - Then el sistema debe autenticarlo localmente y permitir la operación del local.
  - **CA5 — Registro de sesión**
    - Given un inicio de sesión exitoso o fallido
    - When ocurre el evento
    - Then el sistema debe registrar la sesión como acción sensible auditable.
- **Dependencias:** US-56 (permisos), US-55 (usuarios), US-48 (operación offline para CA4).
- **Datos o entidades involucradas:** Usuario, Rol, Permiso, Local, sesión.
- **Riesgos, supuestos o pendientes:** el método de recuperación de acceso no está definido (`07` § Pendientes funcionales) — ver RF-006. La habilitación de la autenticación local para usuarios nuevos o con contraseña recién cambiada durante una caída no está documentada — ver RF-022.
- **Trazabilidad:** `05-usuarios-reportes-multilocal-offline.md` §§ Usuarios y permisos, Offline-first, Respaldo y seguridad; `06-modelo-de-datos.md` § Organización.
- **Estimación inicial:** M

::::

:::: {.historia title="US-54 — Acceder como mozo mediante PIN individual"}

**E5 — Supervisar la operación** · Prioridad **Must** · Release **MVP** · Estimación **S**

**Actor:** Mozo

> *Como mozo,*  
> *quiero identificarme con un PIN individual en un dispositivo compartido,*  
> *para cambiar de usuario rápidamente sin interrumpir la atención.*

- **Descripción funcional:** Acceso rápido por PIN individual para mozos en dispositivos compartidos del POS, con bloqueo temporal tras intentos fallidos. El PIN del mozo es distinto de la contraseña general y del PIN presencial del pensionista.
- **Reglas de negocio asociadas:** RN-011, RN-090, RN-091, RN-092.
- **Criterios de aceptación:**
  - **CA1 — Cambio rápido de mozo**
    - Given un dispositivo compartido con el POS abierto
    - When un mozo ingresa su PIN individual válido
    - Then el sistema debe identificarlo como el usuario activo del dispositivo y atribuirle las acciones que realice.
  - **CA2 — PIN no legible**
    - Given la ficha de un mozo
    - When un administrador la consulta
    - Then el sistema no debe mostrar el PIN en texto legible.
  - **CA3 — Bloqueo por intentos fallidos**
    - Given un número de intentos fallidos consecutivos de PIN
    - When se alcanza el umbral configurado
    - Then el sistema debe bloquear temporalmente el acceso por PIN.
- **Dependencias:** US-55 (usuarios y PIN), US-56 (permisos del rol).
- **Datos o entidades involucradas:** Usuario, PIN de mozo, Local.
- **Riesgos, supuestos o pendientes:** el plazo de bloqueo por intentos fallidos está pendiente de definición (`07` § Pendientes funcionales) — ver RF-006. **Pendiente de definición** para el valor del umbral y la duración del bloqueo.
- **Trazabilidad:** `05-usuarios-reportes-multilocal-offline.md` §§ Usuarios y permisos, Respaldo y seguridad.
- **Estimación inicial:** S

::::

:::: {.historia title="US-55 — Gestionar usuarios y su asignación multilocal"}

**E5 — Supervisar la operación** · Prioridad **Must** · Release **MVP** · Estimación **M**

**Actor:** Administrador central

> *Como administrador central,*  
> *quiero crear usuarios, asignarles roles y uno o varios locales, y desactivarlos cuando corresponda,*  
> *para controlar quién opera en cada local sin perder el historial de lo que hizo.*

- **Descripción funcional:** Mantenimiento de usuarios internos: datos de acceso, rol, excepciones de permisos, PIN cuando corresponde y asignación a uno o varios locales. La desactivación no elimina historial.
- **Reglas de negocio asociadas:** RN-007, RN-012, RN-013, RN-090.
- **Criterios de aceptación:**
  - **CA1 — Asignación a varios locales**
    - Given un usuario creado
    - When el administrador le asigna dos o más locales
    - Then el sistema debe permitirle operar en todos los locales asignados y en ninguno más.
  - **CA2 — Desactivación sin borrado**
    - Given un usuario con operaciones registradas
    - When se desactiva
    - Then el sistema debe impedir su acceso
    - And debe conservar su historial y su atribución en operaciones pasadas.
  - **CA3 — Excepciones de permisos**
    - Given un usuario con rol asignado
    - When se le añade una excepción de permiso
    - Then el sistema debe aplicar el permiso efectivo resultante del rol más la excepción.
- **Dependencias:** US-52, US-56.
- **Datos o entidades involucradas:** Usuario, Rol, Permiso, Local (Usuario N—N Local).
- **Riesgos, supuestos o pendientes:** la cantidad de usuarios concurrentes esperada no está definida (`07`) — ver RF-010.
- **Trazabilidad:** `05-usuarios-reportes-multilocal-offline.md` § Usuarios y permisos; `06-modelo-de-datos.md` § Organización.
- **Estimación inicial:** M

::::

:::: {.historia title="US-56 — Definir roles y permisos granulares"}

**E5 — Supervisar la operación** · Prioridad **Must** · Release **MVP** · Estimación **M**

**Actor:** Administrador central

> *Como administrador central,*  
> *quiero definir roles con permisos granulares por módulo y acción,*  
> *para que cada usuario pueda hacer exactamente lo que su función requiere sin depender del nombre de su cargo.*

- **Descripción funcional:** Configuración de roles y del conjunto de permisos por módulo y acción, con posibilidad de asignar excepciones a un usuario concreto. Los permisos mínimos configurables son: ver, crear, modificar, desactivar, aprobar, anular, descontar, reabrir, ver costos, exportar y administrar permisos. Todo cambio de permisos se audita.
- **Reglas de negocio asociadas:** RN-004, RN-012, RN-014.
- **Criterios de aceptación:**
  - **CA1 — Creación de rol con permisos**
    - Given un administrador con permiso de administrar permisos
    - When crea un rol y le asigna permisos por módulo y acción
    - Then el sistema debe guardar el rol con la combinación exacta de permisos seleccionada.
  - **CA2 — Los once permisos mínimos están disponibles**
    - Given la pantalla de configuración de un rol
    - When se listan las acciones configurables
    - Then el sistema debe ofrecer al menos ver, crear, modificar, desactivar, aprobar, anular, descontar, reabrir, ver costos, exportar y administrar permisos.
  - **CA3 — Independencia del cargo**
    - Given dos usuarios con el mismo cargo
    - When se les asignan roles o excepciones distintas
    - Then el sistema debe aplicar a cada uno sus permisos efectivos y no los del cargo.
  - **CA4 — Restricción efectiva**
    - Given un usuario sin el permiso "ver costos"
    - When accede a una receta o a un reporte que contiene costos
    - Then el sistema no debe mostrarle los costos.
  - **CA5 — Auditoría del cambio**
    - Given un cambio de permisos
    - When se guarda
    - Then el sistema debe registrar usuario, fecha, hora, local, valor anterior y valor nuevo.
  - **CA6 — Exclusión de RRHH**
    - Given la configuración de módulos disponibles
    - When se revisan las opciones
    - Then el sistema no debe ofrecer asistencia, horarios laborales, planilla ni remuneraciones.
- **Dependencias:** US-52 (locales para el ámbito del permiso).
- **Datos o entidades involucradas:** Rol, Permiso, relación Rol N—N Permiso, excepciones por Usuario.
- **Riesgos, supuestos o pendientes:** ninguno documentado.
- **Trazabilidad:** `05-usuarios-reportes-multilocal-offline.md` § Usuarios y permisos; `01-alcance-y-reglas.md` § Auditoría transversal; `06-modelo-de-datos.md` § Organización.
- **Estimación inicial:** M

::::

:::: {.historia title="US-57 — Registrar auditoría transversal de acciones sensibles"}

**E5 — Supervisar la operación** · Prioridad **Must** · Release **MVP** · Estimación **M**

**Actor:** Administrador central / Administrador de local

> *Como administrador,*  
> *quiero que el sistema registre automáticamente quién, cuándo, dónde y qué cambió en cada acción sensible,*  
> *para poder auditar la operación y sustentar cualquier revisión posterior.*

- **Descripción funcional:** Servicio transversal de auditoría que captura usuario, fecha, hora, local, valores anteriores y nuevos y el motivo cuando aplique, para el conjunto de eventos definido en la especificación. Es una capacidad compartida consumida por historias de otras épicas.
- **Reglas de negocio asociadas:** RN-004, RN-092.
- **Criterios de aceptación:**
  - **CA1 — Eventos cubiertos**
    - Given cualquiera de los eventos: descuento, anulación, reapertura, ajuste de inventario, transferencia de inventario, cambio de receta, cambio de precio, cambio de permisos, aprobación o rechazo de abono, cierre de caja
    - When el evento se confirma
    - Then el sistema debe registrar usuario, fecha, hora, local, valores anteriores y nuevos.
  - **CA2 — Motivo obligatorio cuando aplica**
    - Given un evento que la especificación exige justificar (descuento, anulación, reactivación de producto agotado, ajuste)
    - When el usuario confirma el evento sin indicar motivo
    - Then el sistema debe impedir la confirmación hasta que se registre el motivo.
  - **CA3 — Consulta de la bitácora**
    - Given un usuario con permiso de ver auditoría
    - When consulta la bitácora filtrando por local, usuario, fecha o tipo de evento
    - Then el sistema debe mostrar los registros correspondientes.
  - **CA4 — Inmutabilidad**
    - Given un registro de auditoría existente
    - When cualquier usuario intenta modificarlo o eliminarlo
    - Then el sistema debe impedirlo.
- **Dependencias:** US-53 (identidad del usuario), US-52 (local).
- **Datos o entidades involucradas:** Registro de auditoría (usuario, fecha, hora, local, valor anterior, valor nuevo, motivo, tipo de evento).
- **Riesgos, supuestos o pendientes:** **Supuesto de análisis:** la especificación no define el tiempo de retención de la bitácora; se relaciona con la política de retención pendiente — ver RF-011.
- **Trazabilidad:** `01-alcance-y-reglas.md` § Auditoría transversal; `05-usuarios-reportes-multilocal-offline.md` § Respaldo y seguridad.
- **Estimación inicial:** M

::::

:::: {.historia title="US-58 — Configurar horarios de servicio por local"}

**E5 — Supervisar la operación** · Prioridad **Must** · Release **MVP** · Estimación **S**

**Actor:** Administrador central

> *Como administrador central,*  
> *quiero configurar los horarios de desayuno, almuerzo y cena,*  
> *para que el sistema determine automáticamente el servicio del pensionista y controle el vencimiento de los pre-tickets.*

- **Descripción funcional:** Parámetro de configuración de los tres servicios con su rango horario. Este parámetro alimenta la determinación automática del servicio en el autoservicio del pensionista y el vencimiento del pre-ticket pendiente.
- **Reglas de negocio asociadas:** RN-024, RN-026.
- **Criterios de aceptación:**
  - **CA1 — Configuración de rangos**
    - Given un administrador con permiso de modificar configuración
    - When define el rango horario de desayuno, almuerzo y cena
    - Then el sistema debe guardar la configuración y usarla para determinar el servicio.
  - **CA2 — Determinación automática del servicio**
    - Given un pensionista que se identifica en el autoservicio dentro del rango del almuerzo
    - When el sistema determina el servicio
    - Then debe asignar automáticamente "almuerzo" sin intervención del usuario.
  - **CA3 — Vencimiento por fin de horario**
    - Given un pre-ticket en estado pendiente
    - When termina el horario del servicio al que pertenece
    - Then el sistema debe cambiar su estado a vencido.
- **Dependencias:** US-52.
- **Datos o entidades involucradas:** configuración de horarios de servicio, Local, PreTicket, Servicio.
- **Riesgos, supuestos o pendientes:** **Pendiente de definición** — los horarios exactos de desayuno, almuerzo y cena no han sido proporcionados (`07` § Pendientes funcionales). La historia es implementable como parámetro configurable; los valores iniciales deben cargarse cuando negocio los defina. Ver RF-001. Tampoco se documenta si los horarios pueden diferir entre locales — **Supuesto de análisis:** se modela como configuración central con posibilidad de valor por local, sujeto a confirmación.
- **Trazabilidad:** `02-pensionistas.md` § Autoservicio presencial; `07-criterios-de-aceptacion-y-pendientes.md` §§ Casos críticos (2), Pendientes funcionales.
- **Estimación inicial:** S

::::

:::: {.historia title="US-59 — Consultar el panel del administrador con filtros"}

**E5 — Supervisar la operación** · Prioridad **Should** · Release **Post-MVP** · Estimación **L**

**Actor:** Administrador central / Administrador de local

> *Como administrador,*  
> *quiero un panel que priorice ventas y rentabilidad, caja, pensionistas e inventario con filtros,*  
> *para revisar el estado del negocio sin abrir un reporte por cada pregunta.*

- **Descripción funcional:** Panel con las cuatro prioridades definidas (ventas y rentabilidad; caja y diferencias; pensionistas y saldos; inventario y costos) y filtros por día, semana, mes, turno, local, canal, producto, plan y tipo de cliente.
- **Reglas de negocio asociadas:** RN-012, RN-085, RN-002.
- **Criterios de aceptación:**
  - **CA1 — Prioridades del panel**
    - Given un administrador con permisos
    - When abre el panel
    - Then el sistema debe mostrar ventas y rentabilidad, caja y diferencias, pensionistas y saldos, e inventario y costos.
  - **CA2 — Filtros disponibles**
    - Given el panel abierto
    - When se aplican filtros
    - Then el sistema debe permitir filtrar por día, semana, mes, turno, local, canal, producto, plan y tipo de cliente.
  - **CA3 — Restricción por permisos y locales**
    - Given un usuario sin permiso de ver costos o asignado a un solo local
    - When abre el panel
    - Then el sistema no debe mostrarle costos ni datos de locales no asignados.
- **Dependencias:** US-60, US-61, US-62, US-63, US-64.
- **Datos o entidades involucradas:** agregados de Pedido, Pago, TurnoCaja, CuentaPensionista, Existencia, MovimientoInventario.
- **Riesgos, supuestos o pendientes:** ninguno documentado.
- **Trazabilidad:** `05-usuarios-reportes-multilocal-offline.md` § Panel del administrador.
- **Estimación inicial:** L

::::

:::: {.historia title="US-60 — Reportar ventas, costo histórico, utilidad y margen"}

**E5 — Supervisar la operación** · Prioridad **Should** · Release **Post-MVP** · Estimación **L**

**Actor:** Administrador central / Administrador de local

> *Como administrador,*  
> *quiero reportes de ventas por local, canal, servicio, producto y usuario con su rentabilidad,*  
> *para saber qué se vende y cuánto deja realmente.*

- **Descripción funcional:** Reportes de ventas segmentables por local, canal, servicio, producto y usuario, mostrando precio, costo histórico, utilidad y margen a partir del snapshot guardado en cada venta. Exportables a Excel y PDF.
- **Reglas de negocio asociadas:** RN-005, RN-046, RN-085, RN-012.
- **Criterios de aceptación:**
  - **CA1 — Segmentación**
    - Given ventas registradas
    - When se consulta el reporte
    - Then el sistema debe permitir agrupar por local, canal, servicio, producto y usuario.
  - **CA2 — Rentabilidad con costo histórico**
    - Given una venta cerrada
    - When se calcula su utilidad y margen en el reporte
    - Then el sistema debe usar el costo histórico guardado en la venta y no el costo actual.
  - **CA3 — Exportación**
    - Given un reporte generado
    - When el usuario con permiso de exportar lo descarga
    - Then el sistema debe entregarlo en Excel y en PDF.
  - **CA4 — Permiso de costos**
    - Given un usuario sin permiso de ver costos
    - When abre el reporte
    - Then el sistema no debe mostrar costo, utilidad ni margen.
- **Dependencias:** US-19, US-41, US-37, US-56.
- **Datos o entidades involucradas:** Pedido, DetallePedido (snapshot de costo/receta), Pago, Producto, Usuario, Local, Canal.
- **Riesgos, supuestos o pendientes:** ninguno documentado.
- **Trazabilidad:** `05-usuarios-reportes-multilocal-offline.md` §§ Reportes mínimos, Panel del administrador; `04-menu-recetas-inventario-compras.md` § Recetas y costos; `01-alcance-y-reglas.md` § Reglas de historial.
- **Estimación inicial:** L

::::

:::: {.historia title="US-61 — Reportar cierres y diferencias de caja"}

**E5 — Supervisar la operación** · Prioridad **Should** · Release **Post-MVP** · Estimación **M**

**Actor:** Administrador

> *Como administrador,*  
> *quiero un reporte de cierres de caja con sus diferencias,*  
> *para controlar el manejo de efectivo por turno, caja y cajero.*

- **Descripción funcional:** Reporte de cierres de caja con esperado, contado, diferencia y observación, filtrable por local, turno y cajero, exportable a Excel y PDF.
- **Reglas de negocio asociadas:** RN-068, RN-070, RN-085.
- **Criterios de aceptación:**
  - **CA1 — Contenido**
    - Given cierres de caja realizados
    - When se consulta el reporte
    - Then debe mostrar esperado, contado, diferencia y observación por turno, caja, cajero y local.
  - **CA2 — Diferencias visibles**
    - Given un cierre con diferencia distinta de cero
    - When se consulta el reporte
    - Then el sistema debe destacar la diferencia.
  - **CA3 — Exportación**
    - Given el reporte generado
    - When se exporta
    - Then debe estar disponible en Excel y PDF.
- **Dependencias:** US-22.
- **Datos o entidades involucradas:** CierreCaja, TurnoCaja, MovimientoCaja, Usuario, Local.
- **Riesgos, supuestos o pendientes:** ninguno documentado.
- **Trazabilidad:** `05-usuarios-reportes-multilocal-offline.md` § Reportes mínimos; `03-pos-caja-cocina-delivery.md` § Caja.
- **Estimación inicial:** M

::::

:::: {.historia title="US-62 — Reportar pensionistas, saldos, estados y abonos"}

**E5 — Supervisar la operación** · Prioridad **Should** · Release **Post-MVP** · Estimación **M**

**Actor:** Administrador

> *Como administrador,*  
> *quiero reportes de deuda, saldo a favor, límite, crédito disponible, estados mensuales y abonos por estado,*  
> *para gestionar la cobranza de los pensionistas.*

- **Descripción funcional:** Reportes de la cartera de pensionistas: deuda, saldo a favor, límite y crédito disponible; estados de cuenta mensuales; y abonos pendientes, aprobados y rechazados. Exportables a Excel y PDF, conservando el PDF fijo de cada estado mensual cerrado.
- **Reglas de negocio asociadas:** RN-016, RN-085, RN-031, RN-003.
- **Criterios de aceptación:**
  - **CA1 — Cartera de pensionistas**
    - Given pensionistas con movimientos
    - When se consulta el reporte
    - Then debe mostrar deuda, saldo a favor, límite y crédito disponible por pensionista.
  - **CA2 — Abonos por estado**
    - Given abonos registrados
    - When se consulta el reporte
    - Then debe permitir filtrar por pendientes, aprobados y rechazados.
  - **CA3 — Estados mensuales**
    - Given estados mensuales cerrados
    - When se consultan
    - Then el sistema debe entregar el PDF fijo conservado en el cierre.
  - **CA4 — Movimientos multilocal**
    - Given un pensionista con consumos en varios locales
    - When se consulta su detalle
    - Then el sistema debe mostrar el local de cada movimiento.
- **Dependencias:** US-17, US-26, US-28.
- **Datos o entidades involucradas:** CuentaPensionista, MovimientoCuenta, EstadoCuentaMensual, Abono, PlanTarifario.
- **Riesgos, supuestos o pendientes:** ninguno documentado.
- **Trazabilidad:** `05-usuarios-reportes-multilocal-offline.md` § Reportes mínimos; `02-pensionistas.md` §§ Cuenta corriente, Abonos; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (20).
- **Estimación inicial:** M

::::

:::: {.historia title="US-63 — Reportar inventario, Kardex y producción"}

**E5 — Supervisar la operación** · Prioridad **Should** · Release **Post-MVP** · Estimación **M**

**Actor:** Administrador

> *Como administrador,*  
> *quiero reportes de inventario, Kardex, stock negativo, stock mínimo, ajustes y producción,*  
> *para controlar existencias y detectar desviaciones entre lo planificado y lo vendido.*

- **Descripción funcional:** Reportes de existencias, Kardex, alertas de stock negativo y de stock mínimo, ajustes realizados, y producción con porciones planificadas, preparadas, vendidas y sobrantes. Exportables a Excel y PDF.
- **Reglas de negocio asociadas:** RN-050, RN-051, RN-052, RN-048, RN-085.
- **Criterios de aceptación:**
  - **CA1 — Inventario y Kardex**
    - Given movimientos registrados
    - When se consulta el reporte
    - Then debe mostrar existencias por local y el Kardex con origen de cada movimiento.
  - **CA2 — Alertas**
    - Given productos en stock negativo o bajo el mínimo
    - When se consulta el reporte
    - Then el sistema debe listarlos identificando la condición.
  - **CA3 — Producción**
    - Given producciones registradas
    - When se consulta el reporte
    - Then debe mostrar porciones planificadas, preparadas, vendidas y sobrantes.
  - **CA4 — Exportación**
    - Given cualquiera de estos reportes
    - When se exporta
    - Then debe estar disponible en Excel y PDF.
- **Dependencias:** US-39, US-40, US-41, US-43.
- **Datos o entidades involucradas:** Existencia, MovimientoInventario, Ajuste, Produccion, Producto, Local.
- **Riesgos, supuestos o pendientes:** ninguno documentado.
- **Trazabilidad:** `05-usuarios-reportes-multilocal-offline.md` § Reportes mínimos; `04-menu-recetas-inventario-compras.md` §§ Inventario y Kardex, Producción.
- **Estimación inicial:** M

::::

:::: {.historia title="US-64 — Reportar delivery, anulaciones y consumo interno"}

**E5 — Supervisar la operación** · Prioridad **Could** · Release **Post-MVP** · Estimación **M**

**Actor:** Administrador

> *Como administrador,*  
> *quiero reportes de delivery e incidencias, de productos anulados después de comanda y de consumo interno,*  
> *para controlar las pérdidas y el desempeño de la operación no presencial.*

- **Descripción funcional:** Reportes operativos: delivery, entregas, incidencias y liquidación de motorizados; productos anulados después de comanda; consumos internos. Exportables a Excel y PDF.
- **Reglas de negocio asociadas:** RN-084, RN-065, RN-056, RN-085.
- **Criterios de aceptación:**
  - **CA1 — Delivery**
    - Given pedidos de delivery registrados
    - When se consulta el reporte
    - Then debe mostrar entregas, incidencias y liquidación por motorizado.
  - **CA2 — Anulaciones después de comanda**
    - Given productos anulados después de comandar
    - When se consulta el reporte
    - Then debe listarlos con producto, motivo, usuario, fecha y local.
  - **CA3 — Consumo interno**
    - Given consumos internos registrados
    - When se consulta el reporte
    - Then debe mostrarlos separados de las ventas con trabajador, platos, turno y costo.
  - **CA4 — Exportación**
    - Given cualquiera de estos reportes
    - When se exporta
    - Then debe estar disponible en Excel y PDF.
- **Dependencias:** US-09, US-14, US-44.
- **Datos o entidades involucradas:** PedidoDelivery, HistorialDelivery, DetallePedido anulado, consumo interno.
- **Riesgos, supuestos o pendientes:** ninguno documentado.
- **Trazabilidad:** `05-usuarios-reportes-multilocal-offline.md` § Reportes mínimos; `03-pos-caja-cocina-delivery.md` §§ Comandas, Delivery propio; `04-menu-recetas-inventario-compras.md` § Consumo interno.
- **Estimación inicial:** M

::::


# 4. Tareas técnicas habilitantes

No son historias de usuario. Nadie del restaurante las pide, no le entregan nada directamente y
no se pueden escribir con «como… quiero… para…» sin inventar un usuario que no existe. Son
trabajo necesario para que las historias del apartado 3 puedan construirse, y se listan aparte
precisamente para no confundir una cosa con la otra.

| Código | Tarea técnica | Qué resuelve | Habilita a |
|----------|-----------------------------|----------------------------------------------------------|----------------------------|
| HT-01 | **Esqueleto que camina** | Camino completo del navegador al gateway, de ahí a los dos servicios y a la base de datos. Resuelve de una vez la red, el enrutamiento y la comunicación entre servicios. | Todas las historias |
| HT-02 | **Migraciones con un único dueño** | Flyway gobierna el esquema y Hibernate solo valida, de modo que la base de datos sea reproducible en cualquier equipo y en cualquier local. | Todas las historias |
| HT-03 | **Tipos base del dominio** | Identificador generado en el origen y montos sin decimales flotantes, fijados antes de crear la primera tabla. Es la decisión más cara de revertir del proyecto. | Todas las historias |
| HT-05 | **Generación de tipos desde OpenAPI** | El frontend genera sus tipos desde la especificación del backend, para que las dos partes no se desincronicen en silencio. | Todas las pantallas |
| HT-06 | **Verificación automática de fronteras** | La compilación falla si un módulo invade a otro, de modo que la arquitectura no dependa de la memoria del equipo. | Todas las historias |
| HT-08 | **Identificador de correlación en los registros** | Un mismo identificador recorre los servicios y las colas, para poder responder «¿por qué esta venta no llegó a facturación?» sin adivinar. | Emisión del comprobante · Sincronización |
| HT-04 | **Patrón outbox** | El evento se escribe en la misma transacción que el dato y un proceso lo publica después. Es a la vez el canal entre los dos servicios y el mecanismo de sincronización del local con la nube. | Sincronizar sin duplicar · Emitir comprobantes |
| HT-10 | **Prueba de impresión con la comandera real** | Confirma que la comanda sale en el hardware del local con Internet desconectado. Puede invalidar el diseño del agente de impresión, por eso se hace antes de cerrarlo. | Emitir la comanda a cocina |

# 5. Reglas de negocio consolidadas

Catálogo único, sin duplicados. Cuando una regla aparecía en varios documentos, se consolidó en un solo ID y se listaron todas sus fuentes.

| ID regla | Regla de negocio | Módulo | Fuente(s) | Historia(s) relacionada(s) |
|----------|-------------------------------------------|------------|----------------------------|--------------------------------|
| RN-001 | Catálogo, recetas, precios, planes y programación del menú se administran centralmente y son comunes a todos los locales | Transversal / Maestros | `01` Multilocal | US-33, US-34, US-37, US-02, US-03, US-16 |
| RN-002 | Cada local mantiene inventario, compras, producción, caja, mesas, disponibilidad y delivery propios, con un único inventario general | Transversal / Multilocal | `01` Multilocal; `06` Organización | US-52, US-04, US-05, US-18, US-38, US-59 |
| RN-003 | La cuenta del pensionista es única y utilizable en cualquier local, y cada movimiento identifica su local | Pensionistas | `01` Multilocal; `07` Caso 20 | US-17, US-62 |
| RN-004 | Descuentos, anulaciones, reaperturas, ajustes y transferencias de inventario, cambios de recetas, precios y permisos, aprobación o rechazo de abonos y cierres de caja registran usuario, fecha, hora, local, valores anterior y nuevo y motivo cuando aplique | Transversal / Auditoría | `01` Auditoría transversal | US-57, US-37, US-04, US-26, US-09, US-20, US-21, US-22, US-40, US-45 |
| RN-005 | Las ventas cerradas conservan precio, costo y composición históricos | Venta | `01` Reglas de historial; `06` Reglas de integridad | US-07, US-19, US-60 |
| RN-006 | Los cierres mensuales de pensionistas y los cierres de caja son inalterables después de cerrados y no aceptan movimientos retroactivos | Pensionistas / Caja | `01` Reglas de historial; `06` Reglas de integridad; `07` Caso 6 | US-28, US-21, US-22 |
| RN-007 | Desactivar registros no elimina su historial | Transversal | `01` Reglas de historial; `05` Usuarios y permisos | US-52, US-55, US-33, US-35, US-32 |
| RN-008 | Los pensionistas pueden utilizar su cuenta en todos los canales | Pensionistas / Canales | `01` Canales | US-17, US-06, US-10, US-12 |
| RN-009 | Para llevar, recojo y delivery añaden un cargo de táper por cada menú | Venta / Canales | `01` Canales; `03` Recojo programado | US-06, US-10, US-12 |
| RN-010 | Una transferencia entre locales genera ajustes enlazados de salida y entrada al confirmarse | Inventario | `01` Multilocal; `04` Inventario y Kardex; `06` Inventario | US-45 |
| RN-011 | El acceso general es por usuario y contraseña; los mozos usan PIN individual para cambio rápido en dispositivos compartidos | Usuarios | `05` Usuarios y permisos | US-53, US-54 |
| RN-012 | Los permisos son granulares por módulo y acción (ver, crear, modificar, desactivar, aprobar, anular, descontar, reabrir, ver costos, exportar, administrar permisos) y no dependen del nombre del cargo | Usuarios | `05` Usuarios y permisos; `06` Organización | US-56, US-55, US-37, US-09, US-20, US-40, US-44, US-59, US-60 |
| RN-013 | Un usuario puede asignarse a uno o varios locales | Usuarios | `05` Usuarios y permisos; `06` Organización | US-53, US-55 |
| RN-014 | El sistema no incluye asistencia, horarios laborales, planilla ni remuneraciones | Fuera de alcance | `05` Usuarios y permisos; `README` Exclusiones | US-56 |
| RN-015 | El consumo del pensionista se registra únicamente al cerrar en caja | Pensionistas | `02` Cuenta corriente; `07` Caso 1 | US-17, US-19, US-10 |
| RN-016 | Crédito disponible = límite individual − deuda pendiente | Pensionistas | `02` Cuenta corriente | US-17, US-62 |
| RN-017 | El límite de crédito es preventivo: alerta al superarlo pero permite el consumo | Pensionistas | `02` Cuenta corriente; `07` Caso 4 | US-17, US-19 |
| RN-018 | Se aplica primero el saldo a favor y el remanente aumenta la deuda | Pensionistas | `02` Cuenta corriente | US-17, US-19 |
| RN-019 | Se permiten múltiples consumos del mismo servicio en un día y cada uno se cobra | Pensionistas | `02` Cuenta corriente; `07` Caso 3 | US-17 |
| RN-020 | El cierre mensual arrastra deuda o saldo a favor al mes siguiente sin marcar atraso automáticamente | Pensionistas | `02` Cuenta corriente; `07` Caso 6 | US-28 |
| RN-021 | Los planes tarifarios definen precio de almuerzo normal y precios especiales por producto fijo de desayuno y cena, con vigencia, estado e historial | Pensionistas | `02` Planes tarifarios; `06` Pensionistas | US-15, US-16 |
| RN-022 | Los consumos anteriores conservan la tarifa aplicada | Pensionistas | `02` Planes tarifarios | US-16 |
| RN-023 | El menú extra se cobra al precio completo del extra, sin tarifa reducida | Pensionistas / Menú | `02` Planes tarifarios; `04` Almuerzo | US-03, US-16 |
| RN-024 | El servicio se determina automáticamente por horario configurable | Pensionistas | `02` Autoservicio presencial | US-58, US-23 |
| RN-025 | El pre-ticket no selecciona platos, no constituye consumo cargado y muestra código único, pensionista, servicio y fecha/hora con la leyenda correspondiente | Pensionistas | `02` Autoservicio presencial | US-23, US-24 |
| RN-026 | Los estados del pre-ticket son pendiente, vinculado, cerrado, vencido y anulado; el pendiente vence al terminar el horario del servicio | Pensionistas | `02` Autoservicio presencial; `07` Caso 2 | US-58, US-23, US-24 |
| RN-027 | Un pre-ticket vencido o anulado no genera cargo, y no se permite la doble vinculación | Pensionistas | `02` Autoservicio presencial | US-23, US-24 |
| RN-028 | El portal es un acceso único compartido por pensionista y responsable mediante DNI y contraseña, y no permite modificar consumos ni aprobar abonos | Pensionistas / Portal | `02` Portal compartido | US-30, US-31 |
| RN-029 | Las notificaciones internas se emiten solo por abono aprobado o rechazado y por estado mensual generado, nunca por consumo ni por superar el límite | Pensionistas / Portal | `02` Portal compartido | US-30, US-26, US-28 |
| RN-030 | El abono en efectivo lo registra caja, queda confirmado inmediatamente y entra en conciliación | Pensionistas / Caja | `02` Abonos | US-25, US-22 |
| RN-031 | Los abonos digitales quedan pendientes y no modifican el saldo hasta la aprobación exclusiva del administrador | Pensionistas | `02` Abonos; `07` Caso 5 | US-25, US-31, US-26, US-62 |
| RN-032 | El reporte de abono desde el portal exige imagen de comprobante, número de operación, monto, fecha, medio y cuenta destino, y valida la posible duplicidad del número de operación | Pensionistas / Portal | `02` Abonos; `06` Reglas de integridad | US-31 |
| RN-033 | Se permiten abonos parciales y frecuencias habituales semanal, quincenal o mensual | Pensionistas | `02` Abonos, Ficha | US-15, US-25 |
| RN-034 | Un pensionista desactivado no genera nuevos tickets ni consumos, conserva cuenta, historial y portal, admite abonos posteriores, y su reactivación reutiliza la misma cuenta | Pensionistas | `02` Desactivación | US-23, US-32 |
| RN-035 | Se emite un comprobante electrónico por cada consumo al cerrar en caja, a nombre y DNI del pensionista; el abono no genera venta y puede generar constancia interna | Pensionistas / Facturación | `02` Facturación | US-25, US-19, US-27, US-11 |
| RN-036 | El DNI del pensionista es único en la empresa y el responsable es obligatorio | Pensionistas | `02` Ficha; `06` Reglas de integridad | US-15 |
| RN-037 | El menú normal se compone de entrada, segundo con guarnición, refresco y postre, con precio único independiente de las opciones elegidas | Menú | `04` Almuerzo | US-03, US-06 |
| RN-038 | Si hay varias opciones el mozo informa y registra la elegida; si hay una sola se asigna directamente | Menú / POS | `04` Almuerzo | US-24, US-06 |
| RN-039 | La programación semanal se hace por día y servicio, con estados borrador, confirmado y en servicio, registrando opciones y porciones estimadas | Menú | `04` Programación semanal; `06` Menú y producción | US-02 |
| RN-040 | El menú confirmado es editable sin modificar el catálogo maestro | Menú | `04` Programación semanal | US-02 |
| RN-041 | El sistema advierte si un segundo o guiso aparece en los tres días anteriores, pero permite confirmar | Menú | `04` Programación semanal | US-02 |
| RN-042 | El menú solo se publica dentro del POS; desayuno y cena usan productos fijos elegidos individualmente y no llevan programación semanal | Menú | `04` Programación semanal; `README` Exclusiones | US-02, US-06 |
| RN-043 | Cada componente tiene receta valorizada y costo por porción, y se permiten subrecetas dentro de otras recetas | Recetas y costos | `04` Recetas y costos; `06` Catálogo y costos | US-37, US-41 |
| RN-044 | Las recetas se valorizan con el costo de última compra y una nueva compra recalcula costos actuales y márgenes futuros | Recetas y costos | `04` Recetas y costos, Compras; `07` Caso 10 | US-37, US-42 |
| RN-045 | La actualización de la receta maestra aplica únicamente a nuevas ventas | Recetas y costos | `04` Recetas y costos | US-37 |
| RN-046 | Cada venta guarda copia histórica de componentes, receta aplicada, costos, precio, utilidad y margen | Venta / Costos | `04` Recetas y costos; `06` Venta | US-19, US-60 |
| RN-047 | Producir una subreceta descuenta sus insumos e ingresa el rendimiento; vender el plato final descuenta su receta incluidas las subrecetas, sin duplicar consumos | Producción / Inventario | `04` Producción; `07` Caso 11 | US-19, US-41 |
| RN-048 | Se registran porciones planificadas, preparadas y vendidas por plato, y no se calcula automáticamente la lista de insumos requeridos desde la planificación | Producción | `04` Producción | US-41, US-63 |
| RN-049 | El encargado decide el destino del sobrante entre conservar, personal, pérdida, reutilización u otro, y ese destino produce el ajuste de inventario correspondiente | Producción / Inventario | `04` Producción | US-43 |
| RN-050 | El stock negativo se permite con alerta y no bloquea movimientos | Inventario | `04` Inventario y Kardex | US-38, US-63 |
| RN-051 | El stock mínimo es una cantidad configurable por producto y local | Inventario | `04` Inventario y Kardex | US-38, US-63 |
| RN-052 | Los ajustes usan una lista configurable de motivos y observación, y guardan stock anterior, stock nuevo, diferencia, costo, usuario, fecha y local | Inventario | `04` Inventario y Kardex; `06` Inventario | US-36, US-40, US-46, US-43, US-63 |
| RN-053 | Los conteos físicos son totales y periódicos, no detienen la operación, toman snapshot inicial y concilian compras, ventas, producción y ajustes del periodo | Inventario | `04` Inventario y Kardex; `06` Inventario; `07` Caso 12 | US-46 |
| RN-054 | Confirmar una compra aumenta el stock y actualiza el costo de última compra, asociando el comprobante con tipo, serie, número, proveedor, fecha, productos, presentaciones, cantidades, precios, total y archivo adjunto | Compras | `04` Compras; `06` Catálogo y costos; `07` Caso 10 | US-42 |
| RN-055 | El registro de compras es básico: no hay orden de compra ni cuenta por pagar | Compras | `04` Compras; `README` Exclusiones | US-35, US-42 |
| RN-056 | El consumo interno permite una comida sin cobro por trabajador y turno; la segunda requiere autorización según permiso, registra trabajador, platos, turno y costo, descuenta inventario y se reporta separado de las ventas | Consumo interno | `04` Consumo interno; `05` Reportes mínimos | US-44, US-64 |
| RN-057 | Todo movimiento de inventario debe tener origen rastreable entre compra, producción, venta, ajuste, transferencia, conteo o consumo interno | Inventario | `06` Inventario, Reglas de integridad | US-09, US-11, US-38, US-39, US-40, US-45, US-46, US-41, US-43, US-42, US-44 |
| RN-058 | El POS es una PWA responsive para celular, tablet y terminal fija | POS | `03` POS y mesas; `README` Alcance; `05` Restricción tecnológica | US-05, US-06 |
| RN-059 | Los estados de mesa son libre, ocupada, unida, pendiente de cierre y bloqueada | POS | `03` POS y mesas | US-05, US-07 |
| RN-060 | Se permiten mesas mixtas con clientes regulares y varios pensionistas, asignando productos a cada persona al tomar el pedido | POS | `03` POS y mesas; `07` Caso 7 | US-24, US-06, US-07 |
| RN-061 | Se permiten cierres parciales sin liberar la mesa mientras haya consumos pendientes | POS / Caja | `03` POS y mesas | US-19 |
| RN-062 | Existe una sola impresora central de cocina y la comanda muestra pedido/mesa/canal, mozo, hora, productos por persona, componentes, observaciones y tipo de entrega | Cocina | `03` Comandas | US-08 |
| RN-063 | Toda reimpresión de comanda se marca como copia | Cocina | `03` Comandas | US-08 |
| RN-064 | Cocina informa el pedido listo verbalmente o mediante campana y no requiere cambio digital de estado | Cocina | `03` Comandas | US-08 |
| RN-065 | Un producto anulado después de comandar no se cobra pero descuenta receta como consumo o pérdida; anulado antes de comandar no descuenta inventario | Cocina / Inventario | `03` Comandas; `07` Caso 9 | US-09, US-64 |
| RN-066 | Un producto marcado como agotado se oculta en pedidos nuevos sin modificar comandas existentes, y su reactivación registra usuario, hora y motivo | Disponibilidad | `03` Disponibilidad; `07` Caso 8 | US-04, US-06 |
| RN-067 | La caja se abre y se cierra por turno y el cajero puede ver el efectivo esperado antes de contar | Caja | `03` Caja | US-18, US-22 |
| RN-068 | El cierre concilia fondo inicial, cobros, abonos en efectivo y medios digitales o tarjeta | Caja | `03` Caja | US-22, US-61 |
| RN-069 | No se permiten gastos, retiros ni ingresos extraordinarios en caja; la única excepción es el abono en efectivo de pensionista | Caja | `03` Caja; `06` Caja | US-25, US-18, US-19 |
| RN-070 | Cualquier diferencia de caja desde S/0.01 genera alerta sin bloquear el cierre | Caja | `03` Caja; `07` Caso 18 | US-22, US-61 |
| RN-071 | Una cuenta puede dividirse libremente entre efectivo, Yape, Plin y tarjeta | Caja | `03` Caja | US-19 |
| RN-072 | Los descuentos y anulaciones requieren permisos específicos y motivo | Caja | `03` Caja; `07` Caso 19 | US-09, US-20 |
| RN-073 | Solo un administrador autorizado puede reabrir una venta y únicamente mientras el turno de origen siga abierto | Caja | `03` Caja; `06` Reglas de integridad; `07` Caso 17 | US-21 |
| RN-074 | Después del cierre de turno las correcciones se hacen mediante operación compensatoria o documento relacionado, sin alterar el cierre original | Caja | `03` Caja | US-21 |
| RN-075 | La facturación electrónica se integra por API externa, guarda respuesta, estado y errores, y usa cola offline, reintentos idempotentes y prevención de comprobantes duplicados | Facturación | `03` Facturación electrónica; `07` Caso 16 | US-27, US-49 |
| RN-076 | El recojo programado se recibe por WhatsApp, se registra manualmente y guarda fecha y hora futura, nombre y celular de quien recoge | Recojo | `03` Recojo programado | US-10 |
| RN-077 | En recojo, el cliente regular paga por anticipado antes de confirmar y al pensionista se le carga al cerrar el pedido | Recojo | `03` Recojo programado; `07` Caso 13 | US-10 |
| RN-078 | La cancelación de un recojo es sin cobro si ocurre antes de enviar la comanda | Recojo | `03` Recojo programado | US-10 |
| RN-079 | Un recojo preparado y no retirado descuenta inventario y se registra como no recogido: al cliente regular no se le devuelve el pago y al pensionista se le carga el pedido completo con comprobante | Recojo | `03` Recojo programado; `07` Casos 13 y 14 | US-11 |
| RN-080 | El delivery es propio, inicialmente gratuito y sin zonas, y caja decide si acepta la dirección | Delivery | `03` Delivery propio; `README` Alcance | US-12 |
| RN-081 | Los estados del delivery son pendiente, asignado, aceptado, recogido, en ruta, entregado y fallido, y la entrega se confirma solo marcando entregado | Delivery | `03` Delivery propio; `07` Caso 15 | US-12, US-13, US-14 |
| RN-082 | El motorizado usa vista web móvil, consulta dirección y teléfono, cambia estados y comparte su última ubicación mientras está en ruta | Delivery | `03` Delivery propio | US-13 |
| RN-083 | Los motorizados tienen sueldo fijo y no se calculan comisiones | Delivery | `03` Delivery propio; `README` Exclusiones | US-13, US-14 |
| RN-084 | Caja cierra la venta de delivery después de revisar y liquidar el pedido, controlando el dinero cobrado y las incidencias por motorizado | Delivery / Caja | `03` Delivery propio; `07` Caso 15 | US-14, US-64 |
| RN-085 | Los reportes y estados de cuenta se exportan a Excel y PDF, y cada estado mensual cerrado conserva un PDF fijo | Reportes | `05` Reportes mínimos; `README` Alcance | US-28, US-59, US-60, US-61, US-62, US-63, US-64 |
| RN-086 | El local opera completo sin Internet (autenticación local, POS, pensionistas, impresora, caja, inventario, producción y delivery local) y conserva una copia local del saldo necesario para operar pensionistas | Offline | `05` Offline-first; `07` Caso 16 | US-53, US-23, US-08, US-48, US-49 |
| RN-087 | La sincronización previene duplicados mediante identificadores únicos e idempotencia, resuelve conflictos sin perder transacciones y deja registro de la resolución | Offline | `05` Offline-first; `07` Caso 16 | US-27, US-49 |
| RN-088 | El portal externo, la API fiscal y la ubicación remota pueden quedar temporalmente indisponibles sin afectar la operación local | Offline | `05` Offline-first | US-30, US-27, US-13, US-48 |
| RN-089 | Existe respaldo central en tiempo real más copias recuperables | Respaldo | `05` Respaldo y seguridad; `README` Alcance | US-50 |
| RN-090 | Contraseñas y PIN nunca se guardan ni se muestran en texto legible, y el PIN presencial es distinto de la contraseña del portal | Seguridad | `05` Respaldo y seguridad; `02` Ficha | US-53, US-54, US-55, US-15, US-51 |
| RN-091 | Se aplica bloqueo temporal tras intentos fallidos de PIN | Seguridad | `05` Respaldo y seguridad | US-54, US-23, US-51 |
| RN-092 | Los adjuntos de pagos y compras se protegen según permisos y se registran las sesiones y acciones sensibles | Seguridad | `05` Respaldo y seguridad | US-53, US-57, US-31, US-26, US-42, US-51 |
| RN-093 | La documentación no prescribe lenguaje, framework, base de datos, nube ni arquitectura; la solución debe cumplir PWA, multilocal, operación local completa, sincronización y respaldo | Restricción técnica | `05` Restricción tecnológica; `README` Enfoque técnico | US-48, US-50, y todo el backlog |
| RN-094 | El código de pre-ticket es único y de un solo uso | Pensionistas / Integridad | `06` Reglas de integridad | US-23, US-24 |

---



---


# 6. Trazabilidad con el backlog técnico

El repositorio, la especificación funcional, el modelo de datos y el código fuente siguen
usando la numeración anterior, de tres dígitos (US-001 a US-062) y HT-01 a HT-10. Esta tabla
traduce una en otra. **Atención al número de dígitos:** `US-01` es de este documento y `US-001`
del repositorio; no son la misma historia.

| ID nuevo | ID técnico | Épica nueva | Épica anterior | Historia |
|-------------|--------------|----------------|-----------------|------------------------------------------------------------|
| **US-01** | HT-07 | E1 | EP-05 | Operar el POS con una sola mano |
| **US-02** | US-013 | E1 | EP-03 | Programar el menú semanal del almuerzo |
| **US-03** | US-014 | E1 | EP-03 | Configurar el menú normal y el menú extra |
| **US-04** | US-015 | E1 | EP-03 | Marcar productos agotados y reactivarlos |
| **US-05** | US-027 | E1 | EP-05 | Visualizar y operar el mapa de mesas |
| **US-06** | US-028 | E1 | EP-05 | Tomar el pedido asignando productos por persona |
| **US-07** | US-029 | E1 | EP-05 | Unir, mover, dividir y trasladar cuentas y productos |
| **US-08** | US-030 | E1 | EP-05 | Emitir la comanda a la impresora central de cocina |
| **US-09** | US-031 | E1 | EP-05 | Anular productos antes y después de comandar |
| **US-10** | US-038 | E1 | EP-07 | Registrar pedidos de recojo programado |
| **US-11** | US-039 | E1 | EP-07 | Gestionar el recojo preparado y no retirado |
| **US-12** | US-040 | E1 | EP-07 | Registrar el pedido de delivery y asignar motorizado |
| **US-13** | US-041 | E1 | EP-07 | Operar la vista móvil del motorizado |
| **US-14** | US-042 | E1 | EP-07 | Liquidar al motorizado y cerrar la venta de delivery |
| **US-15** | US-016 | E2 | EP-04 | Registrar la ficha del pensionista y su responsable |
| **US-16** | US-017 | E2 | EP-04 | Administrar planes tarifarios y precios especiales |
| **US-17** | US-018 | E2 | EP-04 | Mantener la cuenta corriente del pensionista |
| **US-18** | US-032 | E2 | EP-06 | Abrir el turno de caja con fondo inicial |
| **US-19** | US-033 | E2 | EP-06 | Cobrar la cuenta con medios divididos y cierres parciales |
| **US-20** | US-034 | E2 | EP-06 | Aplicar descuentos y anulaciones con autorización y motivo |
| **US-21** | US-035 | E2 | EP-06 | Reabrir una venta dentro del turno o compensarla después |
| **US-22** | US-036 | E2 | EP-06 | Cerrar el turno de caja con conciliación y alertas |
| **US-23** | US-019 | E2 | EP-04 | Emitir el pre-ticket de autoservicio del pensionista |
| **US-24** | US-020 | E2 | EP-04 | Vincular el pre-ticket al pedido del pensionista |
| **US-25** | US-021 | E2 | EP-04 | Registrar abonos del pensionista en caja |
| **US-26** | US-024 | E2 | EP-04 | Aprobar o rechazar abonos digitales |
| **US-27** | US-037 | E2 | EP-06 | Emitir comprobantes electrónicos por proveedor API |
| **US-28** | US-025 | E2 | EP-04 | Generar el cierre mensual y el estado de cuenta |
| **US-29** | US-062 | E2 | EP-06 | Entregar el comprobante al cliente por correo |
| **US-30** | US-022 | E2 | EP-04 | Consultar el portal compartido del pensionista |
| **US-31** | US-023 | E2 | EP-04 | Reportar un abono digital desde el portal |
| **US-32** | US-026 | E2 | EP-04 | Desactivar y reactivar a un pensionista |
| **US-33** | US-008 | E3 | EP-02 | Administrar el catálogo central de productos |
| **US-34** | US-009 | E3 | EP-02 | Definir unidades base, presentaciones y conversiones |
| **US-35** | US-011 | E3 | EP-02 | Administrar el catálogo de proveedores |
| **US-36** | US-012 | E3 | EP-02 | Configurar listas de motivos del sistema |
| **US-37** | US-010 | E3 | EP-02 | Crear recetas y subrecetas valorizadas por porción |
| **US-38** | US-043 | E3 | EP-08 | Mantener existencias por local con stock mínimo y alertas |
| **US-39** | US-044 | E3 | EP-08 | Consultar el Kardex con origen rastreable |
| **US-40** | US-045 | E3 | EP-08 | Registrar ajustes de inventario con motivo |
| **US-41** | US-048 | E3 | EP-08 | Producir subrecetas y descontar receta al vender |
| **US-42** | US-050 | E3 | EP-08 | Registrar compras y actualizar el costo de última compra |
| **US-43** | US-049 | E3 | EP-08 | Registrar el destino de los sobrantes de producción |
| **US-44** | US-051 | E3 | EP-08 | Registrar el consumo interno del personal |
| **US-45** | US-046 | E3 | EP-08 | Transferir productos entre locales |
| **US-46** | US-047 | E3 | EP-08 | Ejecutar conteos físicos sin detener la operación |
| **US-47** | HT-09 | E4 | EP-10 | Reinicio autónomo del equipo del local |
| **US-48** | US-058 | E4 | EP-10 | Operar el local completo sin conexión a Internet |
| **US-49** | US-059 | E4 | EP-10 | Sincronizar sin duplicados y resolver conflictos |
| **US-50** | US-060 | E4 | EP-10 | Respaldar la información en tiempo real |
| **US-51** | US-061 | E4 | EP-10 | Proteger credenciales, sesiones y adjuntos |
| **US-52** | US-001 | E5 | EP-01 | Administrar empresa y locales |
| **US-53** | US-002 | E5 | EP-01 | Autenticar usuarios con usuario y contraseña |
| **US-54** | US-003 | E5 | EP-01 | Acceder como mozo mediante PIN individual |
| **US-55** | US-005 | E5 | EP-01 | Gestionar usuarios y su asignación multilocal |
| **US-56** | US-004 | E5 | EP-01 | Definir roles y permisos granulares |
| **US-57** | US-006 | E5 | EP-01 | Registrar auditoría transversal de acciones sensibles |
| **US-58** | US-007 | E5 | EP-01 | Configurar horarios de servicio por local |
| **US-59** | US-052 | E5 | EP-09 | Consultar el panel del administrador con filtros |
| **US-60** | US-053 | E5 | EP-09 | Reportar ventas, costo histórico, utilidad y margen |
| **US-61** | US-054 | E5 | EP-09 | Reportar cierres y diferencias de caja |
| **US-62** | US-055 | E5 | EP-09 | Reportar pensionistas, saldos, estados y abonos |
| **US-63** | US-056 | E5 | EP-09 | Reportar inventario, Kardex y producción |
| **US-64** | US-057 | E5 | EP-09 | Reportar delivery, anulaciones y consumo interno |
| — | HT-01 | tarea técnica | EP-11 | Esqueleto que camina |
| — | HT-02 | tarea técnica | EP-11 | Migraciones con un único dueño |
| — | HT-03 | tarea técnica | EP-11 | Tipos base del dominio |
| — | HT-05 | tarea técnica | EP-11 | Generación de tipos desde OpenAPI |
| — | HT-06 | tarea técnica | EP-11 | Verificación automática de fronteras |
| — | HT-08 | tarea técnica | EP-11 | Identificador de correlación en los registros |
| — | HT-04 | tarea técnica | EP-11 | Patrón outbox |
| — | HT-10 | tarea técnica | EP-11 | Prueba de impresión con la comandera real |

