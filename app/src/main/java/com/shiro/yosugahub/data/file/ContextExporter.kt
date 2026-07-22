package com.shiro.yosugahub.data.file

import com.shiro.yosugahub.data.file.model.CalendarExport
import com.shiro.yosugahub.data.file.model.ContextExport
import com.shiro.yosugahub.data.file.model.EventExport
import com.shiro.yosugahub.data.file.model.ProjectExport
import com.shiro.yosugahub.data.file.model.UserContext
import com.shiro.yosugahub.domain.model.CalendarEvent
import com.shiro.yosugahub.domain.model.Project
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

    fun build(
        projects: List<Project>,
        events: List<CalendarEvent>,
        generatedAt: String,
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
        projects = projects.map { it.toExport() },
    )

    fun toJson(export: ContextExport): String = json.encodeToString(export)

    private fun CalendarEvent.toExport(): EventExport = EventExport(
        title = title,
        start = start,
        end = end,
        calendarName = calendarName,
        description = description,
    )

    /**
     * 現状はまだ GitHub 由来の status.md を持たないため(Phase 3)、
     * 手元の構造化フィールドから簡易 Markdown を組み立てて statusMarkdown に入れる。
     */
    private fun Project.toExport(): ProjectExport = ProjectExport(
        id = id,
        name = name,
        statusMarkdown = buildString {
            append("## Current Goal\n").append(currentGoal).append("\n\n")
            append("## In Progress\n").append(inProgress).append("\n\n")
            append("## Next Tasks\n").append(nextTask)
        },
        lastUpdated = lastUpdated,
    )
}
