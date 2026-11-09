package com.example.invernaderoiot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.invernaderoiot.ui.theme.InvernaderoIoTTheme
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

class MainActivity : ComponentActivity() {
    private lateinit var bluetoothHelper: BluetoothManagerHelper
    private lateinit var notificationHelper: NotificationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bluetoothHelper = BluetoothManagerHelper(this)
        notificationHelper = NotificationHelper(this)

        setContent {
            InvernaderoIoTTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // Estado global compartido entre Dashboard e Historial
                    var sesionIniciada by remember { mutableStateOf(false) }
                    var temperatura by remember { mutableFloatStateOf(24.5f) }
                    var humedad by remember { mutableFloatStateOf(60.0f) }

                    var ventiladorEncendido by remember { mutableStateOf(false) }
                    var alarmaSilenciada by remember { mutableStateOf(false) }

                    // Modo Automático y Umbrales Configurables
                    var modoAutomatico by remember { mutableStateOf(true) }
                    var umbralAlerta by remember { mutableFloatStateOf(32f) }
                    var umbralPeligro by remember { mutableFloatStateOf(40f) }
                    var ultimoNivelNotificado by remember { mutableStateOf(AlertLevel.NORMAL) }

                    // Conexión Bluetooth
                    var macAddress by remember { mutableStateOf("98:D3:31:00:00:00") }
                    var conexionEstablecida by remember { mutableStateOf(false) }

                    // Historial de lecturas y registro de eventos
                    val historialLecturas = remember { mutableStateListOf<SensorReading>() }
                    val historialEventos = remember { mutableStateListOf<SystemEvent>() }

                    fun registrarEvento(mensaje: String, nivel: AlertLevel = AlertLevel.NORMAL) {
                        historialEventos.add(
                            0,
                            SystemEvent(
                                timestamp = obtenerHoraActual(),
                                mensaje = mensaje,
                                nivel = nivel
                            )
                        )
                        if (historialEventos.size > 30) {
                            historialEventos.removeAt(historialEventos.lastIndex)
                        }
                    }

                    fun evaluarAutomatizacionYAlertas(tempActual: Float) {
                        // 1. Control automático de ventilación
                        if (modoAutomatico) {
                            if (tempActual >= umbralAlerta && !ventiladorEncendido) {
                                ventiladorEncendido = true
                                if (conexionEstablecida) bluetoothHelper.sendCommand("V")
                                registrarEvento(
                                    mensaje = String.format(
                                        Locale.getDefault(),
                                        "Auto: Ventilación ACTIVADA (%.1f°C >= %.0f°C)",
                                        tempActual,
                                        umbralAlerta
                                    ),
                                    nivel = AlertLevel.ALERTA
                                )
                            } else if (tempActual < (umbralAlerta - 1f) && ventiladorEncendido) {
                                ventiladorEncendido = false
                                if (conexionEstablecida) bluetoothHelper.sendCommand("A")
                                registrarEvento(
                                    mensaje = String.format(
                                        Locale.getDefault(),
                                        "Auto: Ventilación en espera (%.1f°C)",
                                        tempActual
                                    ),
                                    nivel = AlertLevel.NORMAL
                                )
                            }
                        }

                        // 2. notificaciones en cambios de nivel de alerta
                        val nivelActual = when {
                            tempActual >= umbralPeligro -> AlertLevel.PELIGRO
                            tempActual >= umbralAlerta -> AlertLevel.ALERTA
                            else -> AlertLevel.NORMAL
                        }

                        if (nivelActual != ultimoNivelNotificado) {
                            when (nivelActual) {
                                AlertLevel.PELIGRO -> {
                                    notificationHelper.sendTemperatureAlert(
                                        temperatura = tempActual,
                                        umbralSuperado = umbralPeligro,
                                        esPeligro = true,
                                        ventilacionAuto = modoAutomatico && ventiladorEncendido
                                    )
                                    registrarEvento(
                                        mensaje = String.format(
                                            Locale.getDefault(),
                                            "Peligro crítico detectado: %.1f°C",
                                            tempActual
                                        ),
                                        nivel = AlertLevel.PELIGRO
                                    )
                                }
                                AlertLevel.ALERTA -> {
                                    if (ultimoNivelNotificado == AlertLevel.NORMAL) {
                                        notificationHelper.sendTemperatureAlert(
                                            temperatura = tempActual,
                                            umbralSuperado = umbralAlerta,
                                            esPeligro = false,
                                            ventilacionAuto = modoAutomatico && ventiladorEncendido
                                        )
                                        registrarEvento(
                                            mensaje = String.format(
                                                Locale.getDefault(),
                                                "Alerta térmica detectada: %.1f°C",
                                                tempActual
                                            ),
                                            nivel = AlertLevel.ALERTA
                                        )
                                    }
                                }
                                AlertLevel.NORMAL -> {
                                    registrarEvento(
                                        mensaje = String.format(
                                            Locale.getDefault(),
                                            "Temperatura normalizada: %.1f°C",
                                            tempActual
                                        ),
                                        nivel = AlertLevel.NORMAL
                                    )
                                }
                            }
                            ultimoNivelNotificado = nivelActual
                        }
                    }

                    // Monitoreo continuo
                    LaunchedEffect(sesionIniciada) {
                        if (!sesionIniciada) return@LaunchedEffect

                        if (historialLecturas.isEmpty()) {
                            historialLecturas.add(
                                SensorReading(
                                    timestamp = obtenerHoraActual(),
                                    temperatura = temperatura,
                                    humedad = humedad
                                )
                            )
                            registrarEvento("Monitoreo iniciado en Modo Automático")
                        }

                        while (true) {
                            delay(3.seconds)
                            temperatura = (temperatura + (Math.random() * 4.5 - 1.8).toFloat())
                                .coerceIn(15f, 52f)
                            humedad = (humedad + (Math.random() * 5.0 - 2.5).toFloat())
                                .coerceIn(0f, 100f)

                            if (historialLecturas.size >= 25) {
                                historialLecturas.removeAt(0)
                            }
                            historialLecturas.add(
                                SensorReading(
                                    timestamp = obtenerHoraActual(),
                                    temperatura = temperatura,
                                    humedad = humedad
                                )
                            )

                            evaluarAutomatizacionYAlertas(temperatura)
                        }
                    }

                    NavHost(navController = navController, startDestination = "login") {
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = {
                                    sesionIniciada = true
                                    navController.navigate("dashboard") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("dashboard") {
                            InvernaderoScreen(
                                temperatura = temperatura,
                                humedad = humedad,
                                ventiladorEncendido = ventiladorEncendido,
                                alarmaSilenciada = alarmaSilenciada,
                                modoAutomatico = modoAutomatico,
                                umbralAlerta = umbralAlerta,
                                umbralPeligro = umbralPeligro,
                                macAddress = macAddress,
                                conexionEstablecida = conexionEstablecida,
                                onMacChange = { macAddress = it },
                                onConnectDevice = {
                                    conexionEstablecida = bluetoothHelper.connectToDevice(macAddress)
                                    if (conexionEstablecida) {
                                        registrarEvento("Conectado al módulo BT ($macAddress)")
                                    }
                                },
                                onDisconnectDevice = {
                                    bluetoothHelper.disconnect()
                                    conexionEstablecida = false
                                    registrarEvento("Módulo BT desconectado")
                                },
                                onModoAutomaticoChange = { activado ->
                                    modoAutomatico = activado
                                    registrarEvento(
                                        if (activado) "Modo Automático activado" else "Modo Manual activado"
                                    )
                                    if (activado) evaluarAutomatizacionYAlertas(temperatura)
                                },
                                onUmbralAlertaChange = { nuevoAlerta ->
                                    umbralAlerta = nuevoAlerta
                                    if (umbralPeligro < nuevoAlerta + 2f) {
                                        umbralPeligro = (nuevoAlerta + 2f).coerceAtMost(50f)
                                    }
                                    evaluarAutomatizacionYAlertas(temperatura)
                                },
                                onUmbralPeligroChange = { nuevoPeligro ->
                                    umbralPeligro = nuevoPeligro.coerceAtLeast(umbralAlerta + 2f)
                                    evaluarAutomatizacionYAlertas(temperatura)
                                },
                                onVentiladorClick = {
                                    ventiladorEncendido = !ventiladorEncendido
                                    if (conexionEstablecida) {
                                        bluetoothHelper.sendCommand(if (ventiladorEncendido) "V" else "A")
                                    }
                                    registrarEvento(
                                        if (ventiladorEncendido) {
                                            "Manual: Ventilación ACTIVADA"
                                        } else {
                                            "Manual: Ventilación APAGADA"
                                        }
                                    )
                                },
                                onAlarmaClick = {
                                    alarmaSilenciada = !alarmaSilenciada
                                    if (conexionEstablecida) {
                                        bluetoothHelper.sendCommand(if (alarmaSilenciada) "M" else "E")
                                    }
                                    registrarEvento(
                                        if (alarmaSilenciada) {
                                            "Sirena silenciada manualmente"
                                        } else {
                                            "Sirena reactivada"
                                        }
                                    )
                                },
                                onNavigateToHistory = { navController.navigate("history") }
                            )
                        }
                        composable("history") {
                            HistoryScreen(
                                historialLecturas = historialLecturas,
                                historialEventos = historialEventos,
                                umbralAlerta = umbralAlerta,
                                umbralPeligro = umbralPeligro,
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothHelper.disconnect()
    }
}
