package com.monica.monthlyexpensetracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CycleDao {
    @Query("SELECT * FROM cycles ORDER BY year DESC, month DESC")
    fun watchAll(): Flow<List<CycleRecord>>

    @Query("SELECT * FROM cycles ORDER BY year DESC, month DESC")
    suspend fun fetchAll(): List<CycleRecord>

    @Query("SELECT * FROM cycles WHERE id = :id")
    fun watchOne(id: Long): Flow<CycleRecord?>

    @Query("SELECT * FROM cycles WHERE id = :id")
    suspend fun findOne(id: Long): CycleRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: CycleRecord): Long

    @Update
    suspend fun update(record: CycleRecord)

    @Delete
    suspend fun delete(record: CycleRecord)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM ledger_entries WHERE cycle_id = :cycleId ORDER BY spent_at DESC, id DESC")
    fun watchForCycle(cycleId: Long): Flow<List<ExpenseEntry>>

    @Query("SELECT * FROM ledger_entries WHERE cycle_id = :cycleId ORDER BY spent_at DESC, id DESC")
    suspend fun fetchForCycle(cycleId: Long): List<ExpenseEntry>

    @Query("SELECT SUM(amount) FROM ledger_entries WHERE cycle_id = :cycleId")
    fun watchTotal(cycleId: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM ledger_entries WHERE cycle_id = :cycleId")
    suspend fun fetchTotal(cycleId: Long): Double?

    @Query("SELECT * FROM ledger_entries WHERE id = :id")
    suspend fun findOne(id: Long): ExpenseEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ExpenseEntry): Long

    @Update
    suspend fun update(entry: ExpenseEntry)

    @Delete
    suspend fun delete(entry: ExpenseEntry)
}





