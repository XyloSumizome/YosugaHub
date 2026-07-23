package com.shiro.yosugahub

import com.shiro.yosugahub.data.github.StatusParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusParserTest {

    private val validJson = """
        {
          "schemaVersion": 1,
          "projectId": "anri",
          "generatedAt": "2026-07-23T21:00:00+09:00",
          "sourceCommit": "abc123",
          "summary": "プロトタイプの実装を進行中",
          "phase": "prototype",
          "health": "on_track",
          "unknownField": "無視されるべき",
          "currentGoal": { "title": "プロトタイプ完成", "detail": "第2章まで" },
          "completed": [ { "id": "t1", "title": "第1章", "completedAt": "2026-07-20T18:00:00+09:00" } ],
          "inProgress": [ { "id": "t2", "title": "第2章の執筆", "progressPercent": 50 } ],
          "nextTasks": [ { "id": "t3", "title": "戦闘調整", "priority": "high", "estimatedMinutes": 60 } ],
          "blockers": [ { "id": "b1", "title": "素材待ち", "severity": "medium" } ],
          "recentChanges": [ { "date": "2026-07-22", "summary": "会話パート追加", "commit": "def456" } ],
          "questionsForYosuga": ["戦闘の難易度はどうすべきか"]
        }
    """.trimIndent()

    @Test
    fun parses_valid_status_and_ignores_unknown_keys() {
        val result = StatusParser.parse(validJson, expectedProjectId = "anri")
        assertTrue(result is StatusParser.Result.Success)
        val status = (result as StatusParser.Result.Success).status
        assertEquals("anri", status.projectId)
        assertEquals("on_track", status.health)
        assertEquals("プロトタイプ完成", status.currentGoal.title)
        assertEquals(1, status.inProgress.size)
        assertEquals(50, status.inProgress.first().progressPercent)
        assertEquals("high", status.nextTasks.first().priority)
        assertEquals(1, status.blockers.size)
        assertEquals(1, status.questionsForYosuga.size)
    }

    @Test
    fun minimal_status_with_only_required_fields_parses() {
        val result = StatusParser.parse("""{"schemaVersion":1,"projectId":"anri"}""")
        assertTrue(result is StatusParser.Result.Success)
        val status = (result as StatusParser.Result.Success).status
        assertTrue(status.nextTasks.isEmpty())
        assertEquals("", status.summary)
    }

    @Test
    fun rejects_broken_json_without_crashing() {
        val result = StatusParser.parse("{ not json ")
        assertTrue(result is StatusParser.Result.InvalidJson)
    }

    @Test
    fun missing_schema_version_is_invalid() {
        val result = StatusParser.parse("""{"projectId":"anri"}""")
        assertTrue(result is StatusParser.Result.InvalidJson)
    }

    @Test
    fun reports_unsupported_schema_version() {
        val result = StatusParser.parse("""{"schemaVersion":2,"projectId":"anri"}""")
        assertTrue(result is StatusParser.Result.UnsupportedSchema)
        assertEquals(2, (result as StatusParser.Result.UnsupportedSchema).version)
    }

    @Test
    fun detects_project_id_mismatch() {
        val result = StatusParser.parse(validJson, expectedProjectId = "gengenkyo")
        assertTrue(result is StatusParser.Result.ProjectIdMismatch)
        assertEquals("anri", (result as StatusParser.Result.ProjectIdMismatch).actual)
    }

    @Test
    fun skips_project_id_check_when_expectation_is_blank() {
        val result = StatusParser.parse(validJson, expectedProjectId = "")
        assertTrue(result is StatusParser.Result.Success)
    }
}
