package com.shiro.yosugahub.data.github

import com.shiro.yosugahub.data.github.model.ProjectStatus
import com.shiro.yosugahub.data.github.model.StatusBlocker
import com.shiro.yosugahub.data.github.model.StatusChange
import com.shiro.yosugahub.data.github.model.StatusDecision
import com.shiro.yosugahub.data.github.model.StatusTask
import com.shiro.yosugahub.domain.model.ProjectStatusSnapshot
import com.shiro.yosugahub.domain.model.StatusBlockerLine
import com.shiro.yosugahub.domain.model.StatusChangeLine
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
        decisions = decisions.toDecisionLines(),
        recentChanges = recentChanges.toChangeLines(),
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

/** 決定事項。日付は詳細の先頭に置き、いつ決まったかを分かるようにする。 */
private fun List<StatusDecision>.toDecisionLines(): List<StatusLine> =
    filter { it.title.isNotBlank() }.map { decision ->
        StatusLine(
            title = decision.title,
            detail = buildList {
                if (decision.date.isNotBlank()) add(decision.date)
                if (decision.detail.isNotBlank()) add(decision.detail)
            }.joinToString(" / "),
        )
    }

/**
 * 修正のログ。日付は**畳まずに残す**(近況報告で「直近2週間」を絞るのに使うため)。
 * summary が空の行は情報が無いので落とす。
 */
private fun List<StatusChange>.toChangeLines(): List<StatusChangeLine> =
    filter { it.summary.isNotBlank() }.map { change ->
        StatusChangeLine(
            date = change.date,
            summary = change.summary,
            commit = change.commit,
        )
    }

/** ブロッカー。**severity と since を畳まずに残す**(いつからかを読み手に渡すため)。 */
private fun List<StatusBlocker>.toBlockerLines(): List<StatusBlockerLine> =
    filter { it.title.isNotBlank() }.map { blocker ->
        StatusBlockerLine(
            title = blocker.title,
            detail = blocker.detail,
            severity = blocker.severity,
            since = blocker.since,
        )
    }
