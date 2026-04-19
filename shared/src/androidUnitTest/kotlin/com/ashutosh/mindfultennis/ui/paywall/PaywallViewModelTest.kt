package com.ashutosh.mindfultennis.ui.paywall

import com.ashutosh.mindfultennis.data.repository.PurchaseCancelledByUserException
import com.ashutosh.mindfultennis.fake.FakeSubscriptionRepository
import com.ashutosh.mindfultennis.utils.MainDispatcherRule
import com.revenuecat.purchases.kmp.models.Package
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [PaywallViewModel].
 *
 * Covers:
 *   - Test 1.3: Loading state visible while offering is being fetched
 *   - Test 2.1: Successful purchase sets purchaseCompleted = true
 *   - Test 2.x: Purchase error surfaces error message
 *   - Test 5.1: Restore success sets purchaseCompleted = true
 *   - Test 5.2: Restore failure surfaces error message
 *   - Test 7.1: User cancel is silent (no error, no navigation)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PaywallViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // ---------------------------------------------------------------------------
    // Loading state (Test 1.3)
    // ---------------------------------------------------------------------------

    @Test
    fun `initial state shows loading`() {
        val repo = FakeSubscriptionRepository().apply { shouldDelayOffering = true }
        val vm = PaywallViewModel(repo)
        assertTrue(vm.uiState.value.isLoading)
    }

    @Test
    fun `loading clears after offering resolves`() = runTest {
        val repo = FakeSubscriptionRepository() // returns null immediately
        val vm = PaywallViewModel(repo)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
    }

    // ---------------------------------------------------------------------------
    // Offering load outcomes
    // ---------------------------------------------------------------------------

    @Test
    fun `offering load failure surfaces error`() = runTest {
        val repo = FakeSubscriptionRepository().apply {
            offeringResult = Result.failure(Exception("Network error"))
        }
        val vm = PaywallViewModel(repo)
        advanceUntilIdle()
        assertEquals("Network error", vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `offering load success clears error`() = runTest {
        val repo = FakeSubscriptionRepository() // success(null) — no offering, no error
        val vm = PaywallViewModel(repo)
        advanceUntilIdle()
        assertNull(vm.uiState.value.error)
    }

    // ---------------------------------------------------------------------------
    // Purchase flow (Tests 2.1, 2.x)
    // ---------------------------------------------------------------------------

    @Test
    fun `purchase success marks purchaseCompleted`() = runTest {
        val repo = FakeSubscriptionRepository().apply {
            purchaseResult = Result.success(Unit)
        }
        val vm = PaywallViewModel(repo)
        val pkg = mockk<Package>()

        vm.onEvent(PaywallUiEvent.PurchasePackage(pkg))
        advanceUntilIdle()

        assertTrue(vm.uiState.value.purchaseCompleted)
        assertFalse(vm.uiState.value.isPurchasing)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `purchase error surfaces error message`() = runTest {
        val repo = FakeSubscriptionRepository().apply {
            purchaseResult = Result.failure(Exception("Payment declined"))
        }
        val vm = PaywallViewModel(repo)
        val pkg = mockk<Package>()

        vm.onEvent(PaywallUiEvent.PurchasePackage(pkg))
        advanceUntilIdle()

        assertFalse(vm.uiState.value.purchaseCompleted)
        assertFalse(vm.uiState.value.isPurchasing)
        assertEquals("Payment declined", vm.uiState.value.error)
    }

    @Test
    fun `user cancel is silent - no error and no navigation`() = runTest {
        // Test 7.1: dismissing the Play billing sheet must not show an error or navigate away
        val repo = FakeSubscriptionRepository().apply {
            purchaseResult = Result.failure(PurchaseCancelledByUserException())
        }
        val vm = PaywallViewModel(repo)
        val pkg = mockk<Package>()

        vm.onEvent(PaywallUiEvent.PurchasePackage(pkg))
        advanceUntilIdle()

        assertFalse(vm.uiState.value.purchaseCompleted, "Should not navigate away on cancel")
        assertFalse(vm.uiState.value.isPurchasing)
        assertNull(vm.uiState.value.error, "Should not show error on cancel")
    }

    // ---------------------------------------------------------------------------
    // Restore flow (Tests 5.1, 5.2)
    // ---------------------------------------------------------------------------

    @Test
    fun `restore success marks purchaseCompleted`() = runTest {
        val repo = FakeSubscriptionRepository().apply {
            restoreResult = Result.success(Unit)
        }
        val vm = PaywallViewModel(repo)

        vm.onEvent(PaywallUiEvent.RestorePurchases)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.purchaseCompleted)
        assertFalse(vm.uiState.value.isPurchasing)
    }

    @Test
    fun `restore failure surfaces error message`() = runTest {
        val repo = FakeSubscriptionRepository().apply {
            restoreResult = Result.failure(Exception("No purchases found"))
        }
        val vm = PaywallViewModel(repo)

        vm.onEvent(PaywallUiEvent.RestorePurchases)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.purchaseCompleted)
        assertEquals("No purchases found", vm.uiState.value.error)
    }

    // ---------------------------------------------------------------------------
    // Error dismissal
    // ---------------------------------------------------------------------------

    @Test
    fun `error dismissed clears error state`() = runTest {
        val repo = FakeSubscriptionRepository().apply {
            purchaseResult = Result.failure(Exception("Some error"))
        }
        val vm = PaywallViewModel(repo)
        vm.onEvent(PaywallUiEvent.PurchasePackage(mockk()))
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.error)

        vm.onEvent(PaywallUiEvent.ErrorDismissed)
        assertNull(vm.uiState.value.error)
    }
}
