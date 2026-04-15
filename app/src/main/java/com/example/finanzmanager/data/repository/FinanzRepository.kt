package com.example.finanzmanager.data.repository

import com.example.finanzmanager.data.*
import com.example.finanzmanager.data.database.AppDatabase
import com.example.finanzmanager.domain.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class FinanzRepository(private val db: AppDatabase) {

    private val gson = Gson()
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // ── Flows ────────────────────────────────────────────────────────────────
    val accounts: Flow<List<Account>> =
        db.accountDao().getAll().map { list -> list.map { it.toDomain() } }

    val transactions: Flow<List<Transaction>> =
        db.transactionDao().getAll().map { list -> list.map { it.toDomain() } }

    val categories: Flow<List<Category>> =
        db.categoryDao().getAll().map { list -> list.map { it.toDomain() } }

    val standingOrders: Flow<List<StandingOrder>> =
        db.standingOrderDao().getAll().map { list -> list.map { it.toDomain() } }

    // ── Accounts ─────────────────────────────────────────────────────────────
    suspend fun upsertAccount(account: Account) =
        db.accountDao().insert(account.toEntity())

    suspend fun deleteAccount(id: String) =
        db.accountDao().deleteById(id)

    suspend fun updateAccountBalance(id: String, newBalance: Double) =
        db.accountDao().updateBalance(id, newBalance)

    suspend fun getAccountById(id: String): Account? =
        db.accountDao().getById(id)?.toDomain()

    suspend fun updateInvestmentHistory(id: String, history: List<HistoryEntry>) {
        val list = history.map { mapOf("date" to it.date, "val" to it.value) }
        db.accountDao().updateHistory(id, gson.toJson(list))
    }

    // ── Transactions ──────────────────────────────────────────────────────────
    suspend fun upsertTransaction(tx: Transaction) =
        db.transactionDao().insert(tx.toEntity())

    suspend fun deleteTransaction(id: String) =
        db.transactionDao().deleteById(id)

    suspend fun getTransactionById(id: String): Transaction? =
        db.transactionDao().getById(id)?.toDomain()

    // ── Categories ────────────────────────────────────────────────────────────
    suspend fun upsertCategory(cat: Category) =
        db.categoryDao().insert(cat.toEntity())

    suspend fun deleteCategory(id: String) =
        db.categoryDao().deleteById(id)

    // ── Standing Orders ───────────────────────────────────────────────────────
    suspend fun upsertStandingOrder(order: StandingOrder) =
        db.standingOrderDao().insert(order.toEntity())

    suspend fun deleteStandingOrder(id: String) =
        db.standingOrderDao().deleteById(id)

    suspend fun getAllStandingOrdersSync(): List<StandingOrder> =
        db.standingOrderDao().getAllSync().map { it.toDomain() }

    // ── Standing Order Processing ─────────────────────────────────────────────
    suspend fun processStandingOrders(
        currentAccounts: List<Account>,
        currentTransactions: List<Transaction>
    ): Pair<List<Account>, List<Transaction>>? {

        val today = LocalDate.now().format(fmt)
        val orders = getAllStandingOrdersSync()
        val newTxs = mutableListOf<Transaction>()
        val balanceChanges = mutableMapOf<String, Double>()
        var hasChanges = false

        for (order in orders) {
            var currentRun = order.nextRun
            while (currentRun <= today) {
                hasChanges = true
                newTxs.add(
                    Transaction(
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
                )
                when (order.type) {
                    "expense" ->
                        balanceChanges[order.accountId] =
                            (balanceChanges[order.accountId] ?: 0.0) - order.amount
                    "income" ->
                        balanceChanges[order.accountId] =
                            (balanceChanges[order.accountId] ?: 0.0) + order.amount
                    "transfer" -> {
                        balanceChanges[order.accountId] =
                            (balanceChanges[order.accountId] ?: 0.0) - order.amount
                        order.toAccountId?.let { to ->
                            balanceChanges[to] = (balanceChanges[to] ?: 0.0) + order.amount
                        }
                    }
                }
                currentRun = advanceDate(currentRun, order.interval)
            }
            if (currentRun != order.nextRun) {
                db.standingOrderDao().insert(order.copy(nextRun = currentRun).toEntity())
            }
        }

        if (!hasChanges) return null

        newTxs.forEach { db.transactionDao().insert(it.toEntity()) }

        val updatedAccounts = currentAccounts.map { acc ->
            val change = balanceChanges[acc.id] ?: return@map acc
            val updated = acc.copy(balance = round(acc.balance + change))
            db.accountDao().updateBalance(acc.id, updated.balance)
            updated
        }

        return Pair(
            updatedAccounts,
            (newTxs + currentTransactions).sortedByDescending { it.date }
        )
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
            Account("acc1", "Girokonto",   1500.0, "Liquide Mittel", "cash",        "bank"),
            Account("acc2", "Bargeld",       50.0, "Liquide Mittel", "cash",        "wallet"),
            Account("acc3", "Investments", 1200.0, "Investments",    "investment",  "stock")
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

    // ── Export ────────────────────────────────────────────────────────────────
    suspend fun exportToJson(
        accounts: List<Account>,
        transactions: List<Transaction>,
        categories: List<Category>,
        standingOrders: List<StandingOrder>,
        settings: Map<String, Any>
    ): String = gson.toJson(
        mapOf(
            "accounts"       to accounts,
            "transactions"   to transactions,
            "categories"     to categories,
            "standingOrders" to standingOrders,
            "settings"       to settings
        )
    )

    // ── Import ────────────────────────────────────────────────────────────────
    suspend fun importFromJson(json: String): Boolean {
        return try {
            // Parse outer map
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            val data: Map<String, Any> = gson.fromJson(json, mapType)

            // Clear existing data
            db.accountDao().deleteAll()
            db.transactionDao().deleteAll()
            db.categoryDao().deleteAll()
            db.standingOrderDao().deleteAll()

            // Helper: re-serialize a value, then deserialize as List<T>
            fun <T> parseList(key: String, clazz: Class<T>): List<T> {
                val raw = data[key] ?: return emptyList()
                val rawJson = gson.toJson(raw)
                val listType = TypeToken.getParameterized(List::class.java, clazz).type
                return gson.fromJson(rawJson, listType) ?: emptyList()
            }

            parseList("accounts",       Account::class.java)
                .forEach { db.accountDao().insert(it.toEntity()) }

            parseList("transactions",   Transaction::class.java)
                .forEach { db.transactionDao().insert(it.toEntity()) }

            parseList("categories",     Category::class.java)
                .forEach { db.categoryDao().insert(it.toEntity()) }

            parseList("standingOrders", StandingOrder::class.java)
                .forEach { db.standingOrderDao().insert(it.toEntity()) }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    companion object {
        fun round(value: Double): Double = Math.round(value * 100.0) / 100.0
    }
}
