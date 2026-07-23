package com.shiro.yosugahub.data.github

import com.shiro.yosugahub.data.github.model.ProjectStatus
import com.shiro.yosugahub.data.github.model.StatusBlocker
import com.shiro.yosugahub.data.github.model.StatusTask
import com.shiro.yosugahub.domain.model.ProjectStatusSnapshot
import com.shiro.yosugahub.domain.model.StatusLine

/**
 * 通信DTO(ProjectStatus)→ 表示用ドメインモデルへの変換(純粋ロジック)。
 * 空タイトルの項目は表示しても意味がないため落とす。
 */
fun ProjectStatus.toSnapshot(projectId: String, fetchedAt: String): ProjectStatusSnapshot =
    ProjectStatusSnapshot(
        projectId = projectId,
        summary = summary,
        health = health,
        phase = phase,
        goalTitle = currentGoal.title,
        goalDetail = currentGoal.detail,
        inProgress = inProgress.toLines(),
        nextTasks = nextTasks.toLines(),
        blockers = blockers.toBlockerLines(),
        questionsForYosuga = questionsForYosuga.filter { it.isNotBlank() },
        generatedAt = generatedAt,
        sourceCommit = sourceCommit,
        fetchedAt = fetchedAt,
    )

private fun List<StatusTask>.toLines(): List<StatusLine> =
    filter { it.title.isNotBlank() }.map { task ->
        StatusLine(
            title = task.title,
            detail = buildList {
                if (task.detail.isNotBlank()) add(task.detail)
                task.progressPercent?.let { add("$it%") }
                if (task.priority.isNotBlank() && task.priority != "medium") {
                    add("優先度: ${task.priority}")
                }
            }.joinToString(" / "),
        )
    }

private fun List<StatusBlocker>.toBlockerLines(): List<StatusLine> =
    filter { it.title.isNotBlank() }.map { blocker ->
        StatusLine(
            title = blocker.title,
            detail = buildList {
                if (blocker.detail.isNotBlank()) add(blocker.detail)
                if (blocker.severity.isNotBlank()) add("深刻度: ${blocker.severity}")
            }.joinToString(" / "),
        )
    }
