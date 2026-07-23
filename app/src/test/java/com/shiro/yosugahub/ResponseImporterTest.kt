package com.shiro.yosugahub

import com.shiro.yosugahub.data.file.ResponseImporter
import com.shiro.yosugahub.data.file.ResponseImporter.ParseResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponseImporterTest {

    @Test
    fun parses_valid_response_and_ignores_unknown_keys() {
        val json = """
            {
              "schemaVersion": 1,
              "generatedAt": "2026-07-22T20:30:00+09:00",
              "summary": "現在の状況の要約",
              "unknownField": "無視されるべき",
              "recommendations": [
                {"projectId": "anri", "title": "次の作業候補", "detail": "具体的な提案", "priority": "high"}
              ],
              "suggestedTasks": [],
              "notes": []
            }
        """.trimIndent()

        val result = ResponseImporter.parse(json)
        assertTrue(result is ParseResult.Success)
        val response = (result as ParseResult.Success).response
        assertEquals(1, response.schemaVersion)
        assertEquals(1, response.recommendations.size)
        assertEquals("anri", response.recommendations.first().projectId)
        assertEquals("high", response.recommendations.first().priority)
    }

    @Test
    fun parses_v2_response_with_proposals() {
        val json = """
            {
              "schemaVersion": 2,
              "summary": "v2の提案",
              "unknownField": "無視されるべき",
              "proposals": {
                "tasks": [
                  {"projectId": "anri", "title": "戦闘調整", "priority": "high", "dueDate": "2026-07-30"}
                ],
                "items": [
                  {"kind": "decision", "title": "ビート表示を採用", "body": "理由...",
                   "tags": ["Yosuga Hub"], "entities": [{"name": "Yosuga Hub", "type": "project"}]}
                ],
                "diary": [
                  {"date": "2026-07-23", "body": "今日はシロさんが..."}
                ],
                "projectHealth": [
                  {"projectId": "gengenkyo", "health": "停滞中", "reason": "2週間更新なし"}
                ]
              }
            }
        """.trimIndent()

        val result = ResponseImporter.parse(json)
        assertTrue(result is ParseResult.SuccessV2)
        val response = (result as ParseResult.SuccessV2).response
        assertEquals(1, response.proposals.tasks.size)
        assertEquals("anri", response.proposals.tasks.first().projectId)
        assertEquals("decision", response.proposals.items.first().kind)
        assertEquals(listOf("Yosuga Hub"), response.proposals.items.first().tags)
        assertEquals("2026-07-23", response.proposals.diary.first().date)
        assertEquals("停滞中", response.proposals.projectHealth.first().health)
    }

    @Test
    fun v2_with_missing_proposals_defaults_to_empty() {
        val result = ResponseImporter.parse("""{"schemaVersion": 2}""")
        assertTrue(result is ParseResult.SuccessV2)
        val response = (result as ParseResult.SuccessV2).response
        assertTrue(response.proposals.tasks.isEmpty())
        assertTrue(response.proposals.items.isEmpty())
    }

    @Test
    fun rejects_broken_json_without_crashing() {
        val result = ResponseImporter.parse("{ this is not json ")
        assertTrue(result is ParseResult.InvalidJson)
    }

    @Test
    fun reports_unsupported_schema_version() {
        val json = """{"schemaVersion": 3, "recommendations": []}"""
        val result = ResponseImporter.parse(json)
        assertTrue(result is ParseResult.UnsupportedSchema)
        assertEquals(3, (result as ParseResult.UnsupportedSchema).version)
    }

    @Test
    fun treats_missing_schema_version_as_invalid() {
        val json = """{"summary": "no version here"}"""
        val result = ResponseImporter.parse(json)
        assertTrue(result is ParseResult.InvalidJson)
    }

    /** 設計書v4.1 の例そのままの形(snake_case)を受け取れること。 */
    @Test
    fun parses_v2_classifications_in_snake_case() {
        val json = """
            {
              "schemaVersion": 2,
              "proposals": {
                "classifications": [
                  {
                    "document_id": "doc_001",
                    "project_ids": ["fragile-hero"],
                    "categories": ["game-design", "player-action"],
                    "tags": ["grapple", "frog", "rhythm-action"],
                    "document_type": "design-discussion",
                    "summary": "グラップル仕様に関する検討",
                    "related_entities": [{"type": "feature", "id": "grapple"}],
                    "confidence": 0.91,
                    "requires_user_confirmation": true
                  }
                ]
              }
            }
        """.trimIndent()

        val result = ResponseImporter.parse(json)
        assertTrue(result is ParseResult.SuccessV2)
        val classification = (result as ParseResult.SuccessV2)
            .response.proposals.classifications.single()
        assertEquals("doc_001", classification.documentId)
        assertEquals(listOf("fragile-hero"), classification.projectIds)
        assertEquals("design-discussion", classification.documentType)
        assertEquals(listOf("game-design", "player-action"), classification.categories)
        assertEquals("feature", classification.relatedEntities.single().type)
        assertEquals("grapple", classification.relatedEntities.single().id)
        assertEquals(0.91, classification.confidence!!, 0.0001)
    }

    /** 分類が無い回答でも空リストとして扱えること(既存の提案だけの回答が壊れない)。 */
    @Test
    fun v2_without_classifications_yields_empty_list() {
        val json = """{"schemaVersion": 2, "proposals": {"tasks": []}}"""
        val result = ResponseImporter.parse(json)
        assertTrue(result is ParseResult.SuccessV2)
        assertTrue((result as ParseResult.SuccessV2).response.proposals.classifications.isEmpty())
    }
}
