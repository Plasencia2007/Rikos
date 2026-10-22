# iosApp (pendiente)

El proyecto de Xcode se crea desde un Mac, no se puede generar desde aquí.

Pasos cuando haya Mac:

1. En Xcode: *File ▸ New ▸ Project ▸ iOS App* (SwiftUI), nombre `iosApp`, dentro de esta carpeta.
2. Agregar una fase de compilación *Run Script* **antes** de «Compile Sources»:
   `cd "$SRCROOT/.." && ./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`
3. En *Build Settings* ▸ *Framework Search Paths* agregar
   `$(SRCROOT)/../composeApp/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`
4. Enlazar el framework `ComposeApp` y, en `ContentView.swift`, envolver `ComposeUIViewController { App() }`
   (se expone con un `MainViewController.kt` en `iosMain` de `:composeApp`; agregarlo en ese momento).
5. Los tokens de diseño en Swift están en `rikos-app/docs/01-sistema-de-diseno.md` (sección 1, «Swift»).

Ojo: confirmar los nombres de la tarea y las rutas contra la plantilla oficial de Kotlin Multiplatform vigente
en el momento, porque cambian entre versiones.
