package com.example.finanzmanager.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val balance: Double,
    val category: String,   // "Liquide Mittel", "Investments", "Vorsorge"
    val type: String,       // "cash", "investment"
    val icon: String,       // "bank", "wallet", "stock"
    val historyJson: String = "[]"  // JSON array of {date, val} for investments
)
