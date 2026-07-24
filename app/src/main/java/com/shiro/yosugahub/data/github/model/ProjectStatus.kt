package com.shiro.yosugahub.data.github.model

import kotlinx.serialization.Serializable

/**
 * 各ゲームリポジトリの `.yosuga/status.json`(設計書19.2)。
 * 各ゲームの Claude Code が更新する。未知の項目は無視し、欠けても落ちないよう
 * 必須は schemaVersion / projectId のみとし、他はデフォルト値付きにする。
 */
@Serializable
data class ProjectStatus(
    val schemaVersion: Int,
    val projectId: String = "",
    val generatedAt: String = "",
    val sourceCommit: String = "",
    val summary: String = "",
    val phase: String = "",
    val health: String = "",
    val currentGoal: StatusGoal = StatusGoal(),
    val completed: List<StatusTask> = emptyList(),
    val inProgress: List<StatusTask> = emptyList(),
    val nextTasks: List<StatusTask> = emptyList(),
    val blockers: List<StatusBlocker> = emptyList(),
    val recentChanges: List<StatusChange> = emptyList(),
    val risks: List<StatusBlocker> = emptyList(),
    val decisions: List<StatusDecision> = emptyList(),
    /**
     * ゲーム側が文字列配列で書く想定だが、オブジェクト配列や単体文字列で来ることがある。
     * 1項目の型ゆれで status.json 全体が読めなくなるのを避けるため型ゆれを吸収する。
     */
    @Serializable(with = FlexibleTextListSerializer::class)
    val questionsForYosuga: List<String> = emptyList(),
)

@Serializable
data class StatusGoal(
    val title: String = "",
    val detail: String = "",
)

@Serializable
data class StatusTask(
    val id: String = "",
    val title: String = "",
    val detail: String = "",
    val priority: String = "medium",
    val progressPercent: Int? = null,
    val estimatedMinutes: Int? = null,
    val completedAt: String = "",
)

@Serializable
data class StatusBlocker(
    val id: String = "",
    val title: String = "",
    val detail: String = "",
    val severity: String = "medium",
)

@Serializable
data class StatusChange(
    val date: String = "",
    val summary: String = "",
    val commit: String = "",
)

@Serializable
data class StatusDecision(
    val date: String = "",
    val title: String = "",
    val detail: String = "",
)
