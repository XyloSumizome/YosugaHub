package com.shiro.yosugahub

import com.shiro.yosugahub.data.file.AiExporter
import com.shiro.yosugahub.data.file.model.CalendarFile
import com.shiro.yosugahub.data.file.model.ConversationsFile
import com.shiro.yosugahub.data.file.model.DocumentsFile
import com.shiro.yosugahub.data.file.model.KnowledgeFile
import com.shiro.yosugahub.data.file.model.ProjectsFile
import com.shiro.yosugahub.data.file.model.TasksFile
import com.shiro.yosugahub.domain.model.CalendarEvent
import com.shiro.yosugahub.domain.model.DiaryEntry
import com.shiro.yosugahub.domain.model.Document
import com.shiro.yosugahub.domain.model.DocumentStatus
import com.shiro.yosugahub.domain.model.ItemKind
import com.shiro.yosugahub.domain.model.KnowledgeItem
import com.shiro.yosugahub.domain.model.PendingProposal
import com.shiro.yosugahub.domain.model.Project
import com.shiro.yosugahub.domain.model.ProposalStatus
import com.shiro.yosugahub.domain.model.ProposalType
import com.shiro.yosugahub.domain.model.Task
import com.shiro.yosugahub.domain.model.TaskStatus
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiExporterTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val generatedAt = "2026-07-23T23:00:00+09:00"

    private fun buildAll() = AiExporter.buildAll(
        generatedAt = generatedAt,
        projects = listOf(
            Project(
                id = "anri", name = "ANRI", currentGoal = "プロトタイプ", inProgress = "第2章",
                nextTask = "戦闘調整", lastUpdated = "2026-07-22 18:00", health = "on_track",
            ),
        ),
        statuses = emptyMap(),
        tasks = listOf(
            Task(
                id = "t1", projectId = "anri", title = "戦闘調整", detail = "",
                status = TaskStatus.DOING, priority = "high", dueDate = null,
                createdAt = generatedAt, updatedAt = generatedAt, completedAt = null, source = "manual",
            ),
        ),
        items = listOf(
            KnowledgeItem(
                id = "k1", kind = ItemKind.DECISION, title = "ビート表示を採用", body = "理由",
                tags = listOf("UI"), entities = emptyList(),
                createdAt = generatedAt, updatedAt = generatedAt, source = "assistant",
            ),
        ),
        diary = listOf(DiaryEntry(id = "d1", date = "2026-07-23", body = "今日は...", createdAt = generatedAt)),
        todayEvents = listOf(CalendarEvent("歯医者", "10:00", "11:00", "個人")),
        upcomingEvents = emptyList(),
        pastEvents = listOf(CalendarEvent("素材整理", "07-20 20:00", "07-20 21:00", "制作")),
        exchanges = listOf(
            PendingProposal(
                id = "p1", type = ProposalType.ITEM,
                payloadJson = """{"kind":"memo","title":"取り込んだメモ"}""",
                status = ProposalStatus.APPROVED, receivedAt = generatedAt,
            ),
            PendingProposal(
                id = "p2", type = ProposalType.HEALTH,
                payloadJson = """{"projectId":"anri","health":"停滞中"}""",
                status = ProposalStatus.REJECTED, receivedAt = generatedAt,
            ),
        ),
        documents = listOf(
            document("doc-new", DocumentStatus.UNCLASSIFIED),
            document("doc-pending", DocumentStatus.CLASSIFICATION_PENDING),
            document("doc-review", DocumentStatus.NEEDS_REVIEW),
            document("doc-done", DocumentStatus.CLASSIFIED),
            document("doc-old", DocumentStatus.ARCHIVED),
        ),
    )

    private fun document(id: String, status: DocumentStatus) = Document(
        id = id,
        title = "文書 $id",
        body = "原文 $id",
        status = status,
        createdAt = generatedAt,
        updatedAt = generatedAt,
        source = "manual",
        currentClassification = null,
    )

    @Test
    fun builds_six_named_files() {
        val files = buildAll()
        assertEquals(
            listOf(
                "projects.json", "tasks.json", "knowledge.json",
                "calendar.json", "conversations.json", "documents.json",
            ),
            files.map { it.name },
        )
    }

    @Test
    fun documents_file_carries_only_classifiable_documents_with_original_body() {
        val files = buildAll().associateBy { it.name }
        val documents = json.decodeFromString<DocumentsFile>(files["documents.json"]!!.content)

        // 分類済み・確認待ち・アーカイブは送らない(再分類を誘発しない)。
        assertEquals(
            listOf("doc-new", "doc-pending"),
            documents.pendingClassification.map { it.documentId },
        )
        assertEquals(1, documents.schemaVersion)
        assertEquals(generatedAt, documents.generatedAt)
        assertEquals("原文 doc-new", documents.pendingClassification.first().body)
        assertEquals("unclassified", documents.pendingClassification.first().status)
    }

    @Test
    fun each_file_parses_and_carries_schema_and_generated_at() {
        val files = buildAll().associateBy { it.name }

        val projects = json.decodeFromString<ProjectsFile>(files["projects.json"]!!.content)
        assertEquals(1, projects.schemaVersion)
        assertEquals(generatedAt, projects.generatedAt)
        assertEquals("anri", projects.projects.single().id)

        val tasks = json.decodeFromString<TasksFile>(files["tasks.json"]!!.content)
        assertEquals("doing", tasks.tasks.single().status)

        val knowledge = json.decodeFromString<KnowledgeFile>(files["knowledge.json"]!!.content)
        assertEquals("decision", knowledge.items.single().kind)
        assertEquals("2026-07-23", knowledge.diary.single().date)

        val calendar = json.decodeFromString<CalendarFile>(files["calendar.json"]!!.content)
        assertEquals(1, calendar.today.size)
        assertEquals(0, calendar.upcoming.size)
        assertEquals(1, calendar.past.size)
    }

    @Test
    fun conversations_extract_titles_from_payloads() {
        val files = buildAll().associateBy { it.name }
        val conversations = json.decodeFromString<ConversationsFile>(files["conversations.json"]!!.content)
        assertEquals(2, conversations.exchanges.size)
        assertEquals("取り込んだメモ", conversations.exchanges[0].title)
        assertEquals("approved", conversations.exchanges[0].status)
        assertEquals("anri → 停滞中", conversations.exchanges[1].title)
    }

    @Test
    fun broken_payload_in_history_does_not_crash() {
        val files = AiExporter.buildAll(
            generatedAt = generatedAt,
            projects = emptyList(), statuses = emptyMap(), tasks = emptyList(),
            items = emptyList(), diary = emptyList(),
            todayEvents = emptyList(), upcomingEvents = emptyList(), pastEvents = emptyList(),
            exchanges = listOf(
                PendingProposal("p1", ProposalType.TASK, "{ broken", ProposalStatus.PENDING, generatedAt),
            ),
            documents = emptyList(),
        )
        val conversations = json.decodeFromString<ConversationsFile>(
            files.single { it.name == "conversations.json" }.content
        )
        assertTrue(conversations.exchanges.single().title.isEmpty())
    }
}
