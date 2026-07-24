package com.shiro.yosugahub.data.obsidian

/** ノートの保存先。Vault ルートからの相対で表す。 */
data class NoteDestination(
    val directory: String,
    val fileName: String,
    /** Frontmatter の type(記録用)。判定できなければ空文字。 */
    val noteType: String,
    /** 振り分けに失敗して Inbox 行きになったか。 */
    val isInbox: Boolean,
    /** Inbox 行きの理由(画面で説明するため)。通常は空文字。 */
    val reason: String = "",
) {
    val path: String get() = "$directory/$fileName"
}

/**
 * 知識ノートの保存先を決める純粋ロジック(設計書v5 §6 / Phase 3-b)。
 *
 * **中身は読まない。**Frontmatter の `type` と `project_id` だけで機械的に決める
 * (v5 §10「Hub は複雑な判断を持たない」)。
 * 決められないものは捨てずに `Inbox/` へ入れ、人が後で仕分ける。
 */
object NoteRouter {

    const val INBOX = "Inbox"
    private const val GAMES = "Games"

    /** `type` → ゲームフォルダ配下のサブフォルダ(設計書v5 §6)。 */
    private val SUBFOLDER_BY_TYPE = mapOf(
        "design" to "Design",
        "development-log" to "Development Logs",
        "decision" to "Decisions",
        "reference" to "Overview",
    )

    /**
     * @param parsed ノートの Frontmatter 解析結果
     * @param sourceFileName リポジトリ上のファイル名(そのまま使う)
     * @param repoProjectId **どのゲームのリポジトリから取ってきたか**。
     *   Frontmatter に `project_id` が無いときの拠り所になる
     * @param gameFolders projectId → ゲームフォルダ名(Hub が持つプロジェクト一覧から作る)
     */
    fun route(
        parsed: ParsedNote,
        sourceFileName: String,
        repoProjectId: String,
        gameFolders: Map<String, String>,
    ): NoteDestination {
        val fileName = sanitizeFileName(sourceFileName)
        val type = parsed.type?.trim().orEmpty()
        val declaredProjectId = parsed.first("project_id")?.trim().orEmpty()

        // Frontmatter の宣言と取得元リポジトリが食い違う = 取り違えの可能性。人に見せる。
        if (declaredProjectId.isNotEmpty() && declaredProjectId != repoProjectId) {
            return inbox(fileName, type, "project_id がリポジトリと一致しません($declaredProjectId)")
        }

        val projectId = declaredProjectId.ifEmpty { repoProjectId }
        val gameFolder = gameFolders[projectId]
            ?: return inbox(fileName, type, "未知のプロジェクト($projectId)")

        val subFolder = SUBFOLDER_BY_TYPE[type]
            ?: return inbox(
                fileName,
                type,
                if (type.isEmpty()) "type がありません" else "未知の type($type)",
            )

        return NoteDestination(
            directory = "$GAMES/${sanitizeSegment(gameFolder)}/$subFolder",
            fileName = fileName,
            noteType = type,
            isInbox = false,
        )
    }

    private fun inbox(fileName: String, type: String, reason: String) = NoteDestination(
        directory = INBOX,
        fileName = fileName,
        noteType = type,
        isInbox = true,
        reason = reason,
    )

    /**
     * ファイル名からパス区切りと危険な文字を落とす。
     * Frontmatter や リポジトリ側の名前をそのまま信用してディレクトリ外へ書かせない。
     */
    fun sanitizeFileName(name: String): String {
        val base = name.trim().substringAfterLast('/').substringAfterLast('\\')
        val cleaned = sanitizeSegment(base)
        if (cleaned.isEmpty()) return FALLBACK_NAME
        return if (cleaned.endsWith(VaultNote.EXTENSION, ignoreCase = true)) {
            cleaned
        } else {
            cleaned + VaultNote.EXTENSION
        }
    }

    /** フォルダ名・ファイル名の1区画を安全にする。 */
    private fun sanitizeSegment(segment: String): String =
        segment.replace(Regex("""[\\/:*?"<>|]"""), "_")
            .replace("..", "_")
            .trim()
            .trimStart('.')

    private const val FALLBACK_NAME = "untitled.md"
}
