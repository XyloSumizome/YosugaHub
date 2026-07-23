package com.shiro.yosugahub

import com.shiro.yosugahub.data.local.db.KnowledgeItemWithRefs
import com.shiro.yosugahub.data.local.db.dao.DiaryDao
import com.shiro.yosugahub.data.local.db.dao.KnowledgeDao
import com.shiro.yosugahub.data.local.db.dao.PendingProposalDao
import com.shiro.yosugahub.data.local.db.dao.ProjectDao
import com.shiro.yosugahub.data.local.db.dao.TaskDao
import com.shiro.yosugahub.data.local.db.entity.DiaryEntryEntity
import com.shiro.yosugahub.data.local.db.entity.ItemEntityCrossRef
import com.shiro.yosugahub.data.local.db.entity.ItemTagCrossRef
import com.shiro.yosugahub.data.local.db.entity.KnowledgeItemEntity
import com.shiro.yosugahub.data.local.db.entity.PendingProposalEntity
import com.shiro.yosugahub.data.local.db.entity.ProjectEntity
import com.shiro.yosugahub.data.local.db.entity.TagEntity
import com.shiro.yosugahub.data.local.db.entity.TaskEntity
import com.shiro.yosugahub.data.local.db.entity.TrackedEntityEntity
import com.shiro.yosugahub.data.local.db.SampleSeed
import com.shiro.yosugahub.data.obsidian.AppendOutcome
import com.shiro.yosugahub.data.obsidian.KnowledgeStore
import com.shiro.yosugahub.data.repository.ApproveResult
import com.shiro.yosugahub.data.repository.DiaryRepository
import com.shiro.yosugahub.data.repository.KnowledgeRepository
import com.shiro.yosugahub.data.repository.ProjectRepository
import com.shiro.yosugahub.data.repository.ProposalRepository
import com.shiro.yosugahub.data.repository.TaskRepository
import com.shiro.yosugahub.domain.model.PendingProposal
import com.shiro.yosugahub.domain.model.ProposalStatus
import com.shiro.yosugahub.domain.model.ProposalType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 承認/棄却の反映先・状態遷移をフェイクDAO群で検証する。 */
class ProposalRepositoryTest {

    // --- フェイクDAO群(必要最小限のインメモリ実装) ---

    private class FakePendingDao : PendingProposalDao {
        val statuses = mutableMapOf<String, String>()
        override fun observeByStatus(status: String): Flow<List<PendingProposalEntity>> = flowOf(emptyList())
        override fun observeCountByStatus(status: String): Flow<Int> = flowOf(0)
        override suspend fun insertAll(proposals: List<PendingProposalEntity>) = Unit
        override suspend fun updateStatus(id: String, status: String) {
            statuses[id] = status
        }
        override suspend fun recent(limit: Int): List<PendingProposalEntity> = emptyList()
    }

    private class FakeTaskDao : TaskDao {
        val stored = mutableListOf<TaskEntity>()
        override fun observeAll(): Flow<List<TaskEntity>> = flowOf(stored.toList())
        override fun observeByProject(projectId: String): Flow<List<TaskEntity>> = flowOf(emptyList())
        override suspend fun count(): Int = stored.size
        override suspend fun insertAll(tasks: List<TaskEntity>) = Unit
        override suspend fun upsert(task: TaskEntity) {
            stored += task
        }
        override suspend fun updateStatus(id: String, status: String, completedAt: String?, updatedAt: String) = Unit
        override suspend fun deleteById(id: String) = Unit
    }

    private class FakeKnowledgeDao : KnowledgeDao {
        val items = mutableListOf<KnowledgeItemEntity>()
        val tags = mutableListOf<TagEntity>()
        val itemTags = mutableListOf<ItemTagCrossRef>()
        override fun observeItemsWithRefs(): Flow<List<KnowledgeItemWithRefs>> = flowOf(emptyList())
        override fun observeTags(): Flow<List<TagEntity>> = flowOf(emptyList())
        override fun observeEntities(): Flow<List<TrackedEntityEntity>> = flowOf(emptyList())
        override suspend fun countItems(): Int = items.size
        override suspend fun getTagByName(name: String): TagEntity? = tags.firstOrNull { it.name == name }
        override suspend fun getEntityByNameAndType(name: String, type: String): TrackedEntityEntity? = null
        override suspend fun upsertItem(item: KnowledgeItemEntity) {
            items += item
        }
        override suspend fun insertTag(tag: TagEntity) {
            tags += tag
        }
        override suspend fun insertEntity(entity: TrackedEntityEntity) = Unit
        override suspend fun insertItemTag(ref: ItemTagCrossRef) {
            itemTags += ref
        }
        override suspend fun insertItemEntity(ref: ItemEntityCrossRef) = Unit
        override suspend fun clearItemTags(itemId: String) = Unit
        override suspend fun clearItemEntities(itemId: String) = Unit
        override suspend fun deleteItemRow(itemId: String) = Unit
    }

    private class FakeDiaryDao : DiaryDao {
        val stored = mutableListOf<DiaryEntryEntity>()
        override fun observeAll(): Flow<List<DiaryEntryEntity>> = flowOf(stored.toList())
        override suspend fun count(): Int = stored.size
        override suspend fun insert(entry: DiaryEntryEntity) {
            stored += entry
        }
        override suspend fun insertAll(entries: List<DiaryEntryEntity>) = Unit
        override suspend fun deleteById(id: String) = Unit
    }

    private class FakeProjectDao : ProjectDao {
        val healthUpdates = mutableListOf<Pair<String, String>>()
        private val knownIds = SampleSeed.projects.map { it.id }.toSet()
        override fun observeAll(): Flow<List<ProjectEntity>> = flowOf(emptyList())
        override suspend fun count(): Int = 0
        override suspend fun insertAll(projects: List<ProjectEntity>) = Unit
        override suspend fun upsert(project: ProjectEntity) = Unit
        override suspend fun updateHealth(id: String, health: String, lastUpdated: String): Int {
            return if (id in knownIds) {
                healthUpdates += id to health
                1
            } else 0
        }
    }

    private class FakeKnowledgeStore(
        var outcome: AppendOutcome = AppendOutcome.WRITTEN,
    ) : KnowledgeStore {
        val appended = mutableListOf<Pair<String, String>>() // noteName to markdown
        override suspend fun appendToNote(noteName: String, markdown: String): AppendOutcome {
            appended += noteName to markdown
            return outcome
        }
    }

    // --- セットアップ ---

    private val fixedNow = "2026-07-23T17:00:00+09:00"
    private var counter = 0

    private class Env {
        val pendingDao = FakePendingDao()
        val taskDao = FakeTaskDao()
        val knowledgeDao = FakeKnowledgeDao()
        val diaryDao = FakeDiaryDao()
        val projectDao = FakeProjectDao()
        val knowledgeStore = FakeKnowledgeStore()
    }

    private fun repository(env: Env): ProposalRepository = ProposalRepository(
        dao = env.pendingDao,
        taskRepository = TaskRepository(env.taskDao, now = { fixedNow }, newId = { "t-${counter++}" }),
        knowledgeRepository = KnowledgeRepository(env.knowledgeDao, now = { fixedNow }, newId = { "k-${counter++}" }),
        diaryRepository = DiaryRepository(env.diaryDao, now = { fixedNow }, newId = { "d-${counter++}" }),
        projectRepository = ProjectRepository(env.projectDao, now = { fixedNow }),
        knowledgeStore = env.knowledgeStore,
    )

    private fun proposal(type: ProposalType, payload: String) = PendingProposal(
        id = "prop-1",
        type = type,
        payloadJson = payload,
        status = ProposalStatus.PENDING,
        receivedAt = "2026-07-23T16:30:00+09:00",
    )

    // --- テスト ---

    @Test
    fun approve_task_creates_assistant_task_and_marks_approved() = runBlocking {
        val env = Env()
        val result = repository(env).approve(
            proposal(ProposalType.TASK, """{"projectId":"anri","title":"戦闘調整","priority":"high"}""")
        )
        assertEquals(ApproveResult.Applied(), result)
        val task = env.taskDao.stored.single()
        assertEquals("戦闘調整", task.title)
        assertEquals("assistant", task.source)
        assertEquals("anri", task.projectId)
        assertEquals("approved", env.pendingDao.statuses["prop-1"])
    }

    @Test
    fun approve_item_creates_knowledge_item_with_tags() = runBlocking {
        val env = Env()
        val result = repository(env).approve(
            proposal(
                ProposalType.ITEM,
                """{"kind":"decision","title":"ビート表示を採用","body":"理由","tags":["Yosuga Hub"]}""",
            )
        )
        assertEquals(ApproveResult.Applied(), result)
        val item = env.knowledgeDao.items.single()
        assertEquals("decision", item.kind)
        assertEquals("assistant", item.source)
        assertEquals("Yosuga Hub", env.knowledgeDao.tags.single().name)
        assertEquals(1, env.knowledgeDao.itemTags.size)
    }

    @Test
    fun approve_item_with_target_note_appends_to_obsidian() = runBlocking {
        val env = Env()
        val result = repository(env).approve(
            proposal(
                ProposalType.ITEM,
                """{"kind":"memo","title":"設計メモ","body":"本文","tags":["UI"],"targetNote":"GameDesign"}""",
            )
        )
        assertEquals(ApproveResult.Applied(AppendOutcome.WRITTEN), result)
        val (note, markdown) = env.knowledgeStore.appended.single()
        assertEquals("GameDesign", note)
        assertTrue(markdown.contains("## 設計メモ"))
        assertTrue(markdown.contains("#UI"))
        // Room 側の保存も成立している
        assertEquals(1, env.knowledgeDao.items.size)
    }

    @Test
    fun approve_item_when_vault_not_configured_still_applies_to_room() = runBlocking {
        val env = Env()
        env.knowledgeStore.outcome = AppendOutcome.NOT_CONFIGURED
        val result = repository(env).approve(
            proposal(ProposalType.ITEM, """{"kind":"memo","title":"メモ","targetNote":"Note"}""")
        )
        assertEquals(ApproveResult.Applied(AppendOutcome.NOT_CONFIGURED), result)
        assertEquals(1, env.knowledgeDao.items.size)
        assertEquals("approved", env.pendingDao.statuses["prop-1"])
    }

    @Test
    fun approve_item_without_target_note_skips_obsidian() = runBlocking {
        val env = Env()
        repository(env).approve(
            proposal(ProposalType.ITEM, """{"kind":"memo","title":"メモだけ"}""")
        )
        assertTrue(env.knowledgeStore.appended.isEmpty())
    }

    @Test
    fun approve_diary_falls_back_to_received_date_when_blank() = runBlocking {
        val env = Env()
        val result = repository(env).approve(
            proposal(ProposalType.DIARY, """{"body":"今日はシロさんが..."}""")
        )
        assertEquals(ApproveResult.Applied(), result)
        assertEquals("2026-07-23", env.diaryDao.stored.single().date)
    }

    @Test
    fun approve_health_updates_known_project() = runBlocking {
        val env = Env()
        val result = repository(env).approve(
            proposal(ProposalType.HEALTH, """{"projectId":"gengenkyo","health":"停滞中"}""")
        )
        assertEquals(ApproveResult.Applied(), result)
        assertEquals("gengenkyo" to "停滞中", env.projectDao.healthUpdates.single())
    }

    @Test
    fun approve_health_for_unknown_project_is_rejected() = runBlocking {
        val env = Env()
        val result = repository(env).approve(
            proposal(ProposalType.HEALTH, """{"projectId":"ghost","health":"順調"}""")
        )
        assertEquals(ApproveResult.NotApplicable, result)
        assertEquals("rejected", env.pendingDao.statuses["prop-1"])
        assertTrue(env.projectDao.healthUpdates.isEmpty())
    }

    @Test
    fun approve_broken_payload_is_rejected_without_crash() = runBlocking {
        val env = Env()
        val result = repository(env).approve(
            proposal(ProposalType.TASK, "{ broken json ")
        )
        assertEquals(ApproveResult.NotApplicable, result)
        assertEquals("rejected", env.pendingDao.statuses["prop-1"])
        assertTrue(env.taskDao.stored.isEmpty())
    }

    @Test
    fun reject_marks_rejected_without_applying() = runBlocking {
        val env = Env()
        repository(env).reject("prop-9")
        assertEquals("rejected", env.pendingDao.statuses["prop-9"])
        assertTrue(env.taskDao.stored.isEmpty())
    }
}
