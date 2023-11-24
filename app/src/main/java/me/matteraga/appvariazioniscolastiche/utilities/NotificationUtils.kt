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
import me.matteraga.appvariazioniscolastiche.R

class NotificationUtils(private val context: Context) {

    // Invia una notifica se le notifiche sono abilitate e il permesso è concesso
    private fun sendNotification(notification: Notification) {
        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED && !areNotificationsEnabled()
            ) {
                return
            }
            // Tutte le notifiche hanno lo stesso id per sovrascrivere la notifica precedente
            notify(1, notification)
        }
    }

    // Invia una notifica che se cliccata apre il pdf
    fun sendPdfNotification(uri: Uri, title: String, text: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        }
        val pending = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = NotificationCompat.Builder(context, "changes").apply {
            setSmallIcon(R.drawable.ic_check_circle)
            setContentTitle(title)
            setContentText(text)
            priority = NotificationCompat.PRIORITY_DEFAULT
            setContentIntent(pending)
            setAutoCancel(true)
        }

        sendNotification(builder.build())
    }

    // Invia una notifica che se cliccata apre il browser
    fun sendBrowserNotification(url: String, title: String, text: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        }
        val pending = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = NotificationCompat.Builder(context, "changes").apply {
            setSmallIcon(R.drawable.ic_warning)
            setContentTitle(title)
            setContentText(text)
            priority = NotificationCompat.PRIORITY_DEFAULT
            setContentIntent(pending)
            setAutoCancel(true)
        }

        sendNotification(builder.build())
    }
}