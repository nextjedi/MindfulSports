package com.ashutosh.mindfultennis.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit4.runners.AndroidJUnit4
import com.ashutosh.mindfultennis.data.repository.SubscriptionRepository
import com.ashutosh.mindfultennis.domain.model.SubscriptionStatus
import com.ashutosh.mindfultennis.ui.paywall.PaywallScreen
import com.ashutosh.mindfultennis.ui.paywall.PaywallViewModel
import com.revenuecat.purchases.kmp.models.Offering
import com.revenuecat.purchases.kmp.models.Package
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue

/**
 * Instrumented Compose UI tests for [PaywallScreen].
 *
 * Covers:
 *   - Test 1.4: Error state shows "unavailable" message
 *   - Test 1.5: Back button calls onNavigateBack
 *
 * Tests requiring a real [Offering] (package cards display) are marked TODO
 * and covered by the manual Phase 5 sandbox testing plan.
 *
 * Run with: ./gradlew :composeApp:connectedAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class PaywallScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ---------------------------------------------------------------------------
    // Test 1.4 — Error state
    // ---------------------------------------------------------------------------

    @Test
    fun errorState_showsUnavailableMessage() {
        val repo = buildRepo(offeringResult = Result.failure(Exception("test")))
        val vm = PaywallViewModel(repo)

        composeRule.setContent {
            PaywallScreen(viewModel = vm, onNavigateBack = {})
        }

        composeRule.onNodeWithText("Subscription plans unavailable").assertIsDisplayed()
    }

    // ---------------------------------------------------------------------------
    // Test 1.5 — Back navigation
    // ---------------------------------------------------------------------------

    @Test
    fun backButton_callsOnNavigateBack() {
        var navigatedBack = false
        val repo = buildRepo(offeringResult = Result.failure(Exception("test")))
        val vm = PaywallViewModel(repo)

        composeRule.setContent {
            PaywallScreen(viewModel = vm, onNavigateBack = { navigatedBack = true })
        }

        composeRule.onNodeWithContentDescription("Back").performClick()
        assertTrue(navigatedBack)
    }

    // ---------------------------------------------------------------------------
    // TODO: Test 1.1 — Packages shown
    // Requires a real Offering with packages. Covered by Phase 5 manual sandbox testing.
    // To automate: create a FakeOffering implementing Offering, configure FakeRepo, assert
    // package card titles are displayed.
    // ---------------------------------------------------------------------------

    // ---------------------------------------------------------------------------
    // TODO: Test 1.3 — Loading spinner
    // Requires controlling timing so the coroutine hasn't resolved yet.
    // Use mainClock.autoAdvance = false + fakeRepo.shouldDelayOffering = true.
    // ---------------------------------------------------------------------------

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun buildRepo(offeringResult: Result<Offering?> = Result.success(null)): SubscriptionRepository =
        object : SubscriptionRepository {
            private val _status = MutableStateFlow<SubscriptionStatus>(SubscriptionStatus.None)
            override val subscriptionStatus: StateFlow<SubscriptionStatus> = _status.asStateFlow()
            override suspend fun getCurrentOffering() = offeringResult
            override suspend fun purchasePackage(rcPackage: Package) = Result.success(Unit)
            override suspend fun restorePurchases() = Result.success(Unit)
            override suspend fun identifyUser(userId: String) = Unit
            override suspend fun resetUser() = Unit
        }
}
