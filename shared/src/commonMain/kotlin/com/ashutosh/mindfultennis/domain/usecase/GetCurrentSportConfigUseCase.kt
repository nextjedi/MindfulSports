package com.ashutosh.mindfultennis.domain.usecase

import com.ashutosh.mindfultennis.data.local.datastore.UserPreferences
import com.ashutosh.mindfultennis.sport.SportConfig
import com.ashutosh.mindfultennis.sport.SportRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Returns a [Flow] of the currently selected [SportConfig].
 *
 * The sport is stored as a string ID in [UserPreferences.selectedSportId].
 * Emits [SportRegistry.tennis] as a safe default until the user makes a selection.
 * Updates reactively whenever the user changes sport from Settings.
 */
class GetCurrentSportConfigUseCase(
    private val userPreferences: UserPreferences,
) {
    operator fun invoke(): Flow<SportConfig> =
        userPreferences.selectedSportId.map { id ->
            SportRegistry.fromId(id ?: SportRegistry.tennis.sportId)
        }
}
