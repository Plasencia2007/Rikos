# 01 · Sistema de diseño de Rikos-App (Compose y SwiftUI)

Fuente visual: lienzo de diseño de Rikos-App (tableros **Componentes** y **Especificaciones**). Este documento es la versión para código. Familia visual tomada de Fáctivo Restaurante: Plus Jakarta Sans, fondo pizarra clara, tarjetas blancas, verde azulado para actuar y navy para lo que flota encima.

## 1. Colores

Los nombres de token son los mismos en Kotlin y Swift. Cada uno tiene valor claro y oscuro.

| Token | Uso | Claro | Oscuro |
|---|---|---|---|
| bg | Fondo de pantalla | #F8FAFC | #0B1220 |
| surface | Tarjeta | #FFFFFF | #131C2E |
| surface2 | Gris de apoyo, cantidad | #EEF2F6 | #1A2438 |
| line | Borde, separador | #E2E8F0 | #1E293B |
| ink | Texto principal | #0F172A | #E5ECF6 |
| ink2 | Texto secundario | #334155 | #CBD5E1 |
| ink3 | Texto terciario, meta | #5A6A7F | #94A3B8 |
| primary | Acción principal | #0B7A6C | #2DD4BF |
| onPrimary | Texto sobre acción | #FFFFFF | #04241F |
| primarySoft | Fondo de acción suave | #DDF5F1 | #134E4A |
| navy | Barra flotante, chip activo, resumen de caja | #021E43 | #1F2B42 |
| amber / amberSoft / amberText | Mesa ocupada, avisos | #D97706 / #FFF3D6 / #7A4A00 | #FBBF24 / #3A2A0A / #FCD34D |
| blue / blueSoft | Por cobrar | #1E3A9E / #E2E8FB | #A5B4FC / #1E2A5A |
| green / greenSoft / greenText | Libre, listo | #059669 / #E0F5EC / #047857 | #34D399 / #0F3B2E / #6EE7B7 |
| purple / purpleSoft | Pensionista | #6D28D9 / #F3EAFB | #C4B5FD / #2E1A5C |
| red / redSoft | Error, anular | #B91C1C / #FDECEC | #FCA5A5 / #3B1414 |
| amberStrong | Fondo ámbar con texto blanco | #B45309 | #B45309 |

Regla: el ámbar #D97706 solo va en íconos y bordes. Para fondo ámbar con texto blanco se usa `amberStrong`.

### Kotlin (Compose)

```kotlin
@Immutable
data class RikosColors(
    val bg: Color, val surface: Color, val surface2: Color, val line: Color,
    val ink: Color, val ink2: Color, val ink3: Color,
    val primary: Color, val onPrimary: Color, val primarySoft: Color, val navy: Color,
    val amber: Color, val amberSoft: Color, val amberText: Color,
    val blue: Color, val blueSoft: Color,
    val green: Color, val greenSoft: Color, val greenText: Color,
    val purple: Color, val purpleSoft: Color,
    val red: Color, val redSoft: Color,
)

val RikosLight = RikosColors(
    bg = Color(0xFFF8FAFC), surface = Color(0xFFFFFFFF), surface2 = Color(0xFFEEF2F6), line = Color(0xFFE2E8F0),
    ink = Color(0xFF0F172A), ink2 = Color(0xFF334155), ink3 = Color(0xFF5A6A7F),
    primary = Color(0xFF0B7A6C), onPrimary = Color.White, primarySoft = Color(0xFFDDF5F1), navy = Color(0xFF021E43),
    amber = Color(0xFFD97706), amberSoft = Color(0xFFFFF3D6), amberText = Color(0xFF7A4A00),
    blue = Color(0xFF1E3A9E), blueSoft = Color(0xFFE2E8FB),
    green = Color(0xFF059669), greenSoft = Color(0xFFE0F5EC), greenText = Color(0xFF047857),
    purple = Color(0xFF6D28D9), purpleSoft = Color(0xFFF3EAFB),
    red = Color(0xFFB91C1C), redSoft = Color(0xFFFDECEC),
)

val RikosDark = RikosColors(
    bg = Color(0xFF0B1220), surface = Color(0xFF131C2E), surface2 = Color(0xFF1A2438), line = Color(0xFF1E293B),
    ink = Color(0xFFE5ECF6), ink2 = Color(0xFFCBD5E1), ink3 = Color(0xFF94A3B8),
    primary = Color(0xFF2DD4BF), onPrimary = Color(0xFF04241F), primarySoft = Color(0xFF134E4A), navy = Color(0xFF1F2B42),
    amber = Color(0xFFFBBF24), amberSoft = Color(0xFF3A2A0A), amberText = Color(0xFFFCD34D),
    blue = Color(0xFFA5B4FC), blueSoft = Color(0xFF1E2A5A),
    green = Color(0xFF34D399), greenSoft = Color(0xFF0F3B2E), greenText = Color(0xFF6EE7B7),
    purple = Color(0xFFC4B5FD), purpleSoft = Color(0xFF2E1A5C),
    red = Color(0xFFFCA5A5), redSoft = Color(0xFF3B1414),
)

val LocalRikosColors = staticCompositionLocalOf { RikosLight }
```

### Swift (SwiftUI)

Recomendado: definir cada color en el Asset Catalog con variante clara y oscura (mismo nombre que el token) y exponerlos así:

```swift
extension Color {
    init(hex: UInt32) {
        self.init(red: Double((hex >> 16) & 0xFF) / 255,
                  green: Double((hex >> 8) & 0xFF) / 255,
                  blue: Double(hex & 0xFF) / 255)
    }
}

enum RK {
    static let bg = Color("bg"), surface = Color("surface"), surface2 = Color("surface2"), line = Color("line")
    static let ink = Color("ink"), ink2 = Color("ink2"), ink3 = Color("ink3")
    static let primary = Color("primary"), onPrimary = Color("onPrimary"), primarySoft = Color("primarySoft")
    static let navy = Color("navy")
    static let amber = Color("amber"), amberSoft = Color("amberSoft"), amberText = Color("amberText")
    static let blue = Color("blue"), blueSoft = Color("blueSoft")
    static let green = Color("green"), greenSoft = Color("greenSoft"), greenText = Color("greenText")
    static let purple = Color("purple"), purpleSoft = Color("purpleSoft")
    static let red = Color("red"), redSoft = Color("redSoft")
}
```

## 2. Letra

Plus Jakarta Sans (pesos 500, 600, 700, 800). En iOS cada estilo se ata a un estilo de Dynamic Type con `relativeTo`, así crece con la configuración del sistema. En Android se usa `sp`, que respeta la escala de fuente.

| Estilo | Uso | Tamaño / peso | Interletra | SwiftUI (relativeTo) |
|---|---|---|---|---|
| display | Monto grande | 54 / 800 | −2 % | .largeTitle |
| title1 | Título de pantalla | 32 / 800 | −2 % | .largeTitle |
| title2 | Hoja inferior, alerta | 26 / 800 | −2 % | .title |
| title3 | Número de mesa | 22 / 800 | −2 % | .title2 |
| headline | Botón | 17 / 800 | 0 | .headline |
| body | Texto | 16 / 500 | 0 | .body |
| callout | Chip, fila de lista | 15 / 700 | 0 | .callout |
| subhead | Secundario | 14 / 600 | 0 | .subheadline |
| footnote | Meta (monto, tiempo) | 13 / 600 | 0 | .footnote |
| caption | Etiqueta, pestaña | 12.5 / 800 | 0 | .caption |

Montos: el «S/» va al 42 % del tamaño y los céntimos al 55 %, ambos al 72 % de opacidad, con cifras tabulares (`fontFeatureSettings = "tnum"` / `.monospacedDigit()`).

```swift
extension Font {
    static func rk(_ size: CGFloat, _ weight: Font.Weight, relativeTo style: Font.TextStyle) -> Font {
        .custom("PlusJakartaSans", size: size, relativeTo: style).weight(weight)
    }
    static let rkTitle1 = rk(32, .heavy, relativeTo: .largeTitle)
    static let rkHeadline = rk(17, .heavy, relativeTo: .headline)
    static let rkBody = rk(16, .medium, relativeTo: .body)
}
```

```kotlin
val Jakarta = FontFamily(Font(Res.font.jakarta_500, FontWeight.Medium), Font(Res.font.jakarta_600, FontWeight.SemiBold),
                         Font(Res.font.jakarta_700, FontWeight.Bold), Font(Res.font.jakarta_800, FontWeight.ExtraBold))
object RikosType {
    val title1 = TextStyle(fontFamily = Jakarta, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.02).em, lineHeight = 35.sp)
    val headline = TextStyle(fontFamily = Jakarta, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
    val body = TextStyle(fontFamily = Jakarta, fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp)
    // resto según la tabla
}
```

### Letra grande

El tablero **Pedido · letra grande** muestra el pedido con Dynamic Type en tamaño accesible (≈ +20 %). Reglas: los textos pasan a varias líneas en vez de cortarse; el precio baja debajo del nombre cuando no cabe; la barra flotante crece en alto; los botones nunca bajan de 56 de alto.

## 3. Espacios, radios, sombras y áreas táctiles

- Espacios: 4, 8, 12, 16, 20, 24, 32. Margen de pantalla 16; separación entre tarjetas 12; relleno de tarjeta 14 a 16.
- Radios: etiqueta 12; botón y campo 16; grupo de lista 18; tarjeta y mesa 20; chip e ícono redondo 22; hoja inferior, alerta y resumen 30; barra flotante 35 (píldora).
- Sombras: tarjeta 0 1 2 al 5 %; barra flotante 0 14 32 al 32 %; hoja 0 −10 40 al 16 %; alerta 0 24 60 al 25 %. En modo noche, sin sombra en tarjetas; se separan por color de superficie.
- Áreas táctiles: botón 56 de alto; chip, ícono y tecla 44; pestaña 64.

## 4. Movimiento

| Token | Qué hace | Duración | Curva | Compose | SwiftUI |
|---|---|---|---|---|---|
| press | Escala .96 al tocar | 120 ms | ease-out | `tween(120)` | `.easeOut(duration: 0.12)` |
| color | Cambio de estado | 200 ms | ease | `tween(200)` | `.easeInOut(duration: 0.2)` |
| push | Ir a otra pantalla | 320 ms | (.2,.8,.2,1) | `tween(320, CubicBezierEasing(.2f,.8f,.2f,1f))` | `.timingCurve(0.2,0.8,0.2,1,duration:0.32)` |
| sheet | Hoja inferior | 380 ms | (.2,.8,.2,1) | `tween(380, RikosEase)` | `.timingCurve(0.2,0.8,0.2,1,duration:0.38)` |
| alert | Alerta (resorte) | 300 ms | resorte | `spring(dampingRatio = 0.7f, stiffness = 600f)` | `.spring(response: 0.3, dampingFraction: 0.7)` |
| toast | Aviso: baja, espera 3 s, sube | 300 + 3000 ms | (.2,.8,.2,1) | `tween(300)` + `delay(3000)` | `.timingCurve(...)` + `Task.sleep` |
| ring | Pulso verde de «comida lista» | 1800 ms, en bucle | ease-out | `infiniteRepeatable(tween(1800))` | `.easeOut(duration: 1.8).repeatForever(autoreverses: false)` |
| tab | Indicador de pestaña que se desliza | 460 ms | resorte (.34,1.56,.64,1) | `spring(dampingRatio = 0.6f, stiffness = 500f)` | `.spring(response: 0.46, dampingFraction: 0.65)` |
| bump | Contador que rebota al sumar | 460 ms | resorte | escala 1 → 1.3 → 1 con `keyframes` | `.spring(response: 0.35, dampingFraction: 0.5)` |
| count | Monto que cuenta hasta el nuevo valor | 380 ms | cúbica de salida | `animateIntAsState(tween(380))` sobre céntimos | `.contentTransition(.numericText())` |
| shake | PIN incorrecto | 460 ms | (.36,.07,.19,.97) | `keyframes` de `offset` ±10 dp | `.keyframeAnimator` |
| zoom | La tarjeta de mesa crece hasta el pedido | 380 ms | (.3,.8,.2,1) | `SharedTransitionLayout` + `sharedBounds` | `.matchedGeometryEffect` / `.navigationTransition(.zoom)` |
| print | El ticket sale de la ranura | 1100 ms | (.3,.7,.2,1) | `animateFloatAsState` + recorte | `.mask` + `.offset` |
| swipe | Deslizar un plato por enviar para quitarlo | 320 ms | resorte | `SwipeToDismissBox` (se abre a −104 dp) | `.swipeActions(edge: .trailing)` |
| peek | Mantener presionada una mesa: vistazo | 420 ms para activar, 420 ms de entrada | resorte | `combinedClickable(onLongClick)` | `.contextMenu { } preview: { }` |

Con «reducir movimiento» (iOS `accessibilityReduceMotion`, Android `ANIMATOR_DURATION_SCALE = 0`) se cambia sin animar y el pulso se reemplaza por un borde fijo.

## 5. Contraste (WCAG 2.2)

Se revisaron 27 pares reales; todos aprueban. AA pide 4.5 para texto, y 3 para texto grande, íconos y bordes.

| Modo | Par | Ratio | Nivel |
|---|---|---|---|
| Claro | Texto principal sobre fondo | 17.06 | AAA |
| Claro | Texto terciario sobre gris s2 (el más justo de texto) | 4.91 | AA |
| Claro | Botón primario (blanco sobre #0B7A6C) | 5.23 | AA |
| Claro | Teal sobre teal suave | 4.58 | AA |
| Claro | Verde texto sobre verde suave | 4.82 | AA |
| Claro | Ocupada, por cobrar, error, pensionista | 5.66 a 7.97 | AA / AAA |
| Claro | Avatar ámbar corregido (blanco sobre #B45309) | 5.02 | AA |
| Claro | Ícono ámbar y borde de mesa libre (no texto) | 3.19 / 3.31 | AA |
| Oscuro | Todos los pares de texto | 6.41 a 15.75 | AA / AAA |

Corrección aplicada: el avatar ámbar con texto blanco daba 3.19 y se cambió a `amberStrong` (#B45309).

## 6. Componentes (nombres para código)

| Componente | Compose | SwiftUI | Estados |
|---|---|---|---|
| Botón | `RkButton(kind = Primary/Secondary/Danger/DangerSolid)` | `RkButton(.primary)` | normal, presionado, cargando, deshabilitado |
| Botón ícono | `RkIconButton` | `RkIconButton` | normal, con punto de aviso |
| Chip | `RkChip(selected, count)` | `RkChip` | normal, activo, con conteo |
| Segmento | `RkSegmented` | `Picker(.segmented)` con estilo propio | 2 a 3 opciones |
| Etiqueta | `RkTag(tone)` | `RkTag` | porEnviar, enCocina, listo, cuenta, pensionista, anulado, nuevo |
| Estado de conexión | `RkConn` | `RkConn` | en línea, enviando N, sin conexión |
| Campo | `RkField` | `RkField` | vacío, enfocado, error |
| Interruptor, radio, cantidad | `RkSwitch`, `RkRadio`, `RkStepper` | `Toggle`, `RkRadio`, `RkStepper` | — |
| Mesa | `MesaCard(estado)` | `MesaCard` | libre, ocupada, por cobrar, listo (pulso), elegida, fuera de servicio |
| Línea de pedido | `OrderLine` | `OrderLine` | en cocina, por enviar, anulada |
| Producto | `ProductCard` | `ProductCard` | normal, con cantidad, agotado |
| Medio de pago | `PayTile` | `PayTile` | normal, elegido |
| Mensaje | `RkNote(tone)` | `RkNote` | info, aviso, error, vacío |
| Aviso flotante | `RkToast` | `.rkToast(isPresented:)` | — |
| Barra flotante | `TotalBar` | `TotalBar` | sin pendientes, con pendientes |
| Hoja inferior | `ModalBottomSheet` con estilo | `.sheet` + `presentationDetents` | — |
| Alerta | `RkAlert` | `.alert` / vista propia | — |
| Barra de pestañas flotante | `RkTabBar` (4 destinos + botón central «Nuevo pedido») | `TabView` con barra propia | indicador deslizante, ícono activo relleno, contador que rebota, punto de turno abierto |

### Barra de pestañas flotante (`RkTabBar`)

Rediseñada en el lienzo (versión 26). Reglas:

- Flota a 12 dp de los lados y 24 dp del borde inferior, alto 72, radio 28. Fondo `surface` al 84 % con desenfoque (vidrio); en Android sin desenfoque se usa `surface` sólido con sombra.
- Cinco columnas: Mesas, Comandas, **botón central «Nuevo pedido»** (56 × 56, radio 20, color `navy`; en modo noche `primary`), Caja, Más. El administrador usa cinco pestañas sin botón central.
- La pestaña activa tiene una píldora `primarySoft` detrás que se **desliza** con el token `tab`; el ícono activo se rellena al 18 % y el texto pasa a `primary` en negrita.
- Contador de Comandas en `red` con anillo del color de la barra; rebota con el token `bump` cuando cambia. Caja lleva un punto `green` con pulso cuando el turno está abierto.
- El contenido de la pantalla deja 116 dp abajo para no quedar tapado, y un degradado del color de fondo detrás de la barra.
- El botón central abre la hoja «Nuevo pedido» (en una mesa, para llevar, delivery, pensionista); la hoja se cierra arrastrándola hacia abajo más de 110 dp.
- Con «reducir movimiento»: sin deslizamiento ni rebote, cambio directo.

## 7. Pendiente

- Fotos reales de la carta: el diseño usa ilustraciones de plato como espacio reservado. En la app, `ProductCard` debe aceptar una URL de imagen con la ilustración como respaldo.
- Confirmar la licencia de Plus Jakarta Sans para empaquetarla (es OFL, libre de uso).
