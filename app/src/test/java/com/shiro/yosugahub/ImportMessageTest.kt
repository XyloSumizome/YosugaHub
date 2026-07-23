package com.shiro.yosugahub

import com.shiro.yosugahub.data.repository.ImportResult
import com.shiro.yosugahub.ui.share.importResultMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 取り込み結果メッセージ(v4.1 で分類結果の行を追加)。 */
class ImportMessageTest {

    @Test
    fun proposals_only_message() {
        val message = importResultMessage(
            ImportResult.SuccessProposals(proposalCount = 3, fileName = "r.json")
        )
        assertTrue(message.contains("提案を 3 件"))
        assertFalse(message.contains("文書"))
    }

    @Test
    fun classifications_are_reported_with_where_to_confirm() {
        val message = importResultMessage(
            ImportResult.SuccessProposals(
                proposalCount = 0, fileName = "r.json", classificationCount = 2,
            )
        )
        assertFalse(message.contains("提案を"))
        assertTrue(message.contains("文書 2 件の分類"))
        assertTrue(message.contains("記録タブ"))
    }

    @Test
    fun both_proposals_and_classifications_are_reported() {
        val message = importResultMessage(
            ImportResult.SuccessProposals(
                proposalCount = 1, fileName = "r.json",
                classificationCount = 2, skippedClassificationCount = 1,
            )
        )
        assertEquals(3, message.lines().size)
        assertTrue(message.contains("適用できなかった分類が 1 件"))
        // 次に何をすればよいかを書く(設計書8章)
        assertTrue(message.contains("再分類"))
    }

    @Test
    fun empty_response_says_nothing_was_received() {
        val message = importResultMessage(
            ImportResult.SuccessProposals(proposalCount = 0, fileName = "r.json")
        )
        assertEquals("受け取れる提案がありませんでした。", message)
    }
}
