package com.shiro.yosugahub.data.file

import com.shiro.yosugahub.data.file.model.CalendarExport
import com.shiro.yosugahub.data.file.model.ContextExport
import com.shiro.yosugahub.data.file.model.DecisionExport
import com.shiro.yosugahub.data.file.model.EventExport
import com.shiro.yosugahub.data.file.model.ProjectExport
import com.shiro.yosugahub.data.file.model.TaskExport
import com.shiro.yosugahub.data.file.model.UserContext
import com.shiro.yosugahub.domain.model.CalendarEvent
import com.shiro.yosugahub.domain.model.KnowledgeItem
import com.shiro.yosugahub.domain.model.Project
import com.shiro.yosugahub.domain.model.ProjectStatusSnapshot
import com.shiro.yosugahub.domain.model.StatusLine
import com.shiro.yosugahub.domain.model.Task
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * ドメインモデルから状況JSON(ContextExport)を組み立て、文字列へ直列化する純粋ロジック。
 * I/O やプラットフォーム依存を持たないためユニットテスト可能。
 */
object ContextExporter {

    private const val DEFAULT_PURPOSE = "ゲーム制作の状況共有と相談"

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    /**
     * @param statuses GitHub 由来の進捗(projectId → スナップショット)。
     *   取得済みのプロジェクトは手元の値より優先して反映する。
     */
    fun build(
        projects: List<Project>,
        events: List<CalendarEvent>,
        generatedAt: String,
        tasks: List<Task> = emptyList(),
        decisions: List<KnowledgeItem> = emptyList(),
        statuses: Map<String, ProjectStatusSnapshot> = emptyMap(),
        pastDays: Int = 7,
        futureDays: Int = 7,
        purpose: String = DEFAULT_PURPOSE,
    ): ContextExport = ContextExport(
        generatedAt = generatedAt,
        userContext = UserContext(purpose = purpose),
        calendar = CalendarExport(
            pastDays = pastDays,
            futureDays = futureDays,
            events = events.map { it.toExport() },
        ),
        projects = projects.map { projectExportOf(it, statuses[it.id]) },
        tasks = tasks.map { it.toExport() },
        recentDecisions = decisions.map { it.toDecisionExport() },
    )

    fun toJson(export: ContextExport): String = json.encodeToString(export)

    /** タスクの共通変換(状況JSONと AiExporter で共有)。 */
    fun taskExportOf(task: Task): TaskExport = TaskExport(
        projectId = task.projectId,
        title = task.title,
        detail = task.detail,
        status = task.status.dbValue,
        priority = task.priority,
        dueDate = task.dueDate,
    )

    private fun Task.toExport(): TaskExport = taskExportOf(this)

    private fun KnowledgeItem.toDecisionExport(): DecisionExport = DecisionExport(
        date = createdAt.take(10),
        title = title,
        body = body,
    )

    private fun CalendarEvent.toExport(): EventExport = EventExport(
        title = title,
        start = start,
        end = end,
        calendarName = calendarName,
        description = description,
    )

    /**
     * プロジェクトの共通変換(状況JSONと AiExporter で共有)。
     * GitHub の status.json を取得済みならそれを Markdown 化して渡す(source=github)。
     * 未取得なら手元の構造化フィールドから簡易 Markdown を組み立てる(source=local)。
     */
    fun projectExportOf(project: Project, snapshot: ProjectStatusSnapshot?): ProjectExport =
        if (snapshot == null) {
            ProjectExport(
                id = project.id,
                name = project.name,
                statusMarkdown = buildString {
                    append("## Current Goal\n").append(project.currentGoal).append("\n\n")
                    append("## In Progress\n").append(project.inProgress).append("\n\n")
                    append("## Next Tasks\n").append(project.nextTask)
                },
                lastUpdated = project.lastUpdated,
                source = SOURCE_LOCAL,
                health = project.health,
            )
        } else {
            ProjectExport(
                id = project.id,
                name = project.name,
                statusMarkdown = snapshot.toStatusMarkdown(),
                // GitHub 側の生成時刻があればそちらを使う(なければ手元の最終更新)。
                lastUpdated = snapshot.generatedAt.ifBlank { project.lastUpdated },
                source = SOURCE_GITHUB,
                health = snapshot.health.ifBlank { project.health },
                blockers = snapshot.blockers.map { line ->
                    if (line.detail.isBlank()) line.title else "${line.title}(${line.detail})"
                },
                questionsForYosuga = snapshot.questionsForYosuga,
                decisions = snapshot.decisions.map { line ->
                    if (line.detail.isBlank()) line.title else "${line.title}(${line.detail})"
                },
            )
        }

    /** status.json のスナップショットを status.md 相当の Markdown へ(設計書19.3の見出し構成)。 */
    private fun ProjectStatusSnapshot.toStatusMarkdown(): String = buildString {
        if (summary.isNotBlank()) {
            append("## Summary\n").append(summary).append("\n\n")
        }
        if (goalTitle.isNotBlank() || goalDetail.isNotBlank()) {
            append("## Current Goal\n").append(goalTitle)
            if (goalDetail.isNotBlank()) append("\n").append(goalDetail)
            append("\n\n")
        }
        appendLines("In Progress", inProgress.map { it.toMarkdownLine() })
        appendLines("Next Tasks", nextTasks.map { it.toMarkdownLine() })
        appendLines("Blockers", blockers.map { it.toMarkdownLine() })
        appendLines("Decisions", decisions.map { it.toMarkdownLine() })
        appendLines("Questions for Yosuga", questionsForYosuga)
        if (sourceCommit.isNotBlank()) {
            append("## Source Commit\n").append(sourceCommit).append("\n")
        }
    }.trimEnd()

    private fun StringBuilder.appendLines(heading: String, lines: List<String>) {
        if (lines.isEmpty()) return
        append("## ").append(heading).append("\n")
        lines.forEach { append("- ").append(it).append("\n") }
        append("\n")
    }

    private fun StatusLine.toMarkdownLine(): String =
        if (detail.isBlank()) title else "$title(${detail})"

    private const val SOURCE_LOCAL = "local"
    private const val SOURCE_GITHUB = "github"
}
