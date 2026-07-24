package com.shiro.yosugahub.data.repository

import com.shiro.yosugahub.data.obsidian.ConversationNoteBuilder
import com.shiro.yosugahub.data.obsidian.VaultWriteResult
import com.shiro.yosugahub.data.obsidian.VaultWriter
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/** 会話ログ保存の結果。 */
sealed interface ConversationImportResult {
    data class Saved(val path: String) : ConversationImportResult

    /** 貼り付けが空。 */
    data object Empty : ConversationImportResult

    data object VaultNotConfigured : ConversationImportResult
    data class Failed(val reason: String) : ConversationImportResult
}

/**
 * ヨスガとの会話まとめを Obsidian Vault へ保存する(設計書v5 §7 / Phase 3-d)。
 *
 * v5 §7 のとおり **独立したモジュール**にしてある。
 * 将来 ChatGPT の API 連携などへ差し替えるとき、ここだけを置き換えればよい。
 * 保存先は `Conversations/Yosuga/`。既存ノートは上書きしない([VaultWriter] の責務)。
 */
class ConversationImportRepository(
    private val vaultWriter: VaultWriter,
    private val now: () -> OffsetDateTime = { OffsetDateTime.now() },
) {

    suspend fun save(body: String): ConversationImportResult {
        if (body.isBlank()) return ConversationImportResult.Empty

        val timestamp = now()
        val note = ConversationNoteBuilder.build(
            body = body,
            date = timestamp.toLocalDate().toString(),
            generatedAt = timestamp.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
        )

        return when (val written = vaultWriter.write(note.directory, note.fileName, note.content)) {
            is VaultWriteResult.Written -> ConversationImportResult.Saved(written.path)
            VaultWriteResult.NotConfigured -> ConversationImportResult.VaultNotConfigured
            is VaultWriteResult.Failed -> ConversationImportResult.Failed(written.reason)
        }
    }
}
