package com.shiro.yosugahub.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** projects テーブル。projectId を主キーとする(設計書19.1: projectId は変更しない)。 */
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val currentGoal: String,
    val inProgress: String,
    val nextTask: String,
    val lastUpdated: String,
    val health: String,
)
