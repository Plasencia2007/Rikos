# rikos-app

App móvil nativa de Riko's: **Kotlin Multiplatform** con lógica y pantallas compartidas (Compose Multiplatform) para Android e iOS. Primero la app del mozo, con un administrador resumido.

Diseño: lienzo «Rikos-App» (80 láminas) y los documentos de `docs/` (todos son propios de la app y no se mezclan con la documentación general del repo):

- `docs/00-git-y-alcance.md`: **regla de git: commits y push solo afectan `rikos-app/`**.
- `docs/01-sistema-de-diseno.md`: tokens, tipografía, movimiento, componentes.
- `docs/02-brechas-de-api.md`: qué API existe y qué falta.
- `docs/03-pantallas-vs-backlog.md`: cruce de pantallas con el backlog v3 (sin modificarlo).

## Alcance en git

Todo cambio, commit y push de la app toca **únicamente** la carpeta `rikos-app/`; el resto del repo (`rikos-backend`, `rikos-web`, `docs/` general) no se modifica. Detalle y comandos en `docs/00-git-y-alcance.md`.

## Qué hay hoy

| Módulo | Contenido |
|---|---|
| `:core:common` | `Resultado<T,E>`, `Dinero` (céntimos), `Ulid` (26 caracteres) |
| `:core:designsystem` | Tokens claro/oscuro, tipografía, espacios, radios, movimiento; `RkButton`, `RkTag`, `MesaCard` |
| `:feature:bienvenida` | M00: tres pasos, ilustración vectorial, entrada escalonada y halo de comida lista; “Empezar” y “Saltar” avanzan a configurar local |
| `:feature:configuracion` | Pantalla **Configurar local** (M01): vinculación al servidor, estado de respuesta y selección de local activo |
| `:feature:auth` | Pantalla **PIN** (M02): 4 dígitos, entra solo al 4.º, sin botón de confirmar (DT-028) |
| `:feature:mesas` | Pantalla **Mesas** (M03): cuadrícula con estados |
| `:composeApp` | `App()` compartida, raíz de composición (`AppContainer`), framework `ComposeApp` para iOS |
| `:androidApp` | Aplicación Android que aloja `App()` |
| `iosApp/` | Pendiente: se crea desde un Mac (ver `iosApp/LEEME.md`) |

Los datos son **de mentira** (`Fake*Repository`). PIN `1234` entra, `0000` simula bloqueo (423), otro da 401.

## Arquitectura (CLEAN + MVVM)

Cada feature se divide en tres capas: `domain` (modelos, interfaces de repositorio, casos de uso; sin dependencias de UI ni de red) → `data` (implementaciones) → `presentation` (ViewModel con `StateFlow` + pantalla Compose). La dependencia siempre apunta hacia `domain`.

## Reglas heredadas del repo

- IDs: ULID de 26 caracteres, generados por el cliente (`Ulid.nuevo()`).
- Dinero: céntimos enteros (`Dinero`). Nunca `Double`.
- Errores esperables: `Resultado`, no excepciones.
- **Ningún color ni medida suelta en componentes**: todo sale de `Rikos.colors`, `RikosType`, `RikosSpace`, etc.
- Los nombres de token de color se mantienen en inglés (decisión del equipo).
- Textos de pantalla en español.

## Cómo correrlo

Requisitos: Android Studio reciente (con el plugin de Kotlin Multiplatform), JDK 21.

1. Abrir la carpeta `rikos-app` en Android Studio y esperar el *Gradle sync*.
2. Ejecutar la configuración `androidApp` en un emulador o dispositivo.
3. Pruebas: `./gradlew allTests`

> **Importante: este esqueleto se escribió sin poder compilarlo** (el entorno donde se generó no alcanza Maven ni Google). Puede que el primer sync pida ajustes. Los primeros sospechosos:
> - Versiones en `gradle/libs.versions.toml` (Kotlin 2.4.21, Compose Multiplatform 1.12.2, AGP 9.3.1). Se ajustan solo ahí.
> - El bloque `androidLibrary { }` del plugin `com.android.kotlin.multiplatform.library` (los nombres de propiedades han cambiado entre versiones).
> - Falta el *Gradle wrapper* si no vino incluido: `gradle wrapper` o dejar que Android Studio lo cree.

## Pendiente a propósito

- Fuente Plus Jakarta Sans (OFL): agregar los `.ttf` en `composeResources/font` y reemplazar `Jakarta` en `Type.kt`.
- Cliente HTTP (Ktor) y los repositorios reales; contrato WebSocket (ver `docs/02-brechas-de-api.md`).
- Navegación real (hoy es un `if` por estado en `App.kt`).
- Persistencia de sesión y del bloqueo de PIN por dispositivo (DT-039).
- Modo noche y letra grande: el tema oscuro ya existe; falta probar escalas de fuente.
