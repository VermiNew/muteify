package com.muteify.app.data.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.muteify.app.data.model.RuleHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleHistoryDao {
    @Query("SELECT * FROM rule_history ORDER BY occurredAtMillis DESC LIMIT :limit")
    fun getRecentEvents(limit: Int): Flow<List<RuleHistoryEntity>>

    @Insert
    suspend fun insertEvent(event: RuleHistoryEntity): Long

    @Query("DELETE FROM rule_history WHERE id NOT IN (SELECT id FROM rule_history ORDER BY occurredAtMillis DESC LIMIT :limit)")
    suspend fun keepMostRecent(limit: Int)
}
