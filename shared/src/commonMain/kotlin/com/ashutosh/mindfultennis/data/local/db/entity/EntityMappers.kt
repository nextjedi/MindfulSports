package com.ashutosh.mindfultennis.data.local.db.entity

import com.ashutosh.mindfultennis.data.remote.model.FocusPointDto
import com.ashutosh.mindfultennis.data.remote.model.OpponentDto
import com.ashutosh.mindfultennis.data.remote.model.PartnerDto
import com.ashutosh.mindfultennis.data.remote.model.PartnerRatingDto
import com.ashutosh.mindfultennis.data.remote.model.SelfRatingDto
import com.ashutosh.mindfultennis.data.remote.model.SessionDto
import com.ashutosh.mindfultennis.data.remote.model.SetScoreDto
import com.ashutosh.mindfultennis.domain.model.Aspect
import com.ashutosh.mindfultennis.domain.model.FocusPoint
import com.ashutosh.mindfultennis.domain.model.MatchType
import com.ashutosh.mindfultennis.domain.model.Opponent
import com.ashutosh.mindfultennis.domain.model.Partner
import com.ashutosh.mindfultennis.domain.model.Rating
import com.ashutosh.mindfultennis.domain.model.Session
import com.ashutosh.mindfultennis.domain.model.SetScore

// ── Session ───────────────────────────────────────────────────────────

fun SessionEntity.toDomain(): Session = Session(
    id = id,
    userId = userId,
    sportId = sportId,
    focusNote = focusNote,
    startedAt = startedAt,
    endedAt = endedAt,
    timeZoneId = timeZoneId,
    notes = notes,
    matchType = runCatching { MatchType.valueOf(matchType) }.getOrDefault(MatchType.SINGLES),
    opponent1Id = opponent1Id,
    opponent2Id = opponent2Id,
    partnerId = partnerId,
    isActive = isActive,
    overallScore = overallScore,
    createdAt = createdAt,
    updatedAt = updatedAt,
    schemaVersion = schemaVersion,
)

fun Session.toEntity(syncStatus: SyncStatus = SyncStatus.PENDING): SessionEntity = SessionEntity(
    id = id,
    userId = userId,
    sportId = sportId,
    focusNote = focusNote,
    startedAt = startedAt,
    endedAt = endedAt,
    timeZoneId = timeZoneId,
    notes = notes,
    matchType = matchType.name,
    opponent1Id = opponent1Id,
    opponent2Id = opponent2Id,
    partnerId = partnerId,
    isActive = isActive,
    overallScore = overallScore,
    createdAt = createdAt,
    updatedAt = updatedAt,
    schemaVersion = schemaVersion,
    syncStatus = syncStatus.name,
)

fun SessionEntity.toDto(): SessionDto = SessionDto(
    id = id,
    userId = userId,
    sportId = sportId,
    focusNote = focusNote,
    startedAt = startedAt,
    endedAt = endedAt,
    timeZoneId = timeZoneId,
    notes = notes,
    matchType = matchType,
    opponent1Id = opponent1Id,
    opponent2Id = opponent2Id,
    partnerId = partnerId,
    isActive = isActive,
    overallScore = overallScore,
    createdAt = createdAt,
    updatedAt = updatedAt,
    schemaVersion = schemaVersion,
)

fun SessionDto.toEntity(syncStatus: SyncStatus = SyncStatus.SYNCED): SessionEntity = SessionEntity(
    id = id,
    userId = userId,
    sportId = sportId,
    focusNote = focusNote,
    startedAt = startedAt,
    endedAt = endedAt,
    timeZoneId = timeZoneId,
    notes = notes,
    matchType = matchType,
    opponent1Id = opponent1Id,
    opponent2Id = opponent2Id,
    partnerId = partnerId,
    isActive = isActive,
    overallScore = overallScore,
    createdAt = createdAt,
    updatedAt = updatedAt,
    schemaVersion = schemaVersion,
    syncStatus = syncStatus.name,
)

// ── Self Rating ───────────────────────────────────────────────────────

fun SelfRatingEntity.toDomain(): Rating = Rating(
    id = id,
    sessionId = sessionId,
    aspect = runCatching { Aspect.valueOf(aspect) }.getOrDefault(Aspect.FOREHAND),
    rating = rating,
)

fun Rating.toSelfRatingEntity(syncStatus: SyncStatus = SyncStatus.PENDING): SelfRatingEntity =
    SelfRatingEntity(
        id = id,
        sessionId = sessionId,
        aspect = aspect.name,
        rating = rating,
        syncStatus = syncStatus.name,
    )

fun SelfRatingEntity.toDto(): SelfRatingDto = SelfRatingDto(
    id = id,
    sessionId = sessionId,
    aspect = aspect,
    rating = rating,
)

fun SelfRatingDto.toEntity(syncStatus: SyncStatus = SyncStatus.SYNCED): SelfRatingEntity =
    SelfRatingEntity(
        id = id,
        sessionId = sessionId,
        aspect = aspect,
        rating = rating,
        syncStatus = syncStatus.name,
    )

// ── Partner Rating ────────────────────────────────────────────────────

fun PartnerRatingEntity.toDomain(): Rating = Rating(
    id = id,
    sessionId = sessionId,
    aspect = runCatching { Aspect.valueOf(aspect) }.getOrDefault(Aspect.FOREHAND),
    rating = rating,
)

fun Rating.toPartnerRatingEntity(syncStatus: SyncStatus = SyncStatus.PENDING): PartnerRatingEntity =
    PartnerRatingEntity(
        id = id,
        sessionId = sessionId,
        aspect = aspect.name,
        rating = rating,
        syncStatus = syncStatus.name,
    )

fun PartnerRatingEntity.toDto(): PartnerRatingDto = PartnerRatingDto(
    id = id,
    sessionId = sessionId,
    aspect = aspect,
    rating = rating,
)

fun PartnerRatingDto.toEntity(syncStatus: SyncStatus = SyncStatus.SYNCED): PartnerRatingEntity =
    PartnerRatingEntity(
        id = id,
        sessionId = sessionId,
        aspect = aspect,
        rating = rating,
        syncStatus = syncStatus.name,
    )

// ── Focus Point ───────────────────────────────────────────────────────

fun FocusPointEntity.toDomain(): FocusPoint = FocusPoint(
    id = id,
    userId = userId,
    sportId = sportId,
    text = text,
    category = category,
    createdAt = createdAt,
)

fun FocusPoint.toEntity(syncStatus: SyncStatus = SyncStatus.PENDING): FocusPointEntity =
    FocusPointEntity(
        id = id,
        userId = userId,
        sportId = sportId,
        text = text,
        category = category,
        createdAt = createdAt,
        syncStatus = syncStatus.name,
    )

fun FocusPointEntity.toDto(): FocusPointDto = FocusPointDto(
    id = id,
    userId = userId,
    sportId = sportId,
    text = text,
    category = category,
    createdAt = createdAt,
)

fun FocusPointDto.toEntity(syncStatus: SyncStatus = SyncStatus.SYNCED): FocusPointEntity =
    FocusPointEntity(
        id = id,
        userId = userId,
        sportId = sportId,
        text = text,
        category = category,
        createdAt = createdAt,
        syncStatus = syncStatus.name,
    )

// ── Opponent ──────────────────────────────────────────────────────────

fun OpponentEntity.toDomain(): Opponent = Opponent(
    id = id,
    userId = userId,
    sportId = sportId,
    name = name,
    createdAt = createdAt,
)

fun Opponent.toEntity(syncStatus: SyncStatus = SyncStatus.PENDING): OpponentEntity = OpponentEntity(
    id = id,
    userId = userId,
    sportId = sportId,
    name = name,
    createdAt = createdAt,
    syncStatus = syncStatus.name,
)

fun OpponentEntity.toDto(): OpponentDto = OpponentDto(
    id = id,
    userId = userId,
    sportId = sportId,
    name = name,
    createdAt = createdAt,
)

fun OpponentDto.toEntity(syncStatus: SyncStatus = SyncStatus.SYNCED): OpponentEntity =
    OpponentEntity(
        id = id,
        userId = userId,
        sportId = sportId,
        name = name,
        createdAt = createdAt,
        syncStatus = syncStatus.name,
    )

// ── Partner ───────────────────────────────────────────────────────────

fun PartnerEntity.toDomain(): Partner = Partner(
    id = id,
    userId = userId,
    sportId = sportId,
    name = name,
    createdAt = createdAt,
)

fun Partner.toEntity(syncStatus: SyncStatus = SyncStatus.PENDING): PartnerEntity = PartnerEntity(
    id = id,
    userId = userId,
    sportId = sportId,
    name = name,
    createdAt = createdAt,
    syncStatus = syncStatus.name,
)

fun PartnerEntity.toDto(): PartnerDto = PartnerDto(
    id = id,
    userId = userId,
    sportId = sportId,
    name = name,
    createdAt = createdAt,
)

fun PartnerDto.toEntity(syncStatus: SyncStatus = SyncStatus.SYNCED): PartnerEntity = PartnerEntity(
    id = id,
    userId = userId,
    sportId = sportId,
    name = name,
    createdAt = createdAt,
    syncStatus = syncStatus.name,
)

// ── Set Score ─────────────────────────────────────────────────────────

fun SetScoreEntity.toDomain(): SetScore = SetScore(
    id = id,
    sessionId = sessionId,
    setNumber = setNumber,
    userScore = userScore,
    opponentScore = opponentScore,
    opponentId = opponentId,
)

fun SetScore.toEntity(syncStatus: SyncStatus = SyncStatus.PENDING): SetScoreEntity = SetScoreEntity(
    id = id,
    sessionId = sessionId,
    setNumber = setNumber,
    userScore = userScore,
    opponentScore = opponentScore,
    opponentId = opponentId,
    syncStatus = syncStatus.name,
)

fun SetScoreEntity.toDto(): SetScoreDto = SetScoreDto(
    id = id,
    sessionId = sessionId,
    setNumber = setNumber,
    userScore = userScore,
    opponentScore = opponentScore,
    opponentId = opponentId,
)

fun SetScoreDto.toEntity(syncStatus: SyncStatus = SyncStatus.SYNCED): SetScoreEntity =
    SetScoreEntity(
        id = id,
        sessionId = sessionId,
        setNumber = setNumber,
        userScore = userScore,
        opponentScore = opponentScore,
        opponentId = opponentId,
        syncStatus = syncStatus.name,
    )
