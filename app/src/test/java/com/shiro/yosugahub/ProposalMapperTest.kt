package com.shiro.yosugahub

import com.shiro.yosugahub.data.file.ProposalMapper
import com.shiro.yosugahub.data.file.model.AssistantResponseV2
import com.shiro.yosugahub.data.file.model.DiaryProposal
import com.shiro.yosugahub.data.file.model.HealthProposal
import com.shiro.yosugahub.data.file.model.ItemProposal
import com.shiro.yosugahub.data.file.model.ProposalsImport
import com.shiro.yosugahub.data.file.model.TaskProposal
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProposalMapperTest {

    private val receivedAt = "2026-07-23T16:00:00+09:00"
    private var counter = 0
    private fun newId(): String = "prop-${counter++}"

    @Test
    fun converts_each_proposal_kind_to_pending_rows() {
        val response = AssistantResponseV2(
            schemaVersion = 2,
            proposals = ProposalsImport(
                tasks = listOf(TaskProposal(projectId = "anri", title = "戦闘調整")),
                items = listOf(ItemProposal(kind = "decision", title = "ビート表示を採用")),
                diary = listOf(DiaryProposal(date = "2026-07-23", body = "今日は...")),
                projectHealth = listOf(HealthProposal(projectId = "gengenkyo", health = "停滞中")),
            ),
        )
        val rows = ProposalMapper.toPendingEntities(response, receivedAt) { newId() }

        assertEquals(4, rows.size)
        assertEquals(listOf("task", "item", "diary", "health"), rows.map { it.type })
        assertTrue(rows.all { it.status == "pending" })
        assertTrue(rows.all { it.receivedAt == receivedAt })
        assertEquals(rows.size, rows.map { it.id }.toSet().size)
    }

    @Test
    fun payload_json_round_trips_back_to_proposal() {
        val response = AssistantResponseV2(
            schemaVersion = 2,
            proposals = ProposalsImport(
                items = listOf(
                    ItemProposal(
                        kind = "shopping",
                        title = "USB-Cハブ",
                        body = "HDMI付き",
                        tags = listOf("買い物"),
                    )
                ),
            ),
        )
        val row = ProposalMapper.toPendingEntities(response, receivedAt) { newId() }.single()
        val decoded = Json.decodeFromString<ItemProposal>(row.payloadJson)
        assertEquals("shopping", decoded.kind)
        assertEquals("USB-Cハブ", decoded.title)
        assertEquals(listOf("買い物"), decoded.tags)
    }

    @Test
    fun skips_meaningless_proposals() {
        val response = AssistantResponseV2(
            schemaVersion = 2,
            proposals = ProposalsImport(
                tasks = listOf(TaskProposal(title = "  ")),                     // 空タイトル
                items = listOf(ItemProposal(title = "")),                        // 空タイトル
                diary = listOf(DiaryProposal(date = "2026-07-23", body = "")),   // 空本文
                projectHealth = listOf(HealthProposal(projectId = "", health = "順調")), // projectId欠落
            ),
        )
        assertTrue(ProposalMapper.toPendingEntities(response, receivedAt) { newId() }.isEmpty())
    }

    @Test
    fun empty_proposals_produce_no_rows() {
        val response = AssistantResponseV2(schemaVersion = 2)
        assertTrue(ProposalMapper.toPendingEntities(response, receivedAt) { newId() }.isEmpty())
    }
}
