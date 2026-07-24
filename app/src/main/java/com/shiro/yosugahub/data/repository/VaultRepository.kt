package com.shiro.yosugahub.data.repository

import com.shiro.yosugahub.data.obsidian.ContextMarkdown
import com.shiro.yosugahub.data.obsidian.ContextScope
import com.shiro.yosugahub.data.obsidian.Frontmatter
import com.shiro.yosugahub.data.obsidian.LoadedNote
import com.shiro.yosugahub.data.obsidian.NoteTransformer
import com.shiro.yosugahub.data.obsidian.VaultListing
import com.shiro.yosugahub.data.obsidian.VaultNote
import com.shiro.yosugahub.data.obsidian.VaultReader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** コンテキスト生成の結果。プレビューと文字数表示に使う。 */
data class ContextBuildResult(
    val fileName: String,
    val markdown: String,
    val noteCount: Int,
    /** 本文を読めなかったノートの相対パス。0 件でなければ画面で知らせる。 */
    val skipped: List<String>,
) {
    val charCount: Int get() = markdown.length
    val isLarge: Boolean get() = charCount > ContextMarkdown.WARN_CHAR_COUNT
}

/**
 * Obsidian Vault の読み取りとコンテキスト生成をまとめる Repository(設計書v5 Phase 1-a)。
 *
 * 一覧はメモリにキャッシュし、明示的な [refresh] でだけ更新する
 * (SAF 経由の列挙は遅くなりうるため、画面表示のたびに走らせない)。
 * Room には保存しない ＝ **スキーマ変更なし**。
 */
class VaultRepository(
    private val reader: VaultReader,
    /** 本文への加工。Phase 1 は恒等変換(要約しない)。 */
    private val transformer: NoteTransformer = NoteTransformer.Identity,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {

    private val _notes = MutableStateFlow<List<VaultNote>>(emptyList())

    /** 直近に列挙したノート一覧。未取得なら空。 */
    val notes: StateFlow<List<VaultNote>> = _notes.asStateFlow()

    /** Vault を再列挙する。成功時はキャッシュを差し替える。 */
    suspend fun refresh(): VaultListing {
        val listing = reader.listNotes()
        if (listing is VaultListing.Success) _notes.value = listing.notes
        return listing
    }

    /**
     * 選択されたノートの本文を読み、1 本のコンテキスト Markdown へ結合する。
     * 読めなかったノートは [ContextBuildResult.skipped] に入れ、**処理は止めない**。
     */
    suspend fun buildContext(
        selected: List<VaultNote>,
        now: OffsetDateTime = OffsetDateTime.now(zoneId),
    ): ContextBuildResult {
        val loaded = mutableListOf<LoadedNote>()
        val skipped = mutableListOf<String>()

        selected.forEach { note ->
            val raw = reader.readNote(note.documentUri)
            if (raw == null) {
                skipped += note.relativePath
                return@forEach
            }
            val parsed = Frontmatter.parse(raw)
            loaded += LoadedNote.from(note, parsed, fallbackUpdatedAt = formatFileTime(note.lastModified))
        }

        // 要約が必要になってもここを差し替えるだけで済む(設計書v5 §10)。
        val transformed = transformer.transform(loaded)

        val markdown = ContextMarkdown.build(
            notes = transformed,
            vaultName = reader.vaultName(),
            generatedAt = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            scope = ContextScope.of(transformed),
        )
        return ContextBuildResult(
            fileName = ContextMarkdown.fileName(now.toLocalDate().toString()),
            markdown = markdown,
            noteCount = transformed.size,
            skipped = skipped,
        )
    }

    /** ファイル更新時刻を Frontmatter と同じ ISO 表記にする。取得できない場合は空文字。 */
    private fun formatFileTime(epochMillis: Long): String {
        if (epochMillis <= 0L) return ""
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), zoneId)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }
}
