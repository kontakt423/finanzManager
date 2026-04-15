package com.example.finanzmanager.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String,           // "expense", "income", "transfer"
    val amount: Double,
    val description: String,
    val categoryId: String,
    val accountId: String,
    val toAccountId: String?,
    val isSplit: Boolean,
    val splitMode: String,      // "half" (50%), "partner" (100%)
    val date: String,           // "yyyy-MM-dd"
    val isSettlement: Boolean = false  // marks a "Partnerausgleich" expense
)
