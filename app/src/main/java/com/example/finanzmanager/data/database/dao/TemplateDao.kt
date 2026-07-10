package com.example.finanzmanager.data.database.dao

import androidx.room.*
import com.example.finanzmanager.data.database.entities.TemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY name ASC")
    fun getAll(): Flow<List<TemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: TemplateEntity)

    @Query("DELETE FROM templates WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM templates")
    suspend fun deleteAll()
}
