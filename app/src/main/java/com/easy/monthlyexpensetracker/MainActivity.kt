package com.easy.monthlyexpensetracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easy.monthlyexpensetracker.screens.AuroraHomeScreen
import com.easy.monthlyexpensetracker.screens.HomeUiState
import com.easy.monthlyexpensetracker.screens.buildHomeUiState
import com.easy.monthlyexpensetracker.ui.theme.MonthlyExpenseTrackerTheme
import com.easy.monthlyexpensetracker.viewmodel.CyclesViewModel

class MainActivity : ComponentActivity() {

    private val cyclesViewModel: CyclesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MonthlyExpenseTrackerTheme {
                val cycles by cyclesViewModel.cycles.collectAsStateWithLifecycle()
                val uiState = remember(cycles) {
                    buildHomeUiState(cycles)
                }

                val context = LocalContext.current

                AuroraHomeScreen(
                    state = uiState,
                    onCreateQuick = { title, income ->
                        cyclesViewModel.createCycleQuick(title, income)
                    },
                    onCreateAdvanced = { title, year, month, income ->
                        cyclesViewModel.createCycleExplicit(title, year, month, income)
                    },
                    onOpenCycle = { id ->
                        context.startActivity(
                            Intent(context, CycleDetailActivity::class.java)
                                .putExtra(CycleDetailActivity.EXTRA_CYCLE_ID, id)
                        )
                    },
                    onEditCycle = { record ->
                        cyclesViewModel.updateCycle(record)
                    },
                    onDeleteCycle = { record ->
                        cyclesViewModel.deleteCycle(record)
                    },
                    onOpenChart = {
                        context.startActivity(Intent(context, ConstellationChartActivity::class.java))
                    }
                )
            }
        }
    }
}
