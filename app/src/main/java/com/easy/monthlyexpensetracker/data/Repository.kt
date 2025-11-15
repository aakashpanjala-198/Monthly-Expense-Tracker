package com.easy.monthlyexpensetracker.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar

data class CycleDigest(
    val record: CycleRecord,
    val expenses: List<ExpenseEntry>,
    val totalSpent: Double,
    val balance: Double
)

class OrbitRepository(
    private val cycleDao: CycleDao,
    private val expenseDao: ExpenseDao
)

{
    fun observeCycles(): Flow<List<CycleRecord>> = cycleDao.watchAll()

    fun observeDigest(cycleId: Long): Flow<CycleDigest?> {
        val recordFlow = cycleDao.watchOne(cycleId)
        val expenseFlow = expenseDao.watchForCycle(cycleId)
        val totalFlow = expenseDao.watchTotal(cycleId).map { it ?: 0.0 }
        return combine(recordFlow, expenseFlow, totalFlow) { record, expenses, total ->
            record?.let {
                CycleDigest(
                    record = it,
                    expenses = expenses,
                    totalSpent = total,
                    balance = it.income - total
                )
            }
        }
    }

    suspend fun createCycle(
        title: String,
        year: Int? = null,
        month: Int? = null,
        income: Double = 0.0
    ): Long = withContext(Dispatchers.IO) {
        val now = Calendar.getInstance()
        val targetYear = year ?: now.get(Calendar.YEAR)
        val targetMonth = month ?: (now.get(Calendar.MONTH) + 1)
        val normalizedTitle = title.trim()
        val record = CycleRecord(
            title = normalizedTitle,
            year = targetYear,
            month = targetMonth,
            income = income
        )
        cycleDao.insert(record)
    }

    suspend fun updateCycle(record: CycleRecord) = withContext(Dispatchers.IO) {
        cycleDao.update(record)
    }

    suspend fun deleteCycle(record: CycleRecord) = withContext(Dispatchers.IO) {
        cycleDao.delete(record)
    }

    suspend fun getCycle(id: Long): CycleRecord? = withContext(Dispatchers.IO) {
        cycleDao.findOne(id)
    }

    suspend fun addExpense(
        cycleId: Long,
        title: String,
        amount: Double,
        category: String,
        millis: Long = System.currentTimeMillis()
    ): Long = withContext(Dispatchers.IO) {
        val entry = ExpenseEntry(
            cycleId = cycleId,
            title = title.trim(),
            amount = amount,
            category = category.trim(),
            spentAt = millis
        )
        expenseDao.insert(entry)
    }

    suspend fun updateExpense(entry: ExpenseEntry) = withContext(Dispatchers.IO) {
        expenseDao.update(entry)
    }

    suspend fun deleteExpense(entry: ExpenseEntry) = withContext(Dispatchers.IO) {
        expenseDao.delete(entry)
    }

    suspend fun getExpense(id: Long): ExpenseEntry? = withContext(Dispatchers.IO) {
        expenseDao.findOne(id)
    }

    suspend fun loadFullSnapshot(): List<Pair<CycleRecord, Double>> = withContext(Dispatchers.IO) {
        val cycles = cycleDao.fetchAll()
        cycles.map { it to (expenseDao.fetchTotal(it.id) ?: 0.0) }
    }
}


