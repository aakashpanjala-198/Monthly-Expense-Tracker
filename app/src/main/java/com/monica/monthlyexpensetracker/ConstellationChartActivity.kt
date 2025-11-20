package com.monica.monthlyexpensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.monica.monthlyexpensetracker.data.CycleRecord
import com.monica.monthlyexpensetracker.screens.ConstellationChartScreen
import com.monica.monthlyexpensetracker.ui.theme.MonthlyExpenseTrackerTheme
import com.monica.monthlyexpensetracker.viewmodel.CyclesViewModel

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

