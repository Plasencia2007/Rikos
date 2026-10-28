package pe.rikos.app.feature.auth.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.rikos.app.core.designsystem.theme.Rikos
import pe.rikos.app.core.designsystem.theme.RikosMotion
import pe.rikos.app.core.designsystem.theme.RikosSpace
import pe.rikos.app.feature.auth.domain.ErrorAuth
import pe.rikos.app.feature.auth.domain.IniciarConPin
import pe.rikos.app.feature.auth.domain.Mozo

/**
 * Pantalla M02 · PIN de mozo sobre fondo azul marino (`.screen.navy` en el lienzo).
 */
@Composable
fun PinScreen(
    viewModel: PinViewModel,
    modifier: Modifier = Modifier,
    alOlvidarPin: () -> Unit = {},
) {
    val estado by viewModel.estado.collectAsState()
    PinContent(
        estado = estado,
        alTocarTecla = viewModel::alTocarTecla,
        alBorrar = viewModel::alBorrar,
        alSeleccionarMozo = viewModel::alSeleccionarMozo,
        alOlvidarPin = alOlvidarPin,
        modifier = modifier,
    )
}

@Composable
fun PinContent(
    estado: PinState,
    alTocarTecla: (Char) -> Unit,
    alBorrar: () -> Unit,
    alSeleccionarMozo: (Mozo) -> Unit,
    alOlvidarPin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Rikos.colors
    val type = Rikos.type
    val reducirMovimiento = rememberCoroutineScope().coroutineContext[MotionDurationScale]?.scaleFactor == 0f

    // Animación de sacudida (shake) al fallar PIN (token 'shake', 460 ms, offset ±10 dp)
    val shakeOffsetX = remember { Animatable(0f) }
    LaunchedEffect(estado.shakeCount) {
        if (estado.shakeCount > 0 && !reducirMovimiento) {
            shakeOffsetX.snapTo(0f)
            shakeOffsetX.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 460
                    -2f at 46 using RikosMotion.ShakeEase
                    5f at 92 using RikosMotion.ShakeEase
                    -10f at 138 using RikosMotion.ShakeEase
                    10f at 184 using RikosMotion.ShakeEase
                    -10f at 230 using RikosMotion.ShakeEase
                    10f at 276 using RikosMotion.ShakeEase
                    -10f at 322 using RikosMotion.ShakeEase
                    5f at 368 using RikosMotion.ShakeEase
                    -2f at 414 using RikosMotion.ShakeEase
                    0f at 460 using RikosMotion.ShakeEase
                },
            )
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(c.navy),
        contentAlignment = Alignment.TopCenter,
    ) {
        val altoDisponible = maxHeight

        Column(
            modifier = Modifier
                .widthIn(max = 430.dp)
                .fillMaxWidth()
                .heightIn(min = altoDisponible)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RikosSpace.screenMargin)
                .padding(top = 28.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Bloque superior: Ícono, Título, Selector de mozo y Puntos
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Ícono squircle verde azulado (52x52dp, radio 16dp)
                IconoCubiertosPin(
                    modifier = Modifier.size(52.dp),
                    fondo = c.primary,
                    colorTrazo = Color.White,
                )

                // Título y local
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    BasicText(
                        text = "Ingresa tu PIN",
                        modifier = Modifier.semantics { heading() },
                        style = type.title2.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.02).sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        ),
                    )
                    BasicText(
                        text = "${estado.localNombre}. Elige quién atiende",
                        style = type.subhead.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.70f),
                            textAlign = TextAlign.Center,
                        ),
                    )
                }

                // Selector de mozos (Luis, Ana, Pedro)
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    estado.mozos.forEach { mozo ->
                        val seleccionado = mozo.id == estado.mozoSeleccionado.id
                        TarjetaMozo(
                            mozo = mozo,
                            seleccionado = seleccionado,
                            alSeleccionar = { alSeleccionarMozo(mozo) },
                        )
                    }
                }

                // Indicador de 4 puntos (con sacudida si hay error)
                Row(
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .offset(x = shakeOffsetX.value.dp)
                        .clearAndSetSemantics {
                            contentDescription = "${estado.digitos.length} de ${IniciarConPin.LARGO_PIN} dígitos ingresados"
                        },
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(IniciarConPin.LARGO_PIN) { i ->
                        val lleno = i < estado.digitos.length
                        val esError = estado.error != null
                        val colorPunto = when {
                            esError -> Color(0xFFFF6B7F)
                            lleno -> Color.White
                            else -> Color.Transparent
                        }
                        val colorBorde = when {
                            esError -> Color(0xFFFF6B7F)
                            lleno -> Color.White
                            else -> Color.White.copy(alpha = 0.50f)
                        }

                        PinDot(
                            lleno = lleno,
                            colorPunto = colorPunto,
                            colorBorde = colorBorde,
                            reducirMovimiento = reducirMovimiento,
                        )
                    }
                }

                // Mensaje de estado / error
                Box(
                    modifier = Modifier
                        .heightIn(min = 26.dp)
                        .padding(top = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val textoError = when (val err = estado.error) {
                        null -> " "
                        is ErrorAuth.PinIncorrecto -> "PIN incorrecto. Te quedan ${estado.intentosRestantes} intentos"
                        is ErrorAuth.Bloqueado -> "Demasiados intentos. Pide ayuda al administrador."
                        is ErrorAuth.SinConexion -> "Sin conexión con el local."
                        is ErrorAuth.Desconocido -> "Algo salió mal. Intenta de nuevo."
                    }
                    BasicText(
                        text = textoError,
                        style = type.subhead.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (estado.error == null) Color.Transparent else Color(0xFFFF6B7F),
                            textAlign = TextAlign.Center,
                        ),
                    )
                }
            }

            // Teclado numérico redondeado estilo iOS (`.kp.r` en CSS)
            TecladoNumerico(
                alTocarTecla = alTocarTecla,
                alBorrar = alBorrar,
                alOlvidarPin = alOlvidarPin,
                deshabilitado = estado.cargando || estado.error is ErrorAuth.Bloqueado,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun PinDot(
    lleno: Boolean,
    colorPunto: Color,
    colorBorde: Color,
    reducirMovimiento: Boolean,
) {
    val escala = remember { Animatable(1f) }
    LaunchedEffect(lleno, reducirMovimiento) {
        if (lleno && !reducirMovimiento) {
            escala.snapTo(0.6f)
            escala.animateTo(
                targetValue = 1f,
                animationSpec = keyframes {
                    durationMillis = 300
                    1.2f at 180 using RikosMotion.Ease
                    1f at 300 using RikosMotion.Ease
                },
            )
        } else {
            escala.snapTo(1f)
        }
    }

    Box(
        modifier = Modifier
            .size(20.dp)
            .graphicsLayer { scaleX = escala.value; scaleY = escala.value }
            .background(colorPunto, CircleShape)
            .border(2.5.dp, colorBorde, CircleShape),
    )
}

/**
 * Botón selector de mozo (`.who` en el lienzo).
 * Tamaño 84x84dp, radio 20dp. Al estar activo tiene borde turquesa `#2DD4BF`.
 */
@Composable
private fun TarjetaMozo(
    mozo: Mozo,
    seleccionado: Boolean,
    alSeleccionar: () -> Unit,
) {
    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (presionado) 0.96f else 1f,
        animationSpec = RikosMotion.press(),
        label = "escalaMozo",
    )

    val fondo = if (seleccionado) Color(0x1F2DD4BF) else Color.White.copy(alpha = 0.08f)
    val borde = if (seleccionado) Color(0xFF2DD4BF) else Color.Transparent

    Column(
        modifier = Modifier
            .size(84.dp)
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .background(fondo, RoundedCornerShape(20.dp))
            .border(2.dp, borde, RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interaccion,
                indication = null,
                role = Role.RadioButton,
                onClick = alSeleccionar,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Avatar circular 36x36dp
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    if (seleccionado) Color(0xFF0B7A6C) else Color.White.copy(alpha = 0.14f),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = mozo.iniciales,
                style = Rikos.type.caption.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                ),
            )
        }
        Spacer(Modifier.height(6.dp))
        BasicText(
            text = mozo.nombre,
            style = Rikos.type.footnote.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            ),
        )
    }
}

/**
 * Teclado numérico con teclas ovaladas tipo iOS (`.kp.r`).
 */
@Composable
private fun TecladoNumerico(
    alTocarTecla: (Char) -> Unit,
    alBorrar: () -> Unit,
    alOlvidarPin: () -> Unit,
    deshabilitado: Boolean,
    modifier: Modifier = Modifier,
) {
    val letras = mapOf(
        '2' to "ABC", '3' to "DEF", '4' to "GHI",
        '5' to "JKL", '6' to "MNO", '7' to "PQRS",
        '8' to "TUV", '9' to "WXYZ",
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Filas 1 a 3
        listOf("123", "456", "789").forEach { fila ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                fila.forEach { digito ->
                    TeclaPill(
                        digito = digito,
                        subtexto = letras[digito] ?: "",
                        deshabilitado = deshabilitado,
                        alTocar = { alTocarTecla(digito) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // Fila 4: Olvidé, 0, Borrar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Tecla "Olvidé"
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(78.dp)
                    .semantics { contentDescription = "Borrar dígito" }
                    .clickable(
                        enabled = !deshabilitado,
                        role = Role.Button,
                        onClick = alOlvidarPin,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "Olvidé",
                    style = Rikos.type.subhead.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.85f),
                    ),
                )
            }

            // Tecla '0'
            TeclaPill(
                digito = '0',
                subtexto = "",
                deshabilitado = deshabilitado,
                alTocar = { alTocarTecla('0') },
                modifier = Modifier.weight(1f),
            )

            // Tecla Borrar
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(78.dp)
                    .clickable(
                        enabled = !deshabilitado,
                        role = Role.Button,
                        onClickLabel = "Borrar",
                        onClick = alBorrar,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                IconoBorrar(
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

/**
 * Tecla numérica individual con forma de píldora (78dp alto, radio 39dp).
 */
@Composable
private fun TeclaPill(
    digito: Char,
    subtexto: String,
    deshabilitado: Boolean,
    alTocar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (presionado && !deshabilitado) 0.95f else 1f,
        animationSpec = RikosMotion.press(),
        label = "escalaTecla",
    )

    val fondo = if (presionado && !deshabilitado) {
        Color.White.copy(alpha = 0.22f)
    } else {
        Color.White.copy(alpha = 0.10f)
    }

    Column(
        modifier = modifier
            .height(78.dp)
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .background(fondo, RoundedCornerShape(39.dp))
            .clickable(
                interactionSource = interaccion,
                indication = null,
                enabled = !deshabilitado,
                role = Role.Button,
                onClick = alTocar,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BasicText(
            text = digito.toString(),
            style = Rikos.type.title1.copy(
                fontSize = 32.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
            ),
        )
        if (subtexto.isNotEmpty()) {
            BasicText(
                text = subtexto,
                style = Rikos.type.caption.copy(
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.16.sp,
                    color = Color.White.copy(alpha = 0.55f),
                ),
            )
        }
    }
}

/**
 * Ícono de cubiertos para la cabecera del PIN (52x52dp).
 */
@Composable
private fun IconoCubiertosPin(
    fondo: Color,
    colorTrazo: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(fondo, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(28.dp)) {
            val escala = size.width / 24f
            scale(escala, pivot = Offset.Zero) {
                // Tenedor
                val tenedor = Path().apply {
                    moveTo(3f, 2f)
                    lineTo(3f, 9f)
                    cubicTo(3f, 10.1f, 3.9f, 11f, 5f, 11f)
                    lineTo(7f, 11f)
                    cubicTo(8.1f, 11f, 9f, 10.1f, 9f, 9f)
                    lineTo(9f, 2f)
                    moveTo(6f, 2f)
                    lineTo(6f, 22f)
                }
                drawPath(
                    path = tenedor,
                    color = colorTrazo,
                    style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )

                // Cuchillo
                val cuchillo = Path().apply {
                    moveTo(21f, 15f)
                    lineTo(21f, 2f)
                    cubicTo(18.2f, 2f, 16f, 4.2f, 16f, 7f)
                    lineTo(16f, 13f)
                    cubicTo(16f, 14.1f, 16.9f, 15f, 18f, 15f)
                    close()
                    moveTo(21f, 15f)
                    lineTo(21f, 22f)
                }
                drawPath(
                    path = cuchillo,
                    color = colorTrazo,
                    style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
    }
}

/**
 * Ícono SVG de borrar / backspace (viewBox 0 0 24 24).
 */
@Composable
private fun IconoBorrar(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val escala = size.width / 24f
        scale(escala, pivot = Offset.Zero) {
            val contorno = Path().apply {
                moveTo(21f, 4f)
                lineTo(8f, 4f)
                lineTo(1f, 12f)
                lineTo(8f, 20f)
                lineTo(21f, 20f)
                close()
            }
            drawPath(
                path = contorno,
                color = color,
                style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )

            // Cruz interior
            val cruz = Path().apply {
                moveTo(18f, 9f)
                lineTo(12f, 15f)
                moveTo(12f, 9f)
                lineTo(18f, 15f)
            }
            drawPath(
                path = cruz,
                color = color,
                style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}
