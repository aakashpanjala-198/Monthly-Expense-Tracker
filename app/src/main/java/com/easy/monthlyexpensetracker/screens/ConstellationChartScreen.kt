package com.easy.monthlyexpensetracker.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.easy.monthlyexpensetracker.data.CycleRecord
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
                label = "${monthLabel(it.first.month).take(3)} ${it.first.year.toString().takeLast(2)}",
                income = it.first.income,
                expenses = it.second
            )
        }
    }
    var windowStart by remember { mutableIntStateOf(0) }
    val windowSize = 4
    val windowed = remember(sorted, windowStart) {
        if (sorted.isEmpty()) emptyList()
        else sorted.drop(windowStart).take(windowSize)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = palette.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            palette.surfaceVariant,
                            palette.background
                        )
                    )
                )
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
                    text = "Constellation chart",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = { /* no-op */ }) {
                    Icon(Icons.Default.AutoGraph, contentDescription = null)
                }
            }
            Text(
                text = "Drag horizontally to travel through past cycles. Each constellation compares income vs outgoings.",
                style = MaterialTheme.typography.bodyMedium
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(sorted.size) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            if (dragAmount > 0 && windowStart > 0) {
                                windowStart -= 1
                            } else if (dragAmount < 0 && windowStart + windowSize < sorted.size) {
                                windowStart += 1
                            }
                        }
                    }
                    .background(
                        Brush.radialGradient(
                            listOf(
                                palette.surface,
                                palette.surfaceVariant
                            )
                        ),
                        shape = MaterialTheme.shapes.extraLarge
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                OrbitChart(points = windowed, palette = palette)
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
private fun OrbitChart(points: List<ChartPoint>, palette: ColorScheme) {
    if (points.isEmpty()) {
        Text("Not enough data yet.", style = MaterialTheme.typography.bodyLarge)
        return
    }

    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(8.dp)
            .size(320.dp)
    ) {
        val maxValue = (points.maxOf { max(it.income, it.expenses) } * 1.1).coerceAtLeast(1.0)
        val stepX = size.width / (points.size + 1)
        val baselineY = size.height - 40f
        val topPadding = 40f

        // axes
        drawLine(
            color = palette.onSurface.copy(alpha = 0.3f),
            start = Offset(0f, baselineY),
            end = Offset(size.width, baselineY),
            strokeWidth = 2f
        )
        drawLine(
            color = palette.onSurface.copy(alpha = 0.2f),
            start = Offset(40f, size.height),
            end = Offset(40f, topPadding),
            strokeWidth = 2f
        )

        // grid
        val gridLines = 4
        repeat(gridLines) { index ->
            val y = topPadding + (baselineY - topPadding) / gridLines * index
            drawLine(
                color = palette.onSurface.copy(alpha = 0.1f),
                start = Offset(40f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }

        val incomePath = Path()
        val expensePath = Path()

        points.forEachIndexed { idx, point ->
            val x = 40f + stepX * (idx + 1)
            val incomeRatio = (point.income / maxValue).toFloat()
            val expensesRatio = (point.expenses / maxValue).toFloat()
            val incomeY = baselineY - (baselineY - topPadding) * incomeRatio
            val expensesY = baselineY - (baselineY - topPadding) * expensesRatio

            if (idx == 0) {
                incomePath.moveTo(x, incomeY)
                expensePath.moveTo(x, expensesY)
            } else {
                incomePath.lineTo(x, incomeY)
                expensePath.lineTo(x, expensesY)
            }

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

            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    point.label,
                    x - 20f,
                    size.height - 8f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 28f
                    }
                )
            }
        }

        drawPath(
            path = incomePath,
            color = Color(0xFF8A5CF4),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f, cap = StrokeCap.Round)
        )
        drawPath(
            path = expensePath,
            color = Color(0xFFFF6584),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f, cap = StrokeCap.Round)
        )
    }
}

