package com.ashutosh.mindfultennis.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for the `sessions` table.
 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "focus_note")
    val focusNote: String,

    @ColumnInfo(name = "started_at")
    val startedAt: Long,

    @ColumnInfo(name = "ended_at")
    val endedAt: Long? = null,

    @ColumnInfo(name = "time_zone_id")
    val timeZoneId: String,

    val notes: String? = null,

    @ColumnInfo(name = "match_type")
    val matchType: String = "SINGLES",

    @ColumnInfo(name = "opponent1_id")
    val opponent1Id: String? = null,

    @ColumnInfo(name = "opponent2_id")
    val opponent2Id: String? = null,

    @ColumnInfo(name = "partner_id")
    val partnerId: String? = null,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = false,

    @ColumnInfo(name = "overall_score")
    val overallScore: Int? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @ColumnInfo(name = "schema_version")
    val schemaVersion: Int = 1,

    @ColumnInfo(name = "sport_id")
    val sportId: String = "tennis",

    @ColumnInfo(name = "sync_status")
    val syncStatus: String = SyncStatus.PENDING.name,
)
