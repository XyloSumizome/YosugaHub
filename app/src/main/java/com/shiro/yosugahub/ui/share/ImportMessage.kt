package com.shiro.yosugahub.ui.share

import com.shiro.yosugahub.data.repository.ImportResult

/** 取り込み結果をユーザー向けの短いメッセージに変換する(設計書8章: 次に何をすればよいか)。 */
fun importResultMessage(result: ImportResult): String = when (result) {
    is ImportResult.Success ->
        "取り込みました(提案 ${result.recommendationCount} 件)"
    is ImportResult.InvalidJson ->
        "JSONを読み取れませんでした。ファイルの内容を確認してください。"
    is ImportResult.UnsupportedSchema ->
        "未対応のバージョンです(schemaVersion: ${result.version})。"
    ImportResult.ReadError ->
        "ファイルを開けませんでした。もう一度選び直してください。"
}
