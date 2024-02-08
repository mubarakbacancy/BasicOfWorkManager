package com.mubarak.basicofworkmanager.workertimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != null) {
            val serviceIntent = Intent(context, CountdownService::class.java)
            serviceIntent.action = action
            context.startService(serviceIntent)
        }
    }
}
