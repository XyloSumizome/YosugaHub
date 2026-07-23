package com.shiro.yosugahub

import com.shiro.yosugahub.domain.model.Task
import com.shiro.yosugahub.domain.model.TaskStatus
import com.shiro.yosugahub.ui.screen.home.todayFocus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayFocusTest {

    private val today = "2026-07-23"

    private fun task(
        id: String,
        status: TaskStatus = TaskStatus.TODO,
        priority: String = "medium",
        dueDate: String? = null,
    ) = Task(
        id = id,
        projectId = null,
        title = id,
        detail = "",
        status = status,
        priority = priority,
        dueDate = dueDate,
        createdAt = "2026-07-23T09:00:00+09:00",
        updatedAt = "2026-07-23T09:00:00+09:00",
        completedAt = null,
        source = "manual",
    )

    @Test
    fun excludes_done_tasks() {
        val result = todayFocus(listOf(task("done", status = TaskStatus.DONE), task("todo")), today)
        assertEquals(listOf("todo"), result.map { it.id })
    }

    @Test
    fun doing_comes_before_todo_regardless_of_priority() {
        val result = todayFocus(
            listOf(
                task("todo-high", priority = "high"),
                task("doing-low", status = TaskStatus.DOING, priority = "low"),
            ),
            today,
        )
        assertEquals(listOf("doing-low", "todo-high"), result.map { it.id })
    }

    @Test
    fun overdue_then_due_today_then_priority() {
        val result = todayFocus(
            listOf(
                task("no-due-high", priority = "high"),
                task("due-today", priority = "low", dueDate = "2026-07-23"),
                task("overdue", priority = "low", dueDate = "2026-07-20"),
                task("future", priority = "low", dueDate = "2026-08-01"),
            ),
            today,
        )
        assertEquals(listOf("overdue", "due-today", "no-due-high", "future"), result.map { it.id })
    }

    @Test
    fun respects_limit() {
        val tasks = (1..10).map { task("t$it") }
        assertTrue(todayFocus(tasks, today).size <= 5)
        assertEquals(3, todayFocus(tasks, today, limit = 3).size)
    }
}
