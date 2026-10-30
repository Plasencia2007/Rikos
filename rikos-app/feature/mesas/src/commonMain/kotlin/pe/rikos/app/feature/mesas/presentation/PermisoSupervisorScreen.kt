package pe.rikos.app.feature.mesas.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pe.rikos.app.core.designsystem.theme.Rikos
import pe.rikos.app.core.designsystem.theme.RikosMotion
import kotlin.math.roundToInt

/**
 * Pantalla M11c · Permiso de supervisor (7826424e-dcd0-42c7-a696-e5d2caab022b).
 *
 * Se utiliza cuando se intenta anular un plato o línea que ya fue enviada a cocina.
 * Requiere que un supervisor ingrese su PIN de 4 dígitos para autorizar la acción
 * sin necesidad de cerrar la sesión del mozo actual.
 */
@Composable
fun PermisoSupervisorScreen(
    platoNombre: String = "Lomo saltado",
    mesaNumero: String = "5",
    personaNumero: Int = 1,
    motivo: String = "demora en cocina",
    monto: String = "S/ 18.00",
    supervisorNombre: String = "Ana M.",
    enLinea: Boolean = true,
    alVolver: () -> Unit,
    alConfirmarAnulacion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val coroutineScope = rememberCoroutineScope()

    var pinIngresado by remember { mutableStateOf("") }
    val shakeOffset = remember { Animatable(0f) }

    val estaAprobado = pinIngresado.length == 4

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // 1. Cabecera (.hd)
            CabeceraPermisoSupervisor(
                enLinea = enLinea,
                alVolver = alVolver,
            )

            // 2. Cuerpo de la pantalla con tarjeta de plato, PIN y teclado numérico
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Sección superior: Resumen de plato y bloque de PIN
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Tarjeta del plato (.card .pad .row)
                    TarjetaResumenPlato(
                        platoNombre = platoNombre,
                        mesaNumero = mesaNumero,
                        personaNumero = personaNumero,
                        motivo = motivo,
                        monto = monto,
                    )

                    // Bloque de autorización de supervisor con puntos y animación
                    BloqueSupervisorPin(
                        supervisorNombre = supervisorNombre,
                        pinLongitud = pinIngresado.length,
                        estaAprobado = estaAprobado,
                        shakeOffsetX = shakeOffset.value,
                    )
                }

                // Sección inferior del cuerpo: Teclado numérico (.kp)
                TecladoNumericoSupervisor(
                    onNumeroClick = { digito ->
                        if (pinIngresado.length < 4) {
                            pinIngresado += digito
                        }
                    },
                    onBorrarClick = {
                        if (pinIngresado.isNotEmpty()) {
                            pinIngresado = pinIngresado.dropLast(1)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )
            }

            // 3. Dock inferior fijo (.dock)
            DockInferiorPermiso(
                estaAprobado = estaAprobado,
                alAnularPlato = {
                    if (estaAprobado) {
                        alConfirmarAnulacion()
                    } else {
                        coroutineScope.launch {
                            shakeOffset.snapTo(0f)
                            shakeOffset.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioHighBouncy,
                                    stiffness = Spring.StiffnessMedium,
                                ),
                            )
                        }
                    }
                },
            )
        }
    }
}

/**
 * 1. Cabecera (.hd): Barra de herramientas con botón volver (.ib),
 * estado "En línea" (.conn), Título grande "Permiso necesario" y subtítulo.
 */
@Composable
private fun CabeceraPermisoSupervisor(
    enLinea: Boolean,
    alVolver: () -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // Barra superior con botón volver y estado de conexión
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Botón volver (.ib)
            val volverInteraction = remember { MutableInteractionSource() }
            val volverPressed by volverInteraction.collectIsPressedAsState()
            val volverScale by animateFloatAsState(
                targetValue = if (volverPressed) 0.94f else 1f,
                animationSpec = tween(120, easing = RikosMotion.Ease),
                label = "volverScale",
            )

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .graphicsLayer {
                        scaleX = volverScale
                        scaleY = volverScale
                    }
                    .shadow(elevation = 2.dp, shape = CircleShape, clip = false)
                    .background(colors.surface, CircleShape)
                    .border(1.dp, colors.line, CircleShape)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = volverInteraction,
                        indication = null,
                        role = Role.Button,
                        onClick = alVolver,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(20.dp)) {
                    val p = Path().apply {
                        moveTo(size.width * 0.62f, size.height * 0.25f)
                        lineTo(size.width * 0.38f, size.height * 0.5f)
                        lineTo(size.width * 0.62f, size.height * 0.75f)
                    }
                    drawPath(
                        path = p,
                        color = colors.ink,
                        style = Stroke(
                            width = 2.2f.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }
            }

            // Estado de conexión (.conn)
            Row(
                modifier = Modifier
                    .height(32.dp)
                    .background(colors.surface, RoundedCornerShape(16.dp))
                    .border(1.dp, colors.line, RoundedCornerShape(16.dp))
                    .padding(horizontal = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (enLinea) colors.green else colors.red, CircleShape),
                )
                BasicText(
                    text = if (enLinea) "En línea" else "Sin conexión",
                    style = TextStyle(
                        fontFamily = type.headline.fontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.ink2,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Título H1
        BasicText(
            text = "Permiso necesario",
            style = TextStyle(
                fontFamily = type.title1.fontFamily,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.ink,
                lineHeight = 35.sp,
            ),
        )

        // Subtítulo
        BasicText(
            text = "Anular un plato ya enviado",
            style = TextStyle(
                fontFamily = type.subhead.fontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.ink3,
            ),
        )
    }
}

/**
 * 2. Tarjeta de resumen del plato (.card .pad .row):
 * Muestra el ícono de tacho en fondo rojo suave, el nombre del plato,
 * la ubicación en la mesa y motivo, y el precio en tabular numerals.
 */
@Composable
private fun TarjetaResumenPlato(
    platoNombre: String,
    mesaNumero: String,
    personaNumero: Int,
    motivo: String,
    monto: String,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(20.dp), clip = false)
            .background(colors.surface, RoundedCornerShape(20.dp))
            .border(1.dp, colors.line, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Avatar cuadrado con ícono de tacho (.av.sq)
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(colors.redSoft, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.size(22.dp)) {
                val strokeWidth = 2.dp.toPx()
                val w = size.width
                val h = size.height

                // Línea superior tapa
                drawLine(
                    color = colors.red,
                    start = androidx.compose.ui.geometry.Offset(w * 0.12f, h * 0.25f),
                    end = androidx.compose.ui.geometry.Offset(w * 0.88f, h * 0.25f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )

                // Asa de la tapa
                val asa = Path().apply {
                    moveTo(w * 0.33f, h * 0.25f)
                    lineTo(w * 0.33f, h * 0.16f)
                    lineTo(w * 0.67f, h * 0.16f)
                    lineTo(w * 0.67f, h * 0.25f)
                }
                drawPath(asa, colors.red, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))

                // Cuerpo del tacho
                val cuerpo = Path().apply {
                    moveTo(w * 0.79f, h * 0.25f)
                    lineTo(w * 0.75f, h * 0.85f)
                    lineTo(w * 0.25f, h * 0.85f)
                    lineTo(w * 0.21f, h * 0.25f)
                }
                drawPath(cuerpo, colors.red, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }

        // Información de texto
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            BasicText(
                text = "Anular $platoNombre",
                style = TextStyle(
                    fontFamily = type.headline.fontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.ink,
                ),
            )
            BasicText(
                text = "Mesa $mesaNumero, P$personaNumero. Motivo: $motivo",
                style = TextStyle(
                    fontFamily = type.caption.fontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.ink3,
                ),
            )
        }

        // Monto en tabular numerals (.money)
        BasicText(
            text = monto,
            style = TextStyle(
                fontFamily = type.headline.fontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.ink,
            ),
        )
    }
}

/**
 * 3. Bloque de supervisor con escudo de seguridad (.av navy),
 * mensaje de autorización, 4 puntos de PIN (.dots) y pastilla de aprobación (.tag.t-green.rise).
 */
@Composable
private fun BloqueSupervisorPin(
    supervisorNombre: String,
    pinLongitud: Int,
    estaAprobado: Boolean,
    shakeOffsetX: Float,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(shakeOffsetX.roundToInt(), 0) }
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Escudo de seguridad (56x56 dp en Navy)
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(colors.navy, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.size(26.dp)) {
                val w = size.width
                val h = size.height
                val shieldPath = Path().apply {
                    moveTo(w * 0.5f, h * 0.95f)
                    cubicTo(
                        w * 0.5f, h * 0.95f,
                        w * 0.9f, h * 0.72f,
                        w * 0.9f, h * 0.44f,
                    )
                    lineTo(w * 0.9f, h * 0.22f)
                    lineTo(w * 0.5f, h * 0.08f)
                    lineTo(w * 0.1f, h * 0.22f)
                    lineTo(w * 0.1f, h * 0.44f)
                    cubicTo(
                        w * 0.1f, h * 0.72f,
                        w * 0.5f, h * 0.95f,
                        w * 0.5f, h * 0.95f,
                    )
                    close()
                }
                drawPath(
                    path = shieldPath,
                    color = Color.White,
                    style = Stroke(
                        width = 2.2f.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
        }

        // Título del bloque
        BasicText(
            text = "Que un supervisor ingrese su PIN",
            style = TextStyle(
                fontFamily = type.headline.fontFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.ink,
                textAlign = TextAlign.Center,
            ),
        )

        // Subtítulo explicativo
        BasicText(
            text = "Tu perfil no puede anular platos enviados. El supervisor aprueba sin cerrar tu sesión.",
            style = TextStyle(
                fontFamily = type.caption.fontFamily,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                color = colors.ink3,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
            ),
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        // Indicador de 4 puntos de PIN (.dots)
        Row(
            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (i in 1..4) {
                val estaLleno = pinLongitud >= i
                val dotScale by animateFloatAsState(
                    targetValue = if (estaLleno) 1.25f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                    label = "dotScale$i",
                )

                val dotBg = when {
                    estaAprobado -> colors.green
                    estaLleno -> colors.navy
                    else -> Color.Transparent
                }

                val dotBorder = when {
                    estaAprobado -> colors.green
                    estaLleno -> colors.navy
                    else -> colors.line
                }

                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer {
                            scaleX = dotScale
                            scaleY = dotScale
                        }
                        .background(dotBg, CircleShape)
                        .border(2.5.dp, dotBorder, CircleShape),
                )
            }
        }

        // Tag de aprobación con check verde (.tag.t-green.rise)
        AnimatedVisibility(
            visible = estaAprobado,
            enter = fadeIn(tween(220, easing = RikosMotion.Ease)) + scaleIn(tween(220, easing = RikosMotion.Ease)),
            exit = fadeOut(tween(160, easing = RikosMotion.Ease)) + scaleOut(tween(160, easing = RikosMotion.Ease)),
        ) {
            Row(
                modifier = Modifier
                    .height(32.dp)
                    .background(colors.greenSoft, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(16.dp)) {
                    val p = Path().apply {
                        moveTo(size.width * 0.18f, size.height * 0.52f)
                        lineTo(size.width * 0.42f, size.height * 0.76f)
                        lineTo(size.width * 0.82f, size.height * 0.28f)
                    }
                    drawPath(
                        path = p,
                        color = colors.greenText,
                        style = Stroke(
                            width = 2.2f.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }

                BasicText(
                    text = "Aprobado por $supervisorNombre",
                    style = TextStyle(
                        fontFamily = type.headline.fontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.greenText,
                    ),
                )
            }
        }
    }
}

/**
 * 4. Teclado numérico táctil (.kp):
 * Grilla 3x4 con teclas 1-9, tecla vacía, tecla 0 y tecla retroceso (.key.alt).
 */
@Composable
private fun TecladoNumericoSupervisor(
    onNumeroClick: (String) -> Unit,
    onBorrarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val filas = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
        )

        filas.forEach { fila ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                fila.forEach { num ->
                    TeclaNumero(
                        texto = num,
                        onClick = { onNumeroClick(num) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // Última fila: Espacio vacío, Tecla 0, Tecla Borrar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Spacer(modifier = Modifier.weight(1f))

            TeclaNumero(
                texto = "0",
                onClick = { onNumeroClick("0") },
                modifier = Modifier.weight(1f),
            )

            TeclaBorrar(
                onClick = onBorrarClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Tecla numérica individual con animación táctil al presionar (.key).
 */
@Composable
private fun TeclaNumero(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = tween(120, easing = RikosMotion.Ease),
        label = "teclaScale_$texto",
    )

    Box(
        modifier = modifier
            .height(64.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(18.dp), clip = false)
            .background(colors.surface, RoundedCornerShape(18.dp))
            .border(1.dp, colors.line, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = texto,
            style = TextStyle(
                fontFamily = type.title2.fontFamily,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.ink,
            ),
        )
    }
}

/**
 * Tecla para borrar dígitos con ícono SVG de retroceso (.key.alt).
 */
@Composable
private fun TeclaBorrar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = tween(120, easing = RikosMotion.Ease),
        label = "borrarScale",
    )

    Box(
        modifier = modifier
            .height(64.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = "Borrar dígito" },
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(24.dp)) {
            val w = size.width
            val h = size.height
            val strokeWidth = 2.dp.toPx()

            val silueta = Path().apply {
                moveTo(w * 0.9f, h * 0.18f)
                lineTo(w * 0.35f, h * 0.18f)
                lineTo(w * 0.08f, h * 0.5f)
                lineTo(w * 0.35f, h * 0.82f)
                lineTo(w * 0.9f, h * 0.82f)
                close()
            }
            drawPath(
                path = silueta,
                color = colors.ink2,
                style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )

            drawLine(
                color = colors.ink2,
                start = androidx.compose.ui.geometry.Offset(w * 0.72f, h * 0.38f),
                end = androidx.compose.ui.geometry.Offset(w * 0.48f, h * 0.62f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = colors.ink2,
                start = androidx.compose.ui.geometry.Offset(w * 0.48f, h * 0.38f),
                end = androidx.compose.ui.geometry.Offset(w * 0.72f, h * 0.62f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * 5. Dock inferior (.dock):
 * Si no está aprobado: Botón deshabilitado neutro "Esperando PIN del supervisor" (.btn.off.blk).
 * Si está aprobado: Botón destructivo en rojo sólido "Anular plato" (.btn.dang-solid.blk).
 */
@Composable
private fun DockInferiorPermiso(
    estaAprobado: Boolean,
    alAnularPlato: () -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    val anularInteraction = remember { MutableInteractionSource() }
    val anularPressed by anularInteraction.collectIsPressedAsState()
    val anularScale by animateFloatAsState(
        targetValue = if (estaAprobado && anularPressed) 0.96f else 1f,
        animationSpec = tween(120, easing = RikosMotion.Ease),
        label = "anularBtnScale",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .border(1.dp, colors.line)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        if (estaAprobado) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .graphicsLayer {
                        scaleX = anularScale
                        scaleY = anularScale
                    }
                    .background(colors.red, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(
                        interactionSource = anularInteraction,
                        indication = null,
                        role = Role.Button,
                        onClick = alAnularPlato,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "Anular plato",
                    style = TextStyle(
                        fontFamily = type.headline.fontFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    ),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(colors.surface2, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "Esperando PIN del supervisor",
                    style = TextStyle(
                        fontFamily = type.headline.fontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.ink3,
                    ),
                )
            }
        }
    }
}
