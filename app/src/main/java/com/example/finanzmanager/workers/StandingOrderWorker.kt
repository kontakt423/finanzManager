package com.example.finanzmanager.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.finanzmanager.data.database.AppDatabase
import com.example.finanzmanager.data.repository.FinanzRepository
import com.example.finanzmanager.data.toDomain
import kotlinx.coroutines.flow.first

class StandingOrderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val repo = FinanzRepository(db)

            val accounts = db.accountDao().getAll().first().map { it.toDomain() }
            val transactions = db.transactionDao().getAll().first().map { it.toDomain() }

            repo.processStandingOrders(accounts, transactions)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
