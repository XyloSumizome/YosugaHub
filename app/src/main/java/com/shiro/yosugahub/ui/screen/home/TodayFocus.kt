package com.shiro.yosugahub.ui.screen.home

import com.shiro.yosugahub.domain.model.Task
import com.shiro.yosugahub.domain.model.TaskStatus

/**
 * 「今日やること」の導出(純粋関数)。旧「優先タスク」プレースホルダーを
 * 本物のタスクからの導出で置き換える(v3 ホーム再編)。
 *
 * 並び順: 作業中 → (期限切れ → 今日締切 → その他) → 優先度 → 締切近い順 → タイトル。
 * 完了タスクは含めない。
 */
fun todayFocus(tasks: List<Task>, today: String, limit: Int = 5): List<Task> =
    tasks
        .filter { it.status != TaskStatus.DONE }
        .sortedWith(
            compareBy(
                { statusRank(it.status) },
                { dueRank(it.dueDate, today) },
                { priorityRank(it.priority) },
                { it.dueDate ?: DUE_DATE_NONE },
                { it.title },
            )
        )
        .take(limit)

private fun statusRank(status: TaskStatus): Int = when (status) {
    TaskStatus.DOING -> 0
    else -> 1
}

private fun dueRank(dueDate: String?, today: String): Int = when {
    dueDate == null -> 2
    dueDate < today -> 0   // 期限切れ("yyyy-MM-dd" の辞書順比較)
    dueDate == today -> 1
    else -> 2
}

private fun priorityRank(priority: String): Int = when (priority) {
    "high" -> 0
    "medium" -> 1
    "low" -> 2
    else -> 3
}

private const val DUE_DATE_NONE = "9999-12-31"
