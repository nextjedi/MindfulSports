package com.ashutosh.mindfultennis.domain.model

import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers test cases 4.3 and 4.4 from the payment test plan:
 *   - 4.3: Cancelled status still grants access until period ends
 *   - 4.4: Expired status revokes access
 */
class SubscriptionStatusTest {

    @Test
    fun `None has no premium access`() {
        assertFalse(SubscriptionStatus.None.hasPremiumAccess)
    }

    @Test
    fun `Expired has no premium access`() {
        assertFalse(SubscriptionStatus.Expired.hasPremiumAccess)
    }

    @Test
    fun `Active has premium access`() {
        assertTrue(SubscriptionStatus.Active.hasPremiumAccess)
    }

    @Test
    fun `Trial has premium access`() {
        val trial = SubscriptionStatus.Trial(endsAt = Instant.fromEpochSeconds(32503680000L))
        assertTrue(trial.hasPremiumAccess)
    }

    @Test
    fun `Cancelled still has premium access`() {
        // Cancelled = user cancelled but billing period hasn't ended yet → still has access
        assertTrue(SubscriptionStatus.Cancelled.hasPremiumAccess)
    }
}
