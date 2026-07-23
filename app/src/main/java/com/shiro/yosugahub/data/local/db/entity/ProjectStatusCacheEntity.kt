package com.shiro.yosugahub.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * project_status_cache テーブル(v5)。GitHub から取得した status.json のキャッシュ。
 * 取得した本文をそのまま保存し(未知項目を失わない)、表示時にパースする。
 * オフラインでも直近の進捗を表示できるようにするのが目的。
 */
@Entity(tableName = "project_status_cache")
data class ProjectStatusCacheEntity(
    @PrimaryKey val projectId: String,
    val statusJson: String,
    val fetchedAt: String,  // ISO 8601
)
