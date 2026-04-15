package com.example.finanzmanager.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "standing_orders")
data class StandingOrderEntity(
    @PrimaryKey val id: String,
    val type: String,           // "expense", "income", "transfer"
    val amount: Double,
    val description: String,
    val categoryId: String,
    val accountId: String,
    val toAccountId: String?,
    val isSplit: Boolean,
    val splitMode: String,      // "half", "partner"
    val interval: String,       // "weekly", "monthly", "yearly"
    val nextRun: String         // "yyyy-MM-dd"
)
