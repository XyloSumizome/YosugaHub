package com.shiro.yosugahub

import com.shiro.yosugahub.data.file.ContextExporter
import com.shiro.yosugahub.data.file.model.ContextExport
import com.shiro.yosugahub.domain.model.CalendarEvent
import com.shiro.yosugahub.domain.model.ItemKind
import com.shiro.yosugahub.domain.model.KnowledgeItem
import com.shiro.yosugahub.domain.model.Project
import com.shiro.yosugahub.domain.model.ProjectStatusSnapshot
import com.shiro.yosugahub.domain.model.StatusLine
import com.shiro.yosugahub.domain.model.Task
import com.shiro.yosugahub.domain.model.TaskStatus
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextExporterTest {

    private val projects = listOf(
        Project(
            id = "anri",
            name = "ANRI",
            currentGoal = "プロトタイプの完成",
            inProgress = "第2章の執筆",
            nextTask = "戦闘バランス調整",
            lastUpdated = "2026-07-22 18:00",
            health = "on_track",
        ),
    )
    private val events = listOf(
        CalendarEvent("歯医者", "10:00", "11:00", "個人"),
    )

    @Test
    fun build_sets_schema_version_and_maps_fields() {
        val export = ContextExporter.build(
            projects = projects,
            events = events,
            generatedAt = "2026-07-22T20:00:00+09:00",
        )
        assertEquals(ContextExport.SCHEMA_VERSION, export.schemaVersion)
        assertEquals("2026-07-22T20:00:00+09:00", export.generatedAt)
        assertEquals(1, export.projects.size)
        assertEquals("anri", export.projects.first().id)
        assertTrue(export.projects.first().statusMarkdown.contains("プロトタイプの完成"))
        assertEquals(1, export.calendar.events.size)
        assertEquals("歯医者", export.calendar.events.first().title)
    }

    @Test
    fun serialized_json_round_trips_and_contains_schema_version() {
        val export = ContextExporter.build(projects, events, "2026-07-22T20:00:00+09:00")
        val jsonText = ContextExporter.toJson(export)
        assertTrue(jsonText.contains("\"schemaVersion\": 2"))
        assertTrue(jsonText.contains("\"responseSchemaVersion\": 2"))

        val decoded = Json.decodeFromString<ContextExport>(jsonText)
        assertEquals(export, decoded)
    }

    @Test
    fun github_status_replaces_local_markdown_and_carries_blockers() {
        val snapshot = ProjectStatusSnapshot(
            projectId = "anri",
            summary = "第2章を執筆中",
            health = "attention",
            phase = "prototype",
            goalTitle = "プロトタイプ完成",
            goalDetail = "第2章まで",
            inProgress = listOf(StatusLine("第2章の執筆", "50%")),
            nextTasks = listOf(StatusLine("戦闘調整", "優先度: high")),
            blockers = listOf(StatusLine("素材待ち", "深刻度: high")),
            decisions = listOf(StatusLine("リズム判定は3段階にする", "2026-07-20 / 操作を単純に保つため")),
            questionsForYosuga = listOf("難易度はどうすべきか"),
            generatedAt = "2026-07-23T21:00:00+09:00",
            sourceCommit = "abc123",
            fetchedAt = "2026-07-23T22:00:00+09:00",
        )
        val export = ContextExporter.build(
            projects, events, "2026-07-23T22:00:00+09:00",
            statuses = mapOf("anri" to snapshot),
        )
        val project = export.projects.single()

        assertEquals("github", project.source)
        assertEquals("attention", project.health)          // status 側の health を優先
        assertEquals("2026-07-23T21:00:00+09:00", project.lastUpdated)  // generatedAt を優先
        assertTrue(project.statusMarkdown.contains("## Summary"))
        assertTrue(project.statusMarkdown.contains("第2章を執筆中"))
        assertTrue(project.statusMarkdown.contains("## Blockers"))
        assertTrue(project.statusMarkdown.contains("素材待ち(深刻度: high)"))
        assertTrue(project.statusMarkdown.contains("## Questions for Yosuga"))
        assertTrue(project.statusMarkdown.contains("abc123"))
        assertEquals(listOf("素材待ち(深刻度: high)"), project.blockers)
        assertEquals(listOf("難易度はどうすべきか"), project.questionsForYosuga)
        // ゲーム側の確定事項もAIへ渡す(これに矛盾する提案をさせないため)
        assertTrue(project.statusMarkdown.contains("## Decisions"))
        assertEquals(
            listOf("リズム判定は3段階にする(2026-07-20 / 操作を単純に保つため)"),
            project.decisions,
        )
    }

    @Test
    fun falls_back_to_local_fields_when_status_not_fetched() {
        val export = ContextExporter.build(projects, events, "2026-07-23T22:00:00+09:00")
        val project = export.projects.single()
        assertEquals("local", project.source)
        assertEquals("on_track", project.health)
        assertEquals("2026-07-22 18:00", project.lastUpdated)
        assertTrue(project.statusMarkdown.contains("プロトタイプの完成"))
        assertTrue(project.blockers.isEmpty())
    }

    @Test
    fun build_maps_tasks_and_decisions_v2() {
        val tasks = listOf(
            Task(
                id = "task-1",
                projectId = "anri",
                title = "戦闘調整",
                detail = "係数見直し",
                status = TaskStatus.DOING,
                priority = "high",
                dueDate = "2026-07-30",
                createdAt = "2026-07-23T09:00:00+09:00",
                updatedAt = "2026-07-23T09:00:00+09:00",
                completedAt = null,
                source = "manual",
            ),
        )
        val decisions = listOf(
            KnowledgeItem(
                id = "item-1",
                kind = ItemKind.DECISION,
                title = "ビート表示を採用",
                body = "理由",
                tags = emptyList(),
                entities = emptyList(),
                createdAt = "2026-07-23T10:00:00+09:00",
                updatedAt = "2026-07-23T10:00:00+09:00",
                source = "assistant",
            ),
        )
        val export = ContextExporter.build(
            projects, events, "2026-07-23T20:00:00+09:00",
            tasks = tasks, decisions = decisions,
        )
        val task = export.tasks.single()
        assertEquals("doing", task.status)
        assertEquals("anri", task.projectId)
        assertEquals("2026-07-30", task.dueDate)
        val decision = export.recentDecisions.single()
        assertEquals("2026-07-23", decision.date)
        assertEquals("ビート表示を採用", decision.title)
    }
}
