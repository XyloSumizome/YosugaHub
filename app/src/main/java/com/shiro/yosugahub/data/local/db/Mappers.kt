package com.shiro.yosugahub.data.local.db

import com.shiro.yosugahub.data.local.db.entity.CalendarEventEntity
import com.shiro.yosugahub.data.local.db.entity.DiaryEntryEntity
import com.shiro.yosugahub.data.local.db.entity.PendingProposalEntity
import com.shiro.yosugahub.data.local.db.entity.ProjectEntity
import com.shiro.yosugahub.data.local.db.entity.RecommendationEntity
import com.shiro.yosugahub.data.local.db.entity.TaskEntity
import com.shiro.yosugahub.data.local.db.entity.TrackedEntityEntity
import com.shiro.yosugahub.domain.model.CalendarEvent
import com.shiro.yosugahub.domain.model.DiaryEntry
import com.shiro.yosugahub.domain.model.EntityRef
import com.shiro.yosugahub.domain.model.EntityType
import com.shiro.yosugahub.domain.model.ItemKind
import com.shiro.yosugahub.domain.model.KnowledgeItem
import com.shiro.yosugahub.domain.model.PendingProposal
import com.shiro.yosugahub.domain.model.Project
import com.shiro.yosugahub.domain.model.ProposalStatus
import com.shiro.yosugahub.domain.model.ProposalType
import com.shiro.yosugahub.domain.model.Recommendation
import com.shiro.yosugahub.domain.model.Task
import com.shiro.yosugahub.domain.model.TaskStatus
import com.shiro.yosugahub.domain.model.TrackedEntity

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

/** プロジェクトは 1-d で編集可能になったため、書き込み用の逆変換も持つ。 */
fun Project.toEntity(): ProjectEntity = ProjectEntity(
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

fun KnowledgeItemWithRefs.toDomain(): KnowledgeItem = KnowledgeItem(
    id = item.id,
    kind = ItemKind.fromDb(item.kind),
    title = item.title,
    body = item.body,
    tags = tags.map { it.name }.sorted(),
    entities = entities.map { EntityRef(name = it.name, type = EntityType.fromDb(it.type)) },
    createdAt = item.createdAt,
    updatedAt = item.updatedAt,
    source = item.source,
)

fun TrackedEntityEntity.toDomain(): TrackedEntity = TrackedEntity(
    id = id,
    name = name,
    type = EntityType.fromDb(type),
)

fun DiaryEntryEntity.toDomain(): DiaryEntry = DiaryEntry(
    id = id,
    date = date,
    body = body,
    createdAt = createdAt,
)

/**
 * 提案の変換。type が未知(将来のスキーマ由来など)の行は null を返し、
 * 呼び出し側で読み飛ばす(クラッシュさせない)。
 */
fun PendingProposalEntity.toDomainOrNull(): PendingProposal? {
    val proposalType = ProposalType.fromDb(type) ?: return null
    return PendingProposal(
        id = id,
        type = proposalType,
        payloadJson = payloadJson,
        status = ProposalStatus.fromDb(status),
        receivedAt = receivedAt,
    )
}

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
