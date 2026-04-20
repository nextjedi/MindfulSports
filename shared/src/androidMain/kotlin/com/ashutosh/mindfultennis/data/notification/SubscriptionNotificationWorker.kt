package com.ashutosh.mindfultennis.data.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SubscriptionNotificationWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val scheduler = AndroidNotificationScheduler(context)
        when (inputData.getString(KEY_TYPE)) {
            TYPE_TRIAL_WARNING -> scheduler.showNotification(
                id = 1001,
                title = "Trial Expires Tomorrow",
                body = "Your free trial expires in 24 hours. Subscribe now to keep tracking your tennis progress.",
            )
            TYPE_TRIAL_EXPIRED -> scheduler.showNotification(
                id = 1002,
                title = "Trial Expired",
                body = "Your free trial has ended. Subscribe to MindfulTennis to continue improving your game.",
            )
            TYPE_RENEWAL_REMINDER -> scheduler.showNotification(
                id = 1007,
                title = "Subscription Renewing Soon",
                body = "Your MindfulTennis subscription will renew in 3 days. Manage it in Settings.",
            )
            TYPE_ACCESS_ENDING -> scheduler.showNotification(
                id = 1004,
                title = "Access Ending Tomorrow",
                body = "Your subscription access ends tomorrow. Subscribe again to keep your progress.",
            )
            else -> return Result.failure()
        }
        return Result.success()
    }

    companion object {
        const val KEY_TYPE = "type"
        const val TYPE_TRIAL_WARNING = "trial_warning"
        const val TYPE_TRIAL_EXPIRED = "trial_expired"
        const val TYPE_RENEWAL_REMINDER = "renewal_reminder"
        const val TYPE_ACCESS_ENDING = "access_ending"
    }
}
