package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [VirtualAppEntity::class], version = 1, exportSchema = false)
abstract class AtpDatabase : RoomDatabase() {
    abstract fun virtualAppDao(): VirtualAppDao

    companion object {
        @Volatile
        private var INSTANCE: AtpDatabase? = null

        fun getInstance(context: Context): AtpDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AtpDatabase::class.java,
                    "atp_vm.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
