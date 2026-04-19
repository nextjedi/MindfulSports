package com.ashutosh.mindfultennis.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashutosh.mindfultennis.domain.model.DurationFilter
import com.ashutosh.mindfultennis.ui.theme.SessionAverage
import com.ashutosh.mindfultennis.ui.theme.SessionGood
import com.ashutosh.mindfultennis.ui.theme.SessionPoor
import com.ashutosh.mindfultennis.ui.theme.Spacing
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

/**
 * Calendar heatmap (like LeetCode contribution graph) showing session activity.
 * Color-coded by average overall score on each day.
 * Supports month-by-month navigation.
 */
@Composable
fun CalendarHeatmap(
    dailyScores: Map<String, Int?>,
    selectedDuration: DurationFilter,
    modifier: Modifier = Modifier,
) {
    // Convert string keys ("YYYY-MM-DD") to LocalDate once
    val parsedScores: Map<LocalDate, Int?> = remember(dailyScores) {
        dailyScores.entries.associate { (key, value) ->
            LocalDate.parse(key) to value
        }
    }

    // Determine how many months we can go back based on filter
    val maxMonthsBack = remember(selectedDuration) {
        when (selectedDuration) {
            DurationFilter.ONE_WEEK -> 1
            DurationFilter.ONE_MONTH -> 1
            DurationFilter.THREE_MONTHS -> 3
            DurationFilter.SIX_MONTHS -> 6
            DurationFilter.ONE_YEAR -> 12
        }
    }

    var monthOffset by remember(selectedDuration) { mutableIntStateOf(0) } // 0 = current month

    val tz = remember { TimeZone.currentSystemDefault() }
    val now = remember { Clock.System.now().toLocalDateTime(tz) }
    val displayDate = remember(monthOffset, now) {
        val base = now.date
        if (monthOffset == 0) base
        else {
            val inst = Clock.System.now()
            val shifted = inst.minus(DateTimePeriod(months = monthOffset), tz)
            shifted.toLocalDateTime(tz).date
        }
    }
    val displayYear = displayDate.year
    val displayMonth = displayDate.month

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            // Header with month navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Activity",
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { if (monthOffset < maxMonthsBack - 1) monthOffset++ },
                        enabled = monthOffset < maxMonthsBack - 1,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous month",
                        )
                    }
                    Text(
                        text = "${displayMonth.name.lowercase().replaceFirstChar { it.uppercase() }} $displayYear",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    IconButton(
                        onClick = { if (monthOffset > 0) monthOffset-- },
                        enabled = monthOffset > 0,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next month",
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Calendar grid
            CalendarGrid(
                year = displayYear,
                month = displayMonth,
                dailyScores = parsedScores,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Legend
            HeatmapLegend()
        }
    }
}

@Composable
private fun CalendarGrid(
    year: Int,
    month: Month,
    dailyScores: Map<LocalDate, Int?>,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val dayLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant

    // Pre-compute calendar data
    val firstDay = LocalDate(year, month, 1)
    val daysInMonth = when (month) {
        Month.JANUARY, Month.MARCH, Month.MAY, Month.JULY,
        Month.AUGUST, Month.OCTOBER, Month.DECEMBER -> 31
        Month.APRIL, Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER -> 30
        Month.FEBRUARY -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        else -> 30
    }

    // DayOfWeek: MONDAY=1 .. SUNDAY=7, we want Mon=0..Sun=6
    val startDayOfWeek = (firstDay.dayOfWeek.ordinal) // Mon=0, Tue=1, ..., Sun=6
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

    val totalRows = (startDayOfWeek + daysInMonth + 6) / 7

    Canvas(
        modifier = modifier.height(((totalRows + 1) * 22 + 4).dp),
    ) {
        val cellSize = (size.width - 24.dp.toPx()) / 7f  // 24dp for left label space
        val cellPadding = 2.dp.toPx()
        val labelOffsetX = 0f
        val gridOffsetX = 24.dp.toPx()
        val rowHeight = 20.dp.toPx()
        val cornerRadius = 3.dp.toPx()

        // Draw day-of-week labels
        dayLabels.forEachIndexed { col, label ->
            val textResult = textMeasurer.measure(
                text = label,
                style = TextStyle(
                    fontSize = 10.sp,
                    color = dayLabelColor,
                    textAlign = TextAlign.Center,
                ),
            )
            drawText(
                textLayoutResult = textResult,
                topLeft = Offset(
                    x = gridOffsetX + col * cellSize + (cellSize - textResult.size.width) / 2f,
                    y = (rowHeight - textResult.size.height) / 2f,
                ),
            )
        }

        // Draw day cells
        for (day in 1..daysInMonth) {
            val dayIndex = startDayOfWeek + day - 1
            val col = dayIndex % 7
            val row = dayIndex / 7

            val date = LocalDate(year, month, day)
            val score = dailyScores[date]
            val hasSession = dailyScores.containsKey(date)

            val cellColor = when {
                !hasSession -> emptyColor
                score == null -> Color(0xFFBDBDBD) // Unrated session
                score >= 70 -> SessionGood
                score >= 40 -> SessionAverage
                else -> SessionPoor
            }

            val x = gridOffsetX + col * cellSize + cellPadding
            val y = (row + 1) * rowHeight + cellPadding

            drawRoundRect(
                color = cellColor,
                topLeft = Offset(x, y),
                size = Size(cellSize - cellPadding * 2, rowHeight - cellPadding * 2),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            )
        }
    }
}

@Composable
private fun HeatmapLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        LegendItem(color = MaterialTheme.colorScheme.surfaceVariant, label = "No session")
        Spacer(modifier = Modifier.width(Spacing.sm))
        LegendItem(color = SessionPoor, label = "<40")
        Spacer(modifier = Modifier.width(Spacing.sm))
        LegendItem(color = SessionAverage, label = "40-69")
        Spacer(modifier = Modifier.width(Spacing.sm))
        LegendItem(color = SessionGood, label = "≥70")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp)),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
