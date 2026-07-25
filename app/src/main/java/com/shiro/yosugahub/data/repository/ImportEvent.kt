package com.shiro.yosugahub.data.repository

/**
 * ノート取り込みの進捗イベント(v5 UI: ハッキング演出)。
 *
 * **実処理そのもの**を1件ずつ流すための型。演出は本物のログなので、
 * ここに出る値(ファイル名・振り分け先)は実際に処理したものと一致する。
 * UI 側はこれを端末風のログ行に整形して1行ずつ表示する。
 */
sealed interface ImportEvent {
    /** 開始。「github へ接続」相当の見出し。 */
    data object Connect : ImportEvent

    /** 1プロジェクトの処理に入る。 */
    data class Target(val projectName: String) : ImportEvent

    /** `.yosuga/notes/` を走査した。取得対象 + 取得済みの合計件数。 */
    data class Scan(val total: Int) : ImportEvent

    /** 取得対象なし・エラーなど、そのプロジェクトを飛ばす理由(正常も含む)。 */
    data class Note(val message: String) : ImportEvent

    /** 1件の本文を取得した。 */
    data class Fetch(val fileName: String) : ImportEvent

    /** 振り分け先が決まった。 */
    data class Route(val fileName: String, val destination: String, val isInbox: Boolean) : ImportEvent

    /** 既存ノートの新しい版が来た。記録済みの場所を上書きする(2026-07-25)。 */
    data class Update(val fileName: String, val vaultPath: String) : ImportEvent

    /** 記録にあるのにリポジトリから消えたノート。Vault 側は残す(報告だけ)。 */
    data class Missing(val vaultPath: String) : ImportEvent

    /** Vault へ書けた。 */
    data class Written(val path: String) : ImportEvent

    /** 取得済みで飛ばした件数。 */
    data class Skip(val count: Int) : ImportEvent

    /** 取得または書き込みに失敗した。 */
    data class Fail(val path: String) : ImportEvent

    /** 全体の完了。 */
    data class Done(
        val imported: Int,
        val toInbox: Int,
        val updated: Int,
        val skipped: Int,
        val failed: Int,
        val missing: Int,
    ) : ImportEvent
}
