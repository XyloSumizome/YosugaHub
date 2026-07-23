package com.shiro.yosugahub.ui.share

import com.shiro.yosugahub.data.repository.ImportResult
import com.shiro.yosugahub.data.repository.SyncResult

/** 取り込み結果をユーザー向けの短いメッセージに変換する(設計書8章: 次に何をすればよいか)。 */
fun importResultMessage(result: ImportResult): String = when (result) {
    is ImportResult.Success ->
        "取り込みました(提案 ${result.recommendationCount} 件)" + syncSuffix(result.sync)
    is ImportResult.SuccessProposals ->
        listOfNotNull(
            "提案を ${result.proposalCount} 件受け取りました。ヨスガ画面で承認してください。"
                .takeIf { result.proposalCount > 0 },
            "文書 ${result.classificationCount} 件の分類を受け取りました。記録タブの「文書」で確認してください。"
                .takeIf { result.classificationCount > 0 },
            ("適用できなかった分類が ${result.skippedClassificationCount} 件ありました" +
                "(宛先の文書が見つからない、または確定済み・アーカイブ済み)。" +
                "やり直すには文書画面で「再分類」を押してください。")
                .takeIf { result.skippedClassificationCount > 0 },
        ).ifEmpty { listOf("受け取れる提案がありませんでした。") }
            .joinToString("\n") + syncSuffix(result.sync)
    is ImportResult.InvalidJson ->
        "JSONを読み取れませんでした。貼り付けた内容(またはファイル)を確認してください。"
    is ImportResult.UnsupportedSchema ->
        "未対応のバージョンです(schemaVersion: ${result.version})。"
    ImportResult.ReadError ->
        "ファイルを開けませんでした。もう一度選び直してください。"
}

/**
 * 取り込み後の自動同期についての一行(先頭に改行を含む)。
 * サーバー同期を使っていない場合は何も言わない(未設定は失敗ではない)。
 */
private fun syncSuffix(sync: SyncResult?): String = when (sync) {
    null, SyncResult.UrlNotConfigured, SyncResult.TokenMissing -> ""
    is SyncResult.Success -> "\nサーバーへ反映しました。"
    // 取り込み自体は成立しているので、失敗しても「やり直せる」ことを伝える。
    else -> "\nサーバーへの反映に失敗しました(${syncResultMessage(sync)})。" +
        "設定の「今すぐ同期」でやり直せます。"
}
