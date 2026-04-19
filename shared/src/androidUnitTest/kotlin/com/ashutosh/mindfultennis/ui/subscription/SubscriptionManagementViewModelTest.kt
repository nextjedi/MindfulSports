package com.ashutosh.mindfultennis.ui.subscription

import com.ashutosh.mindfultennis.domain.model.SubscriptionStatus
import com.ashutosh.mindfultennis.domain.usecase.GetSubscriptionStatusUseCase
import com.ashutosh.mindfultennis.domain.usecase.RestorePurchasesUseCase
import com.ashutosh.mindfultennis.fake.FakeSubscriptionRepository
import com.ashutosh.mindfultennis.utils.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [SubscriptionManagementViewModel].
 *
 * Covers:
 *   - Subscription status reflected in UI state
 *   - Test 5.1: Restore success shows confirmation
 *   - Test 5.2: Restore failure shows error
 *   - Result dismissal clears state
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionManagementViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun buildViewModel(repo: FakeSubscriptionRepository): SubscriptionManagementViewModel =
        SubscriptionManagementViewModel(
            getSubscriptionStatusUseCase = GetSubscriptionStatusUseCase(repo),
            restorePurchasesUseCase = RestorePurchasesUseCase(repo),
        )

    // ---------------------------------------------------------------------------
    // Subscription status propagation
    // ---------------------------------------------------------------------------

    @Test
    fun `initial state reflects None subscription`() = runTest {
        val repo = FakeSubscriptionRepository() // default = None
        val vm = buildViewModel(repo)
        advanceUntilIdle()
        assertEquals(SubscriptionStatus.None, vm.uiState.value.subscriptionStatus)
    }

    @Test
    fun `active subscription is reflected in state`() = runTest {
        val repo = FakeSubscriptionRepository()
        val vm = buildViewModel(repo)

        repo.setStatus(SubscriptionStatus.Active)
        advanceUntilIdle()

        assertEquals(SubscriptionStatus.Active, vm.uiState.value.subscriptionStatus)
    }

    @Test
    fun `trial subscription is reflected in state`() = runTest {
        val trial = SubscriptionStatus.Trial(endsAt = Instant.fromEpochSeconds(32503680000L))
        val repo = FakeSubscriptionRepository().apply { setStatus(trial) }
        val vm = buildViewModel(repo)
        advanceUntilIdle()
        assertEquals(trial, vm.uiState.value.subscriptionStatus)
    }

    // ---------------------------------------------------------------------------
    // Restore flow (Tests 5.1, 5.2)
    // ---------------------------------------------------------------------------

    @Test
    fun `restore success shows confirmation message`() = runTest {
        val repo = FakeSubscriptionRepository().apply {
            restoreResult = Result.success(Unit)
        }
        val vm = buildViewModel(repo)

        vm.onEvent(SubscriptionManagementUiEvent.RestoreClicked)
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.restoreResult)
        assertFalse(vm.uiState.value.isRestoring)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `restore failure surfaces error`() = runTest {
        val repo = FakeSubscriptionRepository().apply {
            restoreResult = Result.failure(Exception("No purchases found"))
        }
        val vm = buildViewModel(repo)

        vm.onEvent(SubscriptionManagementUiEvent.RestoreClicked)
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.error)
        assertTrue(vm.uiState.value.error!!.contains("Restore failed"))
        assertFalse(vm.uiState.value.isRestoring)
        assertNull(vm.uiState.value.restoreResult)
    }

    @Test
    fun `restoreResult dismissal clears confirmation`() = runTest {
        val repo = FakeSubscriptionRepository().apply { restoreResult = Result.success(Unit) }
        val vm = buildViewModel(repo)
        vm.onEvent(SubscriptionManagementUiEvent.RestoreClicked)
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.restoreResult)

        vm.onEvent(SubscriptionManagementUiEvent.RestoreResultDismissed)
        assertNull(vm.uiState.value.restoreResult)
    }

    @Test
    fun `error dismissal clears error`() = runTest {
        val repo = FakeSubscriptionRepository().apply {
            restoreResult = Result.failure(Exception("error"))
        }
        val vm = buildViewModel(repo)
        vm.onEvent(SubscriptionManagementUiEvent.RestoreClicked)
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.error)

        vm.onEvent(SubscriptionManagementUiEvent.ErrorDismissed)
        assertNull(vm.uiState.value.error)
    }
}
