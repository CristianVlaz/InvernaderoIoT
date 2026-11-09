package com.example.invernaderoiot

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.roundToInt

private enum class MetricType {
    TEMPERATURA,
    HUMEDAD
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    historialLecturas: List<SensorReading>,
    historialEventos: List<SystemEvent>,
    umbralAlerta: Float,
    umbralPeligro: Float,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var metricaSeleccionada by remember { mutableStateOf(MetricType.TEMPERATURA) }

    val valoresActuales = remember(historialLecturas, metricaSeleccionada) {
        if (metricaSeleccionada == MetricType.TEMPERATURA) {
            historialLecturas.map { it.temperatura }
        } else {
            historialLecturas.map { it.humedad }
        }
    }

    val minVal = valoresActuales.minOrNull() ?: 0f
    val maxVal = valoresActuales.maxOrNull() ?: 0f
    val avgVal = if (valoresActuales.isNotEmpty()) valoresActuales.average().toFloat() else 0f
    val unidad = if (metricaSeleccionada == MetricType.TEMPERATURA) "°C" else "%"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial y Estadísticas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            compartirHistorialCsv(context, historialLecturas, umbralAlerta, umbralPeligro)
                        },
                        enabled = historialLecturas.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Exportar CSV")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Selector Temperatura / Humedad
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = metricaSeleccionada == MetricType.TEMPERATURA,
                    onClick = { metricaSeleccionada = MetricType.TEMPERATURA },
                    label = { Text("Temperatura (°C)", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = metricaSeleccionada == MetricType.HUMEDAD,
                    onClick = { metricaSeleccionada = MetricType.HUMEDAD },
                    label = { Text("Humedad (%)", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    titulo = "Mínima",
                    valor = if (valoresActuales.isEmpty()) "--" else String.format(Locale.getDefault(), "%.1f%s", minVal, unidad),
                    colorValor = Color(0xFF1E88E5),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    titulo = "Promedio",
                    valor = if (valoresActuales.isEmpty()) "--" else String.format(Locale.getDefault(), "%.1f%s", avgVal, unidad),
                    colorValor = Color(0xFF43A047),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    titulo = "Máxima",
                    valor = if (valoresActuales.isEmpty()) "--" else String.format(Locale.getDefault(), "%.1f%s", maxVal, unidad),
                    colorValor = Color(0xFFE53935),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (metricaSeleccionada == MetricType.TEMPERATURA) {
                    "Curva de Temperatura en Tiempo Real"
                } else {
                    "Curva de Humedad Relativa"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (valoresActuales.isEmpty()) {
                            Text(
                                text = "Esperando datos...",
                                modifier = Modifier.align(Alignment.Center),
                                color = Color.Gray
                            )
                        } else {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val maxEscala = if (metricaSeleccionada == MetricType.TEMPERATURA) 55f else 100f
                                val widthPerPoint = if (valoresActuales.size > 1) {
                                    size.width / (valoresActuales.size - 1)
                                } else {
                                    0f
                                }
                                val heightRatio = size.height / maxEscala
                                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)

                                if (metricaSeleccionada == MetricType.TEMPERATURA) {
                                    // umbral de alerta
                                    val yAlerta = (size.height - (umbralAlerta * heightRatio)).coerceIn(0f, size.height)
                                    drawLine(
                                        color = Color(0xFFFFA000).copy(alpha = 0.6f),
                                        start = Offset(0f, yAlerta),
                                        end = Offset(size.width, yAlerta),
                                        strokeWidth = 3f,
                                        pathEffect = dashEffect
                                    )

                                    // umbral de peligro
                                    val yPeligro = (size.height - (umbralPeligro * heightRatio)).coerceIn(0f, size.height)
                                    drawLine(
                                        color = Color.Red.copy(alpha = 0.6f),
                                        start = Offset(0f, yPeligro),
                                        end = Offset(size.width, yPeligro),
                                        strokeWidth = 3f,
                                        pathEffect = dashEffect
                                    )
                                } else {
                                    listOf(25f, 50f, 75f).forEach { guia ->
                                        val yGuia = size.height - (guia * heightRatio)
                                        drawLine(
                                            color = Color.LightGray.copy(alpha = 0.5f),
                                            start = Offset(0f, yGuia),
                                            end = Offset(size.width, yGuia),
                                            strokeWidth = 2f,
                                            pathEffect = dashEffect
                                        )
                                    }
                                }

                                val path = Path()
                                val colorLinea = if (metricaSeleccionada == MetricType.TEMPERATURA) {
                                    Color(0xFF1E88E5)
                                } else {
                                    Color(0xFF00897B)
                                }

                                valoresActuales.forEachIndexed { index, valor ->
                                    val x = index * widthPerPoint
                                    val y = (size.height - (valor * heightRatio)).coerceIn(0f, size.height)

                                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)

                                    val colorPunto = if (metricaSeleccionada == MetricType.TEMPERATURA) {
                                        when {
                                            valor >= umbralPeligro -> Color.Red
                                            valor >= umbralAlerta -> Color(0xFFFFA000)
                                            else -> Color(0xFF4CAF50)
                                        }
                                    } else {
                                        when {
                                            valor < 30f -> Color(0xFFFFA000)
                                            valor > 80f -> Color(0xFF1E88E5)
                                            else -> Color(0xFF43A047)
                                        }
                                    }
                                    drawCircle(color = colorPunto, radius = 10f, center = Offset(x, y))
                                }

                                if (valoresActuales.size > 1) {
                                    drawPath(path = path, color = colorLinea, style = Stroke(width = 5f))
                                }
                            }
                        }
                    }

                    if (historialLecturas.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = historialLecturas.first().timestamp,
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "${historialLecturas.size} muestras",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = historialLecturas.last().timestamp,
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (metricaSeleccionada == MetricType.TEMPERATURA) {
                    LegendItem(Color(0xFF4CAF50), "Normal")
                    LegendItem(Color(0xFFFFA000), "Alerta (>=${umbralAlerta.roundToInt()}°)")
                    LegendItem(Color.Red, "Peligro (>=${umbralPeligro.roundToInt()}°)")
                } else {
                    LegendItem(Color(0xFFFFA000), "Seco (<30%)")
                    LegendItem(Color(0xFF43A047), "Óptima (30-80%)")
                    LegendItem(Color(0xFF1E88E5), "Alta (>80%)")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedButton(
                onClick = {
                    compartirHistorialCsv(context, historialLecturas, umbralAlerta, umbralPeligro)
                },
                enabled = historialLecturas.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Exportar / Compartir Datos en CSV", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sección de Registro de Eventos
            Text(
                text = "Registro de Eventos del Sistema",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (historialEventos.isEmpty()) {
                        Text(
                            text = "Sin eventos registrados todavía.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    } else {
                        historialEventos.forEachIndexed { index, evento ->
                            val colorIndicador = when (evento.nivel) {
                                AlertLevel.PELIGRO -> Color(0xFFE53935)
                                AlertLevel.ALERTA -> Color(0xFFFFA000)
                                AlertLevel.NORMAL -> Color(0xFF4CAF50)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(colorIndicador, RoundedCornerShape(50))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = evento.timestamp,
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = evento.mensaje,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (index < historialEventos.lastIndex) {
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.35f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatCard(
    titulo: String,
    valor: String,
    colorValor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = titulo, fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = valor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colorValor
            )
        }
    }
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(50))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, fontSize = 12.sp, color = Color.Gray)
    }
}

private fun compartirHistorialCsv(
    context: Context,
    lecturas: List<SensorReading>,
    umbralAlerta: Float,
    umbralPeligro: Float
) {
    val csvBuilder = StringBuilder()
    csvBuilder.appendLine("Hora,Temperatura_C,Humedad_Porcentaje,Estado")

    lecturas.forEach { lectura ->
        val estado = when {
            lectura.temperatura >= umbralPeligro -> "PELIGRO"
            lectura.temperatura >= umbralAlerta -> "ALERTA"
            else -> "NORMAL"
        }
        csvBuilder.appendLine(
            String.format(
                Locale.US,
                "%s,%.2f,%.2f,%s",
                lectura.timestamp,
                lectura.temperatura,
                lectura.humedad,
                estado
            )
        )
    }

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_SUBJECT, "Historial Invernadero IoT")
        putExtra(Intent.EXTRA_TEXT, csvBuilder.toString())
    }
    context.startActivity(Intent.createChooser(sendIntent, "Exportar historial CSV"))
}
