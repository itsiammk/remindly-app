package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities = [
    ReminderEntity::class,
    ReminderOffsetEntity::class,
    ReminderOccurrenceOverrideEntity::class,
    ReminderHistoryEntity::class,
  ],
  version = 3,
  exportSchema = false,
)
@TypeConverters(ReminderConverters::class)
abstract class RemindlyDatabase : RoomDatabase() {
  abstract fun reminderDao(): ReminderDao

  companion object {
    @Volatile private var instance: RemindlyDatabase? = null

    val MIGRATION_1_2 =
      object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            "ALTER TABLE reminders ADD COLUMN activeOverrideOriginalEpochMillis INTEGER"
          )
          db.execSQL(
            "ALTER TABLE reminders ADD COLUMN lastFiredOffsetForOccurrenceMillis INTEGER"
          )
        }
      }

    val MIGRATION_2_3 =
      object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `reminder_history` (
              `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              `reminderId` INTEGER NOT NULL,
              `reminderTitle` TEXT NOT NULL,
              `category` TEXT NOT NULL,
              `eventType` TEXT NOT NULL,
              `deliveryChannel` TEXT NOT NULL,
              `offsetLabel` TEXT NOT NULL,
              `occurrenceEpochMillis` INTEGER NOT NULL,
              `eventEpochMillis` INTEGER NOT NULL
            )
            """.trimIndent()
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_reminder_history_eventEpochMillis` ON `reminder_history` (`eventEpochMillis`)"
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_reminder_history_reminderId_occurrenceEpochMillis_eventType` ON `reminder_history` (`reminderId`, `occurrenceEpochMillis`, `eventType`)"
          )
        }
      }

    fun getInstance(context: Context): RemindlyDatabase =
      instance
        ?: synchronized(this) {
          instance
            ?: Room.databaseBuilder(
                context.applicationContext,
                RemindlyDatabase::class.java,
                "remindly.db",
              )
              .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
              .build()
              .also { instance = it }
        }
  }
}
