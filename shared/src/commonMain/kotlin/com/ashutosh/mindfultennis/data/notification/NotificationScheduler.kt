package com.ashutosh.mindfultennis.data.notification

/**
 * Platform-specific notification scheduler.
 * Implementations live in androidMain and iosMain.
 *
 * All scheduling methods are fire-and-forget (no suspend) so they can be called
 * from the SubscriptionRepository without blocking the status update flow.
 */
interface NotificationScheduler {
    /**
     * Request notification permission from the OS (Android 13+, iOS).
     * No-op on platforms/versions where permission is not required.
     */
    suspend fun requestPermission()

    /**
     * Schedule two notifications for a trial:
     *  - 24 h before [trialEndMs]: "Trial expires tomorrow"
     *  - At [trialEndMs]: "Trial has expired — subscribe to continue"
     */
    fun scheduleTrial(trialEndMs: Long)

    /** Cancel all pending trial notifications. */
    fun cancelTrialNotifications()

    /**
     * Schedule a "subscription renewing soon" reminder 3 days before [renewsAtMs].
     * No-op if [renewsAtMs] is null (lifetime plan) or < 3 days away.
     */
    fun scheduleRenewalReminder(renewsAtMs: Long)

    /** Cancel any pending renewal reminder. */
    fun cancelRenewalReminder()

    /** Show an immediate "subscription cancelled" notification. */
    fun showSubscriptionCancelledNotification()

    /**
     * Schedule an "access ends soon" reminder 24 h before [accessUntilMs]
     * for a cancelled subscription still within its paid period.
     */
    fun scheduleAccessEndingReminder(accessUntilMs: Long)

    /** Show an immediate "billing issue — update payment" notification. */
    fun showBillingIssueNotification()

    /** Show an immediate "subscription expired" notification. */
    fun showSubscriptionExpiredNotification()

    /** Cancel all subscription-related pending notifications. */
    fun cancelAll()
}
