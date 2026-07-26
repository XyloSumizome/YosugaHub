package com.shiro.yosugahub.ui.share

import com.shiro.yosugahub.data.repository.SyncResult

/** サーバー同期結果をユーザー向けの短文へ(設計書8章)。 */
fun syncResultMessage(result: SyncResult): String = when (result) {
    is SyncResult.Success ->
        "${result.fileCount} ファイルを同期しました"
    SyncResult.UrlNotConfigured ->
        "同期先URLが未設定です。サーバーのURLを入力してください。"
    SyncResult.TokenMissing ->
        "同期トークンが未設定です。config.php と同じトークンを保存してください。"
    SyncResult.Unauthorized ->
        "認証に失敗しました。トークンがサーバーの config.php と一致しているか確認してください。"
    is SyncResult.HttpError ->
        "同期に失敗しました(HTTP ${result.statusCode})。サーバーの設置を確認してください。"
    SyncResult.NetworkError ->
        "通信できませんでした。接続とURLを確認してもう一度お試しください。"
    SyncResult.InsecureUrl ->
        "同期先が http:// です。トークンが平文で流れるうえ Android がブロックします。" +
            "https:// に直してください。"
}

/**
 * 取り込み・GitHub取得のあとに走る**自動同期**についての一行(先頭に改行を含む)。
 * サーバー同期を使っていない場合は何も言わない(未設定は失敗ではない)。
 * 元の操作自体は成立しているので、失敗しても「やり直せる」ことだけ伝える。
 */
fun autoSyncSuffix(sync: SyncResult?): String = when (sync) {
    null, SyncResult.UrlNotConfigured, SyncResult.TokenMissing -> ""
    is SyncResult.Success -> "\nサーバーへ反映しました。"
    else -> "\nサーバーへの反映に失敗しました(${syncResultMessage(sync)})。" +
        "設定の「今すぐ同期」でやり直せます。"
}
