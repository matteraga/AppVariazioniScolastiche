package me.matteraga.appvariazioniscolastiche.alarmmanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    // Riprogramma le allarmi al boot del dispositivo
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            AlarmScheduler(context).scheduleSaved()
        }
    }
}