package com.shiro.yosugahub.ui.share

import com.shiro.yosugahub.data.repository.ImportResult
import com.shiro.yosugahub.data.repository.SyncResult

/**
 * 取り込み結果をユーザー向けの短いメッセージに変換する(設計書8章: 次に何をすればよいか)。
 *
 * **画面名は実際のUIに合わせること**(2026-07-25)。
 * v5 で下部ナビを廃してコンソール化したとき、「ヨスガ画面」「記録タブ」という
 * 存在しない画面名が案内に残っていた。次に何をすればよいかを伝える文言なので、
 * 指す先が無いと案内として成立しない。いまの名前は REVIEW / RECORDS / PROJECTS。
 */
fun importResultMessage(result: ImportResult): String = when (result) {
    is ImportResult.Success ->
        "取り込みました(提案 ${result.recommendationCount} 件)" + syncSuffix(result.sync)
    is ImportResult.SuccessProposals ->
        listOfNotNull(
            "提案を ${result.proposalCount} 件受け取りました。コンソールの REVIEW で承認してください。"
                .takeIf { result.proposalCount > 0 },
            "文書 ${result.classificationCount} 件の分類を受け取りました。RECORDS の「文書」で確認してください。"
                .takeIf { result.classificationCount > 0 },
            ("適用できなかった分類が ${result.skippedClassificationCount} 件ありました" +
                "(宛先の文書が見つからない、または確定済み・アーカイブ済み)。" +
                "やり直すには RECORDS の「文書」で「再分類」を押してください。")
                .takeIf { result.skippedClassificationCount > 0 },
            // セッション記録は承認を挟まず Obsidian へ入るので、REVIEW へは案内しない。
            "セッション記録を ${result.sessionCount} 件 Obsidian へ保存しました。"
                .takeIf { result.sessionCount > 0 },
            ("セッション記録を保存できませんでした: " +
                result.sessionFailures.distinct().joinToString(" / "))
                .takeIf { result.sessionFailures.isNotEmpty() },
        ).ifEmpty { listOf("受け取れる提案がありませんでした。") }
            .joinToString("\n") + syncSuffix(result.sync)
    is ImportResult.InvalidJson ->
        "JSONを読み取れませんでした。貼り付けた内容(またはファイル)を確認してください。"
    is ImportResult.UnsupportedSchema ->
        "未対応のバージョンです(schemaVersion: ${result.version})。"
    ImportResult.ReadError ->
        "ファイルを開けませんでした。もう一度選び直してください。"
}

/** 自動同期の一行は GitHub取得と共通(ui/share/SyncMessage.kt)。 */
private fun syncSuffix(sync: SyncResult?): String = autoSyncSuffix(sync)
