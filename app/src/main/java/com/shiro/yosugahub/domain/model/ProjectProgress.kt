package com.shiro.yosugahub.domain.model

/**
 * プロジェクトの「作業中 / 次」をタスクから導出する(v3-Step 1 からの積み残し・案C)。
 *
 * v3 の設計時点で「タスクからの導出へ置き換える予定」として編集不可にしてあった項目。
 * それまでの暫定として文字列が保存されていたが、
 * **タスクを唯一の情報源にする**ことで手入力と実態のズレが起きなくなる。
 *
 * 並び順は `groupTasks` と同じ規約(優先度 → 締切 → タイトル)にそろえる。
 * 画面に出ている一番上のタスクと「次」が食い違わないようにするため。
 */
object ProjectProgress {

    /** 「作業中」に並べる最大件数。多いと一覧が読めなくなる。 */
    private const val MAX_IN_PROGRESS = 3
    private const val SEPARATOR = " / "

    /** "yyyy-MM-dd" の辞書順比較で締切なしを最後に回すための番兵値。 */
    private const val DUE_DATE_NONE = "9999-12-31"

    /**
     * [tasks] は**そのプロジェクトのタスクだけ**を渡すこと。
     *
     * タスクが1件も無いプロジェクトは保存済みの文字列をそのまま残す。
     * まだタスク管理を始めていないプロジェクトの表示を、導出で空にしてしまわないため。
     */
    fun derive(project: Project, tasks: List<Task>): Project {
        if (tasks.isEmpty()) return project

        val sorted = tasks.sortedWith(
            compareBy(
                { priorityRank(it.priority) },
                { it.dueDate ?: DUE_DATE_NONE },
                { it.title },
            )
        )

        val doing = sorted.filter { it.status == TaskStatus.DOING }
            .take(MAX_IN_PROGRESS)
            .joinToString(SEPARATOR) { it.title }

        // 「次」は未着手の先頭。着手中が無いときも同じ基準で選ぶ。
        val next = sorted.firstOrNull { it.status == TaskStatus.TODO }?.title.orEmpty()

        return project.copy(inProgress = doing, nextTask = next)
    }

    /** プロジェクト一覧へまとめて適用する。タスクは projectId で振り分ける。 */
    fun deriveAll(projects: List<Project>, tasks: List<Task>): List<Project> {
        val byProject = tasks.groupBy { it.projectId }
        return projects.map { project -> derive(project, byProject[project.id].orEmpty()) }
    }

    private fun priorityRank(priority: String): Int = when (priority) {
        "high" -> 0
        "medium" -> 1
        "low" -> 2
        else -> 3
    }
}
