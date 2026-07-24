package com.shiro.yosugahub.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Vault へ取り込み済みの知識ノートの記録(v5 Phase 3-b)。
 *
 * 主キーは **Git のブロブSHA**。内容が変わらない限り不変なので、
 * 「同じノートを二度取り込まない」判定に使える。
 * v5 §10「すべての自動処理に元ファイルと処理日時を残す」ため、
 * 取得元パスと書き込み先パスの両方を持つ。
 */
@Entity(
    tableName = "imported_notes",
    indices = [Index("projectId"), Index("importedAt")],
)
data class ImportedNoteEntity(
    @PrimaryKey val sha: String,
    val projectId: String,
    /** リポジトリ内のパス(例: `.yosuga/notes/2026-07-24-lighting.md`)。 */
    val sourcePath: String,
    /** Vault ルートからの書き込み先(例: `Games/ANRI/Development Logs/2026-07-24-lighting.md`)。 */
    val vaultPath: String,
    /** Frontmatter の type。振り分け結果の確認用。 */
    val noteType: String,
    val importedAt: String,
)
