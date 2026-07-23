package com.shiro.yosugahub.ui.share

import com.shiro.yosugahub.data.repository.StatusFetchResult

/** 取得結果をユーザー向けの短文へ(設計書8章: 次に何をすればよいかを示す)。 */
fun statusFetchMessage(result: StatusFetchResult): String = when (result) {
    is StatusFetchResult.Success ->
        "進捗を取得しました"
    is StatusFetchResult.NotConfigured ->
        "リポジトリが未設定です。プロジェクトを編集して owner とリポジトリ名を入力してください。"
    is StatusFetchResult.TokenMissing ->
        "GitHubトークンが未設定です。設定画面で登録してください。"
    is StatusFetchResult.AuthFailed ->
        "認証に失敗しました。トークンの有効期限と権限(Contents: Read-only)を確認してください。"
    is StatusFetchResult.FileNotFound ->
        ".yosuga/status.json が見つかりません。リポジトリ名とファイルの有無を確認してください。"
    is StatusFetchResult.NetworkError ->
        "通信できませんでした。接続を確認してもう一度お試しください。"
    is StatusFetchResult.HttpError ->
        "取得に失敗しました(HTTP ${result.statusCode})。"
    is StatusFetchResult.InvalidJson ->
        "status.json を読み取れませんでした。ゲーム側で内容を確認してください。"
    is StatusFetchResult.UnsupportedSchema ->
        "未対応のバージョンです(schemaVersion: ${result.version})。"
    is StatusFetchResult.ProjectIdMismatch ->
        "projectId が一致しません(取得: ${result.actual})。リポジトリの指定を確認してください。"
}

/** 複数プロジェクトの一括更新結果をまとめた短文にする。 */
fun statusRefreshSummary(results: List<StatusFetchResult>): String {
    if (results.isEmpty()) return "更新対象のリポジトリがありません"
    val success = results.count { it is StatusFetchResult.Success }
    val failed = results.size - success
    return if (failed == 0) {
        "$success 件の進捗を取得しました"
    } else {
        "$success 件成功 / $failed 件失敗しました"
    }
}
