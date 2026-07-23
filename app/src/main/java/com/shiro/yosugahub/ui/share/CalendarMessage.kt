package com.shiro.yosugahub.ui.share

import com.shiro.yosugahub.data.calendar.CalendarSyncResult

/** カレンダー同期結果をユーザー向けの短文へ(設計書8章)。 */
fun calendarSyncMessage(result: CalendarSyncResult): String = when (result) {
    is CalendarSyncResult.Success ->
        if (result.eventCount == 0) {
            "予定は見つかりませんでした(過去7日〜未来7日)"
        } else {
            "${result.eventCount} 件の予定を取得しました"
        }
    CalendarSyncResult.PermissionDenied ->
        "カレンダーの読み取りが許可されていません。端末の設定から許可してください。"
    is CalendarSyncResult.Failed ->
        "カレンダーを読み取れませんでした。もう一度お試しください。"
}
