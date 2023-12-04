package me.matteraga.appvariazioniscolastiche.alarmmanager

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDateTime
import java.time.ZoneId

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    // Ogni allarme deve essere impostata per un ora diversa e usa l'ora come identificatore
    private val sharedPrefAlarms = context.getSharedPreferences("alarms", Context.MODE_PRIVATE)

    // Programma l'allarme per l'ora indicata, si ripete ogni giorno
    private fun scheduleSaved(hour: Int) {
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
                hour,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    // Attiva tutte le allarmi salvate nelle preference
    fun scheduleSaved() {
        getScheduledAlarms().forEach {
            scheduleSaved(it)
        }
    }

    // Attiva e salva una nuova allarme
    fun scheduleAndSave(hour: Int) {
        scheduleSaved(hour)
        with(sharedPrefAlarms.edit()) {
            putInt(hour.toString(), hour)
            apply()
        }
    }

    // Cancella l'allarme
    private fun cancelSaved(hour: Int) {
        alarmManager.cancel(
            PendingIntent.getBroadcast(
                context,
                hour,
                Intent(context, AlarmReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    // Cancella tutte le allarmi salvate nelle preference
    fun cancelSaved() {
        getScheduledAlarms().forEach {
            cancelSaved(it)
        }
    }

    // Cancella ed elimina un allarme
    fun cancelAndRemove(hour: Int) {
        cancelSaved(hour)
        with(sharedPrefAlarms.edit()) {
            remove(hour.toString())
            apply()
        }
    }

    // Tutte le ore già impostate
    fun getScheduledAlarms(): List<Int> {
        return sharedPrefAlarms.all.map {
            it.value.toString().toInt()
        }
    }

    // Una lista di ore non ancora impostate
    fun getNotSetAlarms(): List<Int> {
        val alarms = getScheduledAlarms()
        return (1..24).mapNotNull {
            it.takeUnless { alarms.contains(it) }
        }
    }
}