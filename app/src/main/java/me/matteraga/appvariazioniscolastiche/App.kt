package me.matteraga.appvariazioniscolastiche

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.content.getSystemService

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        // Creazione del canale per le notifiche
        val channel = NotificationChannel(
            "changes",
            "Variazioni",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifiche relative alle variazioni"
        }
        getSystemService<NotificationManager>()?.createNotificationChannel(channel)
    }
}