package com.shiro.yosugahub.data.repository

import com.shiro.yosugahub.data.file.model.SessionProposal
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
 * セッション記録の書き込み結果(2026-07-27)。
 * 取り込み全体を失敗にはしない——**書けたものは書けた**と伝えるための型。
 */
data class SessionSaveOutcome(
    val saved: Int = 0,
    /** 書けなかったものの理由。同じ理由が並ぶことがあるので、表示側で丸める。 */
    val failures: List<String> = emptyList(),
)

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

    /**
     * 回答JSON の `session[]` を Vault へ書く(2026-07-27)。
     *
     * [save] が「ヨスガが書いた Markdown をそのまま置く」のに対し、こちらは
     * **構造化された値を受け取って Hub が Frontmatter を組む**。
     * 札(date / games / category / tags)を Hub 側で作れるので、
     * ヨスガの YAML の書き癖に左右されない。
     *
     * @return 書けた件数と、書けなかった理由。**1件失敗しても残りは書く**
     *   ——1日分の記録を「一部が転んだから」で丸ごと失うほうが損。
     */
    suspend fun saveSessions(sessions: List<SessionProposal>): SessionSaveOutcome {
        val usable = sessions.filter { it.body.isNotBlank() }
        if (usable.isEmpty()) return SessionSaveOutcome()

        val timestamp = now()
        val today = timestamp.toLocalDate().toString()
        val generatedAt = timestamp.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        var saved = 0
        val failures = mutableListOf<String>()
        for (session in usable) {
            val note = ConversationNoteBuilder.buildSession(
                body = session.body,
                // 日付が空でも捨てない。取り込んだ日に寄せれば記録は残る。
                date = session.date.ifBlank { today },
                generatedAt = generatedAt,
                games = session.games,
                category = session.category,
                tags = session.tags,
            )
            when (val written = vaultWriter.write(note.directory, note.fileName, note.content)) {
                is VaultWriteResult.Written -> saved++
                VaultWriteResult.NotConfigured -> failures += "Vault が未設定です"
                is VaultWriteResult.Failed -> failures += written.reason
            }
        }
        return SessionSaveOutcome(saved = saved, failures = failures)
    }

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
