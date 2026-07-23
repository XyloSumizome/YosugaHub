package com.shiro.yosugahub.ui.screen.projectdetail

import com.shiro.yosugahub.domain.model.Task
import com.shiro.yosugahub.domain.model.TaskStatus

/** プロジェクト詳細画面のタスク表示区分(進行中 → 未着手 → 完了の順に並べる)。 */
data class GroupedTasks(
    val doing: List<Task> = emptyList(),
    val todo: List<Task> = emptyList(),
    val done: List<Task> = emptyList(),
) {
    val isEmpty: Boolean get() = doing.isEmpty() && todo.isEmpty() && done.isEmpty()
}

/**
 * タスクを状態別に分け、各グループ内は 優先度(high→low)→ 締切(近い順、なしは最後)→
 * タイトル順に並べる。純粋関数としてユニットテスト可能にしている。
 */
fun groupTasks(tasks: List<Task>): GroupedTasks {
    val sorted = tasks.sortedWith(
        compareBy(
            { priorityRank(it.priority) },
            { it.dueDate ?: DUE_DATE_NONE },
            { it.title },
        )
    )
    return GroupedTasks(
        doing = sorted.filter { it.status == TaskStatus.DOING },
        todo = sorted.filter { it.status == TaskStatus.TODO },
        done = sorted.filter { it.status == TaskStatus.DONE },
    )
}

private fun priorityRank(priority: String): Int = when (priority) {
    "high" -> 0
    "medium" -> 1
    "low" -> 2
    else -> 3
}

/** "yyyy-MM-dd" の辞書順比較で締切なしを最後に回すための番兵値。 */
private const val DUE_DATE_NONE = "9999-12-31"
