package com.example.finanzmanager.data.database.dao

import androidx.room.*
import com.example.finanzmanager.data.database.entities.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY name ASC")
    fun getAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<AccountEntity>)

    @Update
    suspend fun update(account: AccountEntity)

    @Delete
    suspend fun delete(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE accounts SET balance = :balance WHERE id = :id")
    suspend fun updateBalance(id: String, balance: Double)

    @Query("UPDATE accounts SET historyJson = :historyJson WHERE id = :id")
    suspend fun updateHistory(id: String, historyJson: String)

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()
}
