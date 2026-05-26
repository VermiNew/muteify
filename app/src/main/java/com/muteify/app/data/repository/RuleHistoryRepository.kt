package com.muteify.app.data.repository

import android.content.Context
import com.muteify.app.data.model.RuleHistoryEntity
import kotlinx.coroutines.flow.Flow

class RuleHistoryRepository(context: Context) {

    private val ruleHistoryDao = AppDatabase.getInstance(context).ruleHistoryDao()

    val recentEvents: Flow<List<RuleHistoryEntity>> =
        ruleHistoryDao.getRecentEvents(RECENT_HISTORY_EVENTS)

    suspend fun recordEvent(
        source: String,
        triggerState: String,
        action: String,
        policy: String,
        outcome: String,
        details: String
    ) {
        ruleHistoryDao.insertEvent(
            RuleHistoryEntity(
                occurredAtMillis = System.currentTimeMillis(),
                source = source,
                triggerState = triggerState,
                action = action,
                policy = policy,
                outcome = outcome,
                details = details
            )
        )
        ruleHistoryDao.keepMostRecent(MAX_HISTORY_EVENTS)
    }

    private companion object {
        const val RECENT_HISTORY_EVENTS = 10
        const val MAX_HISTORY_EVENTS = 100
    }
}
