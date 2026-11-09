package com.example.invernaderoiot

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun InvernaderoScreen(
    temperatura: Float,
    humedad: Float,
    ventiladorEncendido: Boolean,
    alarmaSilenciada: Boolean,
    modoAutomatico: Boolean,
    umbralAlerta: Float,
    umbralPeligro: Float,
    macAddress: String,
    conexionEstablecida: Boolean,
    onMacChange: (String) -> Unit,
    onConnectDevice: () -> Unit,
    onDisconnectDevice: () -> Unit,
    onModoAutomaticoChange: (Boolean) -> Unit,
    onUmbralAlertaChange: (Float) -> Unit,
    onUmbralPeligroChange: (Float) -> Unit,
    onVentiladorClick: () -> Unit,
    onAlarmaClick: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    var permisosBtOtorgados by remember { mutableStateOf(false) }

    val permisosBluetooth = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        permisosBtOtorgados = permisosBluetooth.all { permisos[it] == true }
    }

    LaunchedEffect(Unit) {
        val listaPermisos = buildList {
            addAll(permisosBluetooth)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
        permissionLauncher.launch(listaPermisos)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Panel de Monitoreo",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        ConnectionSectionAnimated(
            macAddress = macAddress,
            isConnected = conexionEstablecida,
            onMacChange = onMacChange,
            onConnectClick = {
                if (permisosBtOtorgados) onConnectDevice()
            },
            onDisconnectClick = onDisconnectDevice
        )

        Spacer(modifier = Modifier.height(20.dp))

        SensorCardAnimated(
            temperatura = temperatura,
            humedad = humedad,
            umbralAlerta = umbralAlerta,
            umbralPeligro = umbralPeligro,
            onHistoryClick = onNavigateToHistory
        )

        Spacer(modifier = Modifier.height(20.dp))

        AutomationSectionCard(
            modoAutomatico = modoAutomatico,
            umbralAlerta = umbralAlerta,
            umbralPeligro = umbralPeligro,
            onModoAutomaticoChange = onModoAutomaticoChange,
            onUmbralAlertaChange = onUmbralAlertaChange,
            onUmbralPeligroChange = onUmbralPeligroChange
        )

        Spacer(modifier = Modifier.height(20.dp))

        ControlSectionAnimated(
            ventiladorEncendido = ventiladorEncendido,
            alarmaSilenciada = alarmaSilenciada,
            modoAutomatico = modoAutomatico,
            isConnected = conexionEstablecida,
            onVentiladorClick = onVentiladorClick,
            onAlarmaClick = onAlarmaClick
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ConnectionSectionAnimated(
    macAddress: String,
    isConnected: Boolean,
    onMacChange: (String) -> Unit,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit
) {
    val cardColor by animateColorAsState(
        targetValue = if (isConnected) Color(0xFFE8F5E9) else Color(0xFFF3F4F6),
        animationSpec = tween(500),
        label = "connectionCardColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isConnected) Color(0xFF4CAF50) else Color.Gray,
                    modifier = Modifier.size(12.dp)
                ) {}
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isConnected) "Conectado al Invernadero" else "Módulo Desconectado",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = macAddress,
                onValueChange = onMacChange,
                label = { Text("MAC HC-05") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isConnected,
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = if (isConnected) onDisconnectClick else onConnectClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) Color(0xFFE53935) else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isConnected) "Finalizar Conexión" else "Vincular Dispositivo")
            }
        }
    }
}

@Composable
fun SensorCardAnimated(
    temperatura: Float,
    humedad: Float,
    umbralAlerta: Float,
    umbralPeligro: Float,
    onHistoryClick: () -> Unit
) {
    val animatedTemp by animateFloatAsState(
        targetValue = temperatura,
        animationSpec = tween(800),
        label = "animatedTemp"
    )
    val animatedHum by animateFloatAsState(
        targetValue = humedad,
        animationSpec = tween(800),
        label = "animatedHum"
    )

    val estadoColorTarget = when {
        animatedTemp >= umbralPeligro -> Color(0xFFE53935)
        animatedTemp >= umbralAlerta -> Color(0xFFFFA000)
        else -> Color(0xFF1E88E5)
    }
    val tempColor by animateColorAsState(
        targetValue = estadoColorTarget,
        animationSpec = tween(500),
        label = "tempColor"
    )

    val etiquetaEstado = when {
        temperatura >= umbralPeligro -> "PELIGRO CRÍTICO"
        temperatura >= umbralAlerta -> "ALERTA TÉRMICA"
        else -> "ESTADO ÓPTIMO"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Lecturas en Tiempo Real", fontWeight = FontWeight.SemiBold, color = Color.Gray)
                Surface(
                    color = tempColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = etiquetaEstado,
                        color = tempColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f°C", animatedTemp),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = tempColor
                    )
                    Text("Temperatura", color = Color.Gray)
                }
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { (animatedHum / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.size(80.dp),
                        color = Color(0xFF43A047),
                        strokeWidth = 6.dp,
                        trackColor = Color(0xFFE8F5E9)
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%.0f%%", animatedHum),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            TextButton(
                onClick = onHistoryClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = "Ver Gráficos",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ver Historial, Estadísticas y CSV", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AutomationSectionCard(
    modoAutomatico: Boolean,
    umbralAlerta: Float,
    umbralPeligro: Float,
    onModoAutomaticoChange: (Boolean) -> Unit,
    onUmbralAlertaChange: (Float) -> Unit,
    onUmbralPeligroChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (modoAutomatico) "Modo Automático" else "Modo Manual",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (modoAutomatico) {
                                "Ventilación controlada por umbral"
                            } else {
                                "Control directo por botones"
                            },
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
                Switch(
                    checked = modoAutomatico,
                    onCheckedChange = onModoAutomaticoChange
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(12.dp))

            // Umbral de Alerta y Ventilación Automática
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Umbral Ventilación / Alerta", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = "${umbralAlerta.roundToInt()}°C",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFA000)
                )
            }
            Slider(
                value = umbralAlerta,
                onValueChange = { nuevoValor ->
                    onUmbralAlertaChange(nuevoValor.roundToInt().toFloat())
                },
                valueRange = 20f..42f,
                steps = 21,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFFA000),
                    activeTrackColor = Color(0xFFFFA000)
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Umbral de Peligro Crítico
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Umbral de Peligro Crítico", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = "${umbralPeligro.roundToInt()}°C",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE53935)
                )
            }
            Slider(
                value = umbralPeligro,
                onValueChange = { nuevoValor ->
                    onUmbralPeligroChange(nuevoValor.roundToInt().toFloat())
                },
                valueRange = 25f..50f,
                steps = 24,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFE53935),
                    activeTrackColor = Color(0xFFE53935)
                )
            )
        }
    }
}

@Composable
fun ControlSectionAnimated(
    ventiladorEncendido: Boolean,
    alarmaSilenciada: Boolean,
    modoAutomatico: Boolean,
    isConnected: Boolean,
    onVentiladorClick: () -> Unit,
    onAlarmaClick: () -> Unit
) {
    val alpha = if (isConnected) 1f else 0.75f

    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
        Text("Controles del Sistema", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onVentiladorClick,
            enabled = !modoAutomatico,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (ventiladorEncendido) {
                    Color(0xFF4CAF50).copy(alpha = alpha)
                } else {
                    Color.DarkGray.copy(alpha = alpha)
                },
                disabledContainerColor = if (ventiladorEncendido) {
                    Color(0xFF4CAF50).copy(alpha = 0.65f)
                } else {
                    Color.LightGray
                },
                disabledContentColor = Color.White
            )
        ) {
            val textoVentilador = when {
                modoAutomatico && ventiladorEncendido -> "Ventilación (Auto): ACTIVADA"
                modoAutomatico && !ventiladorEncendido -> "Ventilación (Auto): EN ESPERA"
                ventiladorEncendido -> "Ventilación: ACTIVADA"
                else -> "Ventilación: APAGADA"
            }
            Text(textoVentilador, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        if (modoAutomatico) {
            Text(
                text = "Cambia a Modo Manual arriba para accionar la ventilación manualmente.",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onAlarmaClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (alarmaSilenciada) {
                    Color.Gray.copy(alpha = alpha)
                } else {
                    Color(0xFFE53935).copy(alpha = alpha)
                }
            )
        ) {
            Text(
                text = if (alarmaSilenciada) "Alarma en Silencio" else "Sirena Activa",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
