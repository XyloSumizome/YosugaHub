package com.shiro.yosugahub.domain.model

/** タスクの状態(v3-Step 1 設計: 3状態の最小構成)。 */
enum class TaskStatus(val dbValue: String) {
    TODO("todo"),
    DOING("doing"),
    DONE("done");

    companion object {
        /** DB文字列から変換。未知の値は TODO へフォールバックしクラッシュさせない。 */
        fun fromDb(value: String): TaskStatus =
            entries.firstOrNull { it.dbValue == value } ?: TODO
    }
}

/**
 * 制作タスク(v3-Step 1 で第一級エンティティ化)。
 * projectId が null のタスクはプロジェクト外(展示会準備・事務など)。
 */
data class Task(
    val id: String,
    val projectId: String?,
    val title: String,
    val detail: String,
    val status: TaskStatus,
    val priority: String,      // high / medium / low(Recommendation と同語彙)
    val dueDate: String?,      // "yyyy-MM-dd"。締切なしは null
    val createdAt: String,     // ISO 8601
    val updatedAt: String,     // ISO 8601
    val completedAt: String?,  // DONE にした時刻(完了履歴の土台)。未完了は null
    val source: String,        // manual / assistant(v3-Step 2 で AI提案由来を記録)
)
