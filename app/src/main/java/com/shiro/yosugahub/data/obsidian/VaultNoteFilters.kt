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
    /**
     * 選択されたタグ(**いずれか**を持つノートを残す = OR)。
     * AND にすると 2 つ選んだ時点でほぼ 0 件になり、選ぶ楽しさが無くなるため。
     * タグはファイルを開かないと分からないので、[TagIndex] を併せて渡す必要がある。
     */
    val tags: Set<String> = emptySet(),
) {
    val isActive: Boolean
        get() = query.isNotBlank() || recentDays != null || tags.isNotEmpty()
}

/**
 * 相対パス → タグ の対応表(設計書v5 Phase 2)。
 *
 * **作るには全ノートを開く必要がある**ため、一覧の列挙とは分けて明示的に作る。
 * 未作成のときは [EMPTY] を渡す(タグ絞り込みは効かないが他の条件は動く)。
 */
data class TagIndex(
    val tagsByPath: Map<String, List<String>> = emptyMap(),
    /** 読めなかったノートの相対パス。 */
    val skipped: List<String> = emptyList(),
) {
    val isBuilt: Boolean get() = tagsByPath.isNotEmpty()

    /** 出現数の多い順のタグ一覧(チップの並び順)。 */
    val allTags: List<String>
        get() = tagsByPath.values.flatten()
            .groupingBy { it }.eachCount()
            .entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }
                .thenBy { it.key })
            .map { it.key }

    fun tagsOf(path: String): List<String> = tagsByPath[path].orEmpty()

    companion object {
        val EMPTY = TagIndex()
    }
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
    fun apply(
        notes: List<VaultNote>,
        filter: NoteFilter,
        nowMillis: Long,
        tagIndex: TagIndex = TagIndex.EMPTY,
    ): List<VaultNote> {
        if (!filter.isActive) return notes
        val query = filter.query.trim().lowercase()
        val threshold = filter.recentDays?.let { nowMillis - it * MILLIS_PER_DAY }

        return notes.filter { note ->
            val matchesQuery = query.isEmpty() || note.relativePath.lowercase().contains(query)
            val matchesRecent = threshold == null ||
                (note.lastModified > 0L && note.lastModified >= threshold)
            // 選択タグのいずれかを持てば残す(OR)。索引が無いノートは対象外。
            val matchesTags = filter.tags.isEmpty() ||
                tagIndex.tagsOf(note.relativePath).any { it in filter.tags }
            matchesQuery && matchesRecent && matchesTags
        }
    }
}
