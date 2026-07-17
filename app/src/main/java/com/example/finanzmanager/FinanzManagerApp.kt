package com.example.finanzmanager

import android.app.Application
import androidx.work.*
import com.example.finanzmanager.sync.SyncServer
import com.example.finanzmanager.workers.StandingOrderWorker
import java.util.concurrent.TimeUnit

class FinanzManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        scheduleStandingOrderCheck()
        startSyncServer()
    }

    private fun startSyncServer() {
        try {
            SyncServer(this).start(fi.iki.elonen.NanoHTTPD.SOCKET_READ_TIMEOUT, false)
        } catch (e: Exception) {
            // Port evtl. schon belegt (z.B. zweiter Prozessstart) – Sync ist optional,
            // App darf dadurch nicht abstürzen.
            e.printStackTrace()
        }
    }

    private fun scheduleStandingOrderCheck() {
        val request = PeriodicWorkRequestBuilder<StandingOrderWorker>(
            repeatInterval = 1, repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setConstraints(Constraints.NONE)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "standing_order_check",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
