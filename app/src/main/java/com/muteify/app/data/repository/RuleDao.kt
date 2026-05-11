package com.muteify.app.data.repository

import androidx.room.*
import com.muteify.app.data.model.RuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Query("SELECT * FROM rules")
    fun getAllRules(): Flow<List<RuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: RuleEntity): Long

    @Delete
    suspend fun deleteRule(rule: RuleEntity)

    @Update
    suspend fun updateRule(rule: RuleEntity)
}
