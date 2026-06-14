package com.example.finanzmanager.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val targetAmount: Double,
    val savedAmount: Double,
    val deadline: String,
    val color: String
)
