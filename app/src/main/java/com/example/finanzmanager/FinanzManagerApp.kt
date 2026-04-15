package com.example.finanzmanager

import android.app.Application
import androidx.work.*
import com.example.finanzmanager.workers.StandingOrderWorker
import java.util.concurrent.TimeUnit

class FinanzManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        scheduleStandingOrderCheck()
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
