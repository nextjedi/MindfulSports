package com.ashutosh.mindfultennis.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ashutosh.mindfultennis.ui.home.components.AspectPerformanceCard
import com.ashutosh.mindfultennis.ui.home.components.CalendarHeatmap
import com.ashutosh.mindfultennis.ui.home.components.PerformanceChart
import com.ashutosh.mindfultennis.ui.home.components.TimeRangeSegmentedControl
import com.ashutosh.mindfultennis.ui.home.components.WinLossCard
import com.ashutosh.mindfultennis.ui.theme.Spacing
import com.ashutosh.mindfultennis.util.DateTimeUtils

/**
 * Home / Dashboard screen.
 * Displays performance trend chart, win/loss record, focus points,
 * and aspect performance averages. Bottom buttons for sessions navigation.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onStartSessionClicked: () -> Unit,
    onEndSessionClicked: (sessionId: String) -> Unit,
    onShowSessionsClicked: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    HomeScreenContent(
        state = uiState,
        onEvent = { event ->
            when (event) {
                is HomeUiEvent.StartSessionClicked -> onStartSessionClicked()
                is HomeUiEvent.EndSessionClicked -> {
                    uiState.activeSession?.let { onEndSessionClicked(it.id) }
                }
                is HomeUiEvent.ShowSessionsClicked -> onShowSessionsClicked()
                is HomeUiEvent.NavigateToSettingsClicked -> onNavigateToSettings()
                is HomeUiEvent.CancelSessionClicked,
                is HomeUiEvent.CancelSessionConfirmed,
                is HomeUiEvent.CancelSessionDismissed -> viewModel.onEvent(event)
                else -> viewModel.onEvent(event)
            }
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    state: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasActiveSession = state.activeSession != null

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Mindful Tennis") },
                actions = {
                    IconButton(onClick = { onEvent(HomeUiEvent.NavigateToSettingsClicked) }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Non-blocking sync indicator — sits at the top so content is always visible.
            // First-time users see empty cards loading in rather than a locked spinner.
            if (state.isInitialSyncing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Main scrollable content
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Spacing.md,
                    end = Spacing.md,
                    top = Spacing.sm,
                    bottom = Spacing.md,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                // Global time-range selector
                item(key = "time_range") {
                    TimeRangeSegmentedControl(
                        selected = state.selectedDuration,
                        onSelected = { onEvent(HomeUiEvent.DurationChanged(it)) },
                    )
                }

                // Active session banner
                if (hasActiveSession) {
                    item(key = "active_banner") {
                        ActiveSessionBanner(
                            startedAt = state.activeSession!!.startedAt,
                            elapsedMs = state.activeSessionElapsedMs,
                        )
                    }
                }

                // Performance Trend Chart
                item(key = "performance_chart") {
                    PerformanceChart(
                        trend = state.performanceTrend,
                        selectedDuration = state.selectedDuration,
                        isLoading = state.isLoading,
                        error = state.trendError,
                        onRetry = { onEvent(HomeUiEvent.RetryClicked) },
                    )
                }

                // Win / Loss Card
                item(key = "win_loss") {
                    WinLossCard(
                        record = state.winLossRecord,
                        opponents = state.opponents,
                        selectedOpponentIds = state.selectedWinLossOpponentIds,
                        onOpponentFilterChanged = {
                            onEvent(HomeUiEvent.WinLossOpponentFilterChanged(it))
                        },
                        mode = state.winLossMode,
                        onModeChanged = {
                            onEvent(HomeUiEvent.WinLossModeChanged(it))
                        },
                        isLoading = state.isLoading,
                        error = state.winLossError,
                        onRetry = { onEvent(HomeUiEvent.RetryClicked) },
                    )
                }

                // Aspect Performance Card
                item(key = "aspect_performance") {
                    AspectPerformanceCard(
                        selfAspectAverages = state.selfAspectAverages,
                        partnerAspectAverages = state.partnerAspectAverages,
                        selectedRatingType = state.selectedAspectRatingType,
                        onRatingTypeSelected = { onEvent(HomeUiEvent.AspectRatingTypeChanged(it)) },
                        isLoading = state.isLoading,
                        error = state.aspectError,
                        onRetry = { onEvent(HomeUiEvent.RetryClicked) },
                    )
                }

                // Calendar Heatmap
                item(key = "calendar_heatmap") {
                    CalendarHeatmap(
                        dailyScores = state.calendarDailyScores,
                        selectedDuration = state.selectedDuration,
                    )
                }
            }

            // Bottom button area
            BottomButtonBar(
                hasActiveSession = hasActiveSession,
                onShowSessions = { onEvent(HomeUiEvent.ShowSessionsClicked) },
                onStartOrEnd = {
                    if (hasActiveSession) {
                        onEvent(HomeUiEvent.EndSessionClicked)
                    } else {
                        onEvent(HomeUiEvent.StartSessionClicked)
                    }
                },
                onCancel = { onEvent(HomeUiEvent.CancelSessionClicked) },
            )
        }

        // Cancel session confirmation dialog
        if (state.showCancelSessionDialog) {
            CancelSessionDialog(
                onConfirm = { onEvent(HomeUiEvent.CancelSessionConfirmed) },
                onDismiss = { onEvent(HomeUiEvent.CancelSessionDismissed) },
            )
        }
    }
}

@Composable
private fun ActiveSessionBanner(
    startedAt: Long,
    elapsedMs: Long,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium,
            )
            .padding(Spacing.md),
    ) {
        Column {
            Text(
                text = "Session in progress",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = "Started at ${DateTimeUtils.formatTime(startedAt)} \u2022 ${DateTimeUtils.formatDuration(elapsedMs)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun BottomButtonBar(
    hasActiveSession: Boolean,
    onShowSessions: () -> Unit,
    onStartOrEnd: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (hasActiveSession) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Spacing.md,
                    vertical = Spacing.sm,
                ),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onShowSessions,
                modifier = Modifier.weight(1f),
            ) {
                Text("Sessions")
            }

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            ) {
                Text("Cancel")
            }

            FilledIconButton(
                onClick = onStartOrEnd,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "End Session",
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Spacing.md,
                    vertical = Spacing.sm,
                ),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onShowSessions,
                modifier = Modifier.weight(1f),
            ) {
                Text("Sessions")
            }

            Button(
                onClick = onStartOrEnd,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                Text("Log Session")
            }
        }
    }
}

@Composable
private fun CancelSessionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cancel Session?") },
        text = {
            Text("This will discard the current session and all its data. This action cannot be undone.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Cancel Session")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep Playing")
            }
        },
    )
}
