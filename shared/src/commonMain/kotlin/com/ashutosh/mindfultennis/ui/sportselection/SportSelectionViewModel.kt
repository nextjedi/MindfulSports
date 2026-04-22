package com.ashutosh.mindfultennis.ui.sportselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashutosh.mindfultennis.data.local.datastore.UserPreferences
import com.ashutosh.mindfultennis.sport.SportConfig
import com.ashutosh.mindfultennis.sport.SportRegistry
import kotlinx.coroutines.launch

class SportSelectionViewModel(
    private val userPreferences: UserPreferences,
) : ViewModel() {

    /** All 10 sports available for selection. */
    val sports: List<SportConfig> = SportRegistry.all

    /**
     * Persist the user's sport choice.
     * NavGraph observes [UserPreferences.selectedSportId] and navigates to Home
     * automatically once this write completes.
     */
    fun onSportSelected(sportId: String) {
        viewModelScope.launch {
            userPreferences.setSelectedSportId(sportId)
        }
    }
}
