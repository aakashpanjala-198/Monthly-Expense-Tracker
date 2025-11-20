package com.monica.monthlyexpensetracker.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.monica.monthlyexpensetracker.data.CycleRecord
import kotlin.math.max

data class ChartPoint(
    val label: String,
    val income: Double,
    val expenses: Double
)

@Composable
fun ConstellationChartScreen(
    points: List<Pair<CycleRecord, Double>>,
    onBack: () -> Unit
) {
    val palette = MaterialTheme.colorScheme
    val sorted = remember(points) {
        points.sortedWith(
            compareByDescending<Pair<CycleRecord, Double>> { it.first.year }
                .thenByDescending { it.first.month }
        ).map {
            ChartPoint(
                label = monthLabel(it.first.month),
                income = it.first.income,
                expenses = it.second
            )
        }
    }
    
    // Take maximum 4 months
    val windowed = remember(sorted) {
        if (sorted.isEmpty()) emptyList()
        else sorted.take(4)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = palette.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Income/Expenses",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = { /* no-op */ }) {
                    Icon(Icons.Default.AutoGraph, contentDescription = null)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                IncomeExpensesChart(points = windowed, palette = palette)
            }
            LegendRow()
        }
    }
}

@Composable
private fun LegendRow() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendDot(color = Color(0xFF8A5CF4))
        Text("Income", style = MaterialTheme.typography.bodyMedium)
        LegendDot(color = Color(0xFFFF6584))
        Text("Expenses", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .background(color, shape = MaterialTheme.shapes.small)
    )
}

@Composable
private fun IncomeExpensesChart(points: List<ChartPoint>, palette: ColorScheme) {
    if (points.isEmpty()) {
        Text("Not enough data yet.", style = MaterialTheme.typography.bodyLarge)
        return
    }

    // Square aspect ratio chart
    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Square aspect ratio
            .padding(16.dp)
    ) {
        val paddingLeft = 80f
        val paddingRight = 20f
        val paddingTop = 40f
        val paddingBottom = 80f
        
        val chartWidth = size.width - paddingLeft - paddingRight
        val chartHeight = size.height - paddingTop - paddingBottom
        
        val maxValue = (points.maxOf { max(it.income, it.expenses) } * 1.1).coerceAtLeast(1.0)
        
        // Draw Y-axis (amount axis)
        drawLine(
            color = palette.onSurface,
            start = Offset(paddingLeft, paddingTop),
            end = Offset(paddingLeft, size.height - paddingBottom),
            strokeWidth = 3f
        )
        
        // Draw X-axis (months axis)
        drawLine(
            color = palette.onSurface,
            start = Offset(paddingLeft, size.height - paddingBottom),
            end = Offset(size.width - paddingRight, size.height - paddingBottom),
            strokeWidth = 3f
        )
        
        // Draw Y-axis labels (amount labels)
        val yAxisSteps = 4
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 32f
            textAlign = android.graphics.Paint.Align.RIGHT
        }
        
        repeat(yAxisSteps + 1) { step ->
            val y = paddingTop + (chartHeight / yAxisSteps) * (yAxisSteps - step)
            val value = (maxValue / yAxisSteps) * step
            val label = String.format("%.0f", value)
            
            // Draw grid line
            drawLine(
                color = palette.onSurface.copy(alpha = 0.1f),
                start = Offset(paddingLeft, y),
                end = Offset(size.width - paddingRight, y),
                strokeWidth = 1f
            )
            
            // Draw Y-axis label
            drawContext.canvas.nativeCanvas.drawText(
                label,
                paddingLeft - 10f,
                y + 10f,
                paint
            )
        }
        
        // Draw data points and lines
        val stepX = chartWidth / (points.size - 1).coerceAtLeast(1)
        val incomePath = Path()
        val expensePath = Path()

        points.forEachIndexed { idx, point ->
            val x = paddingLeft + stepX * idx
            val incomeRatio = (point.income / maxValue).toFloat()
            val expensesRatio = (point.expenses / maxValue).toFloat()
            val incomeY = (size.height - paddingBottom) - chartHeight * incomeRatio
            val expensesY = (size.height - paddingBottom) - chartHeight * expensesRatio

            if (idx == 0) {
                incomePath.moveTo(x, incomeY)
                expensePath.moveTo(x, expensesY)
            } else {
                incomePath.lineTo(x, incomeY)
                expensePath.lineTo(x, expensesY)
            }

            // Draw markers
            drawCircle(
                color = Color(0xFF8A5CF4),
                radius = 8f,
                center = Offset(x, incomeY)
            )
            drawCircle(
                color = Color(0xFFFF6584),
                radius = 8f,
                center = Offset(x, expensesY)
            )

            // Draw X-axis label (month)
            val monthPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 36f
                textAlign = android.graphics.Paint.Align.CENTER
            }
            drawContext.canvas.nativeCanvas.drawText(
                point.label,
                x,
                size.height - paddingBottom + 50f,
                monthPaint
            )
        }

        // Draw lines connecting points
        drawPath(
            path = incomePath,
            color = Color(0xFF8A5CF4),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f, cap = StrokeCap.Round)
        )
        drawPath(
            path = expensePath,
            color = Color(0xFFFF6584),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f, cap = StrokeCap.Round)
        )
    }
}

