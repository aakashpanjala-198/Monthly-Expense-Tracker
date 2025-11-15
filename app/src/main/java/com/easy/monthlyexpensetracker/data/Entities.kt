package com.easy.monthlyexpensetracker.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a budgeting cycle (one calendar month).
 */
@Entity(
    tableName = "cycles",
    indices = [Index(value = ["year", "month"], unique = true)]
)
data class CycleRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String = "",
    val year: Int,
    val month: Int,
    val income: Double,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

/**
 * Represents a single outgoing transaction within a cycle.
 */
@Entity(
    tableName = "ledger_entries",
    foreignKeys = [
        ForeignKey(
            entity = CycleRecord::class,
            parentColumns = ["id"],
            childColumns = ["cycle_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("cycle_id"), Index("spent_at")]
)
data class ExpenseEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "cycle_id") val cycleId: Long,
    val title: String,
    val amount: Double,
    val category: String = "",
    @ColumnInfo(name = "spent_at") val spentAt: Long = System.currentTimeMillis()
)


