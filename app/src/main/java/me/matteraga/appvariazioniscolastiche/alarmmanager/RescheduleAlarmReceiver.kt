package me.matteraga.appvariazioniscolastiche.alarmmanager

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class RescheduleAlarmReceiver : BroadcastReceiver() {

    private val actions = listOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED
    ).also {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            it.plus(AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (actions.contains(intent.action)) {
            AlarmScheduler(context).scheduleSaved()
        }
    }
}