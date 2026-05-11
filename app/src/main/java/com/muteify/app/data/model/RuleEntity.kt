package com.muteify.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val wifiSsid: String,
    val actionEnter: String,
    val actionLeave: String,
    val isEnabled: Boolean = true
)
