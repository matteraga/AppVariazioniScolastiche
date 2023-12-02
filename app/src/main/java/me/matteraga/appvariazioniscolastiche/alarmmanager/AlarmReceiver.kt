package me.matteraga.appvariazioniscolastiche.alarmmanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import me.matteraga.appvariazioniscolastiche.workers.CheckChangesWorker
import java.time.LocalDate

class AlarmReceiver : BroadcastReceiver() {

    // Eseguito quando scatta l'allarme
    override fun onReceive(context: Context, intent: Intent) {
        // Avvia il worker per controllare le variazioni
        val data = Data.Builder().apply {
            putString("date", LocalDate.now().plusDays(1).toString())
        }.build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "auto-check",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<CheckChangesWorker>().apply {
                setInputData(data)
            }.build()
        )
    }
}