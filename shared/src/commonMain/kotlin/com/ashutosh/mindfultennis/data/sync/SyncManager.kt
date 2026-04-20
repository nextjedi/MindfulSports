package com.ashutosh.mindfultennis.data.sync

import co.touchlab.kermit.Logger
import com.ashutosh.mindfultennis.data.local.datastore.UserPreferences
import com.ashutosh.mindfultennis.data.local.db.dao.FocusPointDao
import com.ashutosh.mindfultennis.data.local.db.dao.OpponentDao
import com.ashutosh.mindfultennis.data.local.db.dao.PartnerDao
import com.ashutosh.mindfultennis.data.local.db.dao.PartnerRatingDao
import com.ashutosh.mindfultennis.data.local.db.dao.SelfRatingDao
import com.ashutosh.mindfultennis.data.local.db.dao.SessionDao
import com.ashutosh.mindfultennis.data.local.db.dao.SetScoreDao
import com.ashutosh.mindfultennis.data.local.db.entity.SyncStatus
import com.ashutosh.mindfultennis.data.local.db.entity.toDto
import com.ashutosh.mindfultennis.data.local.db.entity.toEntity
import com.ashutosh.mindfultennis.data.remote.SupabaseSessionDataSource
import com.ashutosh.mindfultennis.data.remote.SupabaseUserDataSource
import com.ashutosh.mindfultennis.data.remote.model.UserDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Manages bidirectional sync between Room (local) and Supabase (remote).
 *
 * Strategy:
 * 1. **Push**: Find all local rows with syncStatus = PENDING, push them to Supabase, mark SYNCED.
 * 2. **Pull**: Fetch all remote rows updated after lastSyncTimestamp, merge into Room (LWW by updatedAt).
 * 3. Update lastSyncTimestamp after successful sync.
 */
class SyncManager(
    private val sessionDao: SessionDao,
    private val selfRatingDao: SelfRatingDao,
    private val partnerRatingDao: PartnerRatingDao,
    private val setScoreDao: SetScoreDao,
    private val focusPointDao: FocusPointDao,
    private val opponentDao: OpponentDao,
    private val partnerDao: PartnerDao,
    private val remoteDataSource: SupabaseSessionDataSource,
    private val userDataSource: SupabaseUserDataSource,
    private val supabaseClient: SupabaseClient,
    private val userPreferences: UserPreferences,
) {
    companion object {
        private const val TAG = "SyncManager"
    }

    private val syncMutex = Mutex()

    /**
     * Runs a full sync cycle: push pending local changes, then pull remote updates.
     * Guarded by a Mutex to prevent concurrent sync runs.
     * @param userId The current user's ID.
     * @return Result indicating success or the first failure encountered.
     */
    suspend fun sync(userId: String): Result<Unit> = withContext(Dispatchers.Default) {
        if (!syncMutex.tryLock()) {
            Logger.d(TAG) { "Sync already in progress, skipping" }
            return@withContext Result.success(Unit)
        }
        val startMs = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        try {
            runCatching {
                Logger.i(TAG) { "Sync started for user $userId" }

                // Ensure user record exists in Supabase before pushing FK-dependent data
                ensureUserExists(userId)

                // Push entities with no FK dependencies first
                pushPendingFocusPoints()
                pushPendingOpponents()
                pushPendingPartners()

                // Sessions depend on opponents + partners via FK
                pushPendingSessions()

                // Ratings and scores depend on sessions (and set_scores on opponents) via FK
                pushPendingSelfRatings()
                pushPendingPartnerRatings()
                pushPendingSetScores()

                // Push pending deletes (after upserts, before pulls)
                pushPendingDeleteSessions()
                pushPendingDeleteFocusPoints()
                pushPendingDeleteOpponents()
                pushPendingDeletePartners()

                val lastSync = userPreferences.lastSyncTimestamp.first()
                pullRemoteSessions(userId, lastSync)
                pullRemoteFocusPoints(userId)
                pullRemoteOpponents(userId)
                pullRemotePartners(userId)

                val nowMs = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                userPreferences.setLastSyncTimestamp(nowMs)

                val durationMs = nowMs - startMs
                Logger.i(TAG) { "Sync completed in ${durationMs}ms for user $userId" }
                Unit
            }.also { result ->
                if (result.isFailure) {
                    val durationMs = kotlinx.datetime.Clock.System.now().toEpochMilliseconds() - startMs
                    Logger.e(TAG, result.exceptionOrNull()) {
                        "Sync failed after ${durationMs}ms for user $userId"
                    }
                }
            }
        } finally {
            syncMutex.unlock()
        }
    }

    // ── Ensure User Record ────────────────────────────────────────────

    /**
     * Ensures the user has a row in the Supabase `users` table.
     * All other tables (sessions, opponents, partners, focus_points, etc.) have
     * FK references to users(id), so this must succeed before any push.
     */
    private suspend fun ensureUserExists(userId: String) {
        try {
            val existing = userDataSource.getUser(userId)
            if (existing != null) return

            val authUser = supabaseClient.auth.currentSessionOrNull()?.user
            val email = authUser?.email ?: ""
            val displayName = authUser?.userMetadata?.get("full_name")?.toString()
                ?.removeSurrounding("\"")
            val photoUrl = authUser?.userMetadata?.get("avatar_url")?.toString()
                ?.removeSurrounding("\"")

            userDataSource.upsertUser(
                UserDto(
                    id = userId,
                    email = email,
                    displayName = displayName,
                    photoUrl = photoUrl,
                    createdAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
                    timeZone = kotlinx.datetime.TimeZone.currentSystemDefault().id,
                )
            )
            Logger.d(TAG) { "Created user record for $userId" }
        } catch (e: Exception) {
            Logger.e(TAG, e) { "Failed to ensure user record exists for $userId" }
            throw e // Sync cannot proceed without the user row
        }
    }

    // ── Push: Sessions ────────────────────────────────────────────────

    private suspend fun pushPendingSessions() {
        val pending = sessionDao.getBySyncStatus(SyncStatus.PENDING.name)
        for (session in pending) {
            try {
                remoteDataSource.upsertSession(session.toDto())
                sessionDao.updateSyncStatus(session.id, SyncStatus.SYNCED.name)
            } catch (e: Exception) {
                Logger.w(TAG, e) { "Failed to push session ${session.id}" }
            }
        }
    }

    // ── Push: Self Ratings ────────────────────────────────────────────

    private suspend fun pushPendingSelfRatings() {
        val pending = selfRatingDao.getBySyncStatus(SyncStatus.PENDING.name)
        if (pending.isEmpty()) return
        // Group by sessionId and push per session
        val grouped = pending.groupBy { it.sessionId }
        for ((sessionId, ratings) in grouped) {
            try {
                remoteDataSource.deleteSelfRatingsForSession(sessionId)
                remoteDataSource.upsertSelfRatings(ratings.map { it.toDto() })
                selfRatingDao.updateSyncStatusForSession(sessionId, SyncStatus.SYNCED.name)
            } catch (e: Exception) {
                Logger.w(TAG, e) { "Failed to push self ratings for session $sessionId" }
            }
        }
    }

    // ── Push: Partner Ratings ─────────────────────────────────────────

    private suspend fun pushPendingPartnerRatings() {
        val pending = partnerRatingDao.getBySyncStatus(SyncStatus.PENDING.name)
        if (pending.isEmpty()) return
        val grouped = pending.groupBy { it.sessionId }
        for ((sessionId, ratings) in grouped) {
            try {
                remoteDataSource.deletePartnerRatingsForSession(sessionId)
                remoteDataSource.upsertPartnerRatings(ratings.map { it.toDto() })
                partnerRatingDao.updateSyncStatusForSession(sessionId, SyncStatus.SYNCED.name)
            } catch (e: Exception) {
                Logger.w(TAG, e) { "Failed to push partner ratings for session $sessionId" }
            }
        }
    }

    // ── Push: Set Scores ──────────────────────────────────────────────

    private suspend fun pushPendingSetScores() {
        val pending = setScoreDao.getBySyncStatus(SyncStatus.PENDING.name)
        if (pending.isEmpty()) return
        val grouped = pending.groupBy { it.sessionId }
        for ((sessionId, scores) in grouped) {
            try {
                remoteDataSource.deleteSetScoresForSession(sessionId)
                remoteDataSource.upsertSetScores(scores.map { it.toDto() })
                setScoreDao.updateSyncStatusForSession(sessionId, SyncStatus.SYNCED.name)
            } catch (e: Exception) {
                Logger.w(TAG, e) { "Failed to push set scores for session $sessionId" }
            }
        }
    }

    // ── Push: Focus Points ────────────────────────────────────────────

    private suspend fun pushPendingFocusPoints() {
        val pending = focusPointDao.getBySyncStatus(SyncStatus.PENDING.name)
        for (fp in pending) {
            try {
                remoteDataSource.upsertFocusPoint(fp.toDto())
                focusPointDao.updateSyncStatus(fp.id, SyncStatus.SYNCED.name)
            } catch (e: Exception) {
                Logger.w(TAG, e) { "Failed to push focus point ${fp.id}" }
            }
        }
    }

    // ── Push: Opponents ───────────────────────────────────────────────

    private suspend fun pushPendingOpponents() {
        val pending = opponentDao.getBySyncStatus(SyncStatus.PENDING.name)
        for (opp in pending) {
            try {
                remoteDataSource.upsertOpponent(opp.toDto())
                opponentDao.updateSyncStatus(opp.id, SyncStatus.SYNCED.name)
            } catch (e: Exception) {
                Logger.w(TAG, e) { "Failed to push opponent ${opp.id}" }
            }
        }
    }

    // ── Push: Partners ────────────────────────────────────────────────

    private suspend fun pushPendingPartners() {
        val pending = partnerDao.getBySyncStatus(SyncStatus.PENDING.name)
        for (p in pending) {
            try {
                remoteDataSource.upsertPartner(p.toDto())
                partnerDao.updateSyncStatus(p.id, SyncStatus.SYNCED.name)
            } catch (e: Exception) {
                Logger.w(TAG, e) { "Failed to push partner ${p.id}" }
            }
        }
    }

    // ── Push Delete: Sessions ─────────────────────────────────────

    private suspend fun pushPendingDeleteSessions() {
        val pending = sessionDao.getBySyncStatus(SyncStatus.PENDING_DELETE.name)
        for (session in pending) {
            try {
                remoteDataSource.deleteSession(session.id)
                sessionDao.deleteById(session.id)
            } catch (e: Exception) {
                Logger.w(TAG, e) { "Failed to push delete for session ${session.id}" }
            }
        }
    }

    // ── Push Delete: Focus Points ─────────────────────────────────

    private suspend fun pushPendingDeleteFocusPoints() {
        val pending = focusPointDao.getBySyncStatus(SyncStatus.PENDING_DELETE.name)
        for (fp in pending) {
            try {
                remoteDataSource.deleteFocusPoint(fp.id)
                focusPointDao.deleteById(fp.id)
            } catch (e: Exception) {
                Logger.w(TAG, e) { "Failed to push delete for focus point ${fp.id}" }
            }
        }
    }

    // ── Push Delete: Opponents ────────────────────────────────────

    private suspend fun pushPendingDeleteOpponents() {
        val pending = opponentDao.getBySyncStatus(SyncStatus.PENDING_DELETE.name)
        for (opp in pending) {
            try {
                remoteDataSource.deleteOpponent(opp.id)
                opponentDao.deleteById(opp.id)
            } catch (e: Exception) {
                Logger.w(TAG, e) { "Failed to push delete for opponent ${opp.id}" }
            }
        }
    }

    // ── Push Delete: Partners ─────────────────────────────────────

    private suspend fun pushPendingDeletePartners() {
        val pending = partnerDao.getBySyncStatus(SyncStatus.PENDING_DELETE.name)
        for (p in pending) {
            try {
                remoteDataSource.deletePartner(p.id)
                partnerDao.deleteById(p.id)
            } catch (e: Exception) {
                Logger.w(TAG, e) { "Failed to push delete for partner ${p.id}" }
            }
        }
    }

    // ── Pull: Sessions (LWW merge) ───────────────────────────────────

    private suspend fun pullRemoteSessions(userId: String, lastSyncMs: Long) {
        try {
            val remoteSessions = remoteDataSource.getSessionsUpdatedAfter(userId, lastSyncMs)

            // Upsert newly updated sessions (LWW merge)
            val newSessionIds = mutableListOf<String>()
            for (remoteDto in remoteSessions) {
                val local = sessionDao.getById(remoteDto.id)
                if (local == null || remoteDto.updatedAt > local.updatedAt) {
                    sessionDao.upsert(remoteDto.toEntity(SyncStatus.SYNCED))
                }
                newSessionIds.add(remoteDto.id)
            }

            // Also find sessions in Room that are missing ratings/scores
            // (self-healing for previously failed syncs)
            val incompleteIds = sessionDao.getSessionIdsWithoutRatings(userId)
            val allIds = (newSessionIds + incompleteIds).distinct()

            if (allIds.isNotEmpty()) {
                Logger.d(TAG) { "Pulling ratings/scores for ${allIds.size} sessions " +
                    "(${newSessionIds.size} new, ${incompleteIds.size} incomplete)" }
                pullRatingsAndScoresBulk(allIds)
            }
        } catch (e: Exception) {
            Logger.w(TAG, e) { "Failed to pull remote sessions" }
        }
    }

    private suspend fun pullRatingsAndScoresBulk(sessionIds: List<String>) {
        try {
            val allSelfRatings = remoteDataSource.getSelfRatingsForSessions(sessionIds)
            if (allSelfRatings.isNotEmpty()) {
                val grouped = allSelfRatings.groupBy { it.sessionId }
                for ((sessionId, ratings) in grouped) {
                    selfRatingDao.deleteForSession(sessionId)
                    selfRatingDao.upsertAll(ratings.map { it.toEntity(SyncStatus.SYNCED) })
                }
            }

            val allPartnerRatings = remoteDataSource.getPartnerRatingsForSessions(sessionIds)
            if (allPartnerRatings.isNotEmpty()) {
                val grouped = allPartnerRatings.groupBy { it.sessionId }
                for ((sessionId, ratings) in grouped) {
                    partnerRatingDao.deleteForSession(sessionId)
                    partnerRatingDao.upsertAll(ratings.map { it.toEntity(SyncStatus.SYNCED) })
                }
            }

            val allSetScores = remoteDataSource.getSetScoresForSessions(sessionIds)
            if (allSetScores.isNotEmpty()) {
                val grouped = allSetScores.groupBy { it.sessionId }
                for ((sessionId, scores) in grouped) {
                    setScoreDao.deleteForSession(sessionId)
                    setScoreDao.upsertAll(scores.map { it.toEntity(SyncStatus.SYNCED) })
                }
            }
        } catch (e: Exception) {
            Logger.w(TAG, e) { "Failed to pull ratings/scores in bulk" }
        }
    }

    // ── Pull: Focus Points ────────────────────────────────────────────

    private suspend fun pullRemoteFocusPoints(userId: String) {
        try {
            val remote = remoteDataSource.getFocusPointsForUser(userId)
            for (dto in remote) {
                val local = focusPointDao.getById(dto.id)
                if (local == null || local.syncStatus != SyncStatus.PENDING.name) {
                    // Accept remote if no pending local change
                    focusPointDao.upsert(dto.toEntity(SyncStatus.SYNCED))
                }
            }
        } catch (e: Exception) {
            Logger.w(TAG, e) { "Failed to pull remote focus points" }
        }
    }

    // ── Pull: Opponents ───────────────────────────────────────────────

    private suspend fun pullRemoteOpponents(userId: String) {
        try {
            val remote = remoteDataSource.getOpponentsForUser(userId)
            for (dto in remote) {
                val local = opponentDao.getById(dto.id)
                if (local == null || local.syncStatus != SyncStatus.PENDING.name) {
                    opponentDao.upsert(dto.toEntity(SyncStatus.SYNCED))
                }
            }
        } catch (e: Exception) {
            Logger.w(TAG, e) { "Failed to pull remote opponents" }
        }
    }

    // ── Pull: Partners ────────────────────────────────────────────────

    private suspend fun pullRemotePartners(userId: String) {
        try {
            val remote = remoteDataSource.getPartnersForUser(userId)
            for (dto in remote) {
                val local = partnerDao.getById(dto.id)
                if (local == null || local.syncStatus != SyncStatus.PENDING.name) {
                    partnerDao.upsert(dto.toEntity(SyncStatus.SYNCED))
                }
            }
        } catch (e: Exception) {
            Logger.w(TAG, e) { "Failed to pull remote partners" }
        }
    }
}
