package com.example.finanzmanager.data

import com.example.finanzmanager.data.database.entities.*
import com.example.finanzmanager.domain.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

private val gson = Gson()

fun AccountEntity.toDomain(): Account {
    val historyType = object : TypeToken<List<Map<String, Any>>>() {}.type
    val rawList: List<Map<String, Any>> = try {
        gson.fromJson(historyJson, historyType) ?: emptyList()
    } catch (e: Exception) { emptyList() }
    val history = rawList.mapNotNull { map ->
        val date = map["date"] as? String ?: return@mapNotNull null
        val value = (map["val"] as? Double) ?: (map["val"] as? Long)?.toDouble() ?: return@mapNotNull null
        HistoryEntry(date, value)
    }
    return Account(id, name, balance, category, type, icon, history, interestRate, interestInterval, nextInterestRun)
}

fun Account.toEntity(): AccountEntity {
    val historyList = history.map { mapOf("date" to it.date, "val" to it.value) }
    return AccountEntity(id, name, balance, category, type, icon, gson.toJson(historyList), interestRate, interestInterval, nextInterestRun)
}

fun TransactionEntity.toDomain() = Transaction(
    id, type, amount, description, categoryId, accountId, toAccountId, isSplit, splitMode, date, isSettlement
)
fun Transaction.toEntity() = TransactionEntity(
    id, type, amount, description, categoryId, accountId, toAccountId, isSplit, splitMode, date, isSettlement
)

fun CategoryEntity.toDomain() = Category(id, name, color, type)
fun Category.toEntity() = CategoryEntity(id, name, color, type)

fun StandingOrderEntity.toDomain() = StandingOrder(
    id, type, amount, description, categoryId, accountId, toAccountId, isSplit, splitMode, interval, nextRun
)
fun StandingOrder.toEntity() = StandingOrderEntity(
    id, type, amount, description, categoryId, accountId, toAccountId, isSplit, splitMode, interval, nextRun
)
