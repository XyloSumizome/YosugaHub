package com.shiro.yosugahub.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * tasks テーブル(v3-Step 1)。id は UUID 文字列。
 * projectId は projects.id への緩い参照(null = プロジェクト外タスク)。
 * 外部キー制約は張らない: プロジェクトは将来 GitHub 由来へ差し替わる予定で、
 * 参照切れでタスクを失わないことを優先する。
 */
@Entity(
    tableName = "tasks",
    indices = [Index("projectId")],
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val projectId: String?,
    val title: String,
    val detail: String,
    val status: String,        // todo / doing / done
    val priority: String,      // high / medium / low
    val dueDate: String?,      // "yyyy-MM-dd"
    val createdAt: String,     // ISO 8601
    val updatedAt: String,     // ISO 8601
    val completedAt: String?,  // ISO 8601。未完了は null
    val source: String,        // manual / assistant
)
