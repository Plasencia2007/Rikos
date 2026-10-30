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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.rikos.app.core.designsystem.theme.Rikos
import pe.rikos.app.core.designsystem.theme.RikosMotion
import pe.rikos.app.feature.mesas.presentation.components.MesasBottomNav
import pe.rikos.app.feature.mesas.presentation.components.NavItem

/**
 * M12b · Comandas por estado
 * Canvas ID: a223f2d1-644e-4a2f-b0db-8651916ae890
 *
 * Muestra el seguimiento en tiempo real de lo que cocina y barra van terminando,
 * organizado en 4 estados: Recibido, Preparando, Por entregar y Entregado.
 */

data class ComandaItem(
    val id: String,
    val mesa: String,
    val estacion: String, // "Cocina" o "Barra"
    val haceMinutos: Int,
    val nombre: String,
    val cantidad: Int,
    val indicacion: String,
    val nota: String?,
    val estadoIndex: Int, // 0: Recibido, 1: Preparando, 2: Por entregar, 3: Entregado
)

enum class FiltroEstacion(val titulo: String) {
    MIS_MESAS("Mis mesas"),
    TODAS("Todas"),
    COCINA("Cocina"),
    BARRA("Barra"),
}

@Composable
fun ComandasScreen(
    enLinea: Boolean = true,
    alIrAMesas: () -> Unit = {},
    alNuevoPedido: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    // Lista maestra de comandas basada exactamente en el canvas M12b
    var todasLasComandas by remember {
        mutableStateOf(
            listOf(
                ComandaItem("c-0", "12", "Cocina", 3, "Ají de gallina", 2, "Ají aparte", "sin cebolla", 2),
                ComandaItem("c-1", "12", "Barra", 1, "Chicha morada (jarra)", 1, "Sin indicaciones", null, 2),
                ComandaItem("c-2", "5", "Cocina", 11, "Lomo saltado", 1, "Término medio", "poco picante", 1),
                ComandaItem("c-3", "2", "Cocina", 2, "Menú del día", 3, "Sopa, ají de gallina", null, 0),
            )
        )
    }

    // Pestaña de estado activa (por defecto: 2 "Por entregar")
    var estadoTabActivo by remember { mutableStateOf(2) }

    // Filtro de estación activo
    var filtroActivo by remember { mutableStateOf(FiltroEstacion.MIS_MESAS) }

    // Historial base de entregados (+5 por diseño canvas)
    var entregadosHistoricosCount by remember { mutableStateOf(5) }

    // Cálculo dinámico de conteos para los 4 estados
    val nombresEstados = listOf("Recibido", "Preparando", "Por entregar", "Entregado")
    val conteosEstados = nombresEstados.mapIndexed { index, _ ->
        val countActual = todasLasComandas.count { it.estadoIndex == index }
        if (index == 3) countActual + entregadosHistoricosCount else countActual
    }

    // Comandas filtradas por la pestaña activa y el filtro de estación
    val comandasPestana = todasLasComandas.filter { it.estadoIndex == estadoTabActivo }
    val comandasFiltradas = when (filtroActivo) {
        FiltroEstacion.MIS_MESAS -> comandasPestana
        FiltroEstacion.TODAS -> comandasPestana
        FiltroEstacion.COCINA -> comandasPestana.filter { it.estacion.equals("Cocina", ignoreCase = true) }
        FiltroEstacion.BARRA -> comandasPestana.filter { it.estacion.equals("Barra", ignoreCase = true) }
    }

    // Agrupadas por número de mesa
    val gruposPorMesa = comandasFiltradas.groupBy { it.mesa }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp) // Espacio para MesasBottomNav
        ) {
            // 1. Cabecera (.hd)
            CabeceraComandas(enLinea = enLinea)

            // 2. Cuerpo con scroll (.body)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Selector segmentado de 4 estados (.seg)
                SegmentedTabsEstados(
                    nombres = nombresEstados,
                    conteos = conteosEstados,
                    seleccionadoIndex = estadoTabActivo,
                    onSelect = { estadoTabActivo = it },
                )

                // Chips de filtro (.chips)
                ChipsFiltroEstacion(
                    filtroActivo = filtroActivo,
                    onSelectFiltro = { filtroActivo = it },
                )

                // Lista de tarjetas de comanda por mesa (.kt2)
                if (gruposPorMesa.isEmpty()) {
                    EstadoVacioComandas()
                } else {
                    gruposPorMesa.forEach { (mesaNum, itemsMesa) ->
                        TarjetaComandaMesa(
                            mesaNumero = mesaNum,
                            items = itemsMesa,
                            estadoIndex = estadoTabActivo,
                            onEntregarMesa = {
                                // Mover los ítems de esta mesa al estado "Entregado" (3)
                                val idsMesa = itemsMesa.map { it.id }.toSet()
                                todasLasComandas = todasLasComandas.map {
                                    if (it.id in idsMesa) it.copy(estadoIndex = 3) else it
                                }
                            },
                        )
                    }
                }

                // Leyenda explicativa inferior (.xs .c3)
                BasicText(
                    text = "Cuando cocina marca un plato como listo, te llega el aviso y aparece aquí.",
                    style = type.footnote.copy(
                        color = colors.ink3,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                )
            }
        }

        // 3. Barra de navegación principal inferior flotante
        MesasBottomNav(
            itemSeleccionado = NavItem.COMANDAS,
            alSeleccionarItem = { item ->
                if (item == NavItem.MESAS) alIrAMesas()
            },
            alNuevoPedido = alNuevoPedido,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/**
 * Cabecera (.hd): Título Comandas + Subtítulo + Estado de conexión.
 */
@Composable
private fun CabeceraComandas(enLinea: Boolean) {
    val colors = Rikos.colors
    val type = Rikos.type

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            BasicText(
                text = "Comandas",
                style = type.title1.copy(
                    color = colors.ink,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 32.sp,
                ),
            )
            BasicText(
                text = "Lo que cocina y barra van terminando",
                style = type.subhead.copy(
                    color = colors.ink3,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }

        // Indicador de conexión (.conn)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 4.dp),
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
}

/**
 * Selector segmentado de 4 estados (.seg): Recibido | Preparando | Por entregar | Entregado.
 */
@Composable
private fun SegmentedTabsEstados(
    nombres: List<String>,
    conteos: List<Int>,
    seleccionadoIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(17.dp))
            .background(colors.surface)
            .border(1.dp, colors.line, RoundedCornerShape(17.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        nombres.forEachIndexed { index, nombre ->
            val isSelected = index == seleccionadoIndex
            val tabInteraction = remember { MutableInteractionSource() }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        if (isSelected) {
                            if (colors.isDark) colors.surface2 else colors.ink
                        } else Color.Transparent
                    )
                    .clickable(
                        interactionSource = tabInteraction,
                        indication = null,
                        role = Role.Tab,
                        onClick = { onSelect(index) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    BasicText(
                        text = nombre,
                        style = type.caption.copy(
                            color = if (isSelected) Color.White else colors.ink3,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                    BasicText(
                        text = "${conteos[index]}",
                        style = type.title3.copy(
                            color = if (isSelected) Color.White else colors.ink,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Filtros rápidos en fila horizontal (.chips): Mis mesas | Todas | Cocina | Barra.
 */
@Composable
private fun ChipsFiltroEstacion(
    filtroActivo: FiltroEstacion,
    onSelectFiltro: (FiltroEstacion) -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FiltroEstacion.entries.forEach { filtro ->
            val isSelected = filtro == filtroActivo
            val chipInteraction = remember { MutableInteractionSource() }

            Box(
                modifier = Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(
                        if (isSelected) {
                            if (colors.isDark) colors.surface2 else colors.ink
                        } else Color.Transparent
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (isSelected) {
                            if (colors.isDark) colors.surface2 else colors.ink
                        } else colors.line,
                        shape = RoundedCornerShape(19.dp),
                    )
                    .clickable(
                        interactionSource = chipInteraction,
                        indication = null,
                        role = Role.Button,
                        onClick = { onSelectFiltro(filtro) },
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = filtro.titulo,
                    style = type.footnote.copy(
                        color = if (isSelected) Color.White else colors.ink2,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

/**
 * Tarjeta contenedora de comanda agrupada por mesa (.kt2).
 */
@Composable
private fun TarjetaComandaMesa(
    mesaNumero: String,
    items: List<ComandaItem>,
    estadoIndex: Int,
    onEntregarMesa: () -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    val esListoParaEntregar = estadoIndex == 2
    val esEnEspera = estadoIndex == 0 || estadoIndex == 1
    val totalPlatos = items.sumOf { it.cantidad }
    val maxMinutos = items.maxOfOrNull { it.haceMinutos } ?: 1
    val esTardio = maxMinutos >= 10

    // Estaciones únicas presentes ("cocina y barra" o "cocina")
    val estaciones = items.map { it.estacion }.distinct().joinToString(" y ").lowercase()

    // Borde de realce verde si está listo (.kt2.ready)
    val bordeMod = if (esListoParaEntregar) {
        Modifier.border(2.dp, colors.green, RoundedCornerShape(22.dp))
    } else {
        Modifier.border(1.dp, colors.line, RoundedCornerShape(22.dp))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (esListoParaEntregar) 8.dp else 2.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = if (esListoParaEntregar) colors.green.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.05f),
            )
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surface)
            .then(bordeMod)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 1. Fila de cabecera de la mesa
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Avatar de mesa (44×44, rounded 14dp)
            val avatarBg: Color
            val avatarColor: Color

            when {
                esListoParaEntregar -> {
                    avatarBg = colors.green
                    avatarColor = Color.White
                }
                estadoIndex == 3 -> {
                    avatarBg = colors.surface2
                    avatarColor = colors.ink2
                }
                else -> {
                    avatarBg = colors.amberSoft
                    avatarColor = colors.amberText
                }
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(avatarBg),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = mesaNumero,
                    style = type.title2.copy(
                        color = avatarColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                )
            }

            // Título de mesa y resumen de platos
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                BasicText(
                    text = "Mesa $mesaNumero",
                    style = type.callout.copy(
                        color = colors.ink,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                )
                BasicText(
                    text = "$totalPlatos platos · $estaciones",
                    style = type.footnote.copy(
                        color = colors.ink3,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }

            // Badge de tiempo transcurrido (.kclk / .kclk.late)
            val timerBg = if (esTardio) colors.redSoft else colors.surface2
            val timerColor = if (esTardio) colors.red else colors.ink2

            Row(
                modifier = Modifier
                    .height(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(timerBg)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Canvas(modifier = Modifier.size(13.dp)) {
                    val r = size.minDimension / 2
                    val c = Offset(size.width / 2, size.height / 2)
                    drawCircle(color = timerColor, radius = r, style = Stroke(1.5.dp.toPx()))
                    val p = Path().apply {
                        moveTo(c.x, c.y - r * 0.55f)
                        lineTo(c.x, c.y)
                        lineTo(c.x + r * 0.45f, c.y + r * 0.2f)
                    }
                    drawPath(path = p, color = timerColor, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
                }
                BasicText(
                    text = "$maxMinutos min",
                    style = type.caption.copy(
                        color = timerColor,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                )
            }
        }

        // 2. Contenedor de ítems (.kitems)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface2)
        ) {
            items.forEachIndexed { index, item ->
                FilaPlatoComanda(item = item)
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

        // 3. Botón de acción si está listo: "Entregar en mesa X" (.btn.pri.blk)
        if (esListoParaEntregar) {
            val deliverInteraction = remember { MutableInteractionSource() }
            val isDeliverPressed by deliverInteraction.collectIsPressedAsState()
            val deliverScale by animateFloatAsState(
                targetValue = if (isDeliverPressed) 0.95f else 1f,
                animationSpec = tween(120),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .graphicsLayer {
                        scaleX = deliverScale
                        scaleY = deliverScale
                    }
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.green)
                    .clickable(
                        interactionSource = deliverInteraction,
                        indication = null,
                        role = Role.Button,
                        onClick = onEntregarMesa,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Canvas(modifier = Modifier.size(18.dp)) {
                        val p = Path().apply {
                            // Checkmark: M20 6L9 17l-5-5
                            moveTo(size.width * 0.18f, size.height * 0.52f)
                            lineTo(size.width * 0.42f, size.height * 0.76f)
                            lineTo(size.width * 0.85f, size.height * 0.25f)
                        }
                        drawPath(path = p, color = Color.White, style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                    BasicText(
                        text = "Entregar en mesa $mesaNumero",
                        style = type.callout.copy(
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                        ),
                    )
                }
            }
        }

        // 4. Barra de progreso si está en espera (.kbar)
        if (esEnEspera) {
            val porcentaje = if (estadoIndex == 1) 0.62f else 0.18f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.surface2)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(porcentaje)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(colors.primary)
                )
            }
        }
    }
}

/**
 * Fila individual de plato dentro de la tarjeta (.kit).
 */
@Composable
private fun FilaPlatoComanda(item: ComandaItem) {
    val colors = Rikos.colors
    val type = Rikos.type

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Badge de cantidad (.kq)
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surface),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = "${item.cantidad}",
                style = type.title3.copy(
                    color = colors.ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
            )
        }

        // Nombre, detalle y nota
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            BasicText(
                text = item.nombre,
                style = type.body.copy(
                    color = colors.ink,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
            )

            BasicText(
                text = item.indicacion.ifBlank { "Sin indicaciones" },
                style = type.footnote.copy(
                    color = colors.ink3,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                ),
            )

            if (!item.nota.isNullOrBlank()) {
                BasicText(
                    text = "Nota: ${item.nota}",
                    style = type.footnote.copy(
                        color = colors.amberText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        // Tag de estación (.kst)
        BasicText(
            text = item.estacion.uppercase(),
            style = type.caption.copy(
                color = colors.ink3,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.05.sp,
            ),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/**
 * Estado vacío cuando no hay comandas en la pestaña seleccionada (.none).
 */
@Composable
private fun EstadoVacioComandas() {
    val colors = Rikos.colors
    val type = Rikos.type

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Círculo ámbar grande con icono
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(colors.amberSoft),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(36.dp)) {
                val p = Path().apply {
                    // Check / Tray
                    moveTo(size.width * 0.2f, size.height * 0.5f)
                    lineTo(size.width * 0.45f, size.height * 0.75f)
                    lineTo(size.width * 0.8f, size.height * 0.25f)
                }
                drawPath(path = p, color = colors.amberText, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }

        BasicText(
            text = "Nada en esta lista",
            style = type.title2.copy(
                color = colors.ink,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
        )

        BasicText(
            text = "Cambia de pestaña para ver lo que sigue en cocina.",
            style = type.footnote.copy(
                color = colors.ink2,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
            ),
        )
    }
}
