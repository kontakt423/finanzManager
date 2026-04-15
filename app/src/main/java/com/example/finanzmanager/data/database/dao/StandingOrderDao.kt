package com.example.finanzmanager.data.database.dao

import androidx.room.*
import com.example.finanzmanager.data.database.entities.StandingOrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StandingOrderDao {
    @Query("SELECT * FROM standing_orders ORDER BY nextRun ASC")
    fun getAll(): Flow<List<StandingOrderEntity>>

    @Query("SELECT * FROM standing_orders")
    suspend fun getAllSync(): List<StandingOrderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: StandingOrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(orders: List<StandingOrderEntity>)

    @Update
    suspend fun update(order: StandingOrderEntity)

    @Delete
    suspend fun delete(order: StandingOrderEntity)

    @Query("DELETE FROM standing_orders WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM standing_orders")
    suspend fun deleteAll()
}
