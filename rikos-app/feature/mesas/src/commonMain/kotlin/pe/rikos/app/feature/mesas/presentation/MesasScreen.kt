package pe.rikos.app.feature.mesas.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.rikos.app.core.designsystem.theme.Rikos
import pe.rikos.app.core.designsystem.theme.RikosMotion
import pe.rikos.app.core.designsystem.theme.RikosTheme
import pe.rikos.app.feature.mesas.domain.AvisoMesa
import pe.rikos.app.feature.mesas.domain.EstadoMesa
import pe.rikos.app.feature.mesas.domain.FormaMesa
import pe.rikos.app.feature.mesas.domain.Mesa
import pe.rikos.app.feature.mesas.domain.Salon
import pe.rikos.app.feature.mesas.domain.TipoAviso
import pe.rikos.app.feature.mesas.presentation.components.DrawnTable
import pe.rikos.app.feature.mesas.presentation.components.MesasBottomNav
import pe.rikos.app.feature.mesas.presentation.components.VistazoMesaSheet
import pe.rikos.app.feature.mesas.presentation.components.mesaColores

@Composable
fun MesasScreen(
    viewModel: MesasViewModel,
    alElegirMesa: (Mesa) -> Unit,
    modifier: Modifier = Modifier,
) {
    val estado by viewModel.estado.collectAsState()

    RikosTheme(oscuro = estado.modoNoche) {
        val colors = Rikos.colors
        val type = Rikos.type

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colors.bg),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                // 1. Cabecera (Header) con tipografía Plus Jakarta Sans
                MesasHeader(
                    mozoNombre = estado.mozoNombre,
                    turno = estado.turnoNombre,
                    enLinea = estado.enLinea,
                    alVerAvisos = { viewModel.alMostrarAvisos(true) },
                    alAlternarModoNoche = viewModel::alternarModoNoche,
                )

                // 2. Chips de Salones (Horizontal scroll con animación táctil)
                SalonesChipsRow(
                    salones = estado.salones,
                    salonActivoId = estado.salonActivoId,
                    alSeleccionarSalon = viewModel::alSeleccionarSalon,
                )

                Spacer(Modifier.height(12.dp))

                // 3. Avisos «Te necesitan» (Horizontal scroll con feedback táctil)
                if (estado.avisos.isNotEmpty()) {
                    AvisosUrgentRow(
                        avisos = estado.avisos,
                        alTocarAviso = { viewModel.alMostrarAvisos(true) },
                    )
                    Spacer(Modifier.height(12.dp))
                }

                // 4. Barra de resumen (Leyenda)
                LeyendaBarra(
                    libres = estado.libresCount,
                    ocupadas = estado.ocupadasCount,
                    porCobrar = estado.porCobrarCount,
                )

                Spacer(Modifier.height(12.dp))

                // 5. Grilla de mesas dibujadas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    if (estado.cargando && estado.mesas.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            BasicText(
                                text = "Cargando salón…",
                                style = type.body.copy(color = colors.ink3, fontWeight = FontWeight.SemiBold),
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(
                                items = estado.mesasFiltradas,
                                key = { it.id },
                                span = { mesa ->
                                    if (mesa.forma == FormaMesa.UNIDA) GridItemSpan(2) else GridItemSpan(1)
                                },
                            ) { mesa ->
                                MesaCardItem(
                                    mesa = mesa,
                                    onClick = {
                                        if (mesa.estado == EstadoMesa.OCUPADA || mesa.estado == EstadoMesa.LISTO || mesa.estado == EstadoMesa.POR_COBRAR) {
                                            viewModel.alAbrirVistazo(mesa)
                                        } else {
                                            alElegirMesa(mesa)
                                        }
                                    },
                                    onLongClick = {
                                        viewModel.alAbrirVistazo(mesa)
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // 6. Barra de navegación inferior flotante con animación viva
            MesasBottomNav(
                modifier = Modifier.align(Alignment.BottomCenter),
                alNuevoPedido = { /* TODO: Nuevo Pedido (M04 / M08) */ },
            )

            // M03c: Vistazo de mesa (BottomSheet emergente al mantener pulsada una mesa ocupada)
            VistazoMesaSheet(
                mesa = estado.mesaVistazo,
                visible = estado.mesaVistazo != null,
                alCerrar = viewModel::alCerrarVistazo,
                alAbrirPedido = { mesa ->
                    viewModel.alCerrarVistazo()
                    alElegirMesa(mesa)
                },
                alVerCuenta = { /* Cuenta (M15) */ },
                alCambiarMesa = { /* Cambiar mesa (M05) */ },
                alUnirMesas = { /* Unir mesas (M06) */ },
                alPasarMozo = { /* Transferir mesa (M07) */ },
            )

            // M03d: Pantalla completa de avisos en tiempo real
            AnimatedVisibility(
                visible = estado.mostrarAvisos,
                enter = fadeIn(tween(220, easing = RikosMotion.Ease)),
                exit = fadeOut(tween(180, easing = RikosMotion.Ease)),
            ) {
                AvisosScreen(
                    enLinea = estado.enLinea,
                    alVolver = { viewModel.alMostrarAvisos(false) },
                )
            }
        }
    }
}

@Composable
private fun MesasHeader(
    mozoNombre: String,
    turno: String,
    enLinea: Boolean,
    alVerAvisos: () -> Unit,
    alAlternarModoNoche: () -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type
    val bellInteraction = remember { MutableInteractionSource() }
    val bellPressed by bellInteraction.collectIsPressedAsState()
    val bellScale by animateFloatAsState(
        targetValue = if (bellPressed) 0.92f else 1f,
        animationSpec = RikosMotion.press(),
        label = "bellPress",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = alAlternarModoNoche,
            ),
        ) {
            BasicText(
                text = "Mesas",
                style = type.title1.copy(
                    color = colors.ink,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.02).sp,
                ),
            )
            BasicText(
                text = "Hola, $mozoNombre. $turno",
                style = type.subhead.copy(
                    color = colors.ink3,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Pill "En línea"
            Row(
                modifier = Modifier
                    .height(34.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.line, RoundedCornerShape(17.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (enLinea) Color(0xFF10B981) else Color(0xFFEF4444), CircleShape),
                )
                BasicText(
                    text = if (enLinea) "En línea" else "Sin conexión",
                    style = type.caption.copy(
                        color = if (enLinea) colors.greenText else Color(0xFFDC2626),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }

            val dotTransition = rememberInfiniteTransition(label = "bellDotTransition")
            val dotScale by dotTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.35f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "bellDotScale",
            )
            val ringExpand by dotTransition.animateFloat(
                initialValue = 1f,
                targetValue = 2.4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1600, easing = LinearOutSlowInEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "bellRingExpand",
            )
            val ringAlpha by dotTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1600, easing = LinearOutSlowInEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "bellRingAlpha",
            )

            // Botón de avisos / campanita (.ib con animación táctil)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .graphicsLayer { scaleX = bellScale; scaleY = bellScale }
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.line, RoundedCornerShape(14.dp))
                    .clickable(
                        interactionSource = bellInteraction,
                        indication = null,
                        onClick = alVerAvisos,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(22.dp)) {
                    val color = colors.ink
                    val sx = size.width / 24f
                    val sy = size.height / 24f
                    val p = Path().apply {
                        moveTo(18f * sx, 8f * sy)
                        cubicTo(18f * sx, 4.7f * sy, 15.3f * sx, 2f * sy, 12f * sx, 2f * sy)
                        cubicTo(8.7f * sx, 2f * sy, 6f * sx, 4.7f * sy, 6f * sx, 8f * sy)
                        cubicTo(6f * sx, 15f * sy, 3f * sy, 17f * sy, 3f * sx, 17f * sy)
                        lineTo(21f * sx, 17f * sy)
                        cubicTo(21f * sx, 17f * sy, 18f * sx, 15f * sy, 18f * sx, 8f * sy)
                    }
                    drawPath(p, color, style = Stroke(width = 2f * sx, cap = StrokeCap.Round))
                    drawLine(color, Offset(10f * sx, 20f * sy), Offset(14f * sx, 20f * sy), strokeWidth = 2f * sx, cap = StrokeCap.Round)
                }

                // Punto naranja de notificación (.dot con rk-dot + anillo de pulso)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 7.dp, end = 7.dp)
                        .size(10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    // Anillo expansivo exterior
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = ringExpand
                                scaleY = ringExpand
                                alpha = ringAlpha
                            }
                            .border(1.dp, Color(0xFFFF8A5C), CircleShape),
                    )
                    // Punto coral central pulsante (.dot con rk-dot)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = dotScale
                                scaleY = dotScale
                            }
                            .background(Color(0xFFFF8A5C), CircleShape)
                            .border(2.dp, colors.surface, CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
private fun SalonesChipsRow(
    salones: List<Salon>,
    salonActivoId: String,
    alSeleccionarSalon: (String) -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (salon in salones) {
            val activo = salon.id == salonActivoId
            val fondo = when {
                activo && colors.isDark -> colors.ink
                activo -> colors.navy
                else -> colors.surface
            }
            val textoColor = when {
                activo && colors.isDark -> colors.bg
                activo -> Color.White
                else -> colors.ink
            }
            val badgeFondo = when {
                activo && colors.isDark -> colors.primary.copy(alpha = 0.16f)
                activo -> Color(0xFF0B2A5B)
                else -> colors.surface2
            }
            val badgeTexto = when {
                activo && colors.isDark -> colors.primary
                activo -> Color(0xFF93C5FD)
                else -> colors.ink3
            }

            val interaction = remember { MutableInteractionSource() }
            val isPressed by interaction.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.96f else 1f,
                animationSpec = RikosMotion.press(),
                label = "chipPress_${salon.id}",
            )

            Row(
                modifier = Modifier
                    .height(40.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(RoundedCornerShape(20.dp))
                    .background(fondo)
                    .border(1.dp, if (activo) Color.Transparent else colors.line, RoundedCornerShape(20.dp))
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = { alSeleccionarSalon(salon.id) },
                    )
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BasicText(
                    text = salon.nombre,
                    style = type.subhead.copy(
                        color = textoColor,
                        fontSize = 14.sp,
                        fontWeight = if (activo) FontWeight.Bold else FontWeight.SemiBold,
                    ),
                )
                Box(
                    modifier = Modifier
                        .height(22.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(badgeFondo)
                        .padding(horizontal = 7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = salon.totalMesas.toString(),
                        style = type.caption.copy(
                            color = badgeTexto,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun AvisosUrgentRow(
    avisos: List<AvisoMesa>,
    alTocarAviso: (AvisoMesa) -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    // Animación de pulso expansivo verde para la primera tarjeta (.need .nd:first-child: rk-ring 1.8s ease-out infinite)
    val ringTransition = rememberInfiniteTransition(label = "urgentRingTransition")
    val ringSpread by ringTransition.animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1800
                0f at 0 using FastOutSlowInEasing
                12f at 1260
                12f at 1800
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "urgentRingSpread",
    )
    val ringAlpha by ringTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1800
                0.45f at 0 using FastOutSlowInEasing
                0f at 1260
                0f at 1800
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "urgentRingAlpha",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        for ((index, aviso) in avisos.withIndex()) {
            val esPrimero = index == 0 && aviso.tipo == TipoAviso.PLATOS_LISTOS
            val badgeFondo = when (aviso.tipo) {
                TipoAviso.PLATOS_LISTOS -> colors.green
                TipoAviso.PIDIO_CUENTA -> Color(0xFFFF8A5C)
                TipoAviso.TIEMPO_SIN_PASAR -> colors.amberStrong
            }

            val interaction = remember { MutableInteractionSource() }
            val isPressed by interaction.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.97f else 1f,
                animationSpec = RikosMotion.press(),
                label = "avisoPress_${aviso.id}",
            )

            Row(
                modifier = Modifier
                    .width(224.dp)
                    .height(64.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .then(
                        if (esPrimero && ringAlpha > 0.01f) {
                            Modifier.drawBehind {
                                val sPx = ringSpread.dp.toPx()
                                drawRoundRect(
                                    color = colors.green.copy(alpha = ringAlpha),
                                    topLeft = Offset(-sPx, -sPx),
                                    size = Size(size.width + sPx * 2f, size.height + sPx * 2f),
                                    cornerRadius = CornerRadius(20.dp.toPx() + sPx),
                                )
                            }
                        } else Modifier
                    )
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(20.dp), ambientColor = Color(0x0C0F172A))
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.line, RoundedCornerShape(20.dp))
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = { alTocarAviso(aviso) },
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Avatar redondo con número de mesa
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(badgeFondo),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = aviso.mesaNumero,
                        style = type.headline.copy(
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                        ),
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    BasicText(
                        text = aviso.titulo,
                        style = type.subhead.copy(
                            color = colors.ink,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    BasicText(
                        text = aviso.subtitulo,
                        style = type.caption.copy(
                            color = colors.ink3,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                        ),
                        modifier = Modifier.padding(top = 1.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LeyendaBarra(
    libres: Int,
    ocupadas: Int,
    porCobrar: Int,
) {
    val colors = Rikos.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LeyendaItem(colors.green, "$libres libres")
        LeyendaItem(colors.amber, "$ocupadas ocupadas")
        LeyendaItem(colors.blue, "$porCobrar por cobrar")
    }
}

@Composable
private fun LeyendaItem(color: Color, texto: String) {
    val colors = Rikos.colors
    val type = Rikos.type
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape),
        )
        BasicText(
            text = texto,
            style = type.caption.copy(
                color = colors.ink3,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MesaCardItem(
    mesa: Mesa,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type
    val colores = mesaColores(mesa)
    val esOtroMozo = mesa.mozoNombre != null
    val esSeleccionada = mesa.seleccionada
    val tieneRing = mesa.comidaLista

    // Animación de interacción al presionar tarjeta (.mc:active { transform: scale(0.97); })
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = RikosMotion.press(),
        label = "cardPress_${mesa.id}",
    )

    // Animación de anillo pulsante verde medida exactamente del prototipo (@keyframes rk-ring, 1800ms)
    val ringTransition = rememberInfiniteTransition(label = "mesaRing")
    val ringProgress = if (tieneRing) {
        ringTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1800
                    0f at 0 using RikosMotion.RingEase
                    0.7f at 1260 using RikosMotion.RingEase
                    1f at 1800 using RikosMotion.RingEase
                },
                repeatMode = RepeatMode.Restart,
            ),
            label = "ringExpansion",
        ).value
    } else 0f

    val ringBorderAlpha = if (tieneRing) (1f - ringProgress).coerceIn(0f, 0.7f) else 0f
    val ringExtraSize = if (tieneRing) ringProgress * 8f else 0f

    val bordeMod = when {
        esSeleccionada -> Modifier.border(2.dp, colors.blue, RoundedCornerShape(20.dp))
        tieneRing -> Modifier.border(2.dp, colors.green, RoundedCornerShape(20.dp))
        else -> Modifier.border(1.dp, colors.line, RoundedCornerShape(20.dp))
    }

    val cardFondo = if (mesa.estado == EstadoMesa.FUERA_DE_SERVICIO) colors.surface2 else colors.surface
    val cardAlpha = if (esOtroMozo) 0.55f else 1f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .alpha(cardAlpha)
            .then(
                if (tieneRing) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            color = colors.green.copy(alpha = ringBorderAlpha),
                            topLeft = Offset(-ringExtraSize, -ringExtraSize),
                            size = androidx.compose.ui.geometry.Size(
                                size.width + ringExtraSize * 2f,
                                size.height + ringExtraSize * 2f,
                            ),
                            cornerRadius = CornerRadius(20.dp.toPx() + ringExtraSize),
                            style = Stroke(width = 2.dp.toPx()),
                        )
                    }
                } else Modifier,
            )
            .shadow(
                elevation = if (esSeleccionada) 4.dp else 1.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color(0x0C0F172A),
            )
            .clip(RoundedCornerShape(20.dp))
            .background(cardFondo)
            .then(bordeMod)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Mesa dibujada con sillas
        DrawnTable(mesa = mesa, colores = colores)

        // Fila: "Mesa X" + Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = if (mesa.forma == FormaMesa.UNIDA) "Mesas ${mesa.numero}" else "Mesa ${mesa.numero}",
                style = type.callout.copy(
                    color = colors.ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )

            // Status Badge Pill
            val badgeLabel = when {
                mesa.comidaLista -> "Comida lista"
                esOtroMozo -> if (mesa.mozoNombre.isNotEmpty()) mesa.mozoNombre else "De otro"
                mesa.forma == FormaMesa.UNIDA -> "Unidas, una cuenta"
                mesa.estado == EstadoMesa.LIBRE -> "Libre"
                mesa.estado == EstadoMesa.OCUPADA -> "Ocupada"
                mesa.estado == EstadoMesa.POR_COBRAR -> "Por cobrar"
                mesa.estado == EstadoMesa.FUERA_DE_SERVICIO -> "Fuera de servicio"
                mesa.estado == EstadoMesa.LISTO -> "Comida lista"
                else -> "Libre"
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(colores.badgeFondo)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = badgeLabel,
                    style = type.caption.copy(
                        color = colores.badgeTexto,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }

        // Subtítulo con detalle
        val detalleTexto = when {
            mesa.estado == EstadoMesa.LIBRE -> "Toca para abrirla"
            mesa.estado == EstadoMesa.FUERA_DE_SERVICIO -> "Mantén para activarla"
            mesa.forma == FormaMesa.UNIDA -> "${mesa.total.formatear()}, ${mesa.comensales} personas, ${mesa.minutosTranscurridos ?: 0} min, principal: mesa ${mesa.mesaPrincipalNumero ?: "3"}"
            mesa.mozoNombre != null -> "${mesa.total.formatear()}, ${mesa.comensales} pers."
            mesa.comensales > 0 && mesa.minutosTranscurridos != null -> "${mesa.total.formatear()}, ${mesa.comensales} pers., ${mesa.minutosTranscurridos} min"
            mesa.comensales > 0 -> "${mesa.total.formatear()}, ${mesa.comensales} pers."
            else -> mesa.total.formatear()
        }

        BasicText(
            text = detalleTexto,
            style = type.caption.copy(
                color = colors.ink3,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}
