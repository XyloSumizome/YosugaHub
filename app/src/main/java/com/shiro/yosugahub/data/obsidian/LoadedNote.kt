package com.shiro.yosugahub.data.obsidian

/**
 * 本文まで読み込んだノート(設計書v5 §10「抽出と要約の境界」)。
 *
 * SAF にも Room にも依存しない**素のデータ**。
 * `VaultReader` の出力であり `ContextMarkdown` の入力であり、
 * 将来の要約(`NoteTransformer`)が差し込まれる唯一の型でもある。
 */
data class LoadedNote(
    /** Vault ルートからの相対パス。出力の `Source:` になる。 */
    val relativePath: String,
    /** 見出しに使うタイトル。 */
    val title: String,
    /** ゲーム名(Frontmatter の `game`)。無ければ null。 */
    val game: String?,
    /** 表示用の更新日時。Frontmatter を優先し、無ければファイルの更新時刻。 */
    val updatedAt: String,
    val tags: List<String>,
    val body: String,
) {
    /** 出力の見出し。ゲーム名があれば `ANRI / Lighting Design` の形にする。 */
    val heading: String get() = if (game.isNullOrBlank()) title else "$game / $title"

    companion object {
        /**
         * メタ情報と解析結果からコンテキスト用のノートを組み立てる。
         * `fallbackUpdatedAt` はファイル更新時刻を整形した文字列
         * (整形はタイムゾーンを持つ呼び出し側の責務。ここは純粋に保つ)。
         */
        fun from(
            note: VaultNote,
            parsed: ParsedNote,
            fallbackUpdatedAt: String,
        ): LoadedNote = LoadedNote(
            relativePath = note.relativePath,
            title = parsed.title?.takeIf { it.isNotBlank() } ?: note.title,
            game = parsed.game?.takeIf { it.isNotBlank() },
            updatedAt = parsed.updatedAt?.takeIf { it.isNotBlank() } ?: fallbackUpdatedAt,
            tags = parsed.tags,
            body = parsed.body,
        )
    }
}

/**
 * 本文へ手を入れる差し替え点。**Phase 1 は恒等変換**(何もしない)。
 * Phase 4 で AI 要約を入れる場合も、変更箇所はここの実装 1 つで済む。
 */
fun interface NoteTransformer {

    suspend fun transform(notes: List<LoadedNote>): List<LoadedNote>

    companion object {
        /** 原文をそのまま通す既定の実装(設計書v5 §10「AI 要約を前提にしない」)。 */
        val Identity: NoteTransformer = NoteTransformer { it }
    }
}
