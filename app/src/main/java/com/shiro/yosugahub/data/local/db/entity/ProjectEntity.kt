package com.shiro.yosugahub.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * projects テーブル。projectId を主キーとする(設計書19.1: projectId は変更しない)。
 * v4: GitHub リポジトリ情報を追加(未設定なら null = 取得対象外)。
 */
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val currentGoal: String,
    val inProgress: String,
    val nextTask: String,
    val lastUpdated: String,
    val health: String,
    val repoOwner: String? = null,
    val repoName: String? = null,
    val repoBranch: String? = null,
)
