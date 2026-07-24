package com.example.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.LocalNetApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val repository = LocalNetApp.instance.repository
            CoroutineScope(Dispatchers.IO).launch {
                val isVpnEnabled = repository.settings.isVpnEnabled.first()
                if (isVpnEnabled) {
                    val serviceIntent = Intent(context, LocalNetVpnService::class.java)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
            }
        }
    }
}
