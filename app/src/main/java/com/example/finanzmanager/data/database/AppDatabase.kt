package com.example.finanzmanager.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.finanzmanager.data.database.dao.*
import com.example.finanzmanager.data.database.entities.*

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        StandingOrderEntity::class,
        TemplateEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun standingOrderDao(): StandingOrderDao
    abstract fun templateDao(): TemplateDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE transactions ADD COLUMN isSettlement INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        // Legt die (inzwischen ungenutzte) savings_goals-Tabelle an. Das
        // Sparziel-Feature wurde entfernt, die Migration bleibt aber für die
        // Versionskontinuität erhalten – so kein Downgrade/Datenverlust bei
        // Installationen, die bereits auf DB-Version 3/4 waren. Room ignoriert
        // die Tabelle, da keine Entity mehr darauf zeigt.
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS savings_goals (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        targetAmount REAL NOT NULL,
                        savedAmount REAL NOT NULL,
                        deadline TEXT NOT NULL,
                        color TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN interestRate REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE accounts ADD COLUMN interestInterval TEXT NOT NULL DEFAULT 'monthly'")
                db.execSQL("ALTER TABLE accounts ADD COLUMN nextInterestRun TEXT NOT NULL DEFAULT ''")
            }
        }

        // Schnellbuchungs-Vorlagen
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS templates (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        amount REAL NOT NULL,
                        description TEXT NOT NULL,
                        categoryId TEXT NOT NULL,
                        accountId TEXT NOT NULL,
                        toAccountId TEXT,
                        isSplit INTEGER NOT NULL,
                        splitMode TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finanzmanager_db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
