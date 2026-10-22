package pe.rikos.app.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Colores del sistema de diseño (`01-sistema-de-diseno.md`, sección 1). Los nombres de token se mantienen en inglés. */
@Immutable
data class RikosColors(
    val bg: Color, val surface: Color, val surface2: Color, val line: Color,
    val ink: Color, val ink2: Color, val ink3: Color,
    val primary: Color, val onPrimary: Color, val primarySoft: Color, val navy: Color,
    val amber: Color, val amberSoft: Color, val amberText: Color, val amberStrong: Color,
    val blue: Color, val blueSoft: Color,
    val green: Color, val greenSoft: Color, val greenText: Color,
    val purple: Color, val purpleSoft: Color,
    val red: Color, val redSoft: Color,
    val isDark: Boolean,
)

val RikosLight = RikosColors(
    bg = Color(0xFFF8FAFC), surface = Color(0xFFFFFFFF), surface2 = Color(0xFFEEF2F6), line = Color(0xFFE2E8F0),
    ink = Color(0xFF0F172A), ink2 = Color(0xFF334155), ink3 = Color(0xFF5A6A7F),
    primary = Color(0xFF0B7A6C), onPrimary = Color(0xFFFFFFFF), primarySoft = Color(0xFFDDF5F1), navy = Color(0xFF021E43),
    amber = Color(0xFFD97706), amberSoft = Color(0xFFFFF3D6), amberText = Color(0xFF7A4A00), amberStrong = Color(0xFFB45309),
    blue = Color(0xFF1E3A9E), blueSoft = Color(0xFFE2E8FB),
    green = Color(0xFF059669), greenSoft = Color(0xFFE0F5EC), greenText = Color(0xFF047857),
    purple = Color(0xFF6D28D9), purpleSoft = Color(0xFFF3EAFB),
    red = Color(0xFFB91C1C), redSoft = Color(0xFFFDECEC),
    isDark = false,
)

val RikosDark = RikosColors(
    bg = Color(0xFF0B1220), surface = Color(0xFF131C2E), surface2 = Color(0xFF1A2438), line = Color(0xFF1E293B),
    ink = Color(0xFFE5ECF6), ink2 = Color(0xFFCBD5E1), ink3 = Color(0xFF94A3B8),
    primary = Color(0xFF2DD4BF), onPrimary = Color(0xFF04241F), primarySoft = Color(0xFF134E4A), navy = Color(0xFF1F2B42),
    amber = Color(0xFFFBBF24), amberSoft = Color(0xFF3A2A0A), amberText = Color(0xFFFCD34D), amberStrong = Color(0xFFB45309),
    blue = Color(0xFFA5B4FC), blueSoft = Color(0xFF1E2A5A),
    green = Color(0xFF34D399), greenSoft = Color(0xFF0F3B2E), greenText = Color(0xFF6EE7B7),
    purple = Color(0xFFC4B5FD), purpleSoft = Color(0xFF2E1A5C),
    red = Color(0xFFFCA5A5), redSoft = Color(0xFF3B1414),
    isDark = true,
)
