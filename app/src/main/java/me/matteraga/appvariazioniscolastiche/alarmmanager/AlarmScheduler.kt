package me.matteraga.appvariazioniscolastiche.alarmmanager

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDateTime
import java.time.ZoneId

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    // Programma l'allarme per l'ora indicata, si ripete ogni giorno
    fun schedule(hour: Int) {
        var time = LocalDateTime.now()
            .withHour(hour)
            .withMinute(0)
            .withSecond(0)
        if (time.isBefore(LocalDateTime.now())) {
            time = time.plusDays(1)
        }
        val intent = Intent(context, AlarmReceiver::class.java)
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            time.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000,
            AlarmManager.INTERVAL_DAY,
            PendingIntent.getBroadcast(
                context,
                1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    // Cancella l'allarme
    fun cancel() {
        alarmManager.cancel(
            PendingIntent.getBroadcast(
                context,
                1,
                Intent(context, AlarmReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }
}