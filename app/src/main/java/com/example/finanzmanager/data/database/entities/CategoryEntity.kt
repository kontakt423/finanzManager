package com.example.finanzmanager.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: String,   // Hex color e.g. "#ef4444"
    val type: String     // "expense" or "income"
)
