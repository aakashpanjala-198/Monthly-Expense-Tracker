package com.monica.monthlyexpensetracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.monica.monthlyexpensetracker.screens.CycleDetailScreen
import com.monica.monthlyexpensetracker.ui.theme.MonthlyExpenseTrackerTheme
import com.monica.monthlyexpensetracker.viewmodel.CycleDetailViewModel
import com.monica.monthlyexpensetracker.viewmodel.CycleDetailViewModelFactory

class CycleDetailActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CYCLE_ID = "cycle_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cycleId = intent.getLongExtra(EXTRA_CYCLE_ID, -1L)
        if (cycleId == -1L) {
            finish()
            return
        }

        val viewModel: CycleDetailViewModel by viewModels {
            CycleDetailViewModelFactory(application, cycleId)
        }

        setContent {
            MonthlyExpenseTrackerTheme {
                val digest by viewModel.digest.collectAsStateWithLifecycle()

                CycleDetailScreen(
                    digest = digest,
                    onUpPressed = { onBackPressedDispatcher.onBackPressed() },
                    onAdjustIncome = { income -> viewModel.adjustIncome(income) },
                    onUpdateCycle = { title, year, month ->
                        viewModel.updateCycleMeta(title, year, month)
                    },
                    onAddExpense = { title, amount, category, millis ->
                        viewModel.addExpense(title, amount, category, millis)
                    },
                    onUpdateExpense = { entry ->
                        viewModel.updateExpense(entry)
                    },
                    onDeleteExpense = { entry ->
                        viewModel.deleteExpense(entry)
                    },
                    onOpenChart = {
                        startActivity(
                            Intent(
                                this,
                                ConstellationChartActivity::class.java
                            )
                        )
                    }
                )
            }
        }
    }
}





