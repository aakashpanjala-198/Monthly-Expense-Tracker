package com.easy.monthlyexpensetracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.easy.monthlyexpensetracker.data.CycleDigest
import com.easy.monthlyexpensetracker.data.CycleRecord
import com.easy.monthlyexpensetracker.data.ExpenseEntry
import com.easy.monthlyexpensetracker.data.OrbitDatabase
import com.easy.monthlyexpensetracker.data.OrbitRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CyclesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: OrbitRepository = OrbitRepository(
        OrbitDatabase.get(application).cycleDao(),
        OrbitDatabase.get(application).expenseDao()
    )

    val cycles: StateFlow<List<CycleRecord>> =
        repository.observeCycles()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun createCycleQuick(title: String, income: Double) {
        viewModelScope.launch {
            repository.createCycle(title = title, income = income)
        }
    }

    fun createCycleExplicit(title: String, year: Int, month: Int, income: Double) {
        viewModelScope.launch {
            repository.createCycle(title = title, year = year, month = month, income = income)
        }
    }

    fun updateCycle(record: CycleRecord) {
        viewModelScope.launch {
            repository.updateCycle(record)
        }
    }

    fun deleteCycle(record: CycleRecord) {
        viewModelScope.launch {
            repository.deleteCycle(record)
        }
    }

    suspend fun chartSnapshot(): List<Pair<CycleRecord, Double>> {
        return repository.loadFullSnapshot()
    }
}

class CycleDetailViewModel(
    application: Application,
    private val cycleId: Long
) : AndroidViewModel(application) {

    private val repository: OrbitRepository = OrbitRepository(
        OrbitDatabase.get(application).cycleDao(),
        OrbitDatabase.get(application).expenseDao()
    )

    val digest: StateFlow<CycleDigest?> =
        repository.observeDigest(cycleId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )

    fun adjustIncome(income: Double) {
        viewModelScope.launch {
            repository.getCycle(cycleId)?.let { record ->
                repository.updateCycle(record.copy(income = income))
            }
        }
    }

    fun updateCycleMeta(title: String, year: Int, month: Int) {
        viewModelScope.launch {
            repository.getCycle(cycleId)?.let { record ->
                repository.updateCycle(
                    record.copy(
                        title = title.trim(),
                        year = year,
                        month = month
                    )
                )
            }
        }
    }

    fun addExpense(title: String, amount: Double, category: String, millis: Long) {
        viewModelScope.launch {
            repository.addExpense(
                cycleId = cycleId,
                title = title,
                amount = amount,
                category = category,
                millis = millis
            )
        }
    }

    fun updateExpense(entry: ExpenseEntry) {
        viewModelScope.launch {
            repository.updateExpense(entry)
        }
    }

    fun deleteExpense(entry: ExpenseEntry) {
        viewModelScope.launch {
            repository.deleteExpense(entry)
        }
    }
}

class CycleDetailViewModelFactory(
    private val application: Application,
    private val cycleId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CycleDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CycleDetailViewModel(application, cycleId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

