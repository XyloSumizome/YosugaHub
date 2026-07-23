package com.shiro.yosugahub

import com.shiro.yosugahub.data.repository.ImportResult
import com.shiro.yosugahub.data.repository.SyncResult
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

    // --- 取り込み後の自動同期(v4.1 運用) ---

    @Test
    fun successful_auto_sync_is_reported() {
        val message = importResultMessage(
            ImportResult.SuccessProposals(
                proposalCount = 1, fileName = "r.json", sync = SyncResult.Success(7),
            )
        )
        assertTrue(message.endsWith("サーバーへ反映しました。"))
    }

    /** サーバー同期を使っていない人に「失敗」と言わない(未設定は失敗ではない)。 */
    @Test
    fun unconfigured_sync_is_silent() {
        val notConfigured = importResultMessage(
            ImportResult.SuccessProposals(
                proposalCount = 1, fileName = "r.json", sync = SyncResult.UrlNotConfigured,
            )
        )
        val noToken = importResultMessage(
            ImportResult.SuccessProposals(
                proposalCount = 1, fileName = "r.json", sync = SyncResult.TokenMissing,
            )
        )
        assertFalse(notConfigured.contains("サーバー"))
        assertFalse(noToken.contains("サーバー"))
    }

    /** 同期に失敗しても取り込み自体は成立している。やり直せることを伝える。 */
    @Test
    fun failed_auto_sync_keeps_the_import_result_and_offers_a_retry() {
        val message = importResultMessage(
            ImportResult.SuccessProposals(
                proposalCount = 2, fileName = "r.json", sync = SyncResult.NetworkError,
            )
        )
        assertTrue(message.contains("提案を 2 件"))
        assertTrue(message.contains("サーバーへの反映に失敗しました"))
        assertTrue(message.contains("今すぐ同期"))
    }

    @Test
    fun v1_import_also_reports_auto_sync() {
        val message = importResultMessage(
            ImportResult.Success(
                recommendationCount = 3, fileName = "r.json", sync = SyncResult.Success(7),
            )
        )
        assertTrue(message.contains("提案 3 件"))
        assertTrue(message.endsWith("サーバーへ反映しました。"))
    }
}
