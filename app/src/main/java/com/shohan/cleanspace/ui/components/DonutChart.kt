package com.shohan.cleanspace.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

data class DonutSlice(val value: Float, val color: Color, val label: String)

@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    centerLabel: String = "",
    centerSubLabel: String = ""
) {
    val total = slices.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(0.001f)
    Box(modifier = modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(200.dp)) {
            val strokeWidth = 36.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f
            )
            var startAngle = -90f
            slices.forEach { slice ->
                val sweep = (slice.value / total) * 360f
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = sweep.coerceAtLeast(0.5f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
                startAngle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = centerLabel, style = MaterialTheme.typography.titleLarge)
            Text(text = centerSubLabel, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
