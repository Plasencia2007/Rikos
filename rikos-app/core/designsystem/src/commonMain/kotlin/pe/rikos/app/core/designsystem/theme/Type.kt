package pe.rikos.app.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import rikos_app.core.designsystem.generated.resources.Res
import rikos_app.core.designsystem.generated.resources.jakarta_400
import rikos_app.core.designsystem.generated.resources.jakarta_500
import rikos_app.core.designsystem.generated.resources.jakarta_600
import rikos_app.core.designsystem.generated.resources.jakarta_700
import rikos_app.core.designsystem.generated.resources.jakarta_800

@Composable
internal fun rememberRikosType(): RikosType {
    val jakarta = FontFamily(
        Font(Res.font.jakarta_400, FontWeight.Normal),
        Font(Res.font.jakarta_500, FontWeight.Medium),
        Font(Res.font.jakarta_600, FontWeight.SemiBold),
        Font(Res.font.jakarta_700, FontWeight.Bold),
        Font(Res.font.jakarta_800, FontWeight.ExtraBold),
    )
    return remember(jakarta) { RikosType(jakarta) }
}

/** Plus Jakarta Sans empaquetada con licencia OFL; no requiere conexión. */
class RikosType internal constructor(private val jakarta: FontFamily) {
    private fun s(size: Float, peso: FontWeight, interletra: Float = 0f, alto: Float = size * 1.25f) = TextStyle(
        fontFamily = jakarta,
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
