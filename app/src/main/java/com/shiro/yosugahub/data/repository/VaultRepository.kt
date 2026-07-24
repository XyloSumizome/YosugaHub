package com.shiro.yosugahub.data.repository

import com.shiro.yosugahub.data.obsidian.ContextData
import com.shiro.yosugahub.data.obsidian.ContextFileNames
import com.shiro.yosugahub.data.obsidian.ContextFormat
import com.shiro.yosugahub.data.obsidian.ContextJson
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

/**
 * コンテキスト生成の結果。プレビューと文字数表示に使う。
 *
 * [data] は形式に依存しない中間表現なので、[ContextFormat] を変えても
 * **ファイルを読み直す必要がない**。
 */
data class ContextBuildResult(
    val data: ContextData,
    val format: ContextFormat,
    val content: String,
) {
    val fileName: String get() = ContextFileNames.of(data.date, format)
    val noteCount: Int get() = data.notes.size

    /** 本文を読めなかったノートの相対パス。0 件でなければ画面で知らせる。 */
    val skipped: List<String> get() = data.skipped

    val charCount: Int get() = content.length
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
        format: ContextFormat = ContextFormat.MARKDOWN,
        now: OffsetDateTime = OffsetDateTime.now(zoneId),
    ): ContextBuildResult = format(loadContext(selected, now), format)

    /**
     * 選択されたノートの本文を読み、**形式に依存しない**中間表現を作る。
     * ここまでがファイル読み取りを伴う処理で、以降([format])は純粋。
     */
    suspend fun loadContext(
        selected: List<VaultNote>,
        now: OffsetDateTime = OffsetDateTime.now(zoneId),
    ): ContextData {
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

        return ContextData(
            notes = transformed,
            vaultName = reader.vaultName(),
            generatedAt = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            date = now.toLocalDate().toString(),
            scope = ContextScope.of(transformed),
            skipped = skipped,
        )
    }

    /** 中間表現を指定の形式へ整形する。**読み直しは発生しない**ので形式の切り替えは即座。 */
    fun format(data: ContextData, format: ContextFormat): ContextBuildResult {
        val content = when (format) {
            ContextFormat.MARKDOWN -> ContextMarkdown.build(
                notes = data.notes,
                vaultName = data.vaultName,
                generatedAt = data.generatedAt,
                scope = data.scope,
            )

            ContextFormat.JSON -> ContextJson.build(
                notes = data.notes,
                vaultName = data.vaultName,
                generatedAt = data.generatedAt,
                scope = data.scope,
            )
        }
        return ContextBuildResult(data = data, format = format, content = content)
    }

    /** ファイル更新時刻を Frontmatter と同じ ISO 表記にする。取得できない場合は空文字。 */
    private fun formatFileTime(epochMillis: Long): String {
        if (epochMillis <= 0L) return ""
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), zoneId)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }
}
