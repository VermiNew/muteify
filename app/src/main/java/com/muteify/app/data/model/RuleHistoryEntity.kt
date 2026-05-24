package com.muteify.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rule_history")
data class RuleHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val occurredAtMillis: Long,
    val source: String,
    val triggerState: String,
    val action: String,
    val policy: String,
    val outcome: String,
    val details: String
)
