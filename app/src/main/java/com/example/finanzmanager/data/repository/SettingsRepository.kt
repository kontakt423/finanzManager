package com.example.finanzmanager.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val DARK_MODE               = booleanPreferencesKey("dark_mode")
        val SPLIT_POT_ENABLED       = booleanPreferencesKey("split_pot_enabled")
        val COUNT_FULL_SPLIT_INCOME = booleanPreferencesKey("count_full_split_income")
        // Empty string = "count from all time", any "yyyy-MM-dd" = count from that date
        val SPLIT_POT_START_DATE    = stringPreferencesKey("split_pot_start_date")
        val APP_LOCK_ENABLED        = booleanPreferencesKey("app_lock_enabled")
    }

    val isDarkMode:          Flow<Boolean> = context.dataStore.data.map { it[DARK_MODE]               ?: true  }
    val isSplitPotEnabled:   Flow<Boolean> = context.dataStore.data.map { it[SPLIT_POT_ENABLED]       ?: true  }
    val countFullSplitIncome:Flow<Boolean> = context.dataStore.data.map { it[COUNT_FULL_SPLIT_INCOME] ?: false }
    val splitPotStartDate:   Flow<String>  = context.dataStore.data.map { it[SPLIT_POT_START_DATE]    ?: ""    }
    val appLockEnabled:      Flow<Boolean> = context.dataStore.data.map { it[APP_LOCK_ENABLED]        ?: false }

    suspend fun setDarkMode(value: Boolean)            { context.dataStore.edit { it[DARK_MODE]               = value } }
    suspend fun setSplitPotEnabled(value: Boolean)     { context.dataStore.edit { it[SPLIT_POT_ENABLED]       = value } }
    suspend fun setCountFullSplitIncome(value: Boolean){ context.dataStore.edit { it[COUNT_FULL_SPLIT_INCOME] = value } }
    suspend fun setSplitPotStartDate(date: String)     { context.dataStore.edit { it[SPLIT_POT_START_DATE]    = date  } }
    suspend fun setAppLockEnabled(value: Boolean)      { context.dataStore.edit { it[APP_LOCK_ENABLED]        = value } }
}
