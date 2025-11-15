package com.easy.monthlyexpensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easy.monthlyexpensetracker.data.CycleRecord
import com.easy.monthlyexpensetracker.screens.ConstellationChartScreen
import com.easy.monthlyexpensetracker.ui.theme.MonthlyExpenseTrackerTheme
import com.easy.monthlyexpensetracker.viewmodel.CyclesViewModel

class ConstellationChartActivity : ComponentActivity() {
    private val cyclesViewModel: CyclesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MonthlyExpenseTrackerTheme {
                val cycles by cyclesViewModel.cycles.collectAsStateWithLifecycle()
                val snapshot by produceState(initialValue = emptyList<Pair<CycleRecord, Double>>(), cycles) {
                    value = cyclesViewModel.chartSnapshot()
                }
                ConstellationChartScreen(
                    points = snapshot,
                    onBack = { onBackPressedDispatcher.onBackPressed() }
                )
            }
        }
    }
}

