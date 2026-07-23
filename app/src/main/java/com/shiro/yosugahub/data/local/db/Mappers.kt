package com.shiro.yosugahub.data.local.db

import com.shiro.yosugahub.data.local.db.entity.CalendarEventEntity
import com.shiro.yosugahub.data.local.db.entity.ProjectEntity
import com.shiro.yosugahub.data.local.db.entity.RecommendationEntity
import com.shiro.yosugahub.data.local.db.entity.TaskEntity
import com.shiro.yosugahub.domain.model.CalendarEvent
import com.shiro.yosugahub.domain.model.Project
import com.shiro.yosugahub.domain.model.Recommendation
import com.shiro.yosugahub.domain.model.Task
import com.shiro.yosugahub.domain.model.TaskStatus

/** Room エンティティ → ドメインモデルの変換。UI へは常にドメインモデルを渡す。 */

fun ProjectEntity.toDomain(): Project = Project(
    id = id,
    name = name,
    currentGoal = currentGoal,
    inProgress = inProgress,
    nextTask = nextTask,
    lastUpdated = lastUpdated,
    health = health,
)

fun CalendarEventEntity.toDomain(): CalendarEvent = CalendarEvent(
    title = title,
    start = start,
    end = end,
    calendarName = calendarName,
    description = description,
)

fun RecommendationEntity.toDomain(): Recommendation = Recommendation(
    projectId = projectId,
    title = title,
    detail = detail,
    priority = priority,
)

fun TaskEntity.toDomain(): Task = Task(
    id = id,
    projectId = projectId,
    title = title,
    detail = detail,
    status = TaskStatus.fromDb(status),
    priority = priority,
    dueDate = dueDate,
    createdAt = createdAt,
    updatedAt = updatedAt,
    completedAt = completedAt,
    source = source,
)

/** タスクは編集可能なため、書き込み用の逆変換も持つ。 */
fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    projectId = projectId,
    title = title,
    detail = detail,
    status = status.dbValue,
    priority = priority,
    dueDate = dueDate,
    createdAt = createdAt,
    updatedAt = updatedAt,
    completedAt = completedAt,
    source = source,
)
