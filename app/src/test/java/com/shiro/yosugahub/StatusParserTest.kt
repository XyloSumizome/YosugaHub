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

    private fun successOf(json: String): StatusParser.Result.Success {
        val result = StatusParser.parse(json)
        assertTrue("読めなかった: $result", result is StatusParser.Result.Success)
        return result as StatusParser.Result.Success
    }

    /** 紙エルのように questionsForYosuga をオブジェクト配列で書く例。落とさず読む。 */
    @Test
    fun reads_questions_written_as_objects() {
        val status = successOf(
            """
            {
              "schemaVersion": 1,
              "projectId": "paper-armor-frog",
              "questionsForYosuga": [
                { "id": "q1", "question": "難易度はどうすべきか" },
                { "id": "q2", "text": "章立ては足りているか" },
                { "id": "q3", "title": "BGMの方向性" }
              ]
            }
            """.trimIndent(),
        ).status
        assertEquals(
            listOf("難易度はどうすべきか", "章立ては足りているか", "BGMの方向性"),
            status.questionsForYosuga,
        )
    }

    /** 既知のキーが無いオブジェクトでも、最初の非空な文字列を拾う。 */
    @Test
    fun falls_back_to_first_string_value_in_unknown_object() {
        val status = successOf(
            """
            {"schemaVersion":1,"projectId":"anri",
             "questionsForYosuga":[{"id":"","ask":"素材はどこまで揃える?"}]}
            """.trimIndent(),
        ).status
        assertEquals(listOf("素材はどこまで揃える?"), status.questionsForYosuga)
    }

    /** 配列で書き忘れて単体の文字列にした例。 */
    @Test
    fun reads_questions_written_as_a_bare_string() {
        val status = successOf(
            """{"schemaVersion":1,"projectId":"anri","questionsForYosuga":"難易度はどうすべきか"}""",
        ).status
        assertEquals(listOf("難易度はどうすべきか"), status.questionsForYosuga)
    }

    /** null や空文字は落とし、混在した文字列/オブジェクトは両方読む。 */
    @Test
    fun drops_blank_entries_and_reads_mixed_shapes() {
        val status = successOf(
            """
            {"schemaVersion":1,"projectId":"anri",
             "questionsForYosuga":["生の文字列", "", null, {"id":"q9"}, {"question":"最後の質問"}]}
            """.trimIndent(),
        ).status
        assertEquals(listOf("生の文字列", "最後の質問"), status.questionsForYosuga)
    }

    /** null 丸ごと・数値混じりでも落ちない。 */
    @Test
    fun tolerates_null_and_non_string_scalars() {
        assertEquals(
            emptyList<String>(),
            successOf("""{"schemaVersion":1,"projectId":"anri","questionsForYosuga":null}""")
                .status.questionsForYosuga,
        )
        assertEquals(
            listOf("42"),
            successOf("""{"schemaVersion":1,"projectId":"anri","questionsForYosuga":[42]}""")
                .status.questionsForYosuga,
        )
    }
}
