package com.naman.brainback.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [BlockEvent::class], version = 1, exportSchema = false)
abstract class BrainbackDatabase : RoomDatabase() {
    abstract fun blockEventDao(): BlockEventDao

    companion object {
        @Volatile
        private var INSTANCE: BrainbackDatabase? = null

        fun getDatabase(context: Context): BrainbackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BrainbackDatabase::class.java,
                    "brainback_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
