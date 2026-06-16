package com.example.finanzmanager.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val balance: Double,
    val category: String,
    val type: String,
    val icon: String,
    val historyJson: String = "[]",
    val interestRate: Double = 0.0,         // annual rate in %, 0 = disabled
    val interestInterval: String = "monthly", // "monthly", "quarterly", "yearly"
    val nextInterestRun: String = ""          // "" = disabled, "yyyy-MM-dd" = next scheduled run
)
