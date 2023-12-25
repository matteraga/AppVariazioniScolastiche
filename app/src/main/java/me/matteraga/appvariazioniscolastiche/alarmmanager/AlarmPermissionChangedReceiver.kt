package me.matteraga.appvariazioniscolastiche.alarmmanager

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmPermissionChangedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED == intent.action) {
            AlarmScheduler(context).scheduleSaved()
        }
    }
}