package me.matteraga.appvariazioniscolastiche.workers

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import me.matteraga.appvariazioniscolastiche.R
import me.matteraga.appvariazioniscolastiche.utilities.StorageUtils
import kotlin.random.Random

class DeletePdfsWorker(
    private val context: Context,
    private val workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    private val storageUtils = StorageUtils(context)

    // Mostra notifica
    private suspend fun startForegroundService() {
        setForeground(
            ForegroundInfo(
                Random.nextInt(),
                NotificationCompat.Builder(context, "changes")
                    .setSmallIcon(R.drawable.ic_auto_delete)
                    .setContentTitle("Eliminazione in corso...")
                    .setProgress(0, 0, true)
                    .build()
            )
        )
    }

    override suspend fun doWork(): Result {
        startForegroundService()

        // Cancella tutti i pdf
        val success = storageUtils.bulkDelete()
        return if (success) {
            Result.success()
        } else {
            Result.failure()
        }
    }
}