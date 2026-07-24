package com.shiro.yosugahub

import com.shiro.yosugahub.domain.model.Task
import com.shiro.yosugahub.domain.model.TaskStatus
import com.shiro.yosugahub.ui.screen.projectdetail.GroupedTasks
import com.shiro.yosugahub.ui.screen.projectdetail.groupTasks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskGroupingTest {

    @Test
    fun size_counts_every_bucket() {
        val grouped = GroupedTasks(
            doing = listOf(task("a", TaskStatus.DOING)),
            todo = listOf(task("b", TaskStatus.TODO), task("c", TaskStatus.TODO)),
            done = listOf(task("d", TaskStatus.DONE)),
        )

        assertEquals(4, grouped.size)
        assertEquals(0, GroupedTasks().size)
    }

    private fun task(
        id: String,
        status: TaskStatus = TaskStatus.TODO,
        priority: String = "medium",
        dueDate: String? = null,
        title: String = id,
    ) = Task(
        id = id,
        projectId = "anri",
        title = title,
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
    fun groups_tasks_by_status() {
        val grouped = groupTasks(
            listOf(
                task("a", status = TaskStatus.DONE),
                task("b", status = TaskStatus.TODO),
                task("c", status = TaskStatus.DOING),
            )
        )
        assertEquals(listOf("c"), grouped.doing.map { it.id })
        assertEquals(listOf("b"), grouped.todo.map { it.id })
        assertEquals(listOf("a"), grouped.done.map { it.id })
    }

    @Test
    fun sorts_by_priority_then_due_date_nulls_last_then_title() {
        val grouped = groupTasks(
            listOf(
                task("no-due-medium", priority = "medium", dueDate = null),
                task("late-medium", priority = "medium", dueDate = "2026-08-01"),
                task("early-medium", priority = "medium", dueDate = "2026-07-25"),
                task("high", priority = "high", dueDate = null),
                task("low", priority = "low", dueDate = "2026-07-24"),
            )
        )
        assertEquals(
            listOf("high", "early-medium", "late-medium", "no-due-medium", "low"),
            grouped.todo.map { it.id },
        )
    }

    @Test
    fun unknown_priority_sorts_last() {
        val grouped = groupTasks(
            listOf(
                task("mystery", priority = "urgent??"),
                task("low", priority = "low"),
            )
        )
        assertEquals(listOf("low", "mystery"), grouped.todo.map { it.id })
    }

    @Test
    fun isEmpty_reflects_all_groups() {
        assertTrue(groupTasks(emptyList()).isEmpty)
        assertTrue(!groupTasks(listOf(task("a"))).isEmpty)
    }
}
