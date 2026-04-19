package com.ashutosh.mindfultennis.ui.home

import androidx.compose.runtime.Immutable
import com.ashutosh.mindfultennis.domain.model.Aspect
import com.ashutosh.mindfultennis.domain.model.DurationFilter
import com.ashutosh.mindfultennis.domain.model.Opponent
import com.ashutosh.mindfultennis.domain.model.PerformanceTrend
import com.ashutosh.mindfultennis.domain.model.RatingType
import com.ashutosh.mindfultennis.domain.model.Session
import com.ashutosh.mindfultennis.domain.model.WinLossMode
import com.ashutosh.mindfultennis.domain.model.WinLossRecord

/**
 * UI state for the Home / Dashboard screen.
 */
@Immutable
data class HomeUiState(
    val isLoading: Boolean = true,
    val isInitialSyncing: Boolean = false,
    val performanceTrend: List<PerformanceTrend> = emptyList(),
    val selectedDuration: DurationFilter = DurationFilter.ONE_MONTH,
    val winLossRecord: WinLossRecord? = null,
    val winLossMode: WinLossMode = WinLossMode.MATCHES,
    val calendarDailyScores: Map<String, Int?> = emptyMap(),
    val selfAspectAverages: Map<Aspect, Float> = emptyMap(),
    val partnerAspectAverages: Map<Aspect, Float> = emptyMap(),
    val opponents: List<Opponent> = emptyList(),
    val selectedWinLossOpponentIds: Set<String> = emptySet(),
    val selectedAspectOpponentIds: Set<String> = emptySet(),
    val selectedAspectRatingType: RatingType = RatingType.SELF,
    val activeSession: Session? = null,
    val activeSessionElapsedMs: Long = 0L,
    val showCancelSessionDialog: Boolean = false,
    val isCancellingSession: Boolean = false,
    val trendError: String? = null,
    val winLossError: String? = null,
    val aspectError: String? = null,
    val error: String? = null,
)

/**
 * UI events from the Home / Dashboard screen.
 */
sealed interface HomeUiEvent {
    data class DurationChanged(val duration: DurationFilter) : HomeUiEvent
    data class WinLossOpponentFilterChanged(val ids: Set<String>) : HomeUiEvent
    data class WinLossModeChanged(val mode: WinLossMode) : HomeUiEvent
    data class AspectOpponentFilterChanged(val ids: Set<String>) : HomeUiEvent
    data class AspectRatingTypeChanged(val ratingType: RatingType) : HomeUiEvent
    data object StartSessionClicked : HomeUiEvent
    data object EndSessionClicked : HomeUiEvent
    data object CancelSessionClicked : HomeUiEvent
    data object CancelSessionConfirmed : HomeUiEvent
    data object CancelSessionDismissed : HomeUiEvent
    data object ShowSessionsClicked : HomeUiEvent
    data object RetryClicked : HomeUiEvent
    data object ErrorDismissed : HomeUiEvent
    data object NavigateToSettingsClicked : HomeUiEvent
}
