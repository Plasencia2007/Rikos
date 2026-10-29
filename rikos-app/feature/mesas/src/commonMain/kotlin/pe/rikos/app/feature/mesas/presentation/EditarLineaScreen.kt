package pe.rikos.app.feature.mesas.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.rikos.app.core.common.Dinero
import pe.rikos.app.core.designsystem.theme.Rikos
import pe.rikos.app.feature.mesas.domain.EstadoLineaPedido
import pe.rikos.app.feature.mesas.domain.LineaPedido
import pe.rikos.app.feature.mesas.domain.MotivoAnulacion
import pe.rikos.app.feature.mesas.domain.indicacionesPredefinidas

/**
 * Pantalla M11 · Editar y anular línea.
 * Permite cambiar la cantidad de una línea pedida, seleccionar o escribir indicaciones
 * para cocina, y anular la línea seleccionando un motivo para auditoría.
 */
@Composable
fun EditarLineaScreen(
    linea: LineaPedido,
    personaNumero: Int = 1,
    mesaNumero: String = "5",
    enLinea: Boolean = true,
    alVolver: () -> Unit = {},
    alGuardarCambios: (nuevaCantidad: Int, nuevaObservacion: String?) -> Unit = { _, _ -> },
    alAnularLinea: (MotivoAnulacion) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type
    val focusManager = LocalFocusManager.current

    var cantidad by remember { mutableStateOf(linea.cantidad) }

    // Inicializar chips seleccionados a partir de la observación actual
    var indicacionesSeleccionadas by remember {
        val obs = linea.observacion.orEmpty()
        val iniciales = indicacionesPredefinidas.filter { obs.contains(it, ignoreCase = true) }.toSet()
        mutableStateOf(if (iniciales.isEmpty()) setOf("Sin azúcar") else iniciales)
    }

    var otraIndicacion by remember {
        val obs = linea.observacion.orEmpty()
        var resto = obs
        indicacionesPredefinidas.forEach { resto = resto.replace(it, "", ignoreCase = true) }
        mutableStateOf(resto.trim(',', ' '))
    }

    var sheetAnulacionVisible by remember { mutableStateOf(false) }
    var motivoSeleccionado by remember { mutableStateOf<MotivoAnulacion?>(null) }

    val subtotalCalculado = remember(cantidad, linea.precioUnitario) {
        Dinero(linea.precioUnitario.centimos * cantidad)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Cabecera (.hd)
            CabeceraEditarLinea(
                nombrePlato = linea.nombre,
                personaNumero = personaNumero,
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
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Spacer(Modifier.height(4.dp))
                    // Tarjeta con estado, subtotal y stepper (.card.pad.col)
                    TarjetaResumenLinea(
                        estado = linea.estado,
                        subtotal = subtotalCalculado,
                        cantidad = cantidad,
                        onMenos = { if (cantidad > 1) cantidad-- },
                        onMas = { cantidad++ },
                    )
                }

                item {
                    // Sección Indicaciones rápidas
                    SeccionIndicacionesChips(
                        indicaciones = indicacionesPredefinidas,
                        seleccionadas = indicacionesSeleccionadas,
                        onToggle = { chip ->
                            indicacionesSeleccionadas = if (indicacionesSeleccionadas.contains(chip)) {
                                indicacionesSeleccionadas - chip
                            } else {
                                indicacionesSeleccionadas + chip
                            }
                        },
                    )
                }

                item {
                    // Campo Otra indicación (.fl + .fx)
                    CampoOtraIndicacion(
                        valor = otraIndicacion,
                        onCambio = { otraIndicacion = it },
                        onTerminado = { focusManager.clearFocus() },
                    )
                }

                item {
                    // Aviso informativo (.nt.nt-info)
                    AvisoComandaCambio()
                    Spacer(Modifier.height(16.dp))
                }
            }

            // 3. Dock inferior fijo con Guardar y Anular (.dock)
            DockInferiorEditarLinea(
                onGuardar = {
                    val obsFinal = buildList {
                        addAll(indicacionesSeleccionadas)
                        if (otraIndicacion.isNotBlank()) add(otraIndicacion.trim())
                    }.joinToString(", ").ifBlank { null }
                    alGuardarCambios(cantidad, obsFinal)
                },
                onAnular = {
                    focusManager.clearFocus()
                    sheetAnulacionVisible = true
                },
            )
        }

        // 4. Modal Bottom Sheet de Anulación (.scrim + .sheet)
        if (sheetAnulacionVisible) {
            ModalBottomSheetAnulacion(
                motivoSeleccionado = motivoSeleccionado,
                onSeleccionarMotivo = { motivoSeleccionado = it },
                onConfirmarAnulacion = {
                    motivoSeleccionado?.let { motivo ->
                        sheetAnulacionVisible = false
                        alAnularLinea(motivo)
                    }
                },
                onCerrar = {
                    sheetAnulacionVisible = false
                    motivoSeleccionado = null
                },
            )
        }
    }
}

/**
 * Cabecera superior con botón volver, pill En línea, nombre del plato y comensal/mesa.
 */
@Composable
private fun CabeceraEditarLinea(
    nombrePlato: String,
    personaNumero: Int,
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

        // Título H1: Nombre del plato
        BasicText(
            text = nombrePlato,
            style = type.title1.copy(
                color = colors.ink,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.02).sp,
            ),
        )

        Spacer(Modifier.height(2.dp))

        // Subtítulo (.sub): Persona N, mesa X
        BasicText(
            text = "Persona $personaNumero, mesa $mesaNumero",
            style = type.subhead.copy(
                color = colors.ink3,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

/**
 * Tarjeta de la línea (.card.pad.col) con tag de estado, subtotal y control stepper.
 */
@Composable
private fun TarjetaResumenLinea(
    estado: EstadoLineaPedido,
    subtotal: Dinero,
    cantidad: Int,
    onMenos: () -> Unit,
    onMas: () -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    val estadoTexto = when (estado) {
        EstadoLineaPedido.POR_ENVIAR -> "Por enviar"
        EstadoLineaPedido.EN_COCINA -> "En cocina"
        EstadoLineaPedido.LISTO_PARA_SERVIR -> "Listo para servir"
        EstadoLineaPedido.SERVIDO -> "Servido"
        EstadoLineaPedido.ANULADO -> "Anulado"
    }

    val estadoBg = when (estado) {
        EstadoLineaPedido.POR_ENVIAR -> colors.amberSoft
        EstadoLineaPedido.EN_COCINA -> colors.primarySoft
        EstadoLineaPedido.LISTO_PARA_SERVIR -> colors.greenSoft
        EstadoLineaPedido.SERVIDO -> colors.surface2
        EstadoLineaPedido.ANULADO -> colors.redSoft
    }

    val estadoColor = when (estado) {
        EstadoLineaPedido.POR_ENVIAR -> colors.amberText
        EstadoLineaPedido.EN_COCINA -> colors.primary
        EstadoLineaPedido.LISTO_PARA_SERVIR -> colors.greenText
        EstadoLineaPedido.SERVIDO -> colors.ink3
        EstadoLineaPedido.ANULADO -> colors.red
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .border(1.dp, colors.line, RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Fila 1: Tag de estado + Subtotal
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Tag
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(estadoBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = estadoTexto,
                    style = type.caption.copy(
                        color = estadoColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                    ),
                )
            }

            // Subtotal
            BasicText(
                text = subtotal.formatear(),
                style = type.title1.copy(
                    color = colors.ink,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.02).sp,
                ),
            )
        }

        // Fila 2: Cantidad + Stepper
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = "Cantidad",
                style = type.body.copy(
                    color = colors.ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                ),
            )

            // Stepper control (.stp)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface2)
                    .border(1.dp, colors.line, RoundedCornerShape(16.dp)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Menos (-)
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clickable(enabled = cantidad > 1, onClick = onMenos),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(16.dp)) {
                        drawLine(
                            color = if (cantidad > 1) colors.ink else colors.ink3.copy(alpha = 0.5f),
                            start = Offset(2.dp.toPx(), size.height / 2),
                            end = Offset(size.width - 2.dp.toPx(), size.height / 2),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }
                }

                // Cantidad actual
                BasicText(
                    text = "$cantidad",
                    style = type.title2.copy(
                        color = colors.ink,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                    ),
                    modifier = Modifier
                        .width(44.dp)
                        .padding(horizontal = 4.dp),
                )

                // Más (+)
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clickable(onClick = onMas),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(16.dp)) {
                        drawLine(
                            color = colors.ink,
                            start = Offset(2.dp.toPx(), size.height / 2),
                            end = Offset(size.width - 2.dp.toPx(), size.height / 2),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                        drawLine(
                            color = colors.ink,
                            start = Offset(size.width / 2, 2.dp.toPx()),
                            end = Offset(size.width / 2, size.height - 2.dp.toPx()),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Sección de chips de indicaciones rápidas (.chips.wrap + .chip).
 */
@Composable
private fun SeccionIndicacionesChips(
    indicaciones: List<String>,
    seleccionadas: Set<String>,
    onToggle: (String) -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicText(
            text = "Indicaciones",
            style = type.body.copy(
                color = colors.ink2,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
            ),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            indicaciones.forEach { chipText ->
                val activo = seleccionadas.contains(chipText)

                val chipBgAnim by animateColorAsState(
                    targetValue = if (activo) colors.ink else colors.surface,
                    animationSpec = tween(120),
                    label = "chipBg_$chipText",
                )
                val chipTextAnim by animateColorAsState(
                    targetValue = if (activo) colors.bg else colors.ink2,
                    animationSpec = tween(120),
                    label = "chipText_$chipText",
                )

                Box(
                    modifier = Modifier
                        .height(42.dp)
                        .clip(CircleShape)
                        .background(chipBgAnim)
                        .border(
                            width = 1.dp,
                            color = if (activo) colors.ink else colors.line,
                            shape = CircleShape,
                        )
                        .clickable { onToggle(chipText) }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = chipText,
                        style = type.body.copy(
                            color = chipTextAnim,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Campo de texto libre para otra indicación personalizada a cocina (.fl + .fx).
 */
@Composable
private fun CampoOtraIndicacion(
    valor: String,
    onCambio: (String) -> Unit,
    onTerminado: () -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        BasicText(
            text = "Otra indicación",
            style = type.caption.copy(
                color = colors.ink2,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            ),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .border(1.dp, colors.line, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (valor.isEmpty()) {
                BasicText(
                    text = "Escribe para cocina",
                    style = type.body.copy(
                        color = colors.ink3,
                        fontSize = 15.sp,
                    ),
                )
            }
            BasicTextField(
                value = valor,
                onValueChange = onCambio,
                singleLine = true,
                textStyle = type.body.copy(
                    color = colors.ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onTerminado() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Aviso informativo de comanda de cambio (.nt.nt-info).
 */
@Composable
private fun AvisoComandaCambio() {
    val colors = Rikos.colors
    val type = Rikos.type

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.primarySoft)
            .border(1.dp, colors.primary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Icono info
        Canvas(Modifier.size(20.dp)) {
            val r = size.width * 0.44f
            val c = Offset(size.width / 2, size.height / 2)
            drawCircle(colors.primary, radius = r, center = c, style = Stroke(width = 1.8.dp.toPx()))
            drawCircle(colors.primary, radius = 1.2.dp.toPx(), center = Offset(c.x, c.y - r * 0.4f))
            drawLine(
                color = colors.primary,
                start = Offset(c.x, c.y - r * 0.1f),
                end = Offset(c.x, c.y + r * 0.45f),
                strokeWidth = 1.8.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        BasicText(
            text = "Si la línea ya se envió, el cambio sale como una comanda nueva marcada «cambio».",
            style = type.body.copy(
                color = colors.ink,
                fontSize = 13.5.sp,
                lineHeight = 18.sp,
            ),
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Dock inferior fijo (.dock) con botón Guardar cambios y botón Anular línea.
 */
@Composable
private fun DockInferiorEditarLinea(
    onGuardar: () -> Unit,
    onAnular: () -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    val guardarInteraction = remember { MutableInteractionSource() }
    val guardarPressed by guardarInteraction.collectIsPressedAsState()
    val guardarScale by animateFloatAsState(
        targetValue = if (guardarPressed) 0.97f else 1f,
        animationSpec = tween(120),
        label = "guardarScale",
    )

    val anularInteraction = remember { MutableInteractionSource() }
    val anularPressed by anularInteraction.collectIsPressedAsState()
    val anularScale by animateFloatAsState(
        targetValue = if (anularPressed) 0.97f else 1f,
        animationSpec = tween(120),
        label = "anularScale",
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
        // Botón primario: Guardar cambios (.btn.pri.blk)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .graphicsLayer { scaleX = guardarScale; scaleY = guardarScale }
                .clip(RoundedCornerShape(14.dp))
                .background(colors.primary)
                .semantics { role = Role.Button }
                .clickable(
                    interactionSource = guardarInteraction,
                    indication = null,
                    role = Role.Button,
                    onClick = onGuardar,
                ),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = "Guardar cambios",
                style = type.body.copy(
                    color = colors.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                ),
            )
        }

        // Botón destructivo secundario: Anular línea (.btn.dang.blk)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .graphicsLayer { scaleX = anularScale; scaleY = anularScale }
                .clip(RoundedCornerShape(14.dp))
                .background(colors.redSoft)
                .semantics { role = Role.Button }
                .clickable(
                    interactionSource = anularInteraction,
                    indication = null,
                    role = Role.Button,
                    onClick = onAnular,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Icono tacho de basura
                Canvas(Modifier.size(18.dp)) {
                    val w = 1.8.dp.toPx()
                    // Tapa
                    drawLine(colors.red, Offset(size.width * 0.15f, size.height * 0.25f), Offset(size.width * 0.85f, size.height * 0.25f), strokeWidth = w, cap = StrokeCap.Round)
                    drawLine(colors.red, Offset(size.width * 0.35f, size.height * 0.15f), Offset(size.width * 0.65f, size.height * 0.15f), strokeWidth = w, cap = StrokeCap.Round)
                    // Cuerpo
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width * 0.22f, size.height * 0.25f)
                        lineTo(size.width * 0.28f, size.height * 0.88f)
                        lineTo(size.width * 0.72f, size.height * 0.88f)
                        lineTo(size.width * 0.78f, size.height * 0.25f)
                    }
                    drawPath(path, color = colors.red, style = Stroke(width = w, cap = StrokeCap.Round))
                }

                BasicText(
                    text = "Anular línea",
                    style = type.body.copy(
                        color = colors.red,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    ),
                )
            }
        }
    }
}

/**
 * Modal Bottom Sheet para elegir el motivo de anulación (.scrim + .sheet).
 */
@Composable
private fun ModalBottomSheetAnulacion(
    motivoSeleccionado: MotivoAnulacion?,
    onSeleccionarMotivo: (MotivoAnulacion) -> Unit,
    onConfirmarAnulacion: () -> Unit,
    onCerrar: () -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onCerrar),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = false) {} // Bloquear clicks al scrim
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(colors.surface)
                .border(1.dp, colors.line, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Grab handle (.grab)
            Box(
                modifier = Modifier
                    .size(width = 44.dp, height = 5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.line)
                    .align(Alignment.CenterHorizontally),
            )

            // Título H2
            BasicText(
                text = "¿Por qué se anula?",
                style = type.title2.copy(
                    color = colors.ink,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.02).sp,
                ),
            )

            // Subtítulo
            BasicText(
                text = "Elige un motivo. Queda en la bitácora con tu nombre.",
                style = type.body.copy(
                    color = colors.ink2,
                    fontSize = 14.sp,
                ),
            )

            Spacer(Modifier.height(4.dp))

            // Lista de opciones (.opt)
            MotivoAnulacion.entries.forEach { motivo ->
                val activo = motivo == motivoSeleccionado
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (activo) colors.primarySoft else colors.surface)
                        .border(
                            width = if (activo) 2.dp else 1.dp,
                            color = if (activo) colors.primary else colors.line,
                            shape = RoundedCornerShape(16.dp),
                        )
                        .clickable { onSeleccionarMotivo(motivo) }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Radio indicator
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (activo) 2.dp else 1.5.dp,
                                color = if (activo) colors.primary else colors.line,
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (activo) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(colors.primary),
                            )
                        }
                    }

                    // Texto motivo
                    BasicText(
                        text = motivo.texto,
                        style = type.body.copy(
                            color = colors.ink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Botón de confirmación
            if (motivoSeleccionado != null) {
                // Habilitado: Anular línea (.btn.dang-solid.blk)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.red)
                        .clickable(onClick = onConfirmarAnulacion),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "Anular línea",
                        style = type.body.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        ),
                    )
                }
            } else {
                // Deshabilitado: Elige un motivo (.btn.off.blk)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surface2),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "Elige un motivo",
                        style = type.body.copy(
                            color = colors.ink3,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        ),
                    )
                }
            }

            // Botón Volver (.btn.ghost.blk)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clickable(onClick = onCerrar),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "Volver",
                    style = type.body.copy(
                        color = colors.ink2,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    ),
                )
            }
        }
    }
}
