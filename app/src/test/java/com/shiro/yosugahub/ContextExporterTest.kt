package com.shiro.yosugahub

import com.shiro.yosugahub.data.file.ContextExporter
import com.shiro.yosugahub.data.file.model.ContextExport
import com.shiro.yosugahub.domain.model.CalendarEvent
import com.shiro.yosugahub.domain.model.ItemKind
import com.shiro.yosugahub.domain.model.KnowledgeItem
import com.shiro.yosugahub.domain.model.Project
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
