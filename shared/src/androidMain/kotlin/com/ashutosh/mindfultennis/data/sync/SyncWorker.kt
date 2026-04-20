package com.ashutosh.mindfultennis.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import co.touchlab.kermit.Logger
import com.ashutosh.mindfultennis.data.local.datastore.UserPreferences
import io.sentry.kotlin.multiplatform.Sentry
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val syncManager: SyncManager by inject()
    private val userPreferences: UserPreferences by inject()

    private val log = Logger.withTag("SyncWorker")

    override suspend fun doWork(): Result {
        val userId = userPreferences.cachedUserId.first()
        if (userId.isNullOrBlank()) {
            log.d { "No cached user ID — skipping sync" }
            return Result.success()
        }

        log.i { "WorkManager sync attempt ${runAttemptCount + 1} for user $userId" }

        return try {
            val result = syncManager.sync(userId)
            when {
                result.isSuccess -> {
                    log.i { "WorkManager sync succeeded for user $userId" }
                    Result.success()
                }
                runAttemptCount < 3 -> {
                    log.w { "Sync failed (attempt ${runAttemptCount + 1}/3), will retry" }
                    Result.retry()
                }
                else -> {
                    // All retries exhausted — capture so it appears in Sentry Issues
                    val cause = result.exceptionOrNull()
                    log.e(cause) { "Sync failed after 3 attempts — giving up" }
                    cause?.let { Sentry.captureException(it) }
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            // Defensive catch — sync() wraps in runCatching but guard here anyway
            log.e(e) { "Unexpected exception in SyncWorker.doWork()" }
            Sentry.captureException(e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
