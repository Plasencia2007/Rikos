package pe.rikos.app.feature.mesas.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.rikos.app.core.common.Dinero
import pe.rikos.app.core.designsystem.theme.Rikos
import pe.rikos.app.core.designsystem.theme.RikosMotion
import pe.rikos.app.feature.mesas.domain.ExtraMenu
import pe.rikos.app.feature.mesas.domain.MenuDiaConfig
import pe.rikos.app.feature.mesas.domain.OpcionMenu
import pe.rikos.app.feature.mesas.domain.PedidoMenuDia

/**
 * M09 · Menú del día y extras.
 * Configuración táctil de las opciones del menú: entrada, segundo, refresco,
 * extras opcionales con precio incremental, notas de cocina y selector de cantidad.
 */
@Composable
fun MenuDiaScreen(
    mesaNumero: String,
    asientoEtiqueta: String,
    enLinea: Boolean = true,
    config: MenuDiaConfig = remember { MenuDiaConfig() },
    alVolver: () -> Unit,
    alConfirmarMenu: (PedidoMenuDia) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    // Estado de selección inicial
    var entradaSeleccionada by remember {
        mutableStateOf(config.entradas.firstOrNull() ?: OpcionMenu("ent-0", "Sopa de verduras"))
    }
    var segundoSeleccionado by remember {
        mutableStateOf(config.segundos.firstOrNull { !it.estaAgotado } ?: OpcionMenu("seg-0", "Lomo saltado"))
    }
    var refrescoSeleccionado by remember {
        mutableStateOf(config.refrescos.firstOrNull() ?: OpcionMenu("ref-0", "Chicha morada"))
    }

    // Extras seleccionados (por defecto el primero 'Huevo frito' está seleccionado según el prototipo)
    val extrasSeleccionados = remember {
        mutableStateListOf<ExtraMenu>().apply {
            config.extras.firstOrNull()?.let { add(it) }
        }
    }

    // Notas rápidas activas (por defecto 'Poco picante' está activa según el prototipo)
    val notasActivas = remember {
        mutableStateListOf<String>().apply {
            if (config.notasRapidas.size > 1) {
                add(config.notasRapidas[1]) // "Poco picante"
            }
        }
    }

    // Cantidad del stepper (por defecto 1)
    var cantidad by remember { mutableIntStateOf(1) }

    // Cálculo del precio unitario y total reactivo
    val precioExtraTotal = remember(extrasSeleccionados.size) {
        extrasSeleccionados.fold(Dinero.CERO) { acc, extra -> acc + extra.precio }
    }
    val precioUnitario = config.precioBase + precioExtraTotal
    val totalMonto = precioUnitario * cantidad

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp),
        ) {
            // 1. Cabecera (.hd .tbar)
            CabeceraMenuDia(
                mesaNumero = mesaNumero,
                asientoEtiqueta = asientoEtiqueta,
                enLinea = enLinea,
                precioBase = config.precioBase,
                alVolver = alVolver,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )

            Spacer(Modifier.height(4.dp))

            // 2. Grupo: Entrada (.gh + .gl)
            GrupoSeleccionMenu(
                titulo = "Entrada",
                etiquetaBadge = "Elige 1",
                esBadgeOpcional = false,
                opciones = config.entradas,
                opcionSeleccionada = entradaSeleccionada,
                alSeleccionarOpcion = { entradaSeleccionada = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )

            Spacer(Modifier.height(6.dp))

            // 3. Grupo: Segundo (.gh + .gl)
            GrupoSeleccionMenu(
                titulo = "Segundo",
                etiquetaBadge = "Elige 1",
                esBadgeOpcional = false,
                opciones = config.segundos,
                opcionSeleccionada = segundoSeleccionado,
                alSeleccionarOpcion = { segundoSeleccionado = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )

            Spacer(Modifier.height(6.dp))

            // 4. Grupo: Refresco (.gh + .gl)
            GrupoSeleccionMenu(
                titulo = "Refresco",
                etiquetaBadge = "Elige 1",
                esBadgeOpcional = false,
                opciones = config.refrescos,
                opcionSeleccionada = refrescoSeleccionado,
                alSeleccionarOpcion = { refrescoSeleccionado = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )

            Spacer(Modifier.height(6.dp))

            // 5. Grupo: Extras (.gh + .gl con checkboxes)
            GrupoExtrasMenu(
                titulo = "Extras",
                etiquetaBadge = "Opcional",
                extrasDisponibles = config.extras,
                extrasSeleccionados = extrasSeleccionados,
                alAlternarExtra = { extra ->
                    if (extrasSeleccionados.any { it.id == extra.id }) {
                        extrasSeleccionados.removeAll { it.id == extra.id }
                    } else {
                        extrasSeleccionados.add(extra)
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )

            Spacer(Modifier.height(6.dp))

            // 6. Grupo: Nota para cocina (.chips)
            GrupoNotasCocina(
                notasDisponibles = config.notasRapidas,
                notasActivas = notasActivas,
                alAlternarNota = { nota ->
                    if (notasActivas.contains(nota)) {
                        notasActivas.remove(nota)
                    } else {
                        notasActivas.add(nota)
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }

        // 7. Dock inferior fijo (.dock) con Stepper y Botón Agregar
        DockInferiorMenu(
            cantidad = cantidad,
            totalMonto = totalMonto,
            alDisminuir = { if (cantidad > 1) cantidad-- },
            alAumentar = { cantidad++ },
            alAgregar = {
                val pedido = PedidoMenuDia(
                    entrada = entradaSeleccionada,
                    segundo = segundoSeleccionado,
                    refresco = refrescoSeleccionado,
                    extras = extrasSeleccionados.toList(),
                    notas = notasActivas.toList(),
                    cantidad = cantidad,
                )
                alConfirmarMenu(pedido)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(colors.surface.copy(alpha = 0.96f))
                .border(width = 1.dp, color = colors.line)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        )
    }
}

/**
 * Cabecera con botón volver, avatar distintivo del menú del día y subtítulo contextual.
 */
@Composable
private fun CabeceraMenuDia(
    mesaNumero: String,
    asientoEtiqueta: String,
    enLinea: Boolean,
    precioBase: Dinero,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Fila izquierda: Botón Volver + Avatar + Títulos
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                // Botón volver '<'
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .border(1.dp, colors.line, CircleShape)
                        .clickable(role = Role.Button, onClick = alVolver),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(20.dp)) {
                        val sx = size.width / 24f
                        val sy = size.height / 24f
                        val p = Path().apply {
                            moveTo(15f * sx, 6f * sy)
                            lineTo(9f * sx, 12f * sy)
                            lineTo(15f * sx, 18f * sy)
                        }
                        drawPath(p, color = colors.ink, style = Stroke(width = 2.4f * sx, cap = StrokeCap.Round))
                    }
                }

                // Avatar distintivo del menú (44x44 dp, radius 14 dp, amberSoft)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.amberSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(24.dp)) {
                        val sx = size.width / 24f
                        val sy = size.height / 24f

                        // Campana / plato de menú
                        val cloche = Path().apply {
                            moveTo(4f * sx, 16f * sy)
                            cubicTo(4f * sx, 10f * sy, 8f * sx, 7f * sy, 12f * sx, 7f * sy)
                            cubicTo(16f * sx, 7f * sy, 20f * sx, 10f * sy, 20f * sx, 16f * sy)
                            close()
                        }
                        drawPath(cloche, color = colors.amberText, style = Stroke(width = 2f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round))

                        // Asa superior
                        drawCircle(
                            color = colors.amberText,
                            radius = 2f * sx,
                            center = Offset(12f * sx, 6f * sy),
                            style = Stroke(width = 1.8f * sx),
                        )

                        // Plato base horizontal
                        drawLine(
                            color = colors.amberText,
                            start = Offset(2.5f * sx, 18.5f * sy),
                            end = Offset(21.5f * sx, 18.5f * sy),
                            strokeWidth = 2.2f * sx,
                            cap = StrokeCap.Round,
                        )
                    }
                }

                // Títulos
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    BasicText(
                        text = "Menú del día",
                        style = type.title3.copy(
                            color = colors.ink,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    BasicText(
                        text = "${precioBase.formatear()}, para $asientoEtiqueta",
                        style = type.subhead.copy(
                            color = colors.ink3,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Normal,
                        ),
                    )
                }
            }

            // Indicador de conexión (.conn)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (enLinea) colors.greenSoft else colors.surface2)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (enLinea) Color(0xFF10B981) else colors.ink3),
                )
                BasicText(
                    text = if (enLinea) "En línea" else "Sin conexión",
                    style = type.caption.copy(
                        color = if (enLinea) colors.greenText else colors.ink3,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

/**
 * Grupo de selección de opción única (Entrada, Segundo, Refresco) con radio button.
 */
@Composable
private fun GrupoSeleccionMenu(
    titulo: String,
    etiquetaBadge: String,
    esBadgeOpcional: Boolean,
    opciones: List<OpcionMenu>,
    opcionSeleccionada: OpcionMenu,
    alSeleccionarOpcion: (OpcionMenu) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Encabezado del grupo (.gh)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = titulo,
                style = type.body.copy(
                    color = colors.ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
            )

            // Píldora (.pl)
            Box(
                modifier = Modifier
                    .height(22.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (esBadgeOpcional) colors.surface2 else colors.amberSoft)
                    .padding(horizontal = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = etiquetaBadge,
                    style = type.caption.copy(
                        color = if (esBadgeOpcional) colors.ink3 else colors.amberText,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }

        // Contenedor de lista (.gl)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(colors.surface)
                .border(1.dp, colors.line, RoundedCornerShape(18.dp)),
        ) {
            Column {
                opciones.forEachIndexed { index, opcion ->
                    val esUltimo = index == opciones.lastIndex
                    val estaSeleccionado = opcion.id == opcionSeleccionada.id
                    val estaDeshabilitado = opcion.estaAgotado

                    FilaOpcionMenu(
                        opcion = opcion,
                        estaSeleccionado = estaSeleccionado,
                        estaDeshabilitado = estaDeshabilitado,
                        onClick = {
                            if (!estaDeshabilitado) {
                                alSeleccionarOpcion(opcion)
                            }
                        },
                    )

                    if (!esUltimo) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(colors.line),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Fila individual con radio button (.rr).
 */
@Composable
private fun FilaOpcionMenu(
    opcion: OpcionMenu,
    estaSeleccionado: Boolean,
    estaDeshabilitado: Boolean,
    onClick: () -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !estaDeshabilitado) 0.985f else 1f,
        animationSpec = tween(durationMillis = 100, easing = RikosMotion.Ease),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !estaDeshabilitado,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            BasicText(
                text = opcion.nombre,
                style = type.body.copy(
                    color = if (estaDeshabilitado) colors.ink3 else colors.ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (estaDeshabilitado) TextDecoration.LineThrough else TextDecoration.None,
                ),
            )

            if (!opcion.subtitulo.isNullOrBlank()) {
                val esAlertaStock = opcion.subtitulo.contains("Quedan", ignoreCase = true)
                BasicText(
                    text = opcion.subtitulo,
                    style = type.caption.copy(
                        color = when {
                            estaDeshabilitado -> colors.ink3
                            esAlertaStock -> Color(0xFFFF6B4A)
                            else -> colors.ink3
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        }

        // Radio button (.rr)
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (estaSeleccionado) colors.primary else Color.Transparent)
                .border(
                    width = 2.dp,
                    color = if (estaSeleccionado) colors.primary else colors.line,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (estaSeleccionado) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                )
            }
        }
    }
}

/**
 * Grupo de extras con selección múltiple (checkboxes .ck y precios + S/ X.XX).
 */
@Composable
private fun GrupoExtrasMenu(
    titulo: String,
    etiquetaBadge: String,
    extrasDisponibles: List<ExtraMenu>,
    extrasSeleccionados: List<ExtraMenu>,
    alAlternarExtra: (ExtraMenu) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Encabezado del grupo (.gh)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = titulo,
                style = type.body.copy(
                    color = colors.ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
            )

            // Píldora opcional (.pl.po)
            Box(
                modifier = Modifier
                    .height(22.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(colors.surface2)
                    .padding(horizontal = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = etiquetaBadge,
                    style = type.caption.copy(
                        color = colors.ink3,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }

        // Contenedor de lista (.gl)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(colors.surface)
                .border(1.dp, colors.line, RoundedCornerShape(18.dp)),
        ) {
            Column {
                extrasDisponibles.forEachIndexed { index, extra ->
                    val esUltimo = index == extrasDisponibles.lastIndex
                    val estaMarcado = extrasSeleccionados.any { it.id == extra.id }

                    FilaExtraMenu(
                        extra = extra,
                        estaMarcado = estaMarcado,
                        onClick = { alAlternarExtra(extra) },
                    )

                    if (!esUltimo) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(colors.line),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Fila de extra individual con checkbox (.ck) y precio adicional.
 */
@Composable
private fun FilaExtraMenu(
    extra: ExtraMenu,
    estaMarcado: Boolean,
    onClick: () -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = tween(durationMillis = 100, easing = RikosMotion.Ease),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = extra.nombre,
            style = type.body.copy(
                color = colors.ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            modifier = Modifier.weight(1f),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Precio adicional (+ S/ 2.00)
            BasicText(
                text = "+ ${extra.precio.formatear()}",
                style = type.subhead.copy(
                    color = colors.ink3,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )

            // Checkbox (.ck)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (estaMarcado) colors.primary else Color.Transparent)
                    .border(
                        width = 2.dp,
                        color = if (estaMarcado) colors.primary else colors.line,
                        shape = RoundedCornerShape(7.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (estaMarcado) {
                    Canvas(Modifier.size(13.dp)) {
                        val sx = size.width / 13f
                        val sy = size.height / 13f
                        val p = Path().apply {
                            moveTo(2.5f * sx, 6.5f * sy)
                            lineTo(5.5f * sx, 9.5f * sy)
                            lineTo(10.5f * sx, 3.5f * sy)
                        }
                        drawPath(
                            p,
                            color = Color.White,
                            style = Stroke(width = 2f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Grupo de notas de cocina (.chips wrap).
 */
@Composable
private fun GrupoNotasCocina(
    notasDisponibles: List<String>,
    notasActivas: List<String>,
    alAlternarNota: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BasicText(
            text = "Nota para cocina",
            style = type.body.copy(
                color = colors.ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        // Chips horizontales con scroll fluido
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            notasDisponibles.forEach { nota ->
                val activa = notasActivas.contains(nota)
                ChipNotaCocina(
                    texto = nota,
                    activa = activa,
                    onClick = { alAlternarNota(nota) },
                )
            }
        }
    }
}

/**
 * Chip individual de nota de cocina (.chip).
 */
@Composable
private fun ChipNotaCocina(
    texto: String,
    activa: Boolean,
    onClick: () -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = tween(durationMillis = 100, easing = RikosMotion.Ease),
    )

    Box(
        modifier = Modifier
            .height(40.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(20.dp))
            .background(if (activa) colors.navy else colors.surface)
            .border(
                width = 1.dp,
                color = if (activa) colors.navy else colors.line,
                shape = RoundedCornerShape(20.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = texto,
            style = type.subhead.copy(
                color = if (activa) Color.White else colors.ink,
                fontSize = 13.5.sp,
                fontWeight = if (activa) FontWeight.Bold else FontWeight.SemiBold,
            ),
        )
    }
}

/**
 * Dock inferior fijo (.dock) con stepper de cantidad y botón Agregar · S/ XX.XX.
 */
@Composable
private fun DockInferiorMenu(
    cantidad: Int,
    totalMonto: Dinero,
    alDisminuir: () -> Unit,
    alAumentar: () -> Unit,
    alAgregar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Stepper de cantidad (.stp)
        Row(
            modifier = Modifier
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(colors.surface)
                .border(1.5.dp, colors.line, RoundedCornerShape(28.dp))
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Botón '-'
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(
                        enabled = cantidad > 1,
                        role = Role.Button,
                        onClick = alDisminuir,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(16.dp)) {
                    val strokeColor = if (cantidad > 1) colors.ink else colors.ink3.copy(alpha = 0.4f)
                    drawLine(
                        color = strokeColor,
                        start = Offset(2.dp.toPx(), size.height / 2),
                        end = Offset(size.width - 2.dp.toPx(), size.height / 2),
                        strokeWidth = 2.4.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }

            // Indicador de cantidad (font-size 22sp, ExtraBold)
            Box(
                modifier = Modifier.width(36.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = cantidad.toString(),
                    style = type.title3.copy(
                        color = colors.ink,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                )
            }

            // Botón '+' con fondo primario destacado (.btn +)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.primary)
                    .clickable(
                        role = Role.Button,
                        onClick = alAumentar,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(16.dp)) {
                    val stroke = 2.4.dp.toPx()
                    // Línea horizontal
                    drawLine(
                        color = Color.White,
                        start = Offset(2.dp.toPx(), size.height / 2),
                        end = Offset(size.width - 2.dp.toPx(), size.height / 2),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                    // Línea vertical
                    drawLine(
                        color = Color.White,
                        start = Offset(size.width / 2, 2.dp.toPx()),
                        end = Offset(size.width / 2, size.height - 2.dp.toPx()),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }

        // Botón principal Agregar (.btn.pri.grow)
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.97f else 1f,
            animationSpec = tween(durationMillis = 100, easing = RikosMotion.Ease),
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .shadow(elevation = 3.dp, shape = RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(colors.primary)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = alAgregar,
                ),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = "Agregar · ${totalMonto.formatear()}",
                style = type.headline.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                ),
            )
        }
    }
}
