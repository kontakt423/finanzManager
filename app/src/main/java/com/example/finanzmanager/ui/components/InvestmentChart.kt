package com.example.finanzmanager.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finanzmanager.domain.HistoryEntry
import com.example.finanzmanager.ui.theme.PrimaryBlue

@Composable
fun InvestmentChart(
    history: List<HistoryEntry>,
    modifier: Modifier = Modifier
) {
    if (history.size < 2) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Keine Verlaufsdaten",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
        return
    }

    val lineColor = PrimaryBlue
    val fillColor = PrimaryBlue.copy(alpha = 0.15f)
    val dotColor = PrimaryBlue
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        val w = size.width
        val h = size.height
        val values = history.map { it.value }
        val minVal = values.min()
        val maxVal = values.max()
        val range = if (maxVal - minVal < 1.0) 1.0 else maxVal - minVal

        fun xOf(index: Int) = if (history.size == 1) w / 2 else (index.toFloat() / (history.size - 1)) * w
        fun yOf(value: Double) = (h - ((value - minVal) / range).toFloat() * h).coerceIn(0f, h)

        // Grid lines
        repeat(4) { i ->
            val y = h / 3f * i
            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        }

        // Fill path
        val fillPath = Path()
        fillPath.moveTo(xOf(0), h)
        history.forEachIndexed { i, entry ->
            fillPath.lineTo(xOf(i), yOf(entry.value))
        }
        fillPath.lineTo(xOf(history.size - 1), h)
        fillPath.close()
        drawPath(fillPath, brush = Brush.verticalGradient(
            colors = listOf(fillColor, Color.Transparent), startY = 0f, endY = h
        ))

        // Line path
        val linePath = Path()
        history.forEachIndexed { i, entry ->
            if (i == 0) linePath.moveTo(xOf(0), yOf(entry.value))
            else linePath.lineTo(xOf(i), yOf(entry.value))
        }
        drawPath(linePath, color = lineColor, style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Dots
        history.forEachIndexed { i, entry ->
            drawCircle(Color.White, radius = 5f, center = Offset(xOf(i), yOf(entry.value)))
            drawCircle(dotColor, radius = 4f, center = Offset(xOf(i), yOf(entry.value)))
        }
    }
}
