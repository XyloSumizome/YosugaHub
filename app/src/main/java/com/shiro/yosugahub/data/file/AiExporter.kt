package com.shiro.yosugahub.data.file

import com.shiro.yosugahub.data.file.model.AiExportFile
import com.shiro.yosugahub.data.file.model.CalendarFile
import com.shiro.yosugahub.data.file.model.ConversationsFile
import com.shiro.yosugahub.data.file.model.DiaryExport
import com.shiro.yosugahub.data.file.model.DirectiveExport
import com.shiro.yosugahub.data.file.model.DirectivesFile
import com.shiro.yosugahub.data.file.model.DocumentExport
import com.shiro.yosugahub.data.file.model.DocumentsFile
import com.shiro.yosugahub.data.file.model.EntityRefExport
import com.shiro.yosugahub.data.file.model.EventExport
import com.shiro.yosugahub.data.file.model.ExchangeExport
import com.shiro.yosugahub.data.file.model.KnowledgeFile
import com.shiro.yosugahub.data.file.model.KnowledgeItemExport
import com.shiro.yosugahub.data.file.model.ProjectsFile
import com.shiro.yosugahub.data.file.model.TasksFile
import com.shiro.yosugahub.domain.model.CalendarEvent
import com.shiro.yosugahub.domain.model.DiaryEntry
import com.shiro.yosugahub.domain.model.Directive
import com.shiro.yosugahub.domain.model.Document
import com.shiro.yosugahub.domain.model.DocumentStatus
import com.shiro.yosugahub.domain.model.KnowledgeItem
import com.shiro.yosugahub.domain.model.PendingProposal
import com.shiro.yosugahub.domain.model.Project
import com.shiro.yosugahub.domain.model.ProjectStatusSnapshot
import com.shiro.yosugahub.domain.model.ProposalType
import com.shiro.yosugahub.domain.model.Task
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * AI向けの用途別JSONを組み立てる純粋ロジック(v4 Phase2)。
 * ContextExporter(手動ブリッジの状況JSON)とは独立で、こちらはサーバー同期・ローカル出力用。
 * projects の変換は ContextExporter と同じロジックを共有する。
 */
object AiExporter {

    const val FILE_PROJECTS = "projects.json"
    const val FILE_TASKS = "tasks.json"
    const val FILE_KNOWLEDGE = "knowledge.json"
    const val FILE_CALENDAR = "calendar.json"
    const val FILE_CONVERSATIONS = "conversations.json"
    const val FILE_DOCUMENTS = "documents.json"
    const val FILE_DIRECTIVES = "directives.json"

    /** ヨスガに分類してほしい文書の状態(v4.1)。分類済み・アーカイブは送らない。 */
    private val CLASSIFIABLE_STATUSES = setOf(
        DocumentStatus.UNCLASSIFIED,
        DocumentStatus.CLASSIFICATION_PENDING,
    )

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    /** 7ファイルすべてを組み立てる。 */
    fun buildAll(
        generatedAt: String,
        projects: List<Project>,
        statuses: Map<String, ProjectStatusSnapshot>,
        tasks: List<Task>,
        items: List<KnowledgeItem>,
        diary: List<DiaryEntry>,
        todayEvents: List<CalendarEvent>,
        upcomingEvents: List<CalendarEvent>,
        pastEvents: List<CalendarEvent>,
        exchanges: List<PendingProposal>,
        documents: List<Document>,
        directives: List<Directive>,
    ): List<AiExportFile> = listOf(
        AiExportFile(
            FILE_PROJECTS,
            json.encodeToString(
                ProjectsFile(
                    generatedAt = generatedAt,
                    projects = projects.map { ContextExporter.projectExportOf(it, statuses[it.id]) },
                )
            ),
        ),
        AiExportFile(
            FILE_TASKS,
            json.encodeToString(
                TasksFile(
                    generatedAt = generatedAt,
                    tasks = tasks.map { ContextExporter.taskExportOf(it) },
                )
            ),
        ),
        AiExportFile(
            FILE_KNOWLEDGE,
            json.encodeToString(
                KnowledgeFile(
                    generatedAt = generatedAt,
                    items = items.map { it.toExport() },
                    diary = diary.map { DiaryExport(date = it.date, body = it.body) },
                )
            ),
        ),
        AiExportFile(
            FILE_CALENDAR,
            json.encodeToString(
                CalendarFile(
                    generatedAt = generatedAt,
                    today = todayEvents.map { it.toExport() },
                    upcoming = upcomingEvents.map { it.toExport() },
                    past = pastEvents.map { it.toExport() },
                )
            ),
        ),
        AiExportFile(
            FILE_CONVERSATIONS,
            json.encodeToString(
                ConversationsFile(
                    generatedAt = generatedAt,
                    exchanges = exchanges.map { it.toExchange() },
                )
            ),
        ),
        AiExportFile(
            FILE_DOCUMENTS,
            json.encodeToString(
                DocumentsFile(
                    generatedAt = generatedAt,
                    pendingClassification = documents
                        .filter { it.status in CLASSIFIABLE_STATUSES }
                        .map { it.toExport() },
                )
            ),
        ),
        AiExportFile(
            FILE_DIRECTIVES,
            json.encodeToString(
                DirectivesFile(
                    generatedAt = generatedAt,
                    directives = directives.map { it.toExport() },
                )
            ),
        ),
    )

    private fun Directive.toExport() = DirectiveExport(
        directiveId = id,
        projectId = projectId,
        title = title,
        body = body,
        priority = priority,
        createdAt = createdAt,
    )

    private fun Document.toExport() = DocumentExport(
        documentId = id,
        title = title,
        body = body,
        status = status.dbValue,
        createdAt = createdAt,
    )

    private fun KnowledgeItem.toExport() = KnowledgeItemExport(
        kind = kind.dbValue,
        title = title,
        body = body,
        tags = tags,
        entities = entities.map { EntityRefExport(name = it.name, type = it.type.dbValue) },
        createdAt = createdAt,
    )

    private fun CalendarEvent.toExport() = EventExport(
        title = title,
        start = start,
        end = end,
        calendarName = calendarName,
        description = description,
    )

    /** 提案履歴の1件。payload から表示用タイトルを取り出す(壊れていたら空)。 */
    private fun PendingProposal.toExchange(): ExchangeExport = ExchangeExport(
        type = type.dbValue,
        status = status.dbValue,
        receivedAt = receivedAt,
        title = when (type) {
            ProposalType.TASK -> ProposalPayloads.decodeTask(payloadJson)?.title
            ProposalType.ITEM -> ProposalPayloads.decodeItem(payloadJson)?.title
            ProposalType.DIARY -> ProposalPayloads.decodeDiary(payloadJson)?.date
            ProposalType.HEALTH -> ProposalPayloads.decodeHealth(payloadJson)
                ?.let { "${it.projectId} → ${it.health}" }
            ProposalType.DIRECTIVE -> ProposalPayloads.decodeDirective(payloadJson)?.title
        }.orEmpty(),
    )
}
