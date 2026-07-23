package com.shiro.yosugahub

import com.shiro.yosugahub.domain.model.PendingProposal
import com.shiro.yosugahub.domain.model.ProposalStatus
import com.shiro.yosugahub.domain.model.ProposalType
import com.shiro.yosugahub.ui.screen.assistant.toCardUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProposalCardUiTest {

    private fun proposal(type: ProposalType, payload: String) = PendingProposal(
        id = "prop-1",
        type = type,
        payloadJson = payload,
        status = ProposalStatus.PENDING,
        receivedAt = "2026-07-23T16:30:00+09:00",
    )

    @Test
    fun task_card_shows_priority_and_due_date() {
        val card = proposal(
            ProposalType.TASK,
            """{"title":"戦闘調整","detail":"係数見直し","priority":"high","dueDate":"2026-07-30","projectId":"anri"}""",
        ).toCardUi()
        assertEquals("タスク", card.typeLabel)
        assertEquals("戦闘調整", card.title)
        assertTrue(card.readable)
        assertTrue(card.body.contains("優先度: 高"))
        assertTrue(card.body.contains("締切: 2026-07-30"))
        assertTrue(card.body.contains("プロジェクト: anri"))
    }

    @Test
    fun item_card_uses_kind_label_and_hash_tags() {
        val card = proposal(
            ProposalType.ITEM,
            """{"kind":"decision","title":"ビート表示を採用","body":"理由","tags":["Yosuga Hub","UI"]}""",
        ).toCardUi()
        assertEquals("決定事項", card.typeLabel)
        assertTrue(card.body.contains("#Yosuga Hub #UI"))
    }

    @Test
    fun diary_card_uses_received_date_when_blank() {
        val card = proposal(ProposalType.DIARY, """{"body":"今日は..."}""").toCardUi()
        assertEquals("観察日記", card.typeLabel)
        assertEquals("2026-07-23", card.title)
    }

    @Test
    fun health_card_summarizes_change() {
        val card = proposal(
            ProposalType.HEALTH,
            """{"projectId":"gengenkyo","health":"停滞中","reason":"2週間更新なし"}""",
        ).toCardUi()
        assertEquals("状態更新", card.typeLabel)
        assertEquals("gengenkyo を「停滞中」へ", card.title)
        assertEquals("2週間更新なし", card.body)
    }

    @Test
    fun broken_payload_becomes_unreadable_card() {
        val card = proposal(ProposalType.TASK, "{ broken ").toCardUi()
        assertFalse(card.readable)
        assertEquals("(読み取れない提案)", card.title)
    }
}
