package me.matteraga.appvariazioniscolastiche.utilities

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import me.matteraga.appvariazioniscolastiche.NOTIFICATION_CHANNEL_ID
import me.matteraga.appvariazioniscolastiche.activities.MainActivity
import java.time.LocalDate

class NotificationUtils(private val context: Context) {

    // Invia una notifica se le notifiche sono abilitate e il permesso è concesso
    private fun sendNotification(notification: Notification, id: Int) {
        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED && !areNotificationsEnabled()
            ) {
                return
            }
            // L'id della notifica è l'hashcode della data
            notify(id, notification)
        }
    }

    // Notifica base
    private fun sendBaseNotification(
        id: Int,
        title: String,
        text: String,
        icon: Int,
        intent: Intent
    ) {
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID).apply {
            setSmallIcon(icon)
            setContentTitle(title)
            setContentText(text)
            priority = NotificationCompat.PRIORITY_DEFAULT
            setContentIntent(
                PendingIntent.getActivity(
                    context,
                    id,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            setAutoCancel(true)
        }.build()

        sendNotification(notification, id)
    }

    // Invia una notifica che se cliccata apre il pdf
    fun sendPdfNotification(uri: Uri, date: LocalDate, title: String, text: String, icon: Int) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        }
        sendBaseNotification(date.hashCode(), title, text, icon, intent)
    }

    // Invia una notifica che se cliccata apre il browser
    fun sendBrowserNotification(
        url: String,
        date: LocalDate,
        title: String,
        text: String,
        icon: Int
    ) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        }
        sendBaseNotification(date.hashCode(), title, text, icon, intent)
    }

    // Invia una notifica che se cliccata apre l'app
    fun sendOpenAppNotification(
        date: LocalDate,
        title: String,
        text: String,
        icon: Int
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        sendBaseNotification(date.hashCode(), title, text, icon, intent)
    }
}