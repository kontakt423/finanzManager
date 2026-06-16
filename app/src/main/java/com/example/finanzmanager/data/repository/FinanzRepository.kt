package com.example.finanzmanager.data.repository

import androidx.room.withTransaction
import com.example.finanzmanager.data.*
import com.example.finanzmanager.data.database.AppDatabase
import com.example.finanzmanager.domain.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class FinanzRepository(private val db: AppDatabase) {

    private val gson = Gson()
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // ── Flows ────────────────────────────────────────────────────────────────
    val accounts: Flow<List<Account>> =
        db.accountDao().getAll().map { it.map { e -> e.toDomain() } }
    val transactions: Flow<List<Transaction>> =
        db.transactionDao().getAll().map { it.map { e -> e.toDomain() } }
    val categories: Flow<List<Category>> =
        db.categoryDao().getAll().map { it.map { e -> e.toDomain() } }
    val standingOrders: Flow<List<StandingOrder>> =
        db.standingOrderDao().getAll().map { it.map { e -> e.toDomain() } }
    val savingsGoals: Flow<List<SavingsGoal>> =
        db.savingsGoalDao().getAll().map { it.map { e -> e.toDomain() } }

    // ── Accounts ─────────────────────────────────────────────────────────────
    suspend fun upsertAccount(account: Account) = db.accountDao().insert(account.toEntity())
    suspend fun deleteAccount(id: String) = db.accountDao().deleteById(id)
    suspend fun updateAccountBalance(id: String, v: Double) = db.accountDao().updateBalance(id, v)
    suspend fun getAccountById(id: String): Account? = db.accountDao().getById(id)?.toDomain()
    suspend fun updateInvestmentHistory(id: String, history: List<HistoryEntry>) {
        val list = history.map { mapOf("date" to it.date, "val" to it.value) }
        db.accountDao().updateHistory(id, gson.toJson(list))
    }

    // ── Transactions ──────────────────────────────────────────────────────────
    suspend fun upsertTransaction(tx: Transaction) = db.transactionDao().insert(tx.toEntity())
    suspend fun deleteTransaction(id: String) = db.transactionDao().deleteById(id)
    suspend fun getTransactionById(id: String): Transaction? =
        db.transactionDao().getById(id)?.toDomain()

    // ── Categories ────────────────────────────────────────────────────────────
    suspend fun upsertCategory(cat: Category) = db.categoryDao().insert(cat.toEntity())
    suspend fun deleteCategory(id: String) = db.categoryDao().deleteById(id)

    // ── Standing Orders ───────────────────────────────────────────────────────
    suspend fun upsertStandingOrder(order: StandingOrder) =
        db.standingOrderDao().insert(order.toEntity())
    suspend fun deleteStandingOrder(id: String) = db.standingOrderDao().deleteById(id)
    suspend fun getAllStandingOrdersSync(): List<StandingOrder> =
        db.standingOrderDao().getAllSync().map { it.toDomain() }

    // ── Savings Goals ─────────────────────────────────────────────────────────
    suspend fun upsertSavingsGoal(goal: SavingsGoal) = db.savingsGoalDao().insert(goal.toEntity())
    suspend fun deleteSavingsGoal(id: String) = db.savingsGoalDao().deleteById(id)

    // ── Interest Processing ───────────────────────────────────────────────────
    suspend fun processInterest() {
        val today = LocalDate.now().format(fmt)
        val accounts = db.accountDao().getAllSync().map { it.toDomain() }

        for (acc in accounts) {
            if (acc.interestRate <= 0.0 || acc.nextInterestRun.isEmpty()) continue
            if (acc.nextInterestRun > today) continue

            var currentRun = acc.nextInterestRun
            var iterations = 0

            while (currentRun <= today && iterations < 120) {
                iterations++
                val periodRate = when (acc.interestInterval) {
                    "quarterly" -> acc.interestRate / 4.0 / 100.0
                    "yearly"    -> acc.interestRate / 100.0
                    else        -> acc.interestRate / 12.0 / 100.0  // monthly
                }
                val fresh = getAccountById(acc.id) ?: break
                val interest = round(fresh.balance * periodRate)
                if (interest > 0) {
                    val tx = Transaction(
                        id = UUID.randomUUID().toString().replace("-", "").take(9),
                        type = "income",
                        amount = interest,
                        description = "Zinsen (${acc.interestRate}% p.a.)",
                        categoryId = "",
                        accountId = acc.id,
                        date = currentRun
                    )
                    db.transactionDao().insert(tx.toEntity())
                    updateAccountBalance(acc.id, round(fresh.balance + interest))
                }
                currentRun = advanceDate(currentRun, acc.interestInterval)
            }

            if (currentRun != acc.nextInterestRun) {
                db.accountDao().insert(acc.copy(nextInterestRun = currentRun).toEntity())
            }
        }
    }

    // ── Standing Order Processing ─────────────────────────────────────────────
    suspend fun processStandingOrders(): Boolean = processingLock.withLock {
        val today = LocalDate.now().format(fmt)
        val orders = getAllStandingOrdersSync()
        var hasChanges = false

        for (order in orders) {
            var currentRun = order.nextRun
            var iterations = 0
            while (currentRun <= today && iterations < 366) {
                iterations++
                hasChanges = true

                val tx = Transaction(
                    id = UUID.randomUUID().toString().replace("-", "").take(9),
                    type = order.type,
                    amount = order.amount,
                    description = order.description,
                    categoryId = order.categoryId,
                    accountId = order.accountId,
                    toAccountId = order.toAccountId,
                    isSplit = order.isSplit,
                    splitMode = order.splitMode,
                    date = currentRun
                )
                db.transactionDao().insert(tx.toEntity())

                when (order.type) {
                    "expense" -> getAccountById(order.accountId)?.let { acc ->
                        updateAccountBalance(acc.id, round(acc.balance - order.amount))
                    }
                    "income" -> getAccountById(order.accountId)?.let { acc ->
                        updateAccountBalance(acc.id, round(acc.balance + order.amount))
                    }
                    "transfer" -> {
                        getAccountById(order.accountId)?.let { acc ->
                            updateAccountBalance(acc.id, round(acc.balance - order.amount))
                        }
                        order.toAccountId?.let { toId ->
                            getAccountById(toId)?.let { acc ->
                                updateAccountBalance(acc.id, round(acc.balance + order.amount))
                            }
                        }
                    }
                }

                currentRun = advanceDate(currentRun, order.interval)
            }

            if (currentRun != order.nextRun) {
                db.standingOrderDao().insert(order.copy(nextRun = currentRun).toEntity())
            }
        }
        return hasChanges
    }

    private fun advanceDate(dateStr: String, interval: String): String {
        val date = LocalDate.parse(dateStr, fmt)
        return when (interval) {
            "weekly" -> date.plusWeeks(1)
            "yearly" -> date.plusYears(1)
            else     -> date.plusMonths(1)
        }.format(fmt)
    }

    // ── Seed Data ─────────────────────────────────────────────────────────────
    suspend fun seedDefaultData() {
        if (db.accountDao().getAll().first().isNotEmpty()) return
        listOf(
            Account("acc1", "Girokonto",   1500.0, "Liquide Mittel", "cash",       "bank"),
            Account("acc2", "Bargeld",       50.0, "Liquide Mittel", "cash",       "wallet"),
            Account("acc3", "Investments", 1200.0, "Investments",    "investment", "stock")
        ).forEach { db.accountDao().insert(it.toEntity()) }
        listOf(
            Category("c1", "Essen",        "#f59e0b", "expense"),
            Category("c2", "Gehalt",       "#10b981", "income"),
            Category("c3", "Tabak/Genuss", "#ef4444", "expense"),
            Category("c4", "Miete",        "#3b82f6", "expense"),
            Category("c5", "Transport",    "#6366f1", "expense"),
            Category("c6", "Freelance",    "#10b981", "income"),
            Category("c7", "Sonstiges",    "#64748b", "expense")
        ).forEach { db.categoryDao().insert(it.toEntity()) }
    }

    // ── Export / Import ───────────────────────────────────────────────────────
    suspend fun exportToJson(
        accounts: List<Account>, transactions: List<Transaction>,
        categories: List<Category>, standingOrders: List<StandingOrder>,
        savingsGoals: List<SavingsGoal>, settings: Map<String, Any>
    ): String = gson.toJson(mapOf(
        "accounts" to accounts, "transactions" to transactions,
        "categories" to categories, "standingOrders" to standingOrders,
        "savingsGoals" to savingsGoals, "settings" to settings
    ))

    suspend fun importFromJson(json: String): Boolean {
        return try {
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            val data: Map<String, Any> = gson.fromJson(json, mapType)

            fun <T> parseList(key: String, clazz: Class<T>): List<T> {
                val raw = data[key] ?: return emptyList()
                val rawJson = gson.toJson(raw)
                return gson.fromJson(rawJson,
                    TypeToken.getParameterized(List::class.java, clazz).type) ?: emptyList()
            }

            // Apply safe defaults for fields that may be absent in older backups.
            // Gson sets missing non-nullable Kotlin fields to null at runtime, which
            // causes Room NOT NULL constraint failures without these guards.
            val accounts = parseList("accounts", Account::class.java).map { acc ->
                acc.copy(
                    interestInterval = acc.interestInterval ?: "monthly",
                    nextInterestRun  = acc.nextInterestRun  ?: ""
                )
            }
            val transactions = parseList("transactions", Transaction::class.java).map { tx ->
                tx.copy(
                    splitMode    = tx.splitMode    ?: "half",
                    toAccountId  = tx.toAccountId,     // nullable — already safe
                    categoryId   = tx.categoryId   ?: ""
                )
            }
            val categories     = parseList("categories",    Category::class.java)
            val standingOrders = parseList("standingOrders", StandingOrder::class.java).map { o ->
                o.copy(splitMode = o.splitMode ?: "half")
            }
            val savingsGoals = parseList("savingsGoals", SavingsGoal::class.java).map { g ->
                g.copy(
                    deadline = g.deadline ?: "",
                    color    = g.color    ?: "#3b82f6"
                )
            }

            db.withTransaction {
                db.accountDao().deleteAll()
                db.transactionDao().deleteAll()
                db.categoryDao().deleteAll()
                db.standingOrderDao().deleteAll()
                db.savingsGoalDao().deleteAll()
                accounts.forEach       { db.accountDao().insert(it.toEntity()) }
                transactions.forEach   { db.transactionDao().insert(it.toEntity()) }
                categories.forEach     { db.categoryDao().insert(it.toEntity()) }
                standingOrders.forEach { db.standingOrderDao().insert(it.toEntity()) }
                savingsGoals.forEach   { db.savingsGoalDao().insert(it.toEntity()) }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    companion object {
        fun round(v: Double): Double = Math.round(v * 100.0) / 100.0
        private val processingLock = Mutex()
    }
}
