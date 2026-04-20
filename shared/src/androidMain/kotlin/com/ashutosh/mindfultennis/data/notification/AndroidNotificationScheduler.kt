package com.ashutosh.mindfultennis.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import kotlin.math.max

private const val CHANNEL_ID = "mindful_tennis_subscription"
private const val CHANNEL_NAME = "Subscription"
private const val CHANNEL_DESC = "Trial and subscription status updates"

private const val WORK_TRIAL_WARNING = "trial_warning"
private const val WORK_TRIAL_EXPIRED = "trial_expired"
private const val WORK_RENEWAL_REMINDER = "renewal_reminder"
private const val WORK_ACCESS_ENDING = "access_ending"

private const val NOTIF_TRIAL_WARNING_ID = 1001
private const val NOTIF_TRIAL_EXPIRED_ID = 1002
private const val NOTIF_CANCELLED_ID = 1003
private const val NOTIF_ACCESS_ENDING_ID = 1004
private const val NOTIF_BILLING_ISSUE_ID = 1005
private const val NOTIF_EXPIRED_ID = 1006
private const val NOTIF_RENEWAL_ID = 1007

class AndroidNotificationScheduler(private val context: Context) : NotificationScheduler {

    private val workManager = WorkManager.getInstance(context)
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        ensureChannel()
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = CHANNEL_DESC }
        notificationManager.createNotificationChannel(channel)
    }

    override suspend fun requestPermission() {
        // Android 13+ requires POST_NOTIFICATIONS permission at runtime.
        // We request it from the Activity via the platform module on first launch.
        // No action needed here — the Activity handles the prompt.
    }

    override fun scheduleTrial(trialEndMs: Long) {
        val nowMs = System.currentTimeMillis()

        // 24 h before expiry warning
        val warningDelayMs = max(0L, trialEndMs - 24 * 60 * 60 * 1000L - nowMs)
        scheduleWork(
            tag = WORK_TRIAL_WARNING,
            delayMs = warningDelayMs,
            type = SubscriptionNotificationWorker.TYPE_TRIAL_WARNING,
        )

        // At expiry
        val expiredDelayMs = max(0L, trialEndMs - nowMs)
        scheduleWork(
            tag = WORK_TRIAL_EXPIRED,
            delayMs = expiredDelayMs,
            type = SubscriptionNotificationWorker.TYPE_TRIAL_EXPIRED,
        )
    }

    override fun cancelTrialNotifications() {
        workManager.cancelUniqueWork(WORK_TRIAL_WARNING)
        workManager.cancelUniqueWork(WORK_TRIAL_EXPIRED)
        notificationManager.cancel(NOTIF_TRIAL_WARNING_ID)
        notificationManager.cancel(NOTIF_TRIAL_EXPIRED_ID)
    }

    override fun scheduleRenewalReminder(renewsAtMs: Long) {
        val nowMs = System.currentTimeMillis()
        val reminderMs = renewsAtMs - 3 * 24 * 60 * 60 * 1000L
        val delayMs = reminderMs - nowMs
        if (delayMs <= 0L) return  // Less than 3 days away — skip
        scheduleWork(
            tag = WORK_RENEWAL_REMINDER,
            delayMs = delayMs,
            type = SubscriptionNotificationWorker.TYPE_RENEWAL_REMINDER,
        )
    }

    override fun cancelRenewalReminder() {
        workManager.cancelUniqueWork(WORK_RENEWAL_REMINDER)
        notificationManager.cancel(NOTIF_RENEWAL_ID)
    }

    override fun showSubscriptionCancelledNotification() {
        showNotification(
            id = NOTIF_CANCELLED_ID,
            title = "Subscription Cancelled",
            body = "You've cancelled your subscription. Your access continues until the end of the billing period.",
        )
    }

    override fun scheduleAccessEndingReminder(accessUntilMs: Long) {
        val nowMs = System.currentTimeMillis()
        val delayMs = max(0L, accessUntilMs - 24 * 60 * 60 * 1000L - nowMs)
        scheduleWork(
            tag = WORK_ACCESS_ENDING,
            delayMs = delayMs,
            type = SubscriptionNotificationWorker.TYPE_ACCESS_ENDING,
        )
    }

    override fun showBillingIssueNotification() {
        showNotification(
            id = NOTIF_BILLING_ISSUE_ID,
            title = "Payment Issue",
            body = "We couldn't process your payment. Please update your payment method to keep your subscription active.",
        )
    }

    override fun showSubscriptionExpiredNotification() {
        showNotification(
            id = NOTIF_EXPIRED_ID,
            title = "Subscription Expired",
            body = "Your subscription has expired. Subscribe again to continue tracking your tennis progress.",
        )
    }

    override fun cancelAll() {
        cancelTrialNotifications()
        cancelRenewalReminder()
        workManager.cancelUniqueWork(WORK_ACCESS_ENDING)
        notificationManager.cancel(NOTIF_CANCELLED_ID)
        notificationManager.cancel(NOTIF_ACCESS_ENDING_ID)
        notificationManager.cancel(NOTIF_BILLING_ISSUE_ID)
        notificationManager.cancel(NOTIF_EXPIRED_ID)
        notificationManager.cancel(NOTIF_RENEWAL_ID)
    }

    private fun scheduleWork(tag: String, delayMs: Long, type: String) {
        val request = OneTimeWorkRequestBuilder<SubscriptionNotificationWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(SubscriptionNotificationWorker.KEY_TYPE to type))
            .build()
        workManager.enqueueUniqueWork(tag, ExistingWorkPolicy.REPLACE, request)
    }

    internal fun showNotification(id: Int, title: String, body: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(id, notification)
    }
}
