package com.easy.monthlyexpensetracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CycleRecord::class, ExpenseEntry::class],
    version = 1,
    exportSchema = false
)
abstract class OrbitDatabase : RoomDatabase() {
    abstract fun cycleDao(): CycleDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: OrbitDatabase? = null

        fun get(context: Context): OrbitDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    OrbitDatabase::class.java,
                    "orbit-ledger.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}


