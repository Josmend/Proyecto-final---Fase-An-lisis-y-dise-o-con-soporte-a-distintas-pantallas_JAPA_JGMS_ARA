package com.example.myapplication

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Obtener los datos del intent (título y descripción)
        val title = intent.getStringExtra("title")
        val description = intent.getStringExtra("description")

        // Verificar si el permiso de POST_NOTIFICATIONS está concedido
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {

            // Crear la notificación
            val builder = NotificationCompat.Builder(context, "reminder_channel")
                .setSmallIcon(R.mipmap.ic_launcher) // Usa el ícono predeterminado o uno personalizado
                .setContentTitle(title ?: "Tarea pendiente")
                .setContentText(description ?: "Tienes una tarea por cumplir")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            // Mostrar la notificación
            with(NotificationManagerCompat.from(context)) {
                notify(1001, builder.build()) // Cambia el ID si quieres notificaciones únicas
            }
        } else {
            Log.e("ReminderReceiver", "Permiso de notificación no concedido")
        }
    }
}
