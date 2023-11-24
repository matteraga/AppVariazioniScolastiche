package me.matteraga.appvariazioniscolastiche.alarmmanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager

class BootReceiver : BroadcastReceiver() {

    // Riprogramma l'allarme al boot del dispositivo
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            val hour = PreferenceManager.getDefaultSharedPreferences(
                context
            ).getInt("time", 19)
            AlarmScheduler(context).schedule(hour)
        }
    }
}