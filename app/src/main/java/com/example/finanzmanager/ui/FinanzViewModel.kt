package com.example.finanzmanager.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.finanzmanager.data.repository.FinanzRepository
import com.example.finanzmanager.data.repository.SettingsRepository
import com.example.finanzmanager.data.repository.FinanzRepository.Companion.round
import com.example.finanzmanager.domain.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

enum class FilterType { TODAY, WEEK, MONTH, CUSTOM }
enum class ActiveTab { DASHBOARD, ANALYSIS, SETTINGS }

data class UiState(
    val accounts: List<Account> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val standingOrders: List<StandingOrder> = emptyList(),
    val savingsGoals: List<SavingsGoal> = emptyList(),
    val isDarkMode: Boolean = true,
    val splitPotEnabled: Boolean = true,
    val countFullSplitIncome: Boolean = false,
    val splitPotStartDate: String = "",   // "" = all time, "yyyy-MM-dd" = from that date
    val filterType: FilterType = FilterType.MONTH,
    val customFrom: String = today(),
    val customTo: String = today(),
    val compDate1: String = monthAgo(),
    val compDate2: String = today(),
    val activeTab: ActiveTab = ActiveTab.DASHBOARD,
    val isLoaded: Boolean = false,
    val snackbarMessage: String? = null
)

class FinanzViewModel(
    private val repo: FinanzRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    init {
        viewModelScope.launch {
            // Combine all flows
            combine(
                repo.accounts,
                repo.transactions,
                repo.categories,
                repo.standingOrders,
                settings.isDarkMode
            ) { accs, txs, cats, orders, dark ->
                _state.update { s ->
                    s.copy(
                        accounts = accs,
                        transactions = txs,
                        categories = cats,
                        standingOrders = orders,
                        isDarkMode = dark,
                        isLoaded = true
                    )
                }
            }.collect()
        }
        viewModelScope.launch {
            combine(
                settings.isSplitPotEnabled,
                settings.countFullSplitIncome,
                settings.splitPotStartDate
            ) { split, full, startDate ->
                Triple(split, full, startDate)
            }.collect { (split, full, startDate) ->
                _state.update { it.copy(
                    splitPotEnabled = split,
                    countFullSplitIncome = full,
                    splitPotStartDate = startDate
                ) }
            }
        }
        viewModelScope.launch {
            repo.savingsGoals.collect { goals ->
                _state.update { it.copy(savingsGoals = goals) }
            }
        }
        viewModelScope.launch {
            repo.seedDefaultData()
        }
        viewModelScope.launch {
            // Wait for initial data load, then process any overdue standing orders.
            // processStandingOrders() writes directly to DB; Room Flows auto-update the UI.
            state.filter { it.isLoaded }.take(1).collect {
                try {
                    repo.processStandingOrders()
                } catch (e: Exception) {
                    e.printStackTrace() // never crash the app over a standing-order error
                }
            }
        }
    }

    // ---- Navigation ----
    fun setActiveTab(tab: ActiveTab) = _state.update { it.copy(activeTab = tab) }

    // ---- Settings ----
    fun toggleDarkMode() = viewModelScope.launch { settings.setDarkMode(!_state.value.isDarkMode) }
    fun toggleSplitPot() = viewModelScope.launch { settings.setSplitPotEnabled(!_state.value.splitPotEnabled) }
    fun toggleCountFullSplitIncome() = viewModelScope.launch { settings.setCountFullSplitIncome(!_state.value.countFullSplitIncome) }
    fun setSplitPotStartDate(date: String) = viewModelScope.launch { settings.setSplitPotStartDate(date) }
    fun clearSplitPotStartDate() = viewModelScope.launch { settings.setSplitPotStartDate("") }

    // ---- Filter ----
    fun setFilterType(type: FilterType) = _state.update { it.copy(filterType = type) }
    fun setCustomRange(from: String, to: String) = _state.update { it.copy(customFrom = from, customTo = to) }
    fun setCompDate1(d: String) = _state.update { it.copy(compDate1 = d) }
    fun setCompDate2(d: String) = _state.update { it.copy(compDate2 = d) }

    // ---- Derived: Effective Range ----
    // Overload that accepts explicit params (used by SplitDetailSheet for its own local filter)
    fun effectiveRangeFor(
        filterType: FilterType,
        customFrom: String,
        customTo: String
    ): Pair<String, String> {
        val today = java.time.LocalDate.now()
        return when (filterType) {
            FilterType.TODAY  -> Pair(today.format(fmt), today.format(fmt))
            FilterType.WEEK   -> Pair(today.minusWeeks(1).format(fmt), today.format(fmt))
            FilterType.MONTH  -> Pair(today.minusMonths(1).format(fmt), today.format(fmt))
            FilterType.CUSTOM -> Pair(customFrom, customTo)
        }
    }

    fun effectiveRange(): Pair<String, String> {
        val s = _state.value
        val today = LocalDate.now()
        return when (s.filterType) {
            FilterType.TODAY -> Pair(today.format(fmt), today.format(fmt))
            FilterType.WEEK -> Pair(today.minusWeeks(1).format(fmt), today.format(fmt))
            FilterType.MONTH -> Pair(today.minusMonths(1).format(fmt), today.format(fmt))
            FilterType.CUSTOM -> Pair(s.customFrom, s.customTo)
        }
    }

    // ---- Derived: Filtered transactions ----
    fun filteredTransactions(): List<Transaction> {
        val (from, to) = effectiveRange()
        return _state.value.transactions
            .filter { it.date in from..to }
            .sortedByDescending { it.date }
    }

    // ---- Derived: Totals ----
    fun totals(): Totals {
        val s = _state.value
        var total = s.accounts.sumOf { it.balance }
        var liquid = s.accounts.filter { it.category == "Liquide Mittel" }.sumOf { it.balance }

        if (!s.countFullSplitIncome) {
            // 1. Sum up all split-income reservations
            var splitReservedTotal = 0.0
            var splitReservedLiquid = 0.0
            val splitStartFilter: (Transaction) -> Boolean = { t ->
                s.splitPotStartDate.isEmpty() || t.date >= s.splitPotStartDate
            }
            s.transactions.filter { it.isSplit && it.type == "income" && splitStartFilter(it) }.forEach { t ->
                val share = if (t.splitMode == "partner") t.amount else round(t.amount / 2)
                splitReservedTotal = round(splitReservedTotal + share)
                val acc = s.accounts.find { a -> a.id == t.accountId }
                if (acc?.category == "Liquide Mittel") {
                    splitReservedLiquid = round(splitReservedLiquid + share)
                }
            }
            // 2. Subtract partner settlements already paid — respects start date
            val paidTotal  = s.transactions.filter { it.isSettlement && splitStartFilter(it) }.sumOf { it.amount }
            val paidLiquid = s.transactions.filter { it.isSettlement && splitStartFilter(it) }.sumOf { t ->
                val acc = s.accounts.find { a -> a.id == t.accountId }
                if (acc?.category == "Liquide Mittel") t.amount else 0.0
            }
            // 3. Only reserve what hasn't been settled yet
            val netReservedTotal  = (splitReservedTotal  - paidTotal ).coerceAtLeast(0.0)
            val netReservedLiquid = (splitReservedLiquid - paidLiquid).coerceAtLeast(0.0)
            total  = round(total  - netReservedTotal)
            liquid = round(liquid - netReservedLiquid)

            // 4. Privatrechnung / Cathleen adjustments (unchanged)
            s.transactions.filter { it.type == "expense" && isPrivateRechnung(it.description) }.forEach { t ->
                total = round(total + t.amount)
                val acc = s.accounts.find { a -> a.id == t.accountId }
                if (acc?.category == "Liquide Mittel") liquid = round(liquid + t.amount)
            }
        }
        return Totals(total, liquid)
    }

    // ---- Derived: Analysis ----
    fun analysis(): Analysis {
        val txs = filteredTransactions()
        val s = _state.value

        fun isInvestment(accId: String): Boolean {
            val acc = s.accounts.find { it.id == accId }
            return acc?.category == "Investments" || acc?.category == "Vorsorge"
        }

        val dashTxs = txs.filter { !isInvestment(it.accountId) }
        val expenses = dashTxs.filter { it.type == "expense" }.sumOf { it.amount }
        val income = dashTxs.filter { it.type == "income" }.sumOf { it.amount }

        val netRevenue = dashTxs.filter { it.type == "income" && it.description.trim().lowercase().startsWith("rgnr") }
            .sumOf { t ->
                val share = if (t.isSplit) {
                    if (t.splitMode == "partner") t.amount else t.amount / 2
                } else t.amount
                share / 1.19
            }

        val splitStartDate = s.splitPotStartDate
        val inSplitRange: (Transaction) -> Boolean = { t ->
            splitStartDate.isEmpty() || t.date >= splitStartDate
        }
        val splitExpenses = txs.filter { it.isSplit && it.type == "expense" && inSplitRange(it) }.sumOf { t ->
            if (t.splitMode == "partner") t.amount else round(t.amount / 2)
        }
        val splitIncome = txs.filter { it.isSplit && it.type == "income" && inSplitRange(it) }.sumOf { t ->
            if (t.splitMode == "partner") t.amount else round(t.amount / 2)
        }
        // All-time split totals (not filtered by period) for the persistent SplitPot card
        val allTxs = s.transactions
        val totalSplitExp = allTxs.filter { it.isSplit && it.type == "expense" && inSplitRange(it) }.sumOf { t ->
            if (t.splitMode == "partner") t.amount else round(t.amount / 2)
        }
        val totalSplitInc = allTxs.filter { it.isSplit && it.type == "income" && inSplitRange(it) }.sumOf { t ->
            if (t.splitMode == "partner") t.amount else round(t.amount / 2)
        }
        val totalSettlements = allTxs.filter { it.isSettlement && (splitStartDate.isEmpty() || it.date >= splitStartDate) }.sumOf { it.amount }
        return Analysis(expenses, income, splitExpenses, splitIncome, netRevenue,
            totalSplitExp, totalSplitInc, totalSettlements)
    }

    // ---- Derived: Comparison ----
    fun comparisonData(): ComparisonData {
        val s = _state.value
        val res1 = balancesAtDate(s.compDate1)
        val res2 = balancesAtDate(s.compDate2)
        val diffTotal = round(res2.total - res1.total)
        val diffLiquid = round(res2.liquid - res1.liquid)
        val percentTotal = if (res1.total != 0.0) (diffTotal / res1.total) * 100 else 0.0
        val percentLiquid = if (res1.liquid != 0.0) (diffLiquid / res1.liquid) * 100 else 0.0
        return ComparisonData(res1, res2, diffTotal, percentTotal, diffLiquid, percentLiquid)
    }

    private fun balancesAtDate(targetDate: String): Totals {
        val s = _state.value
        val tempAccounts = s.accounts.map { it.copy() }.toMutableList()
        val futureTx = s.transactions.filter { it.date > targetDate }

        futureTx.forEach { tx ->
            when (tx.type) {
                "expense" -> tempAccounts.indexOfFirst { it.id == tx.accountId }.takeIf { it >= 0 }
                    ?.let { i -> tempAccounts[i] = tempAccounts[i].copy(balance = round(tempAccounts[i].balance + tx.amount)) }
                "income" -> tempAccounts.indexOfFirst { it.id == tx.accountId }.takeIf { it >= 0 }
                    ?.let { i -> tempAccounts[i] = tempAccounts[i].copy(balance = round(tempAccounts[i].balance - tx.amount)) }
                "transfer" -> {
                    tempAccounts.indexOfFirst { it.id == tx.accountId }.takeIf { it >= 0 }
                        ?.let { i -> tempAccounts[i] = tempAccounts[i].copy(balance = round(tempAccounts[i].balance + tx.amount)) }
                    tx.toAccountId?.let { toId ->
                        tempAccounts.indexOfFirst { it.id == toId }.takeIf { it >= 0 }
                            ?.let { i -> tempAccounts[i] = tempAccounts[i].copy(balance = round(tempAccounts[i].balance - tx.amount)) }
                    }
                }
            }
        }

        var total = tempAccounts.sumOf { it.balance }
        var liquid = tempAccounts.filter { it.category == "Liquide Mittel" }.sumOf { it.balance }

        if (!s.countFullSplitIncome) {
            // Reservations up to targetDate
            var splitResTotal = 0.0; var splitResLiquid = 0.0
            s.transactions.filter { it.isSplit && it.type == "income" && it.date <= targetDate }.forEach { t ->
                val share = if (t.splitMode == "partner") t.amount else round(t.amount / 2)
                splitResTotal = round(splitResTotal + share)
                val acc = tempAccounts.find { a -> a.id == t.accountId }
                if (acc?.category == "Liquide Mittel") splitResLiquid = round(splitResLiquid + share)
            }
            // Settlements paid up to targetDate
            val paidT = s.transactions.filter { it.isSettlement && it.date <= targetDate }.sumOf { it.amount }
            val paidL = s.transactions.filter { it.isSettlement && it.date <= targetDate }.sumOf { t ->
                val acc = tempAccounts.find { a -> a.id == t.accountId }
                if (acc?.category == "Liquide Mittel") t.amount else 0.0
            }
            total  = round(total  - (splitResTotal  - paidT).coerceAtLeast(0.0))
            liquid = round(liquid - (splitResLiquid - paidL).coerceAtLeast(0.0))

            s.transactions.filter { it.type == "expense" && it.date <= targetDate && isPrivateRechnung(it.description) }.forEach { t ->
                total = round(total + t.amount)
                val acc = tempAccounts.find { a -> a.id == t.accountId }
                if (acc?.category == "Liquide Mittel") liquid = round(liquid + t.amount)
            }
        }
        return Totals(total, liquid)
    }

    // ---- Transaction CRUD ----
    fun saveTransaction(
        id: String?, type: String, amount: Double, description: String,
        categoryId: String, accountId: String, toAccountId: String?,
        isSplit: Boolean, splitMode: String, date: String,
        isSettlement: Boolean = false
    ) = viewModelScope.launch {
        val txId = id ?: UUID.randomUUID().toString().replace("-", "").take(9)

        // If editing: revert the OLD transaction's balance effect first
        if (id != null) {
            repo.getTransactionById(id)?.let { old ->
                applyBalanceDelta(old, revert = true)
            }
        }

        // Save the new/updated transaction
        val tx = Transaction(
            txId, type, amount, description, categoryId,
            accountId, toAccountId, isSplit, splitMode, date, isSettlement
        )
        repo.upsertTransaction(tx)

        // Apply balance effect using FRESH balance from DB
        applyBalanceDelta(tx, revert = false)
    }

    fun deleteTransaction(id: String) = viewModelScope.launch {
        val tx = repo.getTransactionById(id) ?: return@launch
        applyBalanceDelta(tx, revert = true)
        repo.deleteTransaction(id)
    }

    // Always reads the CURRENT balance from DB — never uses stale snapshot
    private suspend fun applyBalanceDelta(tx: Transaction, revert: Boolean) {
        val sign = if (revert) -1 else 1

        suspend fun update(accId: String, delta: Double) {
            val fresh = repo.getAccountById(accId) ?: return
            repo.updateAccountBalance(accId, round(fresh.balance + delta))
        }

        when (tx.type) {
            "income"   -> update(tx.accountId, sign * tx.amount)
            "expense"  -> update(tx.accountId, -sign * tx.amount)
            "transfer" -> {
                update(tx.accountId, -sign * tx.amount)
                tx.toAccountId?.let { update(it, sign * tx.amount) }
            }
        }
    }

    // ---- Account CRUD ----
    fun saveAccount(id: String?, name: String, balance: Double, category: String, type: String, icon: String) =
        viewModelScope.launch {
            val acc = Account(
                id = id ?: UUID.randomUUID().toString().replace("-", "").take(9),
                name = name, balance = balance, category = category, type = type, icon = icon
            )
            repo.upsertAccount(acc)
        }

    fun deleteAccount(id: String) = viewModelScope.launch { repo.deleteAccount(id) }

    // ---- Investment update ----
    fun updateInvestment(account: Account, newBalance: Double) = viewModelScope.launch {
        val diff = round(newBalance - account.balance)
        if (diff != 0.0) {
            val s = _state.value
            val txId = UUID.randomUUID().toString().replace("-", "").take(9)
            val tx = Transaction(
                id = txId,
                type = if (diff > 0) "income" else "expense",
                amount = Math.abs(diff),
                description = "Kursanpassung",
                categoryId = s.categories.firstOrNull()?.id ?: "",
                accountId = account.id,
                isSplit = false,
                splitMode = "half",
                date = today()
            )
            repo.upsertTransaction(tx)
            val newHistory = account.history + HistoryEntry(today(), newBalance)
            repo.updateAccountBalance(account.id, newBalance)
            repo.updateInvestmentHistory(account.id, newHistory)
        }
    }

    // ---- Category CRUD ----
    fun saveCategory(id: String?, name: String, color: String, type: String) = viewModelScope.launch {
        val cat = Category(
            id = id ?: UUID.randomUUID().toString().replace("-", "").take(9),
            name = name, color = color, type = type
        )
        repo.upsertCategory(cat)
    }

    fun deleteCategory(id: String) = viewModelScope.launch { repo.deleteCategory(id) }

    // ---- Standing Order CRUD ----
    fun saveStandingOrder(
        id: String?, type: String, amount: Double, description: String,
        categoryId: String, accountId: String, toAccountId: String?,
        isSplit: Boolean, splitMode: String, interval: String, nextRun: String
    ) = viewModelScope.launch {
        val order = StandingOrder(
            id = id ?: UUID.randomUUID().toString().replace("-", "").take(9),
            type = type, amount = amount, description = description,
            categoryId = categoryId, accountId = accountId, toAccountId = toAccountId,
            isSplit = isSplit, splitMode = splitMode, interval = interval, nextRun = nextRun
        )
        repo.upsertStandingOrder(order)
    }

    fun deleteStandingOrder(id: String) = viewModelScope.launch { repo.deleteStandingOrder(id) }

    // ---- Savings Goals CRUD ----
    fun saveSavingsGoal(
        id: String?, name: String, targetAmount: Double,
        savedAmount: Double, deadline: String, color: String
    ) = viewModelScope.launch {
        val goal = SavingsGoal(
            id = id ?: UUID.randomUUID().toString().replace("-", "").take(9),
            name = name, targetAmount = targetAmount, savedAmount = savedAmount,
            deadline = deadline, color = color
        )
        repo.upsertSavingsGoal(goal)
    }

    fun deleteSavingsGoal(id: String) = viewModelScope.launch { repo.deleteSavingsGoal(id) }

    // ---- Export / Import ----
    fun exportData(context: Context, onResult: (Uri?) -> Unit) = viewModelScope.launch {
        val s = _state.value
        val settingsMap = mapOf(
            "isDarkMode" to s.isDarkMode,
            "splitPotEnabled" to s.splitPotEnabled,
            "countFullSplitIncome" to s.countFullSplitIncome
        )
        val json = repo.exportToJson(s.accounts, s.transactions, s.categories, s.standingOrders, s.savingsGoals, settingsMap)
        try {
            val fileName = "FinanzBackup_${today()}.json"
            val file = java.io.File(context.cacheDir, fileName)
            file.writeText(json)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
            onResult(uri)
        } catch (e: Exception) {
            onResult(null)
        }
    }

    fun importData(context: Context, uri: Uri, onResult: (Boolean) -> Unit) = viewModelScope.launch {
        try {
            val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
            val success = repo.importFromJson(json)
            onResult(success)
        } catch (e: Exception) {
            onResult(false)
        }
    }

    fun showSnackbar(message: String) = _state.update { it.copy(snackbarMessage = message) }
    fun clearSnackbar() = _state.update { it.copy(snackbarMessage = null) }

    companion object {
        private fun isPrivateRechnung(desc: String): Boolean {
            val lower = desc.trim().lowercase()
            return lower.startsWith("privatrechnung") || lower.startsWith("rechnung cathleen")
        }
    }
}

class FinanzViewModelFactory(
    private val repo: FinanzRepository,
    private val settings: SettingsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return FinanzViewModel(repo, settings) as T
    }
}

fun today(): String {
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    return LocalDate.now().format(fmt)
}

fun monthAgo(): String {
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    return LocalDate.now().minusMonths(1).format(fmt)
}
