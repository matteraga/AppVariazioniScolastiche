package me.matteraga.appvariazioniscolastiche.alarmmanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import me.matteraga.appvariazioniscolastiche.ChangesToCheck
import me.matteraga.appvariazioniscolastiche.workers.CheckChangesWorker
import java.time.LocalDate

class AlarmReceiver : BroadcastReceiver() {

    // Worker
    private fun enqueueCheckChangesWork(context: Context, daysToAdd: Int) {
        val date = LocalDate.now().plusDays(daysToAdd.toLong())
        val data = Data.Builder().apply {
            putString("date", date.toString())
        }.build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "auto-check-${date}",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<CheckChangesWorker>().apply {
                setInputData(data)
            }.build()
        )
    }

    // Eseguito quando scatta l'allarme
    override fun onReceive(context: Context, intent: Intent) {
        // Avvia il o i worker per controllare le variazioni
        val changesToCheck = intent.getIntExtra("changesToCheck", ChangesToCheck.TODAY)
        if (changesToCheck == ChangesToCheck.TODAY || changesToCheck == ChangesToCheck.TODAY_AND_TOMORROW) {
            enqueueCheckChangesWork(context, 0)
        }
        if (changesToCheck == ChangesToCheck.TOMORROW || changesToCheck == ChangesToCheck.TODAY_AND_TOMORROW) {
            enqueueCheckChangesWork(context, 1)
        }
    }
}