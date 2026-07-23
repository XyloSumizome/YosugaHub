package com.shiro.yosugahub.data.obsidian

/** ノート追記の結果。失敗しても Hub 側(Room)の保存は成立している前提で扱う。 */
enum class AppendOutcome {
    WRITTEN,
    NOT_CONFIGURED,
    FAILED,
}

/**
 * 長文知識の書き出し先の抽象化(v3-Step 3)。
 * 初期実装は Obsidian Vault(SAF)。将来 GitHub / Drive 等へ差し替え可能にする。
 */
interface KnowledgeStore {

    /** 指定ノート(例: "GameDesign.md")の末尾へ markdown を追記する。 */
    suspend fun appendToNote(noteName: String, markdown: String): AppendOutcome
}
