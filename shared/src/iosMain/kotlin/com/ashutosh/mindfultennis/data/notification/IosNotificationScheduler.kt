package com.ashutosh.mindfultennis.data.notification

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSString
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max

@OptIn(ExperimentalForeignApi::class)
class IosNotificationScheduler : NotificationScheduler {

    private val center = UNUserNotificationCenter.currentNotificationCenter()

    override suspend fun requestPermission() {
        suspendCoroutine { cont ->
            center.requestAuthorizationWithOptions(
                UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
            ) { _, _ -> cont.resume(Unit) }
        }
    }

    override fun scheduleTrial(trialEndMs: Long) {
        val nowMs = currentTimeMs()

        // 24 h before expiry
        val warningDelaySec = max(1.0, (trialEndMs - 24 * 60 * 60 * 1000L - nowMs) / 1000.0)
        schedule(
            id = "trial_warning",
            title = "Trial Expires Tomorrow",
            body = "Your free trial expires in 24 hours. Subscribe now to keep tracking your tennis progress.",
            delaySec = warningDelaySec,
        )

        // At expiry
        val expiredDelaySec = max(1.0, (trialEndMs - nowMs) / 1000.0)
        schedule(
            id = "trial_expired",
            title = "Trial Expired",
            body = "Your free trial has ended. Subscribe to MindfulTennis to continue improving your game.",
            delaySec = expiredDelaySec,
        )
    }

    override fun cancelTrialNotifications() {
        center.removePendingNotificationRequestsWithIdentifiers(listOf("trial_warning", "trial_expired"))
        center.removeDeliveredNotificationsWithIdentifiers(listOf("trial_warning", "trial_expired"))
    }

    override fun scheduleRenewalReminder(renewsAtMs: Long) {
        val nowMs = currentTimeMs()
        val reminderMs = renewsAtMs - 3 * 24 * 60 * 60 * 1000L
        val delaySec = (reminderMs - nowMs) / 1000.0
        if (delaySec <= 0.0) return
        schedule(
            id = "renewal_reminder",
            title = "Subscription Renewing Soon",
            body = "Your MindfulTennis subscription renews in 3 days. Manage it in Settings.",
            delaySec = delaySec,
        )
    }

    override fun cancelRenewalReminder() {
        center.removePendingNotificationRequestsWithIdentifiers(listOf("renewal_reminder"))
        center.removeDeliveredNotificationsWithIdentifiers(listOf("renewal_reminder"))
    }

    override fun showSubscriptionCancelledNotification() {
        schedule(
            id = "sub_cancelled",
            title = "Subscription Cancelled",
            body = "You've cancelled your subscription. Your access continues until the end of the billing period.",
            delaySec = 1.0,
        )
    }

    override fun scheduleAccessEndingReminder(accessUntilMs: Long) {
        val nowMs = currentTimeMs()
        val delaySec = max(1.0, (accessUntilMs - 24 * 60 * 60 * 1000L - nowMs) / 1000.0)
        schedule(
            id = "access_ending",
            title = "Access Ending Tomorrow",
            body = "Your subscription access ends tomorrow. Subscribe again to keep your progress.",
            delaySec = delaySec,
        )
    }

    override fun showBillingIssueNotification() {
        schedule(
            id = "billing_issue",
            title = "Payment Issue",
            body = "We couldn't process your payment. Please update your payment method to keep your subscription active.",
            delaySec = 1.0,
        )
    }

    override fun showSubscriptionExpiredNotification() {
        schedule(
            id = "sub_expired",
            title = "Subscription Expired",
            body = "Your subscription has expired. Subscribe again to continue tracking your tennis progress.",
            delaySec = 1.0,
        )
    }

    override fun cancelAll() {
        val ids = listOf(
            "trial_warning", "trial_expired", "renewal_reminder",
            "access_ending", "sub_cancelled", "billing_issue", "sub_expired",
        )
        center.removePendingNotificationRequestsWithIdentifiers(ids)
        center.removeDeliveredNotificationsWithIdentifiers(ids)
    }

    private fun schedule(id: String, title: String, body: String, delaySec: Double) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            setSound(platform.UserNotifications.UNNotificationSound.defaultSound())
        }
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
            timeInterval = delaySec,
            repeats = false,
        )
        val request = UNNotificationRequest.requestWithIdentifier(id, content, trigger)
        center.addNotificationRequest(request) { /* ignore error */ _ -> }
    }

    private fun currentTimeMs(): Long =
        (platform.Foundation.NSDate.date().timeIntervalSince1970 * 1000).toLong()
}
