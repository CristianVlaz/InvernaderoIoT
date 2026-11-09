package com.example.invernaderoiot

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AlertLevel {
    NORMAL,
    ALERTA,
    PELIGRO
}

data class SensorReading(
    val timestamp: String,
    val temperatura: Float,
    val humedad: Float
)

data class SystemEvent(
    val timestamp: String,
    val mensaje: String,
    val nivel: AlertLevel = AlertLevel.NORMAL
)

fun obtenerHoraActual(): String {
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return formatter.format(Date())
}
