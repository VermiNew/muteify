package com.muteify.app.data.repository

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.muteify.app.data.model.RuleEntity
import com.muteify.app.data.model.RuleHistoryEntity

@Database(entities = [RuleEntity::class, RuleHistoryEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ruleDao(): RuleDao
    abstract fun ruleHistoryDao(): RuleHistoryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS rule_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        occurredAtMillis INTEGER NOT NULL,
                        source TEXT NOT NULL,
                        triggerState TEXT NOT NULL,
                        action TEXT NOT NULL,
                        policy TEXT NOT NULL,
                        outcome TEXT NOT NULL,
                        details TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "muteify.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
