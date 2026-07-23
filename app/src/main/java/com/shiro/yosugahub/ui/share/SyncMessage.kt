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
}
