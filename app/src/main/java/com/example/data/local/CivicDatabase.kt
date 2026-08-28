package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        IncidentEntity::class,
        TimelineEventEntity::class,
        CitizenReportEntity::class,
        CommunityConfirmationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CivicDatabase : RoomDatabase() {
    abstract fun civicDao(): CivicDao

    companion object {
        @Volatile
        private var INSTANCE: CivicDatabase? = null

        fun getDatabase(context: Context): CivicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CivicDatabase::class.java,
                    "civicsense_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
