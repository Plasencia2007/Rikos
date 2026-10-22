package pe.rikos.app.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Espacios, radios y áreas táctiles (`01-sistema-de-diseno.md`, sección 3). */
object RikosSpace {
    val s4 = 4.dp; val s8 = 8.dp; val s12 = 12.dp; val s16 = 16.dp
    val s20 = 20.dp; val s24 = 24.dp; val s32 = 32.dp
    val screenMargin = s16
    val cardGap = s12
}

object RikosRadius {
    val tag = RoundedCornerShape(12.dp)
    val button = RoundedCornerShape(16.dp)
    val field = RoundedCornerShape(16.dp)
    val listGroup = RoundedCornerShape(18.dp)
    val card = RoundedCornerShape(20.dp)
    val table = RoundedCornerShape(20.dp)
    val chip = RoundedCornerShape(22.dp)
    val sheet = RoundedCornerShape(30.dp)
    val pill = RoundedCornerShape(35.dp)
}

object RikosTouch {
    val button = 56.dp
    val control = 44.dp
    val tab = 64.dp
}

/** Movimiento (`01-sistema-de-diseno.md`, sección 4). */
object RikosMotion {
    val Ease = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
    fun <T> press() = tween<T>(120)
    fun <T> color() = tween<T>(200)
    fun <T> push() = tween<T>(320, easing = Ease)
    fun <T> sheet() = tween<T>(380, easing = Ease)
}
