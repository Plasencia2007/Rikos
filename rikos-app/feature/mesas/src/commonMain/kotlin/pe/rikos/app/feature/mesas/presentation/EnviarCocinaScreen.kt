package pe.rikos.app.feature.mesas.presentation

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pe.rikos.app.core.designsystem.theme.Rikos
import pe.rikos.app.core.designsystem.theme.RikosMotion

/**
 * M12 · Enviar a cocina
 * Canvas ID: 3f0a0bef-2429-4b7d-9e86-5017edaa52ed
 *
 * Pantalla de confirmación y despacho de platos a cocina con previsualización
 * del ticket térmico físico de comanda y estado de la impresora.
 */

data class ItemAEnviar(
    val id: String,
    val cantidad: Int,
    val nombre: String,
    val detalle: String,
    val destino: String, // "Cocina", "Postres", etc.
)

@Composable
fun EnviarCocinaScreen(
    mesaNumero: String = "5",
    comandaNumero: Int = 15,
    mozoNombre: String = "Luis",
    horaActual: String = "13:14",
    salonNombre: String = "salón principal",
    enLinea: Boolean = true,
    impresoraLista: Boolean = true,
    itemsIniciales: List<ItemAEnviar>? = null,
    alVolver: () -> Unit,
    alEnviarEImprimir: (itemsEnviados: List<ItemAEnviar>) -> Unit,
    alReimprimirAnterior: ((comandaId: Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type
    val coroutineScope = rememberCoroutineScope()

    // Datos basados exactamente en el lienzo M12
    val listaItems = remember {
        itemsIniciales ?: listOf(
            ItemAEnviar("it-0", 1, "Postre del día", "P1, sin azúcar", "Postres"),
            ItemAEnviar("it-1", 1, "Menú del día", "P2, ensalada, ají de gallina", "Cocina"),
        )
    }

    // Estado de selección individual por checkbox (por defecto todos seleccionados: on: { 0: true, 1: true })
    var seleccionadosIds by remember {
        mutableStateOf(listaItems.map { it.id }.toSet())
    }

    // Toast flotante al reimprimir comanda anterior
    var toastReimpresion by remember { mutableStateOf(false) }

    fun dispararReimpresion() {
        toastReimpresion = true
        alReimprimirAnterior?.invoke(comandaNumero - 1)
        coroutineScope.launch {
            delay(2800)
            toastReimpresion = false
        }
    }

    val itemsFiltrados = listaItems.filter { it.id in seleccionadosIds }
    val totalCount = itemsFiltrados.size

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Cabecera (.hd): Toolbar + Título A cocina + Subtítulo
            CabeceraEnviarCocina(
                mesaNumero = mesaNumero,
                comandaNumero = comandaNumero,
                enLinea = enLinea,
                alVolver = alVolver,
            )

            // 2. Cuerpo desplazable (.body)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Lista de platos a enviar en tarjeta (.card)
                TarjetaPlatosAEnviar(
                    items = listaItems,
                    seleccionadosIds = seleccionadosIds,
                    onToggle = { id ->
                        seleccionadosIds = if (id in seleccionadosIds) {
                            seleccionadosIds - id
                        } else {
                            seleccionadosIds + id
                        }
                    },
                )

                // Ticket físico térmico de cocina (.tk)
                TicketComandaCocina(
                    mesaNumero = mesaNumero,
                    comandaNumero = comandaNumero,
                    mozoNombre = mozoNombre,
                    horaActual = horaActual,
                    salonNombre = salonNombre,
                    items = itemsFiltrados,
                )

                // Estado de impresora de cocina
                FilaEstadoImpresora(impresoraLista = impresoraLista)

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 3. Dock inferior de acciones (.dock)
            DockEnviarCocina(
                count = totalCount,
                comandaAnterior = comandaNumero - 1,
                onEnviarEImprimir = {
                    if (itemsFiltrados.isNotEmpty()) {
                        alEnviarEImprimir(itemsFiltrados)
                    } else {
                        alVolver()
                    }
                },
                onReimprimir = { dispararReimpresion() },
            )
        }

        // Toast de confirmación de reimpresión
        AnimatedVisibility(
            visible = toastReimpresion,
            enter = slideInVertically(initialOffsetY = { -it }, animationSpec = tween(300)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(200)) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp, start = 14.dp, end = 14.dp)
        ) {
            ToastReimpresionComanda(comandaId = comandaNumero - 1)
        }
    }
}

/**
 * Cabecera (.hd) de la pantalla A cocina.
 */
@Composable
private fun CabeceraEnviarCocina(
    mesaNumero: String,
    comandaNumero: Int,
    enLinea: Boolean,
    alVolver: () -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type
    val backInteraction = remember { MutableInteractionSource() }
    val backPressed by backInteraction.collectIsPressedAsState()
    val backScale by animateFloatAsState(
        targetValue = if (backPressed) 0.92f else 1f,
        animationSpec = tween(120),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
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
                    .graphicsLayer {
                        scaleX = backScale
                        scaleY = backScale
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface2)
                    .border(1.dp, colors.line, RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = backInteraction,
                        indication = null,
                        role = Role.Button,
                        onClick = alVolver,
                    )
                    .semantics { contentDescription = "Volver al pedido" },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(20.dp)) {
                    val p = Path().apply {
                        // Chevron izquierdo: M15 6l-6 6 6 6
                        moveTo(size.width * 0.625f, size.height * 0.25f)
                        lineTo(size.width * 0.375f, size.height * 0.5f)
                        lineTo(size.width * 0.625f, size.height * 0.75f)
                    }
                    drawPath(
                        path = p,
                        color = colors.ink,
                        style = Stroke(
                            width = 2.2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Indicador de conexión (.conn)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(if (enLinea) Color(0xFF22C55E) else colors.ink3, CircleShape)
                )
                BasicText(
                    text = if (enLinea) "En línea" else "Sin conexión",
                    style = type.footnote.copy(
                        color = colors.ink2,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Título principal (.hd h1)
        BasicText(
            text = "A cocina",
            style = type.title1.copy(
                color = colors.ink,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 32.sp,
            ),
        )

        Spacer(modifier = Modifier.height(3.dp))

        // Subtítulo (.hd .sub)
        BasicText(
            text = "Mesa $mesaNumero, comanda $comandaNumero",
            style = type.subhead.copy(
                color = colors.ink3,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

/**
 * Tarjeta de platos a enviar (.card con filas .li).
 */
@Composable
private fun TarjetaPlatosAEnviar(
    items: List<ItemAEnviar>,
    seleccionadosIds: Set<String>,
    onToggle: (String) -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type
    val colorAji = colors.amberStrong

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .border(1.dp, colors.line, RoundedCornerShape(20.dp))
    ) {
        items.forEachIndexed { index, item ->
            val isChecked = item.id in seleccionadosIds
            val rowInteraction = remember { MutableInteractionSource() }
            val isPressed by rowInteraction.collectIsPressedAsState()
            val rowScale by animateFloatAsState(
                targetValue = if (isPressed) 0.985f else 1f,
                animationSpec = tween(100),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = rowScale
                        scaleY = rowScale
                    }
                    .clickable(
                        interactionSource = rowInteraction,
                        indication = null,
                        role = Role.Checkbox,
                        onClick = { onToggle(item.id) },
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Checkbox (.ck)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isChecked) colorAji else Color.Transparent)
                        .border(
                            width = 2.dp,
                            color = if (isChecked) colorAji else colors.ink3.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(9.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isChecked) {
                        Canvas(modifier = Modifier.size(16.dp)) {
                            val p = Path().apply {
                                moveTo(size.width * 0.18f, size.height * 0.52f)
                                lineTo(size.width * 0.42f, size.height * 0.76f)
                                lineTo(size.width * 0.85f, size.height * 0.25f)
                            }
                            drawPath(
                                path = p,
                                color = Color.White,
                                style = Stroke(
                                    width = 2.4.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round,
                                ),
                            )
                        }
                    }
                }

                // Nombre y detalles del plato
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    BasicText(
                        text = item.nombre,
                        style = type.body.copy(
                            color = colors.ink,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                        ),
                    )
                    BasicText(
                        text = item.detalle,
                        style = type.footnote.copy(
                            color = colors.ink3,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }

                // Tag de destino (.tag .t-mute)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface2)
                        .padding(horizontal = 9.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = item.destino,
                        style = type.caption.copy(
                            color = colors.ink2,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                        ),
                    )
                }
            }

            if (index < items.size - 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.line)
                )
            }
        }
    }
}

/**
 * Previsualización del ticket térmico físico de cocina (.tk).
 * Se presenta como un papel de ticket real (#FBF8F1 / #F1F5F9 con tinta oscura y corte punteado).
 */
@Composable
private fun TicketComandaCocina(
    mesaNumero: String,
    comandaNumero: Int,
    mozoNombre: String,
    horaActual: String,
    salonNombre: String,
    items: List<ItemAEnviar>,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    // Color térmico de papel físico
    val colorPapel = if (colors.isDark) Color(0xFFF1F5F9) else Color(0xFFFBF8F1)
    val colorTinta = Color(0xFF1E1A16)
    val colorTintaSuave = Color(0xFF5A4E42)
    val colorLineaPunteada = Color(0xFFB8AFA2)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .shadow(12.dp, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            .background(colorPapel)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 22.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Cabecera: Mesa y Comanda (.row.between)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    text = "Mesa $mesaNumero",
                    style = type.title2.copy(
                        color = colorTinta,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                )
                BasicText(
                    text = "Comanda $comandaNumero",
                    style = type.callout.copy(
                        color = colorTinta,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }

            // Subtítulo con mozo, hora y salón
            BasicText(
                text = "$mozoNombre, $horaActual, $salonNombre",
                style = type.footnote.copy(
                    color = colorTintaSuave,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )

            // Línea punteada de corte (.dash)
            LineaPunteadaTicket(color = colorLineaPunteada)

            // Líneas de comanda impresas
            if (items.isEmpty()) {
                BasicText(
                    text = "Ningún plato seleccionado para marchar",
                    style = type.footnote.copy(
                        color = colorTintaSuave,
                        fontSize = 13.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    ),
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            } else {
                items.forEach { plato ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        BasicText(
                            text = "${plato.cantidad} × ${plato.nombre}",
                            style = type.callout.copy(
                                color = colorTinta,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                            ),
                        )
                        BasicText(
                            text = plato.detalle,
                            style = type.footnote.copy(
                                color = colorTintaSuave,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                            ),
                        )
                    }
                }
            }

            // Segunda línea punteada (.dash)
            LineaPunteadaTicket(color = colorLineaPunteada)

            // Resumen de comanda
            BasicText(
                text = "${items.size} para cocina caliente",
                style = type.callout.copy(
                    color = colorTinta,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
            )
        }
    }
}

/**
 * Línea punteada que simula el corte térmico (.dash).
 */
@Composable
private fun LineaPunteadaTicket(color: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(7.dp)
            .padding(vertical = 2.dp)
    ) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f), 0f),
        )
    }
}

/**
 * Fila informativa de estado de impresora de cocina.
 */
@Composable
private fun FilaEstadoImpresora(impresoraLista: Boolean) {
    val colors = Rikos.colors
    val type = Rikos.type

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Icono de impresora
        Canvas(modifier = Modifier.size(18.dp)) {
            val p = Path().apply {
                // M6 9V2h12v7
                moveTo(size.width * 0.25f, size.height * 0.375f)
                lineTo(size.width * 0.25f, size.height * 0.08f)
                lineTo(size.width * 0.75f, size.height * 0.08f)
                lineTo(size.width * 0.75f, size.height * 0.375f)
                // M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2
                moveTo(size.width * 0.25f, size.height * 0.75f)
                lineTo(size.width * 0.16f, size.height * 0.75f)
                lineTo(size.width * 0.08f, size.height * 0.67f)
                lineTo(size.width * 0.08f, size.height * 0.46f)
                lineTo(size.width * 0.16f, size.height * 0.38f)
                lineTo(size.width * 0.84f, size.height * 0.38f)
                lineTo(size.width * 0.92f, size.height * 0.46f)
                lineTo(size.width * 0.92f, size.height * 0.67f)
                lineTo(size.width * 0.84f, size.height * 0.75f)
                lineTo(size.width * 0.75f, size.height * 0.75f)
                // M6 14h12v8H6z
                moveTo(size.width * 0.25f, size.height * 0.58f)
                lineTo(size.width * 0.75f, size.height * 0.58f)
                lineTo(size.width * 0.75f, size.height * 0.92f)
                lineTo(size.width * 0.25f, size.height * 0.92f)
                close()
            }
            drawPath(
                path = p,
                color = if (impresoraLista) colors.greenText else colors.amber,
                style = Stroke(
                    width = 1.8.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }

        BasicText(
            text = if (impresoraLista) "Impresora de cocina lista" else "Verificando impresora...",
            style = type.footnote.copy(
                color = colors.ink2,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

/**
 * Dock de acciones (.dock):
 * - Enviar {count} e imprimir (.btn.pri.blk)
 * - Reimprimir comanda 14 (.btn.ghost.blk)
 */
@Composable
private fun DockEnviarCocina(
    count: Int,
    comandaAnterior: Int,
    onEnviarEImprimir: () -> Unit,
    onReimprimir: () -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type
    val colorAji = colors.amberStrong

    val enviarInteraction = remember { MutableInteractionSource() }
    val isEnviarPressed by enviarInteraction.collectIsPressedAsState()
    val enviarScale by animateFloatAsState(
        targetValue = if (isEnviarPressed) 0.96f else 1f,
        animationSpec = tween(120),
    )

    val reimprimirInteraction = remember { MutableInteractionSource() }
    val isReimprimirPressed by reimprimirInteraction.collectIsPressedAsState()
    val reimprimirScale by animateFloatAsState(
        targetValue = if (isReimprimirPressed) 0.96f else 1f,
        animationSpec = tween(120),
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 1. Botón Enviar e imprimir (.btn.pri.blk)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .graphicsLayer {
                    scaleX = enviarScale
                    scaleY = enviarScale
                }
                .clip(RoundedCornerShape(14.dp))
                .background(colorAji)
                .clickable(
                    interactionSource = enviarInteraction,
                    indication = null,
                    role = Role.Button,
                    onClick = onEnviarEImprimir,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Icono avión de papel
                Canvas(modifier = Modifier.size(18.dp)) {
                    val p = Path().apply {
                        moveTo(size.width * 0.92f, size.height * 0.08f)
                        lineTo(size.width * 0.46f, size.height * 0.54f)
                        moveTo(size.width * 0.92f, size.height * 0.08f)
                        lineTo(size.width * 0.63f, size.height * 0.92f)
                        lineTo(size.width * 0.46f, size.height * 0.54f)
                        lineTo(size.width * 0.08f, size.height * 0.38f)
                        close()
                    }
                    drawPath(
                        path = p,
                        color = Color.White,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }

                BasicText(
                    text = "Enviar $count e imprimir",
                    style = type.callout.copy(
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                )
            }
        }

        // 2. Botón Reimprimir comanda anterior (.btn.ghost.blk)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .graphicsLayer {
                    scaleX = reimprimirScale
                    scaleY = reimprimirScale
                }
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Transparent)
                .clickable(
                    interactionSource = reimprimirInteraction,
                    indication = null,
                    role = Role.Button,
                    onClick = onReimprimir,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Icono impresora
                Canvas(modifier = Modifier.size(18.dp)) {
                    val p = Path().apply {
                        moveTo(size.width * 0.25f, size.height * 0.375f)
                        lineTo(size.width * 0.25f, size.height * 0.08f)
                        lineTo(size.width * 0.75f, size.height * 0.08f)
                        lineTo(size.width * 0.75f, size.height * 0.375f)

                        moveTo(size.width * 0.25f, size.height * 0.75f)
                        lineTo(size.width * 0.16f, size.height * 0.75f)
                        lineTo(size.width * 0.08f, size.height * 0.67f)
                        lineTo(size.width * 0.08f, size.height * 0.46f)
                        lineTo(size.width * 0.16f, size.height * 0.38f)
                        lineTo(size.width * 0.84f, size.height * 0.38f)
                        lineTo(size.width * 0.92f, size.height * 0.46f)
                        lineTo(size.width * 0.92f, size.height * 0.67f)
                        lineTo(size.width * 0.84f, size.height * 0.75f)
                        lineTo(size.width * 0.75f, size.height * 0.75f)

                        moveTo(size.width * 0.25f, size.height * 0.58f)
                        lineTo(size.width * 0.75f, size.height * 0.58f)
                        lineTo(size.width * 0.75f, size.height * 0.92f)
                        lineTo(size.width * 0.25f, size.height * 0.92f)
                        close()
                    }
                    drawPath(
                        path = p,
                        color = colors.ink2,
                        style = Stroke(
                            width = 1.8.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }

                BasicText(
                    text = "Reimprimir comanda $comandaAnterior",
                    style = type.callout.copy(
                        color = colors.ink2,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

/**
 * Toast flotante al reimprimir comanda anterior.
 */
@Composable
private fun ToastReimpresionComanda(comandaId: Int) {
    val colors = Rikos.colors
    val type = Rikos.type

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(colors.navy)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(colors.amberStrong, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(18.dp)) {
                val p = Path().apply {
                    moveTo(size.width * 0.25f, size.height * 0.375f)
                    lineTo(size.width * 0.25f, size.height * 0.08f)
                    lineTo(size.width * 0.75f, size.height * 0.08f)
                    lineTo(size.width * 0.75f, size.height * 0.375f)
                    moveTo(size.width * 0.25f, size.height * 0.75f)
                    lineTo(size.width * 0.16f, size.height * 0.75f)
                    lineTo(size.width * 0.08f, size.height * 0.67f)
                    lineTo(size.width * 0.08f, size.height * 0.46f)
                    lineTo(size.width * 0.16f, size.height * 0.38f)
                    lineTo(size.width * 0.84f, size.height * 0.38f)
                    lineTo(size.width * 0.92f, size.height * 0.46f)
                    lineTo(size.width * 0.92f, size.height * 0.67f)
                    lineTo(size.width * 0.84f, size.height * 0.75f)
                    lineTo(size.width * 0.75f, size.height * 0.75f)
                    moveTo(size.width * 0.25f, size.height * 0.58f)
                    lineTo(size.width * 0.75f, size.height * 0.58f)
                    lineTo(size.width * 0.75f, size.height * 0.92f)
                    lineTo(size.width * 0.25f, size.height * 0.92f)
                    close()
                }
                drawPath(
                    path = p,
                    color = Color.White,
                    style = Stroke(
                        width = 1.8.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            BasicText(
                text = "Reimprimiendo comanda $comandaId",
                style = type.subhead.copy(
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            BasicText(
                text = "Enviada a la impresora de cocina",
                style = type.footnote.copy(
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}
