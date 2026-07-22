package com.shiro.yosugahub.data.local.db

import com.shiro.yosugahub.data.local.db.entity.CalendarEventEntity
import com.shiro.yosugahub.data.local.db.entity.ProjectEntity
import com.shiro.yosugahub.data.local.db.entity.RecommendationEntity
import com.shiro.yosugahub.domain.model.CalendarEvent
import com.shiro.yosugahub.domain.model.Project
import com.shiro.yosugahub.domain.model.Recommendation

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
