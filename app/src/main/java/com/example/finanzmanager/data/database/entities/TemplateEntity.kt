package com.example.finanzmanager.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey val id: String,
    val name: String,           // Anzeigename der Vorlage, z. B. "Tanken"
    val type: String,           // "expense", "income", "transfer"
    val amount: Double,
    val description: String,
    val categoryId: String,
    val accountId: String,
    val toAccountId: String?,
    val isSplit: Boolean,
    val splitMode: String       // "half", "partner"
)
