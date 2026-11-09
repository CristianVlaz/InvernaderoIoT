package com.example.invernaderoiot

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class NotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "invernadero_alertas_channel"
        private const val NOTIFICATION_ID_TEMP = 1001
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alertas de Temperatura"
            val descriptionText = "Notificaciones por temperatura elevada o crítica en el invernadero"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendTemperatureAlert(
        temperatura: Float,
        umbralSuperado: Float,
        esPeligro: Boolean,
        ventilacionAuto: Boolean
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val tempFormateada = String.format(Locale.getDefault(), "%.1f°C", temperatura)
        val umbralFormateado = String.format(Locale.getDefault(), "%.0f°C", umbralSuperado)

        val titulo = if (esPeligro) {
            "Peligro Crítico en Invernadero ($tempFormateada)"
        } else {
            "Alerta de Temperatura ($tempFormateada)"
        }

        val detalleAuto = if (ventilacionAuto) " Ventilación activada automáticamente." else ""
        val mensaje = if (esPeligro) {
            "Se superó el límite crítico de $umbralFormateado. Revisa el sistema de inmediato.$detalleAuto"
        } else {
            "La temperatura superó el umbral configurado de $umbralFormateado.$detalleAuto"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_TEMP, notification)
    }
}
