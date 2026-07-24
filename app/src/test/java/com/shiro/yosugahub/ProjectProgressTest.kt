package com.shiro.yosugahub

import com.shiro.yosugahub.domain.model.Project
import com.shiro.yosugahub.domain.model.ProjectProgress
import com.shiro.yosugahub.domain.model.Task
import com.shiro.yosugahub.domain.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectProgressTest {

    private val project = Project(
        id = "anri",
        name = "ANRI",
        currentGoal = "プロトタイプ",
        inProgress = "保存されていた作業中",
        nextTask = "保存されていた次",
        lastUpdated = "2026-07-24 10:00",
        health = "on_track",
    )

    private fun task(
        title: String,
        status: TaskStatus = TaskStatus.TODO,
        priority: String = "medium",
        dueDate: String? = null,
        projectId: String? = "anri",
    ) = Task(
        id = title,
        projectId = projectId,
        title = title,
        detail = "",
        status = status,
        priority = priority,
        dueDate = dueDate,
        createdAt = "2026-07-24T09:00:00+09:00",
        updatedAt = "2026-07-24T09:00:00+09:00",
        completedAt = null,
        source = "manual",
    )

    @Test
    fun in_progress_comes_from_doing_tasks() {
        val derived = ProjectProgress.derive(
            project,
            listOf(task("実装A", TaskStatus.DOING), task("後で")),
        )

        assertEquals("実装A", derived.inProgress)
    }

    @Test
    fun next_task_is_the_first_todo_by_priority_then_due_date() {
        val derived = ProjectProgress.derive(
            project,
            listOf(
                task("低優先", priority = "low"),
                task("高優先", priority = "high"),
                task("中優先", priority = "medium"),
            ),
        )

        assertEquals("高優先", derived.nextTask)
    }

    @Test
    fun same_priority_is_broken_by_the_nearer_due_date() {
        val derived = ProjectProgress.derive(
            project,
            listOf(
                task("あとの締切", priority = "high", dueDate = "2026-08-01"),
                task("近い締切", priority = "high", dueDate = "2026-07-25"),
                task("締切なし", priority = "high"),
            ),
        )

        assertEquals("近い締切", derived.nextTask)
    }

    @Test
    fun done_tasks_are_never_chosen() {
        val derived = ProjectProgress.derive(
            project,
            listOf(task("完了済み", TaskStatus.DONE, priority = "high")),
        )

        assertEquals("", derived.inProgress)
        assertEquals("", derived.nextTask)
    }

    @Test
    fun several_doing_tasks_are_joined_and_capped() {
        val derived = ProjectProgress.derive(
            project,
            (1..5).map { task("実装$it", TaskStatus.DOING, priority = "high") },
        )

        // 多すぎると一覧が読めなくなるので上限がある
        assertEquals(3, derived.inProgress.split(" / ").size)
    }

    @Test
    fun a_project_without_tasks_keeps_its_stored_text() {
        // まだタスク管理を始めていないプロジェクトの表示を、導出で空にしない
        val derived = ProjectProgress.derive(project, emptyList())

        assertEquals("保存されていた作業中", derived.inProgress)
        assertEquals("保存されていた次", derived.nextTask)
    }

    @Test
    fun other_fields_are_untouched() {
        val derived = ProjectProgress.derive(project, listOf(task("x")))

        assertEquals(project.name, derived.name)
        assertEquals(project.currentGoal, derived.currentGoal)
        assertEquals(project.health, derived.health)
        assertEquals(project.lastUpdated, derived.lastUpdated)
    }

    @Test
    fun derive_all_routes_tasks_by_project() {
        val frog = project.copy(id = "frog", name = "カエル", inProgress = "旧", nextTask = "旧")
        val derived = ProjectProgress.deriveAll(
            projects = listOf(project, frog),
            tasks = listOf(
                task("ANRIの作業", TaskStatus.DOING),
                task("カエルの作業", TaskStatus.DOING, projectId = "frog"),
            ),
        )

        assertEquals("ANRIの作業", derived[0].inProgress)
        assertEquals("カエルの作業", derived[1].inProgress)
    }

    @Test
    fun tasks_without_a_project_do_not_leak_into_any_project() {
        val derived = ProjectProgress.deriveAll(
            projects = listOf(project),
            tasks = listOf(task("プロジェクト外", TaskStatus.DOING, projectId = null)),
        )

        // projectId が null のタスクはどのプロジェクトにも属さない
        assertEquals("保存されていた作業中", derived.single().inProgress)
    }
}
