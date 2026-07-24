package com.shiro.yosugahub.data.obsidian

/**
 * 一覧の絞り込み条件(設計書v5 Phase 2)。
 *
 * **ここに置けるのはファイルを開かずに判定できる条件だけ。**
 * タグ絞り込みは全ノートの Frontmatter を読む必要があるため、
 * インデックス作成を伴う別の仕組みとして後から足す。
 */
data class NoteFilter(
    /** パス・ファイル名の部分一致(大文字小文字を区別しない)。 */
    val query: String = "",
    /** 直近 N 日以内に更新されたものだけ。null なら期間で絞らない。 */
    val recentDays: Int? = null,
) {
    val isActive: Boolean get() = query.isNotBlank() || recentDays != null
}

/** [NoteFilter] を適用する純粋ロジック。 */
object VaultNoteFilters {

    /** 画面に出す「最近更新」の選択肢(日数)。 */
    val RECENT_DAY_OPTIONS = listOf(1, 7, 30)

    private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

    /**
     * [notes] を [filter] で絞り込む。並び順は元のまま保つ。
     *
     * 更新時刻が取れなかったノート(`lastModified == 0`)は、期間で絞っているときは
     * **除外する**。「いつ更新されたか分からないもの」を「最近更新された」に混ぜないため。
     */
    fun apply(notes: List<VaultNote>, filter: NoteFilter, nowMillis: Long): List<VaultNote> {
        if (!filter.isActive) return notes
        val query = filter.query.trim().lowercase()
        val threshold = filter.recentDays?.let { nowMillis - it * MILLIS_PER_DAY }

        return notes.filter { note ->
            val matchesQuery = query.isEmpty() || note.relativePath.lowercase().contains(query)
            val matchesRecent = threshold == null ||
                (note.lastModified > 0L && note.lastModified >= threshold)
            matchesQuery && matchesRecent
        }
    }
}
