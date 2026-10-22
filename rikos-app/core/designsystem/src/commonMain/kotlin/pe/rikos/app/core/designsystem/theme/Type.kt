package pe.rikos.app.core.designsystem.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Escala tipográfica (`01-sistema-de-diseno.md`, sección 2).
 * TODO: empaquetar Plus Jakarta Sans (OFL) en composeResources/font y reemplazar [Jakarta].
 * Mientras no estén los .ttf se usa la fuente del sistema.
 */
val Jakarta: FontFamily = FontFamily.Default

object RikosType {
    private fun s(size: Float, peso: FontWeight, interletra: Float = 0f, alto: Float = size * 1.25f) = TextStyle(
        fontFamily = Jakarta,
        fontSize = size.sp,
        fontWeight = peso,
        letterSpacing = interletra.em,
        lineHeight = alto.sp,
    )

    val display = s(54f, FontWeight.ExtraBold, -0.02f, 56f)
    val title1 = s(32f, FontWeight.ExtraBold, -0.02f, 35f)
    val title2 = s(26f, FontWeight.ExtraBold, -0.02f, 30f)
    val title3 = s(22f, FontWeight.ExtraBold, -0.02f, 26f)
    val headline = s(17f, FontWeight.ExtraBold, 0f, 22f)
    val body = s(16f, FontWeight.Medium, 0f, 22f)
    val callout = s(15f, FontWeight.Bold, 0f, 20f)
    val subhead = s(14f, FontWeight.SemiBold, 0f, 19f)
    val footnote = s(13f, FontWeight.SemiBold, 0f, 18f)
    val caption = s(12.5f, FontWeight.ExtraBold, 0f, 16f)
}
