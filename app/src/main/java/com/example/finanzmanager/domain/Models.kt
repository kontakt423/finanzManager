package com.example.finanzmanager.domain

data class Account(
    val id: String,
    val name: String,
    val balance: Double,
    val category: String,
    val type: String,
    val icon: String,
    val history: List<HistoryEntry> = emptyList(),
    val interestRate: Double = 0.0,
    val interestInterval: String = "monthly",
    val nextInterestRun: String = ""
)

data class Transaction(
    val id: String,
    val type: String,         // "expense", "income", "transfer"
    val amount: Double,
    val description: String,
    val categoryId: String,
    val accountId: String,
    val toAccountId: String? = null,
    val isSplit: Boolean = false,
    val splitMode: String = "half",
    val date: String,
    val isSettlement: Boolean = false
)

data class Category(
    val id: String,
    val name: String,
    val color: String,
    val type: String
)

data class StandingOrder(
    val id: String,
    val type: String,
    val amount: Double,
    val description: String,
    val categoryId: String,
    val accountId: String,
    val toAccountId: String? = null,
    val isSplit: Boolean = false,
    val splitMode: String = "half",
    val interval: String,     // "weekly", "monthly", "yearly"
    val nextRun: String
)

data class HistoryEntry(
    val date: String,
    val value: Double
)

data class Totals(
    val total: Double,
    val liquid: Double
)

data class ComparisonData(
    val res1: Totals,
    val res2: Totals,
    val diffTotal: Double,
    val percentTotal: Double,
    val diffLiquid: Double,
    val percentLiquid: Double
)

data class Analysis(
    val expenses: Double,
    val income: Double,
    val splitExpenses: Double,       // filtered period
    val splitIncome: Double,         // filtered period
    val netRevenue: Double,
    val totalSplitExpenses: Double,  // all time - what partner owes you
    val totalSplitIncome: Double,    // all time - what you owe partner
    val totalSettlements: Double     // all time - already paid out
)

data class SavingsGoal(
    val id: String,
    val name: String,
    val targetAmount: Double,
    val savedAmount: Double,
    val deadline: String = "",   // "" = no deadline, otherwise "yyyy-MM-dd"
    val color: String = "#3b82f6"
)
