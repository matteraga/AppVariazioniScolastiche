package me.matteraga.appvariazioniscolastiche.alarmmanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager

class TimeChangeReceiver : BroadcastReceiver() {

    // Riprogramma l'allarme al cambio di ora o di fuso orario
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_TIME_CHANGED || action == Intent.ACTION_TIMEZONE_CHANGED) {
            val hour = PreferenceManager.getDefaultSharedPreferences(
                context
            ).getInt("time", 19)
            AlarmScheduler(context).schedule(hour)
        }
    }
}