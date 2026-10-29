package pe.rikos.app.feature.mesas.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.rikos.app.core.designsystem.theme.Rikos
import pe.rikos.app.feature.mesas.domain.ColorCredito
import pe.rikos.app.feature.mesas.domain.Pensionista
import pe.rikos.app.feature.mesas.domain.obtenerPensionistasEjemplo

/**
 * Pantalla M10 · Pensionista en un asiento.
 * Permite buscar y vincular a un cliente pensionista a un asiento específico de la mesa comensal.
 * 
 * Componentes:
 * - Cabecera con botón volver, estado En línea, título "Pensionista en PN" y subtítulo "Mesa X".
 * - Buscador con icono de lupa (Buscar por nombre o DNI).
 * - Lista de tarjetas seleccionables con iniciales, tipo de pensión, saldo y barra de progreso.
 * - Aviso contextual de advertencia si el pensionista seleccionado supera o roza su límite.
 * - Dock inferior fijo con resumen "Hoy le corresponde" y botón primario "Vincular a [Nombre]".
 */
@Composable
fun PensionistaScreen(
    asientoEtiqueta: String = "P2",
    mesaNumero: String = "5",
    enLinea: Boolean = true,
    alVolver: () -> Unit = {},
    alVincular: (Pensionista) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type
    val focusManager = LocalFocusManager.current

    val todosLosPensionistas = remember { obtenerPensionistasEjemplo() }

    // En el prototipo inicial, la búsqueda tiene "ro" para mostrar los resultados coincidentes
    var queryBusqueda by remember { mutableStateOf("ro") }
    var seleccionadoId by remember {
        // En el prototipo inicial, Rosa Torres (índice 0) o Carmen Rojas (índice 1)
        mutableStateOf(todosLosPensionistas.firstOrNull()?.id.orEmpty())
    }

    val pensionistasFiltrados = remember(queryBusqueda, todosLosPensionistas) {
        if (queryBusqueda.isBlank()) {
            todosLosPensionistas
        } else {
            todosLosPensionistas.filter { p ->
                p.nombre.contains(queryBusqueda, ignoreCase = true) ||
                    p.dni.contains(queryBusqueda)
            }
        }
    }

    val pensionistaSeleccionado = remember(seleccionadoId, todosLosPensionistas) {
        todosLosPensionistas.find { it.id == seleccionadoId } ?: todosLosPensionistas.first()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        // 1. Cabecera (.hd)
        CabeceraPensionista(
            asientoEtiqueta = asientoEtiqueta,
            mesaNumero = mesaNumero,
            enLinea = enLinea,
            alVolver = alVolver,
        )

        // 2. Contenido scrolleable
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                // Buscador por nombre o DNI (.fx.focus)
                BuscadorPensionista(
                    query = queryBusqueda,
                    alCambiarQuery = { queryBusqueda = it },
                    alLimpiar = { queryBusqueda = "" },
                    onBuscar = { focusManager.clearFocus() },
                )
            }

            // Lista de tarjetas de pensionistas
            items(
                items = pensionistasFiltrados,
                key = { it.id },
            ) { pensionista ->
                TarjetaPensionista(
                    pensionista = pensionista,
                    seleccionado = pensionista.id == seleccionadoId,
                    alSeleccionar = {
                        seleccionadoId = pensionista.id
                        focusManager.clearFocus()
                    },
                )
            }

            // Aviso de límite contextual (.nt.nt-warn)
            item {
                AnimatedVisibility(
                    visible = pensionistaSeleccionado.advertencia != null,
                    enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                    exit = fadeOut(tween(150)) + shrinkVertically(tween(150)),
                ) {
                    val advertencia = pensionistaSeleccionado.advertencia
                    if (advertencia != null) {
                        AvisoLimitePensionista(advertencia = advertencia)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        // 3. Dock inferior fijo (.dock)
        DockInferiorPensionista(
            pensionista = pensionistaSeleccionado,
            alVincular = { alVincular(pensionistaSeleccionado) },
        )
    }
}

/**
 * Cabecera (.hd) con botón volver, pill En línea y títulos H1 + Sub.
 */
@Composable
private fun CabeceraPensionista(
    asientoEtiqueta: String,
    mesaNumero: String,
    enLinea: Boolean,
    alVolver: () -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    val volverInteraction = remember { MutableInteractionSource() }
    val volverPressed by volverInteraction.collectIsPressedAsState()
    val volverScale by animateFloatAsState(
        targetValue = if (volverPressed) 0.95f else 1f,
        animationSpec = tween(120),
        label = "volverScale",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // Barra superior (.tbar)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Botón volver (.ib)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer { scaleX = volverScale; scaleY = volverScale }
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.line, RoundedCornerShape(12.dp))
                    .semantics { contentDescription = "Volver al pedido" }
                    .clickable(
                        interactionSource = volverInteraction,
                        indication = null,
                        role = Role.Button,
                        onClick = alVolver,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(16.dp)) {
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width * 0.65f, size.height * 0.15f)
                        lineTo(size.width * 0.30f, size.height * 0.50f)
                        lineTo(size.width * 0.65f, size.height * 0.85f)
                    }
                    drawPath(
                        path = path,
                        color = colors.ink,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
            }

            // Pill En línea (.conn)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.line, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (enLinea) Color(0xFF10B981) else colors.ink3),
                )
                BasicText(
                    text = if (enLinea) "En línea" else "Sin conexión",
                    style = type.caption.copy(
                        color = colors.ink2,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    ),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // H1: Pensionista en PN
        BasicText(
            text = "Pensionista en $asientoEtiqueta",
            style = type.title1.copy(
                color = colors.ink,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.02).sp,
            ),
        )

        Spacer(Modifier.height(2.dp))

        // Subtítulo (.sub): Mesa X
        BasicText(
            text = "Mesa $mesaNumero",
            style = type.subhead.copy(
                color = colors.ink3,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

/**
 * Campo de búsqueda (.fx.focus) con icono de lupa e input de texto.
 */
@Composable
private fun BuscadorPensionista(
    query: String,
    alCambiarQuery: (String) -> Unit,
    alLimpiar: () -> Unit,
    onBuscar: () -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(1.dp, colors.line, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Icono lupa
        Canvas(Modifier.size(18.dp)) {
            val strokeW = 1.8.dp.toPx()
            val r = size.width * 0.38f
            val center = Offset(size.width * 0.42f, size.height * 0.42f)
            drawCircle(
                color = colors.ink3,
                radius = r,
                center = center,
                style = Stroke(width = strokeW),
            )
            drawLine(
                color = colors.ink3,
                start = Offset(center.x + r * 0.7f, center.y + r * 0.7f),
                end = Offset(size.width * 0.95f, size.height * 0.95f),
                strokeWidth = strokeW,
                cap = StrokeCap.Round,
            )
        }

        // Input
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (query.isEmpty()) {
                BasicText(
                    text = "Buscar por nombre o DNI",
                    style = type.body.copy(
                        color = colors.ink3,
                        fontSize = 15.sp,
                    ),
                )
            }
            BasicTextField(
                value = query,
                onValueChange = alCambiarQuery,
                singleLine = true,
                textStyle = type.body.copy(
                    color = colors.ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onBuscar() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Botón limpiar
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(colors.ink3.copy(alpha = 0.2f))
                    .clickable(onClick = alLimpiar),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(10.dp)) {
                    val w = 1.5.dp.toPx()
                    drawLine(colors.ink2, Offset(0f, 0f), Offset(size.width, size.height), strokeWidth = w, cap = StrokeCap.Round)
                    drawLine(colors.ink2, Offset(0f, size.height), Offset(size.width, 0f), strokeWidth = w, cap = StrokeCap.Round)
                }
            }
        }
    }
}

/**
 * Tarjeta individual de pensionista (.card.pad.col).
 * Contiene avatar con iniciales, datos, radio button y barra de crédito usado.
 */
@Composable
private fun TarjetaPensionista(
    pensionista: Pensionista,
    seleccionado: Boolean,
    alSeleccionar: () -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = tween(120),
        label = "tarjetaScale_${pensionista.id}",
    )

    val borderColorAnim by animateColorAsState(
        targetValue = if (seleccionado) colors.primary else colors.line,
        animationSpec = tween(150),
        label = "borderColor_${pensionista.id}",
    )

    // Color semántico para crédito
    val colorCredito = when (pensionista.colorCredito) {
        ColorCredito.ROJO -> colors.red
        ColorCredito.VERDE -> colors.green
        ColorCredito.NARANJA -> colors.primary
    }

    val animatedProgreso by animateFloatAsState(
        targetValue = pensionista.porcentajeUso,
        animationSpec = tween(durationMillis = 400),
        label = "progresoCredito_${pensionista.id}",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .border(
                width = if (seleccionado) 2.dp else 1.dp,
                color = borderColorAnim,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.RadioButton,
                onClick = alSeleccionar,
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Fila superior: Avatar, Nombres, Radio button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Avatar con iniciales (.av)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (seleccionado) colors.primary else colors.surface2,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = pensionista.iniciales,
                    style = type.body.copy(
                        color = if (seleccionado) colors.onPrimary else colors.ink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    ),
                )
            }

            // Datos del pensionista
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                BasicText(
                    text = pensionista.nombre,
                    style = type.body.copy(
                        color = colors.ink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    ),
                )
                BasicText(
                    text = pensionista.tipoPension,
                    style = type.caption.copy(
                        color = colors.ink3,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }

            // Radio button (.rr)
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(colors.surface)
                    .border(
                        width = if (seleccionado) 2.dp else 1.5.dp,
                        color = if (seleccionado) colors.primary else colors.line,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (seleccionado) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colors.primary),
                    )
                }
            }
        }

        // Fila inferior: Crédito usado + Barra de progreso (.bar)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    text = "Crédito usado",
                    style = type.caption.copy(
                        color = colors.ink3,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                BasicText(
                    text = pensionista.creditoTexto,
                    style = type.caption.copy(
                        color = colorCredito,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }

            // Barra de progreso de crédito (.bar)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(colors.line),
            ) {
                if (animatedProgreso > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgreso)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(colorCredito),
                    )
                }
            }
        }
    }
}

/**
 * Aviso contextual de advertencia (.nt.nt-warn) cuando el límite del pensionista se supera.
 */
@Composable
private fun AvisoLimitePensionista(
    advertencia: String,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    // Fondo y borde ámbar del sistema de diseño
    val warnBg = colors.amberSoft
    val warnBorder = colors.amber.copy(alpha = 0.4f)
    val warnIconColor = colors.amber
    val warnTextColor = colors.amberText

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(warnBg)
            .border(1.dp, warnBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Icono triángulo de advertencia
        Canvas(Modifier.size(20.dp)) {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(size.width * 0.50f, size.height * 0.12f)
                lineTo(size.width * 0.94f, size.height * 0.88f)
                lineTo(size.width * 0.06f, size.height * 0.88f)
                close()
            }
            drawPath(
                path = path,
                color = warnIconColor,
                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round),
            )
            // Signo de exclamación
            drawLine(
                color = warnIconColor,
                start = Offset(size.width * 0.5f, size.height * 0.42f),
                end = Offset(size.width * 0.5f, size.height * 0.62f),
                strokeWidth = 1.8.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = warnIconColor,
                radius = 1.2.dp.toPx(),
                center = Offset(size.width * 0.5f, size.height * 0.76f),
            )
        }

        // Texto con prefijo en negrita
        val annotatedText = remember(advertencia) {
            val prefix = "Pasaría su límite por S/ 4.00"
            if (advertencia.startsWith(prefix)) {
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(prefix)
                    }
                    append(advertencia.removePrefix(prefix))
                }
            } else {
                buildAnnotatedString {
                    append(advertencia)
                }
            }
        }

        BasicText(
            text = annotatedText,
            style = TextStyle(
                fontFamily = type.caption.fontFamily,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = warnTextColor,
            ),
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Dock inferior fijo (.dock) con resumen "Hoy le corresponde" y botón "Vincular a [Nombre]".
 */
@Composable
private fun DockInferiorPensionista(
    pensionista: Pensionista,
    alVincular: () -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(120),
        label = "btnVincularScale",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .border(
                width = 1.dp,
                color = colors.line,
                shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp),
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Fila: Hoy le corresponde <--> [Valor]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = "Hoy le corresponde",
                style = type.subhead.copy(
                    color = colors.ink3,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            BasicText(
                text = pensionista.correspondeHoy,
                style = type.subhead.copy(
                    color = colors.ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }

        // Botón primario (.btn.pri.blk)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .graphicsLayer { scaleX = buttonScale; scaleY = buttonScale }
                .clip(RoundedCornerShape(14.dp))
                .background(colors.primary)
                .semantics { role = Role.Button }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = alVincular,
                ),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = "Vincular a ${pensionista.primerNombre}",
                style = type.body.copy(
                    color = colors.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                ),
            )
        }
    }
}
